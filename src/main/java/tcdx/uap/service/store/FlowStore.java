package tcdx.uap.service.store;

import lombok.Getter;
import tcdx.uap.common.utils.Lutils;
import tcdx.uap.common.utils.SqlUtil;
import tcdx.uap.mapper.BusinessMapper;
import tcdx.uap.service.BaseDBService;

import java.util.*;
import java.util.stream.Collectors;

public class FlowStore {

    @Getter
    private static FlowStore instance = new FlowStore();

    private FlowStore() {
        System.out.println("单例模式初始化ViewStore");
    }

    //indexPage/modal/drawer
    private final Map<Object,Map> store = new HashMap();
    private final Map<Object,Map> edgeStore = new HashMap();
    private final Map<Object,Map> nodeStore = new HashMap();

    BaseDBService db;
    BusinessMapper busiMapper;
    public void InitAll(BaseDBService db, BusinessMapper busiMapper) {
        this.db = db;
        this.busiMapper = busiMapper;
        InitAll();
    }

    public void InitAll() {
        List<Map> v_table = db.selectByCauses("v_table", null, null);
        List<Map> v_flow_node = busiMapper.get_undeleted_flow_node(Lutils.genMap());
        List<Map> v_flow_edge = busiMapper.get_undeleted_flow_edge(Lutils.genMap());
        List<Map> v_flow_node_event = db.selectByCauses("v_flow_node_event", null, Lutils.genMap("ord", "asc"));
        assemble(v_table, v_flow_node, v_flow_edge, v_flow_node_event);
    }

    public void set(Integer table_id) {
        List<Map> v_table = db.selectEq("v_table", Lutils.genMap("id", table_id));
        List<Map> v_flow_node = new ArrayList<>();
        List<Map> v_flow_edge = new ArrayList<>();
        List<Map> v_flow_node_event = new ArrayList<>();
        List<Integer> tids = v_table.stream().map(t->(Integer)t.get("id")).collect(Collectors.toList());
        if(v_table.size() > 0) {
            v_flow_node = busiMapper.get_undeleted_flow_node(Lutils.genMap("table_id", table_id));
            v_flow_edge = busiMapper.get_undeleted_flow_edge(Lutils.genMap("table_id", table_id));
            v_flow_node_event = db.selectByCauses("v_flow_node_event", SqlUtil.and( SqlUtil.in("sub_table_id", tids)), Lutils.genMap("ord", "asc"));
        }
        assemble(v_table, v_flow_node, v_flow_edge, v_flow_node_event);
    }

    public void assemble(List<Map> v_table, List<Map> v_flow_node, List<Map> v_flow_edge, List<Map> v_flow_node_event){
        //先存到缓存
        for(Map edge: v_flow_edge){
            //节点与边未连接的不取
            edgeStore.put(edge.get("id"), edge);
        }
        //再做结构拼接
        for(Map node: v_flow_node){
            //node发出的边
            List<Map> out_edge = v_flow_edge.stream().filter(e->Objects.equals(node.get("id"), e.get("src"))).collect(Collectors.toList());
            List<Map> in_edge = v_flow_edge.stream().filter(e->Objects.equals(node.get("id"), e.get("dst"))).collect(Collectors.toList());
            node.put("outEdges", out_edge);
            node.put("inEdges", in_edge);
            node.put("events", v_flow_node_event.stream()
                    .filter(e -> Objects.equals( node.get("id"), e.get("when_sub_node"))
                              && Objects.equals( node.get("table_id"), e.get("sub_table_id")))
                    .collect(Collectors.toList()));
            nodeStore.put( node.get("id"), node );
        }
        for(Map edge: v_flow_edge){
            edge.put("srcLabel", nodeStore.get(edge.get("src")).get("label"));
            edge.put("srcType", nodeStore.get(edge.get("src")).get("type"));
            edge.put("dstLabel", nodeStore.get(edge.get("dst")).get("label"));
            edge.put("dstType", nodeStore.get(edge.get("dst")).get("type"));
        }
        for (Map table : v_table) {
            table.put("edges", v_flow_edge.stream().filter(o-> Objects.equals(o.get("table_id"),table.get("id"))).collect(Collectors.toList()));
            table.put("nodes", v_flow_node.stream().filter(o-> Objects.equals(o.get("table_id"),table.get("id"))).collect(Collectors.toList()));
            table.put("events", v_flow_node_event.stream().filter(e->Objects.equals(table.get("id"), e.get("sub_table_id"))).collect(Collectors.toList()));
            store.put(table.get("id"), table);
            store.put("table"+table.get("id"), table);
        }
    }


    public Map get1(Object key) {
        Map flow = store.get(key);
        return flow;
    }

    /**
     * 获取组件，同时装配动态数据
     * @param key key可以是Id，也可以是ObjType+Id
     * */
    public Map get(Object key) {
        Map flow = store.get(key);
        List ls1 = TableStore.getInstance().getPriTableObjs(key);
        List ls2 = TableStore.getInstance().getSubTableObjs(key);
        flow.put("priTableObjs", ls1);
        flow.put("subTableObjs", ls2);
        return flow;
    }

    public List<Map> getTableStartNode(Integer table_id){
        Map table = store.get(table_id);
        List<Map> startNodes = ((List<Map>)table.get("nodes")).stream().filter(o->Objects.equals(o.get("type"), "start")).collect(Collectors.toList());
        return startNodes;
    }

    public Map getNodeFirstOutEdge(Integer node_id){
        Map node = nodeStore.get(node_id);
        List<Map> edge = (List)node.get("outEdges");
        return edge.size()>0?edge.get(0):null;
    }

    /**
     * 浅获取组件，不装配动态数据
     * @param key key可以是Id，也可以是ObjType+Id
     * */
    public Map getSimplify(Object key) {
        Map flow = store.get(key);
        return flow;
    }

    public List<Map> getNodeOutEdges(Object node_id) {
        Map node = nodeStore.get(node_id);
        return (List)node.get("outEdges");
    }

    public Map getNode(Object node_id) {
        Map node = nodeStore.get(node_id);
        return node;
    }

    public Map getEdge(Object edge_id) {
        Map edge = edgeStore.get(edge_id);
        return edge;
    }

    public List<Map> getTableNodeEvents( String pri_tbl, String pri_tbl_node, String sub_tbl_node ){
        Map subNodeObj = nodeStore.get(sub_tbl_node);
        List<Map> events = (List)subNodeObj.get("events");
        if(events==null || events.isEmpty()){
            return new ArrayList<>();
        }
        return events.stream()
                .filter(e->(
                              Objects.equals(e.get("pri_table_id"), pri_tbl )
                            &&Objects.equals(e.get("pri_node"), pri_tbl_node )
                            )
                        ||e.get("pri_table_id")==null
                ).collect(Collectors.toList());

    }
}

