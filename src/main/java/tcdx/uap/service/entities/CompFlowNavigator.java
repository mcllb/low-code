package tcdx.uap.service.entities;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import tcdx.uap.service.store.Modules;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/*
* 此组件依赖会话中的node
* */
@JsonIgnoreProperties
public class CompFlowNavigator  implements Serializable {
    public String id;
    public String table_id;
    public String flow_edge_id;
    public String op_id;
    //当前节点的出方向路径
    public List<FlowEdge> edges;
    //
    public void create(String id){
        this.id = id;
    }

    //get_views时获取
    public void getNodeAndOutEdges(String node_id){
        Table tbl = (Table) Modules.getInstance().get(table_id,true);
        FlowNode node = tbl.getNode(node_id);
        if(node!=null)
            edges = node.outEdges;
        else
            edges = new ArrayList<>();
    }

}
