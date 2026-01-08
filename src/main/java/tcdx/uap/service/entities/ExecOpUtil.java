package tcdx.uap.service.entities;

import com.alibaba.fastjson2.JSON;
import tcdx.uap.common.utils.Lutils;
import tcdx.uap.service.BaseDBService;
import tcdx.uap.service.store.Modules;
import java.util.*;
import java.util.stream.Collectors;

public class ExecOpUtil {

    /**
     * op_db_type 操作的类型：submit/insert/update/delete
     * */
    public static Map GenOpSessionItem(String op_id, String op_db_type,String before_or_after, String table_id, int id_,
                                       String node_, Map row) {
        return Lutils.genMap("op_id", op_id, "op_db_type", op_db_type, "before_or_after",before_or_after, "table_id", table_id, "id_", id_ ,
                "node_", node_,  "row", row
        );
    }

    //判断 当前动作是否满足执行条件
    public static boolean IsOpCanDoNodes(List<Map> sessionContextList, Map op){
        //提交的node存在before_node_中
        List sessionNodes = sessionContextList.stream()
                .filter(o->o.get("before_node_")!=null)
                .map(o->o.get("before_node_")).collect(Collectors.toList());
        List opLimitNodes = op.get("op_exec_in_session_nodes")!=null?(List)JSON.parse(op.get("op_exec_in_session_nodes").toString()):null;
        if(opLimitNodes == null)
            return true;
        if((sessionNodes==null||sessionNodes.isEmpty()) && opLimitNodes.size()>0)
            return false;
        for(Object opNode : opLimitNodes){
            if(sessionNodes.contains(opNode)){
                return true;
            }
        }
        return false;
    }

    /*
    * 从session中，根据op，获取表id
    * [
    *   {
    *       session_type: 'fill-foreign',
    *       table_id: 123,
    *       id_: 1
    *   }...]
    * */


//    /*
//     * 将更新、新增、完成的行，提取id_、node_等信息，到会话中，便于后期提取。
//     * z_table_id 当前表的table_id
//     * beforeRows 更新前的整行数据
//     * afterRows 更新后的整行数据，包含id_
//     * */
//    public static void ExtractTableRowIdsToSession(ExecContext execContext, String op_db_type, String op_id, String table_id,
//                                                   List<Map> beforeRows, List<Map> afterRows){
//        if(beforeRows!=null && beforeRows.size()>0) {
//            for (Map beforeRow : beforeRows) {
//                execContext.addContext(GenOpSessionItem( op_id, op_db_type, "before", table_id, (Integer)beforeRow.get("id_"),
//                        (String)beforeRow.get("node_"), beforeRow));
//            }
//        }
//        if(afterRows!=null && afterRows.size()>0) {
//            for (Map afterRow : afterRows) {
//                execContext.addContext((GenOpSessionItem( op_id, op_db_type,"after",  table_id, (Integer)afterRow.get("id_"),
//                         (String)afterRow.get("node_"), afterRow)));
//            }
//        }
//        //处理外键，从行信息中提取z_table123_id
//        if(beforeRows!=null && !beforeRows.isEmpty()) {
//            //遍历行，添加外联表的行id
//            List<String> z_table_fields = new ArrayList<>();
//            Map<String, Object> firstBeforeRow = beforeRows.get(0);
//            //从行中选择foreign key
//            for (String field : firstBeforeRow.keySet()) {
//                if (field.startsWith("z_table")) {
//                    z_table_fields.add(field);
//                }
//            }
//            //添加更新的外键
//            for(String f: z_table_fields){
//                String foreign_table_id = f.replace("z_","").replace("_id", "");
//                for(Map beforeRow: beforeRows) {
//                    if(beforeRow.get(f)!=null) {
//                        execContext.addContext((GenOpSessionItem(op_id, "foreign-update", "before",
//                                foreign_table_id, (Integer) beforeRow.get(f), null, beforeRow));
//                    }
//                }
//            }
//        }
//        //处理外键，从行信息中提取z_table123_id
//        if(afterRows!=null && afterRows.size()>0) {
//            //遍历行，添加外联表的行id
//            List<String> z_table_fields = new ArrayList<>();
//            Map<String, Object> firstAfterRow = afterRows.get(0);
//            //从行中选择foreign key
//            for (String field : firstAfterRow.keySet()) {
//                if (field.startsWith("z_table")) {
//                    z_table_fields.add(field);
//                }
//            }
//            //添加更新的外键
//            for(String f: z_table_fields){
//                String foreign_table_id = f.replace("z_","").replace("_id", "");
//                List ids1 = afterRows.stream().filter(o->o.get(f)!=null).map(o->o.get(f)).collect(Collectors.toList());
//                for(Map afterRow: afterRows) {
//                    if(afterRow.get(f)!=null) {
//                        execContext.addContext((GenOpSessionItem(op_id, "foreign-update",
//                                "after", foreign_table_id, (Integer) afterRow.get(f), null, afterRow));
//                    }
//                }
//            }
//        }
//    }

    //添加取session中的外联id到行数据。
    public static void FillForeignKeyIntoRows(List<Map> rows, ExecOp op, ExecContext execContext){
        if(!op.hasFillForeignAction())
            return;
        if(op.table_id == null && op.ds_id != null){
            CompDataSource ds = (CompDataSource) Modules.getInstance().get(op.ds_id,false);
            op.table_id = ds.table_id;
        }
        Table tbl = (Table) Modules.getInstance().get(op.table_id, false);
        Map<String,List> foreignTableIds = execContext.getFillForeignTableIds(op);
        //将外联主表的id填充到子表的row中
        for(String priTblId:tbl.priTableIds){
            List ids = foreignTableIds.get(priTblId);
            if(ids!=null && !ids.isEmpty()){
                for(Map row:rows){
                    row.put( "z_"+priTblId+"_id", ids.get(0));
                }
            }
        }
    }

    //添加取session中的外联id到行数据。
    //天翼云眼项目经理。
    public static void FillCountersignIntoRows(List<Map> sub_rows, ExecOp op, ExecContext execContext, BaseDBService db){
        if(!op.hasFillCountersignAction() || sub_rows.isEmpty())
            return;
        List<Map> counterSignContexts = execContext.getEnvCfgMatchedRows(op, ExecContext.ENV_CFG_FILL_COUNTERSIGN);
        //将外联主表的id填充到子表的row中
        for(Map pri_row: counterSignContexts){
            String pri_table_id = (String)pri_row.get("__table_id__");
            Object pri_id_ = pri_row.get("id_"); //行id
            Object node_ = pri_row.get("node_");
            if(node_ == null){
                List<Map> maps = db.selectEq("z_" + pri_table_id, Lutils.genMap("id_", pri_id_));
                node_ = maps.get(0).get("node_").toString();
            }
            //从子流程中，找到当前主流程环节最新的进入次数
            Integer max = db.selectMaxColEq("z_" + op.table_id,
                    "pri_tbl_node_enter_times_",
                    Lutils.genMap(
                            "z_"+pri_table_id+"_id", pri_id_,
                            "pri_tbl_node_", node_
//                          ,"node_", sub_rows.get(0).get("node_")
                    ));
            max = Lutils.nvl(max,0)+1;
            for(Map row:sub_rows){
                //把主表id和会签主表的node存入会签表
                row.put("pri_tbl_", pri_table_id); //表字段
                row.put("z_" + pri_table_id + "_id", pri_id_);
                row.put("pri_tbl_node_", node_);   //保存环节的字段
                row.put("pri_tbl_node_enter_times_", max); //环节次数字段
            }
            break;
        }
    }

    /**
     * 把rows的字段转换为数据库可识别的类型
     * */
    public static void convertRowValueToDBType(CompDataSource ds, List<Map> rows){
        Table tbl = (Table) Modules.getInstance().get(ds.table_id,true);
        if( ds!=null && ds.fields!=null) {  //后台事务执行、多人派单等无视图动作，不做响应动作。
            for (Map row : rows) {
                for (Object key : row.keySet()) {
                    List<CompDataSourceField> findFields = ds.fields.stream().filter(o -> o.field.equals(key)).collect(Collectors.toList());
                    if (findFields.size() > 0 ) {
                        CompDataSourceField dsField = findFields.get(0);
                        TableCol tc = tbl.getCol(dsField.table_col_id);
                        if (tc!=null && tc.data_type.equals("timestamp") && row.get(key) != null) {
                            Object obj = row.get(key);
                            if(obj instanceof Long) {
                                Date d = new Date((Long) row.get(key));
                                row.put(key, d);
                            }
                        }
                        if(tc!=null && tc.data_type.equals("varchar") && row.get(key) != null) {
                            //如果提交了列表，转换成String
                            if(row.get(key) instanceof List) {
                                List ls = (List) row.get(key);
                                row.put(key, JSON.toJSONString(ls));
                            }
                        }
                    }
                }
            }
        }
    }

    public static void RemoveContextParams(Map row){
        row.remove("__op_id__");
        row.remove("__op_st__");
        row.remove("__table_id__");
        row.remove("__op_tp__");
        row.remove("__ds_id__");
        row.remove("create_user_staff_nm");
        row.remove("create_user_phone");
        row.remove("poster_phone");
        row.remove("poster_staff_nm");
        row.remove("receiver_staff_nm");
        row.remove("receiver_phone");
        row.remove("edge_label_");
        row.remove("node_label_");
        row.remove("__row_changed_type__");
        row.remove("update_user_staff_nm");
        row.remove("update_user_phone");
        row.remove("session");
        row.remove("__ds_total__");
    }

}
