package tcdx.uap.controller;

import cn.hutool.core.collection.CollUtil;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.googlecode.aviator.AviatorEvaluator;
import com.googlecode.aviator.Expression;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import tcdx.uap.common.entity.AjaxResult;
import tcdx.uap.common.entity.page.TableDataInfo;
import tcdx.uap.common.utils.Lutils;
import tcdx.uap.handler.CodeHandlerFactory;
import tcdx.uap.handler.CodeHandlerScanner;
import tcdx.uap.mapper.BaseDBMapper;
import tcdx.uap.mapper.ServiceConfigMapper;
import tcdx.uap.mapper.BusinessMapper;
import tcdx.uap.mapper.SystemMapper;
import tcdx.uap.service.*;
import tcdx.uap.service.entities.*;
import tcdx.uap.service.store.*;
import tcdx.uap.service.vo.TableColInfoResp;

import javax.servlet.http.HttpSession;
import java.util.*;
import java.util.stream.Collectors;

import static tcdx.uap.common.utils.PageUtils.startPage;

/**
 * 通用请求处理
 *
 * @author ruoyi
 */
@Controller
@RequestMapping("/uap/service")
public class BusinessController extends BaseController {
    private static final Logger log = LoggerFactory.getLogger(BusinessController.class);

    @Autowired
    private SystemService systemService;

    @Autowired
    private CodeHandlerFactory codeHandlerFactory;

    @Autowired
    private CodeHandlerScanner codeHandlerScanner;

    //视图缓存
    public static boolean groupViewCacheEnable = true;
    public static Map<String,List> groupViewCache = new HashMap();
    @Autowired
    private SystemMapper systemMapper;

    @Autowired
    private ServiceConfigMapper serviceConfigMapper;

    @Autowired
    private BusinessService busi;

    @Autowired
    private ServiceConfigService serviceConfigService;

    @Autowired
    private BusinessMapper businessMapper;

    @Autowired
    BaseDBMapper baseDBMapper;

    @Autowired
    private BaseDBService db;

    @Autowired
    private GenSqlService genSqlService;
    @Autowired
    private HttpSession httpSession;

    /**
     * 前端可视化视图获取
     * */
    //@RequiresPermissions("system:flowable:get_flow_mapper")
    @PostMapping("/get_views")
    @ResponseBody
    public AjaxResult get_views(@RequestBody Map<String, Object> map, HttpSession session){
        //从session中取当前登录人员信息
        String view_id = (String) map.get("view_id");
        //获取ValueRender组件属性
        ExecContext execContext = new ExecContext();
        List<Map> submitSession = map.get("session")!=null?(List<Map>)map.get("session"):new ArrayList<>();
        // 重新拿一遍node
        refreshNode(submitSession);
        execContext.PutSubmitSessionIntoContextList(submitSession);
        UserAction userAction = new UserAction();
        userAction.setUserInfo(session);
        userAction.action_time = new Date();
        Date start = new Date();
        try {
            List<Map> ll = businessMapper.getViewTree(Lutils.genMap("view_id", view_id));
            List<View> views = new ArrayList<>();
            //获取所有视图
            for (Map node : ll) {
                View v = (View) Modules.getInstance().get(node.get("id"), false);
                if(v!=null) {
                    v.parent_id = node.get("parent_id").toString();
                    v.children.clear();
                    //获取
                    if (v.view_type.equals("comp")) {
                        Object comp = Modules.getInstance().get(v.comp_id, true);
                        v.comp = comp;
                    }
                    views.add(v);
                }
            }
            long cost = new Date().getTime() - start.getTime();
            System.out.println("所有视图获取完毕，耗时"+cost+"ms");
            start = new Date();
            //提取根节点indexTab、modal、drawer
            List<View> roots = views.stream().filter(v->Objects.equals(v.view_type,"indexTab")||Objects.equals(v.view_type,"modal")||Objects.equals(v.view_type,"drawer")).collect(Collectors.toList());
            if(roots.size()>0) {
                View root = roots.get(0);
                //添加的数据源为全字段的，在传递到前台前，需要清楚一下不必要的字段
                boolean changed = busi.PutChildrenDSToRootDsList(root, views);
                //将关联数据源组件的id，绑定到数据源的字段上。
                busi.SeEditorViewIdToDsField(root.dsList, views);
            }
            cost = new Date().getTime() - start.getTime();
            System.out.println("根视图的数据源处理完毕，耗时"+cost+"ms");
            start = new Date();
            //删除老版本的数据源
            views = views.stream().filter(v->!(Objects.equals(v.view_type,"comp")&&Objects.equals(v.comp_name,"CompDataSource"))).collect(Collectors.toList());
            // 获取用户角色和岗位范围内的视图与按钮
            Map resultMap = busi.CreatePowerViewTreeWithInitRows(views, execContext, userAction);
            cost = new Date().getTime() - start.getTime();
            System.out.println("视图组建的数据处理完毕，耗时"+cost+"ms");
            start = new Date();
            // 最后过滤一下环节显示
            List<String> session_nodes = new ArrayList<>();
            if (CollUtil.isNotEmpty(submitSession)) {
                // 过滤出会话里的当前节点
                session_nodes = submitSession.stream()
                        .filter(o -> o.containsKey("node_"))
                        .map(o -> (String)o.get("node_"))
                        .collect(Collectors.toList());
            }
            List<View> roots1 = (List<View>) resultMap.get("roots");
            for (View view : roots1) {
                filterViewInPlace(view, session_nodes);
            }
            resultMap.put("roots", roots1);
            cost = new Date().getTime() - start.getTime();
            System.out.println("get_views处理完毕，耗时"+cost+"ms");
            // 找到表名
            return AjaxResult.success("success", resultMap);
        }catch(Exception e){
            e.printStackTrace();
            return AjaxResult.success("failed");
        }
    }

    private View filterViewInPlace(View view, List<String> sessionNodes) {
        if (view == null) {
            return null;
        }

        // 判断当前节点是否应该显示
        if (shouldSkipView(view, sessionNodes)) {
            return null;
        }

        // 过滤子节点
        if (view.children != null) {
            List<View> filteredChildren = new ArrayList<>();
            for (View child : view.children) {
                View filteredChild = filterViewInPlace(child, sessionNodes);
                if (filteredChild != null) {
                    filteredChildren.add(filteredChild);
                }
            }
            view.children = filteredChildren;
        }

        return view;
    }

    private void refreshNode(List<Map> submitSession) {
        for (Map node : submitSession) {
            try {
                String tableId__ = node.get("__table_id__").toString();
                Map<String, Object> params = new HashMap<>();
                params.put("tn", "z_" + tableId__);
                params.put("equalMap", Lutils.genMap("id_", node.get("id_")));
                List<Map> maps = baseDBMapper.selectEq(params);
                if (CollUtil.isNotEmpty(maps)) {
                    node.put("node_", maps.get(0).get("node_"));
                }
            } catch (Exception e) {
                log.error("刷新node失败", e);
            }
        }
    }

    /**
     * 判断View是否应该跳过（不显示）
     */
    private boolean shouldSkipView(View view, List<String> sessionNodes) {
        // 如果show_in_session_nodes为空，默认显示
        if (view.show_in_session_nodes == null || view.show_in_session_nodes.trim().isEmpty()) {
            return false;
        }

        // 解析配置的会话节点
        List<String> showInSessionNodes = JSON.parseArray(view.show_in_session_nodes, String.class);

        // 判断是否有交集
        return showInSessionNodes.stream()
                .noneMatch(sessionNodes::contains); // 没有任何交集，则跳过
    }

    private Map get_comp_log_cfg(String id) {
        return busi.get_comp_log_cfg(id);
    }

    @PostMapping("/get_comp_log_cfg")
    @ResponseBody
    public Map get_comp_log_cfg(@RequestBody Map<String, Object> map) {
        String obj_id = map.get("obj_id").toString();
        return busi.get_comp_log_cfg(obj_id);
    }

    @PostMapping("/rollback_node")
    @ResponseBody
    public AjaxResult rollback_node(@RequestBody Map<String, Object> map, HttpSession session) {
        String target_node = (String) map.get("node_");
        Integer row_id = (Integer) map.get("row_id");
        String table_id = (String) map.get("table_id");
        String tableName = "z_" + table_id;
        String sql = "update " + tableName + "\n" +
                "set node_ = '" + target_node + "',\n" +
                "\t\tedge_ = (\n" +
                "\t\t\t\tSELECT e.id FROM v_flow_edge e\n" +
                "        WHERE e.dst = '" + target_node + "'\n" +
                "\t\t),\n" +
                "\t\tprev_node_ = (\n" +
                "\t\t\t\tSELECT e.src FROM v_flow_edge e\n" +
                "        WHERE e.dst = '" + target_node + "'\n" +
                "\t\t)\n" +
                "where id_ = " + row_id;
        baseDBMapper.executeSql(sql);
        // 记录回滚操作
        String logSql = "insert into " + tableName + "_log\n" +
                "(action_type_, row_id_, create_user_, node_) VALUES ('rollback', " + row_id + ", 879, '" + target_node + "')";
        baseDBMapper.executeSql(logSql);
        return AjaxResult.success("success");
    }

    @PostMapping("/reload_timeline")
    @ResponseBody
    public AjaxResult reload_timeline(@RequestBody Map<String, Object> map, HttpSession session) {
        return AjaxResult.success("success", busi.reloadCompTimeline(map));
    }

    @PostMapping("/get_comp_log_data")
    @ResponseBody
    public Map get_comp_log_data(@RequestBody Map<String, Object> map) {
        List<Map> subSessionList = map.get("session")==null?new ArrayList<>():(List) map.get("session");
        Map config = map.get("config")==null?new HashMap<>():(Map) map.get("config");
        Map this_table_cfg = config.get("this_table_cfg")==null?new HashMap():(Map) config.get("this_table_cfg");
        Map huiqian_table_cfg = config.get("huiqian_table_cfg")==null?new HashMap():(Map) config.get("huiqian_table_cfg");
        List<Map> r1 = busi.get_table_log_data(subSessionList,this_table_cfg);
        return Lutils.genMap("this_table",r1);
    }

    @PostMapping("/get_comp_log_detail")
    @ResponseBody
    public Map get_comp_log_detail(@RequestBody Map<String, Object> map) {
        Map activity = map.get("activity")==null?new HashMap<>():(Map) map.get("activity");
        List<Map> subSessionList = map.get("session")==null?new ArrayList<>():(List) map.get("session");
        Map config = map.get("config")==null?new HashMap<>():(Map) map.get("config");
        Map this_table_cfg = config.get("this_table_cfg")==null?new HashMap():(Map) config.get("this_table_cfg");
        Map huiqian_table_cfg = config.get("huiqian_table_cfg")==null?new HashMap():(Map) config.get("huiqian_table_cfg");
        Map r1 = busi.get_table_log_detail(activity,subSessionList,this_table_cfg,huiqian_table_cfg);
        return r1;
    }

    @PostMapping("/get_grid_cfg")
    @ResponseBody
    public AjaxResult get_grid_cfg(@RequestBody Map<String, Object> map) {
        Integer obj_id = (Integer) map.get("obj_id");
        String obj_type = (String) map.get("obj_type");
        return AjaxResult.success("success",CompGridStore.getInstance().get(obj_type + obj_id));
    }

    @RequestMapping("/get_search_options")
    @ResponseBody
    public Map get_search_options(@RequestBody Map<String, Object> map,HttpSession session) throws Exception {
        Map colMap = (Map) map.get("col");
        String query = map.get("query").toString();
        Map result = serviceConfigService.get_distinct_search_col_options(colMap,query,session);
        return result;
    }


//    /**
//     * 获取组件的配置信息
//     * @param obj_id 模块组id
//     * @param obj_type 模块组id
//     */
//    public Map get_grid_cfg(Integer obj_id,String obj_type) {
//
//        List<Map> compLs = baseDBService.selectEq("v_comp_grid", Lutils.genMap("obj_id", obj_id, "obj_type", obj_type));
//        if(compLs==null || compLs.size()==0){
//            return null;
//        }
//        Map comp = compLs.get(0);
//        Integer comp_id = (Integer) comp.get("id");
//        //获取表格列，含渲染器
//        List<Map> gridCols = baseDBService.selectEq("v_comp_grid_col", Lutils.genMap("comp_id", comp_id), Lutils.genMap("ord","asc"));
//        for (Map col : gridCols) {
//            Map render = CompValueRenderStore.getInstance().get("comp_grid_col"+col.get("id"));
//            if(render!=null)
//                col.put("CompValueRender", render);
//        }
//        comp.put("gridCols", gridCols);
//        //获取数据源，含数据源列
//        Map ds = CompDataSourceStore.getInstance().get("comp_grid"+comp_id);
//        comp.put("ds", ds);
//        //获取搜索框配置
//        List<Map> searchGridCols = serviceConfigService.getSearchGridCols(gridCols, comp_id);
//        comp.put("searchGridCols", searchGridCols);
//        //添加表格组件中的按钮
//        List btnIds = new ArrayList();
//        //获取表格顶部按钮
//        List<Map> gridTopBtns =  businessMapper.get_execs_of_obj_type(
//                Lutils.genMap("obj_type", "comp_grid_top_btn",
//                        "obj_ids", Lutils.genList(comp_id)));
//        btnIds.addAll(gridTopBtns.stream().map(o -> o.get("id")).collect(Collectors.toList()));
//
//        //将列的按钮，添加到关联的列中
//        for (Map col : gridCols) {
//            col.put("btns", ExecStore.getInstance().getByMaster("comp_grid_col_btn"+col.get("id")));
//        }
//        //提取出顶部按钮
//        comp.put("topBtns", ExecStore.getInstance().getByMaster("comp_grid_top_btn"+comp.get("id")));
//        return comp;
//    }

//    public Map get_form_cfg(Integer view_id) {
//        Map comp = new HashMap();
//        //获取表单form
//        List<Map> formList = businessMapper.get_form_fields(Lutils.genMap("view_id", view_id));
//        List<Map> flowList = businessMapper.get_form_fields_related_flows(Lutils.genMap("view_id", view_id));
//        //用table_col表的column_name替换form_field的field
//        for (Map c : formList) {
//            c.put("field", c.get("table_column") != null ? c.get("table_column").toString() : c.get("field"));
//            List<Map> flow = flowList.stream().filter(o->o.get("op_id").equals(c.get("rel_manual_op_id"))).collect(Collectors.toList());
//            c.put("op", flow.size()>0 ? flow.get(0) : Lutils.genMap());
//        }
//        //暂时不补充
//        comp.put("formItems", formList);
//        return comp;
//    }

    @PostMapping("/get_rows")
    @ResponseBody
    public AjaxResult get_rows(@RequestBody Map<String, Object> submitMap, HttpSession httpSession) throws Exception {
        //获取数据源ID
        String ds_id = (String) submitMap.get("ds_id");
        CompDataSource ds = (CompDataSource)Modules.getInstance().get(ds_id, true);
        ExecContext execContext = new ExecContext();
        execContext.addContexts( (List<Map>) submitMap.get("session") );
        UserAction userAction = new UserAction();
        userAction.setUserInfo(httpSession);
        /**获取数据源配置*/
        TableDataInfo dataInfo = CompUtils.getInstance().get_ds_data(ds, submitMap, execContext , userAction,true);
        return AjaxResult.success("success", dataInfo);
    }



    @PostMapping("/get_all_rel_table_data")
    @ResponseBody
    public TableDataInfo get_all_rel_table_data(@RequestBody Map<String, Object> map) {
        String comp_table_id = (String) map.get("table_id");
        //找到主表格与依赖关系
        List<Map> joinForeignTables = businessMapper.get_table_recurive_relations(Lutils.genMap("table_id", comp_table_id));
        //找到要显示的列
//        List<Map> selColumnsList = baseDBService.selectEq("v_view_grid_col", Lutils.genList("table_id","table_col_id", "field"), Lutils.genMap("view_id", view_id));
        List<Map> selColumnsList = new ArrayList<>();
        //剔除自定义列，自定义列的table_col_id是空的
        selColumnsList = selColumnsList.stream().filter(o -> Lutils.nvl((Integer) o.get("table_col_id"), -1) > 0).collect(Collectors.toList());
        map.put("table_id", comp_table_id);
        map.put("joinForeignTables", joinForeignTables);
        map.put("selColumnsList", selColumnsList);

        List list = null;
        if (map.get("orderByColumn") == null) {
            map.put("orderByColumn", "id_");
            map.put("isAsc", "desc");
        }
        startPage(map);
        list = businessMapper.get_grid_data(map);
        return getDataTable(list);
    }

    public String compName(Object comp_name) {
        if(comp_name==null)
            return "";
        else if (comp_name.equals("CompForm")) {
            return "表单";
        } else if (comp_name.equals("CompGrid") || comp_name.equals("CompReportForms")) {
            return "表格";
        }
        return "";
    }

    public String opName(Object op_name) {
        if (op_name.equals("load")) return "加载";
        else if (op_name.equals("insert")) return "添加";
        else if (op_name.equals("insert-grid-rows")) return "添加表格批量数据";
        else if (op_name.equals("update")) return "更新";
        else if (op_name.equals("delete")) return "删除";
        else if (op_name.equals("complete")) return "处理任务";
        else if (op_name.equals("complete-grid-rows")) return "处理表格批量任务";
        else if (op_name.equals("revoke")) return "撤回任务";
        else if (op_name.equals("start")) return "任务启动";
        return (String) op_name;
    }

    public String opFullName(Map op) {
        return "【" + compName(op.get("comp_name")) + ":" + op.get("view_name") + "】" + opName(op.get("op_type"));
    }

    @PostMapping("/btn_click")
    @ResponseBody
    public AjaxResult btn_click(@RequestBody Map<String, Object> map, HttpSession httpSession){
        ExecResult execResult= new ExecResult();
        try {
            handle_btn_click(map, httpSession, execResult);
            System.out.println(execResult);
        }catch(Exception e){
            e.printStackTrace();
//            return AjaxResult.success("failed",e.getMessage());
        }
        return AjaxResult.success(execResult.state?"success":"failed", execResult);
    }

    @Transactional(rollbackFor = Exception.class, isolation = Isolation.READ_UNCOMMITTED)
    public void handle_btn_click(Map map, HttpSession httpSession,ExecResult execResult) throws Exception {
        //所点击按钮的id
        String exec_id = (String) map.get("exec_id");
        //操作时间
        UserAction userAction = new UserAction(exec_id, db);
        userAction.setUserInfo(httpSession);
        userAction.saveToDB();
        //将提交的数据插入操作会话中
        ExecContext execContext = new ExecContext((List<Map>)map.get("session"));
        //点击按钮的动作
        Exec exec = ExecStore.getInstance().get(exec_id);
        //返回值
        Map<String, Object> result1 = new HashMap();
        //记录器，避免死循环
        Map<String,Integer> opExecCounts = new HashMap<>();
        int execOpIndex = 0;
        while(execOpIndex<exec.ops.size()) {
            ExecOp op = exec.ops.get(execOpIndex);
            int exeCount = Lutils.nvl(opExecCounts.get(op.id),0);
            opExecCounts.put(op.id, ++exeCount);
            if(exeCount>1){
                throw new Exception("动作"+op.id+"：循环执行了"+exeCount+"次，超出限制");
            }
            //判断终止条件sql
//            List<Map> submit_list =
//                    submits.values()                      // Collection<Object>
//                            .stream()                      // 转 Stream
//                            .findFirst()                   // 取第一项（LinkedHashMap 顺序可预期）
//                            .map(v -> (List<Map>) v)
//                            .orElse(Collections.emptyList());
//            if( Lutils.nvl(op.stop_sql,"").length() > 0 ){
//                busi.executeStop(op, submit_list);
//            }
            String defined_session_list_str = op.defined_session_list;
            //追加会话数据
            try{
                List defined_session_list = (List) JSON.parse(defined_session_list_str);
                //将提交的数据插入操作会话中
                ((List<Map>) map.get("session")).addAll(defined_session_list);
                execContext.PutSubmitSessionIntoContextList(defined_session_list);
            }catch(Exception e){
                System.out.println("defined_session_list转换对象失败，跳过");
            }
            //不符合执行限制要求，主要判断是否在动作约束的环节
            if(op.op_type.equals("goto")){
                if(op.gotoLs.size()==0||op.gotoLs.get(0).goto_op_id==null)
                    throw new Exception("直接跳转设置错误：缺失跳转指向");
                int gotoIndex = -1;
                for(int i=0;i<exec.ops.size();i++){
                    if(exec.ops.get(i).id.equals(op.gotoLs.get(0).goto_op_id)){
                        gotoIndex = i;
                    }
                }
                execOpIndex = gotoIndex;
            }
            else if(op.op_type.equals("cas_goto")){
                String gotoOpId = null;
                for (ExecOpGoto cas: op.gotoLs) {
                    if (cas.type.equals("any")) {
                        gotoOpId = cas.goto_op_id;
                    }
                    else {
                        //执行函数
                        Expression expression = AviatorEvaluator.compile(cas.script, true);
                        List<String> vars = expression.getVariableFullNames();
                        //注入变量
                        Map<String, Object> cfgVars = execContext.getEnvironmentVariables(op);
                        for(String varName: vars){
                            String firstName = varName.split(".")[0];
                            if(!cfgVars.containsKey(firstName)){
                                throw new Exception("环境变量中，未找到" + firstName);
                            }
                            if(varName.split(".").length>0){
                                Object obj = cfgVars.get(firstName);
                                if(obj instanceof Map){
                                    Map row = (Map) obj;
                                    if(!row.containsKey(varName.split(".")[1])){
                                        throw new Exception("环境变量"+firstName+"中不包含"+varName.split(".")[1]+"，无法访问"+varName);
                                    }
                                }
                                else{
                                    throw new Exception("环境变量"+firstName+"非Map，无法访问"+varName);
                                }
                            }
                        }
                        Boolean rs = (Boolean) expression.execute(cfgVars);
                        if (rs) {
                            gotoOpId = cas.goto_op_id;
                        }
                    }
                }
                if(gotoOpId!=null){
                    int gotoIndex = -1;
                    for(int i=0;i<exec.ops.size();i++){
                        if(exec.ops.get(i).id.equals(gotoOpId)){
                            gotoIndex = i;
                            break;
                        }
                    }
                    if(gotoIndex!=-1) {
                        execOpIndex = gotoIndex;
                        continue;
                    }
                }
                break;   //结束整个动作流程
            }
            else if (op.op_type.equals("cache_ds_rows")) {
                busi.CacheDSRows(op, execContext, userAction, execResult);
            }
            else if(op.op_type.equals("data_sync")){
                busi.DataSync(op, execContext, userAction, execResult);
            }
            else if (op.op_type.equals("insert")) {
                busi.Insert(op, execContext, userAction, execResult);
            }
            else if (op.op_type.equals("update")) {
                busi.Update(op, execContext, userAction, execResult);
            }
            else if (op.op_type.equals("delete")) {
                busi.Delete(op, execContext, userAction, execResult);
            }
            else if (op.op_type.equals("validate")) {
                busi.Validate(op, execContext, userAction, execResult);
            }
            else if (op.op_type.equals("multi_delete")) {
                busi.MultiDelete(op, execContext, userAction, execResult);
            }
            else if (op.op_type.equals("manual_to_node")) {
                busi.ManualToNode(op, execContext, userAction, execResult);
            }
            else if (op.op_type.equals("complete")) {
                busi.Complete(op, execContext, userAction, execResult);
            }
            else if (op.op_type.equals("revoke")) {}
            else if (op.op_type.equals("open")) {}
            else if (op.op_type.equals("trans")) {
                busi.executeTrans(op, execContext, userAction, execResult);
            }
            if(op.then_stop)
                break;
            execOpIndex++;
        }
    }

    @PostMapping("/get_views_recursive")
    @ResponseBody
    public AjaxResult get_views_recursive(@RequestBody Map<String, Object> map) {
        List<Map> ll = businessMapper.getViewRecrusive(map);
        //找到表名
        return AjaxResult.success("success", ll);
    }

    /**
     * 获取处理器下拉框选项
     */
    @GetMapping("/code-options")
    @ResponseBody
    public AjaxResult getHandlerOptions() {
        return AjaxResult.success(codeHandlerScanner.getHandlerOptions());
    }

    @PostMapping("/get_view_contents")
    @ResponseBody
    public AjaxResult get_view_contents(@RequestBody Map<String, Object> map) {
        List<Map> ll = businessMapper.getViewTree(map);
        List<View> views = new ArrayList<>();
        for(Map node: ll){
            if(!Objects.equals(node.get("view_type"),"folder")) {
                View v = (View)Modules.getInstance().get(node.get("id"), false);
                if(v!=null) {
                    v.parent_id = (String)node.get("parent_id");
//                    if(!(Objects.equals(v.view_type,"comp")&&Objects.equals(v.comp_name,"CompDataSource")))
                        views.add(v);
                    if (v.view_type!=null && v.view_type.equals("comp") && v.comp_id != null) {
                        v.comp = Modules.getInstance().get(v.comp_id, false);
                    }
                }
            }
        }
        List<View> roots = views.stream().filter(v->Objects.equals(v.view_type,"indexTab")||Objects.equals(v.view_type,"modal")||Objects.equals(v.view_type,"drawer")).collect(Collectors.toList());
        if(roots.size()>0) {
            View root = roots.get(0);
            boolean changed = busi.PutChildrenDSToRootDsList(root, views);
            if(changed)//保存
                Modules.getInstance().save(root.id, root);
        }
        //找到表名
        return AjaxResult.success("success", views);
    }

    @PostMapping("/init_tree_editor")
    @ResponseBody
    public AjaxResult init_tree_editor(@RequestBody Map<String, Object> submitMap, HttpSession httpSession) throws Exception {
        String ds_id = (String) submitMap.get("ds_id");
        CompDataSource ds = (CompDataSource) Modules.getInstance().get(ds_id, true);
        Map<String, Object> map = new HashMap<>();
        map.put("pageSize", 3000);
        map.put("pageNum", 1);
        ExecContext execContext = new ExecContext();
        List<Map> submitSession = map.get("session")!=null?(List<Map>)map.get("session"):new ArrayList<>();
        execContext.PutSubmitSessionIntoContextList(submitSession);
        UserAction userAction = new UserAction();
        userAction.setUserInfo(httpSession);
        userAction.action_time = new Date();
        TableDataInfo dataInfo = CompUtils.getInstance().get_ds_data(ds, map, execContext, userAction, true);
        List<?> rows = dataInfo.getRows();
        List<Map<String, Object>> dataRows = new ArrayList<>();
        for (Object row : rows) {
            Map<String, Object> dataRow = (Map<String, Object>) row;
            dataRows.add(dataRow);
        }
        return AjaxResult.success("success", dataRows);
    }


    @PostMapping("/get_view_cfg_contents")
    @ResponseBody
    public AjaxResult get_view_cfg_contents(@RequestBody Map<String, Object> map) {
        List<Map> ll = businessMapper.getViewTree(map);
        List<Object> re = new ArrayList<>();
        for(Map node: ll){
            if(!Objects.equals(node.get("view_type"),"folder")){
                View v = Lutils.StringToClass((String)node.get("json"), View.class);
                re.add(v);
                if(v.view_type.equals("comp")){
                    v.comp = Modules.getInstance().get(v.comp_id, false);
                }
            }else{
                re.add(node);
            }
        }
        //找到表名
        return AjaxResult.success("success", re);
    }

//    @PostMapping("/get_view_aggr_data")
//    @ResponseBody
//    public List<Map> get_view_aggr_data(@RequestBody Map<String, Object> map,HttpSession session) throws Exception{
//        Integer user_id = (Integer) session.getAttribute("userId");
//        Integer comp_id = (Integer) map.get("comp_id");
//        Map m = new HashMap();
//        m.put("user_id", user_id);
//        m.put("comp_id", comp_id);
//        Map comp = CompCountAggrStore.getInstance().get(comp_id);
////        List<Map> itemList = (List)comp.get("itemList");
//        //添加视图的顶部或底部按钮
//        //根据view计数
////        for(Map countItem: itemList){
////            Integer ds_id = (Integer) countItem.get("ds_id");
////            Map<String,Object> ds = (Map) Lutils.copy(CompDataSourceStore.getInstance().get(ds_id));
////            //判断数据集是否要取值
////            List fieldTypes = Lutils.genList(countItem.get("label_field_type"),countItem.get("count_field_type"),countItem.get("total_field_type"));
////            DataSourceDataContent dsdc = new DataSourceDataContent(fieldTypes);
////            Object re = CompDataSourceStore.getInstance().get_ds_data((Integer)ds.get("id"),null, user_id, dsdc.containRows, dsdc.containTotal);
////            ds.put("initData", re);
////            countItem.put("ds", ds);
////        }
//        return itemList;
//    }

    @PostMapping("/get_form_data_source")
    @ResponseBody
    public Integer get_form_data_source(@RequestBody Map<String, Object> map) {
        Map session = (Map) map.get("session");
        Integer view_id = (Integer) map.get("view_id");
        Map viewObj = db.selectEq("v_view", Lutils.genMap("view_id", view_id)).get(0);

        return (Integer)viewObj.get("cnt");
    }

    @PostMapping("/get_datasource_fields")
    @ResponseBody
    public AjaxResult get_datasource_fields(@RequestBody Map<String, Object> map) {
        Integer obj_id = (Integer) map.get("obj_id");
        Integer ds_id = (Integer) map.get("ds_id");
        String obj_type = (String) map.get("obj_type");
        Map ds = null;
        if(obj_id!=null&&obj_type!=null){
            ds = DSStore.getInstance().get(obj_type+obj_id);
        }
        else{
            ds = DSStore.getInstance().get(ds_id);
        }
        if(ds!=null) {
            return AjaxResult.success("success",  ds.get("fields"));
        }else{
            return AjaxResult.success("failed");
        }
    }

    @PostMapping("/set_view_cache_state")
    @ResponseBody
    public boolean set_view_cache_state(@RequestBody Map<String, Object> map) {
        Boolean setState = (Boolean)map.get("state");
        groupViewCacheEnable = setState;
        return groupViewCacheEnable;
    }


    @PostMapping("/convert_modules")
    @ResponseBody
    public boolean convert_modules(@RequestBody Map<String, Object> map) {
        busi.convert();
        return true;
    }

    @PostMapping("/gen_sql")
    @ResponseBody
    public AjaxResult gen_sql(@RequestBody JSONObject jsonObject) throws Exception{
        String tableName = "z_" + jsonObject.getString("tableName");
        String replaceTableName = jsonObject.getString("tableName").replace("table", "t");
        Map<String, Object> result = new HashMap<>();
        result.put("data_sql", genSqlService.generateSelectWithLeftJoins(tableName));
        result.put("order_sql", "order by " + replaceTableName + ".create_time_ desc");
        return AjaxResult.success("success", result);
    }

    @PostMapping("/get_table_full_info")
    @ResponseBody
    public AjaxResult getTableFields(@RequestBody JSONObject jsonObject) throws Exception {
        String tableName = jsonObject.getString("tableName");
        Map<String, List<TableColInfoResp>> tableFullInfo = genSqlService.getTableFullColumn(tableName);
        return AjaxResult.success("success", tableFullInfo);
    }
}
