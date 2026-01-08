package tcdx.uap.service.entities;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import tcdx.uap.service.store.Modules;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@JsonIgnoreProperties(ignoreUnknown = true)
@AllArgsConstructor
@NoArgsConstructor
public class CompGridCol  implements Serializable {
    public String id;
    public String title;
    public Integer min_width;
    public Integer width;
    public String fixed;
    public String search_type;
    public Boolean search_expand;
    public Boolean enable_click;
    public Boolean enable_edit;
    public CompValueRender compValueRender;
    public Exec exec; //单元格点击事件
    public List<Exec> btns;//列按钮
    //get时更新
    public CompDataSourceField dsField;
    //get
    public TableCol tableCol;  //table_field
    public String field_type; //flow_field、table_fff 、 defined_fied
    public String flow_edge_id;//flow_field
    public String table_id;
    public String field;
    public String foreign_key_view_id;
    public CompValueEditor compValueEditor;

    public void setDsFieldInfo(){
        if(compValueRender!=null){
            if(compValueRender.ds_id!=null) {
                CompDataSource ds = (CompDataSource) Modules.getInstance().get(compValueRender.ds_id, true);
                if (Objects.nonNull(ds)) {
                    dsField = ds.getField(compValueRender.ds_field_id);
                }
            }
        }
    }
}
