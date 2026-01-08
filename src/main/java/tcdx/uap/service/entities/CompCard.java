package tcdx.uap.service.entities;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import tcdx.uap.constant.Constants;
import tcdx.uap.service.store.Modules;

import java.io.Serializable;

@JsonIgnoreProperties(ignoreUnknown = true)
@AllArgsConstructor
@NoArgsConstructor
public class CompCard implements Serializable {
    public String id;
    public String name;
    public String ds_id;
    public String value_ds_field_id;
    public String title;
    public String color;
    public String description;
    public String icon;
    public String unit;
    public Exec exec = new Exec();
    //get
    public CompDataSourceField valueDsField;

    public void create(String id){
        this.id = id;
        exec = new Exec();
        exec.create();
    }

}
