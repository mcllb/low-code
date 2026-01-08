package tcdx.uap.service.entities;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
@NoArgsConstructor
@AllArgsConstructor
public class CompTree implements Serializable {
    public String id;
    public String ds_id;
    public String label_ds_field_id;
    public String parent_ds_field_id;
    public String search_ds_id;
    public String search_ds_field_id;
    public Exec exec = new Exec();
    public CompDataSourceField search_ds_field;
    public Map initData = new HashMap();

    public void create(String id){
        this.id = id;
        exec = new Exec();
        exec.create();
    }

//    public void setDsFieldInfo(){
//        if(ds_id!=null) {
//        }
//    }
}
