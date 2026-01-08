package tcdx.uap.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import tcdx.uap.common.utils.Lutils;
import tcdx.uap.common.utils.MapUtils;
import tcdx.uap.mapper.ServiceConfigMapper;
import tcdx.uap.mapper.BusinessMapper;
import tcdx.uap.service.entities.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 参数配置 服务层实现
 *
 * @author ruoyi
 */
@Service
public class TableCRUDService {

    @Autowired
    private ServiceConfigMapper serviceConfigMapper;

    @Autowired
    BaseDBService baseDBService;
    @Autowired
    private BusinessMapper businessMapper;

    public List<Integer> gen_ids(int count) {
        List<Map> l = baseDBService.selectSql("select nextval('tcdx_datatable_id_seq') id,generate_series(1, " + count + ") num");
        return l.stream().map(o -> Integer.parseInt(o.get("id").toString())).collect(Collectors.toList());
    }

    public void UpdateTableColumns(ColumnRuleOperationStore columnRuleOperationStore, UserAction action){
        //处理下级数据
        while(true) {
            String table_name = columnRuleOperationStore.getNextTableName();
            if (table_name == null) {
                break;
            }
            //本级表
            int selfColumnRuleSize = 0;
            //自身表有没有额外追加字段，如果追加字段就要触发横向拓展
            while (selfColumnRuleSize < columnRuleOperationStore.getTableColumnSize(table_name)) {
                //判断本级表格，有没有新增规则
                selfColumnRuleSize = columnRuleOperationStore.getTableColumnSize(table_name);
                /**
                 * 一些字段由不同的表触发，不同的表触发的字段，必然独立，不关联
                 * */
                /** 根据变动的列，找到这些列触发变动的字段 -------针对本级表*/
                // updateColumns是本次表单中变动的列
                List<String> updateColumns = columnRuleOperationStore.getOperationList(table_name).stream().map(o -> o.column).collect(Collectors.toList());
                /** 根据变动的列，取触发自身表其他字段的列 ----TableColumnRelation，保存着变动列依赖的字段 */
                List<TableColumnRelation> selfDependRelation = TableColumnRelationList.getInstance(serviceConfigMapper).getTriggerThisTableRelation(table_name, updateColumns);
                // 先锁定依赖表的ids，根据行信息获取上级表的待更新的ids
                for (TableColumnRelation relation : selfDependRelation) {
                    // 中止条件，如果该列已出现，则跳过。
                    if (updateColumns.contains(relation.update_column))
                        continue;
                    // 创建新规则
                    ColumnRuleOperation thisColumnOperation = new ColumnRuleOperation(
                            relation.table_name,
                            relation.update_column,
                            relation.update_type, null, relation, relation.depend_table_name);
                    //合并所有的变动的列的ids,先锁定依赖表的行ids，通过该条件的match字段，
                    addDependIds_FromDependColumnOperations(thisColumnOperation,
                            columnRuleOperationStore.getOperationList(thisColumnOperation.dependTableName));
                    /** 获取inIds，即匹配字段旧值的条件 */
                    addInIds_ByDependMatchColValues(thisColumnOperation);
                    columnRuleOperationStore.addTableColumnRule(thisColumnOperation.tableName, thisColumnOperation);
                }
            }
            //触发上级的操作
            List<String> updateColumns = columnRuleOperationStore.getOperationList(table_name).stream().map(o -> o.column).collect(Collectors.toList());
            List<TableColumnRelation> upperDependRelation = TableColumnRelationList.getInstance(serviceConfigMapper).getTriggerUpperTableRelation(table_name, updateColumns);
            // 根据变动的列，取触发上级表触发更新的列 ----
            // 先锁定依赖表的ids，根据行信息获取上级表的待更新的ids
            for (TableColumnRelation upperRelation : upperDependRelation) {
                // 创建新规则
                ColumnRuleOperation upperTableColumnOperation = new ColumnRuleOperation(
                        upperRelation.table_name,
                        upperRelation.update_column,
                        upperRelation.update_type, null, upperRelation, upperRelation.depend_table_name);
                /** 获取依赖表的Ids，锁定这些ids，用于后期这些ids对应的行，修改前后的值，获取更新表的ids，即依赖更新的范围 */
                addDependIds_FromDependColumnOperations(upperTableColumnOperation,
                        columnRuleOperationStore.getOperationList(upperTableColumnOperation.dependTableName));
                /** 获取Upper表的inIds，此时匹配的字段值修改前的状态 */
                addInIds_ByDependMatchColValues(upperTableColumnOperation);
                //存到字典
                columnRuleOperationStore.addTableColumnRule(upperTableColumnOperation.tableName, upperTableColumnOperation);
            }
            //更新当前表数据
            for (ColumnRuleOperation m : columnRuleOperationStore.getOperationList(table_name)) {
                if (m.updateType.equals("user-form")) {
                    int rs = baseDBService.updateIn(m.tableName, Lutils.genMap(m.column, m.value), "id_", m.inIds);
                } else if (m.updateType.equals("this-table-express")) {

                } else if (m.updateType.equals("depend-col-new-value")) {
                    int rs = serviceConfigMapper.update_by_depend_col_new_value(m.getMybatisMap());
                } else if (m.updateType.equals("depend-col-count")) {
                    int rs = serviceConfigMapper.update_by_depend_col_count(m.getMybatisMap());
                } else if (m.updateType.equals("depend-col-count-distinct")) {

                }
                m.isColumnUpdated = true;
                //更新完，补充本表格依赖数据
                for (ColumnRuleOperation tmpThisRule : columnRuleOperationStore.getOperationList(table_name)) {
                    if (tmpThisRule.isWhereSupplementByNewValue)
                        continue;
                    //检查关联的列，是否已经更新，如果都更新了，则根据新值，补充ids
                    int relColUpdatedCount = 0;
                    int relColCount = 0;
                    int relFormColCount = 0;
                    for (ColumnRuleOperation tmpRelRule : columnRuleOperationStore.getOperationList(table_name)) {
                        //是关联的值
                        if (tmpThisRule.updateRelation.getDependFields().contains(tmpRelRule.column)) {
                            relColCount++;
                            //如果关联的列还未更新，则结束
                            if (tmpRelRule.isColumnUpdated) {
                                relColUpdatedCount++;
                            }
                            if(tmpRelRule.updateType.equals("user-form")) {
                                relFormColCount ++;
                            }
                        }
                    }
                    //如果该列未补充过,且关联的字段都更新了值,则根据depend表新值,获取当前表的ids
                    if (relColCount == relColUpdatedCount && relColCount > 0) {
                        /** 获取依赖表的Ids，锁定这些ids，用于后期这些ids对应的行，修改前后的值，获取更新表的ids，即依赖更新的范围 */
                        //存在非表单类更新的列，这些列的更新范围，会根据依赖值修改前后变化，所以依赖这些列的上级字段的dependids也会变化，导致上级表的更新范围变化。
                        if(relFormColCount<relColCount)
                            addDependIds_FromDependColumnOperations(tmpThisRule,
                                    columnRuleOperationStore.getOperationList(tmpThisRule.dependTableName));
                        /** 根据Depend表锁定的ids,在Depend表取不同的关联的字段值组合,在新表查询这个组合,获得当前表关联ids行 */
                        addInIds_ByDependMatchColValues(tmpThisRule);
                        tmpThisRule.isWhereSupplementByNewValue = true;
                    }
                }
            }
            //更新UpperTable的更新范围，即inIds
            // 根据变动的列，取触发上级表触发更新的列 ----
            // 先锁定依赖表的ids，根据行信息获取上级表的待更新的ids
            for (TableColumnRelation upperRelation : upperDependRelation) {
                // 创建新规则
                ColumnRuleOperation upperTableColumnOperation = columnRuleOperationStore.getColumnOperation(upperRelation.table_name, upperRelation.update_column);
                /** 获取依赖表的Ids，锁定这些ids，用于后期这些ids对应的行，修改前后的值，获取更新表的ids，即依赖更新的范围 */
                addDependIds_FromDependColumnOperations(upperTableColumnOperation, columnRuleOperationStore.getOperationList(upperTableColumnOperation.dependTableName));
                /** 获取Upper表的inIds，此时匹配的字段值修改前的状态 */
                addInIds_ByDependMatchColValues(upperTableColumnOperation);
            }
            //本级处理好后，删除本级数据 ???
            columnRuleOperationStore.removeTableOperations(table_name);
        }
    }

    //字段关联关系提交  form_data表示提交的数据
    //可能提交多个表单
    public void updateOtherColRelData(String tableName, Map form, List<Integer> idList) {
        List<Map> updateColRelAndDataList = new ArrayList<>();
        int table_id = Integer.parseInt( tableName.substring(7) );
        //遍历form 找到本表单提交数据，查询该表每个字段是否有关联关系
        List<String> formColList = new ArrayList<>(form.keySet());
        //查找数据更新前的外键ID
        List<Map> data1 = baseDBService.selectIn(tableName, "id_",idList);
        //更新数据
        baseDBService.updateIn(tableName,form,"id_",idList);
        //查找数据更新后的外键ID
        //
        List<Map> data2 = baseDBService.selectIn(tableName, "id_",idList);
        List data2_idList = Lutils.getColumnValueList(data2, "id_");
        idList.addAll(data2_idList);
        //查找是否有关联本表的关联关系
//        List<Map> selfColRelList = baseDBService.selectByCauses("v_table_col_rel",
//                Lutils.genMap("tp","a","cas",Lutils.genList(
//                Lutils.genMap("tp","eq","col","table_name","val",tableName),
//                Lutils.genMap("tp","eq","col","depend_table_name","val",tableName),
//                Lutils.genMap("tp","in","col","depend_table_column","vals",formColList)
//                )),null);
        List<Map> selfColRelList = baseDBService.selectByCauses("v_table_col",Lutils.genMap("tp","a","cas",Lutils.genList(
                Lutils.genMap("tp","eq","col","table_id","val",table_id),
                Lutils.genMap("tp","eq","col","depend_table_id","val",table_id),
                Lutils.genMap("tp","in","col","depend_column","vals",formColList)
        )),null);
        //新增关联关系处理项
        List<Map> selfRelList = new ArrayList<>();
        for (int i = 0; i < selfColRelList.size(); i++) {
            Map selfItem = selfColRelList.get(i);
            String update_column = selfItem.get("field").toString();
            int depend_col_id = Integer.parseInt(selfItem.get("depend_col_id").toString());
            Map map1 = baseDBService.selectEq("v_table_col", Lutils.genMap("id", depend_col_id)).get(0);
            String depend_table_column = map1.get("field").toString();
            String update_type = selfItem.get("depend_type").toString();
            formColList.add(depend_table_column);
            Map map = Lutils.genMap("update_column", update_column,
                    "depend_table_column", depend_table_column,"update_type",update_type);
            selfRelList.add(map);
        }
        if(selfRelList.size()>0){
            updateColRelAndDataList.add(Lutils.genMap("relationList",selfRelList,"idList",idList,"dependTableRowIdList",idList,
                    "table_name",tableName,"depend_table_name",tableName));
        }

        //查找是否有关联其他表的关联关系
        List<Map> otherColRelList = baseDBService.selectByCauses("v_table_col",
                Lutils.genMap("tp","a","cas",Lutils.genList(
                        Lutils.genMap("tp","neq","col","table_id","val",table_id),
                        Lutils.genMap("tp","eq","col","depend_table_id","val",table_id),
                        Lutils.genMap("tp","in","col","depend_column","vals",formColList)
                )),null);
        //先处理查出来的关联关系  把数据存到updateColRelAndDataList中
        Map<String, TableAncColumnDataEntity> result = new HashMap<>();
        List<Map> otherTableList = new ArrayList<>();
        for (int i = 0; i < otherColRelList.size(); i++) {
            Map otherItem = otherColRelList.get(i);
            String table_name = "z_table"+otherItem.get("table_id").toString();
            String depend_table_name = "z_table"+otherItem.get("depend_table_id").toString();
            String update_column = otherItem.get("field").toString();
            int depend_col_id = Integer.parseInt(otherItem.get("depend_col_id").toString());
            Map map1 = baseDBService.selectEq("v_table_col", Lutils.genMap("id", depend_col_id)).get(0);
            String depend_table_column = map1.get("field").toString();
            String update_type = otherItem.get("depend_type").toString();
            List<Integer> updateTable_idList = Lutils.getColumnValueList(data1, table_name + "_id");
            updateColRelAndDataList.add(Lutils.genMap("relationList",
                    Lutils.genList(Lutils.genMap("update_column", update_column,
                            "depend_table_column", depend_table_column,"update_type",update_type)),
                    "idList",updateTable_idList,"dependTableRowIdList",idList, "table_name",table_name,"depend_table_name",depend_table_name));
            if (result.containsKey(table_name)) {
                result.get(table_name).getUpdate_column_list().add(update_column);
            } else {
                // 如果 result 中没有这个 table_name，则创建一个新的列表并添加 update_column
                TableAncColumnDataEntity item = new TableAncColumnDataEntity(table_name,Lutils.genList(update_column),updateTable_idList);
                result.put(table_name,item);
            }
        }
        //处理完后，再根据延伸表去循环找延伸表的关联关系
        Iterator<Map.Entry<String, TableAncColumnDataEntity>> iterator = result.entrySet().iterator();
        while (iterator.hasNext()) {
            // 获取当前元素
            Map.Entry<String, TableAncColumnDataEntity> entry = iterator.next();
            String table_name = entry.getKey();
            TableAncColumnDataEntity tableInfo = entry.getValue();
            int table_id_item = Integer.parseInt(table_name.substring(7));
            List<Map> list = dealOtherTableRelList(table_id_item, tableInfo);
            updateColRelAndDataList.addAll(list);
            // 处理完成后移除元素
            iterator.remove();
            // 如果没有元素了，退出循环
            if (!iterator.hasNext()) {
                break;
            }
        }
        //处理数据
        System.out.println(updateColRelAndDataList+"232323232323");
        for (int i = 0; i < updateColRelAndDataList.size(); i++) {
            Map map = updateColRelAndDataList.get(i);
            List relationList = (List) map.get("relationList");
            List id_list = (List) map.get("idList");
            List rel_id_list = (List) map.get("dependTableRowIdList");
            Object table_name = map.get("table_name");
            Object depend_table_name = map.get("depend_table_name");
//           "relationList", Lutils.genList(Lutils.genMap("update_column", update_column,
//                   "depend_table_column", depend_table_column,"update_type",update_type)),
//                   "idList",updateTable_idList,"dependTableRowIdList,idList, "table_name",table_name,"depend_table_name",depend_table_name
            for (int j = 0; j < relationList.size(); j++) {
                Map relationItem = (Map) relationList.get(j);
                String update_type = relationItem.get("update_type").toString();
                Map m = Lutils.genMap("relation", relationItem, "table_name", table_name,
                        "depend_table_name", depend_table_name,
                        "id_list", id_list, "dependTableRowIdList", rel_id_list);
                if(update_type.equals("count")){
                    int count = serviceConfigMapper.update_by_depend_col_counts(m);
                }else if(update_type.equals("distinct")){
                    int count = serviceConfigMapper.update_by_depend_col_distinct(m);
                }else if(update_type.equals("new-record")){
                    int count = serviceConfigMapper.update_by_depend_col_newvalue(m);
                }else if(update_type.equals("defined_sql")){

                }else if(update_type.equals("sum")){
                    int count = serviceConfigMapper.update_by_depend_col_sum(m);
                }
            }


        }

    }

    //递归处理表的依赖关系
    public List<Map> dealOtherTableRelList(Integer depend_table_id,TableAncColumnDataEntity tableInfo){
        List<Map> updateColRelAndDataList = new ArrayList<>();
        //查找数据外键ID
        List<Map> data1 = baseDBService.selectIn("z_table"+depend_table_id, "id_",
                tableInfo.getIdList());
        //查找是否有关联本表的关联关系
        List<Map> selfColRelList = baseDBService.selectByCauses("v_table_col",
                Lutils.genMap("tp","a","cas",Lutils.genList(
                        Lutils.genMap("tp","eq","col","table_id","val",depend_table_id),
                        Lutils.genMap("tp","eq","col","depend_table_id","val",depend_table_id),
                        Lutils.genMap("tp","in","col","depend_column","vals",tableInfo.getUpdate_column_list())
                )),null);
        //新增关联关系处理项
        List<Map> selfRelList = new ArrayList<>();
        for (int i = 0; i < selfColRelList.size(); i++) {
            Map selfItem = selfColRelList.get(i);
            String update_column = selfItem.get("field").toString();
            String depend_table_column = selfItem.get("depend_column").toString();
            String update_type = selfItem.get("depend_type").toString();
            tableInfo.getUpdate_column_list().add(depend_table_column);
            Map map = Lutils.genMap("update_column", update_column,
                    "depend_table_column", depend_table_column,"update_type",update_type);
            selfRelList.add(map);
        }
        if(selfRelList.size()>0){
            updateColRelAndDataList.add(Lutils.genMap("relationList",selfRelList,"idList",tableInfo.getIdList(),
                    "dependTableRowIdList",tableInfo.getIdList(),
                    "table_name","z_table"+depend_table_id,"depend_table_name","z_table"+depend_table_id));
        }

        //查找是否有关联其他表的关联关系
        List<Map> otherColRelList = baseDBService.selectByCauses("v_table_col",
                Lutils.genMap("tp","a","cas",Lutils.genList(
                        Lutils.genMap("tp","neq","col","table_id","val",depend_table_id),
                        Lutils.genMap("tp","eq","col","depend_table_id","val",depend_table_id),
                        Lutils.genMap("tp","in","col","depend_column","vals", tableInfo.getUpdate_column_list())
                )),null);
        //先处理查出来的关联关系  把数据存到updateColRelAndDataList中
        Map<String, TableAncColumnDataEntity> result = new HashMap<>();
        for (int i = 0; i < otherColRelList.size(); i++) {
            Map otherItem = otherColRelList.get(i);
            String table_name = "z_table"+otherItem.get("table_id").toString();
            String depend_table_name1 = "z_table"+otherItem.get("depend_table_id").toString();
            String update_column = otherItem.get("field").toString();
            String depend_table_column = otherItem.get("depend_column").toString();
            String update_type = otherItem.get("depend_type").toString();
            List updateTable_idList = Lutils.getColumnValueList(data1, table_name + "_id");
            updateColRelAndDataList.add(Lutils.genMap("relationList",
                    Lutils.genList(Lutils.genMap("update_column", update_column,
                            "depend_table_column", depend_table_column,"update_type",update_type)),
                    "idList",tableInfo.getIdList(),"dependTableRowIdList",updateTable_idList, "table_name",table_name,"depend_table_name",depend_table_name1));
            if (result.containsKey(table_name)) {
                result.get(table_name).getUpdate_column_list().add(update_column);
            } else {
                // 如果 result 中没有这个 table_name，则创建一个新的列表并添加 update_column
                TableAncColumnDataEntity item = new TableAncColumnDataEntity(table_name,Lutils.genList(update_column),updateTable_idList);
                result.put(table_name,item);
            }
        }
        //处理完后，再根据延伸表去循环找延伸表的关联关系
        Iterator<Map.Entry<String, TableAncColumnDataEntity>> iterator = result.entrySet().iterator();
        while (iterator.hasNext()) {
            // 获取当前元素
            Map.Entry<String, TableAncColumnDataEntity> entry = iterator.next();
            String table_name = entry.getKey();
            Integer table_id = Integer.parseInt(table_name.substring(7));
            TableAncColumnDataEntity tableInfo1 = entry.getValue();
            List<Map> list = dealOtherTableRelList(table_id, tableInfo1);
            updateColRelAndDataList.addAll(list);
            // 处理完成后移除元素
            iterator.remove();
            // 如果没有元素了，退出循环
            if (!iterator.hasNext()) {
                break;
            }
        }
        return updateColRelAndDataList;
    }

    //更新字段关联关系
    public void UpdateTableColRel(Map cmap){
        Map map = (Map)cmap.get("updateMap");
        Map updateMap = new HashMap();
        List<Map> tbl_data = baseDBService.selectEq("v_table", MapUtils.G("id", map.get("table_id")));
        String tn = tbl_data.get(0).get("table_name").toString();
        updateMap.put("table_name",tn);
        updateMap.put("update_column",map.get("field"));
        updateMap.put("update_type",map.get("update_rule"));
        List<Map> tbl_depend_data = baseDBService.selectEq("v_table", MapUtils.G("id", map.get("update_from_datatable_id")));
        String depend_tn = tbl_depend_data.get(0).get("table_name").toString();
        updateMap.put("depend_table_name",depend_tn);
        updateMap.put("depend_table_column",map.get("update_from_datatable_column"));
        if(map.containsKey("match_column1")){
            updateMap.put("match_column1",map.get("match_column1"));
            updateMap.put("depend_table_match_column1",map.get("depend_table_match_column1"));
        }
        if(map.containsKey("match_column2")){
            updateMap.put("match_column2",map.get("match_column2"));
            updateMap.put("depend_table_match_column2",map.get("depend_table_match_column2"));
        }
        if(map.containsKey("match_column3")){
            updateMap.put("match_column3",map.get("match_column3"));
            updateMap.put("depend_table_match_column3",map.get("depend_table_match_column3"));
        }
        List<Map> list = baseDBService.selectEq("v_table_col_rel", Lutils.genMap("table_name", tn, "update_column", map.get("field")));
        if(list.size()>0){
            Object id = list.get(0).get("id");
            baseDBService.updateEq("v_table_col_rel",updateMap,Lutils.genMap("id",id));
        }else{
            baseDBService.insertMap("v_table_col_rel",updateMap);
        }
        Map itemMap = map;
        itemMap.remove("match_column1");
        itemMap.remove("match_column2");
        itemMap.remove("match_column3");
        itemMap.remove("update_from_datatable_column");
        itemMap.remove("update_from_datatable_id");
        itemMap.remove("update_rule");
        itemMap.remove("depend_table_match_column1");
        itemMap.remove("depend_table_match_column2");
        itemMap.remove("depend_table_match_column3");
        baseDBService.updateEq("v_table_col",itemMap,MapUtils.G("id", cmap.get("id")));
    }


    public void data_mode_delete(Map userInfo, List ids, String tn){
        //更新time_if_delete数据
        serviceConfigMapper.set_delete_time(MapUtils.G("tn", tn, "ids", ids,
                "action_type_", "data-delete",
                "update_user_", userInfo.get("user_name"),
                "update_group_", userInfo.get("user_group"),
                "update_time_", new Date()));
        //获取新添加的数据，插入日志表
        serviceConfigMapper.copy_to_log(MapUtils.G("tn", tn, "ids", ids, "log_time_", new Date()));
    }


    /**
     * 根据依赖表的ids所在行 关联当前表match_column的值, 与当前表match_column匹配的ids,ids即当前表要依赖更新的范围.
     * @param columnRule  当前列的操作对象ColumnRuleOperation
     * */
    public void addInIds_ByDependMatchColValues(ColumnRuleOperation columnRule){
        List<Map> dependDependFieldOldValues = baseDBService.selectIn(
                columnRule.dependTableName,
                true,
                columnRule.updateRelation.getDependMatchFields(),
                "id_",
                columnRule.dependIds);
        ///------------------------------------------------------------------------
        for (Map dependTableOldRow : dependDependFieldOldValues) {
            Map triggerTableOldMatchValue = new HashMap();
            if(Lutils.nvl(columnRule.updateRelation.match_column1,"").length()>0){
                triggerTableOldMatchValue.put(columnRule.updateRelation.match_column1, dependTableOldRow.get(columnRule.updateRelation.depend_table_match_column1));
            }
            if(Lutils.nvl(columnRule.updateRelation.match_column2,"").length()>0){
                triggerTableOldMatchValue.put(columnRule.updateRelation.match_column2, dependTableOldRow.get(columnRule.updateRelation.depend_table_match_column2));
            }
            if(Lutils.nvl(columnRule.updateRelation.match_column3,"").length()>0){
                triggerTableOldMatchValue.put(columnRule.updateRelation.match_column3, dependTableOldRow.get(columnRule.updateRelation.depend_table_match_column3));
            }
            columnRule.orWhereOfRowsList.add(triggerTableOldMatchValue);
        }
        //查匹配的行的ids
        List<Integer> inIds = baseDBService.selectByCauses(columnRule.tableName,
                Lutils.genList("id_"),
                columnRule.getWhereCause(),
                null).stream().map(o->(Integer)o.get("id_")).collect(Collectors.toList());
        columnRule.addInIds(inIds);
    }

    public void addDependIds_FromDependColumnOperations(ColumnRuleOperation upperTableColumnOperation, List<ColumnRuleOperation> dependTableColOperationList)
    {
        for (String column : upperTableColumnOperation.updateRelation.getDependFields()) {
            //根据每个关联的列，找到匹配的操作，合并操作中的where条件
            for (ColumnRuleOperation dependColumnRuleOperation : dependTableColOperationList) {
                if (column.equals(dependColumnRuleOperation.column)) {
                    upperTableColumnOperation.dependIds.addAll(dependColumnRuleOperation.inIds);
                }
            }
        }
    }


    public void executeTask(String table_name, List<Integer> ids, Integer btnId) {
        Map btn_map = baseDBService.selectEq("v_exec_op", Lutils.genMap("exec_id", btnId)).get(0);
        List<Map> data_list = baseDBService.selectIn(table_name, "id_", ids);
        int flowEdgeId = Integer.parseInt(btn_map.get("flow_edge_id").toString());
        Map flow_edge_map = baseDBService.selectEq("v_flow_edge",
                Lutils.genMap("id", flowEdgeId)).get(0);
        //src  dst都对应node节点id  ，一个是入方向节点，一个是出方向节点
        Integer src = Integer.parseInt( flow_edge_map.get("src").toString() );
        Integer dst = Integer.parseInt( flow_edge_map.get("dst").toString() );
        //flow_edge获取派单信息，current--当前操作人员
        //task-receiver--某环节的接收人员
        //task-completer--某环节的处理人员
        //manual -- 手动派单
        //主要考虑字典派单   rel-dict
        //flow_edge的src是flow_node的id，根据src获取node节点信息判断是哪种类型的节点
        Map flow_node_src = baseDBService.selectEq("v_flow_node",
                Lutils.genMap("id",src)).get(0);
        Map flow_node_dst = baseDBService.selectEq("v_flow_node",
                Lutils.genMap("id",dst)).get(0);
        //判断是否为网关，如果不是网关就一个size，网关则递归处理
        String node_type = flow_node_dst.get("type").toString();
        if(node_type.equals("gate")) {
            //如果为网关 则递归处理 获取该线路下一节点的网关规则 进行数据判断
            // 先处理第一个网关规则
            //flow_edge 中的 condition_field存的是表字段的id

        }else{
            //如果下一节点不是网关，则无需递归
            String assignType = flow_edge_map.get("assign_type").toString();
            if(assignType.equals("rel_dict")){
                if (flow_edge_map.containsKey("rel_dict_id") && flow_edge_map.containsKey("rel_column_id")){
                    //取出对应的字典关系,根据字典派单
                    List<Map> relDictList = getDictRelationByDictIdAndColumnId
                            ((Integer) flow_edge_map.get("rel_dict_id"), (Integer) flow_edge_map.get("rel_column_id"),data_list);

                }
            }
        }
    }



    public List<Map> getDictRelationByDictIdAndColumnId(Integer rel_dict_id,Integer rel_column_id,List<Map> data_list){
        List<Map> dict_list = baseDBService.selectTreeList(rel_dict_id,"v_dict_item");
        String column_field = baseDBService.selectEq("v_table_col", Lutils.genMap("id", rel_column_id)).get(0).get("field").toString();
        List<Map> paidan_data_list = new ArrayList<>();
//        List columnValueList = Lutils.getColumnValueList(data_list, column_field);
        for (int i = 0; i < data_list.size(); i++) {
//            Object column_field_item = columnValueList.get(i);
            Map data_item = data_list.get(i);
            String data_value = data_item.get(column_field).toString();
            for (int j = 0; j < dict_list.size(); j++) {
                Map dict_item = dict_list.get(j);
                String dict_name = dict_item.get("name").toString();
                if(data_value.equals(dict_name)){
                    dict_item.put("data_id",data_item.get("id").toString());
                    paidan_data_list.add(dict_item);
                }

            }
        }

        //获取到派单规则和对应数据后执行派单数据操作，插入三个表
        //rel_post 根据职位派单  rel_role 根据角色派单
        for (int i = 0; i < paidan_data_list.size(); i++) {
            Map paidan_map = paidan_data_list.get(i);
            String rel_type = paidan_map.get("rel_type").toString();
            if(rel_type.equals("rel_role")){
                Integer role_id = Integer.parseInt( paidan_map.get("rel_role_id").toString() );
                Map role_item = baseDBService.selectEq("v_role", Lutils.genMap("id", role_id)).get(0);

            }else if(rel_type.equals("rel_post")){

            }

        }


        return dict_list;
    }

    public void insertAndupdateOtherColRelData(String tableName, Map form, List<Integer> idList) {
        List<Map> updateColRelAndDataList = new ArrayList<>();
        //遍历form 找到本表单提交数据，查询该表每个字段是否有关联关系
        int table_id = Integer.parseInt(tableName.substring(7));

        List<String> formColList = new ArrayList<>(form.keySet());
//        //查找数据更新前的外键ID
        List<Map> data1 = baseDBService.selectIn(tableName, "id_",idList);
//        //更新数据
//        baseDBService.updateIn(tableName,form,"id_",idList);
//        //查找数据更新后的外键ID
//        List<Map> data2 = baseDBService.selectIn(tableName, "id_",idList);
//        List data2_idList = Lutils.getColumnValueList(data2, "id_");
//        idList.addAll(data2_idList);
        //查找是否有关联本表的关联关系
//        List<Map> selfColRelList = baseDBService.selectByCauses("v_table_col_rel",
//                Lutils.genMap("tp","a","cas",Lutils.genList(
//                Lutils.genMap("tp","eq","col","table_name","val",tableName),
//                Lutils.genMap("tp","eq","col","depend_table_name","val",tableName),
//                Lutils.genMap("tp","in","col","depend_table_column","vals",formColList)
//                )),null);
        List<Map> selfColRelList = baseDBService.selectByCauses("v_table_col",Lutils.genMap("tp","a","cas",Lutils.genList(
                Lutils.genMap("tp","eq","col","table_id","val",table_id),
                Lutils.genMap("tp","eq","col","depend_table_id","val",table_id),
                Lutils.genMap("tp","in","col","depend_column","vals",formColList)
        )),null);
        //新增关联关系处理项
        List<Map> selfRelList = new ArrayList<>();
        for (int i = 0; i < selfColRelList.size(); i++) {
            Map selfItem = selfColRelList.get(i);
            String update_column = selfItem.get("field").toString();
            int depend_col_id = Integer.parseInt(selfItem.get("depend_col_id").toString());
            Map map1 = baseDBService.selectEq("v_table_col", Lutils.genMap("id", depend_col_id)).get(0);
            String depend_table_column = map1.get("field").toString();
            String update_type = selfItem.get("depend_type").toString();
            formColList.add(depend_table_column);
            Map map = Lutils.genMap("update_column", update_column,
                    "depend_table_column", depend_table_column,"update_type",update_type);
            selfRelList.add(map);
        }
        if(selfRelList.size()>0){
            updateColRelAndDataList.add(Lutils.genMap("relationList",selfRelList,"idList",idList,"dependTableRowIdList",idList,
                    "table_name",tableName,"depend_table_name",tableName));
        }

        //查找是否有关联其他表的关联关系
        List<Map> otherColRelList = baseDBService.selectByCauses("v_table_col",
                Lutils.genMap("tp","a","cas",Lutils.genList(
                        Lutils.genMap("tp","neq","col","table_id","val",table_id),
                        Lutils.genMap("tp","eq","col","depend_table_id","val",table_id),
                        Lutils.genMap("tp","in","col","depend_column","vals",formColList)
                )),null);
        //先处理查出来的关联关系  把数据存到updateColRelAndDataList中
        Map<String, TableAncColumnDataEntity> result = new HashMap<>();
        List<Map> otherTableList = new ArrayList<>();
        for (int i = 0; i < otherColRelList.size(); i++) {
            Map otherItem = otherColRelList.get(i);
            String table_name = "z_table"+otherItem.get("table_id").toString();
            String depend_table_name = "z_table"+otherItem.get("depend_table_id").toString();
            String update_column = otherItem.get("field").toString();
            int depend_col_id = Integer.parseInt(otherItem.get("depend_col_id").toString());
            Map map1 = baseDBService.selectEq("v_table_col", Lutils.genMap("id", depend_col_id)).get(0);
            String depend_table_column = map1.get("field").toString();
            String update_type = otherItem.get("depend_type").toString();
            List<Integer> updateTable_idList = Lutils.getColumnValueList(data1, table_name + "_id");
            updateColRelAndDataList.add(Lutils.genMap("relationList",
                    Lutils.genList(Lutils.genMap("update_column", update_column,
                            "depend_table_column", depend_table_column,"update_type",update_type)),
                    "idList",updateTable_idList,"dependTableRowIdList",idList, "table_name",table_name,"depend_table_name",depend_table_name));
            if (result.containsKey(table_name)) {
                result.get(table_name).getUpdate_column_list().add(update_column);
            } else {
                // 如果 result 中没有这个 table_name，则创建一个新的列表并添加 update_column
                TableAncColumnDataEntity item = new TableAncColumnDataEntity(table_name,Lutils.genList(update_column),updateTable_idList);
                result.put(table_name,item);
            }
        }
        //处理完后，再根据延伸表去循环找延伸表的关联关系
        Iterator<Map.Entry<String, TableAncColumnDataEntity>> iterator = result.entrySet().iterator();
        while (iterator.hasNext()) {
            // 获取当前元素
            Map.Entry<String, TableAncColumnDataEntity> entry = iterator.next();
            String table_name = entry.getKey();
            TableAncColumnDataEntity tableInfo = entry.getValue();
            int table_id_item = Integer.parseInt(table_name.substring(7));
            List<Map> list = dealOtherTableRelList(table_id_item, tableInfo);
            updateColRelAndDataList.addAll(list);
            // 处理完成后移除元素
            iterator.remove();
            // 如果没有元素了，退出循环
            if (!iterator.hasNext()) {
                break;
            }
        }
        //处理数据
        System.out.println(updateColRelAndDataList+"232323232323");
        for (int i = 0; i < updateColRelAndDataList.size(); i++) {
            Map map = updateColRelAndDataList.get(i);
            List relationList = (List) map.get("relationList");
            List id_list = (List) map.get("idList");
            List rel_id_list = (List) map.get("dependTableRowIdList");
            Object table_name = map.get("table_name");
            Object depend_table_name = map.get("depend_table_name");
//           "relationList", Lutils.genList(Lutils.genMap("update_column", update_column,
//                   "depend_table_column", depend_table_column,"update_type",update_type)),
//                   "idList",updateTable_idList,"dependTableRowIdList,idList, "table_name",table_name,"depend_table_name",depend_table_name
            for (int j = 0; j < relationList.size(); j++) {
                Map relationItem = (Map) relationList.get(j);
                String update_type = relationItem.get("update_type").toString();
                Map m = Lutils.genMap("relation", relationItem, "table_name", table_name,
                        "depend_table_name", depend_table_name,
                        "id_list", id_list, "dependTableRowIdList", rel_id_list);
                if(update_type.equals("count")){
                    int count = serviceConfigMapper.update_by_depend_col_counts(m);
                }else if(update_type.equals("distinct")){
                    int count = serviceConfigMapper.update_by_depend_col_distinct(m);
                }else if(update_type.equals("new-record")){
                    int count = serviceConfigMapper.update_by_depend_col_newvalue(m);
                }else if(update_type.equals("defined_sql")){

                }else if(update_type.equals("sum")){
                    int count = serviceConfigMapper.update_by_depend_col_sum(m);
                }
            }


        }
    }
}
