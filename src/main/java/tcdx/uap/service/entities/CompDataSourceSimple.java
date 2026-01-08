package tcdx.uap.service.entities;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.*;

@JsonIgnoreProperties(ignoreUnknown = true)
@AllArgsConstructor
@NoArgsConstructor
public class CompDataSourceSimple implements Serializable {
    public String id;
    public String view_id;
    public String name;
    public String table_id;
    public String data_type;
    public String data_access_scope;
    public Integer page_size;
    public boolean containTotal;
    public Map data = null;
    public Object this_ = null;
    public List<CompDataSourceField> fields = null;

    public void setInfo(String id,String name) {
        this.name = name;
        this.id = id;
    }

    public void setInfo(CompDataSource ds) {
        this.id = ds.id;
        this.page_size = ds.page_size;
        this.data_access_scope = ds.data_access_scope;
        this.table_id = ds.table_id;
        this.data_type = ds.data_type;
        this.fields = ds.fields;
    }

    public void setDsFields(CompDataSource ds) {
        this.fields = ds.fields;
    }

    public void setNullKeepIdName(){
        this.page_size = null;
        this.data_access_scope = null;
        this.table_id = null;
        this.data_type = null;
        this.fields = null;
    }
}
