package tcdx.uap.service.entities;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@JsonIgnoreProperties(ignoreUnknown = true)
@AllArgsConstructor
@NoArgsConstructor
public class FlowNodeEvent implements Serializable {
    public String id;
    public String pri_table_id;
    public String pri_node;
    public String sub_table_id;
    public String when_sub_node;
    public String event_type;
    public String sql_type;
    public String sql_str;
    public String comment;
    //深拷贝字段：需要查询外部对象
    public String priTableName;
    public String priNodeLabel;
    public String subTableName;
    public String subNodeLabel;
}
