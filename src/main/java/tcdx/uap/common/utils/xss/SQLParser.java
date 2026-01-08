package tcdx.uap.common.utils.xss;

public class SQLParser {
    /**
     * 判断SQL主查询中是否包含WHERE子句
     */
    public static boolean hasWhereClauseInMainQuery(String sql) {
        if (sql == null || sql.trim().isEmpty()) {
            return false;
        }

        // 1. 预处理SQL：移除注释，统一大小写
        String processedSQL = preprocessSQL(sql);

        // 2. 提取主查询部分（跳过WITH子句）
        String mainQuery = extractMainQuery(processedSQL);

        // 3. 检查主查询中是否存在WHERE子句
        return containsWhereClause(mainQuery);
    }

    private static String preprocessSQL(String sql) {
        // 移除单行注释
        sql = sql.replaceAll("--.*?(\n|$)", " ");
        // 移除多行注释
        sql = sql.replaceAll("/\\*.*?\\*/", " ");
        // 统一为小写并移除多余空格
        return sql.toLowerCase().replaceAll("\\s+", " ").trim();
    }

    private static String extractMainQuery(String sql) {
        // 跳过WITH子句，定位主查询的SELECT关键字
        int withIndex = sql.indexOf("with ");
        if (withIndex != 0) {
            return sql; // 没有WITH子句，整个SQL就是主查询
        }

        // 查找主查询的SELECT（跳过WITH中的所有SELECT）
        int mainSelectIndex = findMainSelectIndex(sql);
        if (mainSelectIndex == -1) {
            return sql; // 未找到SELECT，返回原SQL
        }

        return sql.substring(mainSelectIndex);
    }

    private static int findMainSelectIndex(String sql) {
        int selectIndex = -1;
        int withIndex = sql.indexOf("with ");

        if (withIndex != 0) {
            return sql.indexOf("select ");
        }

        // 找到WITH子句结束后的第一个SELECT
        int pos = withIndex + 5; // "with "的长度

        while (true) {
            // 查找下一个SELECT
            selectIndex = sql.indexOf("select ", pos);
            if (selectIndex == -1) {
                break;
            }

            // 检查是否在括号内（子查询）
            if (!isInsideParentheses(sql, selectIndex)) {
                return selectIndex;
            }

            pos = selectIndex + 7; // "select "的长度
        }

        return -1;
    }

    private static boolean isInsideParentheses(String sql, int index) {
        int parenCount = 0;

        for (int i = 0; i < index; i++) {
            char c = sql.charAt(i);

            if (c == '(') {
                parenCount++;
            } else if (c == ')') {
                if (parenCount > 0) {
                    parenCount--;
                }
            }
        }

        return parenCount > 0;
    }

    private static boolean containsWhereClause(String sql) {
        int whereIndex = sql.indexOf("where ");
        if (whereIndex == -1) {
            return false;
        }

        // 确保WHERE不在引号内
        if (isInsideQuotes(sql, whereIndex)) {
            return false;
        }

        // 确保WHERE是主查询的一部分，而不是子查询
        return !isInsideSubquery(sql, whereIndex);
    }

    private static boolean isInsideQuotes(String sql, int index) {
        boolean inSingleQuote = false;
        boolean inDoubleQuote = false;

        for (int i = 0; i < index; i++) {
            char c = sql.charAt(i);

            // 处理转义字符
            if (c == '\\') {
                i++; // 跳过转义的字符
                continue;
            }

            // 检查引号
            if (c == '\'' && !inDoubleQuote) {
                inSingleQuote = !inSingleQuote;
            } else if (c == '"' && !inSingleQuote) {
                inDoubleQuote = !inDoubleQuote;
            }
        }

        return inSingleQuote || inDoubleQuote;
    }

    private static boolean isInsideSubquery(String sql, int whereIndex) {
        // 简化版：检查WHERE前是否有未闭合的括号
        int parenCount = 0;

        for (int i = 0; i < whereIndex; i++) {
            char c = sql.charAt(i);

            if (c == '(') {
                parenCount++;
            } else if (c == ')') {
                if (parenCount > 0) {
                    parenCount--;
                }
            }
        }

        return parenCount > 0;
    }

    // 测试示例
    public static void main(String[] args) {
        String sql = "WITH handle_ids AS (SELECT id_ FROM table WHERE col = 1) " +
                "SELECT * FROM main_table JOIN handle_ids ON main_table.id = handle_ids.id";

        boolean hasWhere = hasWhereClauseInMainQuery(sql);
        System.out.println("主查询是否包含WHERE子句: " + hasWhere);
    }
}