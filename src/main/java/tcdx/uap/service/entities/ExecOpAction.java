package tcdx.uap.service.entities;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@JsonIgnoreProperties(ignoreUnknown = true)
@AllArgsConstructor
@NoArgsConstructor
public class ExecOpAction  implements Serializable {
    public String id;
    public String session_type;
    public String from_op_id;
    public String from_op_db_type;  //
    public String from_table_id;
    public String from_table_col_id;
    public String from_ds_id;
    public String from_ds_field_id;
    public String before_or_after; //状态
    public String cache_value_alias;
    public String cache_value_type;
}
