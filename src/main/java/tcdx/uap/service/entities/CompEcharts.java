package tcdx.uap.service.entities;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@JsonIgnoreProperties(ignoreUnknown = true)
@AllArgsConstructor
@NoArgsConstructor
public class CompEcharts  implements Serializable {
    public String id;
    public String ds_id;
    public String title;
    public String title_ds_field_id;
    public String series_ds_field_id;
    public String x_ds_field_id;
    public String y_ds_field_id;
    public String echarts_type;
    public String start_time;
    public String end_time;
    public String status;
    public String progress;
    public String duration_days;
    public String group;
    public String predecessors;
    public String parent_id;
    public String priority;
    public String notes;
    public String is_milestone;
    public String skin;
    public Exec exec = new Exec();
    public CompDataSourceField valueDsField;
    public void create(String id){
        this.id = id;
        exec = new Exec();
        exec.create();
    }

}
