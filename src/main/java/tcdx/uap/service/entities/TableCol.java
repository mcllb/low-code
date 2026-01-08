package tcdx.uap.service.entities;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import tcdx.uap.constant.Constants;

import java.io.Serializable;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
@AllArgsConstructor
@NoArgsConstructor
@Data
public class TableCol implements Serializable {
    public String id;
    public String name;
    public String field;
    /** 数据类型 */
    public String data_type;  //varchar int4
    public Integer numeric_precision;
    public Integer varchar_size;
    public String field_content_from;
    public Integer rel_dict_id;  //type=='dict'时，需要关联字典id

}
