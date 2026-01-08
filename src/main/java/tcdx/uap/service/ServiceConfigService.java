package tcdx.uap.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import tcdx.uap.common.utils.Lutils;
import tcdx.uap.common.utils.SqlUtil;
import tcdx.uap.mapper.BaseDBMapper;
import tcdx.uap.mapper.ServiceConfigMapper;
import tcdx.uap.mapper.BusinessMapper;
import tcdx.uap.service.entities.*;
import tcdx.uap.service.store.CompValueRenderStore;
import tcdx.uap.service.store.DSStore;
import tcdx.uap.service.store.Modules;

import javax.servlet.http.HttpSession;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 参数配置 服务层实现
 * 
 * @author ruoyi
 */
@Service
public class ServiceConfigService {

    @Autowired
    private ServiceConfigMapper serviceConfigMapper;

    @Autowired
    BaseDBMapper baseDBMapper;

    @Autowired
    BaseDBService baseDBService;

    @Autowired
    private BusinessMapper businessMapper;
    @Autowired
    private BusinessService businessService;

    public List<Map> get_view_grid_cols(Map view){
        List table_ids = new ArrayList();
        table_ids.add(view.get("comp_table_id"));
        //找到主表格与依赖关系
        List<Map> joinDataTables = businessMapper.get_table_recurive_relations(Lutils.genMap("table_id", view.get("comp_table_id")));
        table_ids.addAll(joinDataTables.stream().map(o->o.get("foreign_table_id")).collect(Collectors.toList()));
        return serviceConfigMapper.get_view_grid_cols(Lutils.genMap("view_id", view.get("id"),"table_ids", table_ids));
    }

    public List<Map> get_view_grid_cols(Integer view_id){
        Map view = baseDBService.selectEq("v_view", Lutils.genMap("id", view_id)).get(0);
        List table_ids = new ArrayList();
        table_ids.add(view.get("comp_table_id"));
        //找到主表格与依赖关系
        List<Map> joinDataTables = businessMapper.get_table_recurive_relations(Lutils.genMap("table_id", view.get("comp_table_id")));
        table_ids.addAll(joinDataTables.stream().map(o->o.get("foreign_table_id")).collect(Collectors.toList()));
        return serviceConfigMapper.get_view_grid_cols(Lutils.genMap("view_id", view.get("id"),"table_ids", table_ids));
    }

    public List get_view_grid_data_permission_map(Map dataSource,int user_id,String columnName,String query) {
        Map userInfo = baseDBService.selectEq("v_user",Lutils.genMap("id",user_id)).get(0);
        Integer ds_id = (Integer) dataSource.get("id");
        Integer ds_table_id = (Integer) dataSource.get("table_id");
        List<Map> edgeCauseList = new ArrayList<>();
        List<Map> andCauseList = new ArrayList<>();
        List<Map> andFlowCauseList = new ArrayList<>();

        List<Map> gridDataScope = baseDBService.selectEq("v_grid_data_scope", Lutils.genMap("ds_id", ds_id));
        Integer belong_group_id = (Integer)userInfo.get("belong_group_id");
        List<Map> groupUsers = baseDBService.selectEq("v_user_group", Lutils.genMap("user_id", (Integer)userInfo.get("id")));
         List groupStaffIds = Lutils.getColumnValueList(groupUsers, "user_id");
         List<Integer> edges_ = new ArrayList<>();
        List<Integer> posters_ = new ArrayList<>();
        List<Integer> receivers_ = new ArrayList<>();
        for (int i = 0; i < gridDataScope.size(); i++) {
            Map cause = new HashMap();
            Map scope = gridDataScope.get(i);
            Integer edge_id = (Integer)scope.get("flow_edge_id");
            edges_.add(edge_id);
//            edgeCauseList.add(Lutils.genMap("tp","eq","col","edge_","val",node_id));
            if(!scope.containsKey("contains_flow_action") || scope.get("contains_flow_action").equals(false)){
                if(scope.containsKey("my_posted") && scope.get("my_posted").equals(true)) {
                    posters_.add((Integer)userInfo.get("id"));
//                    andCauseList.add(Lutils.genMap("tp","eq","col","poster_","val",(Integer)userInfo.get("id")));
                }
                if(scope.containsKey("my_received") && scope.get("my_received").equals(true)) {
                    receivers_.add((Integer)userInfo.get("id"));
                    //             andCauseList.add(Lutils.genMap("tp","eq","col","receiver_","val",(Integer)userInfo.get("id")));
                }
                if(scope.containsKey("group_posted") && scope.get("group_posted").equals(true)) {
                    posters_.addAll(groupStaffIds);
                    //    andCauseList.add(Lutils.genMap("tp","in","col","poster_","vals",groupStaffIds));
                }
                if(scope.containsKey("group_received") && scope.get("group_received").equals(true)) {
                    receivers_.addAll(groupStaffIds);
                    //     andCauseList.add(Lutils.genMap("tp","in","col","receiver_","vals",groupStaffIds));
                }
            }else{
                if(scope.containsKey("my_posted") && scope.get("my_posted").equals(true)) {
                    posters_.add((Integer)userInfo.get("id"));
                    //    andFlowCauseList.add(Lutils.genMap("tp","eq","col","poster_","val",(Integer)userInfo.get("id")));
                }
                if(scope.containsKey("my_received") && scope.get("my_received").equals(true)) {
                    receivers_.add((Integer)userInfo.get("id"));
                    //     andFlowCauseList.add(Lutils.genMap("tp","eq","col","receiver_","val",(Integer)userInfo.get("id")));
                }
                if(scope.containsKey("group_posted") && scope.get("group_posted").equals(true)) {
                    posters_.addAll(groupStaffIds);
                    //    andFlowCauseList.add(Lutils.genMap("tp","in","col","poster_","vals",groupStaffIds));
                }
                if(scope.containsKey("group_received") && scope.get("group_received").equals(true)) {
                    receivers_.addAll(groupStaffIds);
                    //   andFlowCauseList.add(Lutils.genMap("tp","in","col","receiver_","vals",groupStaffIds));
                }
            }

        }
        List ids_ = new ArrayList();
        if(edges_.size()>0){
            edges_ = edges_.stream().distinct().collect(Collectors.toList());
            andCauseList.add(Lutils.genMap("tp","in","col","edge_","vals",edges_));
        }
        if(posters_.size()>0){
            posters_ = posters_.stream().distinct().collect(Collectors.toList());
            andCauseList.add(Lutils.genMap("tp","in","col","poster_","vals",posters_));
        }
        if(receivers_.size()>0){
            receivers_ = receivers_.stream().distinct().collect(Collectors.toList());
            andCauseList.add(Lutils.genMap("tp","in","col","receiver_","vals",receivers_));
        }
        List<Map> maps = baseDBService.selectByCauses("z_" + ds_table_id,
                Lutils.genMap("tp","a","cas",andCauseList),
                null);
        ids_.addAll( Lutils.getColumnValueList(maps,"id_") );
//        if(andCauseList.size()>0 && andFlowCauseList.size()==0){
//            List<Map> maps = baseDBService.selectAndOrByCauses("z_table" + ds_table_id,
//                    Lutils.genMap("tp","a","cas",edgeCauseList),
//                    Lutils.genMap("tp","o","cas",andCauseList),
//                    null);
//            ids_.addAll( Lutils.getColumnValueList(maps,"id_") );
//        }else{
//            List<Map> maps = baseDBService.selectAndOrByCauses("z_table" + ds_table_id + "_flow",
//                    Lutils.genMap("tp","a","cas",edgeCauseList),
//                    Lutils.genMap("tp","o","cas",andFlowCauseList),
//                    null);
//            ids_.addAll( Lutils.getColumnValueList(maps,"row_id_") );
//
//        }
        System.out.println(ids_);
        return ids_;
    }

    public List<Map> getSearchGridCols(List<Map> gridCols,Integer comp_id) {
        List<Map> copyGridCols  = new ArrayList<>();
        if(gridCols==null || gridCols.size()==0){return gridCols;}
        for (int i = 0; i < gridCols.size(); i++) {
            Map gridColMap = gridCols.get(i);
            Map compValueRender = CompValueRenderStore.getInstance().get("comp_grid_col" + gridColMap.get("id"));
            if(compValueRender==null)
                continue;
            String columnName="";
            if(compValueRender.get("field")!=null)
                columnName = (String)compValueRender.get("field");

            boolean flag = false;
            if(columnName.indexOf("node")!=-1 || columnName.indexOf("edge")!=-1 ||
                    columnName.indexOf("poster")!=-1 || columnName.indexOf("receiver")!=-1){
                flag = true;
            }
            // 提取 t 后的数字
            Pattern pattern = Pattern.compile("^t(\\d+)_");
            Matcher matcher = pattern.matcher(columnName);

            if (matcher.find()) {
                String numberStr = matcher.group(1); // 捕获组 1：\\d+ 的部分
                int number = Integer.parseInt(numberStr);
                System.out.println("提取的数字: " + number); // 输出: 47
                compValueRender.put("table_id", number);
            } else {
                System.out.println("格式不匹配");
                if(compValueRender.containsKey("ds_id")){
                    Integer ds_id = (Integer) compValueRender.get("ds_id");
//                    List<Map> maps = baseDBService.selectEq("v_datasource", Lutils.genMap("id", ds_id));
//                    Integer table_id = (Integer) maps.get(0).get("table_id");
//                    compValueRender.put("table_id",table_id);
                    compValueRender.put("table_id", DSStore.getInstance().get(ds_id).get("table_id"));
                }
            }
            String columnType = compValueRender.get("render_type").toString();
            if(columnType.equals("datetime")){
                gridColMap.put("data_type","timestamp");
                compValueRender.put("data_type","timestamp");
                Integer table_id  = (Integer) compValueRender.get("table_id");
                compValueRender.put("table_id",table_id);
            }else if(columnType.equals("text")){
                if(!compValueRender.containsKey("data_type")){
                    compValueRender.put("data_type","varchar");
                    gridColMap.put("data_type","varchar");
                }
                Integer table_id  = (Integer) compValueRender.get("table_id");
                compValueRender.put("table_id",table_id);
            }else if (columnType.equals("dict")){
                gridColMap.put("data_type","dict");
                compValueRender.put("data_type","dict");
                Integer table_id  = (Integer) compValueRender.get("table_id");
                compValueRender.put("table_id",table_id);
            }
            else if(columnType.equals("pri-sub-todo-info") || columnType.equals("func") || columnType.equals("file")
            || columnType.equals("tag") || columnType.equals("rich-text")){
                compValueRender.put("data_type","varchar");
                gridColMap.put("data_type","varchar");
            }

            if(flag){
                compValueRender.put("data_type","choice");
                gridColMap.put("data_type","choice");
            }

            copyGridCols.add(gridColMap);
        }
        return copyGridCols;
    }

    public List<Map> getDistinctChoiceGridData(Integer obj_id,Map gridColMap,Map dataSource) {
        int user_id = 111193;
        Map compValueRender = (Map) gridColMap.get("compValueRender");
        Integer table_id  = 0;
        if(compValueRender.containsKey("table_id")){
            table_id  = (Integer) compValueRender.get("table_id");
        }else if(dataSource!=null){
            table_id  = (Integer) dataSource.get("table_id");
        }
        String columnName = compValueRender.get("field").toString();
        Map map = Lutils.genMap("user_id",user_id,"obj_id",obj_id,"obj_c1",null);
        //获取表格域
        Map<String, Object> session = (Map) map.get("session");
        //从session中取当前登录人员信息
        Map userInfo = Lutils.genMap("user_group", 9);

        /** 搜索条件 */
        //搜索中日期型的对象，转换为Date，传入mybatis数据库
        Map causesObj = new HashMap();
        //and条件中的所有条件的值
        if (causesObj.containsKey("tp") && causesObj.get("tp").equals("a")) {
            for (Map obj : (List<Map>) causesObj.get("cas")) {
                BaseDBService.handlerObjectTypedField(obj);
            }
        } else if (causesObj.containsKey("tp") && causesObj.get("tp").equals("o")) {
            for (Map obj : (List<Map>) causesObj.get("cas")) {
                BaseDBService.handlerObjectTypedField(obj);
            }
        }

        //数据获取权限
        if (dataSource.get("data_access_scope").toString().equals("defined")) {
            List ids_ = this.get_view_grid_data_permission_map
                    (dataSource, user_id,columnName,null);
            if(ids_.size()==0){
                ids_.add(-1);
            }
            map.put("obj_in_ids",SqlUtil.in("t" + table_id + ".id_", ids_));
        }

        System.out.println(causesObj);
        //根据类型查询
        if (map.get("orderByColumn") == null) {
            map.put("orderByColumn", "id_");
            map.put("isAsc", "desc");
        }
        map.put("column", columnName.replace("staff_nm",""));
        map.put("table_id", table_id);
        Map<String, Object> re = new HashMap();
        List<Map> list = businessMapper.get_distinct_grid_data(map);
        return list;
    }

    public Map get_distinct_search_col_options(Map col, String query, HttpSession session) throws Exception {
        Map compValueRender = (Map) col.get("compValueRender");
        Map dsField = (Map) col.get("dsField");
        String table_id = (String) dsField.get("table_id");
        String renderType = compValueRender.get("render_type").toString();
        String ds_id = compValueRender.get("ds_id").toString();
        String columnName = dsField.get("field").toString();
        columnName = columnName.replaceFirst("^t\\d+_", "");
        /** 搜索条件 */
        //搜索中日期型的对象，转换为Date，传入mybatis数据库
        Map causesObj = new HashMap();
        //and条件中的所有条件的值
        if (causesObj.containsKey("tp") && causesObj.get("tp").equals("a")) {
            for (Map obj : (List<Map>) causesObj.get("cas")) {
                BaseDBService.handlerObjectTypedField(obj);
            }
        } else if (causesObj.containsKey("tp") && causesObj.get("tp").equals("o")) {
            for (Map obj : (List<Map>) causesObj.get("cas")) {
                BaseDBService.handlerObjectTypedField(obj);
            }
        }
        Map map = new HashMap();
        CompDataSource compDataSource = (CompDataSource) Modules.getInstance().get(ds_id, true);
        ExecContext execContext_s = new ExecContext();
        UserAction userAction = new UserAction();
        List rows = CompUtils.getInstance().get_ds_data_ids(compDataSource, null, execContext_s, userAction, true);
        List ids_list = Lutils.getColumnValueList(rows, "z_" + table_id + "_id");
        ids_list.add(-1);
        map.put("obj_in_ids",SqlUtil.in(table_id + ".id_", ids_list ));
        //根据类型查询
        if (map.get("orderByColumn") == null) {
            map.put("orderByColumn", "id_");
            map.put("isAsc", "desc");
        }

        map.put("table_id", table_id);
        if(query!=null && query.length()>0){
            if(columnName.indexOf("staff_nm")!=-1){
                List<Map> usersList = baseDBService.selectByCauses("v_user",
                        Lutils.genMap("tp","lk","col","staff_nm","val",query),null);
                List columnValueList = Lutils.getColumnValueList(usersList, "id");
                map.put("queryList",columnValueList);
            }else if(columnName.indexOf("edge_")!=-1){
                List<Map> edgeList = baseDBService.selectByCauses("v_flow_edge",
                        Lutils.genMap("tp","lk","col","label","val",query),null);
                List columnValueList = Lutils.getColumnValueList(edgeList, "id");
                map.put("queryList",columnValueList);
            }else if(columnName.indexOf("node_")!=-1){
                List<Map> nodeList = baseDBService.selectByCauses("v_flow_node",
                        Lutils.genMap("tp","lk","col","label","val",query),null);
                List columnValueList = Lutils.getColumnValueList(nodeList, "id");
                map.put("queryList",columnValueList);
            }
        }
        columnName = columnName.replace("staff_nm","");
        columnName = columnName.replace("label","");
        map.put("column", columnName.replace("staff_nm",""));
        Map<String, Object> re = new HashMap();
        List<Map> result = new ArrayList<>();
        List<Map> list = new ArrayList<>();
        try {
            list = businessMapper.get_distinct_grid_data(map);
        }catch (Exception e) {
            result.add(Lutils.genMap(
                    "value","暂无",
                    "label","暂无"));
        }

        if(renderType.equals("dict")){
//            List<Map> rows = (List<Map>) rows.get("rows");
            List dictItemIds = Lutils.getColumnValueList(rows, columnName);
            List<Map> dictItem = baseDBService.selectIn("v_dict_item", "id", dictItemIds);
            for (int j = 0; j < dictItem.size(); j++) {
                Map gridmap = dictItem.get(j);
                result.add(Lutils.genMap(
                        "value",gridmap.get("id"),
                        "label",gridmap.get("name")));
            }
            return Lutils.genMap("data",result);
        }


        for (int j = 0; j < list.size(); j++) {
            Map gridmap = list.get(j);
            result.add(Lutils.genMap(
                    "value",gridmap.get("value_"),
                    "label",gridmap.get("label_")));
        }
        if(result.size()==0){
            result.add(Lutils.genMap(
                    "value","暂无",
                    "label","暂无"));
        }
        return Lutils.genMap("data",result);
    }
}
