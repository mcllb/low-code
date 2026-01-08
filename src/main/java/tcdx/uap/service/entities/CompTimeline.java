package tcdx.uap.service.entities;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
@AllArgsConstructor
@NoArgsConstructor
public class CompTimeline implements Serializable {
    public String id;
    public String table_id;
    public Exec exec = new Exec();
    public Map initData = new HashMap();

    public void create(String id){
        this.id = id;
        exec = new Exec();
        exec.create();
    }
}
