package tcdx.uap.service.entities;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import tcdx.uap.common.utils.Lutils;

import java.io.Serializable;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
@AllArgsConstructor
@NoArgsConstructor
public class CompCarousel  implements Serializable {
    public String id;
    public String ds_id;
    public String title_ds_field_id;
    public String img_ds_field_id;
    public Integer item_num=4;
    public Integer height;
    public Exec exec = new Exec();
    public void create(String id){
        this.id = id;
        exec = new Exec();
    }
}
