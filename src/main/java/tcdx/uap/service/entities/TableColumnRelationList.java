package tcdx.uap.service.entities;

import tcdx.uap.mapper.ServiceConfigMapper;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;


public class TableColumnRelationList {

    private static List<TableColumnRelation> dict = null;
    private TableColumnRelationList(){}

    public static TableColumnRelationList tableColumnRelationDict = null;

    public static synchronized TableColumnRelationList getInstance(ServiceConfigMapper serviceConfigMapper){
       if(tableColumnRelationDict == null){
           tableColumnRelationDict = new TableColumnRelationList();
           dict = serviceConfigMapper.getColumnsRelations();
       }
        return tableColumnRelationDict;
    }

    //获取表关联自身的关系
    public List<TableColumnRelation> getTriggerThisTableRelation(String table_name, List<String> updateColumns){
        List<TableColumnRelation> l = new ArrayList<>();
        for(TableColumnRelation tr : dict){
            List<String> relatedRuleColumn = tr.formWillTriggerThisTableColumnUpdate(table_name, updateColumns);
            if(relatedRuleColumn != null && relatedRuleColumn.size() > 0){
                TableColumnRelation rs = tr.copy();
                l.add(rs);
            }
        }
        return l;
    }

    //获取表关上级的关系
    public List<TableColumnRelation> getTriggerUpperTableRelation(String depend_table_name, List<String> updateColumns){
        return dict.stream().filter(o->o.formWillTriggerUpperTableColumnUpdate(depend_table_name, updateColumns)).collect(Collectors.toList());
    }


}
