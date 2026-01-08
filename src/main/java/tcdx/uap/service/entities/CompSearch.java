package tcdx.uap.service.entities;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@JsonIgnoreProperties(ignoreUnknown = true)
@AllArgsConstructor
@NoArgsConstructor
public class CompSearch implements Serializable {
    public String id;
    public String name;
    public String ds_id;

    public Exec exec = new Exec();
    //get
    public CompDataSourceField valueDsField;

    public void create(String id){
        this.id = id;
        exec = new Exec();
        exec.create();
    }
}
