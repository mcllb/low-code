package tcdx.uap.service.entities;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import tcdx.uap.common.entity.page.TableDataInfo;
import tcdx.uap.constant.Constants;
import tcdx.uap.service.store.Modules;

import java.io.Serializable;
import java.util.*;
import java.util.stream.Collectors;

@JsonIgnoreProperties(ignoreUnknown = true)
@AllArgsConstructor
@NoArgsConstructor
public class CompDataSource  implements Serializable {
    public String id;
    public String view_id;
    public String name;
    public String data_type;
    public String table_id;         //当前表
    public String pri_table_id;     //接单信息主表
    public String data_sql;
    public String where_sql;
    public String order_sql;
    public String data_access_scope;
    public Boolean forModal; //如果用于窗口，则加载数据
    public Boolean enable_total;
    public Boolean has_pri_sub_receiver_info=false;
    public Integer page_size;
    public String limit_session_table_id;
    public List<CompDataSourceField> fields = new ArrayList<>();

    //在get_views时获取
    public boolean containTotal;
    public TableDataInfo data = null;

    public void onlyKeepIdName(){
        this.data_access_scope = null;
        this.table_id = null;
        this.data_type = null;
        this.where_sql = null;
        this.order_sql = null;
        this.data_sql = null;
        this.enable_total = null;
        this.has_pri_sub_receiver_info = false;
        this.page_size = null;
        this.limit_session_table_id = null;
        this.fields = null;
        this.pri_table_id = null;
        this.forModal = null;
    }


    public void onlyKeepFrontPageNeed(){
        this.order_sql = null;
        this.data_sql = null;
        this.enable_total = null;
        this.page_size = null;
        this.limit_session_table_id = null;
    }

    public void create(String id){
        this.id = id;
        this.data_type = "table";
        this.data_access_scope = "all";
        this.enable_total = true;
        fields = new ArrayList<>();
        CompDataSourceField field = new CompDataSourceField();
        field.id = "ds_total";
        field.field = "ds_total";
        field.field_type = "ds_total";
        CompDataSourceField field1 = new CompDataSourceField();
        field1.id = "ds_rows_length";
        field1.field = "ds_rows_length";
        field1.field_type = "ds_rows_length";
        fields.add(field);
        fields.add(field1);
    }

    public void getDetailsModules(){
        CompDataSource ds = (CompDataSource) Modules.getInstance().get(id, false);
        this.id = ds.id;
        this.page_size = ds.page_size;
        this.data_access_scope = ds.data_access_scope;
        this.table_id = ds.table_id;
        this.data_type = ds.data_type;
        this.fields = ds.fields;
    }

    public void setFields(){
        Table tbl = (Table) Modules.getInstance().get(table_id, false);
        for(CompDataSourceField f : fields){
            f.data_type = null;
            f.tableName = null;
            f.fieldName = null;
            f.rel_dict_id = null;
            if (Objects.equals(f.field_type, "defined_field")) {
                f.fieldName = f.field;
                if(tbl!=null){
                    f.tableName = tbl.name;
                }
            }
            else if (Objects.equals(f.field_type, "flow_field")) {
                List<Map> def = Constants.getDsFieldDefinition();
                List<Map> fds = def.stream().filter(o -> o.get("field").equals(f.field)).collect(Collectors.toList());
                if (fds.size() > 0) {
                    Map fd = fds.get(0);
                    f.fieldName = (String) fd.get("name");
                }
                if(tbl!=null){
                    f.tableName = tbl.name;
                }
            } else if (Objects.equals(f.field_type, "table_field")) {
                if(Objects.equals(f.table_id, table_id)) {
                    if (tbl != null) {
                        if(tbl!=null){
                            f.tableName = tbl.name;
                        }
                        TableCol tc = tbl.getCol(f.table_col_id);
                        if (tc != null) {
                            f.field = tc.field;
                            f.data_type = tc.data_type;
                            f.fieldName = tc.name;
                            f.rel_dict_id = tc.rel_dict_id;
                        }
                        else {
                            tc = tbl.getCol(f.field);
                            if (tc != null) {
                                f.field = tc.field;
                                f.data_type = tc.data_type;
                                f.fieldName = tc.name;
                                f.rel_dict_id = tc.rel_dict_id;
                            }
                        }
                    }
                }
                else{
                    Table ftbl = (Table) Modules.getInstance().get(f.table_id, false);
                    if(ftbl==null)
                        ftbl=tbl;
                    f.tableName = ftbl.name;
                    TableCol tc = ftbl.getCol(f.table_col_id);
                    if (tc != null) {
                        f.field = tc.field;
                        f.data_type = tc.data_type;
                        f.fieldName = tc.name;
                        f.rel_dict_id = tc.rel_dict_id;
                    }
                    else {
                        tc = ftbl.getCol(f.field);
                        if (tc != null) {
                            f.field = tc.field;
                            f.data_type = tc.data_type;
                            f.fieldName = tc.name;
                            f.rel_dict_id = tc.rel_dict_id;
                        }
                    }
                }
            }
        }
    }

    public CompDataSourceField getField(String field_id){
        if(field_id == null || field_id.isEmpty()){
            return null;
        }
        if (field_id.matches("\\d+")) { //兼容早期的数字id
            List<CompDataSourceField> fds = fields.stream().filter(f -> Objects.equals(f.id, field_id)).collect(Collectors.toList());
            if (fds.size() > 0) {
                return fds.get(0);
            }
        }
        else{
            List<CompDataSourceField> fds = fields.stream().filter(f -> Objects.equals(f.field, field_id)).collect(Collectors.toList());
            if (fds.size() > 0) {
                return fds.get(0);
            }
        }
        return null;
    }

}
