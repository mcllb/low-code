package tcdx.uap.mapper;

import org.apache.ibatis.annotations.Mapper;

import java.util.List;
import java.util.Map;

/**
 * 用户与角色关联表 数据层
 * 
 * @author ruoyi
 */
@Mapper
public interface BusinessMapper
{
    public List<Map> getViewRecrusive(Map<String, Object> m);
    public List<Map> getViewTree(Map<String, Object> m);
    public List<Map> getParentViewTree(Map<String, Object> m);
    public List<Map> getManageViewTree(Map<String, Object> m);
    public List<Map> getViewsInParentTreeNoJson(Map<String, Object> m);
    public List<Map> getViewsParents(Map<String, Object> m);
    public List<Map> getViewsAndParentsNoJSON(Map<String, Object> m);
    public List<Map> getTableRecrusive(Map<String, Object> m);
    public List<Map> getViewRecrusiveByParent(Map<String, Object> m);
    public List<Map> get_table_recurive_relations(Map<String, Object> m);
    public List<Map> get_grid_data(Map<String, Object> m);
    public List<Map> get_distinct_grid_data(Map<String, Object> m);
    public List<Map> get_grid_data_count(Map<String, Object> m);
    public List<Map> get_grid_data_total(Map<String, Object> m);
    public List<Map> get_defined_sql_count(Map<String, Object> m);
    public List<Map> get_defined_sql_total(Map<String, Object> m);
    public List<Map> get_btn_ops(Map<String, Object> m);
    public List<Map> get_form_fields(Map<String, Object> m);
    public List<Map> get_form_fields_related_flows(Map<String, Object> m);
    public List<Map> get_depend_col_roles(Map<String, Object> m);
    public List<Map> get_form_fields1(Map<String, Object> m);
    public List<Map> get_dict_receivers(Map<String, Object> m);
    public int set_dict_receivers(Map<String, Object> m);
    public List<Map> get_flow_edge_defined_receivers(Map<String, Object> m);
    public int delete_old_flow(Map<String, Object> m);
    public int add_new_flow(Map<String, Object> m);
    public int add_table_log(Map<String, Object> m);
    public int set_log_finished_time(Map<String, Object> m);
    public int delete_notice(Map<String, Object> m);
    public int add_notice(Map<String, Object> m);
    public List<Map> get_gate_edges(Map<String, Object> m);
    public List<Map> get_undeleted_flow_node(Map<String, Object> m);
    public List<Map> get_undeleted_flow_edge(Map<String, Object> m);
    public List<Map> get_undeleted_update_sql_edge(Map<String, Object> m);
    public List<Map> get_grid_flow_btns(Map<String, Object> m);
    public List<Map> get_recur_trans(Map<String, Object> m);
    public Object set_table_flow_rows_by_edge(Map<String, Object> m);
    public List<Map> get_grid_view_info(Map<String, Object> m);
    int get_root_id(Map<String, Object> m);
    List<Map> get_all_id_by_root_id(Map<String, Object> m);
    List<Map> get_all_view_flow_config(Map<String, Object> m);
    List<Map> get_value_renders_of_objs(Map<String, Object> m);
    List<Map> get_value_editors_of_objs(Map<String, Object> m);
    List<Map> get_user_selectors_of_views(Map<String, Object> m);
    List<Map> get_all_user_selectors_of_views(Map<String, Object> m);
    List<Map> get_table_datasource_fields(Map<String, Object> m);
    List<Map> get_defined_datasuorce_fields(Map<String, Object> m);
    List<Map> get_datasource_info(Map<String, Object> m);
    List<Map> get_ds_fields_attr(Map<String, Object> m);
    List<Map> get_comp_test_of_obj_type(Map<String, Object> m);
    List<Map> get_comp_map_point(Map<String, Object> m);
    List<Map> get_execs_of_obj_type(Map<String, Object> m);
//    List<Map> get_default_flow_of_node(Map<String, Object> m);
    List<Map> get_comp_report_forms_of_obj_type(Map<String, Object> m);
//    List<Map> get_flow_node_info(Map<String, Object> m);
    List<Map> get_sub_flow_counts(Map<String, Object> m);
    List<Map> get_sub_flow_handle_info(Map<String, Object> m);
    List<Map> get_sub_flow_event_of_pri(Map<String, Object> m);
    List<Map> get_comp_log_cfg(Map<String, Object> m);
    List<Map> get_this_log_table_data(Map<String, Object> m);
    List<Map> get_huiqian_log_table_data(Map<String, Object> m);
    List<Map> get_pri_table_flow_info(Map<String, Object> m);
    List<Map> get_pri_tables(Map<String, Object> m);
    List<Map> get_pri_cur_receive_info(Map<String, Object> m);
    List<Map> get_pri_rel_sub_receive_info(Map<String, Object> m);
//    List<Map> get_sub_tables_for_get_rows(Map<String, Object> m);
    List<Map> get_this_log_table_detail(Map<String, Object> m);
    List<Map> get_log_view_id_by_action_id(Map<String, Object> m);
    List<Map> selectViewTreeList(Map<String, Object> m);
    List<Map> get_col_fieds_by_editor_obj_ids(Map<String, Object> m);
    List<Integer> getRelatedUsers(Map<String, Object> m);
    List<Map> getLogTableInitData(Map<String, Object> m);
}
