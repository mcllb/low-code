package tcdx.uap.service.entities;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import tcdx.uap.constant.Constants;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@JsonIgnoreProperties(ignoreUnknown = true)
@AllArgsConstructor
@NoArgsConstructor
public class FlowNode implements Serializable {
    public String id;
    public String label;
    public String type;
    public Integer x;
    public Integer y;
    public String sub_flow_table_id;
    public String upd_sql;
    public String sub_finished_sql;
    public List<FlowNodeEvent> events = new ArrayList<>(); //深度拷贝字段
    //深拷贝时获取
    public List<FlowEdge> inEdges = new ArrayList<>();
    public List<FlowEdge> outEdges = new ArrayList<>();
}
