package tcdx.uap.service.entities;

import tcdx.uap.common.utils.Lutils;

import java.util.*;

public class TableColumnRelation {
    public Integer id;
    public Integer table_id;
    public String table_name;
    public String update_column;
    public Integer depend_table_id;
    public String depend_table_name;
    public String depend_table_column;
    public String update_type;
    public String match_column1;
    public String depend_table_match_column1;
    public String match_column2;
    public String depend_table_match_column2;
    public String match_column3;
    public String depend_table_match_column3;
    public String compare_column1;
    public String compare_type1;
    public String compare_value1;
    public String depent_table_compare_column1;
    public String depent_table_compare_type1;
    public String depent_table_compare_value1;

    public TableColumnRelation copy(){
        TableColumnRelation copy = new TableColumnRelation();
        copy.id = id;
        copy.table_id = table_id;
        copy.table_name = table_name;
        copy.update_column = update_column;
        copy.depend_table_id = depend_table_id;
        copy.depend_table_name = depend_table_name;
        copy.depend_table_column = depend_table_column;
        copy.update_type = update_type;
        copy.match_column1 = match_column1;
        copy.depend_table_match_column1 = depend_table_match_column1;
        copy.match_column2 = match_column2;
        copy.depend_table_match_column2 = depend_table_match_column2;
        copy.match_column3 = match_column3;
        copy.depend_table_match_column3 = depend_table_match_column3;
        copy.compare_column1 = compare_column1;
        copy.compare_type1 = compare_type1;
        copy.compare_value1 = compare_value1;
        copy.depent_table_compare_column1 = depent_table_compare_column1;
        copy.depent_table_compare_type1 = depent_table_compare_type1;
        copy.depent_table_compare_value1 = depent_table_compare_value1;
        return copy;
    }

    public List<String> formWillTriggerThisTableColumnUpdate(String table_name, List<String> updateColumns){
        List<String> cols = new ArrayList<>();
        if( table_name.equals(table_name) && depend_table_name.equals(table_name)){
            if(Lutils.nvl(depend_table_column,"").length()>0 && updateColumns.contains(depend_table_column)){
                cols.add(depend_table_column);
            }
             if(Lutils.nvl(depend_table_match_column1,"").length()>0 && updateColumns.contains(depend_table_match_column1)){
                 cols.add(depend_table_match_column1);
             }
             if(Lutils.nvl(depend_table_match_column2,"").length()>0 && updateColumns.contains(depend_table_match_column2)){
                 cols.add(depend_table_match_column2);
             }
             if(Lutils.nvl(depend_table_match_column3,"").length()>0 && updateColumns.contains(depend_table_match_column3)){
                 cols.add(depend_table_match_column3);
             }
             if(Lutils.nvl(match_column1,"").length()>0 && updateColumns.contains(match_column1)){
                 cols.add(match_column1);
             }
             if(Lutils.nvl(match_column2,"").length()>0 && updateColumns.contains(match_column2)){
                 cols.add(match_column2);
             }
             if(Lutils.nvl(match_column3,"").length()>0 && updateColumns.contains(match_column3)){
                 cols.add(match_column3);
             }
        }
        return cols;
    }

    public List<String> getMatchFields(){
        List l = new ArrayList();
        if(Lutils.nvl(match_column1,"").length()>0)
            l.add(match_column1);
        if(Lutils.nvl(match_column2,"").length()>0)
            l.add(match_column2);
        if(Lutils.nvl(match_column3,"").length()>0)
            l.add(match_column3);
        return l;
    }

    public List<String> getDependFields(){
        List l = new ArrayList();
        l.add(depend_table_column);
        if(Lutils.nvl(match_column1,"").length()>0)
            l.add(depend_table_match_column1);
        if(Lutils.nvl(match_column2,"").length()>0)
            l.add(depend_table_match_column2);
        if(Lutils.nvl(match_column3,"").length()>0)
            l.add(depend_table_match_column3);
        return l;
    }

    public List<String> getDependMatchFields(){
        List l = new ArrayList();
        if(Lutils.nvl(match_column1,"").length()>0)
            l.add(depend_table_match_column1);
        if(Lutils.nvl(match_column2,"").length()>0)
            l.add(depend_table_match_column2);
        if(Lutils.nvl(match_column3,"").length()>0)
            l.add(depend_table_match_column3);
        return l;
    }

    //form的字段是否是该规则中的，触发依赖条件字段
    public boolean formWillTriggerUpperTableColumnUpdate(String depend_table_name, List<String> updateColumns){
        if( (!this.depend_table_name.equals(depend_table_name)) && (
                (Lutils.nvl(depend_table_column,"").length()>0 && updateColumns.contains(depend_table_column))
                        || (Lutils.nvl(depend_table_match_column1,"").length()>0 && updateColumns.contains(depend_table_match_column1))
                        || (Lutils.nvl(depend_table_match_column2,"").length()>0 && updateColumns.contains(depend_table_match_column2))
                        || (Lutils.nvl(depend_table_match_column3,"").length()>0 && updateColumns.contains(depend_table_match_column3))
        ))
            return true;
        else
            return false;
    }

}
