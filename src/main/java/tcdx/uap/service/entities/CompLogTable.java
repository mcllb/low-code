package tcdx.uap.service.entities;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import org.docx4j.org.apache.xpath.operations.Bool;
import tcdx.uap.constant.Constants;
import tcdx.uap.service.store.Modules;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
@AllArgsConstructor
@NoArgsConstructor
public class CompLogTable  implements Serializable {
    public String id;
    public Integer height;
    public Boolean complete;
    public Boolean update;
    public String ds_id;
    public String table_id;
    public Table table;
    public List<Map> initData;
    public CompDataSource compDataSource;
    public String ds_id_huiqian;
    public CompDataSource compDataSourceHuiqian;


    public void create(String id){
        this.id = id;
    }
}
