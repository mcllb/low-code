package tcdx.uap.controller;

import cn.hutool.core.collection.CollUtil;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.var;
import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.Select;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import tcdx.uap.common.entity.AjaxResult;
import tcdx.uap.common.utils.*;
import tcdx.uap.constant.Constants;
import tcdx.uap.mapper.ServiceConfigMapper;
import tcdx.uap.mapper.BusinessMapper;
import tcdx.uap.mapper.SystemMapper;
import tcdx.uap.service.*;
import tcdx.uap.mapper.BaseDBMapper;

import net.sf.jsqlparser.statement.Statement;
import tcdx.uap.service.entities.*;
import tcdx.uap.service.store.*;

import javax.servlet.http.HttpSession;
import javax.sql.DataSource;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 通用请求处理
 *
 * @author ruoyi
 */
@Controller
@RequestMapping("/uap/cfg")
public class ServiceConfigController extends BaseController
{
    private static final Logger log = LoggerFactory.getLogger(ServiceConfigController.class);
    private String prefix = "";

    @Autowired
    private DataSource dataSource;

    @Autowired
    private ServiceConfigMapper serviceConfigMapper;

    @Autowired
    private BusinessMapper businessMapper;

    @Autowired
    private BaseDBService db;

    @Autowired
    BaseDBMapper baseDBMapper;

    @Autowired
    private BaseDBService baseDBService;

    @Autowired
    private SystemMapper systemConfigMapper;

    @Autowired
    private ServiceConfigService serviceConfigService;

    @Autowired
    private BusinessService businessService;

    @Autowired
    private SystemService systemService;

    @Autowired
    private BaseDBController baseDBController;
    @Autowired
    private HttpSession httpSession;
    @Autowired
    private SystemMapper systemMapper;

    //    @RequiresPermissions("system:flowable:get_flow_mapper")
    @PostMapping("/mapper")
    @ResponseBody
    public AjaxResult mapper(@RequestBody Map<String, Object> map)
    {
        //从session中取当前登录人员信息
        Map userInfo = Lutils.genMap("user_name", "luyu1", "user_group", 9);
        Object resultData = null;
         if(map.get("mapper").equals("get_table_coulmn_relations"))
            resultData = serviceConfigMapper.get_table_coulmn_relations(map);
        else if(map.get("mapper").equals("get_table_coulmn_by_relations"))
            resultData = serviceConfigMapper.get_table_coulmn_by_relations(map);
        else if(map.get("mapper").equals("select_todo_counts"))
            resultData = serviceConfigMapper.select_todo_counts(map);
        //找到表名
        return AjaxResult.success("success",resultData);
    }

    @PostMapping("/get_table_relations")
    @ResponseBody
    public AjaxResult get_table_relations(@RequestBody Map<String, Object> map)
    {
        List l = serviceConfigMapper.get_table_relations(map);
        return AjaxResult.success("success",l);
    }

    @PostMapping("/get_table_col_lk_name")
    @ResponseBody
    public AjaxResult get_table_col_lk_name(@RequestBody Map<String, Object> map)
    {
        List l = serviceConfigMapper.get_table_relations(map);
        return AjaxResult.success("success",l);
    }

    @PostMapping("/add_table_tree_node")
    @ResponseBody
    public AjaxResult add_table_tree_node(@RequestBody Map<String, Object> map)
    {
        Table t = new Table(map);
        Map newNode = baseDBService.insertMapAutoFillMaxOrd("v_tree_table",
                Lutils.genMap("id", t.id, "parent_id", map.get("parent_id"), "is_deleted", false, "name", t.name, "type", map.get("type")), "ord",
                Lutils.genMap("parent_id", map.get("parent_id")));
        if(map.get("type").equals("table")){
            //在modules表增加数据
            Modules.getInstance().create(t.id, "Table", t);
            //在数据库创建表
            Modules.getInstance().createTable(t.table_name);
        }
        return AjaxResult.success("success", newNode);
    }

    @PostMapping("/set_table_name")
    @ResponseBody
    @Transactional
    public AjaxResult set_table_name(@RequestBody Map<String, Object> map)
    {
        //从session中取当前登录人员信息
        String id = (String) map.get("id");
        String name = (String) map.get("name");
        baseDBService.updateEq("v_tree_table", map, Lutils.genMap("id", id));
        Table tbl = (Table) Modules.getInstance().get(id, false);
        if(tbl!=null) {
            tbl.name = name;
            Modules.getInstance().save(id, tbl);
        }
        return AjaxResult.success("success", tbl);
    }

    @PostMapping("/add_table_relation")
    @ResponseBody
    @Transactional
    public AjaxResult add_table_relation(@RequestBody Map<String, Object> map)
    {
        //从session中取当前登录人员信息
        String table_id = (String) map.get("table_id");
        String pri_table_id = (String) map.get("pri_table_id");
        if(table_id.equals(pri_table_id)){
            return AjaxResult.success("failed","不能添加自身为外部关联表");
        }
        Table tbl = (Table) Modules.getInstance().get(table_id,true);
        Table priTbl = (Table) Modules.getInstance().get(pri_table_id, true);
        if(tbl.priTableIds.contains(pri_table_id)){
            return AjaxResult.success("failed","已有关联关系请勿重复添加");
        }
        //当前表添加字段
        TableCol tc = new TableCol();
        tc.id = Constants.getTimeFormatId();
        tc.name = "外联主表" + priTbl.id + "ID";
        tc.field = priTbl.table_name + "_id";
        tc.field_content_from = "pri_table";
        tc.data_type = "integer";
        tbl.cols.add(tc);
        //当前表添加外联主表
        tbl.priTableIds.add(pri_table_id);
        //主表添加子表
        priTbl.subTableIds.add(table_id);
        //更新深度关系
        tbl.setDeepInfo();
        priTbl.setDeepInfo();
        Modules.getInstance().save(table_id, tbl);
        Modules.getInstance().save(pri_table_id, priTbl);
        //取最大排序
        //增加表结构的列
        try {
            baseDBMapper.tableAddColumn(Lutils.genMap("tn", tbl.table_name, "columnName", tc.field, "columnType", "int4"));
            //包括日志表
            baseDBMapper.tableAddColumn(Lutils.genMap("tn", tbl.table_name + "_log", "columnName", tc.field, "columnType", "int4"));
        }catch(Exception e){}
        //找到表名
        return AjaxResult.success("success", Modules.getInstance().get(table_id, true));
    }

    @PostMapping("/del_table_relation")
    @ResponseBody
    @Transactional
    public AjaxResult del_table_relation(@RequestBody Map<String, Object> map)
    {
        String id = (String)map.get("table_id");
        String pri_table_id = (String)map.get("pri_table_id");
        Table tbl = (Table) Modules.getInstance().get(id, true);
        Table priTbl = (Table) Modules.getInstance().get(pri_table_id, true);
        tbl.priTableIds.remove(pri_table_id);
        priTbl.subTableIds.remove(id);
        tbl.removeColByField(priTbl.table_name+"_id");
        baseDBMapper.tableDropColumn(MapUtils.G("tn", tbl.table_name, "columnName", priTbl.table_name+"_id"));
        baseDBMapper.tableDropColumn(MapUtils.G("tn", tbl.table_name+"_log", "columnName", priTbl.table_name+"_id"));
        tbl.setDeepInfo();
        priTbl.setDeepInfo();
        Modules.getInstance().save(id, tbl);
        Modules.getInstance().save(pri_table_id, priTbl);
        return AjaxResult.success("success", Modules.getInstance().get(id, true));
    }

    @PostMapping("/get_table_cascading_cols")
    @ResponseBody
    public AjaxResult get_table_cascading_cols(@RequestBody Map<String, Object> map)
    {
        String table_id = (String)map.get("table_id");
        List<Map> re = new ArrayList<>();
        Table tbl = (Table)Modules.getInstance().get(table_id, true);
        for(TableCol tc: tbl.cols){
            re.add(Lutils.genMap("field_type","table_field", "parent", "当前表", "table_id", tbl.id,"tableName", tbl.name, "table_id", tbl.id, "table_col_id", tc.id, "name", tc.name, "field", tc.field));
        }
        //主表的字段
        for(String priTblId: tbl.priTableIds){
            Table ptbl = (Table) Modules.getInstance().get(priTblId, true);
            for(TableCol tc: ptbl.cols){
                re.add(Lutils.genMap("field_type","table_field", "parent", tbl.name,"table_id", ptbl.id,"tableName", ptbl.name,  "table_id", tbl.id, "table_col_id", tc.id, "name", tc.name, "field", tc.field));
            }
            for(String priTblId2: ptbl.priTableIds){
                Table ptbl2 = (Table) Modules.getInstance().get(priTblId2, true);
                for(TableCol tc: ptbl2.cols){
                    re.add(Lutils.genMap("field_type","table_field", "parent",  ptbl.name,"table_id", ptbl2.id,"tableName", ptbl2.name,  "table_id", tbl.id, "table_col_id", tc.id, "name", tc.name, "field", tc.field));
                }
            }
        }
        //当前表字段
        List<Map> def = Constants.getDsFieldDefinition();
        for(Map m: def){
            m.put("tableName", tbl.name);
            m.put("table_id", tbl.id);
        }
        //流程字段
        for(FlowEdge edge: tbl.edges){
            re.add(Lutils.genMap("field_type","flow_field", "parent", "当前表", "table_id", tbl.id,"tableName", tbl.name,"field","posted_time_", "flow_edge_id", edge.id, "name", edge.label+":派单时间"));
            re.add(Lutils.genMap("field_type","flow_field", "parent", "当前表", "table_id", tbl.id,"tableName", tbl.name,"field","poster_","flow_edge_id", edge.id, "name", edge.label+":派单人ID"));
            re.add(Lutils.genMap("field_type","flow_field", "parent", "当前表", "table_id", tbl.id,"tableName", tbl.name,"field","poster_staff_nm", "flow_edge_id", edge.id, "name", edge.label+":派单人"));
            re.add(Lutils.genMap("field_type","flow_field", "parent", "当前表", "table_id", tbl.id,"tableName", tbl.name,"field","poster_phone", "flow_edge_id", edge.id, "name", edge.label+":派单人电话"));
            re.add(Lutils.genMap("field_type","flow_field", "parent", "当前表", "table_id", tbl.id,"tableName", tbl.name,"field","receiver_", "flow_edge_id", edge.id, "name", edge.label+":接单人ID"));
            re.add(Lutils.genMap("field_type","flow_field", "parent", "当前表", "table_id", tbl.id,"tableName", tbl.name,"field","receiver_staff_nm", "flow_edge_id", edge.id, "name", edge.label+":接单人"));
            re.add(Lutils.genMap("field_type","flow_field", "parent", "当前表", "table_id", tbl.id,"tableName", tbl.name,"field","receiver_phone", "flow_edge_id", edge.id, "name", edge.label+":接单人电话"));
        }
        return AjaxResult.success("success", re);
    }

    @PostMapping("/get_table_flowed_cols")
    @ResponseBody
    public List get_table_flowed_cols(@RequestBody Map<String, Object> map)
    {
        Integer table_id = (Integer)map.get("table_id");
        List <Map> ls= serviceConfigMapper.get_table_flowed_cols(Lutils.genMap("table_id", table_id));
        return ls;
    }

    public Boolean judge_sql(String data_sql, Integer userId)
    {
        data_sql = data_sql.replace("#{user_id}",userId.toString());
        try {
            Statement statement = CCJSqlParserUtil.parse(data_sql);
            // 只允许简单的SELECT查询
            if (statement instanceof Select) {
                Select select = (Select) statement;
                // 检查是否包含FOR UPDATE等锁定子句
                if (select.getSelectBody() instanceof PlainSelect) {
                    PlainSelect plainSelect = (PlainSelect) select.getSelectBody();
                    return plainSelect.getForUpdateTable() == null;
                }
//                List<Map> maps = baseDBService.selectSql(data_sql);
                return true;
            }
            return false;
        } catch (Exception e) {
            return false;
        }
    }


    public void insert_defined_sql(Object ds_id,String data_sql, Object userId) throws JSQLParserException {
        data_sql = data_sql.replace("#{user_id}",userId.toString());
        List<String> fieldAliases = Lutils.extractFieldAliasesByJSQL(data_sql);
        List<String> tableNames = Lutils.extractTableNames(data_sql);
        for (int i = 0; i < tableNames.size(); i++) {
            String table_name = tableNames.get(i).toString();
            if (table_name.matches("z_table\\d+")) {
                String table_id = table_name.replace("z_", "");
                for (int j = 0; j < fieldAliases.size(); j++) {
                    String field = fieldAliases.get(j);
                    if (field.contains(table_name.replace("z_table", "")) && !field.contains("z_table")) {
                        Map map2 = new HashMap();
                        if (field.contains("receiver_") || field.contains("edge_") || field.contains("node_")
                                || field.contains("poster_") || field.contains("create_") || field.contains("update_")) {
                            map2 = Lutils.genMap("table_id", table_id, "defined_field", field, "ds_id", ds_id, "is_deleted", false);
                        } else {
                            String item = field.replace("t" + table_id + "_", "");
                            List<Map> maps = baseDBService.selectEq("v_table_col", Lutils.genMap("field", item, "table_id", table_id));
                            map2 = Lutils.genMap("table_id", table_id, "defined_field", field, "ds_id", ds_id, "is_deleted", false);
                            if (maps.size() > 0) {
                                map2.put("table_col_id", (Integer) maps.get(0).get("id"));
                            }else{

                            }
                        }
                        map2.put("field_type", "defined_field");
                        map2.put("is_deleted", false);
                        baseDBService.insertWhenNotExistUpdateWhenExists("v_datasource_field", map2,
                                Lutils.genMap("ds_id", ds_id, "table_id", table_id, "defined_field", field));
                    }
                }
            }
        }
        if(fieldAliases.size()>0){
            List<Map> notIds = baseDBService.selectByCauses("v_datasource_field", SqlUtil.and(SqlUtil.notin("defined_field",fieldAliases),SqlUtil.eq("ds_id", ds_id)),null);
            if(notIds.size()>0){
                baseDBService.updateIn("v_datasource_field", Lutils.genMap("is_deleted", true), "id", notIds.stream().map(o->o.get("id")).collect(Collectors.toList()));
            }
        }
    }

    @PostMapping("/get_table_defined_cols")
    @ResponseBody
    public AjaxResult get_table_defined_cols(@RequestBody Map<String, Object> map)
    {
        String data_sql = map.get("sql").toString();

        // 正则表达式匹配 z_table 后面的数字
        Pattern pattern = Pattern.compile("z_table(\\d+)");
        Matcher matcher = pattern.matcher(data_sql);

        List<Integer> numbers = new ArrayList<>();

        while (matcher.find()) {
            numbers.add(Integer.parseInt(matcher.group(1)));
        }
        if(numbers.size()==0){return null;}

        List <Map> foreignTableList= new ArrayList<>();
        for (int i = 0; i < numbers.size(); i++) {
            Integer table_id = numbers.get(i);
            List<Integer>relation_ids = new ArrayList<>();
            foreignTableList.addAll(getDefinedCascadingDatatableRelations(table_id,relation_ids));
        }
        return AjaxResult.success("success", foreignTableList);
    }
    public List<Map> getCascadingDatatableRelations(Integer table_id,List<Integer>relation_tns) {
        if(relation_tns.contains(table_id))
            return null;
        List<Map> tableList = new ArrayList<>();
        List<Map> columns = baseDBService.selectEq("v_table_col", Lutils.genList("id", "field", "name","table_id"),
                Lutils.genMap("table_id", table_id),
                Lutils.genMap("ord", "asc"));
        for (Map m : columns) {
            m.put("value", m.get("id"));
            m.put("label", m.get("name"));
            m.put("isLeaf", true);
            tableList.add(m);
            relation_tns.add(table_id);
        }
        List<Map> foreignTableList = serviceConfigMapper.get_table_relations(Lutils.genMap("table_id", table_id));
        if (foreignTableList.size() > 0) {
            for (Map m : foreignTableList) {
                Integer foreign_table_id = (Integer)m.get("foreign_table_id");
                m.put("value", 100000000 + (Integer) m.get("id"));
                m.put("label", m.get("foreign_table_display_name"));
                m.put("isLeaf", false);
                List tmp = getCascadingDatatableRelations(foreign_table_id,relation_tns);
                m.put("children", tmp);
                if(tmp!=null &&  tmp.size()>0)
                    tableList.add(m);
            }
        }
        return tableList;
    }

    public List<Map> getDefinedCascadingDatatableRelations(Integer table_id,List<Integer>relation_tns) {
        if(relation_tns.contains(table_id))
            return null;
        List<Map> tableList = new ArrayList<>();
        List<Map> columns = baseDBService.selectEq("v_table_col", Lutils.genList("id", "field", "name","table_id"),
                Lutils.genMap("table_id", table_id),
                Lutils.genMap("ord", "asc"));
        for (Map m : columns) {
            m.put("value", m.get("id"));
            m.put("label", "z_"+table_id+m.get("name"));
            m.put("isLeaf", true);
            tableList.add(m);
            relation_tns.add(table_id);
        }
        List<Map> foreignTableList = serviceConfigMapper.get_table_relations(Lutils.genMap("table_id", table_id));
        if (foreignTableList.size() > 0) {
            for (Map m : foreignTableList) {
                Integer foreign_table_id = (Integer)m.get("foreign_table_id");
                m.put("value", 100000000 + (Integer) m.get("id"));
                m.put("label", m.get("foreign_table_display_name"));
                m.put("isLeaf", false);
                List tmp = getCascadingDatatableRelations(foreign_table_id,relation_tns);
                m.put("children", tmp);
                if(tmp!=null &&  tmp.size()>0)
                    tableList.add(m);
            }
        }
        return tableList;
    }

    @PostMapping("/update_view_grid_cols")
    @ResponseBody
    @Transactional
    public AjaxResult update_view_grid_cols(@RequestBody Map<String, Object> map)
    {
        List<Integer> table_col_ids = (List<Integer>) map.get("table_col_ids");
        table_col_ids.add(-1); //防止空数据集，影响mybatis
        Integer view_id = (Integer) map.get("view_id");
        Integer maxOrd = baseDBService.selectMaxColEq("v_view_grid_col","ord", "view_id", view_id);
        maxOrd = Lutils.nvl(maxOrd,0);
        serviceConfigMapper.delete_view_grid_col(Lutils.genMap("view_id", view_id, "table_col_ids", table_col_ids));
        serviceConfigMapper.add_view_grid_col(Lutils.genMap("view_id", view_id, "table_col_ids", table_col_ids, "maxOrd", maxOrd));
        return AjaxResult.success("success");
    }


    @PostMapping("/update_view_grid_flow_cols")
    @ResponseBody
    @Transactional
    public AjaxResult update_view_grid_flow_cols(@RequestBody Map<String, Object> map)
    {
        List<Map> flow_id_attrs = (List<Map>) map.get("flow_id_attrs");
        Integer view_id = (Integer) map.get("view_id");
        Integer table_id = (Integer) map.get("table_id");
        Integer maxOrd = baseDBService.selectMaxColEq("v_view_grid_col","ord", "view_id", view_id);
        maxOrd = Lutils.nvl(maxOrd,0);
        serviceConfigMapper.delete_view_grid_flow_col(Lutils.genMap("view_id", view_id, "flow_attrs", flow_id_attrs));
        int ord = 0;
        for(Map<String,Object> flow_attr_map : flow_id_attrs){
            maxOrd+= ++ord;
            Integer flow_edge_id = (Integer)flow_attr_map.get("flow_edge_id");
            String column_data_attr = (String)flow_attr_map.get("column_data_attr");
            String column_type = "文本";
            if(column_data_attr.contains("time_")){
                column_type="日期与时间";
            }
            else if(column_data_attr.contains("label_")){
                column_type="标签";
            }
            String title = (String)flow_attr_map.get("title");
            Map iMap =  Lutils.genMap(
                    "view_id", view_id,
                    "table_id",     table_id,
                    "title",        title,
                    "column_type",  column_type,
                    "min_width",    200,
                    "is_show",      true,
                    "use_default",  false,
                    "fixed", "none",
                    "ord", maxOrd,
                    "flow_edge_id", flow_edge_id,
                    "column_data_attr", column_data_attr
            );
            if(flow_edge_id!=null) {
                baseDBService.insertWhenNotExist("v_view_grid_col", iMap,
                        Lutils.genMap("view_id", view_id, "flow_edge_id", flow_edge_id,
                                "column_data_attr", column_data_attr));
            }
            else{
                baseDBService.insertWhenNotExist("v_view_grid_col", iMap,
                        Lutils.genMap("view_id", view_id,
                                "column_data_attr", column_data_attr));
            }
        }
        return AjaxResult.success("success");
    }


    @PostMapping("/get_view_grid_cols")
    @ResponseBody
    public AjaxResult get_view_grid_cols(@RequestBody Map<String, Object> map)
    {
        Integer view_id = (Integer)map.get("view_id");
        List<Map> l = serviceConfigService.get_view_grid_cols(view_id);
        return AjaxResult.success("success", l);
    }

    @PostMapping("/get_comp_grid_cols")
    @ResponseBody
    public List<Map> get_comp_grid_cols(@RequestBody Map<String, Object> map)
    {
        Integer comp_id = (Integer)map.get("comp_id");
        List<Map> gridCols = (List)CompGridStore.getInstance().get(comp_id).get("gridCols");
        return gridCols;
    }




    public String get_db_type(String data_type,Integer length, Integer prec){
        //将字段类型转换为数据库可识别的类型
        if(data_type.equals("varchar"))
            return "varchar("+length+")";
        else if(data_type.equals("numeric"))
            return "numeric(16, "+prec+")";
        else if(data_type.equals("integer"))
            return "integer";
        else if(data_type.equals("timestamp"))
            return "timestamp";
        else
            return "varchar(1000)";
    }

    @PostMapping("/get_pinyin")
    @ResponseBody
    public AjaxResult get_pinyin(@RequestBody Map<String, Object> map)
    {
        String name = (String) map.get("name");
        name = name.replace("u:", "v");
        return AjaxResult.success("success", StringUtils.chnToPinyin(name));
    }

    @PostMapping("/get_table_tree")
    @ResponseBody
    public AjaxResult get_table_tree(@RequestBody Map<String, Object> map,HttpSession httpSession)
    {
        UserAction ua = new UserAction();
        ua.setUserInfo(httpSession);
        Integer sas_system_id=(Integer) map.get("sas_system_id");
        List<Map> treeList = new ArrayList<>();
        if(sas_system_id!=null){
            treeList = serviceConfigMapper.get_sas_system_tables(Lutils.genMap("sas_system_id", sas_system_id));
        }
        else {
            if (Objects.equals(ua.user_id, 879)) {
                treeList = baseDBService.selectEq("v_tree_table", Lutils.genMap("is_deleted", false), Lutils.genMap("ord", "asc"));
            }
        }
        return AjaxResult.success("success", treeList);
    }

    @PostMapping("/get_node_tree")
    @ResponseBody
    public AjaxResult get_node_tree(@RequestBody Map<String, Object> map)
    {
        try {
            String data_sql = "select \n" +
                    "\tn.id node_id, \n" +
                    "\tn.label node_name, \n" +
                    "\tt.id table_id, \n" +
                    "\tt.name table_name\n" +
                    "from v_flow_node n\n" +
                    "left join v_tree_table t on t.id = n.table_id\n" +
                    "where t.is_deleted = false and t.type = 'table'";
            String order_sql = "order by t.id, n.id";

            List<Map> queryResult = baseDBMapper.selectDefinedSql(Lutils.genMap("data_sql", data_sql, "order_sql", order_sql));

            // 使用LinkedHashMap保持插入顺序
            Map<Object, Map<String, Object>> tableMap = new LinkedHashMap<>();

            for (Map<String, Object> row : queryResult) {
                Object tableId = row.get("table_id");
                String tableName = (String) row.get("table_name");
                Object nodeId = row.get("node_id");
                String nodeName = (String) row.get("node_name");

                // 如果节点信息为空，跳过
                if (nodeId == null || nodeName == null) {
                    continue;
                }

                // 如果表不存在于map中，创建新的表条目
                if (!tableMap.containsKey(tableId)) {
                    Map<String, Object> tableEntry = new HashMap<>();
                    tableEntry.put("id", tableId);
                    tableEntry.put("name", tableName);
                    tableEntry.put("nodes", new ArrayList<Map<String, String>>());
                    tableMap.put(tableId, tableEntry);
                }

                // 添加节点到对应的表中
                Map<String, Object> tableEntry = tableMap.get(tableId);
                List<Map<String, String>> nodes = (List<Map<String, String>>) tableEntry.get("nodes");

                Map<String, String> node = new HashMap<>();
                node.put("id", String.valueOf(nodeId));
                node.put("name", nodeName);
                nodes.add(node);
            }

            // 转换为前端需要的格式
            List<Map<String, Object>> tablesData = new ArrayList<>(tableMap.values());

            // 如果查询结果为空，返回空数组而不是null
            if (tablesData.isEmpty()) {
                return AjaxResult.success("No data found", Collections.emptyList());
            }

            // 创建返回结果
            Map<String, Object> result = new HashMap<>();
            result.put("tablesData", tablesData);

            return AjaxResult.success("success", result);
        } catch (Exception e) {
            log.error("获取节点树失败", e);
            return AjaxResult.error("获取节点树失败：" + e.getMessage());
        }
    }

    @PostMapping("/get_table_pris")
    @ResponseBody
    public AjaxResult get_table_pris(@RequestBody Map<String, Object> map)
    {
        String table_id = (String)map.get("table_id");
        if(table_id==null)
            return AjaxResult.success("success", new ArrayList<>());
        Table tbl = (Table) Modules.getInstance().get(table_id, false);
        List<Table> re = new ArrayList<>();
        re.add(tbl);
        //主表的字段
        for(String priTblId: tbl.priTableIds){
            Table ptbl = (Table) Modules.getInstance().get(priTblId, false);
            if(re.stream().filter(o->o.id==ptbl.id).collect(Collectors.toList()).size()==0)
                re.add(ptbl);
            for(String priTblId2: ptbl.priTableIds){
                Table ptbl2 = (Table) Modules.getInstance().get(priTblId2, false);
                if(re.stream().filter(o->o.id==ptbl2.id).collect(Collectors.toList()).size()==0)
                    re.add(ptbl2);
            }
        }
        return AjaxResult.success("success", re);
    }


    /**
     * 增加列定义
     * */
    //@RequiresPermissions("system:flowable:add_tbl_col")
    @PostMapping("/add_tbl_col")
    @ResponseBody
    public AjaxResult add_tbl_col(@RequestBody Map<String, Object> map)
    {
        String tid = (String)map.get("table_id");
        Table tbl = (Table) Modules.getInstance().get(tid, true);
        TableCol tc =  Lutils.ObjToClass(map.get("insertMap"), TableCol.class);
        tc.id = Constants.getTimeFormatId();
        SqlUtil.filterKeyword(tc.field);
        //获取流程关联的表名
        //将字段类型转换为数据库可识别的类型
        String columnType = get_db_type(tc.data_type, tc.varchar_size, tc.numeric_precision);
        String msg="";
        try {
            //增加表结构的列
            baseDBMapper.tableAddColumn(MapUtils.G("tn", tbl.table_name, "columnName", tc.field, "columnType", columnType));
            //包括日志表
            baseDBMapper.tableAddColumn(MapUtils.G("tn", tbl.table_name+"_log", "columnName", tc.field, "columnType", columnType));
            //增加表结构的列
            tbl.cols.add(tc);
            Modules.getInstance().save(tid, tbl);
            return AjaxResult.success("success", Modules.getInstance().get(tid, true));
        }
        catch(Exception e){
            e.printStackTrace();
            msg = e.getMessage();
            return AjaxResult.success("failed", msg);
        }
    }

    /**
     * 修改列定义
     * */
    //@RequiresPermissions("system:flowable:save_tbl_col")
    @PostMapping("/save_tbl_col")
    @ResponseBody
    public AjaxResult save_tbl_col(@RequestBody Map<String, Object> map)
    {
        String table_id = (String) map.get("table_id");
        String col_id = (String) map.get("id");
        Table tbl = (Table) Modules.getInstance().get(table_id, true);
        TableCol tc =  Lutils.ObjToClass(map.get("updateMap"), TableCol.class);
        if(!tbl.cols.stream().filter(c->!c.id.equals(tc.id)&&c.name.equals(tc.name)).collect(Collectors.toList()).isEmpty()){
            return AjaxResult.success("failed", "中文名称重复，请修改");
        }
        if(!tbl.cols.stream().filter(c->!c.id.equals(tc.id)&&c.field.equals(tc.field)).collect(Collectors.toList()).isEmpty()){
            return AjaxResult.success("failed", "字段名称重复，请修改");
        }
        String newColumnType = ((Map)map.get("updateMap")).get("data_type").toString();
        SqlUtil.filterKeyword(tc.field);
        //获取流程关联的表名
        //获取修改字段的原数据
        TableCol oldCol = tbl.getCol(col_id);
        String oldColumnType = get_db_type(oldCol.data_type,oldCol.varchar_size,oldCol.numeric_precision);
        newColumnType = get_db_type(tc.data_type, tc.varchar_size, tc.numeric_precision);
        String msg="";
        int rs = 0;
        try {
            //修改字段类型
            if(!oldColumnType.equals(newColumnType)) {
                baseDBMapper.tableAlterColumnType(MapUtils.Combine(MapUtils.New("tn", tbl.table_name),
                        MapUtils.New("columnName", oldCol.field),
                        MapUtils.New("oldColumnType", oldColumnType),
                        MapUtils.New("newColumnType", newColumnType)));

                baseDBMapper.tableAlterColumnType(MapUtils.Combine(MapUtils.New("tn", tbl.table_name + "_log"),
                        MapUtils.New("columnName", oldCol.field),
                        MapUtils.New("oldColumnType", oldColumnType),
                        MapUtils.New("newColumnType", newColumnType)));
            }
            //修改字段名称，发生变化才修改名称，否则会出错
            if(!tc.field.equals(oldCol.field)) {
                baseDBMapper.tableAlterColumnName(MapUtils.Combine(MapUtils.New("tn", tbl.table_name),
                        MapUtils.New("columnName", oldCol.field),
                        MapUtils.New("newColumnName", tc.field)
                ));
                baseDBMapper.tableAlterColumnName(MapUtils.Combine(MapUtils.New("tn", tbl.table_name+"_log"),
                        MapUtils.New("columnName", oldCol.field),
                        MapUtils.New("newColumnName", tc.field)
                ));
            }
            //更新表与表之间的关联关系
            tbl.setCol(col_id, tc);
            //Table特殊，另存边和节点的名称到v_flow_edge和v_flow_node表
            tbl.setDeepInfo();
            Modules.getInstance().HandlerDataSourceField(tbl.id);
            Modules.getInstance().updateTableFlowNodeEdges(tbl);
            Modules.getInstance().save(table_id, tbl);
            return AjaxResult.success("success", tbl);
        }
        catch(Exception e){
            e.printStackTrace();
            msg = e.getMessage();
            return AjaxResult.success("failed", msg);
        }
    }

    /**
     * 删除列
     * */
    //    @RequiresPermissions("uap:base_db:drop_col")
    @PostMapping("/drop_tbl_col")
    @ResponseBody
    public AjaxResult drop_tbl_col(@RequestBody Map<String, Object> map)
    {
        String table_id = (String) map.get("table_id");
        String col_id = (String) map.get("col_id");
        Table tbl = (Table) Modules.getInstance().get(table_id, true);
        TableCol tc = tbl.getCol(col_id);
        //获取流程关联的表名
        try {
            //删除表结构
            baseDBMapper.tableDropColumn(MapUtils.Combine(MapUtils.New("tn", tbl.table_name), MapUtils.New("columnName", tc.field)));
            //包括日志表
            baseDBMapper.tableDropColumn(MapUtils.Combine(MapUtils.New("tn",  tbl.table_name+"_log"), MapUtils.New("columnName", tc.field)));
            //删除数据表
            tbl.removeCol(col_id);
            tbl.setDeepInfo();
            Modules.getInstance().HandlerDataSourceField(tbl.id);
            Modules.getInstance().save(table_id, tbl);
            return AjaxResult.success("success", tbl);
        }catch(Exception e){
            e.printStackTrace();
            return AjaxResult.success("failed", e.getMessage());
        }
    }

    //@RequiresPermissions("uap:flowable:select_view_config")
    @JsonInclude(JsonInclude.Include.ALWAYS)
    @PostMapping("/get_group_views")
    @ResponseBody
    public AjaxResult get_group_views(@RequestBody Map<String, Object> map) {
        Map re = new HashMap();
        List<Map> views = businessMapper.getViewsInParentTreeNoJson(map);
        List<Map> btns = new ArrayList<>();
        //生成树结构
        for(Map node: views){
            View v = (View) Modules.getInstance().get(node.get("id"), false);
            if(v!=null && !v.view_type.equals("folder")) {
                for (Exec exec : v.viewTitleBtns) {
                    btns.add(Lutils.genMap("id", exec.id, "parent_id", v.id, "name", "【标题按钮】" + exec.name, "type", "button"));
                }
                for (Exec exec : v.viewBtns) {
                    btns.add(Lutils.genMap("id", exec.id, "parent_id", v.id, "name", "【视图按钮】" + exec.name, "type", "button"));
                }
                if (Objects.equals(v.view_type,"comp") && Objects.equals(v.comp_name,"CompGrid")) {
                    CompGrid grid = (CompGrid) Modules.getInstance().get(v.comp_id, false);
                    for (Exec exec : grid.topBtns) {
                        btns.add(Lutils.genMap("id", exec.id, "parent_id", v.id, "name", "【表格顶部按钮】" + exec.name, "type", "button"));
                    }
                    for (CompGridCol c : grid.gridCols) {
                        for (Exec exec : c.btns) {
                            btns.add(Lutils.genMap("id", exec.id, "parent_id", v.id, "name", "【列：" + c.title + " 按钮】" + exec.name, "type", "button"));
                        }
                    }
                }
            }
        }
        if(btns.size()>0) {
            List<Map> parents = businessMapper.getViewsAndParentsNoJSON(Lutils.genMap("view_ids", btns.stream().map(o -> o.get("parent_id")).collect(Collectors.toList())));
            btns.addAll(parents);
        }
        re.put("views", views);
        re.put("btns", btns);
        return AjaxResult.success("success", re);
    }



    //@RequiresPermissions("uap:flowable:select_view_config")
    @PostMapping("/get_view_nodes")
    @ResponseBody
    public AjaxResult get_view_nodes(@RequestBody Map<String, Object> map) {
        List<Map> views = serviceConfigMapper.get_parent_views_comps(Lutils.genMap("parent_id", map.get("parent_id")));
        return AjaxResult.success("suc",views );
    }

    @PostMapping("/get_all_views_comps")
    @ResponseBody
    public AjaxResult get_all_views_comps(@RequestBody Map<String, Object> map)
    {

        //本级视图
        List<Map> viewGroups = baseDBService.selectByCauses("v_view_group", null, null);
        for(Map m: viewGroups){
            m.put("view_type", "group");
            m.put("parent_id", 0);
        }
        List<Map> views = serviceConfigMapper.get_group_views(null);
        //生成树结构

//        Map re = Lutils.genMap("curViews", curViews);
//        //是否有上一级视图
//        List<Map> lastViewBtns = baseDBService.selectEq("v_view_btn", Lutils.genMap("open_view_id", view_id));
//        Integer last_view_id = null;
//        if(lastViewBtns.size()>0){
//            Map lastBtn = lastViewBtns.get(0);
//            //判断是表格按钮，还是视图按钮
//            if(btn.get("view_id")!=null){
//                last_view_id = (Integer)baseDBService.selectEq("v_view", Lutils.genMap("comp_id", lastBtn.get("view_id"))).get(0).get("id");
//            }
//            else{
//                last_view_id = (Integer)btn.get("view_id");
//            }
//            List<Map> lastViews = serviceMapper.getViewRecrusive(Lutils.genMap("view_id", last_view_id));
//            re.put("lastViews", lastViews);
//        }
        viewGroups.addAll(views);
        //找到表名
        return AjaxResult.success("success", viewGroups);
    }

//    @PostMapping("/get_btn_view_operations")
//    @ResponseBody
//    public AjaxResult get_btn_view_operations(@RequestBody Map<String, Object> map)
//    {
//        //从session中取当前登录人员信息
//        Integer exec_id =  (Integer)map.get("exec_id");
//        List<Map> views = serviceConfigMapper.get_operations_of_execs(Lutils.genMap("exec_ids", Lutils.genList(exec_id)));
//        //找到表名
//        return AjaxResult.success("success", views);
//    }

    public String compName(Object comp_name){
        if(comp_name.equals("CompForm")){
            return "表单";
        }
        else if(comp_name.equals("CompGrid")){
            return "表格";
        }
        return "";
    }


    @PostMapping("/set_view_tree_order")
    @ResponseBody
    public AjaxResult set_view_tree_order(@RequestBody Map<String, Object> map) {
        int rs = 0;
        Integer parent_id = (Integer)map.get("parent_id");
        Integer id = (Integer)map.get("id");   //当前视图id
        Integer ord = (Integer)map.get("ord");
        Integer step = (Integer)map.get("step");
        //
        List<Map> lastView = serviceConfigMapper.get_prev_view(Lutils.genMap("parent_id", parent_id, "id", id, "step", step));
        if(lastView.size()>0){
            if(!lastView.get(0).get("id").equals(id)){
                rs+= baseDBService.updateEq("v_view", Lutils.genMap("ord", lastView.get(0).get("ord")),Lutils.genMap("id", id));
                rs+=baseDBService.updateEq("v_view", Lutils.genMap("ord", ord),Lutils.genMap("id", lastView.get(0).get("id")));
            }
        }
        return AjaxResult.success("suc", rs);
    }

    @PostMapping("/get_flow_node_btns")
    @ResponseBody
    public List<Map> get_flow_node_btns(@RequestBody Map<String, Object> map) {
        Integer node_id = (Integer)map.get("node_id");
        List<Map> re1 = serviceConfigMapper.get_flow_btns(Lutils.genMap( "node_id", node_id));
        return re1;
    }

    @PostMapping("/get_flow_nodes_edges")
    @ResponseBody
    public Map get_flow_nodes_edges(@RequestBody Map<String, Object> map) {
        Integer table_id = (Integer)map.get("table_id");
        List<Map> re1 = businessMapper.get_undeleted_flow_node(Lutils.genMap("table_id", table_id));
        List<Map> re2 = businessMapper.get_undeleted_flow_edge(Lutils.genMap("table_id", table_id));
        return Lutils.genMap("nodes", re1, "edges", re2);
    }


    @PostMapping("/get_flow_update_sql_nodes_edges")
    @ResponseBody
    public Map get_flow_update_sql_nodes_edges(@RequestBody Map<String, Object> map) {
        Integer table_id = (Integer)map.get("table_id");
        List<Map> re1 = baseDBService.selectEq("v_flow_node", Lutils.genMap("type", "update-sql"));
        List<Map> re2 = businessMapper.get_undeleted_update_sql_edge(Lutils.genMap());
        return Lutils.genMap("nodes", re1, "edges", re2);
    }

    @PostMapping("/get_role_list")
    @ResponseBody
    public AjaxResult get_role_list(@RequestBody Map<String, Object> map, HttpSession httpSession) {
        UserAction ua = new UserAction();
        ua.setUserInfo(httpSession);
        map.put("user", ua);
        List<Map> roles = serviceConfigMapper.get_role_list(map);
        return AjaxResult.success("success", roles);
    }



    @PostMapping("/get_rel_group_user")
    @ResponseBody
    public  Map get_rel_group_user(@RequestBody Map<String, Object> map) {
        List<Map> users = serviceConfigMapper.get_rel_group_user(map);
        List<Map> roles = new ArrayList<>();
        if(users.size()>0) {
            roles = baseDBService.selectIn("v_user_role", "user_id", users.stream().map(o -> o.get("id")).collect(Collectors.toList()));
        }
        return Lutils.genMap("users", users, "roles", roles);
    }

    @PostMapping("/get_group_post")
    @ResponseBody
    public  Map get_group_post(@RequestBody Map<String, Object> map) {
        List<Map> groups = baseDBService.selectByCauses("v_group",null,Lutils.genMap("ord", "asc"));
        List<Map> roles = baseDBService.selectByCauses("v_role",null,Lutils.genMap("ord", "asc"));
        return Lutils.genMap("groups", groups, "roles", roles);
    }

    @PostMapping("/set_table_order")
    @ResponseBody
    public AjaxResult set_table_order(@RequestBody Map<String, Object> map) {
        int rs = 0;
        List<Integer> newOrds = ( List<Integer> )map.get("newOrds");
        String tn = (String)map.get("tn");
        for(int i=0;i<newOrds.size();i++){
            rs +=baseDBService.updateEq(tn,Lutils.genMap("ord", i+1), Lutils.genMap("id", newOrds.get(i)));
        }
        return AjaxResult.success("suc", rs);
    }

    @PostMapping("/get_flow_node_event")
    @ResponseBody
    public List<Map> get_flow_node_event(@RequestBody Map<String, Object> map) {
        return serviceConfigMapper.get_flow_node_event(map);
    }

    @PostMapping("/get_flow_edge_data_scope")
    @ResponseBody
    public List<Map> get_flow_edge_data_scope(@RequestBody Map<String, Object> map) {
        return serviceConfigMapper.get_flow_edge_data_scope(map);
    }

    @PostMapping("/get_table_recurive_relations")
    @ResponseBody
    public List<Map> get_table_recurive_relations(@RequestBody Map<String, Object> map) {
        return businessMapper.get_table_recurive_relations(map);
    }




    @Transactional
    @PostMapping("/set_flow_node_event")
    @ResponseBody
    public List<Map> set_flow_node_event(@RequestBody Map<String, Object> map) {
        Map upMap = (Map)map.get("map");
        Integer event_id = (Integer)map.get("event_id");
        Integer table_id = (Integer)map.get("table_id");
        Integer exec_id = (Integer)map.get("exec_id");
        if(event_id==null){
            baseDBService.insertMap("v_exec_trg_by_flow_event", upMap);
        }
        else{
            baseDBService.updateEq("v_exec_trg_by_flow_event", upMap, Lutils.genMap("id", event_id));
        }
        return serviceConfigMapper.get_flow_node_event(Lutils.genMap("table_id", table_id, "exec_id", exec_id));
    }


    @Transactional
    @PostMapping("/set_flow_edge_scope")
    @ResponseBody
    public List<Map> set_flow_edge_scope(@RequestBody Map<String, Object> map) {
        Map upMap = (Map)map.get("map");
        Integer id = (Integer)map.get("id"); //scope_id
        Integer ds_id = (Integer)map.get("ds_id");
        Integer table_id = (Integer)map.get("table_id");
        if(id == null){
            baseDBService.insertMap("v_grid_data_scope", upMap);
        }
        else{
            baseDBService.updateEq("v_grid_data_scope", upMap, Lutils.genMap("id", id));
        }
        return serviceConfigMapper.get_flow_edge_data_scope(Lutils.genMap("ds_id", ds_id, "table_id", table_id));
    }

//    @PostMapping("/get_flow_edge_event")
//    @ResponseBody
//    public List<Map> get_flow_edge_event(@RequestBody Map<String, Object> map) {
//        return serviceConfigMapper.get_flow_edge_event(map);
//    }



//    @Transactional
//    @PostMapping("/set_flow_edge_event")
//    @ResponseBody
//    public List<Map> set_flow_edge_event(@RequestBody Map<String, Object> map) {
//        Map upMap = (Map)map.get("map");
//        Integer event_id = (Integer)map.get("event_id");
//        Integer table_id = (Integer)map.get("table_id");
//        Integer exec_id = (Integer)map.get("exec_id");
//        if(event_id==null){
//            baseDBService.insertMap("v_exec_trg_by_edge_event", upMap);
//        }
//        else{
//            baseDBService.updateEq("v_exec_trg_by_edge_event", upMap, Lutils.genMap("id", event_id));
//        }
//        return serviceConfigMapper.get_flow_edge_event(Lutils.genMap("table_id", table_id, "exec_id", exec_id));
//    }

    @Transactional
    @PostMapping("/add_dict_item")
    @ResponseBody
    public AjaxResult add_dict_item(@RequestBody Map<String, Object> map) {
        Integer dictId = (Integer)map.get("dict_id");
        Integer rel_group_id = (Integer)map.get("rel_group_id");
        Integer rel_role_id = (Integer)map.get("rel_role_id");
        Map item = baseDBService.insertMapAutoFillMaxOrd("v_dict_item", map, "ord", Lutils.genMap("parent_id", map.get("parent_id")));
        Map scope = baseDBService.insertWhenNotExist("v_user_scope", Lutils.genMap("obj_id", item.get("id"), "obj_type", "dict_item"),
                Lutils.genMap("obj_id", item.get("id"), "obj_type", "dict_item"));
        baseDBService.insertWhenNotExistUpdateWhenExists("v_user_scope_group", Lutils.genMap("scope_id", scope.get("id"), "group_id", rel_group_id ),
                Lutils.genMap("scope_id", scope.get("id"), "group_id", rel_group_id ));
        baseDBService.insertWhenNotExistUpdateWhenExists("v_user_scope_role", Lutils.genMap("scope_id", scope.get("id"), "role_id", rel_role_id ),
                Lutils.genMap("scope_id", scope.get("id"), "role_id", rel_role_id ));
        DictStore.getInstance().set(dictId);
        return AjaxResult.success("success", DictStore.getInstance().get(dictId));
    }

    @Transactional
    @PostMapping("/update_dict_item")
    @ResponseBody
    public AjaxResult update_dict_item(@RequestBody Map<String, Object> map) {
        Integer dictId = (Integer)map.get("dict_id");
        Integer itemId = (Integer)map.get("id");
        Integer rel_group_id = (Integer)map.get("rel_group_id");
        Integer rel_role_id = (Integer)map.get("rel_role_id");
        map.remove("scope_id");
        baseDBService.updateEq("v_dict_item", map, Lutils.genMap("id", itemId));
        Map scope = baseDBService.insertWhenNotExist("v_user_scope", Lutils.genMap("obj_id", itemId, "obj_type", "dict_item"),
                Lutils.genMap("obj_id", itemId, "obj_type", "dict_item"));
        baseDBService.insertWhenNotExistUpdateWhenExists("v_user_scope_group", Lutils.genMap("scope_id", scope.get("id"), "group_id", rel_group_id ),
                Lutils.genMap("scope_id", scope.get("id"), "group_id", rel_group_id ));
        baseDBService.insertWhenNotExistUpdateWhenExists("v_user_scope_role", Lutils.genMap("scope_id", scope.get("id"), "role_id", rel_role_id ),
                Lutils.genMap("scope_id", scope.get("id"), "role_id", rel_role_id ));
        DictStore.getInstance().set(dictId);
        return AjaxResult.success("success", DictStore.getInstance().get(dictId));
    }

    @Transactional
    @PostMapping("/get_dict_items")
    @ResponseBody
    public List<Map> get_dict_items(@RequestBody Map<String, Object> map) {
        Integer dict_id = (Integer)map.get("dict_id");
        List<Map> items = DictStore.getInstance().getDictItems(dict_id);
        if (CollUtil.isEmpty(items)) {
            return new ArrayList<>();
        }
        List<Map> itemUsers = DictStore.getInstance().getDictItemScopeUsers(dict_id);
        for(Map item:items){
            List<Map> itemUserRelation = itemUsers.stream().filter(o->o.get("item_id").equals(item.get("id"))).collect(Collectors.toList());
            if(itemUserRelation.size()>0)
            item.put("users", itemUserRelation.get(0).get("users"));
        }
        return items;
    }

    @PostMapping("/get_dict_item_users")
    @ResponseBody
    public List<Map> get_dict_item_users(@RequestBody Map<String, Object> map) {
        Integer dict_id = (Integer)map.get("dict_id");
        List<Integer> dict_item_ids = (List<Integer>)map.get("item_ids");
        List<Map> itemUsers = businessMapper.get_dict_receivers(Lutils.genMap("dict_id", dict_id, "dict_item_ids", dict_item_ids));
        return itemUsers;
    }


//    @PostMapping("/get_edge_defined_receivers")
//    @ResponseBody
//    public List<Map> get_edge_defined_receivers(@RequestBody Map<String, Object> map) {
//        Integer flow_edge_id = (Integer)map.get("flow_edge_id");
//        List<Map> itemUsers = businessService.getEdgeDefinedreceivers(flow_edge_id);
//        List<Map> users = new ArrayList<>();
//        if(itemUsers.size()>0)
//            users = baseDBService.selectIn("v_user", "id", itemUsers.stream().map(o->o.get("user_id")).collect(Collectors.toList()));
//        return users;
//    }


    @PostMapping("/get_notice_settings")
    @ResponseBody
    public List<Map> get_notice_settings(@RequestBody Map<String, Object> map) {
        Integer view_id = (Integer)map.get("view_id");
        List<Map> notices = serviceConfigMapper.get_view_notice_scope(Lutils.genMap("view_id", view_id));
        return notices;
    }



    @PostMapping("/update_view_notice_scope")
    @ResponseBody
    public List<Map> update_view_notice_scope(@RequestBody Map<String, Object> map) {
        Integer view_id = (Integer)map.get("view_id");
        Integer table_id = (Integer)map.get("table_id");
        Integer obj_id = (Integer)map.get("obj_id");
        String obj_type = (String)map.get("obj_type");
        boolean checked = (Boolean)map.get("checked");
        if(!checked){
            baseDBService.deleteEq("v_view_notice_flow_condition", Lutils.genMap("view_id", view_id, "table_id",table_id,"obj_id",obj_id,"obj_type", obj_type));
        }
        else{
            //如果全选，则删除其他类型
            if(obj_id==-1){
                baseDBService.deleteEq("v_view_notice_flow_condition",Lutils.genMap("view_id", view_id, "table_id",table_id,"obj_type", obj_type));
            }
            else{//如果不全选，则删除-1
                baseDBService.deleteEq("v_view_notice_flow_condition",Lutils.genMap("view_id", view_id, "table_id",table_id,"obj_type", obj_type,"obj_id", -1));
            }
            baseDBService.insertMap("v_view_notice_flow_condition",Lutils.genMap("view_id", view_id,
                    "table_id",table_id,
                    "obj_type", obj_type,
                    "obj_id", obj_id));
        }
        List<Map> notices = serviceConfigMapper.get_view_notice_scope(Lutils.genMap("view_id", view_id));
        return notices;
    }

    @PostMapping("/get_view_aggr_count_items")
    @ResponseBody
    public List<Map> get_view_aggr_count_items(@RequestBody Map<String, Object> map) {
        Integer comp_id = (Integer)map.get("comp_id");
        List<Map> count_views = serviceConfigMapper.get_view_aggr_count_items(Lutils.genMap("comp_id", comp_id));
        return count_views;
    }


    /**
     * 配置树
     * */
    @PostMapping("/get_view_selector_tree")
    @ResponseBody
    public AjaxResult get_view_selector_tree(@RequestBody Map<String, Object> map) {
        String current_view_id = (String)map.get("current_view_id");
        String query_scope = (String) map.get("query_scope");
        String query_type = (String) map.get("query_type");
        List<String> dsComps = new ArrayList<>();
        dsComps.add("CompDataSource");
        dsComps.add("CompGrid");
        dsComps.add("CompCarousel");
        dsComps.add("CompLogTable");
        //所有节点
        List<Map> views = new ArrayList<>();
        if (query_scope.equals("all") || current_view_id == null) {
            views = businessMapper.getParentViewTree(Lutils.genMap("view_id", "view0"));
        }
        else{
            //查找文件夹
            List<Map> viewsParents = serviceConfigMapper.get_view_folder(Lutils.genMap("view_id", current_view_id));
            views = businessMapper.getViewTree(Lutils.genMap("view_id", viewsParents.get(0).get("id")));
        }
        for(Map node: views){
            View v = (View)Modules.getInstance().get(node.get("id"),false);
            if(v!=null) {
                node.put("view_type", v.view_type.equals("group") ? "folder" : v.view_type);
                if (v != null && v.view_type.equals("comp")) {
                    node.put("comp_name", v.comp_name);
                    //获取ds_id
                    if (Objects.equals(v.comp_name, "CompGrid")) {
                        CompGrid grid = (CompGrid) Modules.getInstance().get(v.comp_id, false);
                        node.put("ds_id", grid != null ? grid.ds_id : null);
                        node.put("hasDataSource", true);
                    } else if (Objects.equals(v.comp_name, "CompDataSource")) {
                        node.put("ds_id", v.comp_id);
                        node.put("hasDataSource", true);
                    } else if (Objects.equals(v.comp_name, "CompCarousel")) {
                        CompCarousel carousel = (CompCarousel) Modules.getInstance().get(v.comp_id, false);
                        node.put("ds_id", carousel != null ? carousel.ds_id : null);
                        node.put("hasDataSource", true);
                    } else if (Objects.equals(v.comp_name, "CompLogTable")) {
                        CompLogTable carousel = (CompLogTable) Modules.getInstance().get(v.comp_id, false);
                        node.put("ds_id", carousel != null ? carousel.ds_id : null);
                        node.put("hasDataSource", true);
                    }
                }
            }
            node.remove("json");
        }
        //剔除非数据源组件
        if(Objects.equals(query_type,"ds")) {
            views = views.stream().filter(n -> !(Objects.equals(n.get("view_type"), "comp") && !dsComps.contains(n.get("comp_name")))).collect(Collectors.toList());
        }
//        //只取数据源组件
//        if(Objects.equals(query_type,"ds")){
//            views = views.stream().filter(v->Objects.equals(true,v.get("hasDataSource"))).collect(Collectors.toList());
//        }
//        if(views.size()>0) {
//            List<Map> parents = businessMapper.getViewsParents(Lutils.genMap("view_ids", views.stream().map(v -> v.get("id")).collect(Collectors.toList())));
//            views.addAll(parents);
//        }
        return AjaxResult.success("success",views);
    }


    /**
     * 配置树
     * */
    @PostMapping("/get_datasource_selector_tree")
    @ResponseBody
    public AjaxResult get_datasource_selector_tree(@RequestBody Map<String, Object> map) {
        String current_view_id = (String)map.get("current_view_id");
        String query_scope = (String) map.get("query_scope");
        String query_type = (String) map.get("query_type");
        List<String> dsComps = new ArrayList<>();
        dsComps.add("CompDataSource");
        dsComps.add("CompGrid");
        dsComps.add("CompCarousel");
        dsComps.add("CompLogTable");
        //所有节点
        List<Map> views = new ArrayList<>();
        if (query_scope.equals("all") || current_view_id == null) {
            views = businessMapper.getParentViewTree(Lutils.genMap("view_id", "view0"));
        }
        else{
            //查找文件夹
            List<Map> viewsParents = serviceConfigMapper.get_view_folder(Lutils.genMap("view_id", current_view_id));
            views = businessMapper.getViewTree(Lutils.genMap("view_id", viewsParents.get(0).get("id")));
        }
        for(Map node: views){
            View v = (View)Modules.getInstance().get(node.get("id"),false);
            if(v!=null) {
                node.put("view_type", v.view_type.equals("group") ? "folder" : v.view_type);
                if (v != null && v.view_type.equals("comp")) {
                    node.put("comp_name", v.comp_name);
                    //获取ds_id
                    if (Objects.equals(v.comp_name, "CompGrid")) {
                        CompGrid grid = (CompGrid) Modules.getInstance().get(v.comp_id, false);
                        node.put("ds_id", grid != null ? grid.ds_id : null);
                        node.put("hasDataSource", true);
                    } else if (Objects.equals(v.comp_name, "CompDataSource")) {
                        node.put("ds_id", v.comp_id);
                        node.put("hasDataSource", true);
                    } else if (Objects.equals(v.comp_name, "CompCarousel")) {
                        CompCarousel carousel = (CompCarousel) Modules.getInstance().get(v.comp_id, false);
                        node.put("ds_id", carousel != null ? carousel.ds_id : null);
                        node.put("hasDataSource", true);
                    } else if (Objects.equals(v.comp_name, "CompLogTable")) {
                        CompLogTable carousel = (CompLogTable) Modules.getInstance().get(v.comp_id, false);
                        node.put("ds_id", carousel != null ? carousel.ds_id : null);
                        node.put("hasDataSource", true);
                    }
                }
            }
            node.remove("json");
        }
        //剔除非数据源组件
        if(Objects.equals(query_type,"ds")) {
            views = views.stream().filter(n -> !(Objects.equals(n.get("view_type"), "comp") && !dsComps.contains(n.get("comp_name")))).collect(Collectors.toList());
        }
        return AjaxResult.success("success",views);
    }


    @PostMapping("/get_manage_view_tree")
    @ResponseBody
    public AjaxResult get_manage_view_tree(@RequestBody Map<String, Object> map, HttpSession httpSession) {
        String selected_view_id = (String)map.get("selected_view_id");
        String root_view_id = (String)map.get("root_view_id");
        String selected_ds_id = (String)map.get("selected_ds_id");
        Integer sas_system_id = (Integer)map.get("sas_system_id");
        String highlight_view_id = null;
        UserAction ua = new UserAction();
        ua.setUserInfo(httpSession);
        List<Map> views = new ArrayList<>();
        if(root_view_id!=null) {
            List<Map> roots = serviceConfigMapper.get_view_parent_folder(Lutils.genMap("view_id", root_view_id));
            if(!roots.isEmpty()){
                Object folder = roots.get(roots.size()-1).get("id");
                views = businessMapper.getManageViewTree(Lutils.genMap("view_id", folder));

            }
        }
        else{
            views = businessMapper.getManageViewTree(Lutils.genMap( "sas_system_id", sas_system_id, "user", ua));
        }
        //
        for(Map node: views){
            View v = (View)Modules.getInstance().get(node.get("id"),true);
            if(v != null) {
                node.put("view_type", v.view_type.equals("group") ? "folder" : v.view_type);
                node.put("name", v.name);
            }
        }
        //
        if(selected_view_id!=null){
            //找到选中view所在的indexTab
            List<Map> parent_page = serviceConfigMapper.get_view_parent_page(Lutils.genMap("view_id", selected_view_id));
            if(parent_page.size()>0){
                highlight_view_id = (String)parent_page.get(0).get("id");
            }
        }
        else if(root_view_id!=null){
            //找到选中view所在的indexTab
            List<Map> parent_page = serviceConfigMapper.get_view_parent_page(Lutils.genMap("view_id", root_view_id));
            if(parent_page.size()>0){
                highlight_view_id = (String)parent_page.get(0).get("id");
            }
        }
        if(selected_ds_id!=null){
            //找到选中view所在的indexTab
            for(Map node: views){
                View v = (View)Modules.getInstance().get(node.get("id"),true);
                if(v!=null&&v.dsList!=null){
                    List<CompDataSource> fds = v.dsList.stream()
                            .filter(ds->Objects.equals(ds.id, selected_ds_id)||Objects.equals(ds.view_id, selected_ds_id))
                            .collect(Collectors.toList());
                    if(fds.size()>0){
                        highlight_view_id = v.id;
                    }
                }
            }
        }
        return AjaxResult.success("success", Lutils.genMap("highlight_view_id", highlight_view_id, "views", views));
    }

    @PostMapping("/add_view")
    @ResponseBody
    public AjaxResult add_view(@RequestBody Map<String, Object> viewMap) {
        String id = (String) viewMap.get("id");
        View v = Lutils.ObjToClass(viewMap, View.class);
        baseDBService.insertMapAutoFillMaxOrd("v_tree_view",
                Lutils.genMap("id", v.id, "parent_id", v.parent_id,"name", v.name, "view_type", v.view_type, "is_deleted", false),
                "ord",
                Lutils.genMap("parent_id", v.parent_id));
        Modules.getInstance().create(id, "View", v);
        return AjaxResult.success("success", v);
    }


    @PostMapping("/delete_view")
    @ResponseBody
    public AjaxResult delete_view(@RequestBody Map<String, Object> viewMap) {
        String id = (String) viewMap.get("id");
        baseDBService.deleteEq("v_tree_view", Lutils.genMap("id", id));
        Modules.getInstance().removeViewWithComp(id);
        return AjaxResult.success("success");
    }


    @PostMapping("/update_view_name")
    @ResponseBody
    public AjaxResult update_view_name(@RequestBody Map<String, Object> viewMap) {
        String id = (String) viewMap.get("id");
        View v = (View) Modules.getInstance().get(id, false);
        v.name = (String)viewMap.get("name");
        baseDBService.updateEq("v_tree_view", Lutils.genMap("name", v.name,"sas_system_id", viewMap.get("sas_system_id")),
                Lutils.genMap("id", v.id));
        Modules.getInstance().save(v.id, v);
        return AjaxResult.success("success");
    }

    @PostMapping("/update_view")
    @ResponseBody
    public AjaxResult update_view(@RequestBody Map<String, Object> viewMap) {
        String id = (String) viewMap.get("id");
        viewMap.put("comp", null);
        if(viewMap.get("view_type").equals("comp")){
            String comp_id = (String)viewMap.get("comp_id");
            String comp_name = (String)viewMap.get("comp_name");
            //无组件ID则生成新的ID，有组件，保持数字不变，变更View等前缀
            comp_id = comp_id==null? (comp_name+Constants.getTimeFormatId()):(comp_name+Lutils.ExtraNumberChar(comp_id));
            viewMap.put("comp_id", comp_id);
            //查看有无旧组件
            Map compMap = baseDBService.selectOne("v_module",Lutils.genMap("id", comp_id));
            //如果存在旧组件，则重新加载
            if(compMap!=null) {
                Modules.getInstance().loadFromDB(comp_id);
                //判断组件类型
                if(comp_name.equals("CompValueEditor")) {
                    CompValueEditor editor = (CompValueEditor)Modules.getInstance().get(comp_id,false);
                    if(editor.grid_id==null){
                        editor.grid_id = "CompGrid" + Constants.getTimeFormatId();
                        Modules.getInstance().createEmptyComp(editor.grid_id, "CompGrid");
                    }
                }
            }
            else{//如果无旧组件，则重新创建
                Modules.getInstance().createEmptyComp(comp_id, comp_name);
            }
        }
        View v = Lutils.ObjToClass(viewMap, View.class);
        //如果是组件
        if(v.view_type.equals("indexTab")||
                v.view_type.equals("modal")||
                v.view_type.equals("drawer")){
            if(v.dsList!=null)
            for(CompDataSource ds:v.dsList){
                ds.onlyKeepIdName();
            }
        }
        //保存
        baseDBService.updateEq("v_module",
                Lutils.genMap("json", Lutils.ObjectToJSON(v)),
                Lutils.genMap("id", id));
        Modules.getInstance().loadFromDB(id);
        baseDBService.updateEq("v_tree_view",
                Lutils.genMap("name", viewMap.get("name"), "view_type", viewMap.get("view_type")),
                Lutils.genMap("id", id));
        return AjaxResult.success("success");
    }

    @PostMapping("/create_comp")
    @ResponseBody
    public AjaxResult create_comp(@RequestBody Map<String, Object> map) {
        String pid = (String) map.get("pid");
        String comp_name = (String) map.get("comp_name");
        String comp_id = (String) map.get("comp_id");
        comp_id = comp_id==null? (comp_name+Constants.getTimeFormatId()) :(comp_name+Lutils.ExtraNumberChar(comp_id));
        Map compMap = baseDBService.selectOne("v_module",Lutils.genMap("id", comp_id));
        //如果存在旧组件，则重新加载
        if(compMap!=null) {
            Modules.getInstance().loadFromDB(comp_id);
        }
        else{//如果无旧组件，则重新创建
            Modules.getInstance().createEmptyComp(comp_id, (String)map.get("comp_name"));
        }
        return AjaxResult.success("success", comp_id);
    }

    @PostMapping("/add_aggr_count")
    @ResponseBody
    public int add_aggr_count(@RequestBody Map<String, Object> map) {
        Map re = baseDBService.insertMapAutoFillMaxOrd("v_comp_count_aggr_item", map, "ord", Lutils.genMap("comp_id", map.get("comp_id")));
        //添加按钮
        int rs = baseDBService.insertMap("v_exec_obj", Lutils.genMap("obj_id", re.get("id"), "obj_type", "comp_count_aggr_item", "style", "tag", "use_external_text", true));
        return rs;
    }

    @PostMapping("/update_datasource_field")
    @ResponseBody
    public AjaxResult update_comp_datasource_field(@RequestBody Map<String, Object> map) {
        List col_ids = (List) map.get("col_ids");
        Integer ds_id = (Integer) map.get("ds_id");
        List<Map> table_cols = baseDBService.selectIn("v_table_col","id", col_ids);
        //设置表格is_deleted为false
        serviceConfigMapper.delete_datasource_col(Lutils.genMap("ds_id", ds_id));
        //增量添加新字段数据
        for(Object id:col_ids){
            Object table_id = table_cols.stream().filter(t->t.get("id").equals(id)).map(t->t.get("table_id")).collect(Collectors.toList()).get(0);
            baseDBService.insertWhenNotExistUpdateWhenExists("v_datasource_field",
                    Lutils.genMap("table_col_id", id, "ds_id", ds_id,  "is_deleted", false,"table_id",table_id,"field_type", "table_field"),
                    Lutils.genMap("table_col_id", id, "ds_id", ds_id));
        }
        DSStore.getInstance().set(ds_id);
        return AjaxResult.success("success", DSStore.getInstance().get(ds_id));
    }

    @PostMapping("/update_datasource_flow_field")
    @ResponseBody
    public AjaxResult update_datasource_flow_field(@RequestBody Map<String, Object> map) {
        String flow_field = (String) map.get("flow_field");
        Integer flow_edge_id = (Integer) map.get("flow_edge_id");
        Integer ds_id = (Integer) map.get("ds_id");
        boolean check = (Boolean) map.get("check");
        if(check){
            //如果为真，则添加到数据库，去重添加
            baseDBService.insertWhenNotExistUpdateWhenExists("v_datasource_field",
                    Lutils.genMap("ds_id", ds_id, "flow_edge_id", flow_edge_id, "flow_field", flow_field, "is_deleted", false, "field_type", "flow_field"),
                    Lutils.genMap("ds_id", ds_id, "flow_edge_id", flow_edge_id, "flow_field", flow_field));
        }
        else{
            //如果为假，则修改is_deleted=true
            baseDBService.updateEq("v_datasource_field",Lutils.genMap("is_deleted", true),
                    Lutils.genMap("ds_id", ds_id, "flow_edge_id", flow_edge_id, "flow_field", flow_field));
        }
        DSStore.getInstance().set(ds_id);
        return AjaxResult.success("success", DSStore.getInstance().get(ds_id));
    }
//
//    @PostMapping("/switch_data_source")
//    @ResponseBody
//    public int switch_data_source(@RequestBody Map<String, Object> map) {
//        Integer ds_id = (Integer) map.get("ds_id");
//        //设置表格is_deleted为false
//        serviceConfigMapper.set_not_this_datasource_field_false(Lutils.genMap("ds_id", ds_id));
//        return 1;
//    }

    @PostMapping("/remove_node_with_children")
    @ResponseBody
    public AjaxResult remove_node_with_children(@RequestBody Map<String, Object> map) {
        String view_id = (String) map.get("view_id");
        Map view = baseDBService.selectOne("v_tree_view", Lutils.genMap("id", view_id));
        List<Map> children = businessMapper.getViewTree(Lutils.genMap("view_id", view_id));
        if(children.size()>1&&view.get("view_type").equals("folder")){
            return AjaxResult.success("failed", "分组下存在多个下级窗口，不允许删除");
        }
        List viewIds = children.stream()
                .map(o->o.get("id")).collect(Collectors.toList());
        baseDBService.deleteIn("v_tree_view", "id", viewIds);
        baseDBService.deleteIn("v_module", "id", viewIds);
        //删除数据源
        for(Map chd: children){
            Modules.getInstance().removeViewWithComp(chd.get("id").toString());
        }
        /** 待补充 */
        return AjaxResult.success("success");
    }

    @PostMapping("/add_views_of_form_field")
    @ResponseBody
    public AjaxResult add_views_of_form_field(@RequestBody Map<String, Object> map) {
        String view_id = (String) map.get("view_id");
        List<String> ds_field_ids = (List) map.get("ds_field_ids");
        Integer colNum = (Integer) map.get("colNum");
        String ds_id = (String) map.get("ds_id");
        String valueCompName = (String) map.get("compName");
        //判断当前list，如果defined_border_style为空，则补充style
        View root = new View();
        root.view_type = "form";
        root.display_style="none";
        root.is_show = true;
        root.parent_id = view_id;
        root.id = "view" + Constants.getTimeFormatId();
        root.name = "新表单"+ root.id;
        root.form_col_num = colNum;
        root.item_border_css = "border:1px solid #e3e4e7;background:#fff;overflow:hidden;border-radius:6px;";
        root.form_label_td_style = "width:80px;border:1px solid #f1f2f7;border-bottom:none;border-right:none;padding:10px;font-size:0.9rem;background:#f9faff;color:#888;";
        root.form_content_td_style = "border:1px solid #f1f2f7;border-bottom:none;border-right:none;padding:10px;font-size:0.9rem;";
        //添加根节点v_tree_view
        baseDBService.insertMapAutoFillMaxOrd("v_tree_view", Lutils.genMap("id", root.id, "parent_id", view_id, "view_type", "form", "name", root.name, "is_deleted", false)
                ,"ord", Lutils.genMap("parent_id", view_id));
        //添加根节点module
        Modules.getInstance().create(root.id, "View", root);
        //添加字段组合
        CompDataSource ds = (CompDataSource) Modules.getInstance().get(ds_id, true);
        for (int i = 0; i < ds_field_ids.size(); i++) {
            CompDataSourceField fd = ds.getField(ds_field_ids.get(i));
            /**添加标签*/
            View label = new View();
            label.id = "view" + Constants.getTimeFormatId();
            label.name = fd.fieldName;
            label.view_type = "text";
            label.is_show = true;
            label.parent_id = root.id;
            baseDBService.insertMapAutoFillMaxOrd("v_tree_view", Lutils.genMap("id", label.id, "parent_id", root.id,"view_type", label.view_type, "name", label.name, "is_deleted", false)
                    ,"ord", Lutils.genMap("parent_id", root.id));
            Modules.getInstance().create(label.id, "View", label);
            /**添加标签*/
            View value = new View();
            value.id = "view" + Constants.getTimeFormatId();
            value.view_type = "comp";
            value.comp_name = valueCompName;
            value.is_show = true;
            value.parent_id = root.id;
            baseDBService.insertMapAutoFillMaxOrd("v_tree_view",
                    Lutils.genMap("id", value.id, "parent_id", root.id,"view_type", value.view_type, "name", value.name, "is_deleted", false)
                    ,"ord", Lutils.genMap("parent_id", root.id));
            if(value.comp_name.equals("CompValueEditor")){
                value.name = fd.fieldName+"-编辑器";
                CompValueEditor edt = new CompValueEditor();
                edt.create("CompValueEditor"+ Constants.getTimeFormatId());
                edt.ds_id = ds.id;
                edt.ds_field_id = fd.id;
                //根据字段的存储类型，匹配不同的编辑器
                if(Objects.equals(fd.data_type,"varchar")){
                    edt.editor_type = "text-editor";
                }
                else if(Objects.equals(fd.data_type,"timestamp")){
                    edt.editor_type = "datetime-editor";
                }
                else if(Objects.equals(fd.data_type,"integer")){
                    edt.editor_type = "number-editor";
                }
                value.comp_id = edt.id;
                Modules.getInstance().create(edt.id, "CompValueEditor", edt);
            }
            else{
                value.name = fd.fieldName+"-渲染器";
                CompValueRender vr = new CompValueRender();
                vr.create("CompValueRender"+ Constants.getTimeFormatId());
                vr.ds_id = ds.id;
                vr.use_defined_value = false;
                vr.ds_field_id = fd.id;
                //根据字段的存储类型，匹配不同的编辑器
                if(Objects.equals(fd.data_type,"varchar")){
                    vr.render_type = "text";
                }
                else if(Objects.equals(fd.data_type,"timestamp")){
                    vr.render_type = "datetime";
                    vr.datetime_fmt = "yyyy-MM-dd hh:mm";
                }
                else if(Objects.equals(fd.data_type,"integer")){
                    vr.render_type = "text";
                }
                value.comp_id = vr.id;
                Modules.getInstance().create(vr.id, "CompValueRender", vr);
            }
            Modules.getInstance().create(value.id, "View", value);
        }
        List<Map> ll = businessMapper.getViewTree(Lutils.genMap("view_id", root.id));
        List<Object> re = new ArrayList<>();
        for(Map node: ll){
            if(!Objects.equals(node.get("view_type"),"folder")) {
                View v = (View)Modules.getInstance().get(node.get("id"), false);
                if(v!=null) {
                    v.parent_id = (String)node.get("parent_id");
                    re.add(v);
                    if (v.view_type.equals("comp") && v.comp_id != null) {
                        v.comp = Modules.getInstance().get(v.comp_id, false);
                    }
                }
            } else{
                re.add(node);
            }
        }
        //找到表名
        return AjaxResult.success("success", re);
    }

    @PostMapping("/update_chd_style")
    @ResponseBody
    public List<Map> update_chd_style(@RequestBody Map<String, Object> map) {
        Integer view_id = (Integer) map.get("view_id");
        String item_border_css = (String) map.get("item_border_css");
        String chd_l1_style = (String) map.get("chd_l1_style");
        String chd_l1_odd_style = (String) map.get("chd_l1_odd_style");
        String chd_l1_even_style = (String) map.get("chd_l1_even_style");
        String chd_l2_odd_style = (String) map.get("chd_l2_odd_style");
        String chd_l2_even_style = (String) map.get("chd_l2_even_style");
        String update_type = (String) map.get("update_type");
        //获取所有子节点
        List<Map> views = businessMapper.getViewRecrusiveByParent( Lutils.genMap("parent_id", view_id) );
        if(update_type.equals("item_border_css")){
            //更新上级节点属性
            baseDBService.updateEq("v_view",
                    Lutils.genMap("item_border_css", item_border_css),
                    Lutils.genMap("id", view_id) );
        }
        else if(update_type.equals("l1")){
            List<Map> chdViews = views.stream().filter(o -> o.get("parent_id").equals(view_id)).collect(Collectors.toList());
            //更新上级节点属性
            baseDBService.updateEq("v_view",Lutils.genMap("chd_l1_style", chd_l1_style), Lutils.genMap("id", view_id));
            //更新子节点
            for(Map container: chdViews){
                container.put("item_border_css", chd_l1_style);
                baseDBService.updateEq("v_view", Lutils.genMap("item_border_css", chd_l1_style), Lutils.genMap("id", container.get("id")));
            }
        }
        else if(update_type.equals("l1_odd")){
            List<Map> chdViews = views.stream().filter(o -> o.get("parent_id").equals(view_id)).collect(Collectors.toList());
            //更新上级节点属性
            baseDBService.updateEq("v_view", Lutils.genMap("chd_l1_odd_style", chd_l1_odd_style), Lutils.genMap("id", view_id));
            //更新子节点
            int index = 0;
            for(Map container: chdViews){
                if(index%2==0){
                    container.put("item_border_css", chd_l1_odd_style);
                    baseDBService.updateEq("v_view", Lutils.genMap("item_border_css", chd_l1_odd_style), Lutils.genMap("id", container.get("id")));
                }
                index ++;
            }
        }
        else if(update_type.equals("l1_even")){
            List<Map> chdViews = views.stream().filter(o -> o.get("parent_id").equals(view_id)).collect(Collectors.toList());
            //更新上级节点属性
            baseDBService.updateEq("v_view", Lutils.genMap("chd_l1_even_style", chd_l1_even_style), Lutils.genMap("id", view_id));
            //更新子节点
            int index = 0;
            for(Map container: chdViews){
                if(index%2==1){
                    container.put("item_border_css", chd_l1_even_style);
                    baseDBService.updateEq("v_view", Lutils.genMap("item_border_css", chd_l1_even_style), Lutils.genMap("id", container.get("id")));
                }
                index ++;
            }
        }
        else if(update_type.equals("l2_odd")){
            List<Map> chdViews = views.stream().filter(o -> o.get("parent_id").equals(view_id)).collect(Collectors.toList());
            //更新上级节点属性
            baseDBService.updateEq("v_view",Lutils.genMap("chd_l2_odd_style", chd_l2_odd_style), Lutils.genMap("id", view_id));
            //更新子节点的奇节点
            for(Map container: chdViews) {
                List<Map> chdComps = views.stream().filter(o -> o.get("parent_id").equals(container.get("id"))).collect(Collectors.toList());
                if (chdComps.size() > 1) {
                    for (int i = 0; i < chdComps.size(); i++) {
                        if (i % 2 == 0) {
                            chdComps.get(i).put("item_border_css", chd_l2_odd_style);
                            baseDBService.updateEq("v_view", Lutils.genMap("item_border_css", chd_l2_odd_style), Lutils.genMap("id", chdComps.get(i).get("id")));
                        }
                    }
                }
            }
        }
        else if(update_type.equals("l2_even")){
            List<Map> chdViews = views.stream().filter(o -> o.get("parent_id").equals(view_id)).collect(Collectors.toList());
            //更新上级节点属性
            baseDBService.updateEq("v_view",Lutils.genMap("chd_l2_even_style", chd_l2_even_style), Lutils.genMap("id", view_id));
            //更新子节点的偶节点
            for(Map container: chdViews) {
                List<Map> chdComps = views.stream().filter(o -> o.get("parent_id").equals(container.get("id"))).collect(Collectors.toList());
                if (chdComps.size() > 1) {
                    for (int i = 0; i < chdComps.size(); i++) {
                        if (i % 2 == 1) {
                            chdComps.get(i).put("item_border_css", chd_l2_even_style);
                            baseDBService.updateEq("v_view", Lutils.genMap("item_border_css", chd_l2_even_style), Lutils.genMap("id", chdComps.get(i).get("id")));
                        }
                    }
                }
            }
        }
        return views;
    }

    @PostMapping("/init_comp_value_render")
    @ResponseBody
    public AjaxResult init_comp_value_render(@RequestBody Map<String, Object> map) {
       Integer obj_id = (Integer)map.get("obj_id");
        String obj_type = (String)map.get("obj_type");
       Integer defined_ds_id = (Integer)map.get("defined_ds_id");
       Map render=baseDBService.insertWhenNotExist("v_comp_value_render",
               Lutils.genMap("obj_id", obj_id, "obj_type", obj_type,"render_type", "text"),
               Lutils.genMap("obj_id", obj_id, "obj_type", obj_type));
       //绑定的数据源视图，则修改
       if(defined_ds_id!=null){
           baseDBService.updateEq("v_comp_value_render",
                   Lutils.genMap("ds_id", defined_ds_id),
                   Lutils.genMap("obj_id", obj_id, "obj_type", obj_type));
       }
        //添加渲染器的执行对象
        Map exec = baseDBService.insertWhenNotExist("v_exec_obj",
                Lutils.genMap("obj_id", render.get("id"), "obj_type", "comp_value_render"),
                Lutils.genMap("obj_id", render.get("id"), "obj_type", "comp_value_render"));
        render.put("execObj_id", exec.get("id"));
        return AjaxResult.success("success", CompValueRenderStore.getInstance().get(render.get("id")));
    }

    @PostMapping("/add_value_editor_if_not_exists")
    @ResponseBody
    public Map add_value_editor_if_not_exists(@RequestBody Map<String, Object> map) {
        Integer obj_id = (Integer)map.get("obj_id");
        String obj_type = (String)map.get("obj_type");
        Integer defined_ds_id = (Integer)map.get("defined_ds_id");
        Map render=baseDBService.insertWhenNotExist("v_comp_value_editor",
                Lutils.genMap("obj_id", obj_id, "obj_type", obj_type,"editor_type", "text-editor"),
                Lutils.genMap("obj_id", obj_id, "obj_type", obj_type));
        //绑定的数据源视图，则修改
        if(defined_ds_id!=null){
            baseDBService.updateEq("v_comp_value_editor",
                    Lutils.genMap("ds_id", defined_ds_id),
                    Lutils.genMap("obj_id", obj_id, "obj_type", obj_type));
        }
        List<Map> list = baseDBService.selectEq("v_comp_value_editor",
                Lutils.genMap("obj_id", obj_id, "obj_type", obj_type));
        Map comp = list.get(0);
        return comp;
    }

    @PostMapping("/get_sub_flow_event_of_pri")
    @ResponseBody
    public AjaxResult get_sub_flow_event_of_pri(@RequestBody Map<String, Object> map) {
        return AjaxResult.success("success", businessMapper.get_sub_flow_event_of_pri(map));
    }



    @PostMapping("/init_comp_grid")
    @ResponseBody
    public AjaxResult init_comp_grid(@RequestBody Map<String, Object> map) {
        Integer obj_id = (Integer)map.get("obj_id");
        String obj_type = (String)map.get("obj_type");
        map.put("border_type", "full");
        map.put("show_check_col", false);
        map.put("edit_mode", false);
        map.put("content_layout", "grid");
        map.put("show_search_control", true);
        map.put("show_header", true);
        map.put("paging_mode", "full");
        map.put("page_size", 10);
        map.put("round", true);
        map.put("cell_class_name", "el_table_cell_padding_8");
        Map grid = baseDBService.insertWhenNotExist("v_comp_grid", map, Lutils.genMap("obj_type", obj_type, "obj_id", obj_id));
        DSStore.getInstance().create("comp_grid", grid.get("id"));
        return AjaxResult.success("success", CompGridStore.getInstance().get(grid.get("id")));
    }

    @PostMapping("/init_datasource")
    @ResponseBody
    public AjaxResult init_datasource(@RequestBody Map<String, Object> map) {
        Integer obj_id = (Integer)map.get("obj_id");
        String obj_type = (String)map.get("obj_type");
        Map ds = DSStore.getInstance().create(obj_type, obj_id);
        return AjaxResult.success("success",ds);
    }

    @PostMapping("/set_cached_datasource")
    @ResponseBody
    public AjaxResult set_cached_datasource(@RequestBody Map<String, Object> map) {
        Integer ds_id = (Integer) map.get("ds_id");
        DSStore.getInstance().set(ds_id);
        return AjaxResult.success("success", DSStore.getInstance().get(ds_id));
    }

    /** 为数据表增加子流程字段 */
    @PostMapping("/set_sub_flow_field")
    @ResponseBody
    public AjaxResult set_sub_flow_field(@RequestBody Map<String, Object> map) {
        Object id = map.get("table_id");
        Table tbl = (Table) Modules.getInstance().get(id, true);
        try {
            baseDBMapper.tableAddColumn(MapUtils.G("tn", tbl.table_name, "columnName", "pri_tbl_", "columnType", "integer"));
            baseDBMapper.tableAddColumn(MapUtils.G("tn", tbl.table_name, "columnName", "pri_tbl_node_", "columnType", "integer"));
            baseDBMapper.tableAddColumn(MapUtils.G("tn", tbl.table_name, "columnName", "pri_tbl_node_times_", "columnType", "integer"));
            baseDBMapper.tableAddColumn(MapUtils.G("tn", tbl.table_name, "columnName", "pri_tbl_node_finished_", "columnType", "integer"));
            baseDBMapper.tableAddColumn(MapUtils.G("tn", tbl.table_name + "_log", "columnName", "pri_tbl_", "columnType", "integer"));
            baseDBMapper.tableAddColumn(MapUtils.G("tn", tbl.table_name + "_log", "columnName", "pri_tbl_node_", "columnType", "integer"));
            baseDBMapper.tableAddColumn(MapUtils.G("tn", tbl.table_name + "_log", "columnName", "pri_tbl_node_times_", "columnType", "integer"));
            baseDBMapper.tableAddColumn(MapUtils.G("tn", tbl.table_name + "_log", "columnName", "pri_tbl_node_finished_", "columnType", "integer"));
            return AjaxResult.success("success", tbl);
        }catch(Exception e){
            e.printStackTrace();
            return AjaxResult.success("failed", e.getMessage());
        }
    }

    @PostMapping("/save_view")
    @ResponseBody
    public AjaxResult save_view(@RequestBody Map<String, Object> map) {
        Map view = (Map) map.get("view");
        baseDBService.updateEq("v_view", view, Lutils.genMap("id", view.get("id")));
        return AjaxResult.success("success");
    }

    @Transactional
    @PostMapping("/save_ds")
    @ResponseBody
    public AjaxResult save_ds(@RequestBody Map<String, Object> ds,HttpSession session) {
        //校验
        if(ds.get("data_type").equals("defined")){
//            boolean judgeRs = judge_sql((String)ds.get("data_sql"), (Integer)session.getAttribute("userId"));
//            if(!judgeRs) {
//                return AjaxResult.success("failed", "脚本校验不通过");
//            }
//            else{
                try {
                    insert_defined_sql(ds.get("id"), (String) ds.get("data_sql"), session.getAttribute("userId"));
                }catch(Exception e){
                    e.printStackTrace();
                    return AjaxResult.success("failed", "脚本校验不通过");
                }
//            }
        }
        baseDBService.updateEq("v_datasource", ds, Lutils.genMap("id", ds.get("id")));
        DSStore.getInstance().set((Integer)ds.get("id"));
        return AjaxResult.success("success", DSStore.getInstance().get(ds.get("id")));
    }

    @Transactional
    @PostMapping("/save_count_aggr")
    @ResponseBody
    public AjaxResult save_count_aggr(@RequestBody Map<String, Object> map,HttpSession session) {
        //校验
        Integer id = (Integer)map.get("id");
        baseDBService.updateEq("v_comp_count_aggr", map, Lutils.genMap("id", id));
        CompCountAggrStore.getInstance().set(id);
        return AjaxResult.success("success", DSStore.getInstance().get(id));
    }


    @PostMapping("/get_store_obj")
    @ResponseBody
    public AjaxResult get_store_obj(@RequestBody Map<String, Object> map) {
        String comp_name = (String) map.get("comp_name");
        Object key =  map.get("key");
        Object obj_type =  map.get("obj_type");
        Object obj_id =  map.get("obj_id");
        Object re = null;
        if(comp_name.equals("CompDataSource")){
            re = DSStore.getInstance().get(key);
        }
        else if(comp_name.equals("CompCountAggr")){
            re = CompCountAggrStore.getInstance().get(key);
        }
        else if(comp_name.equals("Exec")){
            re = ExecObjStore.getInstance().get(key);
        }
        else if(comp_name.equals("Execs")){
            re = ExecObjStore.getInstance().getByObj(key);
        }
        else if(comp_name.equals("Table")){
            re = TableStore.getInstance().get(key);
        }
        else if(comp_name.equals("Dict")){
            re = DictStore.getInstance().get(key);
        }
        else if(comp_name.equals("UserScope")){
            re = UserScopeStore.getInstance().get(key);
        }
        else if(comp_name.equals("Flow")){
            re = FlowStore.getInstance().get(key);
        }
        else if(comp_name.equals("CompGrid")){
            CompGridStore.getInstance().set(key);
            re = CompGridStore.getInstance().get(key);
        }
        return AjaxResult.success("success", re);
    }

    @PostMapping("/set_store_obj")
    @ResponseBody
    public AjaxResult set_store_obj(@RequestBody Map<String, Object> map) {
        String comp_name = (String) map.get("comp_name");
        Integer id =  (Integer)map.get("id");
        String obj_type =  (String)map.get("obj_type");
        Object obj_id =  map.get("obj_id");
        Object re = null;
        if(comp_name.equals("CompDataSource")){
            DSStore.getInstance().set(id);
            re = DSStore.getInstance().get(id);
        }
        else if(comp_name.equals("CompCountAggr")){
            CompCountAggrStore.getInstance().set(id);
            re = CompCountAggrStore.getInstance().get(id);
        }
        else if(comp_name.equals("CompGrid")){
            CompGridStore.getInstance().set(id);
            re = CompGridStore.getInstance().get(id);
        }
        else if(comp_name.equals("Exec")){
            ExecObjStore.getInstance().set(id);
            re = ExecObjStore.getInstance().get(id);
        }
        else if(comp_name.equals("Execs")){
            ExecObjStore.getInstance().set(obj_type, obj_id);
            re = ExecObjStore.getInstance().getByObj(obj_type+obj_id);
        }
        else if(comp_name.equals("Table")){
            TableStore.getInstance().set(id);
            re = TableStore.getInstance().get(id);
        }
        else if(comp_name.equals("Dict")){
            DictStore.getInstance().set(id);
            re = DictStore.getInstance().get(id);
        }
        else if(comp_name.equals("UserScope")){
            UserScopeStore.getInstance().set(id);
            re = UserScopeStore.getInstance().get(id);
        }
        else if(comp_name.equals("Flow")){
            FlowStore.getInstance().set(id);
            re = FlowStore.getInstance().get(id);
        }
        else if(comp_name.equals("CompGrid")){
            CompGridStore.getInstance().set(id);
            re = CompGridStore.getInstance().get(id);
        }
        return AjaxResult.success("success", re);
    }

    @PostMapping("/init_comp_count_aggr")
    @ResponseBody
    public AjaxResult init_comp_count_aggr(@RequestBody Map<String, Object> map) {
        Integer obj_id = (Integer)map.get("obj_id");
        String obj_type = (String)map.get("obj_type");
        Map comp = baseDBService.insertWhenNotExist("v_comp_count_aggr", Lutils.genMap("obj_id", obj_id, "obj_type", obj_type,"content_layout","value","list_type","single"),
                Lutils.genMap("obj_id", obj_id, "obj_type", obj_type));
        //初始化绑定的按钮
        ExecObjStore.getInstance().createWhenNotExist("comp_count_aggr", comp.get("id"), null);
        //更新到数据源仓库
        CompCountAggrStore.getInstance().set((Integer)comp.get("id"));
        return AjaxResult.success("success",CompCountAggrStore.getInstance().get(comp.get("id")));
    }

    @PostMapping("/init_comp_card")
    @ResponseBody
    public AjaxResult init_comp_card(@RequestBody Map<String, Object> map) {
        Integer obj_id = (Integer)map.get("obj_id");
        String obj_type = (String)map.get("obj_type");
        Map comp = baseDBService.insertWhenNotExist("v_comp_card", Lutils.genMap("obj_id", obj_id, "obj_type", obj_type),
                Lutils.genMap("obj_id", obj_id, "obj_type", obj_type));
        //初始化绑定的按钮
        ExecObjStore.getInstance().createWhenNotExist("comp_card", comp.get("id"), null);
        //更新到数据源仓库
        CompCardStore.getInstance().set((Integer)comp.get("id"));
        return AjaxResult.success("success",CompCardStore.getInstance().get(comp.get("id")));
    }

    @PostMapping("/save_card")
    @ResponseBody
    public AjaxResult save_card(@RequestBody Map<String, Object> map) {
        baseDBService.updateEq("v_comp_card", map, Lutils.genMap("id", map.get("id")));
        CompCardStore.getInstance().set((Integer)map.get("id"));
        return AjaxResult.success("success", CompCardStore.getInstance().get((Integer)map.get("id")) );
    }

    @PostMapping("/init_comp_timeline")
    @ResponseBody
    public AjaxResult init_comp_timeline(@RequestBody Map<String, Object> map) {
        Integer obj_id = (Integer)map.get("obj_id");
        String obj_type = (String)map.get("obj_type");
        Map comp = baseDBService.insertWhenNotExist("v_comp_timeline", Lutils.genMap("obj_id", obj_id, "obj_type", obj_type),
                Lutils.genMap("obj_id", obj_id, "obj_type", obj_type));
        //初始化绑定的按钮
        ExecObjStore.getInstance().createWhenNotExist("comp_timeline", comp.get("id"), null);
        //更新到数据源仓库
        CompTimelineStore.getInstance().set((Integer)comp.get("id"));
        return AjaxResult.success("success",CompTimelineStore.getInstance().get(comp.get("id")));
    }

    @PostMapping("/save_timeline")
    @ResponseBody
    public AjaxResult save_timeline(@RequestBody Map<String, Object> map) {
        baseDBService.updateEq("v_comp_timeline", map, Lutils.genMap("id", map.get("id")));
        CompTimelineStore.getInstance().set((Integer)map.get("id"));
        return AjaxResult.success("success", CompTimelineStore.getInstance().get((Integer)map.get("id")) );
    }

    @PostMapping("/init_comp_carousel")
    @ResponseBody
    public AjaxResult init_comp_carousel(@RequestBody Map<String, Object> map) {
        Integer obj_id = (Integer)map.get("obj_id");
        String obj_type = (String)map.get("obj_type");
        Map comp = baseDBService.insertWhenNotExist("v_comp_carousel", Lutils.genMap("obj_id", obj_id, "obj_type", obj_type,"item_num",4,"height",255),
                Lutils.genMap("obj_id", obj_id, "obj_type", obj_type));
        DSStore.getInstance().create("comp_carousel", comp.get("id"));
        //初始化绑定的按钮
        ExecObjStore.getInstance().createWhenNotExist("comp_carousel", comp.get("id"), null);
        //更新到数据源仓库
        CompCarouselStore.getInstance().set((Integer)comp.get("id"));
        return AjaxResult.success("success",CompCarouselStore.getInstance().get(comp.get("id")));
    }

    @PostMapping("/save_carousel")
    @ResponseBody
    public AjaxResult save_carousel(@RequestBody Map<String, Object> map) {
        baseDBService.updateEq("v_comp_carousel", map, Lutils.genMap("id", map.get("id")));
        CompCarouselStore.getInstance().set((Integer)map.get("id"));
        return AjaxResult.success("success", CompCarouselStore.getInstance().get((Integer)map.get("id")) );
    }

    @PostMapping("/add_exec_op")
    @ResponseBody
    public AjaxResult add_exec_op(@RequestBody Map<String, Object> map) {
        Integer exec_id =  (Integer) map.get("exec_id");
        baseDBService.insertMapAutoFillMaxOrd("v_exec_op", map, "ord", Lutils.genMap("exec_id",exec_id));
        Map exec = baseDBService.selectOne("v_exec_obj", Lutils.genMap("id", exec_id));
        ExecObjStore.getInstance().set(exec.get("obj_type"), exec.get("obj_id"));
        return AjaxResult.success("success", ExecObjStore.getInstance().get(exec_id));
    }

    @PostMapping("/save_exec_op")
    @ResponseBody
    public AjaxResult save_exec_op(@RequestBody Map<String, Object> map) {
        Integer exec_id =  (Integer) map.get("exec_id");
        Integer op_id =  (Integer) map.get("id");
        baseDBService.updateEq("v_exec_op", map,  Lutils.genMap("id", op_id));
        Map exec = baseDBService.selectOne("v_exec_obj", Lutils.genMap("id", exec_id));
        ExecObjStore.getInstance().set(exec.get("obj_type"), exec.get("obj_id"));
        return AjaxResult.success("success", ExecObjStore.getInstance().get(exec_id));
    }

    @PostMapping("/delete_exec_op")
    @ResponseBody
    public AjaxResult delete_exec_op(@RequestBody Map<String, Object> map) {
        Integer op_id =  (Integer) map.get("op_id");
        Map op = baseDBService.selectOne("v_exec_op", Lutils.genMap("id",op_id));
        Map exec = baseDBService.selectOne("v_exec_obj", Lutils.genMap("id",op.get("exec_id")));
        if(op!=null) {
            baseDBService.deleteEq("v_exec_op_session", Lutils.genMap("op_id", op.get("id")));
            baseDBService.deleteEq("v_exec_op", Lutils.genMap("id", op_id));
            ExecObjStore.getInstance().set(exec.get("obj_type"), exec.get("obj_id"));
            return AjaxResult.success("success", ExecObjStore.getInstance().get(exec.get("id")));
        }
        else{
            return AjaxResult.success("failed", "删除的对象不存在");
        }
    }

    @PostMapping("/add_exec_obj")
    @ResponseBody
    public AjaxResult add_exec_obj(@RequestBody Map<String, Object> map) {
        Map exec = baseDBService.insertMapAutoFillMaxOrd("v_exec_obj", map, "ord",
                Lutils.genMap("obj_type", map.get("obj_type"), "obj_id", map.get("obj_id") ));
        ExecObjStore.getInstance().set(exec.get("obj_type"), exec.get("obj_id"));
        return AjaxResult.success("success", ExecObjStore.getInstance().getByObj((String)exec.get("obj_type")+ exec.get("obj_id")));
    }

    @PostMapping("/save_exec_obj")
    @ResponseBody
    public AjaxResult save_exec_obj(@RequestBody Map<String, Object> map) {
        baseDBService.updateEq("v_exec_obj", map, Lutils.genMap("id", map.get("id")));
        ExecObjStore.getInstance().set(map.get("obj_type"), map.get("obj_id"));
        return AjaxResult.success("success", ExecObjStore.getInstance().getByObj((String)map.get("obj_type")+ map.get("obj_id")));
    }

    @PostMapping("/delete_exec_obj")
    @ResponseBody
    public AjaxResult delete_exec_obj(@RequestBody Map<String, Object> map) {
        Integer id = (Integer) map.get("id");
        Map exec = baseDBService.selectOne("v_exec_obj", Lutils.genMap("id", id));
        if(exec!=null) {
            List<Map> ops = baseDBService.selectEq("v_exec_op", Lutils.genMap("exec_id", id));
            if(ops.size()>0) {
                baseDBService.deleteEq("v_exec_op",  Lutils.genMap("exec_id", id));
                baseDBService.deleteIn("v_exec_op_session",  "op_id", ops.stream().map(o->o.get("id")).collect(Collectors.toList()));
            }
            baseDBService.deleteEq("v_exec_obj", Lutils.genMap("id", id));
            ExecObjStore.getInstance().set(exec.get("obj_type"), exec.get("obj_id"));
            return AjaxResult.success("success", ExecObjStore.getInstance().getByObj((String)exec.get("obj_type")+ exec.get("obj_id")));
        }
        else
            return AjaxResult.success("failed", "删除的对象未找到");
    }

    @PostMapping("/create_dict")
    @ResponseBody
    @Transactional
    public AjaxResult create_dict(@RequestBody Map<String, Object> map, HttpSession session) {
        map.put("is_deleted", false);
        map.put("node_type", "dict");
        baseDBService.insertMapAutoFillMaxOrd("v_dict", map, "ord", Lutils.genMap("parent_id", map.get("parent_id")));
        return AjaxResult.success("success", "添加成功");
    }

    @PostMapping("/init_user_scope")
    @ResponseBody
    @Transactional
    public AjaxResult init_user_scope(@RequestBody Map<String, Object> map, HttpSession session) {
        Integer obj_id = (Integer)map.get("obj_id");
        Object obj_type = map.get("obj_type");
        Map scope = baseDBService.insertWhenNotExist("v_user_scope", Lutils.genMap("obj_type", obj_type,"obj_id", obj_id),Lutils.genMap("obj_type", obj_type,"obj_id", obj_id));
        UserScopeStore.getInstance().set((Integer)scope.get("id"));
        return AjaxResult.success("success", UserScopeStore.getInstance().get((Integer)scope.get("id")));
    }

    @PostMapping("/update_user_scope_unit")
    @ResponseBody
    public AjaxResult update_user_scope_unit(@RequestBody Map<String, Object> map) {
        String obj_type = (String) map.get("obj_type");
        Integer obj_id = (Integer) map.get("obj_id");
        Map scope = baseDBService.selectOne("v_user_scope", Lutils.genMap("obj_type", obj_type, "obj_id", obj_id));
        String unit = (String) map.get("unit");
        List<Integer> checkedKeys = (List) map.get("checkedKeys");
        baseDBService.deleteEq("v_user_scope_" + unit, Lutils.genMap("scope_id", scope.get("id")));
        int updateCount = 0;
        for (Integer key : checkedKeys) {
            baseDBService.insertMap("v_user_scope_" + unit, Lutils.genMap("scope_id", scope.get("id"), unit + "_id", key));
        }
        UserScopeStore.getInstance().set((Integer)scope.get("id"));
        return AjaxResult.success("success");
    }

    @PostMapping("/get_scoped_user")
    @ResponseBody
    @Transactional
    public AjaxResult get_scoped_user(@RequestBody Map<String, Object> map, HttpSession httpSession) {
        UserScope userScope = new UserScope(map);
        UserAction userAction = new UserAction();
        userAction.setUserInfo(httpSession);
        ExecContext execContext_s = new ExecContext();
        execContext_s.addContexts((List)map.get("session"));
        Map re = CompUtils.getInstance().getScopedUsersWithGroup(userScope, execContext_s, userAction);
        return AjaxResult.success("success", re);
    }

    @PostMapping("/add_menu")
    @ResponseBody
    @Transactional
    public AjaxResult add_menu(@RequestBody Map<String, Object> map, HttpSession session) {
        Map rs = baseDBService.insertMapAutoFillMaxOrd("v_menu", map, "ord", Lutils.genMap("parent_id", map.get("parent_id")));
//        MenuStore.getInstance().InitAll();
        return AjaxResult.success("success");
    }

    @PostMapping("/save_menu")
    @ResponseBody
    @Transactional
    public AjaxResult save_menu(@RequestBody Map<String, Object> map, HttpSession session) {
        Integer menu_id = (Integer)map.get("id");
        int rs = baseDBService.updateEq("v_menu", map, Lutils.genMap("id", menu_id));
        return AjaxResult.success("success");
    }

    @PostMapping("/delete_menu")
    @ResponseBody
    @Transactional
    public AjaxResult delete_menu(@RequestBody Map<String, Object> map, HttpSession session) {
        Integer menu_id = (Integer)map.get("id");
        List<Map> childs = baseDBService.selectEq("v_menu", Lutils.genMap("parent_id", menu_id, "is_deleted", false));
        if(!childs.isEmpty()){
            return AjaxResult.success("failed", "该菜单包含子节点，请先删除子节点！");
        }
        baseDBService.updateEq("v_menu", Lutils.genMap("is_deleted", true), Lutils.genMap("id", menu_id));
//        MenuStore.getInstance().InitAll();
        return AjaxResult.success("success");
    }

    @PostMapping("/save_comp_grid")
    @ResponseBody
    @Transactional
    public AjaxResult save_comp_grid(@RequestBody Map<String, Object> map, HttpSession session) {
        Integer id = (Integer)map.get("id");
        int rs = baseDBService.updateEq("v_comp_grid", map, Lutils.genMap("id", id));
        CompGridStore.getInstance().set(id);
        return AjaxResult.success("success", CompGridStore.getInstance().get(id));
    }

    @PostMapping("/save_comp_value_render")
    @ResponseBody
    @Transactional
    public AjaxResult save_comp_value_render(@RequestBody Map<String, Object> map, HttpSession session) {
        Integer id = (Integer)map.get("id");
        map.remove("compDataSource_obj_type");
        baseDBService.updateEq("v_comp_value_render", map, Lutils.genMap("id", id));
        CompValueRenderStore.getInstance().set(id);
        return AjaxResult.success("success", CompValueRenderStore.getInstance().get(id));
    }


    @PostMapping("/save_comp_value_editor")
    @ResponseBody
    @Transactional
    public AjaxResult save_comp_value_editor(@RequestBody Map<String, Object> map, HttpSession session) {
        Integer id = (Integer)map.get("id");
        int rs = baseDBService.updateEq("v_comp_value_editor", map, Lutils.genMap("id", id));
        CompValueEditorStore.getInstance().set(id);
        return AjaxResult.success("success", CompValueEditorStore.getInstance().get(id));
    }


    @PostMapping("/create_exec_obj")
    @ResponseBody
    @Transactional
    public AjaxResult create_exec_obj(@RequestBody Map<String, Object> map, HttpSession session) {
        String obj_type = (String)map.get("obj_type");
        Integer obj_id = (Integer)map.get("obj_id");
        Map exec = ExecObjStore.getInstance().create(obj_type, obj_id, null);
        return AjaxResult.success("success", exec);
    }

    @PostMapping("/create_module")
    @ResponseBody
    @Transactional
    public AjaxResult create_module(@RequestBody Map<String, Object> map, HttpSession session) {
        String id = (String)map.get("id");
        String type = (String)map.get("type");
        Map obj = (Map)map.get("obj");
        Modules.getInstance().create(id, type, obj);
        return AjaxResult.success("success", Modules.getInstance().get(id, true));
    }

    @PostMapping("/get_module")
    @ResponseBody
    @Transactional
    public AjaxResult get_module(@RequestBody Map<String, Object> map, HttpSession session) {
        String id = (String)map.get("id");
        return AjaxResult.success("success", Modules.getInstance().get(id, true));
    }

    @PostMapping("/update_module")
    @ResponseBody
    public AjaxResult update_module(@RequestBody Map<String, Object> map, HttpSession session) {
        String id = (String)map.get("id");
        Modules.getInstance().update(map, dataSource);
//        ExecStore.getInstance().setCompExec(obj); 更新完界面，同步更新Exec缓存
        return AjaxResult.success("success", Modules.getInstance().get(id, true));
    }

    @PostMapping("/update_comp_value_editor")
    @ResponseBody
    public AjaxResult update_comp_value_editor(@RequestBody Map<String, Object> map, HttpSession session) {
        String id = (String)map.get("id");
        String editor_type=(String) map.get("editor_type");
        if(editor_type.equals("foreign-key-editor")){
            String grid_id = (String)map.get("grid_id");
            if(grid_id==null){
                grid_id = "CompGrid"+Constants.getTimeFormatId();
                Modules.getInstance().createEmptyComp(grid_id, "CompGrid");
                map.put("grid_id", grid_id);
            }
        }
        Modules.getInstance().update(map,dataSource);
        return AjaxResult.success("success", Modules.getInstance().get(id, true));
    }

    @PostMapping("/get_random_id")
    @ResponseBody
    @Transactional
    public AjaxResult get_random_id(@RequestBody Map<String, Object> map, HttpSession session) {
        return AjaxResult.success("success", Constants.getTimeFormatId());
    }
    @PostMapping("/get_phone_code")
    @ResponseBody
    @Transactional
    public AjaxResult get_phone_code(@RequestBody Map<String, Object> map) {
        String phoneNumber=(String)map.get("phoneNumber");

        var res2 = SmsUtils.sendCode(phoneNumber);
        if (res2 != null) {
            System.out.println("通知短信发送结果: " +res2.getBody().getMessage());
        }
        return AjaxResult.success("success", 1);
    }


    @PostMapping("/get_view_export_json")
    @ResponseBody
    public AjaxResult get_view_export_json(@RequestBody Map<String, Object> map) {
        String view_id=(String)map.get("view_id");
        Boolean change_id=(Boolean)map.get("change_id");
        return AjaxResult.success("success", Modules.getInstance().getViewExportJson(view_id,change_id));
    }

    @PostMapping("/import_view_json")
    @ResponseBody
    public AjaxResult import_view_json(@RequestBody Map<String, Object> map) {
        String view_id=(String)map.get("import_to_view_id");
        String json=(String)map.get("json");  //root_view_id
        String text = "";
        try {
            Map result = Lutils.StringToClass(json, HashMap.class);
            Modules.getInstance().importViewsByJson(result, view_id);
        }catch(Exception e){
            text = e.getMessage();
            return AjaxResult.success("failed", text);
        }
        return AjaxResult.success("success", text);
    }


    @PostMapping("/async_views")
    @ResponseBody
    public AjaxResult async_views(@RequestBody Map<String, Object> map) {
        Modules.getInstance().InitAll();
        return AjaxResult.success("success");
    }


    @PostMapping("/create_datasource")
    @ResponseBody
    public AjaxResult create_datasource(@RequestBody Map<String, Object> map) {
        String view_id = (String) map.get("view_id");
        CompDataSource ds = Lutils.ObjToClass(map.get("ds"), CompDataSource.class);
        ds.id = "CompDataSource"+Constants.getTimeFormatId();
        View v = (View)Modules.getInstance().get(view_id, false);
        if(v.dsList==null)
            v.dsList = new ArrayList<>();
        CompDataSource ds1 = Lutils.deepCopy(ds);
        ds1.onlyKeepIdName();
        v.dsList.add(ds1);
        Modules.getInstance().create(ds.id, "CompDataSource", ds);
        Modules.getInstance().save(v.id, v);
        return AjaxResult.success("success", ds);
    }


    @PostMapping("/remove_datasource")
    @ResponseBody
    public AjaxResult remove_datasource(@RequestBody Map<String, Object> map) {
        String view_id = (String)map.get("view_id");
        String ds_id = (String)map.get("ds_id");
        View v = (View)Modules.getInstance().get(view_id, false);
        v.dsList = v.dsList.stream().filter(ds->!Objects.equals(ds.id, ds_id)).collect(Collectors.toList());
        Modules.getInstance().save(v.id, v);
        //删除数据源
        Modules.getInstance().remove(ds_id);
        return AjaxResult.success("success");
    }


    @PostMapping("/save_datasource")
    @ResponseBody
    public AjaxResult save_datasource(@RequestBody Map<String, Object> map) {
        String view_id = (String)map.get("view_id");
        Map dsMap = (Map)map.get("ds");
        CompDataSource ds = Lutils.ObjToClass(dsMap, CompDataSource.class);
        String ds_id = dsMap.get("id").toString();
        //线view->dsList的name
        View v = (View)Modules.getInstance().get(view_id, false);
        List <CompDataSource> fds = v.dsList.stream().filter(d->Objects.equals(d.id, ds_id)).collect(Collectors.toList());
        if(fds.size()>0){
            fds.get(0).name = ds.name;
        }
        Modules.getInstance().save(v.id, v);
        ds.setFields();
        Modules.getInstance().save(ds.id, ds);
        //删除数据源
        return AjaxResult.success("success");
    }

    public String replaceSql_z_table_ids_for_test(String upd_sql) {
        Pattern pattern = Pattern.compile("\\.\\.\\.z_table\\d+_ids");
        Matcher matcher = pattern.matcher(upd_sql);
        while (matcher.find()) {
            System.out.println("Found '" + matcher.group() + "' at position " + matcher.start() + " to " + matcher.end());
            String z_table_ids_param = matcher.group();
            String table_id = z_table_ids_param.replace('.', ' ').replace("z_", "").replace("_ids", "").trim();
            upd_sql = upd_sql.replace(z_table_ids_param, "1");
        }
        return upd_sql;
    }

    @PostMapping("/get_defined_sql_fields")
    @ResponseBody
    public AjaxResult get_defined_sql_fields(@RequestBody Map<String, Object> map) throws Exception {
        String data_sql = (String)map.get("data_sql");
        List<CompDataSourceField> fields = new ArrayList<>();
        if(data_sql==null)
            return AjaxResult.success("failed", "未提交sql语句");
        Pattern pattern1 = Pattern.compile("#\\{\\w+\\}");
        Matcher matcher1 = pattern1.matcher(data_sql);
        while (matcher1.find()) {
            String match = matcher1.group();
            data_sql = data_sql.replace(match, "10000");
        }
        data_sql = replaceSql_z_table_ids_for_test(data_sql);
        //自动释放连接
        try {
            SqlJdbcIntrospector.ColumnsAndTables r1 =
                    SqlJdbcIntrospector.extractColumnsAndTables(dataSource, data_sql, 3);
            List<String> fieldAliases = r1.getColumns();
            List<String> tableNames = r1.getTables();
            for (String defined_field : fieldAliases) {
                CompDataSourceField field = new CompDataSourceField();
//                field.defined_field = defined_field;
                field.field_type = "defined_field";
                field.id = defined_field;
                field.field = defined_field;
                field.fieldName = defined_field;
                if (defined_field.matches("t\\d+_.*")) {
                    Pattern pattern = Pattern.compile("t\\d+_");
                    Matcher matcher = pattern.matcher(defined_field);
                    if (matcher.find()) {
                        String match = matcher.group();
                        String table_id = match.replace("t", "table").replace("_", "");
                        Table table = (Table) Modules.getInstance().get(table_id, false);
                        if (table != null) {
                            for (int i = 0; i < table.cols.size(); i++) {
                                if (table.cols.get(i).field.equals(defined_field.replace(match, ""))) {
                                    field.table_col_id = table.cols.get(i).id;
                                }
                            }
                            field.table_id = table_id;
                        }
                    } else {
                        //未找到表名
                    }
                }
                fields.add(field);
            }
        }catch (Exception e){
            return AjaxResult.success("failed", e.getMessage());
        }
        //删除数据源
        return AjaxResult.success("success", fields);
    }

    @PostMapping("/get_ds_ls_fields")
    @ResponseBody
    public AjaxResult get_ds_ls_fields(@RequestBody Map<String, Object> map) throws Exception {
        List<Map> dsFields = new ArrayList();
        List<String> ds_ids = (List)map.get("ds_ids");
        for(String ds_id: ds_ids.stream().distinct().collect(Collectors.toList())){
            CompDataSource ds = (CompDataSource) Modules.getInstance().get(ds_id, false);
            Map re = Lutils.genMap("ds_id", ds_id, "fields", ds.fields.stream().map(f->f.field));
            dsFields.add(re);
        }
        //删除数据源
        return AjaxResult.success("success", dsFields);
    }

    @PostMapping("/gen_table_sql")
    @ResponseBody
    public AjaxResult gen_table_sql(@RequestBody Map<String, Object> map)
    {
        CompDataSource ds = Lutils.ObjToClass(map, CompDataSource.class);
        return AjaxResult.success("success", CompUtils.gen_ds_sql(ds));
    }

    @PostMapping("/get_exec_op_env_tables")
    @ResponseBody
    public AjaxResult get_exec_op_env_tables(@RequestBody Map<String, Object> map)
    {
        String exec_id = map.get("exec_id").toString();
        String op_id = map.get("op_id").toString();
        Exec exec = ExecStore.getInstance().get(exec_id);
        List<ExecOp> ops = exec.ops.stream().filter(o->o.id.equals(op_id)).collect(Collectors.toList());
        ExecOp op = ops.get(0);
        List<Map> op_tables = new ArrayList<>();
        String []ds_ops={"insert", "update", "data_sync", "validate"};
        String []table_ops={"complete", "delete"};
        String table_id = null;
        if(Arrays.asList(ds_ops).contains(op.op_type)|| Arrays.asList(table_ops).contains(op.op_type)){
            if(Arrays.asList(ds_ops).contains(op.op_type)) {
                CompDataSource ds = null;
                Object obj = Modules.getInstance().get(op.ds_id, false);
                //兼容老版本，老版本insert是绑定的view_id
                if(obj!=null && obj instanceof View){
                    ds = (CompDataSource) Modules.getInstance().get(((View) obj).comp_id, false);
                }
                else if(obj!=null && obj instanceof CompDataSource){
                    ds = (CompDataSource) obj;
                }
                else if (op.view_id != null) {
                    ds = (CompDataSource) Modules.getInstance().get(op.view_id + "ds", false);
                }
                table_id = ds.table_id;
            }
            else if(Arrays.asList(table_ops).contains(op.op_type)){
                table_id = op.table_id;
            }
        }
        else if(op.op_type.equals("trans")){
            String upd_type = null;
            if ((table_id = CompUtils.ExtraOperatedTable(op.trans_sql, "update\\s+z_table\\d+")) != null) {//新增
                upd_type = "update";
            } else if ((table_id = CompUtils.ExtraOperatedTable(op.trans_sql, "insert\\s+into\\s+z_table\\d+")) != null) {//更新
                upd_type = "insert";
            } else if ((table_id = CompUtils.ExtraOperatedTable(op.trans_sql, "delete\\s+from\\s+z_table\\d+")) != null) {//删除
                upd_type = "delete";
            }
        }
        else if(op.op_type.equals("validate")){
            CompDataSource ds = (CompDataSource) Modules.getInstance().get(op.ds_id, false);
            table_id = ds.table_id;
        }

        if(table_id!=null) {
            Table tbl = (Table) Modules.getInstance().get(table_id, false);
            op_tables = tbl.listUpDownRel();
        }
        return AjaxResult.success("success", op_tables);
    }
}
