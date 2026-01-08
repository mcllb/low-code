package tcdx.uap.service.store;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import net.sf.jsqlparser.JSQLParserException;
import tcdx.uap.common.utils.Lutils;
import tcdx.uap.common.utils.SqlJdbcIntrospector;
import tcdx.uap.constant.Constants;
import tcdx.uap.mapper.BusinessMapper;
import tcdx.uap.service.BaseDBService;
import tcdx.uap.service.BusinessService;
import tcdx.uap.service.entities.*;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

public class Modules {

    @Getter
    private static final Modules instance = new Modules();
    private Modules()
    {
        System.out.println("单例模式初始化权限对象仓库");
    }

    BaseDBService db;
    BusinessMapper busiMapper;
    BusinessService busi;
    Map<String,Object> modules = new HashMap();
    //存储数据源与表的关系
    public void InitAll(BaseDBService db,BusinessMapper busiMapper,BusinessService busi){
        this.db = db;
        this.busiMapper = busiMapper;
        this.busi = busi;
        InitAll();
    }

    public void InitAll(){
        List<Map> v_module = db.selectByCauses("v_module", new HashMap(), null);
        assemble(v_module);
        Date start = new Date();
        System.out.println("加载模块数据完成");
        System.out.println("处理数据表深度信息...");
        for (Map.Entry<String, Object> entry : modules.entrySet()) {
            Object obj = entry.getValue();
            if(obj instanceof Table){
                Table m = (Table)obj;
                m.setDeepInfo();
            }
        }
        Date end = new Date();
        long diff = end.getTime() - start.getTime();
        System.out.println("数据表深度信息处理完成，用时"+diff+"ms");
        System.out.println("处理数据源深度信息...");
        start = new Date();
        HandlerDataSourceField(null);
        end = new Date();
        diff = end.getTime() - start.getTime();
        System.out.println("数据源字段处理完成，用时"+diff+"ms");
    }

    public void HandlerDataSourceField(String table_id){
        if(table_id==null) {
            for (Map.Entry<String, Object> entry : modules.entrySet()) {
                Object obj = entry.getValue();
                if (obj instanceof CompDataSource) {
                    CompDataSource ds = (CompDataSource) obj;
                    ds.setFields();
                }
            }
        }
        else{
            for (Map.Entry<String, Object> entry : modules.entrySet()) {
                Object obj = entry.getValue();
                if (obj instanceof CompDataSource) {
                    CompDataSource ds = (CompDataSource) obj;
                    if(Objects.equals(ds.table_id, table_id))
                        ds.setFields();
                }
            }
        }
    }

    public void loadFromDB(Object id){
        List<Map> v_modules = db.selectEq("v_module", Lutils.genMap("id", id));
        assemble(v_modules);
    }

    public void assemble(List<Map> v_modules){
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        try {
            for(Map m : v_modules){
                if(m.get("type").equals("Table")){
                    Table obj = mapper.readValue(m.get("json").toString(), Table.class);
                    modules.put(m.get("id").toString(), obj);
//                    updateTableFlowNodeEdges(obj);
                }
                else if(m.get("type").equals("View")){
                    View obj = mapper.readValue(m.get("json").toString(), View.class);
//                    if(Objects.equals(obj.comp_name,"CompDataSource")){
//
//                    }
                    modules.put(m.get("id").toString(), obj);
                }
                else if(m.get("type").equals("CompCarousel")){
                    CompCarousel obj = mapper.readValue(m.get("json").toString(), CompCarousel.class);
                    modules.put(m.get("id").toString(), obj);
                }
                else if(m.get("type").equals("CompGrid")){
                    CompGrid obj = mapper.readValue(m.get("json").toString(), CompGrid.class);
                    modules.put(m.get("id").toString(), obj);
                }
                else if(m.get("type").equals("CompDataSource")){
                    CompDataSource obj = mapper.readValue(m.get("json").toString(), CompDataSource.class);
                    modules.put(m.get("id").toString(), obj);
                }
                else if(m.get("type").equals("CompCountAggr")){
                    CompCountAggr obj = mapper.readValue(m.get("json").toString(), CompCountAggr.class);
                    modules.put(m.get("id").toString(), obj);
                }
                else if(m.get("type").equals("CompValueRender")){
                    CompValueRender obj = mapper.readValue(m.get("json").toString(), CompValueRender.class);
                    modules.put(m.get("id").toString(), obj);
                }
                else if(m.get("type").equals("CompValueEditor")){
                    CompValueEditor obj = mapper.readValue(m.get("json").toString(), CompValueEditor.class);
                    modules.put(m.get("id").toString(), obj);
                }
                else if(m.get("type").equals("CompUserSelector")){
                    CompUserSelector obj = mapper.readValue(m.get("json").toString(), CompUserSelector.class);
                    modules.put(m.get("id").toString(), obj);
                }
                else if(m.get("type").equals("CompCard")){
                    CompCard obj = mapper.readValue(m.get("json").toString(), CompCard.class);
                    modules.put(m.get("id").toString(), obj);
                }
                else if(m.get("type").equals("CompLogTable")){
                    CompLogTable obj = mapper.readValue(m.get("json").toString(), CompLogTable.class);
                    modules.put(m.get("id").toString(), obj);
                }
                else if(m.get("type").equals("CompEcharts")){
                    CompEcharts obj = mapper.readValue(m.get("json").toString(), CompEcharts.class);
                    modules.put(m.get("id").toString(), obj);
                }
                else if(m.get("type").equals("CompGantt")){
                    CompGantt obj = mapper.readValue(m.get("json").toString(), CompGantt.class);
                    modules.put(m.get("id").toString(), obj);
                }
                else if(m.get("type").equals("CompTimeline")){
                    CompTimeline obj = mapper.readValue(m.get("json").toString(), CompTimeline.class);
                    modules.put(m.get("id").toString(), obj);
                }
                else if(m.get("type").equals("CompFlowNavigator")){
                    CompFlowNavigator obj = mapper.readValue(m.get("json").toString(), CompFlowNavigator.class);
                    modules.put(m.get("id").toString(), obj);
                }
                else if(m.get("type").equals("CompNodeSelector")){
                    CompNodeSelector obj = mapper.readValue(m.get("json").toString(), CompNodeSelector.class);
                    modules.put(m.get("id").toString(), obj);
                }
                else if(m.get("type").equals("CompTree")){
                    CompTree obj = mapper.readValue(m.get("json").toString(), CompTree.class);
                    modules.put(m.get("id").toString(), obj);
                }
                else if(m.get("type").equals("CompTimeline")){
                    CompTimeline obj = mapper.readValue(m.get("json").toString(), CompTimeline.class);
                    modules.put(m.get("id").toString(), obj);
                }
                else if(m.get("type").equals("CompSearch")){
                    CompSearch obj = mapper.readValue(m.get("json").toString(), CompSearch.class);
                    modules.put(m.get("id").toString(), obj);
                }
            }

            for(Map m : v_modules){
                Object v = modules.get(m.get("id"));
                if(v instanceof View){
                    ExecStore.getInstance().setCompExec(v);
                }
            }

        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    //将前台提交的json数据更新到内部
    public void update(Map obj){
        String id = obj.get("id").toString();
        ObjectMapper mapper = new ObjectMapper();
        try {
            String json = mapper.writeValueAsString(obj);
            db.updateEq("v_module", Lutils.genMap("json", json), Lutils.genMap("id",id));
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        loadFromDB(id);
        //Table特殊，另存边和节点的名称到v_flow_edge和v_flow_node表
        if(obj.get("id").toString().toLowerCase().startsWith("table")) {
            Table tbl = Lutils.ObjToClass(obj, Table.class);
            tbl.setDeepInfo();
            updateTableFlowNodeEdges(tbl);
        }
    }

    //将前台提交的json数据更新到内部
    public void update(Map obj, DataSource dataSource){
        String id = obj.get("id").toString();
        Object obj1 = Modules.getInstance().get(id, false);
        //保存到历史表
        if(obj1 instanceof Table){
            Table t = (Table) obj1;
            db.insertMap("v_module_his", Lutils.genMap("id", t.id, "type", "Table", "json", Lutils.ObjectToJSON(t),"abandon_time", new Date()));
        }
        ObjectMapper mapper = new ObjectMapper();
        try {
            String json = mapper.writeValueAsString(obj);
            db.updateEq("v_module", Lutils.genMap("json", json), Lutils.genMap("id",id));
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        loadFromDB(id);
        //Table特殊，另存边和节点的名称到v_flow_edge和v_flow_node表
        if(obj.get("id").toString().toLowerCase().startsWith("table")) {
            Table tbl = Lutils.ObjToClass(obj, Table.class);
            updateTableFlowNodeEdges(tbl);
            //找到相关的数据源，更新数据源的字段

            System.out.println("处理数据源深度信息...");
            Date start = new Date();
            for (Map.Entry<String, Object> entry : modules.entrySet()) {
                Object o = entry.getValue();
                if(o instanceof CompDataSource){
                    CompDataSource ds = (CompDataSource)o;
                    if(Objects.equals(ds.table_id,tbl.id))
                        ds.setFields();
                }
            }
            Date end = new Date();
            long diff = end.getTime() - start.getTime();
            System.out.println("数据源字段处理完成，用时"+diff+"ms");
        }
        //数据源的自定义类型，需要重新解析字段
        else if(obj.get("id").toString().startsWith("CompDataSource")) {
            //校验
            CompDataSource ds = (CompDataSource) Modules.getInstance().get(id, false);
            if(ds.data_type.equals("defined")){
                try {
                    insert_defined_sql(ds.id,  ds.data_sql, 879, dataSource);
                }catch(Exception e){
                    e.printStackTrace();
                }
            }
        }
    }

    public void remove(String id){
        this.modules.remove(id);
        db.deleteEq("v_module", Lutils.genMap("id", id));
    }

    public void removeViewWithComp(String id){
        Object obj = Modules.getInstance().get(id, false);
        //删除视图
        if(obj instanceof View){
            View v = (View) obj;
            if(Objects.equals(v.view_type,"indexTab")||Objects.equals(v.view_type,"modal")||Objects.equals(v.view_type,"drawer")){
                if(v.dsList!=null||v.dsList.size()>0){
                    for(CompDataSource ds: v.dsList){
                        remove(ds.id);
                    }
                }
            }
            //删除组件
            if(Objects.equals(v.view_type,"comp")){
                //删除字段编辑器表格
                if(Objects.equals(v.comp_name, "CompValueEditor")){
                    CompValueEditor edt = (CompValueEditor) Modules.getInstance().get(v.comp_id, false);
                    if(Objects.equals(edt.editor_type,"foreign-key-editor")) {
                        remove(edt.grid_id);
                    }
                }
                remove(v.comp_id);
            }
            remove(id);
        }
    }

    public void insert_defined_sql(String ds_id,String data_sql1, Object userId, DataSource dataSource) throws JSQLParserException, SQLException {
        String data_sql = data_sql1.replace("#{user_id}",userId.toString());
//      List<String> fieldAliases = Lutils.extractFieldAliasesByJSQL(data_sql);
        // DataSource（自动释放连接）
        SqlJdbcIntrospector.ColumnsAndTables r1 =
                SqlJdbcIntrospector.extractColumnsAndTables(dataSource, data_sql, 3);
        List<String> fieldAliases = r1.getColumns();
        List<String> tableNames = r1.getTables();

        CompDataSource ds = (CompDataSource) Modules.getInstance().get(ds_id,false);
        ds.data_sql = data_sql1;
        ds.data_type = "defined";
        List sys_fields = ds.fields.stream().filter(e ->
                e.field_type.equals("ds_total")
                        || e.field_type.equals("pri_sub_receiver_info")
                        || e.field_type.equals("ds_rows_length")).collect(Collectors.toList());
        List fds = fieldAliases.stream()
                .map(f->new CompDataSourceField().createDefined(f,ds_id))
                .collect(Collectors.toList());
        ds.fields.clear();
        ds.fields.addAll(fds);
        ds.fields.addAll(sys_fields);
        Modules.getInstance().save(ds_id,ds);
    }

    public void updateTableFlowNodeEdges(Table tbl){
        db.deleteEq("v_flow_node", Lutils.genMap("table_id", tbl.id));
        db.deleteEq("v_flow_edge", Lutils.genMap("table_id", tbl.id));
        //将edges和nodes的标签存入v_label
        for (FlowNode n : tbl.nodes) {
            db.insertMap("v_flow_node", Lutils.genMap("id", n.id, "label", n.label, "table_id", tbl.id, "type", n.type));
        }
        for (FlowEdge e : tbl.edges) {
            db.insertMap("v_flow_edge", Lutils.genMap("id", e.id, "label", e.label, "table_id", tbl.id, "src", e.src, "dst", e.dst));
        }
    }

    public void create(Object id, String type, Object obj){
        ObjectMapper mapper = new ObjectMapper();
        String json = "";
        try {
            json = mapper.writeValueAsString(obj);
            db.insertWhenNotExistUpdateWhenExists("v_module",
                    Lutils.genMap("id", id, "type", type, "json", json),
                    Lutils.genMap("id", id)
            );
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        loadFromDB(id);
    }

    /**
     * 将内存数据存储到数据库
     * */
    public void save(String id, Object module){
        ObjectMapper mapper = new ObjectMapper();
        String json = "";
        if(module!=null) {
            try {
                json = mapper.writeValueAsString(module);
                db.updateEq("v_module",
                        Lutils.genMap( "json", json),
                        Lutils.genMap("id", id));
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        }
        loadFromDB(id);
    }
    /**
     * 将从内存中取对象，深获取或浅获取
     * */
    public Object get(Object id, boolean deep){
        Object obj = modules.get(id);
        if(obj==null)
            return null;
        //组件
        if(obj instanceof Table){
            return obj;
        }
        else if (obj instanceof View){
            View v =  Lutils.deepCopy((View)obj);
            if(deep){
                if(v.view_type.equals("comp")){
                    v.comp = Modules.getInstance().get(v.comp_id, true);
                }
            }
            if(v.viewBtns!=null) {
                for (Exec exec : v.viewBtns) {
                    HandleExecCompatibility(exec);
                }
            }
            return v;
        }
        else if (obj instanceof CompDataSource){
            CompDataSource comp =  Lutils.deepCopy((CompDataSource)obj);
            return comp;
        }
        else if (obj instanceof CompGrid){
            CompGrid grid =  Lutils.deepCopy((CompGrid)obj);
            //处理exec.ops的insert、update、view_id改ds_id
            for(Exec exec:grid.topBtns){
                HandleExecCompatibility(exec);
            }
            for(CompGridCol c:grid.gridCols){
                HandleExecCompatibility(c.exec);
            }
            for(CompGridCol c:grid.gridCols){
                for(Exec exec:c.btns){
                    HandleExecCompatibility(exec);
                }
            }
            if(deep){
                grid.setDsFieldInfo();
                grid.getFieldColInfo();
            }
            return grid;
        }
        else if (obj instanceof CompCountAggr){
            CompCountAggr comp = Lutils.deepCopy((CompCountAggr)obj);
            if(comp.exec==null){
                comp.exec = new Exec();
                comp.exec.create();
            }
            HandleExecCompatibility(comp.exec);
            return comp;
        }
        else if (obj instanceof CompCarousel){
            CompCarousel comp =  Lutils.deepCopy((CompCarousel)obj);
            if(comp.exec==null){
                comp.exec = new Exec();
                comp.exec.create();
            }
            HandleExecCompatibility(comp.exec);
            return comp;
        }
        else if (obj instanceof CompCard){
            CompCard comp =  Lutils.deepCopy((CompCard)obj);
            if(comp.exec==null){
                comp.exec = new Exec();
                comp.exec.create();
            }
            HandleExecCompatibility(comp.exec);
            return comp;
        }
        else if (obj instanceof CompValueRender){
            CompValueRender comp =  Lutils.deepCopy((CompValueRender)obj);
//            comp.setDsFieldInfo();
            if(comp.exec==null){
                comp.exec = new Exec();
                comp.exec.create();
            }
            HandleExecCompatibility(comp.exec);
            return comp;
        }
        else if (obj instanceof CompValueEditor){
            CompValueEditor comp =  Lutils.deepCopy((CompValueEditor)obj);
//            comp.setDsFieldInfo();
            if(comp.userScope==null){
                comp.userScope = new UserScope();
            }
            if(comp.editor_type!=null&&comp.editor_type.equals("foreign-key-editor")){
                comp.compGrid = (CompGrid)Modules.getInstance().get(comp.grid_id, false);
            }
            return comp;
        }
        else if (obj instanceof CompLogTable){
            CompLogTable comp =  Lutils.deepCopy((CompLogTable)obj);
            if(deep){
                //获取字段名称
                if(comp.table_id!=null){
                   Table compLog_table = (Table)Modules.getInstance().get(comp.table_id,false);
                   comp.table = compLog_table;
                }
            }
            return comp;
        }
        else if (obj instanceof CompFlowNavigator){
            CompFlowNavigator comp =  Lutils.deepCopy((CompFlowNavigator)obj);
            return comp;
        }
        else if (obj instanceof CompNodeSelector){
            CompNodeSelector comp =  Lutils.deepCopy((CompNodeSelector)obj);
            return comp;
        }
        else if (obj instanceof CompUserSelector){
            CompUserSelector comp =  Lutils.deepCopy((CompUserSelector)obj);
            return comp;
        }
         else if (obj instanceof CompEcharts){
            CompEcharts comp =  Lutils.deepCopy((CompEcharts)obj);
            if(comp.exec==null){
                comp.exec = new Exec();
                comp.exec.create();
            }
            HandleExecCompatibility(comp.exec);
            return comp;
        }
        else if (obj instanceof CompGantt){
            CompGantt comp =  Lutils.deepCopy((CompGantt)obj);
            if(comp.exec==null){
                comp.exec = new Exec();
                comp.exec.create();
            }
            HandleExecCompatibility(comp.exec);
            return comp;
        }
        else if (obj instanceof CompTree) {
            CompTree comp =  Lutils.deepCopy((CompTree)obj);
            if(comp.exec==null){
                comp.exec = new Exec();
                comp.exec.create();
            }
            HandleExecCompatibility(comp.exec);
            return comp;
        }
        else if (obj instanceof CompTimeline) {
            CompTimeline comp =  Lutils.deepCopy((CompTimeline)obj);
            if(comp.exec==null){
                comp.exec = new Exec();
                comp.exec.create();
            }
            HandleExecCompatibility(comp.exec);
            return comp;
        }
        else if (obj instanceof CompSearch) {
            CompSearch comp =  Lutils.deepCopy((CompSearch)obj);
            if(comp.exec==null){
                comp.exec = new Exec();
                comp.exec.create();
            }
            HandleExecCompatibility(comp.exec);
            return comp;
        }
        return null;
    }

    public void HandleExecCompatibility(Exec exec){
        if(exec!=null)
        for(ExecOp op: exec.ops){
            if(Objects.equals(op.op_type,"insert")||Objects.equals(op.op_type,"update")){
                if((op.ds_id==null||!op.ds_id.startsWith("CompDataSource"))&&op.view_id!=null){
                    View v = (View)Modules.getInstance().get(op.view_id,false);
                    if(v!=null)
                        op.ds_id = v.comp_id;
                }
            }
        }
    }
    /**
     * 有组件，则用原组件，无则新建组件。
     * @param comp_id 例CompGrid123
     * */
    public void createEmptyComp(String comp_id, String comp_name){
        if (comp_name.equals("CompDataSource")) {
            CompDataSource comp = new CompDataSource();
//            comp.create("CompDataSource" + Lutils.ExtraNumberChar(comp_id));
//            Modules.getInstance().create(comp.id, "CompDataSource", comp);
        }
        else if (comp_name.equals("CompGrid")){
            CompGrid comp = new CompGrid();
            comp.create(comp_id);
            Modules.getInstance().create(comp.id, "CompGrid", comp);
        }
        else if (comp_name.equals("CompCarousel")) {
            CompCarousel comp = new CompCarousel();
            comp.create(comp_id);
            Modules.getInstance().create(comp.id, "CompCarousel", comp);
        }
        else if (comp_name.equals("CompCountAggr")) {
            CompCountAggr comp = new CompCountAggr();
            comp.create("CompCountAggr" + Lutils.ExtraNumberChar(comp_id));
            Modules.getInstance().create(comp.id, "CompCountAggr", comp);
        }
        else if (comp_name.equals("CompUserSelector")) {
            CompUserSelector comp = new CompUserSelector();
            comp.create("CompUserSelector" + Lutils.ExtraNumberChar(comp_id));
            Modules.getInstance().create(comp.id, "CompUserSelector", comp);
        }
        else if (comp_name.equals("CompCard")) {
            CompCard comp = new CompCard();
            comp.create("CompCard" + Lutils.ExtraNumberChar(comp_id));
            Modules.getInstance().create(comp.id, "CompCard", comp);
        }
        else if (comp_name.equals("CompValueRender")) {
            CompValueRender comp = new CompValueRender();
            comp.create("CompValueRender" + Lutils.ExtraNumberChar(comp_id));
            Modules.getInstance().create(comp.id, "CompValueRender", comp);
        }
        else if (comp_name.equals("CompValueEditor")) {
            CompValueEditor comp = new CompValueEditor();
            comp.create("CompValueEditor" + Lutils.ExtraNumberChar(comp_id));
            Modules.getInstance().create(comp.id, "CompValueEditor", comp);
        }
        else if (comp_name.equals("CompEcharts")) {
            CompEcharts comp = new CompEcharts();
            comp.create("CompEcharts" + Lutils.ExtraNumberChar(comp_id));
            Modules.getInstance().create(comp.id, "CompEcharts", comp);
        }
        else if (comp_name.equals("CompNodeSelector")) {
            CompNodeSelector comp = new CompNodeSelector();
            comp.create("CompNodeSelector" + Lutils.ExtraNumberChar(comp_id));
            Modules.getInstance().create(comp.id, "CompNodeSelector", comp);
        }
        else if (comp_name.equals("CompFlowNavigator")) {
            CompFlowNavigator comp = new CompFlowNavigator();
            comp.create("CompFlowNavigator" + Lutils.ExtraNumberChar(comp_id));
            Modules.getInstance().create(comp.id, "CompFlowNavigator", comp);
        }
        else if (comp_name.equals("CompNodeSelector")) {
            CompNodeSelector comp = new CompNodeSelector();
            comp.create("CompNodeSelector" + Lutils.ExtraNumberChar(comp_id));
            Modules.getInstance().create(comp.id, "CompNodeSelector", comp);
        }
        else if (comp_name.equals("CompLogTable")) {
            CompLogTable comp = new CompLogTable();
            comp.create("CompLogTable" + Lutils.ExtraNumberChar(comp_id));
            Modules.getInstance().create(comp.id, "CompLogTable", comp);
        }
        else if (comp_name.equals("CompGantt")) {
            CompGantt comp = new CompGantt();
            comp.create("CompGantt" + Lutils.ExtraNumberChar(comp_id));
            Modules.getInstance().create(comp.id, "CompGantt", comp);
        }
        else if (comp_name.equals("CompTree")) {
            CompTree comp = new CompTree();
            comp.create("CompTree" + Lutils.ExtraNumberChar(comp_id));
            Modules.getInstance().create(comp.id, "CompTree", comp);
        }
        else if (comp_name.equals("CompTimeline")) {
            CompTimeline comp = new CompTimeline();
            comp.create("CompTimeline" + Lutils.ExtraNumberChar(comp_id));
            Modules.getInstance().create(comp.id, "CompTimeline", comp);
        }
        else if (comp_name.equals("CompSearch")) {
            CompSearch comp = new CompSearch();
            comp.create("CompSearch" + Lutils.ExtraNumberChar(comp_id));
            Modules.getInstance().create(comp.id, "CompSearch", comp);
        }
    }

    public String getViewExportJson(String view_id, boolean changeId){
        List<Map> views = busiMapper.getViewTree(Lutils.genMap("view_id", view_id));
        Map<String, String> replaceMap = new HashMap<>(); //key old_id， value new_id
        //生成旧ID对应的新ID
        if(changeId) {
            for (Map view : views) {
                String newViewId = "view"+Constants.getTimeFormatId();
                replaceMap.put(view.get("id").toString(), newViewId);
            }
        }
        List<String> v_tree_table_ids = new ArrayList<>();
        List<String> v_flow_node_ids = new ArrayList<>();
        List<String> v_flow_edge_ids = new ArrayList<>();
        //结果对象
        Map<String, Object> result = new HashMap<>();
        //获取sql值
        List<Map> v_tree_view = new ArrayList<>();
        List<Map> v_module =    new ArrayList<>();
        /**
         * id改变，
         * tree的结构改变
         * comp_id的引用改变
         * table不变
         * */
        List<Object> tmpModule = new ArrayList<>();
        for(Map view:views){
            View v = (View)Modules.getInstance().get(view.get("id"), false);
            v.parent_id = view.get("parent_id").toString();
            if(v.view_type.equals("comp")){
                //获取ds组件
                Object obj = Modules.getInstance().get(v.comp_id, false);
                if(v.comp_name.equals("CompGrid")){
                    CompGrid comp = (CompGrid) obj;
                    if(comp!=null) {
                        CompDataSource ds = export_ds(v_tree_table_ids, v_module, comp.ds_id, changeId, replaceMap);
                        comp.ds_id = ds.id;
                        //保存Comp
                        if(changeId) {
                            comp.id = "CompGrid" + Constants.getTimeFormatId();
                            v.comp_id = comp.id;
                            //修改Col绑定的ValueRender的ds_id
                            for(CompGridCol rdr: comp.gridCols){
                                if(rdr.compValueRender!=null)
                                    rdr.compValueRender.ds_id = ds.id;
                            }
                        }
                    }
                }
                else if(v.comp_name.equals("CompCarousel")){
                    //获取ds组件
                    CompCarousel comp = (CompCarousel)obj;
                    if(comp!=null) {
                        CompDataSource ds = export_ds( v_tree_table_ids, v_module, comp.ds_id, changeId, replaceMap);
                        comp.ds_id = ds.id;
                        //保存Comp
                        if(changeId) {
                            comp.id = "CompCarousel" + Constants.getTimeFormatId();
                            v.comp_id = comp.id;
                        }
                    }
                }
                else if(v.comp_name.equals("CompLogTable")){
                    //获取ds组件
                    CompLogTable comp = (CompLogTable)obj;
                    if(comp!=null) {
                        //修改数据源id
                        CompDataSource ds = export_ds(v_tree_table_ids, v_module, comp.ds_id, changeId, replaceMap);
                        //同时修改绑定的ds_id
                        comp.ds_id = ds.id;
                        //保存Comp
                        if(changeId) {
                            comp.id = "CompLogTable" + Constants.getTimeFormatId();
                            v.comp_id = comp.id;
                        }
                    }
                }
                else if(v.comp_name.equals("CompValueEditor")){
                    System.out.println("CompValueEditor："+v.id);
                    //获取ds组件
                    CompValueEditor comp = (CompValueEditor)obj;
                    if(comp!=null) {
                        //处理数据源，改变id，存入v_module
                        if(changeId) {
                            comp.ds_id = replaceMap.get(comp.ds_id) == null ?comp.ds_id:replaceMap.get(comp.ds_id);
                        }
                        System.out.println("CompValueEditor：" + comp.id + "的ds_id=" + comp.ds_id);
                        if (comp.editor_type != null && comp.editor_type.equals("foreign-key-editor")) {
                            System.out.println("CompValueEditor：type="+comp.id+",type=foreign-key-editor的ds_id="+comp.ds_id);
                            comp.compGrid = (CompGrid) Modules.getInstance().get(comp.grid_id, false);
                            CompDataSource ds1 = export_ds(v_tree_table_ids, v_module, comp.compGrid.ds_id, changeId, replaceMap);
                            comp.compGrid.ds_id = ds1.id;
                            if(changeId) {
                                comp.compGrid.id = "CompGrid" + Constants.getTimeFormatId();
                                comp.grid_id = comp.compGrid.id;
                                //处理表格列的渲染器数据源id
                                for(CompGridCol gc: comp.compGrid.gridCols){
                                    if(gc.compValueRender!=null){
                                        gc.compValueRender.ds_id = ds1.id;
                                    }
                                }
                            }
                            v_module.add(Lutils.genMap("id", comp.compGrid.id, "type", "CompGrid", "json", Lutils.ObjectToJSON(comp.compGrid)));
                        }
                        //保存Comp
                        if(changeId) {
                            comp.id = "CompValueEditor" + Constants.getTimeFormatId();
                            v.comp_id = comp.id;
                        }
                    }
                }
                else if(v.comp_name.equals("CompValueRender")){
                    //获取ds组件
                    CompValueRender comp = (CompValueRender)obj;
                    if(comp!=null) {
                        //保存Comp
                        if(changeId) {
                            comp.id = "CompValueRender" + Constants.getTimeFormatId();
                            v.comp_id = comp.id;
                        }
                    }
                }
                else if(v.comp_name.equals("CompCard")){
                    //获取ds组件
                    CompCard comp = (CompCard)obj;
                    if(comp!=null) {
                        //保存Comp
                        if(changeId) {
                            comp.id = "CompCard" + Constants.getTimeFormatId();
                            v.comp_id = comp.id;
                        }
                    }
                }
                else if(v.comp_name.equals("CompCountAggr")){
                    //获取ds组件
                    CompCountAggr comp = (CompCountAggr)obj;
                    if(comp!=null) {
                        //保存Comp
                        if(changeId) {
                            comp.id = "CompCountAggr" + Constants.getTimeFormatId();
                            v.comp_id = comp.id;
                        }
                    }
                }
                else if(v.comp_name.equals("CompDataSource")){
                    //获取ds组件
                    CompDataSource comp = (CompDataSource)obj;
                    if(comp!=null) {
                        //保存Comp
                        if(changeId) {
                            String nid = "CompDataSource" + Constants.getTimeFormatId();
                            replaceMap.put(comp.id, nid);
                            comp.id = nid;
                            v.comp_id = comp.id;
                        }
                    }
                }
                else if(v.comp_name.equals("CompEcharts")){
                    //获取ds组件
                    CompEcharts comp = (CompEcharts)obj;
                    if(comp!=null) {
                        //保存Comp
                        if(changeId) {
                            comp.id = "CompEcharts" + Constants.getTimeFormatId();
                            v.comp_id = comp.id;
                        }
                    }
                }
                else if(v.comp_name.equals("CompGantt")){
                    //获取ds组件
                    CompGantt comp = (CompGantt)obj;
                    if(comp!=null) {
                        //保存Comp
                        if(changeId) {
                            comp.id = "CompGantt" + Constants.getTimeFormatId();
                            v.comp_id = comp.id;
                        }
                    }
                }
                else if(v.comp_name.equals("CompFlowNavigator")){
                    //获取ds组件
                    CompFlowNavigator comp = (CompFlowNavigator)obj;
                    if(comp!=null) {
                        //保存Comp
                        if(changeId) {
                            comp.id = "CompFlowNavigator" + Constants.getTimeFormatId();
                            v.comp_id = comp.id;
                        }
                    }
                }
                else if(v.comp_name.equals("CompNodeSelector")){
                    //获取ds组件
                    CompNodeSelector comp = (CompNodeSelector)obj;
                    if(comp!=null) {
                        //保存Comp
                        if(changeId) {
                            comp.id = "CompNodeSelector" + Constants.getTimeFormatId();
                            v.comp_id = comp.id;
                        }
                    }
                }
                else if(v.comp_name.equals("CompTimeline")){
                    //获取ds组件
                    CompTimeline comp = (CompTimeline)obj;
                    if(comp!=null) {
                        //保存Comp
                        if(changeId) {
                            comp.id = "CompTimeline" + Constants.getTimeFormatId();
                            v.comp_id = comp.id;
                        }
                    }
                }
                else if(v.comp_name.equals("CompUserSelector")){
                    //获取ds组件
                    CompUserSelector comp = (CompUserSelector)obj;
                    if(comp!=null) {
                        //保存Comp
                        if(changeId) {
                            comp.id = "CompUserSelector" + Constants.getTimeFormatId();
                            v.comp_id = comp.id;
                        }
                    }
                }
                else if(v.comp_name.equals("CompSearch")){
                    //获取ds组件
                    CompSearch comp = (CompSearch)obj;
                    if(comp!=null) {
                        //保存Comp
                        if(changeId) {
                            comp.id = "CompUSearch" + Constants.getTimeFormatId();
                            v.comp_id = comp.id;
                        }
                    }
                }
                tmpModule.add(obj);
            }
            if(changeId) {
                //保存新旧ID
                v.id = replaceMap.get(v.id);
                v.parent_id = replaceMap.get(v.parent_id);
            }
            view.put("id", v.id);
            view.put("parent_id", v.parent_id);
            view.put("json", null);
            v.children = new ArrayList<>();
            v_tree_view.add(view);
            v_module.add(Lutils.genMap("id", v.id, "type", "View", "json", Lutils.ObjectToJSON(v)));
        }
        //处理ds_id的引用
        for(Object obj: tmpModule){
            if(obj instanceof CompCard){
                CompCard comp = (CompCard)obj;
                comp.ds_id = replaceMap.get(comp.ds_id)!=null?replaceMap.get(comp.ds_id):comp.ds_id;
                v_module.add(Lutils.genMap("id", comp.id,"type", "CompCard", "json", Lutils.ObjectToJSON(comp)));
            }
            else if(obj instanceof CompCarousel){
                CompCarousel comp = (CompCarousel)obj;
                v_module.add(Lutils.genMap("id", comp.id,"type", "CompCarousel", "json", Lutils.ObjectToJSON(comp)));
            }
            else if(obj instanceof CompDataSource){
                CompDataSource comp = (CompDataSource)obj;
                v_module.add(Lutils.genMap("id", comp.id,"type", "CompDataSource","json", Lutils.ObjectToJSON(comp)));
            }
            else if(obj instanceof CompCountAggr){
                CompCountAggr comp = (CompCountAggr)obj;
                comp.ds_id = replaceMap.get(comp.ds_id)!=null?replaceMap.get(comp.ds_id):comp.ds_id;
                v_module.add(Lutils.genMap("id", comp.id,"type", "CompCountAggr", "json", Lutils.ObjectToJSON(comp)));
            }
            else if(obj instanceof CompEcharts){
                CompEcharts comp = (CompEcharts)obj;
                comp.ds_id = replaceMap.get(comp.ds_id)!=null?replaceMap.get(comp.ds_id):comp.ds_id;
                v_module.add(Lutils.genMap("id", comp.id,"type", "CompEcharts","json", Lutils.ObjectToJSON(comp)));
            }
            else if(obj instanceof CompGantt){
                CompGantt comp = (CompGantt)obj;
                comp.ds_id = replaceMap.get(comp.ds_id)!=null?replaceMap.get(comp.ds_id):comp.ds_id;
                v_module.add(Lutils.genMap("id", comp.id,"type", "CompGantt","json", Lutils.ObjectToJSON(comp)));
            }
            else if(obj instanceof CompFlowNavigator){
                CompFlowNavigator comp = (CompFlowNavigator)obj;
                v_module.add(Lutils.genMap("id", comp.id,"type", "CompFlowNavigator", "json", Lutils.ObjectToJSON(comp)));
            }
            else if(obj instanceof CompNodeSelector){
                CompNodeSelector comp = (CompNodeSelector)obj;
                v_module.add(Lutils.genMap("id", comp.id,"type", "CompNodeSelector", "json", Lutils.ObjectToJSON(comp)));
            }
            else if(obj instanceof CompGrid){
                CompGrid comp = (CompGrid)obj;
                v_module.add(Lutils.genMap("id", comp.id,"type", "CompGrid", "json", Lutils.ObjectToJSON(comp)));
            }
            else if(obj instanceof CompLogTable){
                CompLogTable comp = (CompLogTable)obj;
                v_module.add(Lutils.genMap("id", comp.id,"type", "CompLogTable", "json", Lutils.ObjectToJSON(comp)));
            }
            else if(obj instanceof CompTimeline){
                CompTimeline comp = (CompTimeline)obj;
                v_module.add(Lutils.genMap("id", comp.id,"type", "CompTimeline", "json", Lutils.ObjectToJSON(comp)));
            }
            else if(obj instanceof CompUserSelector){
                CompUserSelector comp = (CompUserSelector)obj;
                v_module.add(Lutils.genMap("id", comp.id,"type", "CompUserSelector", "json", Lutils.ObjectToJSON(comp)));
            }
            else if(obj instanceof CompValueEditor){
                CompValueEditor comp = (CompValueEditor)obj;
                comp.ds_id = replaceMap.get(comp.ds_id)!=null?replaceMap.get(comp.ds_id):comp.ds_id;
                v_module.add(Lutils.genMap("id", comp.id,"type", "CompValueEditor", "json", Lutils.ObjectToJSON(comp)));
            }
            else if(obj instanceof CompValueRender){
                CompValueRender comp = (CompValueRender)obj;
                comp.ds_id = replaceMap.get(comp.ds_id)!=null?replaceMap.get(comp.ds_id):comp.ds_id;
                v_module.add(Lutils.genMap("id", comp.id, "type", "CompValueRender", "json", Lutils.ObjectToJSON(comp)));
            }
            else if(obj instanceof CompSearch){
                CompValueRender comp = (CompValueRender)obj;
                comp.ds_id = replaceMap.get(comp.ds_id)!=null?replaceMap.get(comp.ds_id):comp.ds_id;
                v_module.add(Lutils.genMap("id", comp.id, "type", "CompSearch", "json", Lutils.ObjectToJSON(comp)));
            }
        }
        //处理flow_node、floe_edge;
        for(String tid:v_tree_table_ids){
            Table tbl = (Table)Modules.getInstance().get(tid, false);
            if(tbl!=null) {
                for (FlowNode n : tbl.nodes) {
                    v_flow_node_ids.add(n.id);
                }
                for (FlowEdge n : tbl.edges) {
                    v_flow_edge_ids.add(n.id);
                }
            }
        }
//        if(v_tree_view_ids.size()>0){
//            v_tree_view = db.selectIn("v_tree_view","id", v_tree_view_ids);
//        }
//        if(v_module_ids.size()>0){
//            v_module = db.selectIn("v_module","id", v_module_ids);
//        }// Comp+CompDs
        List<Map> v_tree_table = new ArrayList<>();
        List<Map> v_flow_node = new ArrayList<>();
        List<Map> v_flow_edge = new ArrayList<>();
        if(v_tree_table_ids.size()>0){
            v_tree_table = db.selectIn("v_tree_table","id", v_tree_table_ids);
        }
        if(v_flow_node_ids.size()>0){
            v_flow_node = db.selectIn("v_flow_node","id", v_flow_node_ids);
        }
        if(v_flow_edge_ids.size()>0){
            v_flow_edge = db.selectIn("v_flow_edge","id", v_flow_edge_ids);
        }

        result.put("root_view_id", view_id);
        if(changeId) {
            result.put("root_view_id", replaceMap.get(view_id));
        }
        result.put("v_tree_view", v_tree_view);
        result.put("v_tree_table", v_tree_table);
        result.put("v_module", v_module);
        result.put("v_flow_node", v_flow_node);
        result.put("v_flow_edge", v_flow_edge);
        return Lutils.ObjectToJSON(result);
    }

    public CompDataSource export_ds(List v_tree_table_ids,List v_module,String ds_id, boolean changeId, Map replaceMap){
        CompDataSource ds = (CompDataSource) Modules.getInstance().get(ds_id, false);
        if(ds!=null) {
            if(ds.table_id!=null) {
                Table tbl = (Table) Modules.getInstance().get(ds.table_id, false);
                if(tbl!=null){
                    v_tree_table_ids.add(ds.table_id);
                    v_module.add(Lutils.genMap("id", tbl.id, "type", "Table", "json", Lutils.ObjectToJSON(tbl)));
                }
            }
            if(changeId) {
                String nid = "CompDataSource" + Constants.getTimeFormatId();
                replaceMap.put(ds.id, nid);
                ds.id = nid;
            }
        }
        else{
            ds = new CompDataSource();
            String nid = "CompDataSource" + Constants.getTimeFormatId();
            ds.create(nid);
        }
        v_module.add(Lutils.genMap("id", ds.id, "type", "CompDataSource", "json", Lutils.ObjectToJSON(ds)));
        return ds;
    }

    //导入视图
    public void importViewsByJson(Map result, String import_to_view_id){
        //导入view
        String root_view_id = (String) result.get("root_view_id");
        List<Map> v_tree_view = (List)result.get("v_tree_view");
        List<Map> v_tree_table = (List)result.get("v_tree_table");
        List<Map> v_module = (List)result.get("v_module");
        List<Map> v_flow_node = (List)result.get("v_flow_node");
        List<Map> v_flow_edge = (List)result.get("v_flow_edge");
        for(Map table:v_tree_table){
            db.insertWhenNotExistUpdateWhenExists("v_tree_table", table, Lutils.genMap("id", table.get("id")));
            //如果table的父id不存在
        }
        for(Map module:v_module){
            db.insertWhenNotExistUpdateWhenExists("v_module", module, Lutils.genMap("id", module.get("id")));
            Modules.getInstance().loadFromDB(module.get("id"));
        }
        for(Map view:v_tree_view){
            db.insertWhenNotExistUpdateWhenExists("v_tree_view", view, Lutils.genMap("id", view.get("id")));
            Modules.getInstance().loadFromDB(view.get("id"));
        }
        for(Map flowNode:v_flow_node){
            db.insertWhenNotExistUpdateWhenExists("v_flow_node", flowNode, Lutils.genMap("id", flowNode.get("id")));
        }
        for(Map flowEdge:v_flow_edge){
            db.insertWhenNotExistUpdateWhenExists("v_flow_edge", flowEdge, Lutils.genMap("id", flowEdge.get("id")));
        }
        //设置view的父id为
        db.updateEq("v_tree_view", Lutils.genMap("parent_id", import_to_view_id, "name", "新导入"+Constants.getTimeFormatId()), Lutils.genMap("id", root_view_id));

        for(Map table:v_tree_table){
            Modules.getInstance().loadFromDB(table.get("id").toString());
            Table tbl = (Table)Modules.getInstance().get(table.get("id").toString(), false);
            //如果表不存在，则创建
            Map exists = db.selectOne("information_schema.tables", Lutils.genMap("table_name", tbl.table_name));
            if(exists == null){
                createTable(tbl.table_name);
                for(TableCol tc:tbl.cols){
                    createTableCol(tbl.table_name, tc.field, get_db_type(tc.data_type,tc.varchar_size, tc.numeric_precision));
                }
            }
            else{
                for(TableCol tc:tbl.cols){
                    Map existCols = db.selectOne("information_schema.columns",
                            Lutils.genMap("table_name", tbl.table_name, "column_name", tc.field));
                    if(existCols == null) {
                        createTableCol(tbl.table_name, tc.field, get_db_type(tc.data_type, tc.varchar_size, tc.numeric_precision));
                    }
                }
            }
        }
    }

    public void createTable(String table_name){
        String same_sql="create_user_ int4,\n" +
                "create_time_ timestamp,\n" +
                "poster_ int4,\n" +
                "posted_time_ timestamp,\n" +
                "prev_node_ varchar(100),\n" +
                "edge_ varchar(100),\n" +
                "node_ varchar(100),\n" +
                "receiver_ int4,\n" +
                "finished_time_ timestamp,\n" +
                "update_user_ int4,\n" +
                "update_time_ timestamp,\n" +
                "pri_tbl_ varchar(100),\n" +
                "pri_tbl_node_ varchar(100),\n" +
                "pri_tbl_node_finished_ int2,\n" +
                "pri_tbl_node_enter_times_ int2,\n" +
                "time_if_delete_ timestamp\n";
        db.executeSql("create table "+ table_name+" (\n" +
                "id_ serial primary key,\n" +
                same_sql +
                ")");
        //存入最新的流程执行数据
        db.executeSql("create table "+ table_name+"_flow (\n" +
                "id_ serial primary key,\n" +
                "row_id_ integer,\n" +
                same_sql +
                ")");
        db.executeSql("create table "+ table_name+"_log (\n" +
                "id_ serial primary key,\n" +
                "log_time_ timestamp default CURRENT_TIMESTAMP,\n" +
                "action_id_ int4,\n" +  //新增
                "action_type_ varchar(200),\n" +
                "exec_id_ varchar(100),\n" +  //新增
                "exec_op_id_ varchar(100),\n" +  //新增
                "op_view_id varchar(100),\n" +  //新增
                "row_id_ int4,\n" +
                same_sql +
                ")");
    }

    public void createTableCol(String table_name, String col_name, String columnType){
        db.addTableCol( table_name,  col_name, columnType);
        db.addTableCol( table_name+"_log",  col_name, columnType);
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

    public void convertCompFieldV20260102(){
        for(Map.Entry<String, Object> entry: modules.entrySet()){
            convertCompFieldV2(entry.getValue());
        }
    }

    public void convertDSFieldV20260102(){
        for(Map.Entry<String, Object> entry: modules.entrySet()){
            convertCompDataSourceV2(entry.getValue());
        }
    }

    //将id转换为field
    public static String convertId2Field(String ds_id,String ds_field_id){
        CompDataSource ds = (CompDataSource) Modules.getInstance().get(ds_id, false);
        if(ds==null || ds.getField(ds_field_id)==null)
            return null;
        String field = ds_field_id;
        if(ds_field_id.matches("\\d+")) {
            //找到CompDataSourceField
            CompDataSourceField f = ds.getField(ds_field_id);
            field = f.field;
        }
        return field;
    }

    //修改ds_field_id为field
    public static void convertCompFieldV2(Object obj){
        if (obj instanceof CompGrid){
            CompGrid grid =  (CompGrid)obj;
            //处理exec.ops的insert、update、view_id改ds_id
            for(CompGridCol col:grid.gridCols){
                if(col.compValueRender !=null){
                    col.compValueRender.ds_field_id = convertId2Field(col.compValueRender.ds_id, col.compValueRender.ds_field_id);
                    col.compValueRender.lng_ds_field_id = convertId2Field(col.compValueRender.ds_id, col.compValueRender.lng_ds_field_id);
                    col.compValueRender.lat_ds_field_id = convertId2Field(col.compValueRender.ds_id, col.compValueRender.lat_ds_field_id);
                }
                if(col.compValueEditor !=null){
                    col.compValueEditor.ds_field_id = convertId2Field(col.compValueEditor.ds_id, col.compValueEditor.ds_field_id);
                    col.compValueEditor.lat_ds_field_id = convertId2Field(col.compValueEditor.ds_id, col.compValueEditor.lat_ds_field_id);
                    col.compValueEditor.lng_ds_field_id = convertId2Field(col.compValueEditor.ds_id, col.compValueEditor.lng_ds_field_id);
                    col.compValueEditor.pdf_ds_field_id = convertId2Field(col.compValueEditor.ds_id, col.compValueEditor.pdf_ds_field_id);
                    col.compValueEditor.sign_ds_field_id = convertId2Field(col.compValueEditor.ds_id, col.compValueEditor.sign_ds_field_id);
                }
            }
        }
        else if (obj instanceof CompCountAggr){
            CompCountAggr cmp = (CompCountAggr)obj;
            cmp.label_ds_field_id = convertId2Field(cmp.ds_id, cmp.label_ds_field_id);
            cmp.total_ds_field_id = convertId2Field(cmp.ds_id, cmp.total_ds_field_id);
            cmp.unit_ds_field_id = convertId2Field(cmp.ds_id, cmp.unit_ds_field_id);
            cmp.value_ds_field_id = convertId2Field(cmp.ds_id, cmp.value_ds_field_id);
        }
        else if (obj instanceof CompCarousel){
            CompCarousel cmp =  (CompCarousel)obj;
            cmp.img_ds_field_id = convertId2Field(cmp.ds_id, cmp.img_ds_field_id);
            cmp.title_ds_field_id = convertId2Field(cmp.ds_id, cmp.title_ds_field_id);
        }
        else if (obj instanceof CompCard){
            CompCard cmp =  (CompCard)obj;
            cmp.value_ds_field_id = convertId2Field(cmp.ds_id, cmp.value_ds_field_id);
        }
        else if (obj instanceof CompValueRender){
            CompValueRender cmp =  (CompValueRender)obj;
            cmp.ds_field_id = convertId2Field(cmp.ds_id, cmp.ds_field_id);
            cmp.lng_ds_field_id = convertId2Field(cmp.ds_id, cmp.lng_ds_field_id);
            cmp.lat_ds_field_id = convertId2Field(cmp.ds_id, cmp.lat_ds_field_id);
        }
        else if (obj instanceof CompValueEditor){
            CompValueEditor cmp =  (CompValueEditor)obj;
            cmp.pdf_ds_field_id = convertId2Field(cmp.ds_id, cmp.pdf_ds_field_id);
            cmp.sign_ds_field_id = convertId2Field(cmp.ds_id, cmp.sign_ds_field_id);
            cmp.lat_ds_field_id = convertId2Field(cmp.ds_id, cmp.lat_ds_field_id);
            cmp.lng_ds_field_id = convertId2Field(cmp.ds_id, cmp.lng_ds_field_id);
            cmp.ds_field_id = convertId2Field(cmp.ds_id, cmp.ds_field_id);
            if(Objects.equals(cmp.editor_type,"foreign-key-editor")){
                CompGrid grid = cmp.compGrid;
                if(grid!=null) {
                    //处理exec.ops的insert、update、view_id改ds_id
                    for (CompGridCol col : grid.gridCols) {
                        if (col.compValueRender != null) {
                            col.compValueRender.ds_field_id = convertId2Field(col.compValueRender.ds_id, col.compValueRender.ds_field_id);
                            col.compValueRender.lng_ds_field_id = convertId2Field(col.compValueRender.ds_id, col.compValueRender.lng_ds_field_id);
                            col.compValueRender.lat_ds_field_id = convertId2Field(col.compValueRender.ds_id, col.compValueRender.lat_ds_field_id);
                        }
                        if (col.compValueEditor != null) {
                            col.compValueEditor.ds_field_id = convertId2Field(col.compValueEditor.ds_id, col.compValueEditor.ds_field_id);
                            col.compValueEditor.lat_ds_field_id = convertId2Field(col.compValueEditor.ds_id, col.compValueEditor.lat_ds_field_id);
                            col.compValueEditor.lng_ds_field_id = convertId2Field(col.compValueEditor.ds_id, col.compValueEditor.lng_ds_field_id);
                            col.compValueEditor.pdf_ds_field_id = convertId2Field(col.compValueEditor.ds_id, col.compValueEditor.pdf_ds_field_id);
                            col.compValueEditor.sign_ds_field_id = convertId2Field(col.compValueEditor.ds_id, col.compValueEditor.sign_ds_field_id);
                        }
                    }
                }
            }
        }
        else if (obj instanceof CompLogTable){
            CompLogTable cmp =  (CompLogTable)obj;
        }
        else if (obj instanceof CompFlowNavigator){
            CompFlowNavigator cmp =  (CompFlowNavigator)obj;
        }
        else if (obj instanceof CompNodeSelector){
            CompNodeSelector cmp =  (CompNodeSelector)obj;
        }
        else if (obj instanceof CompUserSelector){
            CompUserSelector cmp = (CompUserSelector)obj;
        }
        else if (obj instanceof CompEcharts){
            CompEcharts cmp =  (CompEcharts)obj;
            cmp.y_ds_field_id = convertId2Field(cmp.ds_id, cmp.y_ds_field_id);
            cmp.x_ds_field_id = convertId2Field(cmp.ds_id, cmp.x_ds_field_id);
            cmp.title_ds_field_id = convertId2Field(cmp.ds_id, cmp.title_ds_field_id);
            cmp.series_ds_field_id = convertId2Field(cmp.ds_id, cmp.series_ds_field_id);
        }
        else if (obj instanceof CompGantt){
            CompGantt cmp =  (CompGantt)obj;
            cmp.duration_days = convertId2Field(cmp.ds_id, cmp.duration_days);
            cmp.end_time = convertId2Field(cmp.ds_id, cmp.end_time);
            cmp.start_time = convertId2Field(cmp.ds_id, cmp.start_time);
            cmp.progress = convertId2Field(cmp.ds_id, cmp.progress);
            cmp.group = convertId2Field(cmp.ds_id, cmp.group);
            cmp.status = convertId2Field(cmp.ds_id, cmp.status);
            cmp.parent_id = convertId2Field(cmp.ds_id, cmp.parent_id);
            cmp.priority = convertId2Field(cmp.ds_id, cmp.priority);
            cmp.notes = convertId2Field(cmp.ds_id, cmp.notes);
            cmp.is_milestone = convertId2Field(cmp.ds_id, cmp.is_milestone);
        }
        else if (obj instanceof CompTree) {
            CompTree cmp =  (CompTree)obj;
            cmp.label_ds_field_id = convertId2Field(cmp.ds_id, cmp.label_ds_field_id);
            cmp.parent_ds_field_id = convertId2Field(cmp.ds_id, cmp.parent_ds_field_id);
            cmp.search_ds_field_id = convertId2Field(cmp.search_ds_id, cmp.search_ds_field_id);
        }
        else if (obj instanceof CompTimeline) {
            CompTimeline cmp =  (CompTimeline)obj;
        }
        else if (obj instanceof CompSearch) {
            CompSearch cmp =  (CompSearch)obj;
        }
    }

    public static void convertCompDataSourceV2(Object obj){
        if (obj instanceof CompDataSource) {
            CompDataSource ds = (CompDataSource)obj;
            for(CompDataSourceField f: ds.fields){
                f.id = f.field;
            }
            ds.fields = ds.fields.stream()
                    .filter(f->(
                            !Objects.equals(f.field,"ds_total")
                                    &&!Objects.equals(f.field,"ds_rows_length")
                                    &&!Objects.equals(f.field,"pri_sub_receiver_info")
                    ))
                    .collect(Collectors.toList());
            ds.data_sql = CompUtils.gen_ds_sql(ds);
        }
    }

}
