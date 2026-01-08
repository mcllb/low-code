package tcdx.uap.service.entities;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import tcdx.uap.common.entity.page.TableDataInfo;
import tcdx.uap.constant.Constants;
import tcdx.uap.service.store.Modules;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;


@JsonIgnoreProperties(ignoreUnknown = true)
@AllArgsConstructor
@NoArgsConstructor
public class CompValueEditor  implements Serializable {
    public String id;
    public Integer dict_id;
    public String editor_type;
    public String grid_id;
    public String ds_id;
    public String ds_field_id;
    public Boolean required;
    public String lng_ds_field_id;
    public String lat_ds_field_id;
    public String sign_ds_field_id;
    public String pdf_ds_field_id;
    public String single_options;
    public String muti_options;
    public String uuid_prefix;
    public Boolean show_reason;
    public String datetime_fmt;
    public Boolean user_multi_mode;
    public UserScope userScope;
    public Map initData = new HashMap();
    public CompGrid compGrid;
    public String value;
    public String tree_ds_id;
    public Integer disp_field_cnt;
    //get
    public CompDataSourceField dsField;
    public CompDataSourceField lngDsField;
    public CompDataSourceField latDsField;
    public CompDataSourceField signDsField;
    public CompDataSourceField pdfDsField;
    public Exec changeEventExec;

    public void create(String id){
        this.id = id;
        this.editor_type = "text-editor";
        userScope = new UserScope();
    }

//    public void setDsFieldInfo(){
//        if(ds_id!=null) {
//            CompDataSource ds = (CompDataSource) Modules.getInstance().get(ds_id, true);
//            dsField = ds.getField(ds_field_id);
//            lngDsField = ds.getField(lng_ds_field_id);
//            latDsField = ds.getField(lat_ds_field_id);
//            signDsField = ds.getField(sign_ds_field_id);
//            pdfDsField = ds.getField(pdf_ds_field_id);
//
//        }
//    }
}
