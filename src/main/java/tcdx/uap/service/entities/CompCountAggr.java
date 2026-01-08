package tcdx.uap.service.entities;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@JsonIgnoreProperties(ignoreUnknown = true)
@AllArgsConstructor
@NoArgsConstructor
public class CompCountAggr  implements Serializable {
    public String id;
    public String ds_id;
    public Integer height;
    public String default_class;
    public String color;
    public String content_layout;
    public String label_ds_field_id;
    public String value_ds_field_id;
    public String total_ds_field_id;
    public String unit_ds_field_id;
    public String unit;
    public String list_type;
    public String direction;
    public String filter_cause;
    public String label_style;
    public String value_style;
    public String total_style;
    public String unit_style;
    public String icon;
    public String display_style;
    public Exec exec;
    public String label;

    public void create(String id){
        this.id= id;
        exec = new Exec();
        exec.create();
    }
}
