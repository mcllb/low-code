package tcdx.uap.service.entities;

import tcdx.uap.common.utils.Lutils;
import tcdx.uap.common.utils.SqlUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class ColumnRuleOperation {
    public String column = "";
    public String tableName = "";
    public List<Integer> inIds = new ArrayList<>();
    public Object value = "";
    public boolean isColumnUpdated = false;
    public boolean isWhereSupplementByNewValue = false;
    public String updateType; // 通过id匹配
    public TableColumnRelation updateRelation = null;
    //这三个条件用来求当前表的ids
    public String dependTableName = "";
    //保存下级触发的行，用来获取更新后的新值，补充要更新的ids
    public List<Integer> dependIds = new ArrayList<>();
    public List<Map<String,Object>> orWhereOfRowsList = new ArrayList<>();
    public Map getWhereCause(){
        List<Map> orList = new ArrayList<>();
        for(Map<String,Object> m: orWhereOfRowsList){
            List <Map> andList = new ArrayList<>();
            for(Map.Entry<String,Object> e: m.entrySet()){
                andList.add(SqlUtil.eq(e.getKey(), e.getValue()));
            }
            orList.add(SqlUtil.and(Lutils.MapListToArray(andList)));
        }
        return SqlUtil.or(Lutils.MapListToArray(orList));
    }


    public void addInIds(List <Integer> addList){
        for(Integer o : addList){
            if(!inIds.contains(o)){
                inIds.add(o);
            }
        }
    }


    public ColumnRuleOperation(String tableName, String update_column, String update_type,
                               Object form_value, TableColumnRelation relation, String dependTableName){
        this.tableName = tableName;
        this.column=update_column;
        this.value=form_value;
        this.updateType=update_type;
        this.updateRelation = relation;
        this.dependTableName = dependTableName;
    }

    public Map genEqualMap(String match_from_datatable_column,String match_datatable_column,Object from_value,
                           String match_from_datatable_column2,String match_datatable_column2,Object from_value2){
        Map equalMap = new HashMap();
        if(Lutils.nvl(match_datatable_column,"").length()>0&&Lutils.nvl(match_from_datatable_column,"").length()>0){
            equalMap.put(match_datatable_column, from_value);
        }
        if(Lutils.nvl(match_datatable_column2,"").length()>0&&Lutils.nvl(match_from_datatable_column2,"").length()>0){
            equalMap.put(match_datatable_column2, from_value2);
        }
        return equalMap;
    }

    public Map getMybatisMap(){
        return Lutils.genMap("table_name", this.tableName,
                "update_column", this.column,
                "depend_table_name", dependTableName,
                "inIds", inIds,
                "obj_c1", this.getWhereCause(),
                "relation", updateRelation);
    }


}