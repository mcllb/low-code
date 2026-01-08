package tcdx.uap.service.store;

import lombok.Getter;
import tcdx.uap.common.utils.Lutils;
import tcdx.uap.common.utils.SqlUtil;
import tcdx.uap.mapper.ServiceConfigMapper;
import tcdx.uap.service.BaseDBService;

import java.util.*;
import java.util.stream.Collectors;

public class ExecObjStore {
    @Getter
    private static ExecObjStore instance = new ExecObjStore();

    private ExecObjStore() {
        System.out.println("单例模式初始化ExecStore");
    }

    private final Map<Integer, Map> execStore = new HashMap();
    private final Map<String, List> objExecsStore = new HashMap();
    private final Map<Object, Map> execOpStore = new HashMap();
    private final Map<Object, List> execRoleMap = new HashMap();

    BaseDBService db;
    ServiceConfigMapper cfgMapper;
    public void InitAll(BaseDBService db, ServiceConfigMapper cfgMapper){
        this.db = db;
        this.cfgMapper = cfgMapper;
        InitAll();
    }

    //根据用户角色，获取有权限的视图
    public boolean HasBtnPermission(Object exec_id, List<Integer> userRoleIds){
        if( execRoleMap ==null){
            InitAll();
        }
        List<Integer> roles = (List<Integer>) execRoleMap.get(exec_id);
        if(roles!=null ) {
            List<Integer> intersection = roles.stream().filter(userRoleIds::contains).collect(Collectors.toList());
            return !intersection.isEmpty();
        }
        return false;
    }
    public void InitAll(){
        List<Map> v_exec_obj = db.selectByCauses("v_exec_obj", SqlUtil.and(), null);
        List<Map> v_exec_op = db.selectByCauses("v_exec_op", SqlUtil.and(), Lutils.genMap("ord", "asc"));
        List<Map> v_flow_edge = db.selectByCauses("v_flow_edge", SqlUtil.and(
                SqlUtil.in("id",v_exec_op.stream()
                        .filter(o->o.get("flow_edge_id")!=null)
                        .map(o->o.get("flow_edge_id")).collect(Collectors.toList()))
                ), null);
        List<Map> v_exec_op_session = new ArrayList<>();
        if(!v_exec_op.isEmpty()) {
            v_exec_op_session = db.selectIn("v_exec_op_session", "op_id", v_exec_op.stream().map(o -> o.get("id")).collect(Collectors.toList()),
                    Lutils.genMap("ord", "asc"));
        }
        //菜单对应角色
        List<Map> v_role_perm_obj = db.selectEq("v_role_perm_obj", Lutils.genMap("obj_type", "btn"));
        objExecsStore.clear();
        //装配
        assemble(v_exec_obj,v_exec_op,v_exec_op_session, v_flow_edge, v_role_perm_obj);
    }

    public void set(Object exec_id){
        execStore.remove(exec_id);
        Map exec = db.selectOne("v_exec_obj", Lutils.genMap("id", exec_id));
        set(exec.get("obj_type"), exec.get("obj_id"));
    }

    public void set(Object obj_type, Object obj_id){
        objExecsStore.remove((String)obj_type+obj_id);
        List<Map> v_exec_obj = db.selectByCauses("v_exec_obj",
                SqlUtil.and(SqlUtil.eq("obj_type", obj_type),SqlUtil.eq("obj_id", obj_id)),
                Lutils.genMap("ord", "asc"));
        List<Map> v_exec_op = new ArrayList<>();
        List<Map> v_flow_edge = new ArrayList<>();
        List execIds = v_exec_obj.stream().map(o->o.get("id")).collect(Collectors.toList());
        if(execIds!=null && execIds.size()>0) {
            //加载动作
            v_exec_op = db.selectByCauses("v_exec_op",
                    SqlUtil.and(SqlUtil.in("exec_id", execIds)),
                    Lutils.genMap("ord", "asc"));
            List flowIds = v_exec_op.stream().filter(o -> o.get("flow_edge_id") != null).map(o -> o.get("flow_edge_id")).collect(Collectors.toList());
            if (flowIds != null && flowIds.size() > 0) {
                v_flow_edge = db.selectByCauses("v_flow_edge", SqlUtil.and(
                        SqlUtil.in("id", flowIds)
                ), null);
            }
        }
        List<Map> v_exec_op_session = new ArrayList<>();
        List<Map> v_role_perm_obj = new ArrayList<>();
        if(!v_exec_op.isEmpty()) {
            v_exec_op_session = db.selectIn("v_exec_op_session", "op_id", v_exec_op.stream().map(o -> o.get("id")).collect(Collectors.toList()),
                    Lutils.genMap("ord", "asc"));
            //菜单对应角色
            v_role_perm_obj = db.selectByCauses("v_role_perm_obj",
                    SqlUtil.and(SqlUtil.eq("obj_type", "btn"),
                            SqlUtil.in("obj_id", v_exec_obj.stream().map(o->o.get("id")).collect(Collectors.toList()))),
                    null);
        }
        assemble(v_exec_obj, v_exec_op, v_exec_op_session, v_flow_edge, v_role_perm_obj);
    }

    public void assemble(List<Map> v_exec_obj, List<Map> v_exec_op, List<Map> v_exec_op_session,List<Map> v_flow_edge, List<Map> v_role_perm_obj){
        //数据源缓存
        for(Map exec : v_exec_obj){
            String key = (String)exec.get("obj_type")+exec.get("obj_id");
            objExecsStore.put(key, new ArrayList<>());
            execStore.remove(exec.get("id"));
        }
        //组装角色、对象集合
        for(Map exec : v_exec_obj){
            String key = (String)exec.get("obj_type")+exec.get("obj_id");
            exec.put("ops", new ArrayList<Map>());
            List<Map> mRoles = v_role_perm_obj.stream().filter(r-> Objects.equals(r.get("obj_id"), exec.get("id"))).collect(Collectors.toList());
            List<Integer> roleIds = mRoles.stream().map(r-> (Integer)r.get("role_id")).collect(Collectors.toList());
            execRoleMap.put(exec.get("id"), roleIds);
            execStore.put((Integer)exec.get("id"), exec);
            ((List<Map>) objExecsStore.get(key)).add(exec);
        }
        //添加exec_op清单
        for(Map op : v_exec_op){
            execOpStore.put(op.get("id"), op);
            op.put("opActions", v_exec_op_session.stream().filter(o->o.get("op_id").equals(op.get("id"))).collect(Collectors.toList()));
            //add到对应的exec对象的ops中
            Map execObj = execStore.get(op.get("exec_id"));
            if(execObj!=null) {
                //添加flow_edge信息
                if(op.get("flow_edge_id")!=null) {
                    List<Map> op_flow_edges = v_flow_edge.stream().filter(o -> Objects.equals(o.get("id"), op.get("flow_edge_id"))).collect(Collectors.toList());
                    if(op_flow_edges.size()>0) {
                        op.put("edgeObj", op_flow_edges.get(0));
                    }
                    else{
                        op.put("edgeObj", null);
                    }
                }
                List<Map> ops = (List<Map>) execObj.get("ops");
                ops.add(op);
            }
        }
    }

    public void createWhenNotExist(Object obj_type,Object obj_id, Map extraExecParams){
        if(extraExecParams==null)
            extraExecParams = new HashMap();
        extraExecParams.put("obj_id", obj_id);
        extraExecParams.put("obj_type", obj_type);
        extraExecParams.put("ord", 0);
        Map exec = db.insertWhenNotExist("v_exec_obj", extraExecParams,
                Lutils.genMap("obj_id", obj_id, "obj_type", obj_type));
        set(obj_type, obj_id);
    }

    public Map create(Object obj_type,Object obj_id, Map extraExecParams){
        if(extraExecParams==null)
            extraExecParams = new HashMap();
        extraExecParams.put("obj_id", obj_id);
        extraExecParams.put("obj_type", obj_type);
        extraExecParams.put("ord", 0);
        Map comp = db.insertWhenNotExist("v_exec_obj", extraExecParams, Lutils.genMap("obj_id", obj_id, "obj_type", obj_type));
        set(obj_type, obj_id);
        return comp;
    }

    public Map getOp(Object op_id){
        return execOpStore.get(op_id);
    }

    /**
     * 获取组件，同时装配动态数据
     * @param exec_id exec_id
     * */
    public Map get(Object exec_id){
        if(!execStore.containsKey(exec_id))
            return null;
        Map exec = execStore.get(exec_id);
        List<Map> ops = (List<Map>) exec.get("ops");
        for(Map op : ops){
            Integer view_id = (Integer)op.get("view_id");
            Integer from_view_id = (Integer)op.get("from_view_id");
            //操作视图
            if(Objects.equals("view",op.get("op_obj_type"))){
                if(view_id!=null) {
                    Map viewObj = ViewStore.getInstance().getSimplify(view_id);
                    Map fromViewObj = ViewStore.getInstance().getSimplify(from_view_id);
                    if (viewObj != null) {
                        op.put("viewObj_view_type", viewObj.get("view_type"));
                        op.put("viewObj_name", viewObj.get("name"));
                        op.put("viewObj_comp_name", viewObj.get("comp_name"));
                        op.put("viewObj", viewObj);
                    }
                    if (fromViewObj != null) {
                        op.put("fromViewObj_view_type", fromViewObj.get("view_type"));
                        op.put("fromViewObj_name", fromViewObj.get("name"));
                        op.put("fromViewObj_comp_name", fromViewObj.get("comp_name"));
                        op.put("fromViewObj", fromViewObj);
                    }
                }
            }
            //操作视图
            else if(Objects.equals("table",op.get("op_obj_type"))){
                Map tableObj = TableStore.getInstance().get(op.get("table_id"));
                op.put("tableObj_table_display_name", tableObj.get("table_display_name"));
                op.put("tableObj", tableObj);
                Map edgeObj = FlowStore.getInstance().getEdge(op.get("flow_edge_id"));
                op.put("edgeObj", edgeObj);
            }
        }
        return exec;
    }

    public List<Map> getByObj(Object key){
        List<Map> execs = objExecsStore.get(key)==null?new ArrayList<>(): objExecsStore.get(key);
        List<Map> re = new ArrayList<>();
        for(Map exec : execs){
            re.add(get(exec.get("id")));
        }
        return re;
    }
}
