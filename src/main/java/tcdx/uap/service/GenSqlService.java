package tcdx.uap.service;

import cn.hutool.core.map.MapBuilder;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import tcdx.uap.service.entities.CompDataSourceField;
import tcdx.uap.service.entities.CompGrid;
import tcdx.uap.service.entities.Table;
import tcdx.uap.service.entities.TableCol;
import tcdx.uap.service.entities.gensql.ColumnInfo;
import tcdx.uap.service.entities.gensql.ForeignKeyInfo;
import tcdx.uap.service.entities.gensql.TableInfo;
import tcdx.uap.service.entities.gensql.TableJoinInfo;
import tcdx.uap.service.store.Modules;
import tcdx.uap.service.vo.TableColInfoResp;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
public class GenSqlService {

    @Autowired
    private DataSource dataSource;

    // 默认过滤的字段列表
    private Set<String> defaultExcludedFields = new HashSet<>(Arrays.asList(
            "poster_", "create_user_", "update_user_", "time_if_delete_", "edge_", "pri_tbl_", "pri_tbl_node_", "prev_node_", "finished_time_", "posted_time_", "update_time_"
    ));

    // 用户自定义过滤字段
    private Set<String> customExcludedFields = new HashSet<>();

    /**
     * 生成包含所有字段和外键LEFT JOIN的SQL查询
     */
    public String generateSelectWithLeftJoins(String tableName) throws SQLException {
        return generateSelectWithLeftJoins(tableName, true, true);
    }

    /**
     * 生成SQL查询（带过滤选项）
     */
    public String generateSelectWithLeftJoins(String tableName,
                                              boolean includeForeignKeys,
                                              boolean includeAllFields) throws SQLException {
        return generateSelectWithLeftJoins(tableName, includeForeignKeys, includeAllFields, null);
    }

    /**
     * 生成SQL查询（完整参数）
     */
    public String generateSelectWithLeftJoins(String tableName,
                                              boolean includeForeignKeys,
                                              boolean includeAllFields,
                                              Set<String> excludedFields) throws SQLException {

        // 使用单个连接
        try (Connection conn = dataSource.getConnection()) {
            DatabaseMetaData metaData = conn.getMetaData();

            // 获取主表信息
            TableInfo tableInfo = getTableInfoWithConnection(tableName, metaData);
            StringBuilder sql = new StringBuilder();

            // 使用LinkedHashMap保持表顺序，避免重复
            Map<String, TableJoinInfo> processedTables = new LinkedHashMap<>();
            String mainTableAlias = getTableAlias(tableInfo.getTableName());
            processedTables.put(tableInfo.getTableName(),
                    new TableJoinInfo(tableInfo.getTableName(), mainTableAlias, true));

            // 如果需要外键，递归收集所有需要JOIN的表
            if (includeForeignKeys) {
                collectAllJoinTables(tableInfo, processedTables, metaData); // 传入metaData
            }

            // 合并默认过滤字段和自定义过滤字段
            Set<String> finalExcludedFields = new HashSet<>(defaultExcludedFields);
            if (excludedFields != null) {
                finalExcludedFields.addAll(excludedFields);
            }
            finalExcludedFields.addAll(customExcludedFields);

            // SELECT 部分
            sql.append("SELECT \n");
            sql.append(generateSelectColumns(processedTables, includeAllFields, finalExcludedFields)); // 传入metaData
            sql.append("\nFROM ").append(tableInfo.getTableName()).append(" ").append(mainTableAlias);

            // 如果需要外键，生成LEFT JOIN部分
            if (includeForeignKeys) {
                sql.append(generateLeftJoins(processedTables)); // 传入metaData
            }
            // 添加常用LEFT JOIN条件
            sql.append(generateCommonLeftJoins(tableName));
            // 添加默认WHERE条件
            sql.append(generateDefaultWhereCondition(tableName));

            return sql.toString();
        }
    }

    /**
     * 生成SELECT列部分（带过滤）
     */
    private String generateSelectColumns(Map<String, TableJoinInfo> processedTables,
                                         boolean includeAllFields,
                                         Set<String> excludedFields) throws SQLException {
        List<String> columns = new ArrayList<>();

        for (TableJoinInfo tableJoin : processedTables.values()) {
            TableInfo tableInfo = getTableInfo(tableJoin.getTableName());
            String tableAlias = tableJoin.getAlias();

            // 如果是外键表且不需要所有字段，只选择id字段
            if (!tableJoin.isMainTable() && !includeAllFields) {
                // 只添加外键表的id字段
                for (ColumnInfo column : tableInfo.getColumns()) {
                    if ("id_".equals(column.getColumnName())) {
                        String renamedColumn = tableJoin.getTableName() + "_id";
                        columns.add(tableAlias + "." + column.getColumnName() + " AS " + renamedColumn);
                        break; // 只添加id字段
                    }
                }
            } else {
                // 添加所有字段（根据过滤规则）
                for (ColumnInfo column : tableInfo.getColumns()) {
                    String columnName = column.getColumnName();

                    // 跳过被排除的字段
                    if (excludedFields.contains(columnName)) {
                        continue;
                    }

                    String renamedColumn;

                    if (tableJoin.isMainTable()) {
                        // 主表字段重命名规则
                        if ("id_".equals(columnName)) {
                            renamedColumn = tableJoin.getTableName() + "_id";
                        } else {
                            renamedColumn = tableAlias + "_" + columnName;
                        }
                    } else {
                        // 外键表字段重命名规则
                        if ("id_".equals(columnName)) {
                            renamedColumn = tableJoin.getTableName() + "_id";
                        } else {
                            renamedColumn = tableAlias + "_" + columnName;
                        }
                    }

                    columns.add(tableAlias + "." + columnName + " AS " + renamedColumn);
                }
            }
        }

        return columns.isEmpty() ? "    *" : "    " + String.join(",\n    ", columns);
    }

    /**
     * 递归收集所有需要JOIN的表
     */
    private void collectAllJoinTables(TableInfo tableInfo, Map<String, TableJoinInfo> processedTables, DatabaseMetaData metaData) throws SQLException {
        for (ForeignKeyInfo fk : tableInfo.getForeignKeys()) {
            String refTableName = fk.getReferencedTable();

            // 如果表已经处理过，跳过避免重复
            if (processedTables.containsKey(refTableName)) {
                continue;
            }

            String tableAlias = getTableAlias(refTableName);
            processedTables.put(refTableName,
                    new TableJoinInfo(refTableName, tableAlias, false));

            // 递归处理外键表的外键
            TableInfo refTableInfo = getTableInfo(refTableName, metaData);
            collectAllJoinTables(refTableInfo, processedTables, metaData);
        }
    }

    /**
     * 获取表全信息（重构版本，使用单个连接）
     */
    public Map<String, TableInfo> getTableFullInfo(String tableName, boolean collectForeign) throws Exception {
        Map<String, TableInfo> tableFullInfo = new HashMap<>();

        // 使用单个连接处理所有表信息获取
        try (Connection conn = dataSource.getConnection()) {
            DatabaseMetaData metaData = conn.getMetaData();

            // 获取主表信息
            TableInfo mainTableInfo = getTableInfoWithConnection(tableName, metaData);
            tableFullInfo.put(tableName, mainTableInfo);

            if (collectForeign) {
                // 收集所有需要的外键表
                Set<String> allTableNames = collectAllForeignTableNames(mainTableInfo, metaData);

                // 批量获取所有外键表信息
                for (String foreignTableName : allTableNames) {
                    if (!tableFullInfo.containsKey(foreignTableName)) {
                        TableInfo foreignTableInfo = getTableInfoWithConnection(foreignTableName, metaData);
                        tableFullInfo.put(foreignTableName, foreignTableInfo);
                    }
                }
            }
        }

        return tableFullInfo;
    }

    /**
     * 获取模块生成的配置信息
     */
    public List<TableColInfoResp> getTableFullColumn(String tableName, boolean collectForeign) throws Exception {
        List<TableColInfoResp> result = new ArrayList<>();
        // 使用单个连接处理所有表信息获取
        try (Connection conn = dataSource.getConnection()) {
            DatabaseMetaData metaData = conn.getMetaData();
            ImmutablePair<List<TableColInfoResp>, TableInfo> tableColInfo = getSingleTableColInfo(tableName, metaData, true);
            result.addAll(tableColInfo.getLeft());
            if (collectForeign) {
                // 收集所有需要的外键表
                Set<String> allTableNames = collectAllForeignTableNames(tableColInfo.getRight(), metaData);

                // 批量获取所有外键表信息
                for (String foreignTableName : allTableNames) {
                    result.addAll(getSingleTableColInfo(foreignTableName, metaData, false).getLeft());
                }
            }
        }

        return result;
    }

    public Map<String, List<TableColInfoResp>> getTableFullColumn(String tableName) throws Exception {
        Map<String, List<TableColInfoResp>> result = new HashMap<>();
        // 收集主表的列
        List<TableColInfoResp> mainTableList = new ArrayList<>();
        Table mainTable = (Table) Modules.getInstance().get(tableName, false);
        for (TableCol tc: mainTable.cols) {
            if (tc.field.equals("id_")) {
                continue;
            }
            mainTableList.add(buildTableColInfoResp(tc, mainTable, true));
        }
        result.put("mainTable", mainTableList);
        // 收集外键表的列
        List<TableColInfoResp> priTableList = new ArrayList<>();
        for (String priTableName: mainTable.priTableIds) {
            Table priTable = (Table) Modules.getInstance().get(priTableName, false);
            for (TableCol tc: priTable.cols) {
                if (tc.field.contains("_id")) {
                    continue;
                }
                priTableList.add(buildTableColInfoResp(tc, priTable, false));
            }
        }
        result.put("priTable", priTableList);
        // 收集子表的列
        List<TableColInfoResp> subTableList = new ArrayList<>();
        for (String subTableName: mainTable.subTableIds) {
            Table subTable = (Table) Modules.getInstance().get(subTableName, false);
            for (TableCol tc: subTable.cols) {
                if (tc.field.equals("z_" + tableName + "_id")) {
                    continue;
                }
                subTableList.add(buildTableColInfoResp(tc, subTable, false));
            }
        }
        result.put("subTable", subTableList);
        return result;
    }

    private TableColInfoResp buildTableColInfoResp(TableCol tc, Table table, Boolean isMainTable) {
        TableColInfoResp tcInfo = new TableColInfoResp();
        tcInfo.setColumnName(tc.field);
        tcInfo.setDisplayName(tc.name);
        tcInfo.setDataType(tc.data_type);
        String renderType = inferRenderType(tc.data_type, tc.name);
        tcInfo.setRenderType(renderType);
        // 外键不在详情展示
        if (renderType.equals("foreign-key")) {
            tcInfo.setShowInDetail(false);
            tcInfo.setShowInTable(false);
        }
        String tableName = table.table_name.replace("z_", "");
        tcInfo.setTableName(tableName);
        tcInfo.setIsMainTable(isMainTable);
        tcInfo.setTableDisplayName(table.name);
        tcInfo.setColId(tc.id);
        return tcInfo;
    }

    private ImmutablePair<List<TableColInfoResp>, TableInfo> getSingleTableColInfo(String tableName, DatabaseMetaData metaData, Boolean isMainTable) throws Exception {
        List<TableColInfoResp> tableColInfoRespList = new ArrayList<>();
        if (tableName.contains("z_")) {
            tableName = tableName.replace("z_", "");
        }
        String proceedTableName = "z_" + tableName;
        // 获取表信息
        TableInfo mainTableInfo = getTableInfoWithConnection(proceedTableName, metaData);
        // 表格组件
        Table table = (Table) Modules.getInstance().get(tableName, true);
        List<TableCol> cols = table.cols;
        // 表格数据源字段
        Map<String, TableCol> colMap = new HashMap<>();//cols.stream().collect(Collectors.toMap(TableCol::getField, v -> v));
        for (ColumnInfo column : mainTableInfo.getColumns()) {
            TableCol tableCol = colMap.get(column.getColumnName());
            if (Objects.isNull(tableCol)) {
                continue;
            }
            if (column.getColumnName().contains("_id")) {
                continue;
            }
            TableColInfoResp tableColInfoResp = new TableColInfoResp();
            tableColInfoResp.setColumnName(column.getColumnName());
            tableColInfoResp.setDisplayName(tableCol.getName());
            tableColInfoResp.setDataType(column.getDataType());
            tableColInfoResp.setIsMainTable(isMainTable);
            tableColInfoResp.setTableName(tableName);
            // 默认设置 主表字段：显示、添加、编辑、详情;外联表字段：详情
            if (!isMainTable) {
                tableColInfoResp.setShowInTable(false);
                tableColInfoResp.setShowInAdd(false);
                tableColInfoResp.setShowInEdit(false);
            }
            tableColInfoResp.setRenderType(inferRenderType(column.getDataType(), column.getColumnName()));
            tableColInfoRespList.add(tableColInfoResp);
        }
        return ImmutablePair.of(tableColInfoRespList, mainTableInfo);
    }

    public String inferRenderType(String type, String name) {
        if (name.contains("时间") || name.contains("日期") || type.contains("datetime") || type.contains("timestamp")) return "datetime";
        if (name.contains("地点") || name.contains("地址")) return "area";
        if (name.contains("附件") || name.contains("图片") || name.contains("照片") || name.contains("文件")) return "file";
        if (name.contains("编号")) return "uuid";
        if (name.contains("外联主表")) return "foreign-key";
        if (type.contains("int") || type.contains("decimal") || type.contains("numeric")) return "number";
        if (name.contains("审批状态") || name.contains("审核状态") || name.contains("是否")) return "single-select";

        return "input";
    }

    /**
     * 收集所有外键关联的表名
     */
    private Set<String> collectAllForeignTableNames(TableInfo tableInfo, DatabaseMetaData metaData) throws SQLException {
        Set<String> allTableNames = new HashSet<>();
        Deque<TableInfo> queue = new LinkedList<>();
        queue.add(tableInfo);

        while (!queue.isEmpty()) {
            TableInfo currentTable = queue.poll();

            for (ForeignKeyInfo fk : currentTable.getForeignKeys()) {
                String refTableName = fk.getReferencedTable();

                if (!allTableNames.contains(refTableName)) {
                    allTableNames.add(refTableName);
                    // 获取外键表信息并继续遍历
                    TableInfo refTableInfo = getTableInfoWithConnection(refTableName, metaData);
                    queue.add(refTableInfo);
                }
            }
        }

        return allTableNames;
    }

    /**
     * 使用现有连接获取表信息（不创建新连接）
     */
    private TableInfo getTableInfoWithConnection(String tableName, DatabaseMetaData metaData) throws SQLException {
        // 先检查缓存
        if (tableInfoCache.containsKey(tableName)) {
            return tableInfoCache.get(tableName);
        }

        TableInfo tableInfo = new TableInfo();
        tableInfo.setTableName(tableName);

        // 获取列信息
        List<ColumnInfo> columns = getColumns(metaData, tableName);
        tableInfo.setColumns(columns);

        // 获取外键信息
        List<ForeignKeyInfo> foreignKeys = getForeignKeys(metaData, tableName);
        tableInfo.setForeignKeys(foreignKeys);

        // 放入缓存
        tableInfoCache.put(tableName, tableInfo);
        return tableInfo;
    }
    /**
     * 生成LEFT JOIN部分
     */
    private String generateLeftJoins(Map<String, TableJoinInfo> processedTables) throws SQLException {
        StringBuilder joins = new StringBuilder();

        // 跳过主表，只处理外键表
        for (TableJoinInfo tableJoin : processedTables.values()) {
            if (tableJoin.isMainTable()) {
                continue;
            }

            // 为每个外键表找到对应的父表和关联条件
            String joinCondition = findJoinCondition(processedTables, tableJoin.getTableName());
            if (joinCondition != null) {
                joins.append("\nLEFT JOIN ")
                        .append(tableJoin.getTableName()).append(" ").append(tableJoin.getAlias())
                        .append(" ON ").append(joinCondition);
            }
        }

        return joins.toString();
    }

    /**
     * 添加常用表LEFT JOIN
     */
    private String generateCommonLeftJoins(String tableName) {
        tableName = tableName.replace("z_table", "t");
        return  "\nLEFT JOIN v_flow_node n ON n.id = " +
                tableName + ".node_" +
                "\nLEFT JOIN v_flow_edge e ON e.id = " +
                tableName + ".edge_" +
                "\nLEFT JOIN v_user r ON r.id = " +
                tableName + ".receiver_" +
                "\nLEFT JOIN v_user p ON p.id = " +
                tableName + ".poster_" +
                "\nLEFT JOIN v_user c ON c.id = " +
                tableName + ".create_user_" +
                "\nLEFT JOIN v_user u ON u.id = " +
                tableName + ".update_user_";
    }

    private String generateDefaultWhereCondition(String tableName) {
        tableName = tableName.replace("z_table", "t");
        return "\nWHERE " + tableName + ".time_if_delete_ IS NULL";
    }

    /**
     * 查找JOIN条件
     */
    private String findJoinCondition(Map<String, TableJoinInfo> processedTables, String tableName) throws SQLException {
        // 在所有已处理的表中查找哪个表引用了当前表
        for (TableJoinInfo potentialParent : processedTables.values()) {
            TableInfo parentTableInfo = getTableInfo(potentialParent.getTableName());

            for (ForeignKeyInfo fk : parentTableInfo.getForeignKeys()) {
                if (tableName.equals(fk.getReferencedTable())) {
                    return potentialParent.getAlias() + "." + fk.getColumnName() +
                            " = " + getTableAlias(tableName) + "." + fk.getReferencedColumn();
                }
            }
        }

        return null;
    }

    /**
     * 生成表别名
     */
    private String getTableAlias(String tableName) {
        // 提取表名中的数字部分
        String numbers = tableName.replaceAll("[^0-9]", "");
        if (!numbers.isEmpty()) {
            return "t" + numbers;
        } else {
            // 如果没有数字，使用原表名
            return tableName;
        }
    }

    /**
     * 获取表信息（缓存优化）
     */
    private Map<String, TableInfo> tableInfoCache = new ConcurrentHashMap<>();

    public TableInfo getTableInfo(String tableName) throws SQLException {
        if (tableInfoCache.containsKey(tableName)) {
            return tableInfoCache.get(tableName);
        }

        TableInfo tableInfo = new TableInfo();
        tableInfo.setTableName(tableName);

        try (Connection conn = dataSource.getConnection()) {
            DatabaseMetaData metaData = conn.getMetaData();

            // 获取列信息
            List<ColumnInfo> columns = getColumns(metaData, tableName);
            tableInfo.setColumns(columns);

            // 获取外键信息
            List<ForeignKeyInfo> foreignKeys = getForeignKeys(metaData, tableName);
            tableInfo.setForeignKeys(foreignKeys);
        }

        tableInfoCache.put(tableName, tableInfo);
        return tableInfo;
    }

    public TableInfo getTableInfo(String tableName, DatabaseMetaData metaData) throws SQLException {
        if (tableInfoCache.containsKey(tableName)) {
            return tableInfoCache.get(tableName);
        }

        TableInfo tableInfo = new TableInfo();
        tableInfo.setTableName(tableName);

        // 获取列信息
        List<ColumnInfo> columns = getColumns(metaData, tableName);
        tableInfo.setColumns(columns);

        // 获取外键信息
        List<ForeignKeyInfo> foreignKeys = getForeignKeys(metaData, tableName);
        tableInfo.setForeignKeys(foreignKeys);
        tableInfoCache.put(tableName, tableInfo);
        return tableInfo;
    }

    /**
     * 改进的外键推断方法
     */
    private ForeignKeyInfo inferForeignKeyFromColumnName(String columnName, String currentTable) {
        // 常见的外键字段名模式
        String[][] patterns = {
                {"z_table(\\d+)_id$", "z_table$1", "id_"},     // z_table93_id -> z_table93.id_
        };

        for (String[] pattern : patterns) {
            if (columnName.matches(pattern[0])) {
                String refTable = columnName.replaceAll(pattern[0], pattern[1]);
                ForeignKeyInfo fk = new ForeignKeyInfo();
                fk.setColumnName(columnName);
                fk.setReferencedTable(refTable);
                fk.setReferencedColumn(pattern[2]);
                return fk;
            }
        }

        return null;
    }

    /**
     * 获取外键信息
     */
    private List<ForeignKeyInfo> getForeignKeys(DatabaseMetaData metaData, String tableName)
            throws SQLException {
        List<ForeignKeyInfo> foreignKeys = new ArrayList<>();

        // 先获取当前表的所有列
        List<String> columns = new ArrayList<>();
        try (ResultSet rs = metaData.getColumns(null, null, tableName, null)) {
            while (rs.next()) {
                columns.add(rs.getString("COLUMN_NAME"));
            }
        }

        // 通过字段名模式推断外键关系
        for (String column : columns) {
            ForeignKeyInfo fk = inferForeignKeyFromColumnName(column, tableName);
            if (fk != null) {
                // 验证被引用的表是否存在
                if (tableExists(metaData, fk.getReferencedTable())) {
                    foreignKeys.add(fk);
                }
            }
        }

        return foreignKeys;
    }

    /**
     * 检查表是否存在
     */
    private boolean tableExists(DatabaseMetaData metaData, String tableName) throws SQLException {
        try (ResultSet rs = metaData.getTables(null, null, tableName, new String[]{"TABLE"})) {
            return rs.next();
        }
    }

    // 其他辅助方法
    private List<ColumnInfo> getColumns(DatabaseMetaData metaData, String tableName)
            throws SQLException {
        List<ColumnInfo> columns = new ArrayList<>();

        try (ResultSet rs = metaData.getColumns(null, null, tableName, null)) {
            while (rs.next()) {
                ColumnInfo column = new ColumnInfo();
                column.setColumnName(rs.getString("COLUMN_NAME"));
                column.setDataType(rs.getString("TYPE_NAME"));
                columns.add(column);
            }
        }

        return columns;
    }

}
