package tcdx.uap.service.entities;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import tcdx.uap.service.store.Modules;

import java.io.Serializable;
import java.util.List;


@JsonIgnoreProperties(ignoreUnknown = true)
@AllArgsConstructor
@NoArgsConstructor
public class CompValueRender  implements Serializable {
    public String id;
    public String render_type;
    public String func;
    public String defined_value;
    public String datetime_fmt;
    public Boolean use_defined_value;
    public String ds_id;
    public String ds_field_id;
    public String lng_ds_field_id;
    public String lat_ds_field_id;
    public String prefix_icon;
    public String prefix_icon_style;
    public Boolean enable_click;
    public Boolean required;
    public Exec exec = new Exec();
    //get
    public CompDataSourceField dsField;

    public void create(String id){
        this.id = id;
        this.render_type="text";
        this.use_defined_value= true;
        this.defined_value = "";
        this.required = false;
        exec.create();
    }

//    public void setDsFieldInfo(){
//        if(ds_id!=null) {
//            CompDataSource ds = (CompDataSource) Modules.getInstance().get(ds_id, true);
//            dsField = ds.getField(ds_field_id);
//        }
//    }
}
