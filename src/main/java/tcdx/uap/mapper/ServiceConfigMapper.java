package tcdx.uap.mapper;

import org.apache.ibatis.annotations.Mapper;
import tcdx.uap.service.entities.TableColumnRelation;

import java.util.List;
import java.util.Map;

/**
 * 用户与角色关联表 数据层
 *
 * @author ruoyi
 */
@Mapper
public interface ServiceConfigMapper
{
    public List<Map> select_flowable_act_re_deployment_pagedata();
    public List<Map> select_taskdata(Map<String, Object> m);
    public List<Map> select_flow_todo_list(Map<String, Object> m);
    public List<Map> select_flow_done_list(Map<String, Object> m);
    public List<Map> select_flow_created_list(Map<String, Object> m);
    public List<Map> select_task_top_btns(Map<String, Object> m);
    public List<Map> get_role_list(Map<String, Object> m);
    public List<Map> select_task_col_btns(Map<String, Object> m);
    public int copy_to_log(Map<String, Object> m);
    public int set_delete_time(Map<String, Object> m);
    public List<Map> select_function(Map<String, Object> m);
    public int delete_old_done_task(Map<String, Object> m);
    public int insert_when_not_exists(Map<String, Object> m);
    public List<Map> select_next_assign_task(Map<String, Object> m);
    public int update_todo_tasks(Map<String, Object> m);
    public List<Map> get_inst_his(Map<String, Object> m);
    public List<Map> get_data_his(Map<String, Object> m);
    public List<Map> get_related_form_field(Map<String, Object> m);
    public List<Map> get_view_grid_cols(Map<String, Object> m);
    public List<Map> get_table_coulmn_relations(Map<String, Object> m);
    public List<Map> get_table_coulmn_by_relations(Map<String, Object> m);
    public int updateunCompletedTask(Map<String, Object> m);
    public List<Map> select_todo_counts(Map<String, Object> m);
    public List<TableColumnRelation> get_table_column_depent_relations();
    public int update_depend_new_record(Map<String, Object> m);
    public List<TableColumnRelation> getColumnsRelations();
    public int update_by_depend_col_new_value(Map<String, Object> m);
    public int update_by_depend_col_count(Map<String, Object> m);
    public int add_view_grid_col(Map<String, Object> m);
    public int delete_view_grid_col(Map<String, Object> m);
    public int delete_view_grid_flow_col(Map<String, Object> m);
    public List<Map> get_taskdata(Map<String, Object> m);
    public List<Map> get_table_col_lk_name(Map<String, Object> m);
    public List<Map> get_table_relations(Map<String, Object> m);
//    public List<Map> get_view_nodes(Map<String, Object> m);
//    public List<Map> get_operations_of_execs(Map<String, Object> m);
    public List<Map> get_parent_views_comps(Map<String, Object> m);
    public List<Map> get_group_views(Map<String, Object> m);
    public List<Map> get_sas_system_tables(Map<String, Object> m);
    public List<Map> get_prev_view(Map<String, Object> m);
    public List<Map> get_flow_btns(Map<String, Object> m);
    public List<Map> get_rel_group_user(Map<String, Object> m);
    public List<Map> get_flow_node_event(Map<String, Object> m);
    public List<Map> get_flow_edge_data_scope(Map<String, Object> m);
    public List<Map> get_flow_edge_event(Map<String, Object> m);
    public List<Map> get_table_flowed_cols(Map<String, Object> m);
    public List<Map> get_view_notice_scope(Map<String, Object> m);
    public List<Map> get_view_aggr_count_items(Map<String, Object> m);
    public List<Map> get_view_folder(Map<String, Object> m);
    public List<Map> get_view_group(Map<String, Object> m);
    public List<Map> get_table_group(Map<String, Object> m);
    public List<Map> get_all_enable_counting_views(Map<String, Object> m);
    public List<Map> get_comp_echarts_cfg(Map<String, Object> m);
    public List<Map> get_comp_gantt_echarts_cfg(Map<String, Object> m);
    public List<Map> get_view_parent_page(Map<String, Object> m);
    public List<Map> get_view_parent_folder(Map<String, Object> m);
    public List<Map> get_ds_tables_of_parent(Map<String, Object> m);
    public List<Map> get_btn_with_views(Map<String, Object> m);
    public List<Map> get_view_for_module(Map<String, Object> m);
    int update_by_depend_col_counts(Map map);
    int update_by_depend_col_distinct(Map map);
    int update_by_depend_col_newvalue(Map map);
    int update_by_depend_col_sum(Map map);
    int insert_by_depend_col_newvalue(Map map);
    int delete_datasource_col(Map map);
    int set_not_this_datasource_field_false(Map map);
}
