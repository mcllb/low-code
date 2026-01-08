package tcdx.uap.service.entities;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import tcdx.uap.common.entity.page.TableDataInfo;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
@AllArgsConstructor
@NoArgsConstructor
public class View implements Serializable {
    public String id;
    public String root_id;
    public String parent_id;
    public String name;
    public String icon;
    public String view_type;
    public Integer width;
    public Integer height;
    public String comp_name;
    public String display_style="none";
    public Boolean show_title;
    public String item_border_css;
    public String panel_title_css;
    public String panel_body_css;
    public Integer col_span;
    public Integer row_span;
    public Boolean tr_end;
    public Integer gutter;
    public Boolean is_show;
    public String show_in_session_nodes; // 环节显示
    public String tab_style_class;
    public String layout_border_css;
    public Integer col_counts;
    public String chd_l1_odd_style;
    public String chd_l2_even_style;
    public Integer mobile_col_num;
    public Integer form_col_num;
    public String form_label_td_style;
    public String form_content_td_style;
    public String comp_id; //当view是CompDataSource时，comp_id 与 ds_id 相同
    public List<Exec> viewBtns = new ArrayList<>();
    public List<Exec> viewTitleBtns = new ArrayList<>();
    public List<View> children = new ArrayList<>();
    public List<List<View>> formTable = null;
    //仅仅存储id和name，其他不存储。其他数据在getView时获取
    public List<CompDataSource> dsList = new ArrayList<>();
    public Map<String, TableDataInfo> tableDataInfo;

    //get时获取
    public Object comp;

    public void create(){
        display_style = "none";
        viewBtns = new ArrayList<>();
        viewTitleBtns = new ArrayList<>();
        dsList = new ArrayList<>();
    }

}
