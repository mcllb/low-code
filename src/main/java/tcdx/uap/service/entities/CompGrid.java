package tcdx.uap.service.entities;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import tcdx.uap.common.entity.page.TableDataInfo;
import tcdx.uap.common.utils.Lutils;
import tcdx.uap.service.store.Modules;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
@AllArgsConstructor
@NoArgsConstructor
public class CompGrid  implements Serializable {
    public String id;
    public String paging_mode;
    public String border_type;
    public Boolean show_check_col;
    public Boolean edit_mode;
    public String cell_class_name;
    public String content_layout;
    public Boolean show_search_control;
    /** 分页大小 */
    public Integer page_size;
    /** 是否显示标题 */
    public Boolean show_header; // 显示标题
    public Boolean round;  //圆角
    public Boolean enable_row_click;
    public Integer height;
    public String grid_size;
    public List<Exec> topBtns = new ArrayList<>();
    public List<CompGridCol> searchGridCols = new ArrayList<>();
    public List<CompGridCol> gridCols = new ArrayList<>();
    public String ds_id;
    //深度获取时get
    public CompDataSource compDataSource;
    //深度获取时get
    public void create(String id){
        this.id = id;
        this.show_search_control = true;
        this.paging_mode = "full";
        this.show_check_col = false;
        this.edit_mode = false;
        this.content_layout = "grid";
        this.page_size = 10;
        this.round = true;
        this.grid_size = "small";
        this.show_header = true;
        this.cell_class_name = "el_table_cell_padding_8";
        topBtns = new ArrayList<>();
        gridCols = new ArrayList<>();
        searchGridCols = new ArrayList<>();
    }

    public void setDsFieldInfo(){
        for(CompGridCol col : gridCols){
            col.setDsFieldInfo();
        }
    }

    //get
    public void getFieldColInfo(){
        for(CompGridCol col : gridCols){
            CompDataSource ds = (CompDataSource) Modules.getInstance().get(ds_id,true);
            if(ds==null || col.compValueRender.ds_field_id == null) continue;
            CompDataSourceField f = ds.getField(col.compValueRender.ds_field_id);
            if(f!=null) {
                if (f.field_type.equals("table_field")) {
                    Table tbl = (Table) Modules.getInstance().get(ds.table_id, true);
                    TableCol tc = tbl.getCol(f.table_col_id);
                    col.tableCol = tc;
                    if(tc!=null) {
                        col.field = tc.field;
                        col.table_id = tbl.id;
                    }
                } else if (f.field_type.equals("flow_field")) {
                    col.table_id = f.table_id;
                    col.field = f.field;
//                    col.flow_edge_id = f.flow_edge_id;
                } else if (f.field_type.equals("defined_field")) {
                    if (f.table_col_id != null) {
                        Table tbl = (Table) Modules.getInstance().get(ds.table_id, true);
                        TableCol tc1 = tbl.getCol(f.table_col_id);
                        col.tableCol = tc1;
                    }
                    col.table_id = f.table_id;
                    col.field = f.field;
                }
            }
        }
    }
}
