package tcdx.uap.service.entities;

import com.github.pagehelper.PageInfo;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;
import tcdx.uap.common.entity.page.TableDataInfo;
import tcdx.uap.common.utils.Lutils;
import tcdx.uap.common.utils.SqlUtil;
import tcdx.uap.common.utils.xss.SQLParser;
import tcdx.uap.mapper.BusinessMapper;
import tcdx.uap.mapper.SystemMapper;
import tcdx.uap.service.BaseDBService;
import tcdx.uap.service.store.Modules;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static tcdx.uap.common.utils.PageUtils.startPage;

public class CompUtils {
    @Getter
    private static CompUtils instance = new CompUtils();
    private CompUtils()
    {
        System.out.println("单例模式实现一-饿汉式");
    }


    BaseDBService db;
    BusinessMapper businessMapper;
    SystemMapper systemMapper;
    public void Init(BaseDBService db, BusinessMapper businessMapper, SystemMapper systemMapper){
        this.db = db;
        this.businessMapper = businessMapper;
        this.systemMapper = systemMapper;
    }


    /**
     * 查询数据源数据
     * @param submitMap 包含pageNum、pageSize、obj_c1、table_id{1,2,3,4}、role_ids、group_ids
     * */
    public TableDataInfo get_ds_data(CompDataSource ds, Map<String,Object> submitMap,ExecContext execContext_s, UserAction userAction, Boolean needTotal) throws Exception {
        if(ds.data_type==null||ds.data_sql==null)
            return null;
        Map mybatisParams = submitMap==null?new HashMap<>():Lutils.copyMap(submitMap);
        String ac_scope= Lutils.nvl(ds.data_access_scope,"none");
        //准备查询
        TableDataInfo dataInfo = new TableDataInfo();
        List<Map> rows = new ArrayList<>();
        boolean containTotal = true;
        if( needTotal != null ){
            containTotal = needTotal && Lutils.nvl( ds.enable_total,false);
        }
        Map pageInfo = Lutils.genMap(
                "pageSize", mybatisParams.get("pageSize")!=null?mybatisParams.get("pageSize"):(Lutils.nvl(ds.page_size,0)==0?10:ds.page_size),
                "pageNum", mybatisParams.get("pageNum")!=null?mybatisParams.get("pageNum"):1
        );
        Table tbl = (Table) Modules.getInstance().get(ds.table_id, false);
        //处理别名关系
        Map<String, String> aName = GenTableAlias(tbl);
        //开始取数
        if((ds.data_type.equals("table")&&!ac_scope.equals("none"))||ds.data_type.equals("defined")){
            //(不纳入统计)搜索causesObj是前端传上来的，and关系相连
            if(mybatisParams.get("obj_c1") != null) {
                Map andSqlObj = (Map) mybatisParams.get("obj_c1");
                //处理传上来的如日期、数组等值，转化成数据库可识别的值and条件中的所有条件的值
                //先处理cas里的条件
                if (andSqlObj.containsKey("tp") && (andSqlObj.get("tp").equals("a") || andSqlObj.get("tp").equals("a"))) {
                    for (Map obj : (List<Map>) andSqlObj.get("cas")) {
                        BaseDBService.handlerObjectTypedField(obj);
                        if (ds.data_type.equals("table")) {
                            String table_FieldName = obj.get("col").toString();
                            String[] segs = table_FieldName.split("\\.");
                            if (segs.length > 1) {
                                obj.put("col", table_FieldName.replace(segs[0] + ".", aName.get(segs[0]) + "."));
                            }
                        }
                    }
                } else {
                    if (ds.data_type.equals("table")) {
                        String table_FieldName = andSqlObj.get("col").toString();
                        String[] segs = table_FieldName.split("\\.");
                        if (segs.length > 1) {
                            andSqlObj.put("col", table_FieldName.replace(segs[0] + ".", aName.get(segs[0]) + "."));
                        }
                    }
                }
            }
            //准备传入mybatis的数据
            mybatisParams.put("user_id", userAction.user_id);
            mybatisParams.put("staff_nm", userAction.staff_nm);
            mybatisParams.put("role_ids", userAction.userRoleIds);
            mybatisParams.put("group_ids", userAction.userGroupIds);
            String data_sql = ds.data_sql.toString();
            //替换...z_table123_ids
            Map whereIdsMap  = execContext_s.getWhereIdsMap();
            data_sql = replaceSql_z_table_ids(data_sql, whereIdsMap, userAction);
            //sql中的...z_table123_ids未匹配到，则退出取数
            if(data_sql==null)
                return new TableDataInfo();
            mybatisParams.put("data_sql", data_sql);
            mybatisParams.put("has_where", SQLParser.hasWhereClauseInMainQuery(data_sql));
            mybatisParams.put("where_sql", ds.where_sql);
            mybatisParams.put("order_sql",ds.order_sql);
            //取行数据，自带取total
            startPage(pageInfo);
            rows = db.selectDefinedSql(mybatisParams);
            dataInfo.setCode(0);
            dataInfo.setRows(rows);
            dataInfo.ds_id = ds.id;
            dataInfo.containTotal = containTotal;
            if(containTotal) {
                long total = new PageInfo(rows).getTotal();
                dataInfo.setTotal(total);
                for(Map row: rows){
                    row.put("__ds_total__", total);
                }
            }
            //获取字典 放在 page.dict_items中dataInfo.dict_items=[]
        }
//        //table模式，根据模版查询
//        else{
//            if (ac_scope.equals("none")) {
//
//            }
//            else{
//                //数据表的列，即table_col_id不为空的列
//                /**根据会话、用户、数据源字段，处理拼接的sql语句及查询范围。*/
//                boolean cando = HandleAndWhere( ds, submitMap, execContext_s, userAction);
//                if(!cando)
//                    return re;
//                submitMap.put("where_sql",ds.where_sql);
//                submitMap.put("order_sql",ds.order_sql);
//                if(Lutils.nvl(ds.order_sql,"").trim().length()==0) {
//                    submitMap.put("order_sql"," order by "+ds.table_id+".id_ desc");
//                }
//                //根据类型查询
//                if (submitMap.get("orderByColumn") == null) {
//                    submitMap.put("orderByColumn", "id_");
//                    submitMap.put("isAsc", "desc");
//                }
//                startPage(pageInfo);
//                rows = businessMapper.get_grid_data(submitMap);
//                //找出字典相关的列。
//                List<CompDataSourceField> dsDictFields = dsFields.stream()
//                        .filter(o -> Objects.equals(o.field_type, "table_field"))
//                        //数据源字段，获取时，获取数据类型
//                        .filter(o -> ((Table) Modules.getInstance().get(o.table_id, false)).getCol(o.table_col_id).data_type.equals("integer") &&
//                                ((Table) Modules.getInstance().get(o.table_id,false)).getCol(o.table_col_id).rel_dict_id != null)
//                        .collect(Collectors.toList());
//                //查看列字典选项
//                List<Map> dictItems = new ArrayList<>();
//                //循环取字典的值
//                for (CompDataSourceField dictCol : dsDictFields) {
//                    List dictItemIds = rows.stream().map(o -> o.get(dictCol.field)).collect(Collectors.toList());
//                    if (dictItemIds.size() > 0) {
//                        List<Map> dictItem = db.selectIn("v_dict_item", "id", dictItemIds);
//                        dictItems.addAll(dictItem);
//                    }
//                }
//                re.put("dictItems", dictItems);
//                TableDataInfo pageData = new TableDataInfo();
//                pageData.setCode(0);
//                pageData.setRows(rows);
//                if(containTotal)
//                    pageData.setTotal(new PageInfo(rows).getTotal());
//                re.put("page", pageData);
//
//            }
//        }
        //如果数据源有待办人员字段，则获取当前表与其子表的接单人信息
        if(Objects.equals(ds.has_pri_sub_receiver_info, true) || ds.pri_table_id!=null) {
            String pri_table_id = ds.pri_table_id;
            List pri_ids = rows.stream()
                    .filter(r -> r.get("z_" + pri_table_id + "_id") != null)
                    .map(r -> r.get("z_" + pri_table_id + "_id"))
                    .collect(Collectors.toList());
            for(Map row: rows){
                row.put("pri_sub_receiver_info", new HashMap<>());
            }
            if(!pri_ids.isEmpty()) {
                //获取接收人信息
                List<Map> priRowsReceivers = businessMapper.get_pri_cur_receive_info(Lutils.genMap("pri_table_id", pri_table_id, "pri_ids", pri_ids));
                //将子流程，存入当前接单人信息中
                for (Map p : priRowsReceivers) {
                    //找到rows的行，添加到行
                    List<Map> findRows = rows.stream().filter(r -> Objects.equals(r.get("z_" + pri_table_id + "_id"), p.get("z_" + pri_table_id + "_id"))).collect(Collectors.toList());
                    //添加到rows
                    if (findRows.size() > 0) {
                        ((Map)findRows.get(0).get("pri_sub_receiver_info")).put("priReceiverInfo", p);
                    }
                }
                List<String> nodes = priRowsReceivers.stream()
                        .filter(p -> p.get("node_") != null).map(p -> (String) p.get("node_"))
                        .collect(Collectors.toList());
                List<Map> subReceiverInfos = new ArrayList<>();
                //行数据中，存在流程信息，再根据父流程的node，查找子流程的信息
                if (nodes != null && nodes.size() > 0) {
                    //获取当前表的子表，同时node_所属当前表
                    Table ptbl = (Table) Modules.getInstance().get(pri_table_id, false);
                    for (String sub_table_id : ptbl.subTableIds) {
                        Table stbl = (Table) Modules.getInstance().get(sub_table_id, false);
                        //查子流程的表列, 获取列属性
                        List<Map> subReceiverInfo = businessMapper.get_pri_rel_sub_receive_info(Lutils.genMap(
                                "pri_table_id", pri_table_id,
                                "sub_table_id", sub_table_id,
                                "pri_ids", pri_ids));
                        for (Map s : subReceiverInfo) {
                            s.put("sub_table_id", sub_table_id);
                            s.put("sub_table_name", stbl.name);
                            subReceiverInfos.add(s);
                        }
                    }
                    //将子流程，存入当前接单人信息中
                    for (Map row : rows) {
                        ((Map)row.get("pri_sub_receiver_info")).put("subReceiverInfo", subReceiverInfos.stream()
                                .filter(s->Objects.equals(s.get("z_"+pri_table_id+"_id"), row.get("z_"+pri_table_id+"_id")))
                                .collect(Collectors.toList()) );
                    }
                }
            }
        }
        return dataInfo;
    }


//    public boolean HandleAndWhere(CompDataSource ds, Map submitMap, ExecContext execContext_s, UserAction userAction ){
//        //数据源的列
//        //数据表的列，即table_col_id不为空的列
//        List<CompDataSourceField> dataTableCols = ds.fields.stream().filter(o -> o.table_col_id!=null).collect(Collectors.toList());
//        //当前表的用户列、发件人、发件时间等，默认提供。不需要额外处理
//        List<String> flowFields = Constants.getDsFieldDefinition().stream().map(o->o.get("field").toString()).collect(Collectors.toList());
//        List<CompDataSourceField> curFlowEdgeCols1 = ds.fields.stream()
//                .filter(o ->
//                        o.flow_edge_id != null && o.flow_edge_id.equals("edge-1")
//                ).collect(Collectors.toList());
//        List<Map> curFlowEdgeCols = curFlowEdgeCols1.stream().map(o->Lutils.genMap("flow_edge_id","curFlow", "field", o.flow_field)).collect(Collectors.toList());
//        //某个动作的时间列
//        List<CompDataSourceField> selFlowEdgePostedTimeCols = ds.fields.stream()
//                .filter(o -> o.flow_edge_id != null && !o.flow_edge_id.equals("edge-1") && Lutils.nvl(o.flow_field, "").contains("time"))
//                .collect(Collectors.toList());
//        //某个动作的派发人员、号码等列
//        List<CompDataSourceField> selFlowEdgePosterCols = ds.fields.stream()
//                .filter(o -> o.flow_edge_id != null && !o.flow_edge_id.equals("edge-1")  && Lutils.nvl(o.flow_field, "").contains("poster_"))
//                .collect(Collectors.toList());
//        //某个动作的接受人员、号码等列
//        List<CompDataSourceField> selFlowEdgeReceiverCols = ds.fields.stream()
//                .filter(o -> o.flow_edge_id != null && !o.flow_edge_id.equals("edge-1")  && Lutils.nvl(o.flow_field, "").contains("receiver_"))
//                .collect(Collectors.toList());
//        //选定动作的flow_edge_id集合
//        List<String> selFlowEdgeIds = ds.fields.stream()
//                .filter(o -> o.flow_edge_id != null && !o.flow_edge_id.equals("edge-1"))
//                .map(o -> o.flow_edge_id).distinct().collect(Collectors.toList());
//
//        //拼接sql语句
//        //剔除自定义列，自定义列的table_col_id是空的
//        submitMap.put("table_id", ds.table_id);
//        submitMap.put("dataTableCols", dataTableCols);
//        //当前操作人和操作时间
//        submitMap.put("curFlowEdgeCols", curFlowEdgeCols);
//        //指定动作的操作人和操作时间
//        submitMap.put("selFlowEdgePostedTimeCols", selFlowEdgePostedTimeCols);
//        submitMap.put("selFlowEdgePosterCols", selFlowEdgePosterCols);
//        submitMap.put("selFlowEdgeReceiverCols", selFlowEdgeReceiverCols);
//        submitMap.put("leftJoinFlowEdgeTables", selFlowEdgeIds);
//        //(不纳入统计)搜索causesObj是前端传上来的，and关系相连
//        Map andSqlObj = submitMap.get("obj_c1")!=null?(Map)submitMap.get("obj_c1"):Lutils.genMap("obj_c1",new HashMap<>());
//        //处理传上来的如日期、数组等值，转化成数据库可识别的值and条件中的所有条件的值
//        if (andSqlObj.containsKey("tp") && andSqlObj.get("tp").equals("a")) {
//            for (Map obj : (List<Map>) andSqlObj.get("cas")) {
//                BaseDBService.handlerObjectTypedField(obj);
//            }
//        }
//        // 会话过滤（不纳入统计）增补会话筛选。如果session有数据，则提交表格域限制条件，则添加权限筛选条件
//        //记录需要过滤的外联表，用于后续优化sql
//        List<String> filterSessionForeignTableIds = new ArrayList<>();
//        if(ds.limit_session_table_id != null){
//            //到会话中查找当前表或当前表主表
//            List<Integer> whereIds = execContext_s.getTableIds(ds.limit_session_table_id);
//            if(whereIds.isEmpty()) {
//                return false;
//            }
//            //增加变andSqlObj
//            andSqlObj = appendToAndCause(andSqlObj, SqlUtil.in( ds.limit_session_table_id + ".id_", whereIds));
//            filterSessionForeignTableIds.add(ds.limit_session_table_id);
//        }
//        //根据提交map中需要筛选的表ID，做左连接
//        Table tbl = (Table) Modules.getInstance().get(ds.table_id, false);
//        List<Map> subPriMap = tbl.getSubPriMap();
//        List<String> priIds = subPriMap.stream().map(o->o.get("pid").toString()).collect(Collectors.toList());
//        //控制外联表的关联深度：1、根据字段；2、根据会话中的筛选条件都
//        int foreignScopeIndex = -1;
//        for(int i=0; i < priIds.size(); i ++ ) {
//            String pri_table_id = priIds.get(i);
//            List<String> fieldTableIds = ds.fields.stream().filter(f->f.table_id!=null).map(f->f.table_id)
//                    .collect(Collectors.toList());
//            //如果查询的字段涉及外联表的字段，或where条件涉及外联表的字段，则保留外联表的left join 关系
//            if(fieldTableIds.contains(pri_table_id) || filterSessionForeignTableIds.contains(pri_table_id))
//            {
//                foreignScopeIndex = i;
//            }
//        }
//        subPriMap = subPriMap.subList(0, foreignScopeIndex+1);
//        submitMap.put("leftJoinForeignTables", subPriMap);
//
////        //数据获取权限
////        if (dataSource.containsKey("data_access_scope")&&
////                dataSource.get("data_access_scope").toString().equals("defined")) {
////            List ids_ = serviceConfigService.get_view_grid_data_permission_map(dataSource, user_id,null,null);
////            if(ids_.size()==0){
////                ids_.add(-1);
////            }
////            map.put("obj_in_ids",SqlUtil.in("t" + ds_table_id + ".id_", ids_));
////        }
//        submitMap.put("obj_c1", andSqlObj);
//        return true;
//    }

    /**
     * SqlUtil.in("t" + table_id + ".id_", whereIds)
     * */
    public Map appendToAndCause(Map andSqlObj, Map sqlObj){
        if (andSqlObj.containsKey("tp") && andSqlObj.get("tp").equals("a")) {
            ((List<Map>) andSqlObj.get("cas")).add(sqlObj);
        } else {
            andSqlObj=SqlUtil.and(sqlObj);
        }
        return andSqlObj;
    }

    public static List<String> getSql___z_table_ids(String sql){
        Pattern pattern = Pattern.compile("\\.\\.\\.z_table\\d+_ids");
        Matcher matcher = pattern.matcher(sql);
        List<String> tableIds = new ArrayList<>();
        while (matcher.find()) {
            String z_table_ids_param = matcher.group();
            String table_id = z_table_ids_param.replace('.', ' ').replace("z_","").replace("_ids", "").trim();
            tableIds.add(table_id);
        }
        return tableIds;
    }


    /**
     * 替换...z_table123_ids、...role_ids、...group_ids
     * */
    public static String replaceSql_z_table_ids(String upd_sql, Map<String,List> idsMap, UserAction ua){
        List<String> table_ids = getSql___z_table_ids(upd_sql);
        for (String table_id: table_ids) {
            String ___z_table_ids = "...z_"+table_id+"_ids";
            if(idsMap==null || idsMap.isEmpty())
                return null;
            List limitIds = idsMap.get(table_id);
            if(limitIds==null || limitIds.isEmpty())
                return null;
            //替换sql语句
            upd_sql = upd_sql.replace(___z_table_ids, StringUtils.join(limitIds, ","));
        }
        if(upd_sql.contains("...role_ids")){
            upd_sql = upd_sql.replace("...role_ids", StringUtils.join(ua.userRoleIds, ","));
        }
        if(upd_sql.contains("...group_ids")){
            upd_sql = upd_sql.replace("...group_ids", StringUtils.join(ua.userGroupIds, ","));
        }
        return upd_sql;
    }

    //#{}  ...z_table111_ids
    //查找update z_table   insert into z_table  delete from z_table
    public static String ExtraOperatedTable(String upd_sql, String pattern) {
        //从脚本中，提取upd_table
        Pattern pattern1 = Pattern.compile(pattern);
        Matcher matcher1 = pattern1.matcher(upd_sql);
        String upd_table = null;
        while (matcher1.find()) {
            upd_table = matcher1.group()
                    .replace("update","")
                    .replace("insert","")
                    .replace("delete","").trim();
        }
        if (upd_table == null) {
            return null;
        }
        return upd_table.replace("z_","");
    }
    //获取ds数据的ids
    //获取所都ds数据
    public List get_ds_data_ids(CompDataSource ds, Map submitMap,
                               ExecContext execContext_s, UserAction userAction, Boolean needTotal) throws Exception {
        return new ArrayList();
    }

    public static CompDataSource getViewDataSource(View view) {
        if(view.view_type.equals("comp")&&view.comp_name.equals("CompGrid")){
            CompGrid comp = (CompGrid)Modules.getInstance().get(view.comp_id, true);
            if(comp!=null) {
                return (CompDataSource) Modules.getInstance().get(comp.ds_id, true);
            }
            else
                return null;
        }
        else if(view.view_type.equals("comp")&&view.comp_name.equals("CompCarousel")){
            CompCarousel comp = (CompCarousel)Modules.getInstance().get(view.comp_id, true);
            if(comp!=null) {
                return (CompDataSource) Modules.getInstance().get(comp.ds_id, true);
            }
            else{
                return null;
            }
        }
        else if(view.view_type.equals("comp")&&view.comp_name.equals("CompDataSource")){
            return (CompDataSource)Modules.getInstance().get(view.comp_id, true);
        }
        else{
            return null;
        }
    }


    /**
     * 获取到的人员反馈行结构： scope_id, user_id, role_id, group_id,及中文信息
     * */
    public List<Map> getScopesUsers(List roles, List groups){
        //选项对应的scopeIds
        List users = systemMapper.selectScopedUsers(Lutils.genMap("roles", roles, "groups",groups));
        return users;
    }

    /**
     * 获取到的人员反馈行结构： scope_id, user_id, role_id, group_id,及中文信息
     * @params key  obj_type+obj_id 或  scope表的id
     * */
    public Map getScopedUsersWithGroup(UserScope userScope, ExecContext execContext_s, UserAction userAction){

        // 判定是否启用自定义 SQL
        String sqlStr = Objects.toString(Lutils.nvl(userScope.sql_str, ""), "").trim();
        boolean isDefinedSql = Lutils.nvl(userScope.is_defined_sql, false);
        //从 contextList 中取出不同表的id，存放于z_table123_id、z_table123_id中。
        /**
         * contextList = [
         *      { table_id: 123, op_db_type: submit/update`/complete, before_or_after: before/after, id_: 12323
         * ]
         * */
        Map<String, Object> params = new HashMap<>();

        //开始取数
        List<Map> users;
        if (isDefinedSql && !sqlStr.isEmpty()) {
            // ========== 自定义 SQL 路径 ==========
            // 准备参数：注意你的 sql_str 里使用 #{user_id}，因此这里 key 取 "user_id"
            // 会话里的用户上下文
            params.put("user_id", userAction.user_id); // #{user_id}
            // 如需角色/组织等，可按需加入（若你的自定义 SQL 用得到）
            params.put("userGroupIds", userAction.userGroupIds); // #{userGroupIds}
            params.put("userRoleIds",  userAction.userRoleIds);  // #{userRoleIds}
            sqlStr = Lutils.parseUserDefinedSql(sqlStr, params);
            sqlStr = replaceSql_z_table_ids(sqlStr, execContext_s.getWhereIdsMap(), userAction);
            params.put("sql", sqlStr);
            // 执行自定义 SQL（只读查询）
            users = systemMapper.runUserDefinedSql(params);

            // 期望自定义 SQL 至少返回：user_id、group_id、staff_nm
            for (Map row : users) {
                Object uid  = row.get("user_id");
                Object gid  = row.get("group_id");
                Object name = row.get("staff_nm");
                row.put("node_key",   "user" + uid);
                row.put("parent_key", "group" + gid);
                row.put("node_type",  "user");
                row.put("id",         uid);
                row.put("name",       name);
            }
        } else {
            if (userScope.roles == null || userScope.groups== null) {
                return Lutils.genMap("users", Collections.emptyList(), "groups", Collections.emptyList());
            }
            // ========== 原有内置 SQL 路径 ==========
            userScope.roles.add(-10000);   // 防止空数组出错
            userScope.groups.add(-10000);  //防止空数组出错
            users = systemMapper.selectScopedUsers(Lutils.genMap("roles", userScope.roles, "groups", userScope.groups));
            for (Map user : users) {
                user.put("node_key",   "user" + user.get("user_id"));
                user.put("parent_key", "group" + user.get("group_id"));
                user.put("node_type",  "user");
                user.put("id",         user.get("user_id"));
                user.put("user_id",    user.get("user_id"));
                user.put("name",       user.get("staff_nm"));
            }
        }

        // ========== 组织树（上级组织） ==========
        List<Map> groups = new ArrayList<>();
        if (!users.isEmpty()) {
            List<Object> groupIds = users.stream()
                    .map(o -> o.get("group_id"))
                    .filter(Objects::nonNull)
                    .distinct()
                    .collect(Collectors.toList());
            groups = systemMapper.selectUpperGroups(Lutils.genMap("groupIds", groupIds));
            for (Map group : groups) {
                group.put("node_key",   "group" + group.get("id"));
                group.put("parent_key", "group" + group.get("parent_id"));
                group.put("node_type",  "group");
                group.put("id",         group.get("id"));
                group.put("group_id",   group.get("id"));
                group.put("name",       group.get("name"));
            }
        }

        return Lutils.genMap("users", users, "groups", groups);
    }

    public List<Integer> getScopedUsers(UserScope userScope, ExecContext execContext_s, UserAction userAction){
        // 判定是否启用自定义 SQL
        String sqlStr = Objects.toString(Lutils.nvl(userScope.sql_str, ""), "").trim();
        boolean isDefinedSql = Lutils.nvl(userScope.is_defined_sql, false);
        Map<String, Object> params = new HashMap<>();
        //开始取数
        List<Map> users;
        if (isDefinedSql && !sqlStr.isEmpty()) {
            // ========== 自定义 SQL 路径 ==========
            // 准备参数：注意你的 sql_str 里使用 #{user_id}，因此这里 key 取 "user_id"
            // 会话里的用户上下文
            params.put("user_id", userAction.user_id); // #{user_id}
            params.put("group_ids", userAction.userGroupIds);
            params.put("role_ids", userAction.userRoleIds);
//            sqlStr = Lutils.parseUserDefinedSql(sqlStr, params);
            sqlStr = replaceSql_z_table_ids(sqlStr, execContext_s.getWhereIdsMap(), userAction);
            params.put("sql", sqlStr);
            // 执行自定义 SQL（只读查询）
            users = systemMapper.runUserDefinedSql(params);
            return users.stream().map(o->(Integer)o.get("user_id")).collect(Collectors.toList());
        } else {
            if (userScope.roles == null || userScope.groups== null) {
                return new ArrayList<>();
            }
            // ========== 原有内置 SQL 路径 ==========
            userScope.roles.add(-10000);   // 防止空数组出错
            userScope.groups.add(-10000);  //防止空数组出错
            users = systemMapper.selectScopedUsers(Lutils.genMap("roles", userScope.roles, "groups", userScope.groups));
            return users.stream().map(o->(Integer)o.get("user_id")).collect(Collectors.toList());
        }
    }

    //获取数据源涉及表的别名
    public static Map<String, String> GenTableAlias(Table tbl) {
        Map<String, String> aName = new HashMap<>();

        for (int i = 2; i < 10; i++) {
            aName.clear();

            Set<String> usedAlias = new HashSet<>();
            boolean conflict = false;

            // 主表
            String mainAlias = "t" + safeSuffix(tbl.id, i);
            if (!usedAlias.add(mainAlias)) conflict = true;
            aName.put(tbl.id, mainAlias);

            // 关联表
            for (String tid : tbl.priTableIds) {
                String alias = "t" + safeSuffix(tid, i);

                // 检查别名是否重复
                if (!usedAlias.add(alias)) {
                    conflict = true;
                    break;
                }
                aName.put(tid, alias);
            }

            // 别名不冲突才 break
            if (!conflict) {
                break;
            }
        }
        return aName;
    }

    private static String safeSuffix(String s, int len) {
        if (s == null) return "";
        int start = Math.max(0, s.length() - len);
        return s.substring(start);
    }


    public static String gen_ds_sql(CompDataSource ds){
        Table tbl = (Table)Modules.getInstance().get(ds.table_id, false);
        if(tbl==null)
            return null;
        if(Objects.equals(ds.data_type,"table")){
            boolean left_join_poster = false;
            boolean left_join_receiver = false;
            boolean left_join_update_user = false;
            boolean left_join_create_user = false;
            boolean left_join_node_label = false;
            boolean left_join_edge_label = false;
            List<String> field_table_ids = ds.fields.stream().map(f->f.table_id).distinct().collect(Collectors.toList());
            //缩短别名
            Map<String, String> aName = GenTableAlias(tbl);
            //构建sql
//            String []flowFields = {"edge_", "node_", "receiver_", "poster_", "create_user_", "update_user_"};
            String selectSql = "select "+aName.get(ds.table_id)+".id_ \n";
            selectSql += ","+aName.get(ds.table_id)+".id_ z_" +ds.table_id+"_id \n";
            for(CompDataSourceField f: ds.fields) {
                if(f.field==null)
                    continue;
                if(f.field.startsWith("poster_")&&!f.field.equals("poster_")) {
                    selectSql += ",p." + f.field.replace("poster_","") + " " + f.field + "\n";  //p.staff_nm poster_staff_nm
                    left_join_poster = true;
                }
                else if(f.field.startsWith("receiver_")&&!f.field.equals("receiver_")) {
                    left_join_receiver = true;
                    selectSql += ",r." + f.field.replace("receiver_", "") + " " + f.field + "\n";
                }
                else if(f.field.startsWith("update_user_")&&!f.field.equals("update_user_")) {
                    left_join_update_user = true;
                    selectSql += ",u." + f.field.replace("update_user_","") + " " + f.field + "\n";  //p.staff_nm poster_staff_nm
                }
                else if(f.field.startsWith("create_user_")&&!f.field.equals("create_user_")) {
                    left_join_create_user = true;
                    selectSql += ",c." + f.field.replace("create_user_","") + " " + f.field + "\n";  //p.staff_nm poster_staff_nm
                }
                else if(f.field.equals("node_label_")) {
                    left_join_node_label = true;
                    selectSql += ",n.label " + f.field + "\n";  //p.staff_nm poster_staff_nm
                }
                else if(f.field.equals("edge_label_")) {
                    left_join_edge_label = true;
                    selectSql += ",e.label " + f.field + "\n";  //p.staff_nm poster_staff_nm
                }
                else {
                    selectSql += "," + aName.get(f.table_id) + "." + f.field + "\n";  //table123.zhuti
                }
            }
            String fromSql = "from z_"+ ds.table_id +" " + aName.get(ds.table_id) +"\n";
            //左连接主表
            for(String ftid: field_table_ids.stream().filter(f->!Objects.equals(f, ds.table_id)).collect(Collectors.toList())){
                fromSql += "left join z_" + ftid + " " + aName.get(ftid)
                        + " on "+aName.get(ds.table_id)+".z_"+ftid+"_id="+aName.get(ftid)+".id_\n";
                selectSql += "," + aName.get(ftid) + ".id_ z_"+ ftid+"_id\n";
            }
            //左连接动作表
            if(left_join_poster)
                fromSql += "left join v_user p on "+ aName.get(ds.table_id) +".poster_=p.id \n";
            if(left_join_receiver)
                fromSql += "left join v_user r on "+ aName.get(ds.table_id) +".receiver_=r.id \n";
            if(left_join_update_user)
                fromSql += "left join v_user u on "+ aName.get(ds.table_id) +".update_user_=u.id \n";
            if(left_join_create_user)
                fromSql += "left join v_user c on "+ aName.get(ds.table_id) +".create_user_=c.id \n";
            if(left_join_node_label)
                fromSql += "left join v_flow_node n on "+ aName.get(ds.table_id) +".node_=n.id \n";
            if(left_join_edge_label)
                fromSql += "left join v_flow_edge e on "+ aName.get(ds.table_id) +".edge_=e.id \n";
            String whereSql = "where "+ aName.get(ds.table_id) +".time_if_delete_ is null \n";
            if(ds.limit_session_table_id!=null){
                if(Objects.equals(ds.table_id, ds.limit_session_table_id))  //本表
                    whereSql += "  and " + aName.get(ds.table_id) + ".id_ in (...z_"+ds.limit_session_table_id+"_ids) \n";
                else
                    whereSql += "  and " + aName.get(ds.table_id) + ".z_"+ds.limit_session_table_id+"_id in (...z_"+ds.limit_session_table_id+"_ids) \n";
            }
            return selectSql+fromSql+whereSql;
        }
        else {
            return ds.data_sql;
        }
    }
}
