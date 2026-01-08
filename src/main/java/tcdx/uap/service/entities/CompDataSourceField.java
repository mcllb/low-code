package tcdx.uap.service.entities;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import tcdx.uap.common.utils.Lutils;
import tcdx.uap.constant.Constants;
import tcdx.uap.service.store.Modules;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@JsonIgnoreProperties(ignoreUnknown = true)
@AllArgsConstructor
@NoArgsConstructor
@Data
public class CompDataSourceField  implements Serializable {
    public String id; //同  field
    public String field;    //这个字段对应数据表的字段，数据表字段发生变化后，需要手工修改数据源的字段；
    public String ds_id;
    public String field_type;
    public String table_id;
    public String table_col_id;
//    public String flow_edge_id;
//    public String flow_field;
//    public String defined_field;
    //get时获取
    public String fieldName;  //对应的中文名
    public String tableName;
    public String data_type;   //table_col 列的数据类型
    public Integer rel_dict_id; //table_col 的字典id
    public String search_type;
    public String editor_id;   //界面中绑定的编辑器id

    private void getSearchType(TableCol tc) {
        String columnType = tc.data_type;
        if(columnType.equals("datetime")){
            search_type="timestamp";
        }else if(columnType.equals("varchar")){
            search_type="varchar";
        }else if (columnType.equals("dict")){
            search_type="dict";
        }else if (columnType.equals("integer") || columnType.equals("numeric")){
            search_type="integer";
        }
    }

    public CompDataSourceField createDefined( String defined_field, String ds_id ){
        this.ds_id = ds_id;
        this.field_type = "defined_field";
        this.id = defined_field;
        this.field = defined_field;
//        this.defined_field = defined_field;
        this.fieldName = defined_field;
        //切分t123_field->  table123   field
        if(defined_field.matches("t\\d+_.*")){
            Pattern pattern = Pattern.compile("t\\d+_");
            Matcher matcher = pattern.matcher(defined_field);
            if (matcher.find()) {
                String match = matcher.group();
                //因为使用了缩写，这里要使用缩写别名还原实际table_id
                String table_id = match.replace("t", "table").replace("_", "");
                Table table = (Table) Modules.getInstance().get(table_id,false);
                if(table!=null) {
                    for (int i = 0; i < table.cols.size(); i++) {
                        if (table.cols.get(i).field.equals(defined_field.replace(match,""))) {
                            this.table_col_id = table.cols.get(i).id;
                        }
                    }
                    this.table_id = table_id;
                }
            } else {
               //未找到表名
            }
        }
        return this;
    }

}
