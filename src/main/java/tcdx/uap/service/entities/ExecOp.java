package tcdx.uap.service.entities;

//import com.aspose.cad.internal.P.M;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import tcdx.uap.service.store.Modules;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@JsonIgnoreProperties(ignoreUnknown = true)
@AllArgsConstructor
@NoArgsConstructor
public class ExecOp  implements Serializable {
    public static String OP_TYPE_TRIGGER_COMPLETE = "complete";
    public static String OP_ILLEGAL_PROCESSING_STOP = "stop";
    public static String OP_ILLEGAL_PROCESSING_SKIP = "skip";
    public static String OP_ILLEGAL_PROCESSING_SK = "skip";

    public String id;
    public String op_obj_type;
    public String op_type;
    public String view_id;
    public String from_view_id;
    public String ds_id;
    public String ds_left_join_field_id;
    public String from_ds_id;
    public String from_ds_left_join_field_id;
    public String from_ds_field_id;  //
    public String flow_edge_id;
    public String table_id;
    public String from_ds_row;       //
    public String trans_sql;              //
    public String code_name;  // 脚本类名
    public String confirm_msg;            //
    public List<Map<String,String>> scripts;            //[{script:'',msg:''}]
    public String illegal_processing;     //
    public String defined_session_list;   //skip stop
    public String stop_sql;               //skip stop
    public Boolean then_stop=false;
    public Boolean left_join=false;
    public String left_join_key;
    public List<ExecOpAction> opActions = new ArrayList<>();
    //get时获取
    public String viewType;
    public String viewName;
    public String viewIcon;
    public String fromViewType;
    public String fromViewName;
    public String tableName;
    public String flowEdgeName;
    public FlowEdge edge;
    public List<ExecOpGoto> gotoLs = new ArrayList();

    public void setInfo(){
        //有一些id没转换成功的，重新转换一下
        if(view_id != null && !view_id.startsWith("view")){
            view_id = "view" + view_id.toLowerCase().replace("view","");
        }
        if(flow_edge_id != null && !flow_edge_id.startsWith("edge")){
            flow_edge_id = "edge" + flow_edge_id.toLowerCase().replace("edge","");
        }
        if(table_id != null && !table_id.startsWith("table")){
            table_id = "table" + table_id.toLowerCase().replace("table","");
        }
        if(from_view_id != null && !from_view_id.startsWith("view")){
            from_view_id = "view"+from_view_id.toLowerCase().replace("view","");
        }
//        if(from_ds_id!=null && !from_ds_id.startsWith("CompDataSource")){
//            from_ds_id = "CompDataSource"+from_ds_id.replace("CompDataSource","");
//        }
//        if(from_ds_field_id!=null && !from_ds_field_id.startsWith("CompDataSourceField")){
//            from_ds_field_id = "CompDataSourceField"+from_ds_field_id.replace("CompDataSourceField","");
//        }
//        if(from_ds_row!=null && !from_ds_row.startsWith("view")){
//            from_ds_row = "view"+from_ds_row.toLowerCase().replace("view","");
//        }
        if(op_type.equals("complete")){
            Table tbl = (Table) Modules.getInstance().get(table_id, false);
            if(tbl!=null)
               edge = tbl.getEdge(flow_edge_id);
        }
        if(view_id!=null){
            View v = (View) Modules.getInstance().get(view_id, false);
            if(v!=null) {
                viewType = v.view_type;
                viewName = v.name;
                viewIcon = v.icon;
            }
            else{
//                System.out.println("Exception:  view_id is null ------------------>"+view_id);
            }
        }
    }

    //操作是否包含某个动作 actionType fill-foreign、where-limited-ids等等
    public boolean hasActionType(String opActionType){
        return opActions.stream()
                .filter(s-> Objects.equals(s.session_type, opActionType))
                .collect(Collectors.toList()).size()>0;
    }

    //操作是否包含某个动作 actionType fill-foreign、where-limited-ids等等
    public boolean hasFillForeignAction(){
        return opActions.stream().anyMatch(s -> Objects.equals(s.session_type, ExecContext.ENV_CFG_FILL_FOREIGN));
    }

    public boolean hasFillCountersignAction(){
        return opActions.stream().anyMatch(s -> Objects.equals(s.session_type, ExecContext.ENV_CFG_FILL_COUNTERSIGN));
    }

    public boolean hasWhereLimitedIdsAction(){
        return opActions.stream().anyMatch(s -> Objects.equals(s.session_type, ExecContext.ENV_CFG_WHERE_LIMIT_IDS));
    }

}
