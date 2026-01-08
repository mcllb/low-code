package tcdx.uap.service.store;

import com.github.pagehelper.PageInfo;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;
import tcdx.uap.common.entity.page.TableDataInfo;
import tcdx.uap.common.utils.Lutils;
import tcdx.uap.common.utils.SqlUtil;
import tcdx.uap.common.utils.xss.SQLParser;
import tcdx.uap.mapper.BusinessMapper;
import tcdx.uap.service.BaseDBService;
import tcdx.uap.service.entities.DSHasSettings;

import javax.servlet.http.HttpSession;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import static tcdx.uap.common.utils.PageUtils.startPage;

public class CompDataSourceStore {

    private HashMap<Object,Map> store;
    String [] tableTypedCols= new String[] {"table_field", "flow_field", "ds_rows_length", "ds_total", "pri_sub_receiver_info"};
    String [] definedTypedCols= new String[] {"defined_field", "ds_rows_length", "ds_total", "pri_sub_receiver_info"};

    @Getter
    private static CompDataSourceStore instance = new CompDataSourceStore();

    private CompDataSourceStore()
    {
        System.out.println("单例模式实现一-饿汉式");
    }

    BaseDBService db;
    BusinessMapper businessMapper;
    public void InitAll(BaseDBService db, BusinessMapper businessMapper){
        this.db = db;
        this.businessMapper = businessMapper;
        InitAll();
    }

    public synchronized void InitAll(){
        //集合缓存，初始化数据源字段
        store = new HashMap();
        //所有数据源
        List<Map> v_datasource = db.selectByCauses("v_datasource", null, null);
        if(v_datasource.size()>0) {
            //所有数据源
           //获取字段
            List<Map> v_datasource_field = db.selectByCauses("v_datasource_field",
                    SqlUtil.and(SqlUtil.eq("is_deleted", false)
                    ), Lutils.genMap("ord", "asc"));
            //编辑器
            List<Map> v_comp_value_editor = db.selectByCauses("v_comp_value_editor",
                    SqlUtil.in("ds_field_id", v_datasource_field.stream().map(o -> o.get("id")).collect(Collectors.toList())),
                    null);
            //组装数据源
            assemble(v_datasource, v_datasource_field, v_comp_value_editor);
        }
    }

    public void copy( Map oldNode, Map newNode ) {

    }

    public synchronized void set(Object key){
        List<Map> v_datasource = new ArrayList<>();
        if(key instanceof String){
            String keyStr = key.toString();
            Integer obj_id = null;
            String obj_type = null;
            if(keyStr.startsWith("comp_log")){
                obj_type = "comp_log";
                obj_id = Integer.parseInt(keyStr.replace("comp_log",""));
            }
            else if(keyStr.startsWith("comp_huiqian")){
                obj_type = "comp_huiqian";
                obj_id = Integer.parseInt(keyStr.replace("comp_huiqian",""));
            }
            else if(keyStr.startsWith("task_plan")){
                obj_type = "task_plan";
                obj_id = Integer.parseInt(keyStr.replace("task_plan",""));
            }
            else if(keyStr.startsWith("comp_carousel")){
                obj_type = "comp_carousel";
                obj_id = Integer.parseInt(keyStr.replace("comp_carousel",""));
            }
            else if(keyStr.startsWith("view")){
                obj_type = "view";
                obj_id = Integer.parseInt(keyStr.replace("view",""));
            }
            else if(keyStr.startsWith("comp_grid")){
                obj_type = "comp_grid";
                obj_id = Integer.parseInt(keyStr.replace("comp_grid",""));
            }
            else if(keyStr.startsWith("comp_echarts")){
                obj_type = "comp_echarts";
                obj_id = Integer.parseInt(keyStr.replace("comp_echarts",""));
            }
            else if(keyStr.startsWith("comp_echarts_gantt")){
                obj_type = "comp_echarts_gantt";
                obj_id = Integer.parseInt(keyStr.replace("comp_echarts_gantt",""));
            }
            v_datasource = db.selectEq("v_datasource", Lutils.genMap("obj_type",obj_type, "obj_id", obj_id));
        }
        else if(key instanceof Integer){//所有数据源
             v_datasource = db.selectEq("v_datasource", Lutils.genMap("id", key));
        }
        if(v_datasource.size()>0) {
            //获取字段
            List<Map> v_datasource_field = db.selectByCauses("v_datasource_field",
                    SqlUtil.and(SqlUtil.in("ds_id", v_datasource.stream().map(o -> o.get("id")).collect(Collectors.toList())),
                            SqlUtil.eq("is_deleted", false)
                    ),
                    Lutils.genMap("ord", "asc"));
            //编辑器
            List<Map> v_comp_value_editor = db.selectByCauses("v_comp_value_editor",
                    SqlUtil.in("ds_field_id", v_datasource_field.stream().map(o -> o.get("id")).collect(Collectors.toList())),
                    null);
            //组装数据源
            assemble(v_datasource, v_datasource_field, v_comp_value_editor);
        }
    }

    private void assemble( List<Map>v_datasource, List<Map> v_datasource_field, List<Map> v_comp_value_editor){
        //数据源缓存
        for(Map map : v_datasource){
            map.put("fields", new ArrayList<Map>());
            store.put(map.get("id"), map);
            //view123、comp_grid123、comp_carousel123
            store.put((String)map.get("obj_type")+map.get("obj_id"), map);
        }
        //集合缓存
        Map <Object,Map> editorsMap = new HashMap();
        for(Map map:v_comp_value_editor){
            editorsMap.put(map.get("id"), map);
        }
        /** 将数据列加入到数据源 */
        for(Map f : v_datasource_field){
            Integer ds_id = (Integer)f.get("ds_id");
            String field_type = (String)f.get("field_type");
            Map ds = store.get(ds_id);
            if(ds==null)
                continue;
            //将字段添加到对应的数据源fields中
            List<Map> fields = (List<Map>)ds.get("fields");
            if(Objects.equals(ds.get("data_type"),"defined") && Arrays.stream(definedTypedCols).anyMatch(m->m.equals(field_type))) {
                fields.add(f);
            }
            else if(Objects.equals(ds.get("data_type"),"table") && Arrays.stream(tableTypedCols).anyMatch(m->m.equals(field_type))) {
                fields.add(f);
            }
        }
    }

    public Map create(Object obj_type, Object obj_id){
        Map ds = db.insertWhenNotExist("v_datasource", Lutils.genMap("obj_id", obj_id, "obj_type", obj_type), Lutils.genMap("obj_id", obj_id, "obj_type", obj_type));
        db.insertWhenNotExist("v_datasource_field",
                Lutils.genMap("ds_id", ds.get("id"), "is_deleted", false, "field_type", "ds_total"),
                Lutils.genMap("ds_id", ds.get("id"), "field_type", "ds_total"));
        db.insertWhenNotExist("v_datasource_field",
                Lutils.genMap("ds_id", ds.get("id"), "is_deleted", false, "field_type", "ds_rows_length"),
                Lutils.genMap("ds_id", ds.get("id"), "field_type", "ds_rows_length"));
        CompDataSourceStore.getInstance().set(ds.get("id"));
        return CompDataSourceStore.getInstance().get(ds.get("id"));
    }

    /**
     * 获取组件，同时装配动态数据
     * @param key key可以是Id，也可以是ObjType+Id
     * */
    public synchronized Map getSimplify(Object key){
        Map compDataSource = store.get(key);
        if(compDataSource==null)
            return null;
        List<Map> fields = (List<Map>)compDataSource.get("fields");
        if(fields==null||fields.size()==0)
            return compDataSource;
        /** 将数据列纳入数据源 */
        for(Map f : fields){
            String field_type = (String)f.get("field_type");
            if(compDataSource==null)
                continue;
            String field = null;
            if(Objects.equals(compDataSource.get("data_type"),"table") && Arrays.stream(tableTypedCols).anyMatch(m->m.equals(field_type))){
                if(field_type.equals("table_field")){
                    Integer table_id = (Integer)f.get("table_id");
                    Integer table_col_id = (Integer)f.get("table_col_id");
                    Map tableCol = TableStore.getInstance().getTableCol(table_col_id);
                    if(tableCol==null)
                        continue;
                    field = (String)tableCol.get("field");
                    f.put("data_type", tableCol.get("data_type"));
                    f.put("rel_dict_id", tableCol.get("rel_dict_id"));
                    f.put("tableColObj", tableCol);
                    f.put("tableObj", TableStore.getInstance().get(table_id));
                }
                else if(field_type.equals("flow_field")){
                    Integer flow_edge_id = (Integer)f.get("flow_edge_id");
                    field = Objects.equals(flow_edge_id,-1)?(String)f.get("flow_field"):("f"+flow_edge_id+f.get("flow_field"));
                }
            }
            if(Objects.equals(compDataSource.get("data_type"),"defined") && Arrays.stream(definedTypedCols).anyMatch(m->m.equals(field_type))){
                //如果不符合defined数据源的字段，丢弃
                if(f.get("defined_field")==null){
                    continue;
                }
                field =  (String) f.get("defined_field");
            }
            if(field_type.equals("ds_rows_length")){
                field = "ds_rows_length";
            }
            else if(field_type.equals("ds_total")){
                field = "ds_total";
            }
            else if(field_type.equals("pri_sub_receiver_info")){
                field = "pri_sub_receiver_info";
            }
            f.put("field", field);
//            f.put("editors", CompValueEditorStore.getInstance().getByField(f.get("id")));
            //将字段添加到对应的数据源fields中
        }
        return compDataSource;
    }

    /**
     * 获取组件，同时装配动态数据
     * @param key key可以是Id，也可以是ObjType+Id
     * */
    public synchronized Map get(Object key){
        if(!store.containsKey(key))
            set(key);
        Map compDataSource = store.get(key);
        List<Map> fields = (List<Map>)compDataSource.get("fields");
        if(fields==null||fields.size()==0)
            return compDataSource;
        /** 将数据列纳入数据源 */
        for(Map f : fields){
            String field_type = (String)f.get("field_type");
            if(compDataSource==null)
                continue;
            String field = null;
            if(Objects.equals(compDataSource.get("data_type"),"table") && Arrays.stream(tableTypedCols).anyMatch(m->m.equals(field_type))){
                if(field_type.equals("table_field")){
                    Integer table_id = (Integer)f.get("table_id");
                    Integer table_col_id = (Integer)f.get("table_col_id");
                    Map tableCol = TableStore.getInstance().getTableCol(table_col_id);
                    if(tableCol==null)
                        continue;
                    field = (String)tableCol.get("field");
                    f.put("data_type", tableCol.get("data_type"));
                    f.put("rel_dict_id", tableCol.get("rel_dict_id"));
                    f.put("tableColObj", tableCol);
                    f.put("tableObj", TableStore.getInstance().get(table_id));
                }
                else if(field_type.equals("flow_field")){
                    Integer flow_edge_id = (Integer)f.get("flow_edge_id");
                    field = Objects.equals(flow_edge_id,-1)?(String)f.get("flow_field"):("f"+flow_edge_id+f.get("flow_field"));
                }
            }
            if(Objects.equals(compDataSource.get("data_type"),"defined") && Arrays.stream(definedTypedCols).anyMatch(m->m.equals(field_type))){
                //如果不符合defined数据源的字段，丢弃
                if(f.get("defined_field")==null){
                    continue;
                }
                field =  (String) f.get("defined_field");
                Integer table_id = (Integer)f.get("table_id");
                Integer table_col_id = (Integer)f.get("table_col_id");
                Map tableCol = TableStore.getInstance().getTableCol(table_col_id);
                if(tableCol==null)
                    continue;
                f.put("data_type", tableCol.get("data_type"));
                f.put("rel_dict_id", tableCol.get("rel_dict_id"));
                f.put("tableColObj", tableCol);
                f.put("tableObj", TableStore.getInstance().get(table_id));
            }
            if(field_type.equals("ds_rows_length")){
                field = "ds_rows_length";
            }
            else if(field_type.equals("ds_total")){
                field = "ds_total";
            }
            else if(field_type.equals("pri_sub_receiver_info")){
                field = "pri_sub_receiver_info";
            }
            f.put("field", field);
            f.put("editors", CompValueEditorStore.getInstance().getByField(f.get("id")));
            //将字段添加到对应的数据源fields中
        }
        return compDataSource;
    }

    public Map getFields(Integer ds_id, Integer ds_field_id){
        // if(ds_id==null) return null;
        Map compDataSource = store.get(ds_id);
        List<Map> fields = (List<Map>) compDataSource.get("fields");
        List<Map> finds = fields.stream().filter(f->Objects.equals(f.get("id"), ds_field_id)).collect(Collectors.toList());
        if(!finds.isEmpty())
            return finds.get(0);
        return null;
    }

//
//    public List<Map> get_ds_fields(BusinessMapper businessMapper, Map datasource){
//        if (datasource.get("data_type")!=null&&datasource.get("data_type").equals("defined")) {
//            return businessMapper.get_defined_datasuorce_fields(Lutils.genMap( "ds_id", datasource.get("id")));
//        } else {
//            return businessMapper.get_table_datasource_fields(Lutils.genMap("ds_id", datasource.get("id")));
//        }
//    }

    public Map get_comp_ds( String obj_type,Integer obj_id){
        Map op_view_ds = null;
        if(obj_type!=null&&obj_id!=null){
            //获取操作视图绑定的数据源
            if(obj_type.equals("CompDataSource")){
                op_view_ds = CompDataSourceStore.getInstance().get("view"+obj_id);
            }
            else if(obj_type.equals("CompGrid")){
                //找到comp_grid
                Map comp_grid = db.selectEq("v_comp_grid", Lutils.genMap("obj_id", obj_id, "obj_type", "view")).get(0);
                op_view_ds = CompDataSourceStore.getInstance().get("comp_grid"+comp_grid.get("id"));
            }
            else if(obj_type.equals("CompReportForms")){
                Map comp_grid = db.selectEq("v_comp_grid", Lutils.genMap("obj_id", obj_id, "obj_type", "view")).get(0);
                op_view_ds = CompDataSourceStore.getInstance().get("comp_grid"+comp_grid.get("id"));
            }
        }
        return op_view_ds;
    }
//
//    public Map get_ds_data(Integer ds_id, Map submitMap, HttpSession session, Boolean needRows, Boolean needTotal) throws Exception {
//        Map compDataSource = this.store.get(ds_id);
//        Object data_type = compDataSource.get("data_type");
//        if(data_type==null)
//            return null;
//        String ac_scope=Lutils.nvl(compDataSource.get("data_access_scope"), "none");
//        submitMap = (Map)Lutils.copy(submitMap==null?new HashMap():submitMap);
//        submitMap.put("data_access_scope", ac_scope);
//        Integer user_id = (Integer)session.getAttribute("userId");
//        submitMap.put("user_id", user_id);
//        List<Map> rows = null;
//        List<Map> submitSessions = submitMap.get("session") == null ? new ArrayList<>() : (List) submitMap.get("session");
//        List<Map> dsFields = (List<Map>) compDataSource.get("fields");
//        boolean containRows = true;
//        if(needRows!=null)
//            containRows = needRows;
//        boolean containTotal = true;
//        if(needTotal!=null){
//            containTotal = needTotal&&Lutils.nvl(compDataSource.get("enable_total"), false);
//        }
//        Map<String, Object> re = new HashMap();
//        re.put("containRows", containRows);
//        re.put("containTotal", containTotal);
//        Map pageInfo = Lutils.genMap(
//                "pageSize", submitMap.get("pageSize")!=null?submitMap.get("pageSize"):compDataSource.get("default_size"),
//                "pageNum", submitMap.get("pageNum")
//        );
//        //自定义SQL
//        if(data_type.equals("defined")&&compDataSource.containsKey("data_sql")){
//            //(不纳入统计)搜索causesObj是前端传上来的，and关系相连
//            Map andSqlObj = submitMap.get("obj_c1")!=null?(Map)submitMap.get("obj_c1"):Lutils.genMap("obj_c1",new HashMap<>());
//            //处理传上来的如日期、数组等值，转化成数据库可识别的值and条件中的所有条件的值
//            if (andSqlObj.containsKey("tp") && andSqlObj.get("tp").equals("a")) {
//                for (Map obj : (List<Map>) andSqlObj.get("cas")) {
//                    BaseDBService.handlerObjectTypedField(obj);
//                }
//            }
//            String data_sql = compDataSource.get("data_sql").toString().replace("#{user_id}",user_id.toString());
//            //替换...z_table123_ids 、 node_
//            data_sql = replaceSql_z_table_ids(data_sql, (List<Map>)submitMap.get("session"));
//            submitMap.put("data_sql", data_sql);
//            boolean has_where = SQLParser.hasWhereClauseInMainQuery(data_sql);
//            submitMap.put("has_where", has_where);
//            if(compDataSource.containsKey("order_sql") && compDataSource.get("order_sql") != null){
//                submitMap.put("order_sql",compDataSource.get("order_sql"));
//            }
//            //需要汇总数和行内容
//            if(containRows){
//                //取行数据，自带取total
//                startPage(pageInfo);
//                rows = db.selectDefinedSql(submitMap);
//                TableDataInfo pageData = new TableDataInfo();
//                pageData.setCode(0);
//                pageData.setRows(rows);
//                if(containTotal)
//                    pageData.setTotal(new PageInfo(rows).getTotal());
//                re.put("page", pageData);
//            }
//            else if(!containRows&&containTotal){  /** 只取汇总数 */
//                //获取自定义脚本的汇总，参数data_sql
//                List<Map> rs = db.selectDefinedSqlCounts(submitMap);
//                if (rs != null && rs.size() > 0)
//                    re.put("page", Lutils.genMap("total", rs.get(0).get("cnt"), "rows", null) );
//                else
//                    re.put("page", Lutils.genMap("total", 0, "rows", null));
//            }
//            else{
//                re.put("page", Lutils.genMap("total", null, "rows", null));
//            }
//
//        }
//        //table模式，根据模版查询
//        else{
//            if (ac_scope.equals("none")) {
//
//            }
//            else{
//                //数据表的列，即table_col_id不为空的列
//                /**根据会话、用户、数据源字段，处理拼接的sql语句及查询范围。*/
//                HandleAndWhere(businessMapper, compDataSource, submitSessions, user_id, submitMap);
//                //根据类型查询
//                if (submitMap.get("orderByColumn") == null) {
//                    submitMap.put("orderByColumn", "id_");
//                    submitMap.put("isAsc", "desc");
//                }
//                if(containRows) {//需要汇总数和行内容
//                    startPage(pageInfo);
//                    rows = businessMapper.get_grid_data(submitMap);
//                    //找出字典相关的列。
//                    List<Map> dsDictFields = dsFields.stream()
//                            .filter(o -> o.get("table_col_id") != null)
//                            .filter(o -> o.get("data_type").equals("integer") && o.get("rel_dict_id") != null)
//                            .collect(Collectors.toList());
//                    //查看列字典选项
//                    List<Map> dictItems = new ArrayList<>();
//                    //循环取字典的值
//                    for (Map dictCol : dsDictFields) {
//                        String field = dictCol.get("field").toString();
//                        List dictItemIds = rows.stream().map(o -> o.get(field)).collect(Collectors.toList());
//                        if (dictItemIds.size() > 0) {
//                            List<Map> dictItem = db.selectIn("v_dict_item", "id", dictItemIds);
//                            dictItems.addAll(dictItem);
//                        }
//                    }
//                    re.put("dictItems", dictItems);
//                    TableDataInfo pageData = new TableDataInfo();
//                    pageData.setCode(0);
//                    pageData.setRows(rows);
//                    if(containTotal)
//                        pageData.setTotal(new PageInfo(rows).getTotal());
//                    re.put("page", pageData);
//                }
//                else if(!containRows&&containTotal){ /** 只取数行数据 */
//                    List<Map> rs = businessMapper.get_grid_data_total(submitMap);
//                    if (rs != null && rs.size() > 0)
//                        re.put("page", Lutils.genMap("total", rs.get(0).get("cnt"), "rows", null));
//                    else
//                        re.put("page", Lutils.genMap("total", 0, "rows", null));
//                }
//                else{
//                    re.put("page", Lutils.genMap("total", null, "rows", null));
//                }
//            }
//        }
//        //如果字段中，包含当前处理信息列，则获取主表的处理信息
//        if(dsFields!=null && dsFields.stream().filter(f->Objects.equals(f.get("field_type"),"pri_sub_receiver_info")).collect(Collectors.toList()).size()>0) {
//            Integer pri_table_id = (Integer) compDataSource.get("table_id");
//            List pri_ids = rows.stream().filter(r -> r.get("z_table" + pri_table_id + "_id") != null).map(r -> r.get("z_table" + pri_table_id + "_id")).collect(Collectors.toList());
//            if(!pri_ids.isEmpty()) {
//                //获取接收人信息
//                List<Map> priRowsReceivers = businessMapper.get_pri_cur_receive_info(Lutils.genMap("pri_table_id", pri_table_id, "pri_ids", pri_ids));
//                //将子流程，存入当前接单人信息中
//                for (Map p : priRowsReceivers) {
//                    //找到rows的行，添加到行
//                    List<Map> findRows = rows.stream().filter(r -> Objects.equals(r.get("z_table" + pri_table_id + "_id"), p.get("z_table" + pri_table_id + "_id"))).collect(Collectors.toList());
//                    //添加到rows
//                    if (findRows.size() > 0) {
//                        findRows.get(0).put("priReceiverInfo", p);
//                    }
//                }
//                //                List<Map> pri_row_flow_infos = businessMapper.get_pri_table_flow_info(Lutils.genMap("pri_table_id", pri_table_id, "pri_ids", pri_ids));
//                List<Integer> pri_nodes = priRowsReceivers.stream().filter(p -> p.get("node_") != null).map(p -> (Integer) p.get("node_")).collect(Collectors.toList());
//                List<Map> subReceiverInfos = new ArrayList<>();
//                //行数据中，存在流程信息，再根据父流程的node，查找子流程的信息
//                if (pri_nodes != null && pri_nodes.size() > 0) {
//                    List<Map> subFlowTables = businessMapper.get_sub_tables_for_get_rows(Lutils.genMap("pri_nodes", pri_nodes));
//                    for (Map subTable : subFlowTables) {
//                        Integer sub_table_id = (Integer) subTable.get("sub_flow_table_id");
//                        String sub_table_name = (String) subTable.get("sub_table_name");
//                        //查子流程的表列, 获取列属性
//                        List<Map> subReceiverInfo = businessMapper.get_pri_rel_sub_receive_info(Lutils.genMap("pri_table_id", pri_table_id, "sub_table_id", sub_table_id, "pri_ids", pri_ids));
//                        for (Map s : subReceiverInfo) {
//                            s.put("sub_table_id", sub_table_id);
//                            s.put("sub_table_name", sub_table_name);
//                            subReceiverInfos.add(s);
//                        }
//                    }
//                    //将子流程，存入当前接单人信息中
//                    for (Map row : rows) {
//                       row.put("subReceiverInfo",subReceiverInfos.stream().filter(s->Objects.equals(s.get("z_table"+pri_table_id+"_id"), row.get("z_table"+pri_table_id+"_id"))).collect(Collectors.toList()) );
//                    }
//                }
//            }
//        }
//        return re;
//    }

//    //获取所都ds数据
//    public Map get_all_ds_data(Integer ds_id, Map submitMap, HttpSession session, Boolean needRows, Boolean needTotal) throws Exception {
//        Map compDataSource = this.store.get(ds_id);
//        Object data_type = compDataSource.get("data_type");
//        if(data_type==null)
//            return null;
//        String ac_scope=Lutils.nvl(compDataSource.get("data_access_scope"), "none");
//        submitMap = (Map)Lutils.copy(submitMap==null?new HashMap():submitMap);
//        submitMap.put("data_access_scope", ac_scope);
//        Integer user_id = (Integer)session.getAttribute("userId");
//        submitMap.put("user_id", user_id);
//        List<Map> rows = null;
//        List<Map> submitSessions = submitMap.get("session") == null ? new ArrayList<>() : (List) submitMap.get("session");
//        List<Map> dsFields = (List<Map>) compDataSource.get("fields");
//        boolean containRows = true;
//        if(needRows!=null)
//            containRows = needRows;
//        boolean containTotal = true;
//        if(needTotal!=null){
//            containTotal = needTotal&&Lutils.nvl(compDataSource.get("enable_total"), false);
//        }
//        Map<String, Object> re = new HashMap();
//        re.put("containRows", containRows);
//        re.put("containTotal", containTotal);
//        Map pageInfo = Lutils.genMap(
//                "pageSize", submitMap.get("pageSize")!=null?submitMap.get("pageSize"):compDataSource.get("default_size"),
//                "pageNum", submitMap.get("pageNum")
//        );
//        //自定义SQL
//        if(data_type.equals("defined")&&compDataSource.containsKey("data_sql")){
//            //(不纳入统计)搜索causesObj是前端传上来的，and关系相连
//            Map andSqlObj = submitMap.get("obj_c1")!=null?(Map)submitMap.get("obj_c1"):Lutils.genMap("obj_c1",new HashMap<>());
//            //处理传上来的如日期、数组等值，转化成数据库可识别的值and条件中的所有条件的值
//            if (andSqlObj.containsKey("tp") && andSqlObj.get("tp").equals("a")) {
//                for (Map obj : (List<Map>) andSqlObj.get("cas")) {
//                    BaseDBService.handlerObjectTypedField(obj);
//                }
//            }
//            String data_sql = compDataSource.get("data_sql").toString().replace("#{user_id}",user_id.toString());
//            //替换...z_table123_ids 、 node_
//            data_sql = replaceSql_z_table_ids(data_sql, (List<Map>)submitMap.get("session"));
//            submitMap.put("data_sql", data_sql);
//            boolean has_where = SQLParser.hasWhereClauseInMainQuery(data_sql);
//            submitMap.put("has_where", has_where);
//            if(compDataSource.containsKey("order_sql") && compDataSource.get("order_sql") != null){
//                submitMap.put("order_sql",compDataSource.get("order_sql"));
//            }
//            //需要汇总数和行内容
//            if(containRows){
//                //取行数据，自带取total
////                startPage(pageInfo);
//                rows = db.selectDefinedSql(submitMap);
//                TableDataInfo pageData = new TableDataInfo();
//                pageData.setCode(0);
//                pageData.setRows(rows);
//                if(containTotal)
//                    pageData.setTotal(new PageInfo(rows).getTotal());
//                re.put("page", pageData);
//            }
//            else if(!containRows&&containTotal){  /** 只取汇总数 */
//                //获取自定义脚本的汇总，参数data_sql
//                List<Map> rs = db.selectDefinedSqlCounts(submitMap);
//                if (rs != null && rs.size() > 0)
//                    re.put("page", Lutils.genMap("total", rs.get(0).get("cnt"), "rows", null) );
//                else
//                    re.put("page", Lutils.genMap("total", 0, "rows", null));
//            }
//            else{
//                re.put("page", Lutils.genMap("total", null, "rows", null));
//            }
//
//        }
//        //table模式，根据模版查询
//        else{
//            if (ac_scope.equals("none")) {
//
//            }
//            else{
//                //数据表的列，即table_col_id不为空的列
//                /**根据会话、用户、数据源字段，处理拼接的sql语句及查询范围。*/
//                HandleAndWhere(businessMapper, compDataSource, submitSessions, user_id, submitMap);
//                //根据类型查询
//                if (submitMap.get("orderByColumn") == null) {
//                    submitMap.put("orderByColumn", "id_");
//                    submitMap.put("isAsc", "desc");
//                }
//                if(containRows) {//需要汇总数和行内容
////                    startPage(pageInfo);
//                    rows = businessMapper.get_grid_data(submitMap);
//                    //找出字典相关的列。
//                    List<Map> dsDictFields = dsFields.stream()
//                            .filter(o -> o.get("table_col_id") != null)
//                            .filter(o -> o.get("data_type").equals("integer") && o.get("rel_dict_id") != null)
//                            .collect(Collectors.toList());
//                    //查看列字典选项
//                    List<Map> dictItems = new ArrayList<>();
//                    //循环取字典的值
//                    for (Map dictCol : dsDictFields) {
//                        String field = dictCol.get("field").toString();
//                        List dictItemIds = rows.stream().map(o -> o.get(field)).collect(Collectors.toList());
//                        if (dictItemIds.size() > 0) {
//                            List<Map> dictItem = db.selectIn("v_dict_item", "id", dictItemIds);
//                            dictItems.addAll(dictItem);
//                        }
//                    }
//                    re.put("dictItems", dictItems);
//                    TableDataInfo pageData = new TableDataInfo();
//                    pageData.setCode(0);
//                    pageData.setRows(rows);
//                    if(containTotal)
//                        pageData.setTotal(new PageInfo(rows).getTotal());
//                    re.put("page", pageData);
//                }
//                else if(!containRows&&containTotal){ /** 只取数行数据 */
//                    List<Map> rs = businessMapper.get_grid_data_total(submitMap);
//                    if (rs != null && rs.size() > 0)
//                        re.put("page", Lutils.genMap("total", rs.get(0).get("cnt"), "rows", null));
//                    else
//                        re.put("page", Lutils.genMap("total", 0, "rows", null));
//                }
//                else{
//                    re.put("page", Lutils.genMap("total", null, "rows", null));
//                }
//            }
//        }
//        //如果字段中，包含当前处理信息列，则获取主表的处理信息
//        if(dsFields!=null && dsFields.stream().filter(f->Objects.equals(f.get("field_type"),"pri_sub_receiver_info")).collect(Collectors.toList()).size()>0) {
//            Integer pri_table_id = (Integer) compDataSource.get("table_id");
//            List pri_ids = rows.stream().filter(r -> r.get("z_table" + pri_table_id + "_id") != null).map(r -> r.get("z_table" + pri_table_id + "_id")).collect(Collectors.toList());
//            if(!pri_ids.isEmpty()) {
//                //获取接收人信息
//                List<Map> priRowsReceivers = businessMapper.get_pri_cur_receive_info(Lutils.genMap("pri_table_id", pri_table_id, "pri_ids", pri_ids));
//                //将子流程，存入当前接单人信息中
//                for (Map p : priRowsReceivers) {
//                    //找到rows的行，添加到行
//                    List<Map> findRows = rows.stream().filter(r -> Objects.equals(r.get("z_table" + pri_table_id + "_id"), p.get("z_table" + pri_table_id + "_id"))).collect(Collectors.toList());
//                    //添加到rows
//                    if (findRows.size() > 0) {
//                        findRows.get(0).put("priReceiverInfo", p);
//                    }
//                }
//                //                List<Map> pri_row_flow_infos = businessMapper.get_pri_table_flow_info(Lutils.genMap("pri_table_id", pri_table_id, "pri_ids", pri_ids));
//                List<Integer> pri_nodes = priRowsReceivers.stream().filter(p -> p.get("node_") != null).map(p -> (Integer) p.get("node_")).collect(Collectors.toList());
//                List<Map> subReceiverInfos = new ArrayList<>();
//                //行数据中，存在流程信息，再根据父流程的node，查找子流程的信息
//                if (pri_nodes != null && pri_nodes.size() > 0) {
//                    List<Map> subFlowTables = businessMapper.get_sub_tables_for_get_rows(Lutils.genMap("pri_nodes", pri_nodes));
//                    for (Map subTable : subFlowTables) {
//                        Integer sub_table_id = (Integer) subTable.get("sub_flow_table_id");
//                        String sub_table_name = (String) subTable.get("sub_table_name");
//                        //查子流程的表列, 获取列属性
//                        List<Map> subReceiverInfo = businessMapper.get_pri_rel_sub_receive_info(Lutils.genMap("pri_table_id", pri_table_id, "sub_table_id", sub_table_id, "pri_ids", pri_ids));
//                        for (Map s : subReceiverInfo) {
//                            s.put("sub_table_id", sub_table_id);
//                            s.put("sub_table_name", sub_table_name);
//                            subReceiverInfos.add(s);
//                        }
//                    }
//                    //将子流程，存入当前接单人信息中
//                    for (Map row : rows) {
//                        row.put("subReceiverInfo",subReceiverInfos.stream().filter(s->Objects.equals(s.get("z_table"+pri_table_id+"_id"), row.get("z_table"+pri_table_id+"_id"))).collect(Collectors.toList()) );
//                    }
//                }
//            }
//        }
//        return re;
//    }


//    //获取ds数据的ids
//    //获取所都ds数据
//    public Map get_ds_data_ids(Integer ds_id,BusinessMapper businessMapper, BaseDBService baseDBService, Map submitMap, Integer user_id, DSHasSettings settings) throws Exception {
//
//        Map compDataSource = this.store.get(ds_id);
//        submitMap = (Map)Lutils.copy(submitMap==null?new HashMap():submitMap);
//        //user_id
//        submitMap.put("user_id", user_id);
//        //获取表格域
//        List<Map> subSessionList = submitMap.get("session")==null?new ArrayList<>():(List) submitMap.get("session");
//        /**获取数据源配置*/
//        //数据源的列
//        List<Map> dataSourceFields = (List<Map>) compDataSource.get("fields");
//        if(Objects.equals(compDataSource.get("enable_counting"),true)==false){
//            settings.has_count = false;
//        }
//        if(Objects.equals(compDataSource.get("enable_total"),true)==false){
//            settings.has_total = false;
//        }
//
//        //返回数据
//        Map<String, Object> re = new HashMap();
//        Integer countingNum;
//        Integer totalNum;
//        //准备取数
//        List<Map> rows = new ArrayList<>();
//        //如果自定义sql，袭击者执行sql
//        String dt_type=Lutils.nvl(compDataSource.get("data_type"), "all");
//        String ac_scope=Lutils.nvl(compDataSource.get("data_access_scope"), "none");
//        submitMap.put("data_access_scope", ac_scope);
//        //自定义SQL
//        if(dt_type.equals("defined")){
//            if(compDataSource.containsKey("data_sql")){
//                //分页配置
//                //(不纳入统计)搜索causesObj是前端传上来的，and关系相连
//                Map andSqlObj = submitMap.get("obj_c1")!=null?(Map)submitMap.get("obj_c1"):Lutils.genMap("obj_c1",new HashMap<>());
//                //处理传上来的如日期、数组等值，转化成数据库可识别的值and条件中的所有条件的值
//                if (andSqlObj.containsKey("tp") && andSqlObj.get("tp").equals("a")) {
//                    for (Map obj : (List<Map>) andSqlObj.get("cas")) {
//                        BaseDBService.handlerObjectTypedField(obj);
//                    }
//                }
//                String data_sql = compDataSource.get("data_sql").toString().replace("#{user_id}",user_id.toString());
//                //替换...z_table123_ids 、 node_
//                data_sql = replaceSql_z_table_ids(data_sql, (List<Map>)submitMap.get("session"));
//                submitMap.put("data_sql",data_sql);
//                boolean has_where = SQLParser.hasWhereClauseInMainQuery(data_sql);
//                submitMap.put("has_where",has_where);
//                if(compDataSource.containsKey("order_sql") && compDataSource.get("order_sql") != null){
//                    submitMap.put("order_sql", compDataSource.get("order_sql"));
//                }
//                //取行数据，自带取total
//                if(settings.has_rows) {
////                    startPage(submitMap);
//                    rows = baseDBService.selectDefinedSql(submitMap);
////                    TableDataInfo pageData = new TableDataInfo();
////                    pageData.setCode(0);
////                    pageData.setRows(rows);
////                    pageData.setTotal(new PageInfo(rows).getTotal());
////                    re.put("page", pageData);
//                    re.put("rows", rows);
//                    re.put("ids",Lutils.getColumnValueList(rows,"id_"));
//                }
//                //如果不需要行数据，total数据需要单独获取
//                if(!settings.has_rows && settings.has_total){
//                    //获取自定义脚本的汇总，参数data_sql
//                    List<Map> rs = baseDBService.selectDefinedSqlCounts(submitMap);
//                    if (rs != null && rs.size() > 0)
//                        re.put("page", Lutils.genMap("total", rs.get(0).get("cnt")) );
//                    else
//                        re.put("page", Lutils.genMap("total", null) );
//                }
//                //查询通知统计计数
//                if (settings.has_count && Objects.equals( compDataSource.get("enable_counting"), true)) {
//                    String count_sql = Lutils.nvl( compDataSource.get("count_sql"),"");
//                    if(!count_sql.contains("select")||!count_sql.contains("from")||!count_sql.contains("cnt"))
//                        throw new Exception("汇总脚本的SQL不合法，必须包含select、from、cnt等关键字");
//                    //替换...z_table123_ids 、 node_
//                    count_sql = replaceSql_z_table_ids(count_sql, (List<Map>)submitMap.get("session"));
//                    List<Map> rs = baseDBService.selectDefinedSql(Lutils.genMap("data_sql", count_sql,"user_id", user_id));
//                    if (rs != null && rs.size() > 0)
//                        re.put("cnt", rs.get(0).get("cnt"));
//                    else
//                        re.put("cnt", null);
//                }
//            }
//        }
//        //根据模版查询
//        else{
//            if (!ac_scope.equals("none")) {
//                //数据表的列，即table_col_id不为空的列
//                /**根据会话、用户、数据源字段，处理拼接的sql语句及查询范围。*/
//                HandleAndWhere(businessMapper, compDataSource, subSessionList, user_id, submitMap);
//                //根据类型查询
//                if (submitMap.get("orderByColumn") == null) {
//                    submitMap.put("orderByColumn", "id_");
//                    submitMap.put("isAsc", "desc");
//                }
//                if(settings.has_rows) {
//                    //分页配置
////                    startPage(submitMap);
//                    rows = businessMapper.get_grid_data(submitMap);
//                    //找出字典相关的列。
//                    List<Map> dsDictFields = dataSourceFields.stream()
//                            .filter(o -> o.get("table_col_id") != null)
//                            .filter(o -> o.get("data_type").equals("integer") && o.get("rel_dict_id") != null)
//                            .collect(Collectors.toList());
//                    //查看列字典选项
//                    List<Map> dictItems = new ArrayList<>();
//                    //循环取字典的值
//                    for (Map dictCol : dsDictFields) {
//                        String field = dictCol.get("field").toString();
//                        List dictItemIds = rows.stream().map(o -> o.get(field)).collect(Collectors.toList());
//                        if (dictItemIds.size() > 0) {
//                            List<Map> dictItem = baseDBService.selectIn("v_dict_item", "id", dictItemIds);
//                            dictItems.addAll(dictItem);
//                        }
//                    }
//                    re.put("rows", rows);
//                    re.put("ids",Lutils.getColumnValueList(rows,"id_"));
//                }
//                //如果不需要行数据，total数据需要单独获取
//                if(!settings.has_rows && settings.has_total){
//                    //获取自定义脚本的汇总
//                    List<Map> rs = businessMapper.get_grid_data_total(submitMap);
//                    if (rs != null && rs.size() > 0)
//                        re.put("cnt", rs.get(0).get("cnt"));
//                    else
//                        re.put("cnt", null);
//                }
//                //查询通知统计计数
//                String count_sql = Lutils.nvl(compDataSource.get("count_sql"),"");
//                if (settings.has_count && Objects.equals(compDataSource.get("enable_counting"), true) && count_sql.length()>0) {
//                    submitMap.put("count_sql", count_sql);
//                    List<Map> rs = businessMapper.get_grid_data_count(submitMap);
//                    if (rs != null && rs.size() > 0)
//                        re.put("cnt", rs.get(0).get("cnt"));
//                    else
//                        re.put("cnt", null);
//                }
//            }
//        }
//        //如果字段中，包含当前处理信息列，则获取主表的处理信息
//        if(dataSourceFields!=null && dataSourceFields.stream().filter(f->Objects.equals(f.get("field_type"),"pri_sub_receiver_info")).collect(Collectors.toList()).size()>0) {
//            Integer pri_table_id = (Integer) compDataSource.get("table_id");
//            List pri_ids = rows.stream().filter(r -> r.get("z_table" + pri_table_id + "_id") != null).map(r -> r.get("z_table" + pri_table_id + "_id")).collect(Collectors.toList());
//            if(!pri_ids.isEmpty()) {
//                List<Map> pri_row_flow_infos = businessMapper.get_pri_table_flow_info(Lutils.genMap("pri_table_id", pri_table_id, "pri_ids", pri_ids));
//                List<Integer> pri_nodes = pri_row_flow_infos.stream().filter(p -> p.get("node_") != null).map(p -> (Integer) p.get("node_")).collect(Collectors.toList());
//                //行数据中，存在流程信息，再根据父流程的node，查找子流程的信息
//                if (pri_nodes != null && pri_nodes.size() > 0) {
//                    List<Map> priReceiverInfo = businessMapper.get_pri_cur_receive_info(Lutils.genMap("pri_table_id", pri_table_id, "pri_ids", pri_ids));
//                    List<Map> subTables = businessMapper.get_sub_tables_for_get_rows(Lutils.genMap("pri_nodes", pri_nodes));
//                    for (Map subTable : subTables) {
//                        Integer sub_table_id = (Integer) subTable.get("sub_flow_table_id");
//                        String sub_table_name = (String) subTable.get("sub_table_name");
//                        //查子流程的表列, 获取列属性
//                        if (pri_ids.size() > 0) {
//
//                            List<Map> subReceiverInfo = businessMapper.get_pri_rel_sub_receive_info(Lutils.genMap("pri_table_id", pri_table_id, "sub_table_id", sub_table_id, "pri_ids", pri_ids));
//                            for (Map s : subReceiverInfo) {
//                                s.put("sub_table_id", sub_table_id);
//                                s.put("sub_table_name", sub_table_name);
//                            }
//                            //将子流程，存入当前接单人信息中
//                            for (Map p : priReceiverInfo) {
//                                p.put("sub_receiver_info", subReceiverInfo.stream()
//                                        .filter(s -> Objects.equals(s.get("z_table" + pri_table_id + "_id"), p.get("z_table" + pri_table_id + "_id"))
//                                                && Objects.equals(s.get("pri_node"), p.get("pri_node"))
//                                        ).collect(Collectors.toSet()));
//                            }
//                        }
//                    }
//                    //将子流程，存入当前接单人信息中
//                    for (Map p : priReceiverInfo) {
//                        //找到rows的行，添加到行
//                        List<Map> findRows = rows.stream().filter(r -> Objects.equals(r.get("z_table" + pri_table_id + "_id"), p.get("z_table" + pri_table_id + "_id"))).collect(Collectors.toList());
//                        //添加到rows
//                        if (findRows.size() > 0) {
//                            findRows.get(0).put("pri_sub_receiver_info", p);
//                        }
//                    }
//                }
//            }
//        }
//        re.put("settings", settings);
//        return re;
//    }

    public String replaceSql_z_table_ids(String upd_sql,List<Map> session){
        if(session==null)
            return upd_sql;
        Pattern pattern = Pattern.compile("\\.\\.\\.z_table\\d+_ids");
        Matcher matcher = pattern.matcher(upd_sql);
        while (matcher.find()) {
            System.out.println("Found '" + matcher.group() + "' at position " + matcher.start() + " to " + matcher.end());
            String z_table_ids_param = matcher.group();
            Integer table_id = Integer.parseInt(z_table_ids_param.replace('.', ' ').replace("z_table","").replace("_ids", "").trim());
            List limitIds = session.stream().filter(o->Objects.equals(o.get("table_id"), table_id))
                    .map(o->o.get("id_")).collect(Collectors.toList());
            //替换sql语句
            upd_sql = upd_sql.replace(z_table_ids_param, StringUtils.join(limitIds, ","));
        }
        return upd_sql;
    }

    public void HandleAndWhere(BusinessMapper businessMapper, Map<String,Object> dataSource, List<Map> subSessionList, Integer user_id, Map map){
        //数据源的列
        List<Map> dataSourceFields = (List<Map>) dataSource.get("fields");
        //数据表的列，即table_col_id不为空的列
        List<Map> dataTableCols = dataSourceFields.stream().filter(o -> o.get("table_col_id")!=null).collect(Collectors.toList());
        //主数据表id
        Integer ds_table_id = (Integer) dataSource.get("table_id");
        //找到主表格与依赖关系
        List<Map> leftJoinForeignTables = businessMapper.get_table_recurive_relations(Lutils.genMap("table_id", ds_table_id));
        //当前表的用户列、发件人、发件时间等，默认提供。不需要额外处理
        List<Map> curFlowEdgeCols = dataSourceFields.stream()
                .filter(o -> o.get("flow_edge_id") != null && o.get("flow_edge_id").equals(-1)).collect(Collectors.toList());
        //某个动作的时间列
        List<Map> selFlowEdgePostedTimeCols = dataSourceFields.stream()
                .filter(o -> o.get("flow_edge_id") != null && (Integer) o.get("flow_edge_id") > 0 && Lutils.nvl((String) o.get("flow_field"), "").contains("time"))
                .collect(Collectors.toList());
        //某个动作的派发人员、号码等列
        List<Map> selFlowEdgePosterCols = dataSourceFields.stream()
                .filter(o -> o.get("flow_edge_id") != null && (Integer) o.get("flow_edge_id") > 0 && Lutils.nvl((String) o.get("flow_field"), "").contains("poster_"))
                .collect(Collectors.toList());
        //某个动作的接受人员、号码等列
        List<Map> selFlowEdgeReceiverCols = dataSourceFields.stream()
                .filter(o -> o.get("flow_edge_id") != null && (Integer) o.get("flow_edge_id") > 0 && Lutils.nvl((String) o.get("flow_field"), "").contains("receiver_"))
                .collect(Collectors.toList());
        //选定动作的flow_edge_id集合
        List<Integer> selFlowEdgeIds = dataSourceFields.stream()
                .filter(o -> o.get("flow_edge_id") != null && (Integer) o.get("flow_edge_id") > 0)
                .map(o -> (Integer) o.get("flow_edge_id")).collect(Collectors.toList());
        Set leftJoinFlowEdgeTables = new HashSet(selFlowEdgeIds);
        //拼接sql语句
        //剔除自定义列，自定义列的table_col_id是空的
        map.put("table_id", ds_table_id);
        map.put("dataTableCols", dataTableCols);
        //当前操作人和操作时间
        map.put("curFlowEdgeCols", curFlowEdgeCols);
        //指定动作的操作人和操作时间
        map.put("selFlowEdgePostedTimeCols", selFlowEdgePostedTimeCols);
        map.put("selFlowEdgePosterCols", selFlowEdgePosterCols);
        map.put("selFlowEdgeReceiverCols", selFlowEdgeReceiverCols);
        map.put("leftJoinFlowEdgeTables", leftJoinFlowEdgeTables);

        //(不纳入统计)搜索causesObj是前端传上来的，and关系相连
        Map andSqlObj = map.get("obj_c1")!=null?(Map)map.get("obj_c1"):Lutils.genMap("obj_c1",new HashMap<>());
        //处理传上来的如日期、数组等值，转化成数据库可识别的值and条件中的所有条件的值
        if (andSqlObj.containsKey("tp") && andSqlObj.get("tp").equals("a")) {
            for (Map obj : (List<Map>) andSqlObj.get("cas")) {
                BaseDBService.handlerObjectTypedField(obj);
            }
        }
        // 会话过滤（不纳入统计）增补会话筛选。如果session有数据，则提交表格域限制条件，则添加权限筛选条件
        //记录需要过滤的外联表，用于后续优化sql
        List<Integer> filterSessionForeignTableIds = new ArrayList<>();
        Integer session_table_id = (Integer) dataSource.get("limit_session_table_id");
        if(session_table_id != null){
            //到会话中查找
            List<Integer> whereIds = subSessionList.stream()
                    .filter(o->o.get("table_id").equals(session_table_id))
                    .map(o->(Integer)o.get("id_")).collect(Collectors.toList());
            if(whereIds==null||whereIds.size()==0) {
                whereIds = Lutils.genList(-1);
            }
            //增加变andSqlObj
            andSqlObj = appendToAndCause(andSqlObj, SqlUtil.in("t" + session_table_id + ".id_", whereIds));
            filterSessionForeignTableIds.add(session_table_id);
        }
        //优化：自带的和筛选条件都不涉及的话，不关联查询相关的外联表
        int foreignScopeIndex = -1;
        for(int i=0; i < leftJoinForeignTables.size(); i ++ ) {
            Integer foreign_table_id = (Integer) leftJoinForeignTables.get(i).get("foreign_table_id");
            //如果查询的字段涉及外联表的字段，或where条件涉及外联表的字段，则保留外联表的left join 关系
            if(dataSourceFields.stream().filter(f->f.get("table_id")!=null).map(f->f.get("table_id"))
                    .collect(Collectors.toList()).contains(foreign_table_id)
                    || filterSessionForeignTableIds.contains(foreign_table_id)){
                foreignScopeIndex = i;
            }
        }
        leftJoinForeignTables = leftJoinForeignTables.subList(0, foreignScopeIndex+1);
        map.put("leftJoinForeignTables", leftJoinForeignTables);

//        //数据获取权限
//        if (dataSource.containsKey("data_access_scope")&&
//                dataSource.get("data_access_scope").toString().equals("defined")) {
//            List ids_ = serviceConfigService.get_view_grid_data_permission_map(dataSource, user_id,null,null);
//            if(ids_.size()==0){
//                ids_.add(-1);
//            }
//            map.put("obj_in_ids",SqlUtil.in("t" + ds_table_id + ".id_", ids_));
//        }

        map.put("obj_c1", andSqlObj);
    }

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

    public boolean isRowField(Object field_type){
        return Objects.equals(field_type, "flow_field") ||Objects.equals(field_type,"table_field")
                ||Objects.equals(field_type,"defined_field");
    }
}
