package tcdx.uap.common.utils;

import org.springframework.jdbc.datasource.DataSourceUtils;

import javax.sql.DataSource;
import java.sql.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * JDBC-based SQL introspection without third-party parsers (JDK 8).
 *
 * Features:
 * - Extracts output column labels (alias preferred)
 * - Extracts involved base tables/views (filters out aliases & CTE names)
 * - Vendor-aware zero-row probing & EXPLAIN fallbacks
 * - **New**: Safe overloads that accept a {@link DataSource} and ALWAYS release connections
 * - **New**: Optional timeout controls to avoid long-running validations
 *
 * Usage recommendations:
 * 1) In Spring-managed code, prefer the `UsingSpring(...)` overloads which use
 *    {@link DataSourceUtils#getConnection(DataSource)} / releaseConnection(...)
 *    so they cooperate with @Transactional.
 * 2) Outside of Spring transactions, you can use the plain DataSource overloads
 *    which obtain/close connections with try-with-resources.
 * 3) The original Connection-based methods are kept for backward-compatibility
 *    and **do not** close the passed Connection (call site is responsible).
 */
public class SqlJdbcIntrospector {

    /** Default per-statement timeout (seconds) for metadata probing. */
    public static final int DEFAULT_TIMEOUT_SEC = 3;

    /** Supported DB vendors */
    public enum DbVendor {
        POSTGRES, MYSQL, SQLSERVER, ORACLE, OTHER
    }

    /* =========================================================
     * Public API — DataSource overloads (auto-release)
     * ========================================================= */

    /** Extract SELECT output labels (alias preferred) — auto close Connection. */
    public static List<String> extractFieldAliasesByJDBC(DataSource ds, String sql) throws SQLException {
        return extractFieldAliasesByJDBC(ds, sql, DEFAULT_TIMEOUT_SEC);
    }

    /** Extract SELECT output labels (alias preferred) — auto close Connection. */
    public static List<String> extractFieldAliasesByJDBC(DataSource ds, String sql, int timeoutSec) throws SQLException {
        try (Connection conn = ds.getConnection()) {
            return extractFieldAliasesByJDBC(conn, sql, detectVendor(conn), timeoutSec);
        }
    }

    /** Extract involved table/view names — auto close Connection. */
    public static List<String> extractTableNamesByJDBC(DataSource ds, String sql) throws SQLException {
        return extractTableNamesByJDBC(ds, sql, DEFAULT_TIMEOUT_SEC);
    }

    /** Extract involved table/view names — auto close Connection. */
    public static List<String> extractTableNamesByJDBC(DataSource ds, String sql, int timeoutSec) throws SQLException {
        try (Connection conn = ds.getConnection()) {
            return extractTableNamesByJDBC(conn, sql, detectVendor(conn), timeoutSec);
        }
    }

    /* =========================================================
     * Public API — DataSource overloads that cooperate with Spring transactions
     * ========================================================= */

    /** Spring-aware: uses DataSourceUtils; will NOT actually close a tx-bound Connection. */
    public static List<String> extractFieldAliasesByJDBCUsingSpring(DataSource ds, String sql) throws SQLException {
        return extractFieldAliasesByJDBCUsingSpring(ds, sql, DEFAULT_TIMEOUT_SEC);
    }

    /** Spring-aware: uses DataSourceUtils with timeout. */
    public static List<String> extractFieldAliasesByJDBCUsingSpring(DataSource ds, String sql, int timeoutSec) throws SQLException {
        Connection conn = DataSourceUtils.getConnection(ds);
        try {
            return extractFieldAliasesByJDBC(conn, sql, detectVendor(conn), timeoutSec);
        } finally {
            DataSourceUtils.releaseConnection(conn, ds);
        }
    }

    /** Spring-aware: uses DataSourceUtils; will NOT actually close a tx-bound Connection. */
    public static List<String> extractTableNamesByJDBCUsingSpring(DataSource ds, String sql) throws SQLException {
        return extractTableNamesByJDBCUsingSpring(ds, sql, DEFAULT_TIMEOUT_SEC);
    }

    /** Spring-aware: uses DataSourceUtils with timeout. */
    public static List<String> extractTableNamesByJDBCUsingSpring(DataSource ds, String sql, int timeoutSec) throws SQLException {
        Connection conn = DataSourceUtils.getConnection(ds);
        try {
            return extractTableNamesByJDBC(conn, sql, detectVendor(conn), timeoutSec);
        } finally {
            DataSourceUtils.releaseConnection(conn, ds);
        }
    }

    /* =========================================================
     * Public API — Connection overloads (backward compatible; caller manages conn)
     * ========================================================= */

    /** Extract SELECT output labels (alias preferred; falls back to column name) */
    public static List<String> extractFieldAliasesByJDBC(Connection conn, String sql) throws SQLException {
        return extractFieldAliasesByJDBC(conn, sql, detectVendor(conn), DEFAULT_TIMEOUT_SEC);
    }

    /** Extract involved table/view names */
    public static List<String> extractTableNamesByJDBC(Connection conn, String sql) throws SQLException {
        return extractTableNamesByJDBC(conn, sql, detectVendor(conn), DEFAULT_TIMEOUT_SEC);
    }

    /* =========================================================
     * Internal core (with explicit vendor & timeout)
     * ========================================================= */

    /** Extract SELECT output labels (alias preferred; falls back to column name) */
    public static List<String> extractFieldAliasesByJDBC(Connection conn, String sql, DbVendor vendor, int timeoutSec) throws SQLException {
        List<String> labels = new ArrayList<>();
        String normalized = normalizePlaceholders(sql);
        String lower = stripLeading(normalized).toLowerCase(Locale.ROOT);
        if (!(lower.startsWith("select") || lower.startsWith("with"))) return labels;

        if (vendor == DbVendor.SQLSERVER) {
            // SQL Server: do not execute query; ask metadata via system proc
            String batch = "EXEC sp_describe_first_result_set @tsql = N'" + escapeSqlServerNVar(normalized) + "'";
            try (Statement st = conn.createStatement()) {
                applyTimeout(st, timeoutSec);
                try (ResultSet rs = st.executeQuery(batch)) {
                    while (rs.next()) {
                        String name = rs.getString("name"); // alias if present
                        if (name != null) labels.add(name);
                    }
                }
            }
            return labels;
        }

        String zero = toZeroRowSql(normalized, vendor);
        try (PreparedStatement ps = conn.prepareStatement(zero)) {
            applyTimeout(ps, timeoutSec);
            try (ResultSet rs = ps.executeQuery()) {
                ResultSetMetaData md = rs.getMetaData();
                for (int i = 1; i <= md.getColumnCount(); i++) {
                    labels.add(md.getColumnLabel(i)); // alias preferred
                }
            }
        }
        return labels;
    }

    /** Extract involved table/view names */
    public static List<String> extractTableNamesByJDBC(Connection conn, String sql, DbVendor vendor, int timeoutSec) throws SQLException {
        String normalized = normalizePlaceholders(sql);
        String lower = stripLeading(normalized).toLowerCase(Locale.ROOT);
        if (!(lower.startsWith("select") || lower.startsWith("with"))) return Collections.emptyList();

        LinkedHashSet<String> tables = new LinkedHashSet<>();

        // Step 1: use ResultSetMetaData (some drivers fill table names for simple queries)
        if (vendor != DbVendor.SQLSERVER) { // SQL Server rarely fills this; skip to reduce cost
            try {
                String zero = toZeroRowSql(normalized, vendor);
                try (PreparedStatement ps = conn.prepareStatement(zero)) {
                    applyTimeout(ps, timeoutSec);
                    try (ResultSet rs = ps.executeQuery()) {
                        ResultSetMetaData md = rs.getMetaData();
                        for (int i = 1; i <= md.getColumnCount(); i++) {
                            String tn = safeTrim(md.getTableName(i));
                            if (tn != null && !tn.isEmpty()) {
                                tables.add(tn);
                            }
                        }
                    }
                }
            } catch (SQLException ignore) {
                // ignore and fall through to EXPLAIN
            }
        }
        if (!tables.isEmpty()) {
            return new ArrayList<>(tables);
        }

        // Step 2: EXPLAIN / SHOWPLAN
        try {
            switch (vendor) {
                case POSTGRES:
                    tables.addAll(explainPostgres(conn, normalized, timeoutSec));
                    break;
                case MYSQL:
                    tables.addAll(explainMySQL(conn, normalized, timeoutSec));
                    break;
                case SQLSERVER:
                    tables.addAll(explainSqlServer(conn, normalized, timeoutSec));
                    break;
                case ORACLE:
                    tables.addAll(explainOracle(conn, normalized, timeoutSec));
                    break;
                default:
                    break;
            }
        } catch (SQLException ignore) {
            // no privilege / unsupported — will fallback
        }
        if (!tables.isEmpty()) {
            // filter aliases from EXPLAIN results too (rare, but safe)
            filterOutAliasesAndGenerated(tables, normalized);
            return new ArrayList<>(tables);
        }

        // Step 3: regex fallback (exclude CTE names) + filter aliases
        Set<String> cteNames = extractCteNames(normalized);
        tables.addAll(regexExtractTables(normalized, cteNames));
        filterOutAliasesAndGenerated(tables, normalized);

        return new ArrayList<>(tables);
    }

    /* =========================================================
     * Vendor detection & helpers
     * ========================================================= */

    public static DbVendor detectVendor(Connection conn) {
        try {
            String name = conn.getMetaData().getDatabaseProductName().toLowerCase(Locale.ROOT);
            if (name.contains("postgres")) return DbVendor.POSTGRES;
            if (name.contains("mysql") || name.contains("mariadb")) return DbVendor.MYSQL;
            if (name.contains("microsoft") || name.contains("sql server")) return DbVendor.SQLSERVER;
            if (name.contains("oracle")) return DbVendor.ORACLE;
        } catch (SQLException ignore) {}
        return DbVendor.OTHER;
    }

    /** Replace custom placeholders like (...z_table123_ids) -> (1) */
    private static String normalizePlaceholders(String sql) {
        if (sql == null) return "";
        return sql.replaceAll("\\(\\.\\.\\.z_table\\d+_ids\\)", "(1)");
    }

    /** Build zero-row SQL for metadata (keeps WITH at top level) */
    private static String toZeroRowSql(String sql, DbVendor vendor) {
        String s = stripTrailingSemicolon(sql);
        switch (vendor) {
            case POSTGRES:
            case MYSQL:
                return s + " LIMIT 0";
            case ORACLE:
                return s + " FETCH FIRST 0 ROWS ONLY";
            default:
                // unknown: if it starts with WITH, executing raw may be needed (costly)
                String lower = stripLeading(s).toLowerCase(Locale.ROOT);
                if (lower.startsWith("with")) {
                    return s; // call site will only fetch metadata
                } else {
                    return "SELECT * FROM (" + s + ") __x WHERE 1=0";
                }
        }
    }

    /** JDK 8 version of stripLeading */
    private static String stripLeading(String s) {
        if (s == null) return "";
        int i = 0, len = s.length();
        while (i < len && Character.isWhitespace(s.charAt(i))) i++;
        return s.substring(i);
    }

    private static String stripTrailingSemicolon(String s) {
        if (s == null) return "";
        String t = s.trim();
        if (t.endsWith(";")) return t.substring(0, t.length() - 1);
        return t;
    }

    private static String safeTrim(String s) {
        return s == null ? null : s.trim();
    }

    private static String escapeSqlServerNVar(String s) {
        return s == null ? "" : s.replace("'", "''");
    }

    private static void applyTimeout(Statement st, int timeoutSec) throws SQLException {
        if (timeoutSec > 0) st.setQueryTimeout(timeoutSec);
    }

    /* =========================================================
     * EXPLAIN extractors (vendor-specific) — with timeouts
     * ========================================================= */

    // PostgreSQL: EXPLAIN (VERBOSE, FORMAT JSON) <sql> ; parse "Relation Name"
    private static Set<String> explainPostgres(Connection conn, String sql, int timeoutSec) throws SQLException {
        LinkedHashSet<String> out = new LinkedHashSet<>();
        String q = "EXPLAIN (VERBOSE, FORMAT JSON) " + stripTrailingSemicolon(sql);
        try (Statement st = conn.createStatement()) {
            applyTimeout(st, timeoutSec);
            try (ResultSet rs = st.executeQuery(q)) {
                while (rs.next()) {
                    String json = rs.getString(1);
                    if (json == null) continue;
                    Matcher m = Pattern.compile("\"Relation Name\"\\s*:\\s*\"([^\"]+)\"", Pattern.CASE_INSENSITIVE).matcher(json);
                    while (m.find()) out.add(m.group(1));
                }
            }
        }
        return out;
    }

    // MySQL 5.7+/8.0: EXPLAIN FORMAT=JSON <sql> ; parse "table_name"
    private static Set<String> explainMySQL(Connection conn, String sql, int timeoutSec) throws SQLException {
        LinkedHashSet<String> out = new LinkedHashSet<>();
        String q = "EXPLAIN FORMAT=JSON " + stripTrailingSemicolon(sql);
        try (Statement st = conn.createStatement()) {
            applyTimeout(st, timeoutSec);
            try (ResultSet rs = st.executeQuery(q)) {
                while (rs.next()) {
                    String json = rs.getString(1);
                    if (json == null) continue;
                    Matcher m1 = Pattern.compile("\"table_name\"\\s*:\\s*\"([^\"]+)\"", Pattern.CASE_INSENSITIVE).matcher(json);
                    while (m1.find()) out.add(m1.group(1));
                    Matcher m2 = Pattern.compile("\"table\"\\s*:\\s*\\{[^}]*\"table_name\"\\s*:\\s*\"([^\"]+)\"", Pattern.CASE_INSENSITIVE).matcher(json);
                    while (m2.find()) out.add(m2.group(1));
                }
            }
        }
        return out;
    }

    // SQL Server: SET SHOWPLAN_XML ON; <sql>; SET SHOWPLAN_XML OFF; parse Table=""
    private static Set<String> explainSqlServer(Connection conn, String sql, int timeoutSec) throws SQLException {
        LinkedHashSet<String> out = new LinkedHashSet<>();
        try (Statement st = conn.createStatement()) {
            applyTimeout(st, timeoutSec);
            st.execute("SET SHOWPLAN_XML ON");
            try (ResultSet rs = st.executeQuery(stripTrailingSemicolon(sql))) {
                while (rs.next()) {
                    String xml = rs.getString(1);
                    if (xml == null) continue;
                    Matcher m = Pattern.compile("Table\\s*=\\s*\"([^\"]+)\"", Pattern.CASE_INSENSITIVE).matcher(xml);
                    while (m.find()) out.add(m.group(1));
                }
            } finally {
                st.execute("SET SHOWPLAN_XML OFF");
            }
        }
        return out;
    }

    // Oracle: EXPLAIN PLAN FOR <sql>; then SELECT DISTINCT OBJECT_NAME FROM PLAN_TABLE
    private static Set<String> explainOracle(Connection conn, String sql, int timeoutSec) throws SQLException {
        LinkedHashSet<String> out = new LinkedHashSet<>();
        try (Statement st = conn.createStatement()) {
            applyTimeout(st, timeoutSec);
            st.execute("EXPLAIN PLAN FOR " + stripTrailingSemicolon(sql));
            try (ResultSet rs = st.executeQuery(
                    "SELECT DISTINCT OBJECT_NAME FROM PLAN_TABLE WHERE OBJECT_NAME IS NOT NULL")) {
                while (rs.next()) {
                    String name = safeTrim(rs.getString(1));
                    if (name != null && !name.isEmpty()) out.add(name);
                }
            }
        } catch (SQLException ignore) {
            // missing privilege/PLAN_TABLE — ignore
        }
        return out;
    }

    /* =========================================================
     * CTE & alias handling + regex fallback
     * ========================================================= */

    /** Collect CTE names from WITH clause (lowercased) */
    private static Set<String> extractCteNames(String sql) {
        LinkedHashSet<String> out = new LinkedHashSet<>();
        String s = removeCommentsAndQuotes(sql);
        String lower = stripLeading(s).toLowerCase(Locale.ROOT);
        if (!lower.startsWith("with")) return out;

        // Match: (WITH|,)  name  [(...)]  AS (
        Pattern p = Pattern.compile("(?i)(?:\\bwith\\b|,)\\s*([a-zA-Z0-9_]+)\\s*(?:\\([^)]*\\))?\\s+as\\s*\\(");
        Matcher m = p.matcher(s);
        while (m.find()) {
            out.add(m.group(1).toLowerCase(Locale.ROOT));
        }
        return out;
    }

    /** Remove comments and quoted strings to reduce false positives in regex */
    private static String removeCommentsAndQuotes(String s) {
        if (s == null) return "";
        String t = s;
        // block comments (DOTALL)
        t = t.replaceAll("(?s)/\\*.*?\\*/", " ");
        // line comments
        t = t.replaceAll("(?m)--.*?$", " ");
        // single-quoted strings
        t = t.replaceAll("'([^']|'')*'", "''");
        // double-quoted strings
        t = t.replaceAll("\"([^\"]|\"\")*\"", "\"\"");
        // backticked strings
        t = t.replaceAll("`([^`]|``)*`", "``");
        return t;
    }

    /** Regex fallback: extract names after FROM/JOIN (exclude CTE names) */
    private static Set<String> regexExtractTables(String sql, Set<String> cteNamesLower) {
        LinkedHashSet<String> out = new LinkedHashSet<>();
        String s = removeCommentsAndQuotes(sql);

        Pattern p = Pattern.compile("(?i)\\bFROM\\s+([a-zA-Z0-9_\\.]+)|\\bJOIN\\s+([a-zA-Z0-9_\\.]+)");
        Matcher m = p.matcher(s);
        while (m.find()) {
            String g1 = m.group(1);
            String g2 = m.group(2);
            String name = safeTrim(g1 != null ? g1 : g2);
            if (name == null || name.isEmpty()) continue;

            // strip trailing alias: "schema.tab AS t" / "schema.tab t"
            name = name.replaceAll("(?i)\\s+AS\\s+.*$", "")
                    .replaceAll("\\s+\\w+$", "");
            if (name.startsWith("(")) continue;

            String lower = name.toLowerCase(Locale.ROOT);
            if (!cteNamesLower.contains(lower)) {
                out.add(name);
            }
        }
        return out;
    }

    /** Collect aliases after FROM/JOIN to filter out later (lowercased) */
    private static Set<String> extractFromJoinAliases(String sql) {
        LinkedHashSet<String> aliases = new LinkedHashSet<>();
        String s = removeCommentsAndQuotes(sql);

        // FROM schema.table t  |  FROM table AS t  |  JOIN (subquery) t
        Pattern p = Pattern.compile("(?i)\\b(?:FROM|JOIN)\\s+([^\\s]+)\\s+(?:AS\\s+)?([a-zA-Z_][a-zA-Z0-9_]*)");
        Matcher m = p.matcher(s);
        while (m.find()) {
            String obj = m.group(1);
            String ali = m.group(2);
            if (obj != null && obj.startsWith("(")) continue; // skip subqueries
            if (ali != null && !ali.isEmpty()) {
                aliases.add(ali.toLowerCase(Locale.ROOT));
            }
        }
        return aliases;
    }

    /** Remove aliases (e.g., u, e, gj_1) from collected table names */
    private static void filterOutAliasesAndGenerated(Set<String> tables, String sql) {
        Set<String> aliasSet = extractFromJoinAliases(sql); // lowercased
        if (aliasSet.isEmpty()) return;

        for (Iterator<String> it = tables.iterator(); it.hasNext(); ) {
            String name = it.next();
            String lower = name.toLowerCase(Locale.ROOT);

            // direct alias
            if (aliasSet.contains(lower)) {
                it.remove();
                continue;
            }
            // generated alias like u_1 / gj_2 → strip "_\\d+" and check base
            int us = lower.lastIndexOf('_');
            if (us > 0 && us < lower.length() - 1) {
                String base = lower.substring(0, us);
                String suffix = lower.substring(us + 1);
                if (suffix.matches("\\d+") && aliasSet.contains(base)) {
                    it.remove();
                }
            }
        }
    }

    /* =========================================================
     * NEW: Combined columns+tables API (single connection acquisition)
     * ========================================================= */

    /** Simple DTO for combined result (Java 8 compatible). */
    public static final class ColumnsAndTables {
        private final List<String> columns;
        private final List<String> tables;
        public ColumnsAndTables(List<String> columns, List<String> tables) {
            this.columns = columns == null ? Collections.emptyList() : Collections.unmodifiableList(new ArrayList<>(columns));
            this.tables  = tables  == null ? Collections.emptyList()  : Collections.unmodifiableList(new ArrayList<>(tables));
        }
        public List<String> getColumns() { return columns; }
        public List<String> getTables()  { return tables; }
        @Override public String toString() { return "ColumnsAndTables{" +
                "columns=" + columns +
                ", tables=" + tables +
                '}'; }
    }

    // --- DataSource (auto-release) overloads ---
    public static ColumnsAndTables extractColumnsAndTables(DataSource ds, String sql) throws SQLException {
        return extractColumnsAndTables(ds, sql, DEFAULT_TIMEOUT_SEC);
    }

    public static ColumnsAndTables extractColumnsAndTables(DataSource ds, String sql, int timeoutSec) throws SQLException {
        try (Connection conn = ds.getConnection()) {
            DbVendor vendor = detectVendor(conn);
            return extractColumnsAndTables(conn, sql, vendor, timeoutSec);
        }
    }

    // --- Spring-aware overloads (cooperate with @Transactional) ---
    public static ColumnsAndTables extractColumnsAndTablesUsingSpring(DataSource ds, String sql) throws SQLException {
        return extractColumnsAndTablesUsingSpring(ds, sql, DEFAULT_TIMEOUT_SEC);
    }

    public static ColumnsAndTables extractColumnsAndTablesUsingSpring(DataSource ds, String sql, int timeoutSec) throws SQLException {
        Connection conn = DataSourceUtils.getConnection(ds);
        try {
            DbVendor vendor = detectVendor(conn);
            return extractColumnsAndTables(conn, sql, vendor, timeoutSec);
        } finally {
            DataSourceUtils.releaseConnection(conn, ds);
        }
    }

    // --- Connection-based overloads (caller manages connection) ---
    public static ColumnsAndTables extractColumnsAndTables(Connection conn, String sql) throws SQLException {
        return extractColumnsAndTables(conn, sql, detectVendor(conn), DEFAULT_TIMEOUT_SEC);
    }

    public static ColumnsAndTables extractColumnsAndTables(Connection conn, String sql, DbVendor vendor, int timeoutSec) throws SQLException {
        List<String> cols = extractFieldAliasesByJDBC(conn, sql, vendor, timeoutSec);
        List<String> tabs = extractTableNamesByJDBC(conn, sql, vendor, timeoutSec);
        return new ColumnsAndTables(cols, tabs);
    }
}
