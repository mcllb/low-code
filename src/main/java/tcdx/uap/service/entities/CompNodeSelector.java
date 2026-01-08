package tcdx.uap.service.entities;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import tcdx.uap.service.store.Modules;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/*
* 此组件依赖会话中的node
* */
@JsonIgnoreProperties
public class CompNodeSelector implements Serializable {
    public String id;
    public String table_id;
    public List<String> candidateNodes = new ArrayList<>();
    public String op_id;  //绑定的
    //当前节点的出方向路径
    public List<FlowNode> nodes;
    //
    public void create(String id){
        this.id = id;
    }

    //get_views时获取
    public void setNodes(){
        Table tbl = (Table) Modules.getInstance().get(table_id,true);
        nodes = tbl.nodes.stream().filter(o-> candidateNodes.contains(o.id)).collect(Collectors.toList());
    }

}
