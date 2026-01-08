package tcdx.uap.service.entities;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import tcdx.uap.common.utils.Lutils;

import java.util.*;

public class ColumnRuleOperationStore {

    public LinkedHashMap<String, List<ColumnRuleOperation>> columnRuleOperationMap = new LinkedHashMap<>();
    public LinkedHashMap<String, Boolean> columnRuleOperationFinished = new LinkedHashMap<>();

    public ColumnRuleOperation getColumnOperation(String table_name, String column_name) {
        List<ColumnRuleOperation> columnRuleOperations = columnRuleOperationMap.get(table_name);
        for (ColumnRuleOperation columnRuleOperation : columnRuleOperations) {
            if(columnRuleOperation.column.equals(column_name))
                return columnRuleOperation;
        }
        return null;
    }

    public String getNextTableName(){
        // 使用迭代器获取第一个元素
        Iterator<Map.Entry<String, List<ColumnRuleOperation>>> iterator = columnRuleOperationMap.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, List<ColumnRuleOperation>> firstEntry = iterator.next();
            return firstEntry.getKey();
        }
        return null;
    }

    public void removeTableOperations(String tableName){
        columnRuleOperationMap.remove(tableName);
    }

    public void addTableColumnRule(String table_name, ColumnRuleOperation columnRuleOperation){
        if(!columnRuleOperationMap.containsKey(table_name)){
            List<ColumnRuleOperation> tmp = new ArrayList<>();
            tmp.add(columnRuleOperation);
            columnRuleOperationMap.put(table_name, tmp);
        }
        else{
            List<ColumnRuleOperation> tmp  = columnRuleOperationMap.get(table_name);
            for (ColumnRuleOperation temp: tmp){
                if(temp.column.equals(columnRuleOperation)){
                    temp.orWhereOfRowsList.addAll(columnRuleOperation.orWhereOfRowsList);
                }
            }
        }
    }

    public int getTableColumnSize(String table_name){
        if(!columnRuleOperationMap.containsKey(table_name)){
            return columnRuleOperationMap.get(table_name).size();
        }
        return -1;
    }

    public List<ColumnRuleOperation> getOperationList(String table_name){
        return columnRuleOperationMap.get(table_name);
    }

}
