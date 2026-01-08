package tcdx.uap.service.entities;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import tcdx.uap.common.utils.Lutils;

import java.io.Serializable;
import java.util.*;

@JsonIgnoreProperties(ignoreUnknown = true)
@AllArgsConstructor
@NoArgsConstructor
public class FlowEdge implements Comparable<FlowEdge> , Serializable {
    public String id;
    public String label;
    public String src;
    public String dst;
    public String vertices;
    public Integer ord;
    public String condition_field1;
    public String condition_operator1;
    public String condition_value_from_type1;
    public String condition_value_from_col1;
    public String condition_value1;
    public String condition_field2;
    public String condition_operator2;
    public String condition_value_from_type2;
    public String condition_value_from_col2;
    public String condition_value2;
    public String assign_type;
    public String operator_of_table;
    public String operator_of_edge;
    public String operator_of_node;
    public String receiver_of_node;
    public String manual_select_many;
    public String rel_dict_id;
    public String rel_dict_col_id;
    public Boolean assign_required;
    public Boolean assign_multi_users;
    public UserScope userScope;
    public Boolean is_sms;
    //get时获取
    public String srcLabel;
    public String dstLabel;
    public String srcType;
    public String dstType;

    @Override
    public int compareTo(FlowEdge o) {
        return Lutils.nvl(this.ord,0) - Lutils.nvl(o.ord,0);
    }
}
