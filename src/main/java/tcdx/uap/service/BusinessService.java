package tcdx.uap.service;

import cn.hutool.core.collection.CollUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.googlecode.aviator.AviatorEvaluator;
import com.googlecode.aviator.Expression;
import lombok.var;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import tcdx.uap.common.entity.page.TableDataInfo;
import tcdx.uap.common.utils.*;
import tcdx.uap.constant.Constants;
import tcdx.uap.mapper.BaseDBMapper;
import tcdx.uap.mapper.ServiceConfigMapper;
import tcdx.uap.mapper.BusinessMapper;
import tcdx.uap.service.entities.*;
import tcdx.uap.service.entities.custom.ContractPayment;
import tcdx.uap.service.entities.custom.ContractSale;
import tcdx.uap.service.entities.custom.DeliveryNote;
import tcdx.uap.service.entities.custom.PurchaseReceipt;
import tcdx.uap.service.store.*;

import javax.servlet.http.HttpSession;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 参数配置 服务层实现
 *
 * @author ruoyi
 */
@Service
public class BusinessService {

    @Autowired
    BaseDBService db;

    @Autowired
    private BusinessMapper businessMapper;

    @Autowired
    private ServiceConfigMapper serviceConfigMapper;
    @Autowired
    private BaseDBMapper baseDBMapper;

    @Autowired
    private SystemService systemService;
    @Autowired
    private HttpSession httpSession;
    private BaseDBService baseDBService;

    /**
     * 在afterRows中填充接单人
     */
    public void setReceivers(List<Map> afterRows, String flow_edge_id,
                             ExecOp op, ExecContext execContext, UserAction userAction, ExecResult execResult) throws Exception {
        Table tbl = (Table) Modules.getInstance().get(op.table_id, false);
        FlowEdge edge = tbl.getEdge(op.flow_edge_id);
        if (edge == null) {
            return;
        }
        if (edge.assign_type == null || Objects.equals(edge.assign_type, "null")) {
            for (Map row : afterRows) {
                row.put("receiver_", null);
            }
        }
        //当前操作人
        else if (edge.assign_type.equals("current-user")) {
            for (Map row : afterRows) {
                row.put("receiver_", userAction.user_id);
            }
        } else if (edge.assign_type.equals("keep-current-receiver")) {
            //无动作
        } else if (edge.assign_type.equals("rel-dict")) {
            //关联的字典列的id
            Map tableColObj = TableStore.getInstance().getTableCol(edge.rel_dict_col_id);
            //列对应的字典，对应的接单人，取第一个 item_id , user_id
            List<Map> dictItemScopeUsers = DictStore.getInstance().getDictItemScopeUsers(tableColObj.get("rel_dict_id"));
            for (Map row : afterRows) {
                List<Map> fd = dictItemScopeUsers.stream().filter(o ->
                        Objects.equals(
                                row.get(tableColObj.get("field")),
                                o.get("item_id")
                        )
                ).collect(Collectors.toList());
                row.put("receiver_", fd.isEmpty() ? null : fd.get(0).get("first_user_id"));
            }
        } else if (edge.assign_type.equals("defined-role")) {
            //获取流程边上的自定义的组织和角色下的人员。
            ExecContext execContext_s = execContext.getActionContext(op, ExecContext.ENV_CFG_WHERE_LIMIT_IDS);
            Map re = CompUtils.getInstance().getScopedUsersWithGroup(edge.userScope, execContext_s, userAction);
            List<Map> users = (List) re.get("users");
            Object receiver_ = users.isEmpty() ? null : users.get(0).get("user_id");
            //列对应的字典，对应的接单人，取第一个
            for (Map row : afterRows) {
                row.put("receiver_", receiver_);
            }
        } else if (edge.assign_type.equals("edge-poster")) {   //某个动作的完成者 flow-表去找
            //处理新增时，数据行没有id的情况
            String operate_table = edge.operator_of_table;
            List<Integer> ids_ = null;
            if (operate_table.equals(tbl.id)) { //当前表的某个动作的执行人
                ids_ = afterRows.stream().filter(o -> o.get("id_") != null)
                        .map(o -> (Integer) o.get("id_")).collect(Collectors.toList());
            } else {  //当前表对应的主表的某个动作的执行人
                ids_ = afterRows.stream().filter(o -> o.get("z_" + operate_table + "_id") != null)
                        .map(o -> (Integer) o.get("z_" + operate_table + "_id")).collect(Collectors.toList());
            }
            if (ids_.size() > 0) {
                //取ids对应flow表的poster
                List<Map> rowEdgeList = db.selectByCauses("z_" + tbl.id + "_flow", SqlUtil.and(
                        SqlUtil.in("row_id_", ids_),
                        SqlUtil.eq("edge_", edge.operator_of_edge)
                ), null);
                for (Map row : afterRows) {
                    List<Integer> receivers = rowEdgeList.stream().filter(o -> o.get("row_id_").equals(row.get("id_")))
                            .map(o -> (Integer) o.get("poster_")).distinct().collect(Collectors.toList());
                    if (receivers != null && receivers.size() > 0) {
                        row.put("receiver_", receivers.get(0));
                    } else {
                        row.put("receiver_", null);
                    }
                }
            } else {
                throw new Exception("派单到指定流程动作的发件人，未找到关联表的id");
            }
        } else if (edge.assign_type.equals("edge-receiver")) {
            //某个动作的发送者
            //处理新增时，数据行没有id的情况
            String operate_table = edge.operator_of_table;
            List<Integer> ids_ = null;
            if (operate_table.equals(tbl.id)) { //当前表的某个动作的执行人
                ids_ = afterRows.stream().filter(o -> o.get("id_") != null)
                        .map(o -> (Integer) o.get("id_")).collect(Collectors.toList());
            } else {  //当前表对应的主表的某个动作的执行人
                ids_ = afterRows.stream().filter(o -> o.get("z_" + operate_table + "_id") != null)
                        .map(o -> (Integer) o.get("z_" + operate_table + "_id")).collect(Collectors.toList());
            }
            if (ids_.size() > 0) {
                //取ids对应flow表的poster
                List<Map> rowEdgeList = db.selectByCauses("z_" + tbl.id + "_flow", SqlUtil.and(
                        SqlUtil.in("row_id_", ids_),
                        SqlUtil.eq("edge_", edge.operator_of_edge)
                ), null);

                for (Map row : afterRows) {
                    List<Integer> receivers = rowEdgeList.stream().filter(o -> o.get("row_id_").equals(row.get("id_")))
                            .map(o -> (Integer) o.get("receiver_")).distinct().collect(Collectors.toList());
                    if (receivers != null && receivers.size() > 0) {
                        row.put("receiver_", receivers.get(0));
                    } else {
                        row.put("receiver_", null);
                    }
                }
            } else {
                throw new Exception("派单到指定流程动作的发件人，未找到关联表的id");
            }
        } else if (edge.assign_type.equals("node-receiver")) { //某个环节最新的接收人
            //处理新增时，数据行没有id的情况
            String operate_table = edge.operator_of_table;
            List<Integer> ids_ = null;
            if (operate_table.equals(tbl.id)) { //当前表的某个动作的执行人
                ids_ = afterRows.stream().filter(o -> o.get("id_") != null)
                        .map(o -> (Integer) o.get("id_")).collect(Collectors.toList());
            } else {  //当前表对应的主表的某个动作的执行人
                ids_ = afterRows.stream().filter(o -> o.get("z_" + operate_table + "_id") != null)
                        .map(o -> (Integer) o.get("z_" + operate_table + "_id")).collect(Collectors.toList());
            }
            if (ids_.size() > 0) {
                //取ids对应flow表的poster
                List<Map> rowEdgeList = db.selectByCauses("z_" + tbl.id + "_flow", SqlUtil.and(
                        SqlUtil.in("row_id_", ids_),
                        SqlUtil.eq("edge_", edge.operator_of_edge)
                ), null);

                for (Map row : afterRows) {
                    List<Integer> receivers = rowEdgeList.stream().filter(o -> o.get("row_id_").equals(row.get("id_")))
                            .map(o -> (Integer) o.get("poster_")).distinct().collect(Collectors.toList());
                    if (receivers != null && receivers.size() > 0) {
                        row.put("receiver_", receivers.get(0));
                    } else {
                        row.put("receiver_", null);
                    }
                }
            } else {
                throw new Exception("派单到指定流程动作的发件人，未找到关联表的id");
            }
        }
        /**
         * 优先以提交的表单中的receiver_，如果表单中receiver_不存在，则不取flow_edge_123_receiver的信息了。
         * */
        else if (edge.assign_type.equals("manual")) {
            //如果是手动选择的接单人，则从前台的接收人中选择。
            boolean assign_required = Lutils.nvl(edge.assign_required, false);
            List receivers = execContext.getSubmittedReceivers(op);
            //将结果写入rows
            if (receivers.isEmpty() && assign_required) {
                throw new Exception("没有选择接收人");
            }
            Object receiver = receivers.isEmpty() ? null : receivers.get(0);
            for (Map row : afterRows) {
                row.put("receiver_", receiver);
            }
        }
    }


    /**
     * 获取流程动作自定义的接单人
     * @param edge_id 边id
     * */
//    public List<Map> getEdgeDefinedreceivers(Object edge_id){
//        List<Map> flowEdgeDefinedreceivers = businessMapper.get_flow_edge_defined_receivers(Lutils.genMap("flow_edge_id", edge_id));
//        return flowEdgeDefinedreceivers;
//    }

    /**
     * 网关跳转
     * 如果返回true，则再row上添加flow动作
     */
    public void FillGateFlowDst(List<Map> updateRows, Table tbl, String flow_node_id) throws Exception {
        FlowNode dst = tbl.getNode(flow_node_id);
        //获取网关的分支以及递归
        for (Map row : updateRows) {
            //递归找到最后匹配的流程路径
            FlowEdge matchEdge = findGateEdge(row, tbl, dst.outEdges, new HashSet<>());
            if (matchEdge != null) {
                row.put("edge_", matchEdge.id);
                row.put("node_", matchEdge.dst);
            } else {
                //无路径，则保持原样。
                throw new Exception("网关匹配路径失败，id_=" + row.get("id_"));
            }
        }
    }

    public FlowEdge findGateEdge(Map row, Table tbl, List<FlowEdge> flowEdges, Set historyGate) {
        //判断是否满足条件
        for (FlowEdge edge : flowEdges) {
            String dstId = edge.dst;
            if (historyGate.contains(dstId)) {
                return null;
            }
            historyGate.add(dstId);
            boolean isEdge = EdgeConditionResult(row, tbl, edge);
            if (isEdge) {
                if (Objects.equals(edge.dstType, "gate")) { //闭环
                    List<FlowEdge> gateEdges = tbl.getNode(edge.dst).inEdges;
                    return findGateEdge(row, tbl, gateEdges, historyGate);
                } else {
                    return edge;
                }
            }
        }
        return null;
    }

    public boolean EdgeConditionResult(Map row, Table tbl, FlowEdge edge) {
        String condition_field1 = edge.condition_field1 == null ? null : tbl.getCol(edge.condition_field1).field;
        String condition_operator1 = edge.condition_operator1;
        String condition_value_from_type1 = edge.condition_value_from_type1;
        Object condition_value1 = edge.condition_value1;
        Object value_field1 = edge.condition_value_from_col1 == null ? null : tbl.getCol(edge.condition_value_from_col1).field;
        String condition_field2 = edge.condition_field2 == null ? null : tbl.getCol(edge.condition_field2).field;
        String condition_operator2 = edge.condition_operator2;
        String condition_value_from_type2 = edge.condition_value_from_type2;
        Object condition_value2 = edge.condition_value2;
        Object value_field2 = edge.condition_value_from_col1 == null ? null : tbl.getCol(edge.condition_value_from_col1).field;
        String rs1 = compareCondition(condition_field1, condition_operator1, condition_value_from_type1, condition_value1, value_field1, row);
        String rs2 = compareCondition(condition_field2, condition_operator2, condition_value_from_type2, condition_value2, value_field2, row);
        //有效条件数量与匹配成功数量相等
        if ((rs1.equals("success") || rs2.equals("success")) && (!rs1.equals("condition-valid-compare-failed") && !rs2.equals("condition-valid-compare-failed"))) {
            return true;
        } else
            return false;
    }


    public void handlerLog(Table tbl, List update_ids, Map logInfo) {
        //记录log信息
//                baseDBService.selectEq("v_table_col", Lutils.genMap("table_id", table_id));
        List<String> fields = tbl.cols.stream().map(o -> o.field).collect(Collectors.toList());
        logInfo = logInfo != null ? logInfo : Lutils.genMap();
        businessMapper.add_table_log(Lutils.genMap("table_id", tbl.id,
                "fields", fields,
                "row_ids", update_ids,
                "logInfo", logInfo));
    }


    public void handlerFlowAndLog(Table tbl, List update_ids, Map logInfo) {
        if (update_ids != null && !update_ids.isEmpty()) {
            //以id_与edge_的组合为单位，
            businessMapper.delete_old_flow(Lutils.genMap("table_id", tbl.id, "row_ids", update_ids));
            businessMapper.add_new_flow(Lutils.genMap("table_id", tbl.id, "row_ids", update_ids));
            //更新flow信息
            //记录log信息
            handlerLog(tbl, update_ids, logInfo);
        }
    }

//
//    /**
//     *  执行添加数据的一套动作
//     * */
//    public void RowsInsert(List<Map> insertRows, ExecOp op, ExecContext execContext, List<Map> sessionContextList){
//        CompDataSource ds = (CompDataSource) Modules.getInstance().get(op.ds_id, false);
//        //兼容老版本，老版本insert是绑定的view_id
//        if(ds==null&& op.view_id!=null){
//            ds = (CompDataSource) Modules.getInstance().get(op.view_id+"ds", false);
//        }
//        String table_id = ds.table_id;
//        Table tbl = (Table)Modules.getInstance().get(ds.table_id, true);
//        if (CollUtil.isNotEmpty(tbl.cols)) {
//            Map<String, String> collect = tbl.cols.stream().collect(Collectors.toMap(o -> o.id, o -> o.field));
//            for (CompDataSourceField field : ds.fields) {
//                if (StringUtils.isNotEmpty(collect.get(field.table_col_id))) {
//                    field.setField(collect.get(field.table_col_id));
//                }
//            }
//        }
//        //添加操作人数据
//        for(Map op_grid_row: insertRows){
//            op_grid_row.put("create_user_", userAction.get("user_id"));
//            op_grid_row.put("create_time_", userAction.get("action_time"));
//        }
//        //为每行添加scope中的外联id，补充外联表z_table123_id,如果session中出现了多个id，则取第一个
//        if (execContext.(op, ExecContext.ENV_CFG_FILL_COUNTERSIGN)&&tbl.priTableIds.size()>0) {
//            List<Map> preparedList = ExecOpUtil.getPreparedList(sessionContextList, op);
//            ExecOpUtil.FillForeignKeyFromSession(insertRows, tbl.priTableIds, preparedList);
//        }
//        /**
//         * 将提交的数据插入数据库，并将id更新到提交的数据中，同时更新scope
//         * 方法中，同时更新了会话
//         */
//        String  op_table = "z_"+table_id;
//        //根据数据源类型，将提交的数据转换为数据库可存的类型，如：数据库类型timestamp的变量，提交时为long类型，应转换为java.utils.Date
//        ExecOpUtil.convertFieldToDatasourceType(ds, insertRows);
//        //存入数据库，返回数据库完成添加后的行数据
//        List<Map> afterInsertedRows = baseDBService.insertMapListRetRows(op_table, insertRows);
//        //添加到session中
//        ExecOpUtil.ExtractTableRowIdsToSession(sessionContextList,  "insert", op.id,
//                table_id,null, afterInsertedRows);
//        //记录日志
//        handlerLog(tbl,
//                insertRows.stream().map(o->o.get("id_")).collect(Collectors.toList()),
//                Lutils.genMap(
//                "log_time_", userAction.get("action_time"),
//                "action_id_", userAction.get("id"),
//                "action_type_", userAction.get("action_type"),
//                "op_view_id", op.view_id
//        ));
//    }
//
//    /**
//     * 执行更新动作
//     * */
//    public void RowsUpdate(Map update_form, ExecOp op, Map userAction,List<Map> opSessionList){
//        List<Map> opSessions = ExecOpUtil.getPreparedList(opSessionList, op);
//        CompDataSource ds = (CompDataSource) Modules.getInstance().get(op.ds_id, false);
//        //兼容老版本，老版本insert是绑定的view_id
//        if(ds==null&& op.view_id!=null){
//            ds = (CompDataSource) Modules.getInstance().get(op.view_id+"ds", false);
//        }
//        Table tbl = (Table)Modules.getInstance().get(ds.table_id, true);
//        if (CollUtil.isNotEmpty(tbl.cols)) {
//            Map<String, String> collect = tbl.cols.stream().collect(Collectors.toMap(o -> o.id, o -> o.field));
//            for (CompDataSourceField field : ds.fields) {
//                if (StringUtils.isNotEmpty(collect.get(field.table_col_id))) {
//                    field.setField(collect.get(field.table_col_id));
//                }
//            }
//        }
//        String table_id = ds.table_id;
//        List limitIds = opSessions.stream()
//                .filter(o->Objects.equals(o.get("session_type"),"where-limit-ids"))
//                .filter(o->Objects.equals(o.get("table_id"), table_id))
//                .map(o->o.get("id_"))
//                .collect(Collectors.toList());
//
//        //取更新前的数据
//        List<Map> beforeRows = baseDBService.selectIn("z_" + table_id, "id_", limitIds);
//        //更新表单数据
//        update_form.put("update_user_", userAction.get("user_id"));
//        update_form.put("update_time_", userAction.get("action_time"));
//        //执行更新
//        ExecOpUtil.convertFieldToDatasourceType(ds,Lutils.genList(update_form));
//        baseDBService.updateIn("z_" + table_id, update_form, "id_", limitIds);
//        List tmp = baseDBService.selectIn("z_" + table_id, "id_", limitIds);
//        //更新afterRows
//        List<Map> afterRows = new ArrayList<>();
//        for(Map row:beforeRows){
//            Map cp = Lutils.copyMap(row);
//            cp.putAll(update_form);
//            afterRows.add(cp);
//        }
//        //将id添加到会话中，同时提取外键id
//        ExecOpUtil.ExtractTableRowIdsToSession( opSessionList, "update", op.id,
//                table_id, beforeRows, afterRows);
//        //记录日志
//        handlerLog(tbl, limitIds, Lutils.genMap(
//                "log_time_", userAction.get("action_time"),
//                "action_id_", userAction.get("id"),
//                "action_type_", userAction.get("action_type"),
//                "op_view_id", op.view_id
//        ));
//    }
//
//    /**
//     * 分发数据新增数据
//     * */
//    public void DistributeComplete(Map form, ExecOp op, List<Integer> receivers, List<Map> contextList, Map userAction) throws Exception {
//        form = form==null?Lutils.genMap():form;
////        List<Integer> opInSessionNodes = new ArrayList<>();
////        try {
////            opInSessionNodes = (List<Integer>) JSON.parse((String) op.get("op_exec_in_session_nodes"));
////        } catch (Exception e) {
////            System.out.println(e.toString());
////        }
//        Table tbl = (Table) Modules.getInstance().get(op.table_id, true);
//        List<Map> preparedList = ExecOpUtil.getPreparedList(contextList, op);
//        FlowEdge edge = tbl.getEdge(op.flow_edge_id);
//        if (receivers == null && Lutils.nvl(edge.assign_required, false)) {
//            throw new Exception("动作op_id=" + op.id + "，候选人为空");
//        }
//        //根据人员生成行
//        List<Map> insertRows = new ArrayList<>();
//        if (receivers != null && receivers.size() > 0) {
//            for (Integer receiver_ : receivers) {
//                for (int i = 0; i < preparedList.size(); i++) {
//                    Map row = Lutils.copyMap(form);
//                    row.put("poster_", userAction.get("user_id"));
//                    row.put("posted_time_", userAction.get("action_time"));
//                    row.put("prev_node_", edge.src);
//                    row.put("edge_", edge.id);
//                    row.put("node_", edge.dst);
//                    row.put("receiver_", receiver_);
//                    row.put("create_user_", userAction.get("user_id"));
//                    row.put("create_time_", userAction.get("action_time"));
//                    insertRows.add(row);
//                }
//            }
//        }
//        /** 为每行加入外联id，补充外联表z_table123_id,如果session中出现了多个id，则取第一个 */
//        if (ExecOpUtil.IsOpActoinsContainsType(op, "fill-foreign") && tbl.priTableIds.size() > 0) {
//            ExecOpUtil.FillForeignKeyIntoRows(insertRows, tbl.priTableIds, preparedList);
//        }
//        //如果当前是子流程，行中需要添加主表的信息，从会话设置中取：pri_table_id
//        if (ExecOpUtil.IsOpActoinsContainsType(op, "fill-countersign") && tbl.priTableIds.size() > 0) {
//            ExecOpUtil.FillCountersignColFromSession(insertRows, preparedList, op, baseDBService);
//        }
//        //插入数据库
//        List<Map> afterInsertedRows = baseDBService.insertMapListRetRows("z_" + tbl.id, insertRows);
//        //将id添加到会话中，同时提取外键id
//        ExecOpUtil.ExtractTableRowIdsToSession(contextList, "insert",  op.id,
//                op.table_id, null, afterInsertedRows);
//        List insertIds = afterInsertedRows.stream().map(o -> o.get("id_")).collect(Collectors.toList());
//        //要先执行，记录日志和流水，然后子流程信息更新才能获取到信息
//
//        handlerFlowAndLog(tbl, insertIds,
//                Lutils.genMap(
//                        "log_time_", userAction.get("action_time"),
//                        "action_id_", userAction.get("id"),
//                        "action_type_", userAction.get("action_type")
//                ));
//
//        /** 当前操作的表是子流程，子流程执行完后，要汇聚主流程信息，并执行主流程事件 * */
//        HandleNodeEvent(op.table_id, afterInsertedRows);
////        IfTriggerPriComplete(table_id,);
//    }
//
//    /**
//     * 表格的临时数据，提交后添加到数据库并任务处理。
//     * */
//    public void RowsComplete(String table_id, List<Integer> limitIds, String flow_edge_id, String illegalAction,
//                             List<Map> sessionContextList, Map userAction, Integer submitReceiver, String op_id,HttpSession httpSession) throws Exception {
//        //取当前操作的表属性，判断当前表是否子流程表
//        Table tbl = (Table)Modules.getInstance().get(table_id, true);
//        //对已在数据行进行流程流转：设置派发人、接单人、node_、edge_等
//        List<Map> beforeRows = baseDBService.selectIn("z_"+table_id,"id_", limitIds);
//        FlowEdge edge = null;
//        //edge-1是走默认路径。
//        if(Objects.equals(flow_edge_id, "edge-1"))
//            edge = tbl.getFirstEdgeOfNode(Lutils.nvl(beforeRows.get(0).get("node_"),""));
//        else
//            edge = tbl.getEdge(flow_edge_id);
//        Map flowInfo = new HashMap();
//        flowInfo.put("poster_",      userAction.get("user_id"));
//        flowInfo.put("posted_time_", userAction.get("action_time"));
//        flowInfo.put("prev_node_",   edge.src);
//        flowInfo.put("edge_", edge.id);
//        flowInfo.put("node_", edge.dst);
//        List<Map> tmp = new ArrayList<>();
//        /**
//         * 如果该边是汇聚边，对每一行数据判断流程是否满足要求，不满足要求的行，不执行相关动作。
//         */
//        if(Objects.equals(edge.srcType,"sub-flow")) {
//            for (Map row : beforeRows) {
//                boolean rowEdgePassed = EdgeConditionResult(row, tbl, edge);
//                if (rowEdgePassed) {
//                    tmp.add(row);
//                }
//            }
//            beforeRows = tmp;
//        }
//        //判断所执行的动作，是否在当前流程上 行的node_如果为空，有可能时新建完的数据，允许完成
//        FlowEdge finalEdge = edge;
//        List<Map> illegalRows = beforeRows.stream().filter(r -> r.get("node_")!=null && !Objects.equals(r.get("node_"), finalEdge.src)).collect(Collectors.toList());
//        if (illegalRows.size() > 0) {
//            FlowNode illegalNode = tbl.getNode(illegalRows.get(0).get("node_").toString());
//            String msg = "当前流程【" + tbl.name + "】不符合流程环节要求。id_=" + illegalRows.get(0).get("id_")
//                    + ")，执行动作【" + edge.label +"】所需环节:"+ edge.srcLabel +"，而当前数据所在环节：" + illegalNode.label;
//            if(Objects.equals(illegalAction, "stop")) {
//                throw new Exception(msg);
//            }
//            else if(Objects.equals(illegalAction, "skip")) {
//                System.out.println("跳过："+msg);
//                return ;
//            }
//            else if(Objects.equals(illegalAction, "forcibly-exec")) {
//                System.out.println("强制执行："+msg);
//            }
//        }
//
//        //如果当前流程是子流程，则找到主流程的状态，判断主流程的状态是否处于子流程对应的状态
//        if(tbl.priTableIds.size()>0){
//            if(beforeRows.size()>0){
//                //判断主流程是否结束
//                List<Map> subFinishedLs = beforeRows.stream().filter(r->Objects.equals(r.get("pri_tbl_node_finished_"), 1)).collect(Collectors.toList());
//                if(subFinishedLs.size()>0){
//                    throw new Exception("流程【"+tbl.name+"】已结束，请刷新后重试！");
//                }
//                //判断主流程的节点是否正确
//                Map subRow = beforeRows.get(0);
//                String pri_tbl_ = (String) subRow.get("pri_tbl_");
//                String pri_tbl_node_ = (String) subRow.get("pri_tbl_node_");
//                List<Integer> pri_ids = new ArrayList<>();
//                //取涉及到主流程的ids
//                if(pri_tbl_!=null) {
//                    pri_ids = beforeRows.stream().filter(r -> r.get("z_" + pri_tbl_ + "_id") != null).map(r -> (Integer) r.get("z_" + pri_tbl_ + "_id")).collect(Collectors.toList());
//                    if (pri_ids.size() > 0 && pri_tbl_node_ != null) {
//                        //取出主流程节点状态
//                        List<Map> priList = baseDBService.selectIn("z_" + pri_tbl_, "id_", pri_ids);
//                        List<Map> illegalNodes = priList.stream().filter(p -> p.get("node_") != null && !Objects.equals(p.get("node_"), pri_tbl_node_)).collect(Collectors.toList());
//                        if (illegalNodes.size() > 0) {
//                            Table priTbl = (Table) Modules.getInstance().get(pri_tbl_, true);
//                            FlowNode priNode = priTbl.getNode(pri_tbl_node_);
//                            FlowNode priNode1 = priTbl.getNode(illegalNodes.get(0).get("node_").toString());
//                            throw new Exception("流程【"+ tbl.name + "】行(" + subRow.get("id_") + ")与主流程【" + priTbl.name + "】环节不一致，无法执行！子流程依赖主流程环节：" + priNode.label + "，主流程所在节点：" + priNode1.label);
//                        }
//                    }
//                }
//            }
//        }
//
//        //在afterRows中填充poster_、posted_time_、edge_、node_
//        List<Map> afterRows = new ArrayList<>();
//        for(Map row:beforeRows){
//            Map cp = Lutils.copyMap(row);
//            cp.putAll(flowInfo);
//            afterRows.add(cp);
//        }
//
//        //如果是分支，重新设置node_，即：根据分支确定dst
//        FlowNode dstNode = tbl.getNode( edge.dst );
//        if(!Objects.equals(dstNode.type,"gate")){
//            setReceivers(afterRows, tbl, edge.id, userAction, submitReceiver, httpSession, sessionContextList);
//        }
//        else{
//            //更新每一行的目的节点，因为分支判断时，每一行数据值不一样，所以需要针对每一行判断分支，不同的分支派单类型不一样，需要单独派单.node_
//            FillGateFlowDst( afterRows, tbl, edge.id);
//            List distinctNodes = afterRows.stream().filter( r -> r.get("node_") != null).map( r -> r.get("node_")).distinct().collect(Collectors.toList());
//            for(Object distNode: distinctNodes) {
//                List<Map> distNodeRows = afterRows.stream().filter(r->Objects.equals(distNode, r.get("node_"))).collect(Collectors.toList());
//                //判断是到了结束节点
//                Map row = distNodeRows.get(0);
//                String gated_flow_edge_id = (String)row.get("edge_");
//                setReceivers(distNodeRows, tbl, gated_flow_edge_id, userAction, submitReceiver,httpSession, sessionContextList);
//            }
//        }
//        //更新
//        for(Map row: afterRows){
//            baseDBService.updateEq("z_"+table_id,
//                    Lutils.genMap("poster_", row.get("poster_"),
//                            "posted_time_", row.get("posted_time_"),
//                            "prev_node_", row.get("prev_node_"),
//                            "edge_", row.get("edge_"),
//                            "node_", row.get("node_"),
//                            "receiver_", row.get("receiver_")),
//                    Lutils.genMap("id_", row.get("id_"))
//            );
//
//            //查看是否需要短信提醒
//            if(edge.is_sms!=null && edge.is_sms){
//                Map userMap = baseDBService.selectEq("v_user",
//                        Lutils.genMap("id", row.get("receiver_")), null).get(0);
//                String phoneNumber = userMap.get("phone").toString();
////                String phoneNumber = userAction.get("phone").toString();
//                String message = tbl.name;
//                var res2 = SmsUtils.sendMessage(phoneNumber, message);
//                if (res2 != null) {
//                    System.out.println("通知短信发送结果: " + res2.getBody().getMessage());
//                }
//            }
//        }
//        //将id添加到会话中，同时提取外键id
//        ExecOpUtil.ExtractTableRowIdsToSession( sessionContextList, "complete", op_id, table_id, beforeRows, afterRows);
//        //执行事件，将直接结果按node分类后，针对不同的node，执行事件。
//        List distinctNodes = afterRows.stream().filter( r -> r.get("node_") != null).map( r -> r.get("node_")).distinct().collect(Collectors.toList());
//        for(Object distNode: distinctNodes) {
//            List<Map> distNodeRows = afterRows.stream().filter(r->Objects.equals(distNode, r.get("node_"))).collect(Collectors.toList());
//            HandleNodeEvent(table_id, distNodeRows);
//        }
//        List update_ids = afterRows.stream().map(o->o.get("id_")).collect(Collectors.toList());
//        //更新上一个环节的finished_time
//        if(afterRows.size()>0) {
//            businessMapper.set_log_finished_time(Lutils.genMap("table_id", table_id, "row_ids", update_ids, "finished_time_", userAction.get("action_time"), "prev_node_", afterRows.get(0).get("prev_node_")));
//        }
//        //记录日志
//        handlerFlowAndLog( tbl, update_ids,
//                Lutils.genMap(
//                        "log_time_", userAction.get("action_time"),
//                        "action_id_", userAction.get("id"),
//                        "action_type_", userAction.get("action_type")
//                ));
//        //子流程执行完成后，触发主流程汇聚
//        if (tbl.priTableIds.size()>0) {
//            TriggerPriComplete(table_id, afterRows, sessionContextList, submitReceiver, userAction, op_id);
//        }
//    }
//
//    /**
//     * 表格的临时数据，提交后添加到数据库并任务处理。
//     * */
//    public void RowsDelete(List<Map> contextList, ExecOp op, Map userAction) throws Exception {
//        Table tbl = (Table) Modules.getInstance().get(op.table_id, true);
//        List<Map> prepareList = ExecOpUtil.getPreparedList(contextList, op);
//        List limitIds = prepareList.stream()
//                .filter(o->Objects.equals(o.get("session_type"),"where-limit-ids"))
//                .filter(o->Objects.equals(o.get("table_id"), op.table_id))
//                .map(o->o.get("id_")).collect(Collectors.toList());
//        List<Map> beforeRows = baseDBService.selectEq("z_"+op.table_id, Lutils.genMap("id_", limitIds));
//        baseDBService.updateIn("z_"+op.table_id, Lutils.genMap("time_if_delete", userAction.get("action_time")),"id_", limitIds);
//        ExecOpUtil.ExtractTableRowIdsToSession( contextList, "delete",  op.id,
//                op.table_id, beforeRows, null);
//        //记录日志
//        handlerFlowAndLog( tbl,
//                beforeRows.stream().map(o->o.get("id_")).collect(Collectors.toList()),
//                Lutils.genMap(
//                        "log_time_", userAction.get("action_time"),
//                        "action_id_", userAction.get("id"),
//                        "action_type_", userAction.get("action_type")
//                ));
//    }
//
//    /**
//     * 执行事务，同时将更新表的外联id保存到会话。
//     */
    public void executeTrans(ExecOp op, ExecContext execContext, UserAction userAction, ExecResult execResult) throws Exception {
        //获取递归执行清单
        if (Lutils.nvl(op.trans_sql, "").length() == 0){
            String msg = "事务sql为空，无法执行";
            execResult.addResult(new ExecOpResult( op.id,  0, false, msg, null));
            throw new Exception(msg);
        }

        Map whereIdsMap = execContext.getWhereIdsMap(op);
        //更新操作
        //execContext_s 是所有wherelimitId取出来的记录
        List<String> table_ids = CompUtils.getSql___z_table_ids(op.trans_sql);
        for(String table_id : table_ids) {
            if (!whereIdsMap.containsKey(table_id)) {
                Table tbl = (Table) Modules.getInstance().get(table_id, false);
                String msg = "事务动作" + op.id + "：缺失“" + tbl.name + "”的限制ID";
                execResult.addResult(new ExecOpResult( op.id,  0, false, msg, null));
                throw new Exception(msg);
            }
        }
        String upd_sql = CompUtils.replaceSql_z_table_ids(op.trans_sql, whereIdsMap, userAction);
        //执行更新
//        List<Map> afterRows = db.selectIn("z_"+upd_table_id, "id_", whereLimitedIds);
//        execContext.addContexts(afterRows, op.id, ExecContext.OP_STATE_AFTER, upd_table_id, ExecContext.OP_TYPE_EXECUTE_SQL, null);
//        execContext.addContextsForeignTableIds(afterRows, op.id, ExecContext.OP_STATE_AFTER, op.table_id, ExecContext.OP_TYPE_RELATED_FOREIGN);
        upd_sql = upd_sql.replace("#{user_id}", userAction.user_id.toString());
        db.executeSql(upd_sql, userAction.getMap());
        execResult.addResult(new ExecOpResult( op.id,  0, true, ExecContext.RS_MSG_SUCCESS, null));
    }

    // 从任意字符串中提取 z_tablexxx（来自 z_tablexxx_ids）
    public static List<String> extractZTableList(String text) {
        if (text == null || text.isEmpty()) return Collections.emptyList();

        // group(1) = z_tablexxx （不包含 _ids）
        Pattern p = Pattern.compile("\\b(z_table\\w*)_ids\\b");
        Matcher m = p.matcher(text);

        // 用 LinkedHashSet 去重 + 保持首次出现顺序
        Set<String> set = new LinkedHashSet<>();
        while (m.find()) {
            set.add(m.group(1));
        }
        return new ArrayList<>(set);
    }


    /**
     * 返回:'公式无效'、‘公式有效数据不匹配’、“公式有效数据匹配”
     */
    public String compareCondition(String condition_field1,
                                   String condition_operator1,
                                   String condition_value_from_type1,
                                   Object condition_value1,
                                   Object value_field1, Map row) {
        if (condition_field1 != null && condition_value_from_type1 != null && condition_operator1 != null) {
            if (condition_value_from_type1.equals("defined-value")) {
                try {
                    if (condition_operator1.equals("in")) {
                        String value = row.get(condition_field1).toString();
                        String[] vals = condition_value1.toString().split("\n");
                        if (Arrays.asList(vals).contains(value))
                            return "success";
                    } else if (condition_operator1.equals("=") && row.get(condition_field1).toString().equals(condition_value1.toString()))
                        return "success";
                    else if (condition_operator1.equals(">") && Float.parseFloat(row.get(condition_field1).toString()) > Float.parseFloat(condition_value1.toString()))
                        return "success";
                    else if (condition_operator1.equals(">=") && Float.parseFloat(row.get(condition_field1).toString()) >= Float.parseFloat(condition_value1.toString()))
                        return "success";
                    else if (condition_operator1.equals("<") && Float.parseFloat(row.get(condition_field1).toString()) < Float.parseFloat(condition_value1.toString()))
                        return "success";
                    else if (condition_operator1.equals("<=") && Float.parseFloat(row.get(condition_field1).toString()) <= Float.parseFloat(condition_value1.toString()))
                        return "success";
                } catch (Exception e) {
                }
            } else {  //table-col
                try {
                    if (condition_operator1.equals("=") && row.get(condition_field1).toString().equals(row.get(value_field1).toString()))
                        return "success";
                    else if (condition_operator1.equals(">") && Float.parseFloat(row.get(condition_field1).toString()) > Float.parseFloat(row.get(value_field1).toString()))
                        return "success";
                    else if (condition_operator1.equals(">=") && Float.parseFloat(row.get(condition_field1).toString()) >= Float.parseFloat(row.get(value_field1).toString()))
                        return "success";
                    else if (condition_operator1.equals("<") && Float.parseFloat(row.get(condition_field1).toString()) < Float.parseFloat(row.get(value_field1).toString()))
                        return "success";
                    else if (condition_operator1.equals("<=") && Float.parseFloat(row.get(condition_field1).toString()) <= Float.parseFloat(row.get(value_field1).toString()))
                        return "success";
                } catch (Exception e) {
                }
            }
            return "condition-valid-compare-failed";
        }
        return "condition-invalid";
    }

    /**
     * 将列表生成树形结构，同时过滤掉无权限的视图 和 按钮
     * 同时查询初始化数据
     */
    public Map CreatePowerViewTreeWithInitRows(List<View> views, ExecContext execContext_s, UserAction userAction) throws Exception {
        /** 限权 */
        List<View> permedViews = new ArrayList<>();
        //界面上使用到的dsIds
        List<String> viewDSIds = new ArrayList<>();
        for (View v : views) {
            if (!Objects.equals(userAction.user_id, 879) && !RoleStore.getInstance().isViewPermed(userAction.userRoleIds, v.id)) {
                continue;
            }
            //剔除视图的底部的按钮
            v.viewBtns = v.viewBtns.stream()
                    .filter(o -> RoleStore.getInstance().isBtnPermed(userAction.userRoleIds, o.id, userAction.user_id))
                    .collect(Collectors.toList());
            //剔除视图标题内部的按钮
            v.viewTitleBtns = v.viewTitleBtns.stream()
                    .filter(o -> RoleStore.getInstance().isBtnPermed(userAction.userRoleIds, o.id, userAction.user_id))
                    .collect(Collectors.toList());
            //如果是表格，需要剔除表格顶部的按钮和表格列的按钮
            if (Objects.equals(v.view_type, "comp") && v.comp_name != null) {
                //处理表格的按钮
                if (v.comp_name.equals("CompGrid")) {
                    CompGrid comp = (CompGrid) v.comp;
                    if (comp != null) {
                        comp.topBtns = comp.topBtns.stream()
                                .filter(o -> RoleStore.getInstance().isBtnPermed(userAction.userRoleIds, o.id, userAction.user_id))
                                .collect(Collectors.toList());
                        //对每一列
                        for (CompGridCol gridCol : comp.gridCols) {
                            gridCol.btns = gridCol.btns.stream()
                                    .filter(o -> RoleStore.getInstance().isBtnPermed(userAction.userRoleIds, o.id, userAction.user_id))
                                    .collect(Collectors.toList());
                        }
                        //处理表格每列的编辑器
                        for (CompGridCol gridCol : comp.gridCols) {
                            if(gridCol.foreign_key_view_id != null){
                                View fv = (View) Modules.getInstance().get(gridCol.foreign_key_view_id,false);
                                String fk_comp_id = fv.comp_id;
                                CompValueEditor fk_editor = (CompValueEditor) Modules.getInstance().get(fk_comp_id,false);
                                fk_editor.compGrid.compDataSource = (CompDataSource) Modules.getInstance().get(fk_editor.compGrid.ds_id,false);
                                gridCol.compValueEditor = fk_editor;
                            }
                        }
                    }
                }
                if(Objects.equals(v.comp_name,"CompGrid")){
                    CompGrid comp = (CompGrid)Modules.getInstance().get(v.comp_id,false);
                    viewDSIds.add(comp.ds_id);
                }
                else if(Objects.equals(v.comp_name,"CompCountAggr")){
                    CompCountAggr comp = (CompCountAggr)Modules.getInstance().get(v.comp_id,false);
                    viewDSIds.add(comp.ds_id);
                }
                else if(Objects.equals(v.comp_name,"CompCarousel")){
                    CompCarousel comp = (CompCarousel)Modules.getInstance().get(v.comp_id,false);
                    viewDSIds.add(comp.ds_id);
                }
                else if(Objects.equals(v.comp_name,"CompValueRender")){
                    CompValueRender comp = (CompValueRender)Modules.getInstance().get(v.comp_id,false);
                    viewDSIds.add(comp.ds_id);
                }
                else if(Objects.equals(v.comp_name,"CompValueEditor")){
                    CompValueEditor comp = (CompValueEditor)Modules.getInstance().get(v.comp_id,false);
                    viewDSIds.add(comp.ds_id);
                }
                else if(Objects.equals(v.comp_name,"CompEcharts")){
                    CompEcharts comp = (CompEcharts)Modules.getInstance().get(v.comp_id,false);
                    viewDSIds.add(comp.ds_id);
                }
                else if(Objects.equals(v.comp_name,"CompGantt")){
                    CompGantt comp = (CompGantt)Modules.getInstance().get(v.comp_id,false);
                    viewDSIds.add(comp.ds_id);
                }
                else if(Objects.equals(v.comp_name,"CompGantt")){
                    CompGantt comp = (CompGantt)Modules.getInstance().get(v.comp_id,false);
                    viewDSIds.add(comp.ds_id);
                }
            }
            permedViews.add(v);
        }
        /** 取数 */
        List<String> viewIds = permedViews.stream().map(o -> o.id).collect(Collectors.toList());
        //求根节点
        List<View> roots = permedViews.stream().filter(o -> !viewIds.contains(o.parent_id)).collect(Collectors.toList());
        //提取提交的session数据
        View root = roots.get(0);
        root.root_id = root.id;
        //查询数据源数据
        root.dsList = root.dsList.stream().filter(o->viewDSIds.contains(o.id)).collect(Collectors.toList());
        root.tableDataInfo = new HashMap<>();
        for (CompDataSource ds : root.dsList) {
            TableDataInfo data = CompUtils.getInstance().get_ds_data(ds, null, execContext_s, userAction, ds.enable_total);
            ds.data = data;
            ds.onlyKeepFrontPageNeed();
            root.tableDataInfo.put(ds.id, data);
        }
        //将每一个view，添加到其父节点的children中
        for (View v : permedViews) {
            v.root_id = root.id;
            //找到父节点，用于构建树
            List<View> parents = permedViews.stream().filter(o -> o.id.equals(v.parent_id)).collect(Collectors.toList());
            if (parents != null && parents.size() > 0) {
                //初始化数据
                if (v.view_type.equals("comp") && v.comp_name.equals("CompGrid")) {
                } else if (v.view_type.equals("comp") && v.comp_name.equals("CompCarousel")) {
                } else if (v.view_type.equals("comp") && v.comp_name.equals("CompCountAggr")) {
                } else if (v.view_type.equals("comp") && v.comp_name.equals("CompUserSelector")) {
                    CompUserSelector compUserSelector = (CompUserSelector) v.comp;
                    compUserSelector.initData = CompUtils.getInstance().getScopedUsersWithGroup(compUserSelector.userScope, execContext_s, userAction);
                }
                //区UUID组件的初始化数据
                else if (v.view_type.equals("comp") && v.comp_name.equals("CompValueEditor")) {
                    CompValueEditor cmp = (CompValueEditor) v.comp;
                    if (cmp != null) {
                        if (Objects.equals(cmp.editor_type, "uuid-editor")) {
                            cmp.value = Lutils.nvl(cmp.uuid_prefix, "") + Constants.getUUID();
                        } else if (Objects.equals(cmp.editor_type, "user-editor")) {
                            cmp.initData = CompUtils.getInstance().getScopedUsersWithGroup(cmp.userScope, execContext_s, userAction);
                        }else if (Objects.equals(cmp.editor_type, "foreign-key-editor")) {
                            cmp.compGrid = (CompGrid) Modules.getInstance().get(cmp.grid_id, true);
                            cmp.compGrid.compDataSource = (CompDataSource) Modules.getInstance().get(cmp.compGrid.ds_id, false);
                            cmp.compGrid.compDataSource.onlyKeepFrontPageNeed();
                        }
                    }
                }
                else if (v.view_type.equals("comp") && v.comp_name.equals("CompFlowNavigator")) {
                    CompFlowNavigator compFlowNavigator = (CompFlowNavigator) v.comp;
                    if (compFlowNavigator != null) {
                        String node_id = "";
                        //从submitMap中获取table_id对应的node_id；
                        List<Map> fds = execContext_s.getTableRows(compFlowNavigator.table_id);
                        if (fds.size() > 0)
                            node_id = (String) fds.get(0).get("node_");
                        compFlowNavigator.getNodeAndOutEdges(node_id);
                    }
                }
                else if (v.view_type.equals("comp") && v.comp_name.equals("CompNodeSelector")) {
                    CompNodeSelector nodeSelector = (CompNodeSelector) v.comp;
                    if (nodeSelector != null) {
                        String node_id = "";
                        //从submitMap中获取table_id对应的node_id；
                        List<Map> fds = execContext_s.getTableRows(nodeSelector.table_id);
                        if (fds.size() > 0)
                            node_id = (String) fds.get(0).get("node_");
                        nodeSelector.setNodes();
                        Set<String> prevNodeId = getNodePrevNodeIds(node_id);
                        nodeSelector.nodes = nodeSelector.nodes.stream().filter(o -> prevNodeId.contains(o.id)).collect(Collectors.toList());
                    }
                }
                else if (v.view_type.equals("comp") && v.comp_name.equals("PurchaseReceipt")) {
                    PurchaseReceipt purchaseReceipt = new PurchaseReceipt();
                    purchaseReceipt.initData = buildReceiptDataMap(execContext_s);
                    v.comp = purchaseReceipt;
                } else if (v.view_type.equals("comp") && v.comp_name.equals("DeliveryNote")) {
                    DeliveryNote deliveryNote = new DeliveryNote();
                    deliveryNote.initData = buildReceiptDataMap(execContext_s);
                    v.comp = deliveryNote;
                } else if (v.view_type.equals("comp") && v.comp_name.equals("ContractSale")) {
                    ContractSale contractSale = new ContractSale();
                    contractSale.initData = buildReceiptDataMap(execContext_s);
                    v.comp = contractSale;
                } else if (v.view_type.equals("comp") && v.comp_name.equals("ContractPayment")) {
                    ContractPayment contractPayment = new ContractPayment();
                    contractPayment.initData = buildReceiptDataMap(execContext_s);
                    v.comp = contractPayment;
                } else if (v.view_type.equals("comp") && v.comp_name.equals("CompLogTable")) {
                    CompLogTable compLogTable = (CompLogTable) v.comp;
                    List<Map> rows = execContext_s.getTableRows(compLogTable.table_id);
                    if (!rows.isEmpty()) {
                        Map form = rows.get(0);
                        form.put("table_id", form.get("__table_id__"));
                        compLogTable.initData = businessMapper.getLogTableInitData(form);
                    }
                }
                else if (v.view_type.equals("comp") && v.comp_name.equals("CompTree")) {
                    CompTree compTree = (CompTree) v.comp;
                    String ds_id = compTree.ds_id;
                    CompDataSource ds = (CompDataSource) Modules.getInstance().get(ds_id, true);
//                    CompDataSourceField compDataSourceField = (CompDataSourceField) Modules.getInstance().get(compTree.search_ds_field_id,true);
//                    compTree.search_ds_field = compDataSourceField;
                    Map<String, Object> map = new HashMap<>();
                    map.put("pageSize",3000);
                    map.put("pageNum",1);
                    TableDataInfo tableInfo = CompUtils.getInstance().get_ds_data(ds, map, execContext_s, userAction, true);
                    List<?> rows = tableInfo.getRows();
                    String labelField = compTree.label_ds_field_id;
                    String parentField = compTree.parent_ds_field_id;
                    String idField = "id_";
                    List<Map<String, Object>> dataRows = new ArrayList<>();
                    for (Object row : rows) {
                        Map<String, Object> dataRow = (Map<String, Object>) row;
                        dataRows.add(dataRow);
                    }
//                    List<Map<String, Object>> treeData = convertToTree(dataRows, labelField, parentField, idField);
                    // 构建成树形列表设置为初始数据
                    compTree.initData.put("data", dataRows);
                }
                else if (v.view_type.equals("comp") && v.comp_name.equals("CompTimeline")) {
                    CompTimeline compTimeline = (CompTimeline) v.comp;
                }
                //构建树
                parents.get(0).children.add(v);
            }
        }
        //加载数据
        return Lutils.genMap("roots", roots, "size", permedViews.size());
    }

    private Set<String> getNodePrevNodeIds(String node_id) {
        String sql = "WITH RECURSIVE node_path AS (\n" +
                "    SELECT \n" +
                "        src AS node_id,\n" +
                "        1 AS level\n" +
                "    FROM v_flow_edge\n" +
                "    WHERE dst = '" + node_id + "'\n" +
                "    \n" +
                "    UNION ALL\n" +
                "    \n" +
                "    SELECT \n" +
                "        e.src AS node_id,\n" +
                "        np.level + 1 AS level\n" +
                "    FROM v_flow_edge e\n" +
                "    INNER JOIN node_path np ON e.dst = np.node_id\n" +
                ")\n" +
                "SELECT DISTINCT node_id, level\n" +
                "FROM node_path\n" +
                "ORDER BY level DESC";
        List<Map> prevNodeMaps = baseDBMapper.selectDefinedSql(Lutils.genMap("data_sql", sql));
        return prevNodeMaps.stream().map(o -> o.get("node_id").toString()).collect(Collectors.toSet());
    }

    public List<Map> reloadCompTimeline(Map submitMap) {
        String table_id = (String)submitMap.get("table_id");
        //提取提交的session数据
        Map copySubmitMap = (Map)Lutils.copy(submitMap!=null?submitMap:new HashMap());
        //获取ValueRender组件属性
        List<Map> submitSession = copySubmitMap.get("session")!=null?(List<Map>)copySubmitMap.get("session"):new ArrayList<>();
        List<Map> rows = new ArrayList<>();
        if (CollUtil.isNotEmpty(submitSession)) {
            List<Map> fds = submitSession.stream().filter(i->Objects.equals(i.get("__table_id__"), table_id)).collect(Collectors.toList());
            String tableName = "z_" + table_id + "_log";
            Table table = (Table) Modules.getInstance().get(table_id, true);
            Map<String, TableCol> colMap = table.cols.stream().collect(Collectors.toMap(TableCol::getField, f -> f));
            // 这边根据table_id构建日志查询sql
            String data_sql = "select\n" +
                    "\tt.*,\n" +
                    "\tn.label,\n" +
                    "\tt.action_type_,\n" +
                    "\tp.staff_nm sender,\n" +
                    "\tr.staff_nm receiver\n" +
                    "from " + tableName + " t\n" +
                    "left join v_flow_node n on n.id = t.node_\n" +
                    "left join v_user p on p.id = t.poster_\n" +
                    "left join v_user r on r.id = t.receiver_\n" +
                    "where t.row_id_ = " + fds.get(0).get("id_") + "\n";
            String order_sql = "order by \n" +
                    "\tt.log_time_ asc, \n" +
                    "\tcase t.action_type_\n" +
                    "\t\twhen 'insert' then 1\n" +
                    "\t\twhen 'update' then 2\n" +
                    "\t\twhen 'complete' then 3\n" +
                    "\t\telse 99\n" +
                    "\tend asc";
            Map<String, Object> map = new HashMap<>();
            map.put("data_sql", data_sql);
            map.put("order_sql", order_sql);
            List<Map> maps = baseDBMapper.selectDefinedSql(map);
            rows.addAll(maps);
            // 排除字段
            Set<String> excludedFields = new HashSet<>(Arrays.asList(
                    "id_", "action_type_", "row_id_", "finished_time_",
                    "node_", "update_user_", "create_user_",
                    "posted_time_", "create_time_", "edge_",
                    "poster_", "receiver_", "log_time_",
                    "action_id_", "prev_node_", "update_time_"
            ));

            Set<String> joinedTableFields = new HashSet<>(Arrays.asList(
                    "label", "sender", "receiver"
            ));

            // 构建一下提交表单数据
            // 构建一下提交表单数据
            for (int i = 0; i < rows.size(); i++) {
                Map<String, Object> row = rows.get(i);

                // 处理insert操作 - 新增记录时记录所有字段
                if ("insert".equals(row.get("action_type_"))) {
                    row.put("label", "开始");
                    List<Map<String, Object>> formData = new ArrayList<>();

                    // 遍历原始map中的每个键值对
                    for (Object keyObj : row.keySet()) {
                        String key = keyObj.toString();

                        // 如果字段既不在排除列表中，也不是关联表字段，则添加到formData
                        if (!excludedFields.contains(key) && !joinedTableFields.contains(key)) {
                            TableCol tableCol = colMap.get(key);
                            if (tableCol == null) {
                                continue;
                            }

                            Object value = row.get(key);
                            if (value == null) {
                                continue;
                            }

                            Map<String, Object> formItem = new HashMap<>();
                            formItem.put("field", tableCol.getName());

                            // 根据值类型处理
                            if (value instanceof Timestamp) {
                                handleTimestampValue(formItem, (Timestamp) value, tableCol, "newValue");
                            } else if (value instanceof String) {
                                handleStringValue(formItem, (String) value, "newValue");
                            } else if (value instanceof Number) {
                                handleNumberValue(formItem, (Number) value, "newValue");
                            } else {
                                // 其他类型保持原样
                                formItem.put("type", "text");
                                formItem.put("newValue", value);
                            }

                            formData.add(formItem);
                        }
                    }

                    row.put("formData", formData);
                }
                // 处理update操作 - 只记录变化的字段
                else if ("update".equals(row.get("action_type_"))) {
                    // 查找当前记录的上一条update或insert记录
                    Map<String, Object> previousRow = findPreviousRecord(rows, i, row);

                    if (previousRow != null) {
                        List<Map<String, Object>> formData = new ArrayList<>();

                        // 只比较需要记录的字段
                        for (Object keyObj : row.keySet()) {
                            String key = keyObj.toString();

                            // 如果字段既不在排除列表中，也不是关联表字段，则进行比较
                            if (!excludedFields.contains(key) && !joinedTableFields.contains(key)) {
                                TableCol tableCol = colMap.get(key);
                                if (tableCol == null) {
                                    continue;
                                }

                                Object currentValue = row.get(key);
                                Object previousValue = previousRow.get(key);

                                // 比较当前值和之前的值是否不同
                                boolean hasChanged = false;

                                if (currentValue == null && previousValue != null) {
                                    hasChanged = true;
                                } else if (currentValue != null && previousValue == null) {
                                    hasChanged = true;
                                } else if (currentValue != null && !currentValue.equals(previousValue)) {
                                    hasChanged = true;
                                }

                                // 如果值有变化，则记录这个字段
                                if (hasChanged) {
                                    Map<String, Object> formItem = new HashMap<>();
                                    formItem.put("field", tableCol.getName());

                                    // 记录旧值和新值
                                    if (previousValue instanceof Timestamp) {
                                        handleTimestampValue(formItem, (Timestamp) previousValue, tableCol, "oldValue");
                                    } else if (previousValue instanceof String) {
                                        handleStringValue(formItem, (String) previousValue, "oldValue");
                                    } else if (previousValue instanceof Number) {
                                        handleNumberValue(formItem, (Number) previousValue, "oldValue");
                                    } else {
                                        formItem.put("type", "text");
                                        formItem.put("oldValue", previousValue);
                                    }

                                    // 根据值类型处理新值
                                    if (currentValue instanceof Timestamp) {
                                        handleTimestampValue(formItem, (Timestamp) currentValue, tableCol, "newValue");
                                    } else if (currentValue instanceof String) {
                                        handleStringValue(formItem, (String) currentValue, "newValue");
                                    } else if (currentValue instanceof Number) {
                                        handleNumberValue(formItem, (Number) currentValue, "newValue");
                                    } else {
                                        formItem.put("type", "text");
                                        formItem.put("newValue", currentValue);
                                    }

                                    formData.add(formItem);
                                }
                            }
                        }

                        row.put("formData", formData);
                    } else {
                        // 如果没有找到上一条记录，按insert处理（记录所有字段）
                        List<Map<String, Object>> formData = new ArrayList<>();

                        for (Object keyObj : row.keySet()) {
                            String key = keyObj.toString();

                            if (!excludedFields.contains(key) && !joinedTableFields.contains(key)) {
                                TableCol tableCol = colMap.get(key);
                                if (tableCol == null) {
                                    continue;
                                }

                                Object value = row.get(key);
                                if (value == null) {
                                    continue;
                                }

                                Map<String, Object> formItem = new HashMap<>();
                                formItem.put("field", tableCol.getName());

                                if (value instanceof Timestamp) {
                                    handleTimestampValue(formItem, (Timestamp) value, tableCol, "newValue");
                                } else if (value instanceof String) {
                                    handleStringValue(formItem, (String) value, "newValue");
                                } else if (value instanceof Number) {
                                    handleNumberValue(formItem, (Number) value, "newValue");
                                } else {
                                    formItem.put("type", "text");
                                    formItem.put("newValue", value);
                                }

                                formData.add(formItem);
                            }
                        }

                        row.put("formData", formData);
                    }
                }
            }
        }
        return rows;
    }

    /**
     * 查找当前记录的上一条update或insert记录
     * @param rows 所有记录列表
     * @param currentIndex 当前记录索引
     * @param currentRow 当前记录
     * @return 上一条记录，如果没有则返回null
     */
    private Map<String, Object> findPreviousRecord(List<Map> rows, int currentIndex, Map<String, Object> currentRow) {
        // 假设记录有唯一标识符字段，比如"id"或"record_id"
        String idField = "row_id_"; // 根据实际情况调整字段名
        Object currentId = currentRow.get(idField);

        if (currentId == null) {
            return null;
        }

        // 从当前位置向前查找同一条记录的上一个版本
        for (int i = currentIndex - 1; i >= 0; i--) {
            Map<String, Object> previousRow = rows.get(i);
            Object previousId = previousRow.get(idField);

            // 如果是同一条记录，并且是update或insert操作
            if (currentId.equals(previousId) &&
                    ("update".equals(previousRow.get("action_type_")) ||
                            "insert".equals(previousRow.get("action_type_")))) {
                return previousRow;
            }
        }

        return null;
    }

    /**
     * 处理时间戳类型值
     */
    private void handleTimestampValue(Map<String, Object> formItem, Timestamp timestamp, TableCol tableCol, String valueField) {
        formItem.put("type", "text");

        if ("timestamp".equals(tableCol.getData_type())) {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            formItem.put(valueField, sdf.format(timestamp));
        } else {
            formItem.put(valueField, timestamp);
        }
    }

    /**
     * 处理字符串类型值
     */
    private void handleStringValue(Map<String, Object> formItem, String stringValue, String valueField) {
        // 使用正则表达式或更精确的方法判断是否为文件URL
        if (isFileUrl(stringValue)) {
            formItem.put("type", "file");
        } else {
            formItem.put("type", "text");
        }
        formItem.put(valueField, stringValue);
    }

    /**
     * 处理数字类型值
     */
    private void handleNumberValue(Map<String, Object> formItem, Number numberValue, String valueField) {
        formItem.put("type", "text");
        formItem.put(valueField, numberValue.toString());
    }

    /**
     * 判断是否为文件URL（更精确的判断）
     */
    private boolean isFileUrl(String value) {
        if (value == null || value.trim().isEmpty()) {
            return false;
        }
        // 可以根据实际需求调整判断逻辑
        return value.contains("url") ||
                value.startsWith("http") ||
                value.endsWith(".jpg") || value.endsWith(".png") ||
                value.endsWith(".pdf") || value.endsWith(".doc");
    }

    /**
     * 将平铺数据转换为树形结构
     * @param rows 原始数据列表，每个元素是一个Map
     * @param labelField 标签字段名（对应ElementUI的label）
     * @param parentField 父节点字段名
     * @param idField 节点ID字段名（对应ElementUI的id）
     * @return ElementUI树形结构数据
     */
    public List<Map<String, Object>> convertToTree(
            List<Map<String, Object>> rows,
            String labelField,
            String parentField,
            String idField) {

        if (rows == null || rows.isEmpty()) {
            return new ArrayList<>();
        }

        // 1. 将数据按父节点ID分组
        Map<Object, List<Map<String, Object>>> parentChildMap = rows.stream()
                .collect(Collectors.groupingBy(
                        row -> row.get(parentField),
                        Collectors.toList()
                ));

        // 2. 找出所有根节点（父节点为0、null或不存在的节点）
        List<Map<String, Object>> rootNodes = new ArrayList<>();

        // 先找到所有存在的ID，用于判断父节点是否存在
        Set<Object> existingIds = rows.stream()
                .map(row -> row.get(idField))
                .collect(Collectors.toSet());

        for (Map<String, Object> row : rows) {
            Object parentId = row.get(parentField);

            // 判断是否为根节点：父节点为0、null或父节点不存在于当前数据中
            boolean isRoot = parentId == null ||
                    "0".equals(parentId.toString()) ||
                    !existingIds.contains(parentId);

            if (isRoot) {
                rootNodes.add(createTreeNode(row, labelField, idField));
            }
        }

        // 3. 递归构建树形结构
        for (Map<String, Object> rootNode : rootNodes) {
            buildTree(rootNode, parentChildMap, labelField, idField);
        }

        return rootNodes;
    }

    /**
     * 创建树节点
     */
    private Map<String, Object> createTreeNode(
            Map<String, Object> row,
            String labelField,
            String idField) {

        Map<String, Object> node = new HashMap<>();
        node.put("id", row.get(idField));
        node.put("label", row.get(labelField));
        node.put("children", new ArrayList<Map<String, Object>>());

        // 保留原始数据中的所有字段（可选）
        // row.forEach((key, value) -> {
        //     if (!key.equals(labelField) && !key.equals(idField)) {
        //         node.put(key, value);
        //     }
        // });

        return node;
    }

    /**
     * 递归构建子树
     */
    private void buildTree(
            Map<String, Object> parentNode,
            Map<Object, List<Map<String, Object>>> parentChildMap,
            String labelField,
            String idField) {

        Object parentId = parentNode.get("id");
        List<Map<String, Object>> childrenData = parentChildMap.get(parentId);

        if (childrenData != null && !childrenData.isEmpty()) {
            List<Map<String, Object>> children = new ArrayList<>();

            for (Map<String, Object> childData : childrenData) {
                Map<String, Object> childNode = createTreeNode(childData, labelField, idField);
                children.add(childNode);

                // 递归处理子节点的子节点
                buildTree(childNode, parentChildMap, labelField, idField);
            }

            parentNode.put("children", children);
        }
    }

    //流程执行后，若当前流程时子流程，则处理子流程的关联的动作,afterRows: 不同的node、不同的pri_tbl_row_id
    public void TriggerPriComplete(List<Map> subAfterRows,
                                   ExecOp sub_op, ExecContext execContext, UserAction userAction, ExecResult execResult) throws Exception {
        Table tbl = (Table) Modules.getInstance().get(sub_op.table_id, true);
        //子流程不同的dst节点处理
        List distinctNodes = subAfterRows.stream()
                .filter(r -> r.get("node_") != null)
                .map(r -> r.get("node_"))
                .distinct()
                .collect(Collectors.toList());
        for (Object distNode : distinctNodes) {
            List<Map> distNodeRows = subAfterRows.stream()
                    .filter(r -> Objects.equals(distNode, r.get("node_")))
                    .collect(Collectors.toList());
            //判断是到了结束节点
            Map row = distNodeRows.get(0);
            String node_ = (String) row.get("node_");
            FlowNode node = tbl.getNode(node_);
            //如果子流程到了结束，找到对应的主流程，判断是否可以进行主流程的下一步
            if (Objects.equals(node.type, "end")) {
                //数据
                String pri_tbl_ = (String) row.get("pri_tbl_");
                Table priTbl = (Table) Modules.getInstance().get(pri_tbl_, true);
                String pri_tbl_node_ = (String) row.get("pri_tbl_node_");
                if (pri_tbl_ == null || pri_tbl_node_ == null)  //无主表信息，则不执行
                    return;
                Integer pri_tbl_node_enter_times_ = (Integer) row.get("pri_tbl_node_enter_times_");
                List<FlowEdge> priOutEdges = priTbl.getNode(pri_tbl_node_).outEdges;
                //动作
                ExecOp priOp = new ExecOp();
                priOp.id = sub_op.id + "_" + node_;
                priOp.table_id = priTbl.id;
                priOp.op_type = ExecOp.OP_TYPE_TRIGGER_COMPLETE;
                priOp.flow_edge_id = priOutEdges.get(0).id;
                priOp.illegal_processing = ExecOp.OP_ILLEGAL_PROCESSING_STOP;
                priOp.opActions = new ArrayList<>();
                //动作属性
                ExecOpAction opAction = new ExecOpAction();
                opAction.session_type = ExecContext.ENV_CFG_WHERE_LIMIT_IDS;
                opAction.from_op_id = sub_op.id + "_" + node_;
                opAction.before_or_after = ExecContext.OP_ST_AFTER_SUB_COMPLETE_FINISHED_BEFORE_PRI_COMPLETE_START;
                opAction.from_op_db_type = ExecContext.OP_TP_COMPLETE;
                opAction.from_table_id = priTbl.id;
                priOp.opActions.add(opAction);
                //添加会话
                execContext.addContexts(distNodeRows,
                        sub_op.id + "_" + node_, ExecContext.OP_ST_AFTER_SUB_COMPLETE_FINISHED_BEFORE_PRI_COMPLETE_START,
                        sub_op.table_id, ExecContext.OP_TP_COMPLETE, null);
                //添加会话
                execContext.addContextsForeignTableIds(distNodeRows,
                        sub_op.id + "_" + node_, ExecContext.OP_ST_AFTER_SUB_COMPLETE_FINISHED_BEFORE_PRI_COMPLETE_START,
                        sub_op.table_id, ExecContext.OP_TP_RELATED_FOREIGN);
                //取数满足要求的主流程行
                Complete(priOp, execContext, userAction, execResult);
            }
        }
    }

    /**
     * 执行某个环节的事件，要求afterRows行的node_一致
     */
    public void HandleNodeEventSQL(String sub_tbl_id, List<Map> subAfterRows,
                                   ExecOp sub_op, ExecContext execContext, UserAction userAction) throws Exception {
        if (subAfterRows == null || subAfterRows.isEmpty())
            return;
        Map row = subAfterRows.get(0);
        String pri_tbl_id = (String) row.get("pri_tbl_");
        String pri_tbl_node_ = (String) row.get("pri_tbl_node_");
        if (pri_tbl_id == null || pri_tbl_node_ == null)  //无主表信息，则不执行
            return;
        Integer pri_tbl_node_enter_times_ = (Integer) row.get("pri_tbl_node_enter_times_");
        List pri_ids = subAfterRows.stream().map(r -> r.get("z_" + pri_tbl_id + "_id")).distinct().collect(Collectors.toList());
        String sub_tbl_node_ = (String) row.get("node_");
        List sub_ids = subAfterRows.stream().map(r -> r.get("id_")).collect(Collectors.toList());
        Table subTbl = (Table) Modules.getInstance().get(sub_tbl_id, false);
        List<FlowNodeEvent> events = subTbl.getNode(sub_tbl_node_).events.stream().filter(e -> e.pri_node == null || Objects.equals(e.pri_node, pri_tbl_node_)).collect(Collectors.toList());
        for (FlowNodeEvent event : events) {
            String sql_str = Lutils.nvl(event.sql_str, "").trim();
            if (sql_str.length() == 0)
                continue;
            //子流程的事件，id范围应该是子流程的ids和行内的ids
            List list = execContext.getContextRows(sub_op.id, ExecContext.OP_ST_AFTER, null, null);
            ExecContext execContext_s = new ExecContext();
            execContext_s.addContexts(list);
            sql_str = CompUtils.replaceSql_z_table_ids(sql_str, execContext_s.getWhereIdsMap(), userAction);
            Map params = Lutils.genMap(
                    "pri_tbl_", pri_tbl_id,
                    "pri_tbl_node_", pri_tbl_node_,
                    "pri_tbl_node_enter_times_", pri_tbl_node_enter_times_,
                    "sub_tbl_", sub_tbl_id,
                    "sub_tbl_node_", sub_tbl_node_
            );
            db.executeSql(sql_str, params);
        }
        //判断是否完成
//        else if(Objects.equals(sql_type,"if_finished") && sql_str.length()>0) {
//            //判断流程是否执行完毕
//            List finishedExists = null;
//            if(sql_str.trim().length()>0){
//                sql_str=sql_str.replace("...z_table" + pri_tbl_ + "_ids",  String.join(",", pri_ids));
//                sql_str=sql_str.replace("...z_table" + sub_tbl_ + "_ids", StringUtils.join(sub_ids, ","));
//                finishedExists = baseDBService.querySql(sql_str, params);
//            }
//            if(finishedExists == null){
//                throw new Exception("子流程汇聚条件设置不正确");
//            }
//            //子流程执行完
//            if(finishedExists.size()>0) {
//                Map pri_row = baseDBService.selectOne("z_table"+pri_table_id, Lutils.genMap("id_", pri_id_));
//                //执行下一步
//                Map flow_edge = FlowStore.getInstance().getNodeFirstOutEdge((Integer)pri_row.get("node_"));
////                        businessMapper.get_default_flow_of_node(Lutils.genMap("op_table_id", pri_table_id, "ids", Lutils.genList(pri_id_)));
//                Map priNextOp = Lutils.genMap("sessionSettings", Lutils.genList(Lutils.genMap("session_type", "where-limit-ids",
//                                "from_op_id", op.get("id"),
//                                "from_op_db_type", "foreign-update",
//                                "from_table_id", pri_table_id,
//                                "before_or_after", "after")));
//                priNextOp.put("id", op.get("id"));
//                priNextOp.put("flow_edge_id", flow_edge.get("id"));
//                priNextOp.put("flow_edge_obj", flow_edge);
//                priNextOp.put("table_id", pri_table_id);
//                priNextOp.put("op_type", "background-countersign-complete");
//                userAction.put("action_type", "background-countersign-complete");
//                RowsComplete(sessionContextList, priNextOp, userAction, submitReceivers);
//            }
    }

//    public void handleNodeHandleInfo(Map subCountInfo,Object pri_table_id, Integer pri_id_,
//                                     Object sub_table_id, Object when_sub_node,
//                                     Integer pri_node_times){
//        //判断当前节点是否在选中的环节中，如若在，则计算环节的处理情况
//        List<Map> handle_info = businessMapper.get_sub_flow_handle_info(Lutils.genMap("pri_table_id", pri_table_id, "sub_table_id", sub_table_id,
//                "pri_ids", Lutils.genList(pri_id_), "when_sub_node", when_sub_node, "pri_node_times", pri_node_times));
//        //进入环节人数
//            subCountInfo.put("cur_node_z_table"+sub_table_id+"_total", handle_info.size());
//            subCountInfo.put("cur_node_z_table"+sub_table_id+"_finished", handle_info.stream().filter(o -> o.get("finished_time_") != null).collect(Collectors.toList()).size());
//
//        if(subCountInfo.size()>0) {
//            int updateCount = baseDBService.updateEq("z_table" + pri_table_id, subCountInfo, Lutils.genMap("id_", pri_id_));
//        }
//    }


    public Map get_comp_log_cfg(String id) {
        Map r1 = new HashMap();
        List<Map> this_table_cfg = new ArrayList<>();
        List<Map> huiqian_table_cfg = new ArrayList<>();
        List<Map> log_cfg_list = new ArrayList<>();
        CompLogTable o = (CompLogTable) Modules.getInstance().get(id, true);
        Integer obj_id = (Integer) log_cfg_list.get(0).get("id");
        Map map = DSStore.getInstance().get("comp_log" + obj_id);
        //根据comp_log 取comp_huiqian

        Map map2 = DSStore.getInstance().get("comp_huiqian" + obj_id);
        r1.put("this_table_cfg", map);
        r1.put("huiqian_table_cfg", map2);
        return r1;
    }

    public List<Map> get_table_log_data(List<Map> sessionList, Map thisTableCfg) {
        if (thisTableCfg == null || thisTableCfg.isEmpty()) return null;
        Integer table_id = (Integer) thisTableCfg.get("table_id");
        Map session = null;
        for (int i = 0; i < sessionList.size(); i++) {
            Map session1 = (Map) sessionList.get(i);
            if (session1.get("table_id").equals(table_id)) {
                session = session1;
            }
        }
        List<Map> thisLogTableData = businessMapper.get_this_log_table_data(Lutils.genMap("table_id", table_id,
                "id_", session.get("id_"), "dataTableCols", (List) thisTableCfg.get("fields")));
        return thisLogTableData;
    }

    public Map get_table_log_detail(Map activity, List<Map> sessionList, Map thisTableCfg, Map huiqianTableCfg) {
        //根据ds获取本表log信息
        if (thisTableCfg == null || thisTableCfg.isEmpty()) return null;
        List<Map> thishuiqianLogTableData = new ArrayList<>();
        List<Map> thisLogTableData = new ArrayList<>();

        Integer table_id = (Integer) thisTableCfg.get("table_id");
        Integer log_id = (Integer) activity.get("id_");
        Map session = null;
        for (int i = 0; i < sessionList.size(); i++) {
            Map session1 = (Map) sessionList.get(i);
            if (session1.get("table_id").equals(table_id)) {
                session = session1;
            }
        }
        List<Map> logInfos = businessMapper.get_log_view_id_by_action_id
                (Lutils.genMap("table_id", table_id, "log_id", log_id));
        Integer view_id = logInfos.size() > 0 ? (Integer) logInfos.get(0).get("view_id") : -1;
        //获取v_view树
        List<Map> views = businessMapper.selectViewTreeList(Lutils.genMap("view_id", view_id));
        List value_render_obj_ids = Lutils.getColumnValueList(views, "id");
        List<Map> fields = businessMapper.get_col_fieds_by_editor_obj_ids(Lutils.genMap("obj_ids", value_render_obj_ids));
        List<Map> fields1 = (List) thisTableCfg.get("fields");
        fields1.addAll(fields);
        thisLogTableData = businessMapper.get_this_log_table_detail(Lutils.genMap("table_id", table_id,
                "log_id", log_id, "dataTableCols", fields1));
        //判断是否是会签节点
        Integer node_ = (Integer) activity.get("node_");
        List<Map> maps = db.selectEq("v_flow_node", Lutils.genMap("id", node_, "type", "sub-flow"));
        if (maps.size() > 0) {
            Map map = maps.get(0);
            if (map.containsKey("sub_flow_table_id")) {
                Integer sub_flow_table_id = (Integer) map.get("sub_flow_table_id");
                //有会签数据
                thishuiqianLogTableData = businessMapper.get_huiqian_log_table_data(Lutils.genMap("table_id", table_id,
                        "huiqian_table_id", (Integer) huiqianTableCfg.get("table_id"),
                        "id_", (Integer) session.get("id_"), "dataTableCols", (List) huiqianTableCfg.get("fields")));


            }
        }
        return Lutils.genMap("this_data", thisLogTableData, "huiqian_data", thishuiqianLogTableData, "comp_edge_log_field_data", fields);
    }

    //执行终止条件SQL，只有select语句
    public void executeStop(ExecOp op1, List<Map> submit_list) throws Exception {
        //获取递归执行清单
        boolean pureSelect = Lutils.isPureSelect(op1.stop_sql);
        if (!pureSelect) {
            throw new Exception("错误信息：【终止配置错误】");
        }
        String true_stop_sql = SqlPlaceholder.replace(op1.stop_sql, submit_list, null);
        List<Map> maps = db.selectSql(true_stop_sql);
        if (maps.size() > 0) {
            Boolean flag = (Boolean) maps.get(0).get("is_true");
            if (!flag) {
                throw new Exception(maps.get(0).get("message").toString());
            }
        } else {
            throw new Exception("终止查询为空，配置错误");
        }
    }


    public void test() throws JsonProcessingException {
        String json = "{\n" +
                "\"viewBtns\":[],\n" +
                "\"compDataSource\":\n" +
                "{\"data_access_scope\":\"all\",\n" +
                "\"data_type\":\"table\",\n" +
                "\"id\": \"ds382\",\n" +
                "\"table_id\": \"table154\",\n" +
                "\"fields\":\n" +
                "[\n" +
                "{\"field_type\":\"ds_total\"},\n" +
                "{\"field_type\":\"ds_rows_length\"}\n" +
                "]},\n" +
                "\"view_type\":\"comp\",\n" +
                "\"col_span\":3,\n" +
                "\"is_show\":true,\n" +
                "\"viewTitleRightBtns\":[],\n" +
                "\"gutter\":16,\n" +
                "\"comp_name\":\"CompDataSource\",\n" +
                "\"name\":\"数据源\",\n" +
                "\"id\":\"view3596\"\n" +
                "}";
        ObjectMapper mapper = new ObjectMapper();
        View obj = mapper.readValue(json, View.class);
        String json1 = mapper.writeValueAsString(obj);
        System.out.println(json1);
    }


    public void convert() {
        List<Map> views = db.selectByCauses("v_view", null, null);
        for (Map v : views) {
            if (v.get("view_type") == null || (v.get("view_type").equals("comp") && v.get("comp_name") == null)) {
                continue;
            }
            if (v.get("view_type").equals("drawer") || v.get("view_type").equals("modal") || v.get("view_type").equals("indexTab")) {
                //获取事件清单
//                List<Map> events = baseDBService.selectEq("v_comp_events", Lutils.genMap("view_id", v.get("id")));
//                List<Map> listeners = baseDBService.selectEq("v_comp_listener", Lutils.genMap("belong_view_id", v.get("id")));
//                v.put("events", events);
//                v.put("listeners", listeners);
            } else if (v.get("view_type").equals("comp") && v.get("comp_name").equals("CompGrid")) {
                v.put("compGrid", CompGridStore.getInstance().getSimplify("view" + v.get("id")));
            }
//            else if (v.get("view_type").equals("comp") && v.get("comp_name").equals("CompValueRender")) {
//                //获取ValueRender组件属性
//                v.put("compValueRender", CompValueRenderStore.getInstance().get("view"+v.get("id")));
//            }
//            else if (v.get("view_type").equals("comp") && v.get("comp_name").equals("CompValueEditor")) {
//                v.put("compValueEditor", CompValueEditorStore.getInstance().get("view"+v.get("id")));
//            }
//            else if (v.get("view_type").equals("comp") && v.get("comp_name").equals("CompUserSelector")) {
//                //获取ValueRender组件属性
//                v.put("compUserSelector", CompUserSelectorStore.getInstance().get("view"+v.get("id")));
//            }
//            else if (v.get("view_type").equals("comp") && v.get("comp_name").equals("CompDataSource")) {
//                v.put("compDataSource", CompDataSourceStore.getInstance().get("view"+v.get("id")));
//            }
//            else if (v.get("view_type").equals("comp") && v.get("comp_name").equals("CompCountAggr")) {
//                v.put("compCountAggr", CompCountAggrStore.getInstance().get("view"+v.get("id")));
//            }
//            else if (v.get("view_type").equals("comp") && v.get("comp_name").equals("CompCarousel")) {
//                v.put("compCarousel", CompCarouselStore.getInstance().get("view"+v.get("id")));
//            }
//            else if (v.get("view_type").equals("comp") && v.get("comp_name").equals("CompMapView")) {
//
//            }
//            else if (v.get("view_type").equals("comp") && v.get("comp_name").equals("CompReportForms")) {
//                Map r1 = CompGridStore.getInstance().get("view" + v.get("id"));
//                v.put(v.get("comp_name"), r1);
//            }
//            else if (v.get("view_type").equals("comp") && v.get("comp_name").equals("CompLogTable")) {
//                Map r1 = get_comp_log_cfg((Integer) v.get("id"));
//                v.put(v.get("comp_name"), r1);
//            }
//            else if (v.get("view_type").equals("comp") && v.get("comp_name").equals("CompEcharts")) {
////                List<Map> comps = serviceConfigMapper.get_comp_echarts_cfg(Lutils.genMap("obj_id", v.get("id"), "obj_type", "view"));
////                if (comps != null && comps.size() > 0) {
////                    Map comp = comps.get(0);
////                    comp.put("ds", CompDataSourceStore.getInstance().get(comp.get("ds_id")));
////                    //在视图中添加组件
////                    v.put(v.get("comp_name"), comp);
////                    v.put("comp", comp);
//                List<Map> maps = baseDBService.selectEq("v_comp_echarts", Lutils.genMap("obj_id", v.get("id")));
//                String echartsType = maps.get(0).get("echarts_type").toString();
//                if(echartsType.equals("gantt")){
//                    List<Map> comps = serviceConfigMapper.get_comp_gantt_echarts_cfg(Lutils.genMap("obj_id", v.get("id"), "obj_type", "view"));
//                    if (comps != null && comps.size() > 0) {
//                        Map comp = comps.get(0);
//                        comp.put("ds", CompDataSourceStore.getInstance().get(comp.get("ds_id")));
//                        //在视图中添加组件
//                        v.put(v.get("comp_name"), comp);
//                        v.put("comp", comp);
//                    }
//                }else{
//                    List<Map> comps = serviceConfigMapper.get_comp_echarts_cfg(Lutils.genMap("obj_id", v.get("id"), "obj_type", "view"));
//                    if (comps != null && comps.size() > 0) {
//                        Map comp = comps.get(0);
//                        comp.put("ds", CompDataSourceStore.getInstance().get(comp.get("ds_id")));
//                        //在视图中添加组件
//                        v.put(v.get("comp_name"), comp);
//                        v.put("comp", comp);
//                    }
//                }
//            }
//            else if (v.get("view_type").equals("comp") && v.get("comp_name").equals("CompCard")) {
//                v.put(v.get("comp_name"), CompCardStore.getInstance().get("view"+v.get("id")));
//            }
//            else if (v.get("view_type").equals("comp") && v.get("comp_name").equals("CompTimeline")) {
//                v.put(v.get("comp_name"), CompTimelineStore.getInstance().get("view"+v.get("id")));
//            }
            v.put("viewBtns", ExecObjStore.getInstance().getByObj("view_btn" + v.get("id")));
            v.put("viewTitleRightBtns", ExecObjStore.getInstance().getByObj("view_title_btn" + v.get("id")));
            if (!(v.get("view_type").equals("comp") && v.get("comp_name").equals("CompGrid"))) {
                continue;
            }
            ObjectMapper mapper = new ObjectMapper();
            try {
                String json = mapper.writeValueAsString(v);
                View obj = mapper.readValue(json, View.class);
                String json1 = mapper.writeValueAsString(obj);
                System.out.println("---------------------view:" + v.get("id") + " ------------------");
                System.out.println(json);
                db.insertWhenNotExistUpdateWhenExists("v_module", Lutils.genMap("json", json1, "id", "view" + v.get("id"), "type", "View"),
                        Lutils.genMap("id", "view" + v.get("id")));
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public List<Map> getViewsFromParent(Integer parent_view_id) {
        //从view往下，递归取view，先全部取出来，然后构建树形结构，当节点没有权限时，终止该节点。
        List<Map> views = businessMapper.getViewRecrusive(Lutils.genMap("view_id", parent_view_id));
        //view 的id ，用于后面查询组件
        List<Integer> viewIds = views.stream().map(o -> (Integer) o.get("id")).collect(Collectors.toList());
        //获取视图中，组件的配置
        for (Map v : views) {
            if (v.get("view_type").equals("drawer") || v.get("view_type").equals("modal") || v.get("view_type").equals("indexTab")) {
                //获取事件清单
                List<Map> events = db.selectEq("v_comp_events", Lutils.genMap("view_id", v.get("id")));
                List<Map> listeners = db.selectEq("v_comp_listener", Lutils.genMap("belong_view_id", v.get("id")));
                v.put("events", events);
                v.put("listeners", listeners);
            } else if (v.get("view_type").equals("comp") && v.get("comp_name").equals("CompGrid")) {
                v.put("compGrid", CompGridStore.getInstance().get("view" + v.get("id")));
            } else if (v.get("view_type").equals("comp") && v.get("comp_name").equals("CompValueRender")) {
                //获取ValueRender组件属性
                v.put("compValueRender", CompValueRenderStore.getInstance().get("view" + v.get("id")));
            } else if (v.get("view_type").equals("comp") && v.get("comp_name").equals("CompValueEditor")) {
                v.put("compValueEditor", CompValueEditorStore.getInstance().get("view" + v.get("id")));
            } else if (v.get("view_type").equals("comp") && v.get("comp_name").equals("CompUserSelector")) {
                //获取ValueRender组件属性
                v.put("compUserSelector", CompUserSelectorStore.getInstance().get("view" + v.get("id")));
            } else if (v.get("view_type").equals("comp") && v.get("comp_name").equals("CompDataSource")) {
                v.put("compDataSource", DSStore.getInstance().get("view" + v.get("id")));
            } else if (v.get("view_type").equals("comp") && v.get("comp_name").equals("CompCountAggr")) {
                v.put("compCountAggr", CompCountAggrStore.getInstance().get("view" + v.get("id")));
            } else if (v.get("view_type").equals("comp") && v.get("comp_name").equals("CompCarousel")) {
                v.put("compCarousel", CompCarouselStore.getInstance().get("view" + v.get("id")));
            } else if (v.get("view_type").equals("comp") && v.get("comp_name").equals("CompMapView")) {

            } else if (v.get("view_type").equals("comp") && v.get("comp_name").equals("CompReportForms")) {
                Map r1 = CompGridStore.getInstance().get("view" + v.get("id"));
                v.put(v.get("comp_name"), r1);
            } else if (v.get("view_type").equals("comp") && v.get("comp_name").equals("CompLogTable")) {
                Map r1 = get_comp_log_cfg(v.get("id").toString());
                v.put(v.get("comp_name"), r1);
            } else if (v.get("view_type").equals("comp") && v.get("comp_name").equals("CompEcharts")) {
//                List<Map> comps = serviceConfigMapper.get_comp_echarts_cfg(Lutils.genMap("obj_id", v.get("id"), "obj_type", "view"));
//                if (comps != null && comps.size() > 0) {
//                    Map comp = comps.get(0);
//                    comp.put("ds", CompDataSourceStore.getInstance().get(comp.get("ds_id")));
//                    //在视图中添加组件
//                    v.put(v.get("comp_name"), comp);
//                    v.put("comp", comp);
                List<Map> maps = db.selectEq("v_comp_echarts", Lutils.genMap("obj_id", v.get("id")));
                String echartsType = maps.get(0).get("echarts_type").toString();
                if (echartsType.equals("gantt")) {
                    List<Map> comps = serviceConfigMapper.get_comp_gantt_echarts_cfg(Lutils.genMap("obj_id", v.get("id"), "obj_type", "view"));
                    if (comps != null && comps.size() > 0) {
                        Map comp = comps.get(0);
                        comp.put("ds", DSStore.getInstance().get(comp.get("ds_id")));
                        //在视图中添加组件
                        v.put(v.get("comp_name"), comp);
                        v.put("comp", comp);
                    }
                } else {
                    List<Map> comps = serviceConfigMapper.get_comp_echarts_cfg(Lutils.genMap("obj_id", v.get("id"), "obj_type", "view"));
                    if (comps != null && comps.size() > 0) {
                        Map comp = comps.get(0);
                        comp.put("ds", DSStore.getInstance().get(comp.get("ds_id")));
                        //在视图中添加组件
                        v.put(v.get("comp_name"), comp);
                        v.put("comp", comp);
                    }
                }
            } else if (v.get("view_type").equals("comp") && v.get("comp_name").equals("CompCard")) {
                v.put(v.get("comp_name"), CompCardStore.getInstance().get("view" + v.get("id")));
            } else if (v.get("view_type").equals("comp") && v.get("comp_name").equals("CompTimeline")) {
                v.put(v.get("comp_name"), CompTimelineStore.getInstance().get("view" + v.get("id")));
            }
            v.put("viewBtns", ExecObjStore.getInstance().getByObj("view_btn" + v.get("id")));
            v.put("viewTitleRightBtns", ExecObjStore.getInstance().getByObj("view_title_btn" + v.get("id")));
        }
        return views;
    }

    public void convertTable() {
        List<Map> tables = db.selectEq("v_table", Lutils.genMap("node_type", "table", "is_deleted", false));
        for (Map t : tables) {
            Map map = TableStore.getInstance().get(t.get("id"));
            ObjectMapper mapper = new ObjectMapper();
            try {
                String json = mapper.writeValueAsString(map);
                Table tbl = mapper.readValue(json, Table.class);
                tbl.name = (String) map.get("table_display_name");
                tbl.id = "table" + tbl.id;
                for (int i = 0; i < tbl.priTableIds.size(); i++) {
                    tbl.priTableIds.set(i, "table" + tbl.priTableIds.get(i));
                }
                for (int i = 0; i < tbl.subTableIds.size(); i++) {
                    tbl.subTableIds.set(i, "table" + tbl.subTableIds.get(i));
                }
                //处理列id
//                for(int i=0;i<tbl.cols.size();i++){
//                    tbl.cols.get(i).id= tbl.cols.get(i).id;
//                }
                System.out.println("---------------------Table:" + tbl.id + " ------------------");
                //设置流程信息
                Map flow = FlowStore.getInstance().get1(map.get("id"));
                Table ft = Lutils.ObjToClass(flow, Table.class);
                if (ft != null) {
                    tbl.edges = ft.edges;
                    tbl.nodes = ft.nodes;
                    for (FlowEdge fe : tbl.edges) {
                        fe.src = "node" + fe.src;
                        fe.dst = "node" + fe.dst;
                        //处理人员范围
                        if (Objects.equals(fe.assign_type, "defined-role")
                                || Objects.equals(fe.assign_type, "manual")
                                || Objects.equals(fe.assign_type, "insert-rows-with-defined-roles-users")
                                || Objects.equals(fe.assign_type, "insert-rows-with-manual-receivers")
                        ) {
                            Map usMap = UserScopeStore.getInstance().get("edge-assign-scope" + fe.id);
                            fe.userScope = new UserScope();
                            convertUserScope(fe.userScope, usMap);
                        }
                        fe.id = "edge" + fe.id;
                    }
                    for (FlowNode fe : tbl.nodes) {
                        fe.id = "node" + fe.id;
                        if (fe.sub_flow_table_id != null)
                            fe.sub_flow_table_id = "table" + fe.sub_flow_table_id;
                        for (FlowNodeEvent event : fe.events) {
                            event.pri_table_id = "table" + event.pri_table_id;
                            event.pri_node = "node" + event.pri_node;
                            event.sub_table_id = "table" + event.sub_table_id;
                            event.when_sub_node = "node" + event.when_sub_node;
                        }
                    }
                }
                Modules.getInstance().create(tbl.id, "Table", tbl);
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public void convertViews(){
        //处理视图类型，非文件夹类型
//        List<Map> views = serviceConfigMapper.get_view_for_module(null);
        List<Map> views = businessMapper.getViewRecrusiveByParent(Lutils.genMap("parent_id", 0));
//        List<Map> views1 = businessMapper.getViewRecrusive(Lutils.genMap("view_id", 170));
//        List<Map> views2 = businessMapper.getViewRecrusive(Lutils.genMap("view_id", 118));
//        List<Map> views3 = businessMapper.getViewRecrusive(Lutils.genMap("view_id", 125));
//        views.addAll(views1);
//        views.addAll(views2);
//        views.addAll(views3);
        for(Map v: views){
            Map map = ViewStore.getInstance().get(v.get("id"));
            View vi = Lutils.ObjToClass(map, View.class);
            vi.show_title=Lutils.nvl(vi.show_title,false);
            //按钮
            List<Map> btn = ExecObjStore.getInstance().getByObj("view_btn"+vi.id);
            for(Map b: btn){
                Exec exec = Lutils.ObjToClass(b, Exec.class);
                convertExec(exec);
                vi.viewBtns.add(exec);
            }
            //面板按钮
            List<Map> btn1 = ExecObjStore.getInstance().getByObj("view_title_btn"+vi.id);
            for(Map b: btn1){
                Exec exec = Lutils.ObjToClass(b, Exec.class);
                convertExec(exec);
                vi.viewTitleBtns.add(exec);
            }
            System.out.println("view_id:"+vi.id);
            //处理组件
            if(vi.view_type.equals("comp")&&vi.comp_name!=null){
                if(vi.comp_name.equals("CompGrid")){
                    Map gridMap = CompGridStore.getInstance().get("view"+vi.id);
                    CompGrid grid = Lutils.ObjToClass(gridMap, CompGrid.class);
                    if(grid!=null) {
                        convertCompGrid(grid);//先看一下原表配置情况
                        System.out.println("view_id:" + vi.id + "---》grid.id:" + grid.id);
                        vi.comp_id = grid.id;
                        Modules.getInstance().create(grid.id, "CompGrid", grid);
                    }
                }
                else if(vi.comp_name.equals("CompDataSource")){
                    //处理数据源
                    Map dsMap = DSStore.getInstance().get("view"+vi.id);
                    CompDataSource ds = Lutils.ObjToClass(dsMap, CompDataSource.class);
                    convertDataSource(ds);
                    //数据源组件值保存id，不保存对象
                    vi.comp_id = ds.id;
                    Modules.getInstance().create(ds.id, "CompDataSource", ds);
                }
                else if(vi.comp_name.equals("CompCarousel")){
                    Map carMap = CompCarouselStore.getInstance().get("view"+vi.id);
                    CompCarousel carousel = Lutils.ObjToClass(carMap, CompCarousel.class);
                    Map dsMap = DSStore.getInstance().get("comp_carousel"+carousel.id);
                    CompDataSource ds = Lutils.ObjToClass(dsMap, CompDataSource.class);
                    convertDataSource(ds);
                    carousel.ds_id = ds.id;
                    Modules.getInstance().create(ds.id, "CompDataSource", ds);
                    carousel.id="CompCarousel"+carousel.id;
                    vi.comp_id = carousel.id;
                    Map execMap = (Map)carMap.get("execObj");
                    Exec exec = Lutils.ObjToClass(execMap, Exec.class);
                    convertExec(exec);
                    carousel.exec=exec;
                    Modules.getInstance().create(carousel.id, "CompCarousel", carousel);
                }
                else if(vi.comp_name.equals("CompCountAggr")){
                    Map compMap = CompCountAggrStore.getInstance().get("view"+vi.id);
                    CompCountAggr compCountAggr = Lutils.ObjToClass(compMap, CompCountAggr.class);
                    convertCompCountAggr(compCountAggr);
                    vi.comp_id = compCountAggr.id;
                    Map execMap = (Map)compMap.get("execObj");
                    Exec exec = Lutils.ObjToClass(execMap, Exec.class);
                    convertExec(exec);
                    compCountAggr.exec=exec;
                    Modules.getInstance().create(compCountAggr.id, "CompCountAggr", compCountAggr);
                }
                else if(vi.comp_name.equals("CompValueRender")){
                    Map compMap = CompValueRenderStore.getInstance().get("view"+vi.id);
                    CompValueRender comp = Lutils.ObjToClass(compMap, CompValueRender.class);
                    if(comp!=null) {
                        convertCompValueRender(comp);
                        Map execMap = (Map) compMap.get("exec");
                        Exec exec = Lutils.ObjToClass(execMap, Exec.class);
                        if (exec == null) {
                            exec = new Exec();
                            exec.create();
                        }
                        convertExec(exec);
                        comp.exec = exec;
                        Modules.getInstance().create(comp.id, "CompValueRender", comp);
                    }
                    else{
                        comp = new CompValueRender();
                        comp.create("CompValueRender"+Constants.getTimeFormatId());
                    }
                    vi.comp_id = comp.id;
                }
                else if(vi.comp_name.equals("CompValueEditor")){
                    Map compMap = CompValueEditorStore.getInstance().get("view"+vi.id);
                    CompValueEditor comp = Lutils.ObjToClass(compMap, CompValueEditor.class);
                    //外联选择器，需重新保存表格
                    if(comp.editor_type.equals("foreign-key-editor")){
                        Map gridMap = CompGridStore.getInstance().get("comp_value_editor"+comp.id);
                        CompGrid grid = Lutils.ObjToClass(gridMap, CompGrid.class);
                        convertCompGrid(grid);
                        comp.grid_id = grid.id;
                        comp.compGrid = grid;
                        Modules.getInstance().create(grid.id, "CompGrid", grid);
                    }
                    convertCompValueEditor(comp);
                    vi.comp_id = comp.id;
                    Modules.getInstance().create(comp.id, "CompValueEditor", comp);
                }
                else if(vi.comp_name.equals("CompUserSelector")){
                    Map compMap = CompUserSelectorStore.getInstance().get("view"+vi.id);
                    CompUserSelector comp = Lutils.ObjToClass(compMap, CompUserSelector.class);
                    Map usMap = UserScopeStore.getInstance().get("view"+vi.id);
                    comp.userScope = new UserScope();
                    convertUserScope(comp.userScope, usMap);
                    comp.id = "CompUserSelector"+comp.id;
                    vi.comp_id = comp.id;
                    Modules.getInstance().create(comp.id, "CompUserSelector", comp);
                }
                else if(vi.comp_name.equals("CompCard")){
                    Map compMap = CompCardStore.getInstance().get("view"+vi.id);
                    CompCard comp = Lutils.ObjToClass(compMap, CompCard.class);
                    comp.exec = new Exec();
                    List<Map> mapExecs = ExecObjStore.getInstance().getByObj("comp_card"+comp.id);
                    if(mapExecs.size()>0) {
                        Map exec = mapExecs.get(0);
                        comp.exec = Lutils.ObjToClass(exec, Exec.class);
                        convertExec(comp.exec);
                    }
                    comp.id = "CompCard" + comp.id;
                    comp.ds_id = "CompDataSource" + comp.ds_id;
                    vi.comp_id =  comp.id;
                    Modules.getInstance().create(comp.id, "CompCard", comp);
                }
                else if(vi.comp_name.equals("CompEcharts")){
                    List<Map> maps = db.selectEq("v_comp_echarts", Lutils.genMap("obj_id", v.get("id")));
                    if(maps.size()>0) {
                        String echartsType = maps.get(0).get("echarts_type").toString();
                        if (echartsType.equals("gantt")) {
                            List<Map> comps = serviceConfigMapper.get_comp_gantt_echarts_cfg(Lutils.genMap("obj_id", v.get("id"), "obj_type", "view"));
                            if (comps != null && comps.size() > 0) {
                                Map compMap = comps.get(0);
                                compMap.put("ds_id", "CompDataSource" + compMap.get("ds_id"));
                                //在视图中添加组件
                                CompEcharts comp = Lutils.ObjToClass(compMap, CompEcharts.class);
                                comp.id = "CompEcharts" + comp.id;
                                vi.comp_id = comp.id;
                                Modules.getInstance().create(comp.id, "CompEcharts", comp);
                            }
                        } else {
                            List<Map> comps = serviceConfigMapper.get_comp_echarts_cfg(Lutils.genMap("obj_id", v.get("id"), "obj_type", "view"));
                            if (comps != null && comps.size() > 0) {
                                Map compMap = comps.get(0);
                                compMap.put("ds_id", "CompDataSource" + compMap.get("ds_id"));
                                //在视图中添加组件
                                CompEcharts comp = Lutils.ObjToClass(compMap, CompEcharts.class);
                                comp.id = "CompEcharts" + comp.id;
                                vi.comp_id = comp.id;
                                Modules.getInstance().create(comp.id, "CompEcharts", comp);
                            }
                        }
                    }
                }
                else if(vi.comp_name.equals("CompTimeline")){
                    Map compMap = CompTimelineStore.getInstance().get("view"+v.get("id"));
                    CompTimeline comp = Lutils.ObjToClass(compMap, CompTimeline.class);
                    comp.exec = new Exec();
                    List<Map> mapExecs = ExecObjStore.getInstance().getByObj("comp_time_line"+comp.id);
                    if(mapExecs.size()>0) {
                        Map exec = mapExecs.get(0);
                        comp.exec = Lutils.ObjToClass(exec, Exec.class);
                        convertExec(comp.exec);
                    }
                    comp.id = "CompTimeline" + comp.id;
                    vi.comp_id =  comp.id;
                    Modules.getInstance().create(comp.id, "CompTimeline", comp);
                }
                else if (vi.comp_name.equals("CompLogTable")) {
//                        Map compMap = get_comp_log_cfg(v.get("id").toString());
//                        CompLogTable comp = Lutils.ObjToClass(compMap, CompLogTable.class);
//                        //处理数据源
//                        Map dsMap = DSStore.getInstance().get("comp_log" + comp.id);
//                        if(dsMap!=null) {
//                            CompDataSource ds = Lutils.ObjToClass(dsMap, CompDataSource.class);
//                            if (ds != null) {
//                                convertDataSource(ds);
//                                comp.ds_id = ds.id;
//                                Modules.getInstance().create(ds.id, "CompDataSource", ds);
//                                comp.ds_id = "CompDataSource" + ds.id;
//                            } else {
//                                ds = new CompDataSource();
//                                ds.create("CompDataSource" + Constants.getTimeFormatId());
//                                comp.ds_id = ds.id;
//                            }
//                        }
//                        else{
//                            CompDataSource ds = new CompDataSource();
//                            ds.create("CompDataSource" + Constants.getTimeFormatId());
//                            comp.ds_id = ds.id;
//                        }
//                        comp.id = "CompLogTable" + comp.id;
//                        Modules.getInstance().create(comp.id, "CompLogTable", comp);
                }
            }
            vi.comp = null;
            vi.id = "view" + vi.id;
            vi.parent_id= "view"+vi.parent_id;
            System.out.println("insert view:" + vi.id);
            if(vi.id.equals("view797")){
                System.out.println(vi);
            }
            System.out.println("insert json:" +  Lutils.ObjectToJSON(vi));
            Modules.getInstance().create(vi.id,"View", vi);
//                baseDBService.updateEq("v_tree_view", Lutils.genMap("json", Lutils.ObjectToJSON(vi)), Lutils.genMap("id", vi.id));

        }
    }

    public void convertExec(Exec exec) {
        for (ExecOp op : exec.ops) {
            op.view_id = op.view_id != null ? "view" + op.view_id : null;
            op.from_view_id = op.from_view_id != null ? "view" + op.from_view_id : null;
            op.flow_edge_id = op.flow_edge_id != null ? "edge" + op.flow_edge_id : null;
            op.table_id = op.table_id != null ? "table" + op.table_id : null;
            op.from_ds_id = op.from_ds_id != null ? ("CompDataSource" + op.from_ds_id) : null;
            if (op.opActions != null) {
                for (ExecOpAction ac : op.opActions) {
                    ac.from_table_id = "table" + ac.from_table_id;
                }
            }
        }
    }

    public void convertDataSource(CompDataSource ds) {
        for (CompDataSourceField f : ds.fields) {
//                            f.id = "DSF"+f.id;
            f.table_id = f.table_id == null ? null : "table" + f.table_id;

//            f.flow_edge_id = f.flow_edge_id == null ? null : "edge" + f.flow_edge_id;
        }
        ds.id = "CompDataSource" + ds.id;
        ds.table_id = "table" + ds.table_id;
        ds.limit_session_table_id = ds.limit_session_table_id == null ? null : "table" + ds.limit_session_table_id;
    }

    public void convertCompValueRender(CompValueRender comp) {
        if (comp == null)
            return;
        comp.id = "CompValueRender" + comp.id;
        comp.ds_id = comp.ds_id == null ? null : "CompDataSource" + comp.ds_id;
    }

    public void convertCompValueEditor(CompValueEditor comp) {
        if (comp == null)
            return;
        comp.id = "CompValueEditor" + comp.id;
        comp.ds_id = comp.ds_id == null ? null : "CompDataSource" + comp.ds_id;
    }

    public void convertCompCountAggr(CompCountAggr comp) {
        if (comp == null)
            return;
        comp.id = "CompCountAggr" + comp.id;
        comp.ds_id = comp.ds_id == null ? null : "CompDataSource" + comp.ds_id;
    }

    public void convertUserScope(UserScope userScope, Map usMap) {
        if (usMap != null) {
            userScope.is_defined_sql = Lutils.nvl(usMap.get("is_defined_sql"), false);
            userScope.sql_str = (String) usMap.get("sql_str");
            userScope.roles = new ArrayList();
            if (userScope.roles != null) {
                userScope.roles = ((List<Map>) usMap.get("roles")).stream().map(o -> o.get("role_id")).collect(Collectors.toList());
            }
            if (usMap.get("groups") != null) {
                userScope.groups = ((List<Map>) usMap.get("groups")).stream().map(o -> o.get("group_id")).collect(Collectors.toList());
            }
        }
    }

    public void convertCompGrid(CompGrid grid) {
        for (CompGridCol col : grid.gridCols) {
            //处理渲染器
            Map renderMap = CompValueRenderStore.getInstance().get("comp_grid_col" + col.id);
            if (renderMap == null) {
                CompValueRender render = new CompValueRender();
                render.create("CompValueRender" + Constants.getTimeFormatId());
                col.compValueRender = render;
            } else {
                CompValueRender render = Lutils.ObjToClass(renderMap, CompValueRender.class);
                convertCompValueRender(render);
                col.compValueRender = render;
                List<Map> execMap = ExecObjStore.getInstance().getByObj("comp_value_render" + render.id);
                if (execMap.size() > 0) {
                    Exec exec = Lutils.ObjToClass(execMap.get(0), Exec.class);
                    convertExec(exec);
                    render.exec = exec;
                }
            }
            //处理列中的按钮
            col.btns = new ArrayList<>();
            List<Map> gridColExecsMap = ExecObjStore.getInstance().getByObj("comp_grid_col_btn" + col.id);
            for (Map gridColExecMap : gridColExecsMap) {
                Exec exec = Lutils.ObjToClass(gridColExecMap, Exec.class);
                convertExec(exec);
                col.btns.add(exec);
            }
            //列的按钮
            List<Map> cellExecs = ExecObjStore.getInstance().getByObj("comp_grid_col_cell_click" + col.id);
            if (cellExecs != null && cellExecs.size() > 0) {
                Exec exec = Lutils.ObjToClass(cellExecs.get(0), Exec.class);
                convertExec(exec);
                col.exec = exec;
            }
        }
        //处理数据源
        Map dsMap = DSStore.getInstance().get("comp_grid" + grid.id);
        CompDataSource ds = Lutils.ObjToClass(dsMap, CompDataSource.class);
        if (ds != null) {
            convertDataSource(ds);
            grid.ds_id = ds.id;
            Modules.getInstance().create(ds.id, "CompDataSource", ds);
            grid.id = "CompGrid" + grid.id;
        } else {
            ds = new CompDataSource();
            ds.create("CompDataSource" + Constants.getTimeFormatId());
            grid.ds_id = ds.id;
        }
    }

    private Map<String, Object> buildReceiptDataMap(ExecContext execContext_s) {
        Map<String, Object> map = new HashMap<>();
        // 拿row
        List<Map> fds = execContext_s.getTableRows("table2510240918148181");
        if (CollUtil.isNotEmpty(fds)) {
            Map fd = fds.get(0);
            // 拿合同信息
            Integer id = (Integer) fd.get("id_");
            Map rowData = db.selectOne("z_table2510240918148181", Lutils.genMap("id_", id));
            // 拿客户信息
            Map customer = db.selectOne("z_table2510240927368971", Lutils.genMap("id_", rowData.get("z_table2510240927368971_id")));
            // 拿产品信息
            List<Map> products = db.selectEq("z_table2510240926010041", Lutils.genMap("z_table2510240918148181_id", id));
            List<Map> collect = products.stream().filter(product -> Objects.isNull(product.get("time_if_delete_"))).map(product -> {
                Map<String, Object> map1 = db.selectOne("z_table2510271706060341", Lutils.genMap("id_", product.get("z_table2510271706060341_id")));
                Map<String, Object> filter = map1.entrySet().stream().filter(entry -> !entry.getKey().equals("danwei")).collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue()));
                product.putAll(filter);
                return product;
            }).collect(Collectors.toList());
            map.put("contract", rowData);
            map.put("customer", customer);
            map.put("products", collect);
        }
        return map;
    }

    public void SetViewsDsListToNull() {
        List<Map> mainPages = db.selectIn("v_tree_view", "view_type", Lutils.genList("indexTab", "modal", "drawer"));
        for (Map map : mainPages) {
            View root = (View) Modules.getInstance().get(map.get("id"), false);
            List<Map> ll = businessMapper.getViewTree(Lutils.genMap("view_id", map.get("id")));
            List<View> views = new ArrayList<>();
            for (Map node : ll) {
                View v = (View) Modules.getInstance().get(node.get("id"), false);
                views.add(v);
            }
            root.dsList = null;
            Modules.getInstance().save(root.id, root);
        }
    }

    public void SetViewsDsToRootView() {
        List<Map> mainPages = db.selectIn("v_tree_view", "view_type", Lutils.genList("indexTab", "modal", "drawer"));
        for (Map map : mainPages) {
            View root = (View) Modules.getInstance().get(map.get("id"), false);
            List<Map> ll = businessMapper.getViewTree(Lutils.genMap("view_id", map.get("id")));
            List<View> views = new ArrayList<>();
            for (Map node : ll) {
                View v = (View) Modules.getInstance().get(node.get("id"), false);
                views.add(v);
            }
            PutChildrenDSToRootDsList(root, views);
        }
    }


    public boolean PutChildrenDSToRootDsList(View root, List<View> views) {
        if (root.dsList == null || root.dsList.size() == 0) {
            List<CompDataSource> dsList = new ArrayList<>();
            for (View v : views) {
                if (v != null) {
                    if (Objects.equals(v.view_type, "comp") && Objects.equals(v.comp_name, "CompDataSource")) {
                        CompDataSource ds = (CompDataSource) Modules.getInstance().get(v.comp_id, false);
                        if (ds != null) {
                            ds.view_id = v.id;
                            dsList.add(ds);
                        }
                    } else if (Objects.equals(v.view_type, "comp") && Objects.equals(v.comp_name, "CompCarousel")) {
                        CompCarousel comp = (CompCarousel) Modules.getInstance().get(v.comp_id, false);
                        if (comp == null)
                            continue;
                        CompDataSource ds = (CompDataSource) Modules.getInstance().get(comp.ds_id, false);
                        if (ds != null) {
                            ds.view_id = v.id;
                            dsList.add(ds);
                        }
                    } else if (Objects.equals(v.view_type, "comp") && Objects.equals(v.comp_name, "CompGrid")) {
                        CompGrid comp = (CompGrid) Modules.getInstance().get(v.comp_id, false);
                        if (comp == null)
                            continue;
                        CompDataSource ds = (CompDataSource) Modules.getInstance().get(comp.ds_id, false);
                        if (ds != null) {
                            ds.view_id = v.id;
                            dsList.add(ds);
                        }
                    } else if (Objects.equals(v.view_type, "comp") && Objects.equals(v.comp_name, "CompValueEditor")) {
                        CompValueEditor comp = (CompValueEditor) Modules.getInstance().get(v.comp_id, false);
                        if (comp == null)
                            continue;
                        if (Objects.equals(comp.editor_type, "foreign-key-editor")) {
                            CompGrid grid = (CompGrid) Modules.getInstance().get(comp.grid_id, false);
                            CompDataSource ds = (CompDataSource) Modules.getInstance().get(grid.ds_id, false);
                            if (ds != null) {
                                ds.view_id = v.id;
                                dsList.add(ds);
                            }
                        }
                    }
                }
            }
            root.dsList = dsList;
            return true;
        }
        else{
            for(int i=0;i< root.dsList.size();i++){
                CompDataSource ds = (CompDataSource) Modules.getInstance().get(root.dsList.get(i).id, false);
                root.dsList.set(i, ds);
            }
        }
        return false;
    }

    //将视图中的编辑器，绑定到数据源的editor上。
    public void SeEditorViewIdToDsField(List<CompDataSource> dsList, List<View> views) {
        // 1. 建立 field_id -> view_id 的映射表
        Map<String, String> fieldIdToViewIdMap = new HashMap<>();
        for (View v : views) {
            if (!Objects.equals(v.view_type, "comp") ||
                    !Objects.equals(v.comp_name, "CompValueEditor")) {
                continue;
            }
            CompValueEditor edt = (CompValueEditor) Modules.getInstance().get(v.comp_id, false);
            if (edt != null && edt.ds_field_id != null) {
                if(edt.editor_type.equals("location-editor")){
                    fieldIdToViewIdMap.put(edt.lng_ds_field_id, v.id);
                    fieldIdToViewIdMap.put(edt.lat_ds_field_id, v.id);
                }
                else
                    fieldIdToViewIdMap.put(edt.ds_field_id, v.id);
            }
        }

        // 2. 使用映射表快速查找
        for (CompDataSource ds : dsList) {
            for (CompDataSourceField fd : ds.fields) {
                if (fd.id != null) {
                    fd.editor_id = fieldIdToViewIdMap.get(fd.id);
                }
            }
        }
    }

    public void Validate(ExecOp op, ExecContext execContext, UserAction userAction, ExecResult execResult) throws Exception {
        //从数据行中校验。
        //从环境变量中，获取要校验的行
        List<Map> validRows = execContext.getEnvCfgMatchedRows(op, ExecContext.ENV_CFG_CACHE_ROWS);
        //注入变量
        Map<String, Object> cfgVars = execContext.getEnvironmentVariables(op);
        boolean state = true;
        String err_msg = "";
        //如果没有行，则使用环境变量校验
        if(validRows.isEmpty()){
            for (Map sc : op.scripts) {
                String script = sc.get("script").toString();
                String msg = sc.get("msg").toString();
                Expression expression = AviatorEvaluator.compile(script, true);
                //脚本中涉及的变量名称
                List<String> expVarNames = expression.getVariableFullNames();
                Map<String, Object> scriptVars = new HashMap<>();
                for (String varName : expVarNames) {
                    //如果环境变量中存在该参数，则以环境变量为准，覆盖行内参数
                    if (cfgVars.containsKey(varName)) {
                        scriptVars.put(varName, cfgVars.get(varName));
                    }
                }
                Boolean script_rs = (Boolean) expression.execute(scriptVars);
                if (Objects.equals(script_rs, false)) {
                    state = false;
                    err_msg += "，" + msg+" \n";
                    execResult.addResult(new ExecOpResult( op.id,  0, false, msg, null));
                }
                else{
                    execResult.addResult(new ExecOpResult( op.id,  0, true, ExecContext.VLD_SUCCESS, null));
                }
            }
        }
        else {
            for (int i = 0; i < validRows.size(); i++) {
                Map validRow = validRows.get(i);
                for (Map sc : op.scripts) {
                    String script = sc.get("script").toString();
                    String msg = sc.get("msg").toString();
                    Expression expression = AviatorEvaluator.compile(script, true);
                    //脚本中涉及的变量名称
                    List<String> expVarNames = expression.getVariableFullNames();
                    Map<String, Object> scriptVars = new HashMap<>();
                    for (String varName : expVarNames) {
                        scriptVars.put(varName, validRow.get(varName));
                        //如果环境变量中存在该参数，则以环境变量为准，覆盖行内参数
                        if (cfgVars.containsKey(varName)) {
                            scriptVars.put(varName, cfgVars.get(varName));
                        }
                    }
                    Boolean script_rs = (Boolean) expression.execute(scriptVars);
                    if (Objects.equals(script_rs, false)) {
                        state = false;
                        err_msg += "，" + msg+" \n";
                        execResult.addResult(new ExecOpResult( op.id,  i, false, msg, null));
                    }
                    else{
                        execResult.addResult(new ExecOpResult( op.id,  i, true, ExecContext.VLD_SUCCESS, null));
                    }
                }
            }
        }
        if(state==false) {
            throw new Exception("校验失败" + err_msg);
        }
    }

    public void Insert(ExecOp op, ExecContext execContext, UserAction userAction, ExecResult execResult) throws Exception {
        CompDataSource ds = null;
        Object obj = Modules.getInstance().get(op.ds_id, false);
        //兼容老版本，老版本insert是绑定的view_id
        if(obj!=null && obj instanceof View){
            ds = (CompDataSource) Modules.getInstance().get(((View) obj).comp_id, false);
        }
        else if(obj!=null && obj instanceof CompDataSource){
            ds = (CompDataSource) obj;
        }
        else if (op.view_id != null) {
            ds = (CompDataSource) Modules.getInstance().get(op.view_id + "ds", false);
        }
        List<Map> submit_rows = execContext.getContextRows(op.id, ExecContext.OP_ST_SUBMITTED,null, op.ds_id);
        if (submit_rows == null || submit_rows.isEmpty()) {
            String msg = "op" + op.id + "操作失败：未提交数据源“" + ds.name + "”的数据";
            execResult.addResult(new ExecOpResult(op.id,0,false, msg));
            throw new Exception(msg);
        }
        userAction.action_type = op.op_type;
        //兼容老版本，老版本insert是绑定的view_id
        if (ds == null && op.view_id != null) {
            ds = (CompDataSource) Modules.getInstance().get(op.view_id + "ds", false);
        }
        Table tbl = (Table) Modules.getInstance().get(ds.table_id, true);
        if (CollUtil.isNotEmpty(tbl.cols)) {
            Map<String, String> collect = tbl.cols.stream().collect(Collectors.toMap(o -> o.id, o -> o.field));
            for (CompDataSourceField field : ds.fields) {
                if (StringUtils.isNotEmpty(collect.get(field.table_col_id))) {
                    field.setField(collect.get(field.table_col_id));
                }
            }
        }

        List<Map> insertRows = (List) Lutils.copy(submit_rows);
        //添加操作人数据
        for (Map submit_row : insertRows) {
            submit_row.put("create_user_", userAction.user_id);
            submit_row.put("create_time_", userAction.action_time);
        }
        //填充外键（如果actionLis中有配置fill-foreign）
        ExecOpUtil.FillForeignKeyIntoRows(insertRows, op, execContext);
        /*
         * 将提交的数据插入数据库，并将id更新到提交的数据中，同时更新scope
         * 方法中，同时更新了会话
         */
        String op_table = "z_" + tbl.id;
        //根据数据源类型，将提交的数据转换为数据库可存的类型，如：数据库类型timestamp的变量，提交时为long类型，应转换为java.utils.Date
        ExecOpUtil.convertRowValueToDBType(ds, insertRows);
        //存入数据库，返回数据库完成添加后的行数据
        for (Map row : insertRows) {
            row.remove("__op_id__");
            row.remove("__op_st__");
            row.remove("__table_id__");
            row.remove("__op_tp__");
            row.remove("__ds_id__");
        }
        List<Map> afterInsertedRows = db.insertMapListRetRows(op_table, insertRows);
        //添加到session中
        execContext.addContexts(afterInsertedRows,
                op.id, ExecContext.OP_ST_AFTER,
                tbl.id, ExecContext.OP_TP_INSERT, ds.id);
        execContext.addContextsForeignTableIds(afterInsertedRows,
                op.id, ExecContext.OP_ST_AFTER,
                tbl.id, ExecContext.OP_TP_RELATED_FOREIGN);
        //记录日志
        handlerLog(tbl,
                afterInsertedRows.stream().map(o -> o.get("id_")).collect(Collectors.toList()),
                Lutils.genMap(
                        "log_time_", userAction.action_time,
                        "action_id_", userAction.id,
                        "action_type_", userAction.action_type,
                        "op_view_id", op.view_id
                ));
        execResult.addResult(new ExecOpResult(op.id,0, true, ExecContext.RS_MSG_SUCCESS ));
    }

    public void Update(ExecOp op, ExecContext execContext, UserAction userAction, ExecResult execResult) throws Exception {
        CompDataSource ds = (CompDataSource) Modules.getInstance().get(op.ds_id, false);
        //兼容老版本，老版本insert是绑定的view_id
        if (ds == null && op.view_id != null) {
            ds = (CompDataSource) Modules.getInstance().get(op.view_id + "ds", false);
        }
        List<Map> submit_rows = execContext.getContextRows(op.id, ExecContext.OP_ST_SUBMITTED, null, op.ds_id);
        if (submit_rows == null || submit_rows.isEmpty()) {
            String msg = "op" + op.id + "操作失败：未提交“" + ds.name + "”数据";
            execResult.addResult(new ExecOpResult(op.id, 0,false, msg));
            throw new Exception(msg);
        }
        List whereLimitedIds = execContext.getWhereLimitedIds(op, ds.table_id);
        //如果操作域是本组件的表
        if (whereLimitedIds.isEmpty()) {
            String msg = "op" + op.id + "操作失败：限制ID缺失";
            execResult.addResult(new ExecOpResult(op.id,0, false, msg));
            throw new Exception(msg);
        }
        userAction.action_type = op.op_type;
        Map form = (Map) Lutils.copy(submit_rows.get(0));
        Table tbl = (Table) Modules.getInstance().get(ds.table_id, true);
        if (CollUtil.isNotEmpty(tbl.cols)) {
            Map<String, String> collect = tbl.cols.stream().collect(Collectors.toMap(o -> o.id, o -> o.field));
            for (CompDataSourceField field : ds.fields) {
                if (StringUtils.isNotEmpty(collect.get(field.table_col_id))) {
                    field.setField(collect.get(field.table_col_id));
                }
            }
        }
        //取更新前的数据
        List<Map> beforeRows = db.selectIn("z_" + ds.table_id, "id_", whereLimitedIds);
        //更新表单数据
        form.put("update_user_", userAction.user_id);
        form.put("update_time_", userAction.action_time);
        //执行更新
        ExecOpUtil.convertRowValueToDBType(ds, Lutils.genList(form));
        ExecOpUtil.RemoveContextParams(form);
        db.updateIn("z_" + ds.table_id, form, "id_", whereLimitedIds);
        List<Map> afterRows = db.selectIn("z_" + ds.table_id, "id_", whereLimitedIds);
        //更新前的数据添加到会话
        execContext.addContexts(beforeRows,
                op.id, ExecContext.OP_ST_BEFORE,
                op.table_id, ExecContext.OP_TP_UPDATE, ds.id);
        //更新后的数据添加到会话
        execContext.addContexts(afterRows,
                op.id, ExecContext.OP_ST_AFTER,
                op.table_id, ExecContext.OP_TP_UPDATE, ds.id);
        //父节点的数据添加到会话
        execContext.addContextsForeignTableIds(afterRows,
                op.id, ExecContext.OP_ST_AFTER,
                op.table_id, ExecContext.OP_TP_RELATED_FOREIGN);
        //记录日志
        handlerLog(tbl, whereLimitedIds, Lutils.genMap(
                "log_time_", userAction.action_time,
                "action_id_", userAction.id,
                "action_type_", userAction.action_type,
                "op_view_id", op.view_id
        ));
        execResult.addResult(new ExecOpResult(op.id, 0,true, ExecContext.RS_MSG_SUCCESS ));
    }

    public void DataSync(ExecOp op, ExecContext execContext, UserAction userAction, ExecResult execResult) throws Exception {
        CompDataSource ds = (CompDataSource) Modules.getInstance().get(op.ds_id, false);
        Table table = (Table) Modules.getInstance().get(ds.table_id, true);
        String tName = ds.table_id.replace("table", "t");
        Map<String, TableCol> colMap = table.cols.stream().collect(Collectors.toMap(TableCol::getField, v -> v));
        //兼容老版本，老版本insert是绑定的view_id
        if (ds == null && op.view_id != null) {
            ds = (CompDataSource) Modules.getInstance().get(op.view_id + "ds", false);
        }
        List<Map> submit_rows = execContext.getContextRows(op.id, ExecContext.OP_ST_SUBMITTED, null, op.ds_id);
        if (submit_rows == null || submit_rows.isEmpty()) {
            String msg = "op" + op.id + "操作失败：未提交“" + ds.name + "”数据";
            execResult.addResult(new ExecOpResult(op.id,0,false, msg));
            throw new Exception(msg);
        }
        //注入主表id
        ExecOpUtil.FillForeignKeyIntoRows(submit_rows, op, execContext);
        List<String> filterKeys = Arrays.asList("session", "__row_changed_type__", "_X_ROW_KEY", "__op_id__",
                "__op_st__", "__table_id__", "__op_tp__", "__ds_id__");
        for(Map row:submit_rows){
            Object type = row.get("__row_changed_type__");
            if (Objects.isNull(type)) {
                continue;
            }
            String rowChangedType = type.toString();
            String tn = "z_" + row.get("__table_id__");
            Map<String, Object> params = new HashMap<>();
            Map<String, Object> dataMap = new HashMap<>();
            params.put("tn", tn);
            row.forEach((key, value) -> {
                String keyStr = key.toString();
                if (filterKeys.contains(keyStr)) {
                    return;
                }
                keyStr = keyStr.replace(tName + "_", "");
                if (!colMap.containsKey(keyStr)) {
                    return;
                }
                TableCol tableCol = colMap.get(keyStr);
                if (Objects.nonNull(tableCol)) {
                    String dataType = tableCol.getData_type();
                    if (keyStr.contains("time_") || Objects.equals(dataType, "timestamp")) {
                        dataMap.put(keyStr, value == null ? null : new Date((Long)value));
                    } else if (Objects.equals(dataType, "integer")) {
                        dataMap.put(keyStr, value == null ?  0 : Integer.valueOf(value.toString()));
                    }
                    else if (Objects.equals(dataType, "numeric")) {
                        if(Objects.nonNull(value) && StringUtils.isNoneBlank(value.toString())) {
                            dataMap.put(keyStr, Double.valueOf(value.toString()));
                        }
                    }else {
                        dataMap.put(keyStr, value);
                    }
                }
            });
            if (Objects.equals(rowChangedType, "insert")) {
                dataMap.put("create_time_", new Date());
                params.put("insertMap", dataMap);
                // 新增
                int i = baseDBMapper.insertMap(params);
                System.out.println("新增了" + i + "行数据");
            }
            else if (Objects.equals(rowChangedType, "update")) {
                Long rowId = Long.parseLong(row.get(tn+ "_id").toString());
                // 更新
                params.put("updateMap", dataMap);
                params.put("equalMap", Lutils.genMap("id_", rowId));
                int i = baseDBMapper.updateEq(params);
                System.out.println("更新了" + i + "行数据");
            } else if (Objects.equals(rowChangedType, "delete")) {
                Long rowId = Long.parseLong(row.get(tn+ "_id").toString());
                params.put("updateMap", Lutils.genMap("time_if_delete_", new Date()));
                params.put("equalMap", Lutils.genMap("id_", rowId));
                int i = baseDBMapper.updateEq(params);
                System.out.println("删除了" + i + "行数据");
            }
        }
        execResult.addResult(new ExecOpResult(op.id,0,true, ExecContext.RS_MSG_SUCCESS ));
    }

    public void Delete(ExecOp op, ExecContext execContext, UserAction userAction, ExecResult execResult) throws Exception {
        Table tbl = (Table) Modules.getInstance().get(op.table_id, true);
        List whereLimitedIds = execContext.getWhereLimitedIds(op, op.table_id);
        //如果操作域是本组件的表
        if (whereLimitedIds.isEmpty()) {
            String msg = "表“" + tbl.name + "”的限制ID缺失";
            execResult.addResult(new ExecOpResult(op.id,0,false, msg));
            throw new Exception(op.id+msg);
        }
        int updateCount = db.updateIn("z_" + op.table_id,
                Lutils.genMap("time_if_delete_", userAction.action_time),
                "id_", whereLimitedIds);
        execResult.addResult(new ExecOpResult(op.id,0,true, ExecContext.RS_MSG_SUCCESS ));
    }

    public void MultiDelete(ExecOp op, ExecContext execContext, UserAction userAction, ExecResult execResult) throws Exception {
        Table tbl = (Table) Modules.getInstance().get(op.table_id, true);
        List whereLimitedIds = execContext.getWhereLimitedIds(op, op.table_id);
        //如果操作域是本组件的表
        if (whereLimitedIds.isEmpty()) {
            String msg = "表“" + tbl.name + "”的限制ID缺失";
            execResult.addResult(new ExecOpResult(op.id,0,false, msg ));
            throw new Exception(op.id+":"+msg);
        }
        int updateCount = db.updateIn("z_" + op.table_id,
                Lutils.genMap("time_if_delete_", userAction.action_time),
                "id_", whereLimitedIds);
        execResult.addResult(new ExecOpResult(op.id,0,true, ExecContext.RS_MSG_SUCCESS));
    }

    public void ManualToNode(ExecOp op, ExecContext execContext, UserAction userAction, ExecResult execResult) throws Exception {
        Table tbl = (Table) Modules.getInstance().get(op.table_id, true);
        if (tbl == null) {
            String msg = "动作" + op.id + "：执行流程表【" + op.table_id + "】不存在";
            execResult.addResult(new ExecOpResult(op.id,0,false, msg ));
            throw new Exception(msg);
        }
        String submit_flow_node_id = execContext.getSubmittedFlowNodeId(op);
        FlowNode node = tbl.getNode(submit_flow_node_id);
        if (node == null) {
            String msg =  "动作" + op.id + "：“" + tbl.name + "”的流程节点（" + submit_flow_node_id + "）不存在，无法执行流程";
            execResult.addResult(new ExecOpResult(op.id,0,false, msg ));
            throw new Exception(msg);
        }
        // 回退前先判断一下当前回退是否已经执行完成
        List<Integer> rowIds = execContext.getContextRows("any", ExecContext.OP_ST_SUBMITTED, op.table_id,null).stream().filter(o -> (
                Objects.equals(o.get("__table_id__"), op.table_id)
        )).map(o -> (Integer)o.get("id_")).collect(Collectors.toList());
        if (CollUtil.isNotEmpty(rowIds)) {
            String sql = "select action_type_ \n" +
                    "from z_" + op.table_id + "_log\n" +
                    "where row_id_ = " + rowIds.get(0) + "\n" +
                    "order by log_time_ desc";
            List<Map> maps = baseDBMapper.selectDefinedSql(Lutils.genMap("data_sql", sql));
            if (CollUtil.isNotEmpty(maps) && Objects.equals(maps.get(0).get("action_type_"), "manual_to_node")) {
                String msg = op.id+":"+"动作" + op.id + "：操作失败，请先完成当前流程后再进行回退";
                execResult.addResult(new ExecOpResult(op.id,0,false, msg ));
                throw new Exception(msg);
            }
        }
        //判断动作是分发，还是派单。
        List whereLimitedIds = execContext.getWhereLimitedIds(op, op.table_id);
            if (whereLimitedIds.isEmpty()) {
                String msg = "动作" + op.id + "操作失败：执行流程【" + tbl.name + "】的限制ID缺失";
                execResult.addResult(new ExecOpResult(op.id,0,false, msg ));
                throw new Exception(msg);
            }
            List<Map> beforeRows = db.selectIn("z_" + tbl.id, "id_", whereLimitedIds);
            //获取流程的接单人
            List receivers = execContext.getSubmittedReceivers(op);
            if (receivers.isEmpty()) {
                String msg = "动作" + op.id + "：操作失败，提交用户数据异常";
            }
            Map flowInfo = new HashMap();
            flowInfo.put("poster_", userAction.user_id);
            flowInfo.put("posted_time_", userAction.action_time);
            flowInfo.put("edge_", op.op_type);
            flowInfo.put("node_", node.id);
            /* *
             * 如果该边是子流程的汇聚边，对每一行数据判断流程是否满足要求，不满足要求的行，不执行相关动作。
             */

            //如果当前流程是子流程，则找到主流程的状态，判断主流程的状态是否处于子流程对应的状态
            if (!tbl.priTableIds.isEmpty()) {
                //判断主流程是否结束
                List<Map> subFinishedLs = beforeRows.stream()
                        .filter(r -> Objects.equals(r.get("pri_tbl_node_finished_"), 1)).collect(Collectors.toList());
                if (!subFinishedLs.isEmpty()) {
                    String msg = "【" + tbl.name + "】存在已结束的任务，请刷新后重试！";
                    execResult.addResult(new ExecOpResult(op.id,0,false, msg ));
                    throw new Exception(msg);
                }
                //子流程的数据
                if (!beforeRows.isEmpty()) {
                    //判断主流程的节点是否正确
                    Map subRow = beforeRows.get(0);
                    String pri_tbl_ = (String) subRow.get("pri_tbl_");
                    String pri_tbl_node_ = (String) subRow.get("pri_tbl_node_");
                    List<Integer> pri_ids = new ArrayList<>();
                    //取涉及到主流程的ids
                    if (pri_tbl_ != null) {
                        pri_ids = beforeRows.stream().filter(r -> r.get("z_" + pri_tbl_ + "_id") != null).map(r -> (Integer) r.get("z_" + pri_tbl_ + "_id")).collect(Collectors.toList());
                        if (pri_ids.size() > 0 && pri_tbl_node_ != null) {
                            //取出主流程节点状态
                            List<Map> priList = db.selectIn("z_" + pri_tbl_, "id_", pri_ids);
                            List<Map> illegalNodes = priList.stream()
                                    .filter(p -> p.get("node_") != null && !Objects.equals(p.get("node_"), pri_tbl_node_))
                                    .collect(Collectors.toList());
                            if (illegalNodes.size() > 0) {
                                Table priTbl = (Table) Modules.getInstance().get(pri_tbl_, true);
                                FlowNode priNode = priTbl.getNode(pri_tbl_node_);
                                FlowNode priNode1 = priTbl.getNode(illegalNodes.get(0).get("node_").toString());
                                String msg = "流程【" + tbl.name + "】行(" + subRow.get("id_") + ")与主流程【" + priTbl.name + "】环节不一致，无法执行！子流程依赖主流程环节：" + priNode.label + "，主流程所在节点：" + priNode1.label;
                                execResult.addResult(new ExecOpResult(op.id,0,false, msg ));
                                throw new Exception(msg);
                            }
                        }
                    }
                }
            }

            //填充poster_、posted_time_、edge_、node_
            List<Map> afterRows = new ArrayList<>();
            for (Map row : beforeRows) {
                Map cp = Lutils.copyMap(row);
                //记录上一个节点状态
                cp.put("prev_node_", cp.get("node_"));
                cp.putAll(flowInfo);
                afterRows.add(cp);
            }

            //填充接单人，node_，如果是分支，重新设置node_，即：根据分支确定dst
            FlowNode dstNode = node;
            if (!Objects.equals(dstNode.type, "gate")) {
                //将receiver_填充到afterRows中
//                setReceivers(afterRows, edge.id, op, execContext, userAction, execResult);
            } else {
                //根据分支判断，修改每一行的edge_和node_
                FillGateFlowDst(afterRows, tbl, node.id);
                //接单人处理：不同的分支派单类型不一样，需要根据node_分组处理
                List<String> distinctEdges = afterRows.stream()
                        .filter(r -> r.get("edge_") != null)
                        .map(r -> (String) r.get("edge_"))
                        .distinct()
                        .collect(Collectors.toList());
                for (String edge_ : distinctEdges) {
                    List<Map> distNodeRows = afterRows.stream()
                            .filter(r -> Objects.equals(edge_, r.get("edge_")))
                            .collect(Collectors.toList());
//                    setReceivers(distNodeRows, edge_, op, execContext, userAction, execResult);
                }
            }
            //更新
            for (Map row : afterRows) {
                db.updateEq("z_" + op.table_id,
                        Lutils.genMap("poster_", row.get("poster_"),
                                "posted_time_", row.get("posted_time_"),
                                "prev_node_", row.get("prev_node_"),
                                "edge_", ExecContext.OP_TP_MANUAL_TO_NODE,
                                "node_", row.get("node_"),
                                "receiver_", row.get("receiver_")),
                        Lutils.genMap("id_", row.get("id_"))
                );

                //查看是否需要短信提醒
//                if (edge.is_sms != null && edge.is_sms) {
//                    Map userMap = db.selectEq("v_user",
//                            Lutils.genMap("id", row.get("receiver_")), null).get(0);
//                    String phoneNumber = userMap.get("phone").toString();
////                String phoneNumber = userAction.get("phone").toString();
//                    String message = tbl.name;
//                    var res2 = SmsUtils.sendMessage(phoneNumber, message);
//                    if (res2 != null) {
//                        System.out.println("通知短信发送结果: " + res2.getBody().getMessage());
//                    }
//                }
            }
            execContext.addContexts(beforeRows,
                    op.id, ExecContext.OP_ST_BEFORE,
                    tbl.id, ExecContext.OP_TP_MANUAL_TO_NODE, null);
            execContext.addContexts(afterRows,
                    op.id, ExecContext.OP_ST_AFTER,
                    tbl.id, ExecContext.OP_TP_MANUAL_TO_NODE, null);
            execContext.addContextsForeignTableIds(afterRows,
                    op.id, ExecContext.OP_ST_AFTER,
                    op.table_id, ExecContext.OP_TP_RELATED_FOREIGN);

            //执行事件，将直接结果按node分类后，针对不同的node，执行事件。
            List distinctNodes = afterRows.stream()
                    .filter(r -> r.get("node_") != null)
                    .map(r -> r.get("node_"))
                    .distinct()
                    .collect(Collectors.toList());
            for (Object distNode : distinctNodes) {
                List<Map> distNodeRows = afterRows.stream().filter(r -> Objects.equals(distNode, r.get("node_"))).collect(Collectors.toList());
                //处理事件，即执行SQL
                HandleNodeEventSQL(op.table_id, distNodeRows, op, execContext, userAction);
            }
            List update_ids = afterRows.stream().map(o -> o.get("id_")).collect(Collectors.toList());
            //更新日志中，上一个环节的finished_time
            if (afterRows.size() > 0) {
                businessMapper.set_log_finished_time(Lutils.genMap("table_id", op.table_id, "row_ids", update_ids, "finished_time_", userAction.action_time, "prev_node_", afterRows.get(0).get("prev_node_")));
            }
            //记录日志
            handlerFlowAndLog(tbl, update_ids,
                    Lutils.genMap(
                            "log_time_", userAction.action_time,
                            "action_id_", userAction.id,
                            "action_type_", ExecContext.OP_TP_MANUAL_TO_NODE
                    ));
            //子流程执行完成后，触发主流程汇聚
            if (tbl.priTableIds.size() > 0) {
                TriggerPriComplete(afterRows, op, execContext, userAction, execResult);
            }
        execResult.addResult(new ExecOpResult(op.id,0,true, ExecContext.RS_MSG_SUCCESS ));
    }

    public void Complete(ExecOp op, ExecContext execContext, UserAction userAction, ExecResult execResult) throws Exception {
        Table tbl = (Table) Modules.getInstance().get(op.table_id, true);
        if (tbl == null) {
            String msg = "动作" + op.id + "：执行流程表【" + op.table_id + "】不存在";
            execResult.addResult(new ExecOpResult(op.id,0,false, msg ));
            throw new Exception(msg);
        }
        FlowEdge edge;
        //如果动作执行依赖用户选择，则从提交的数据中找
        if (Objects.equals(op.flow_edge_id, "edge-1") || Objects.equals(op.flow_edge_id, "edge-submit")) {
            String submit_flow_edge_id = execContext.getSubmittedFlowEdgeId(op);
            List<Integer> rowIds = execContext.getContextRows("any", ExecContext.OP_ST_SUBMITTED, op.table_id, null).stream().filter(o -> (
                    Objects.equals(o.get("__table_id__"), op.table_id)
            )).map(o -> (Integer)o.get("id_")).collect(Collectors.toList());
            if (CollUtil.isNotEmpty(rowIds)) {
                // 从日志上拿上一条流程完成或者回退的动作，如果上一条日志是回退的直接流转到回退记录的那个环节
                String sql = "select l.action_type_, l.prev_node_, l.edge_, l.node_, pe.id edge_id\n" +
                        "from z_" + op.table_id + "_log l\n" +
                        "left join v_flow_edge pe on pe.dst = l.prev_node_\n" +
                        "where l.action_type_ in ('complete', 'manual_to_node') and l.row_id_ = " + rowIds.get(0) + "\n" +
                        "order by l.log_time_ desc\n" +
                        "limit 1 OFFSET 0";
                List<Map> maps = baseDBMapper.selectDefinedSql(Lutils.genMap("data_sql", sql));
                if (CollUtil.isNotEmpty(maps)) {
                    String actionType = maps.get(0).get("action_type_").toString();
                    if (Objects.equals(actionType, "manual_to_node")) {
                        submit_flow_edge_id = maps.get(0).get("edge_id").toString();
                    }
                }
            }
            edge = tbl.getEdge(submit_flow_edge_id);
        } else {
            edge = tbl.getEdge(op.flow_edge_id);
        }
        if (edge == null) {
            String msg =  "动作" + op.id + "：表“" + tbl.name + "”的流程路径" + op.flow_edge_id + "不存在，无法执行流程";
            execResult.addResult(new ExecOpResult(op.id,0,false, msg ));
            throw new Exception(msg);
        }
        //判断动作是分发，还是派单。
        if (Objects.equals(edge.assign_type, "insert-rows-with-manual-receivers") ||
                Objects.equals(edge.assign_type, "insert-rows-with-defined-roles-users")) {
            DistributeComplete(op, execContext, userAction, execResult);
        }
        else {
            List whereLimitedIds = execContext.getWhereLimitedIds(op, op.table_id);
            if (whereLimitedIds.isEmpty()) {
                String msg = "动作" + op.id + "操作失败：执行流程【" + tbl.name + "】的限制ID缺失";
                execResult.addResult(new ExecOpResult(op.id,0,false, msg ));
                throw new Exception(msg);
            }
            List<Map> beforeRows = db.selectIn("z_" + tbl.id, "id_", whereLimitedIds);
            final String tmp_srcType = edge.srcType;
            final String tmp_src = edge.src;
            //校验执行的动作与当前数据所在环节是否匹配
            List<Map> illegalRows = beforeRows.stream().filter(r ->
                    (r.get("node_") == null && !Objects.equals(tmp_srcType, "start"))
                            || (r.get("node_") != null && !Objects.equals(r.get("node_"), tmp_src))
            ).collect(Collectors.toList());
            if (!illegalRows.isEmpty()) {
                String msg = "当前流程【" + tbl.name + "】不符合流程环节要求。id_=" + illegalRows.get(0).get("id_")
                        + ")，执行动作【" + edge.label + "】所需环节:" + edge.srcLabel;
                if (Objects.equals(op.illegal_processing, "stop")) {
                    execResult.addResult(new ExecOpResult(op.id,0,false, msg ));
                    throw new Exception(msg);
                } else if (Objects.equals(op.illegal_processing, "skip")) {
                    System.out.println("跳过：" + msg);
                    execResult.addResult(new ExecOpResult(op.id,0,true, "跳过执行不匹配的节点："+msg));
                    return;
                } else if (Objects.equals(op.illegal_processing, "forcibly-exec")) {
                    System.out.println("强制执行：" + msg);
                    execResult.addResult(new ExecOpResult(op.id,0,true, "强制执行不匹配的节点："+msg));
                }
            }
            //获取流程的接单人
            List receivers = execContext.getSubmittedReceivers(op);
            if (receivers.isEmpty()) {
                String msg = "动作" + op.id + "：操作失败，提交用户数据异常";
//                execResult.put("failed", msg);
//                throw new Exception(msg);
            }
            Map flowInfo = new HashMap();
            flowInfo.put("poster_", userAction.user_id);
            flowInfo.put("posted_time_", userAction.action_time);
            flowInfo.put("edge_", edge.id);
            flowInfo.put("node_", edge.dst);
            /* *
             * 如果该边是子流程的汇聚边，对每一行数据判断流程是否满足要求，不满足要求的行，不执行相关动作。
             */
            if (Objects.equals(edge.srcType, "sub-flow")) {
                List<Map> tmpList = new ArrayList<>();
                for (Map row : beforeRows) {
                    boolean rowEdgePassed = EdgeConditionResult(row, tbl, edge);
                    if (rowEdgePassed) {
                        tmpList.add(row);
                    }
                }
                beforeRows = tmpList;
            }

            //如果当前流程是子流程，则找到主流程的状态，判断主流程的状态是否处于子流程对应的状态
            if (!tbl.priTableIds.isEmpty()) {
                //判断主流程是否结束
                List<Map> subFinishedLs = beforeRows.stream()
                        .filter(r -> Objects.equals(r.get("pri_tbl_node_finished_"), 1)).collect(Collectors.toList());
                if (!subFinishedLs.isEmpty()) {
                    String msg = "【" + tbl.name + "】存在已结束的任务，请刷新后重试！";
                    execResult.addResult(new ExecOpResult(op.id,0,true, msg));
                    throw new Exception(msg);
                }
                //子流程的数据
                if (!beforeRows.isEmpty()) {
                    //判断主流程的节点是否正确
                    Map subRow = beforeRows.get(0);
                    String pri_tbl_ = (String) subRow.get("pri_tbl_");
                    String pri_tbl_node_ = (String) subRow.get("pri_tbl_node_");
                    List<Integer> pri_ids = new ArrayList<>();
                    //取涉及到主流程的ids
                    if (pri_tbl_ != null) {
                        pri_ids = beforeRows.stream().filter(r -> r.get("z_" + pri_tbl_ + "_id") != null).map(r -> (Integer) r.get("z_" + pri_tbl_ + "_id")).collect(Collectors.toList());
                        if (pri_ids.size() > 0 && pri_tbl_node_ != null) {
                            //取出主流程节点状态
                            List<Map> priList = db.selectIn("z_" + pri_tbl_, "id_", pri_ids);
                            List<Map> illegalNodes = priList.stream()
                                    .filter(p -> p.get("node_") != null && !Objects.equals(p.get("node_"), pri_tbl_node_))
                                    .collect(Collectors.toList());
                            if (illegalNodes.size() > 0) {
                                Table priTbl = (Table) Modules.getInstance().get(pri_tbl_, true);
                                FlowNode priNode = priTbl.getNode(pri_tbl_node_);
                                FlowNode priNode1 = priTbl.getNode(illegalNodes.get(0).get("node_").toString());
                                String msg = "流程【" + tbl.name + "】行(" + subRow.get("id_") + ")与主流程【" + priTbl.name + "】环节不一致，无法执行！子流程依赖主流程环节：" + priNode.label + "，主流程所在节点：" + priNode1.label;
                                execResult.addResult(new ExecOpResult(op.id,0,true, msg));
                                throw new Exception(msg);
                            }
                        }
                    }
                }
            }

            //填充poster_、posted_time_、edge_、node_
            List<Map> afterRows = new ArrayList<>();
            for (Map row : beforeRows) {
                Map cp = Lutils.copyMap(row);
                //记录上一个节点状态
                cp.put("prev_node_", cp.get("node_"));
                cp.putAll(flowInfo);
                afterRows.add(cp);
            }

            //填充接单人，node_，如果是分支，重新设置node_，即：根据分支确定dst
            FlowNode dstNode = tbl.getNode(edge.dst);
            if (!Objects.equals(dstNode.type, "gate")) {
                //将receiver_填充到afterRows中
                setReceivers(afterRows, edge.id, op, execContext, userAction, execResult);
            } else {
                //根据分支判断，修改每一行的edge_和node_
                FillGateFlowDst(afterRows, tbl, dstNode.id);
                //接单人处理：不同的分支派单类型不一样，需要根据node_分组处理
                List<String> distinctEdges = afterRows.stream()
                        .filter(r -> r.get("edge_") != null)
                        .map(r -> (String) r.get("edge_"))
                        .distinct()
                        .collect(Collectors.toList());
                for (String edge_ : distinctEdges) {
                    List<Map> distNodeRows = afterRows.stream()
                            .filter(r -> Objects.equals(edge_, r.get("edge_")))
                            .collect(Collectors.toList());
                    setReceivers(distNodeRows, edge_, op, execContext, userAction, execResult);
                }
            }
            //更新
            for (Map row : afterRows) {
                db.updateEq("z_" + op.table_id,
                        Lutils.genMap("poster_", row.get("poster_"),
                                "posted_time_", row.get("posted_time_"),
                                "prev_node_", row.get("prev_node_"),
                                "edge_", row.get("edge_"),
                                "node_", row.get("node_"),
                                "receiver_", row.get("receiver_")),
                        Lutils.genMap("id_", row.get("id_"))
                );

                //查看是否需要短信提醒
                if (edge.is_sms != null && edge.is_sms) {
                    Map userMap = db.selectEq("v_user",
                            Lutils.genMap("id", row.get("receiver_")), null).get(0);
                    String phoneNumber = userMap.get("phone").toString();
//                String phoneNumber = userAction.get("phone").toString();
                    String message = tbl.name;
                    var res2 = SmsUtils.sendMessage(phoneNumber, message);
                    if (res2 != null) {
                        System.out.println("通知短信发送结果: " + res2.getBody().getMessage());
                    }
                }
            }
            execContext.addContexts(beforeRows,
                    op.id, ExecContext.OP_ST_BEFORE,
                    tbl.id, ExecContext.OP_TP_COMPLETE, null);
            execContext.addContexts(afterRows,
                    op.id, ExecContext.OP_ST_AFTER,
                    tbl.id, ExecContext.OP_TP_COMPLETE, null);
            execContext.addContextsForeignTableIds(afterRows,
                    op.id, ExecContext.OP_ST_AFTER,
                    op.table_id, ExecContext.OP_TP_RELATED_FOREIGN);

            //执行事件，将直接结果按node分类后，针对不同的node，执行事件。
            List distinctNodes = afterRows.stream()
                    .filter(r -> r.get("node_") != null)
                    .map(r -> r.get("node_"))
                    .distinct()
                    .collect(Collectors.toList());
            for (Object distNode : distinctNodes) {
                List<Map> distNodeRows = afterRows.stream().filter(r -> Objects.equals(distNode, r.get("node_"))).collect(Collectors.toList());
                //处理事件，即执行SQL
                HandleNodeEventSQL(op.table_id, distNodeRows, op, execContext, userAction);
            }
            List update_ids = afterRows.stream().map(o -> o.get("id_")).collect(Collectors.toList());
            //更新日志中，上一个环节的finished_time
            if (afterRows.size() > 0) {
                businessMapper.set_log_finished_time(Lutils.genMap("table_id", op.table_id, "row_ids", update_ids, "finished_time_", userAction.action_time, "prev_node_", afterRows.get(0).get("prev_node_")));
            }
            //记录日志
            handlerFlowAndLog(tbl, update_ids,
                    Lutils.genMap(
                            "log_time_", userAction.action_time,
                            "action_id_", userAction.id,
                            "action_type_", "complete"
                    ));
            //子流程执行完成后，触发主流程汇聚
            if (tbl.priTableIds.size() > 0) {
                TriggerPriComplete(afterRows, op, execContext, userAction, execResult);
            }
        }
        execResult.addResult(new ExecOpResult(op.id,0,true, ExecContext.RS_MSG_SUCCESS));
    }

    /**
     * 分发数据新增数据
     */
    public void DistributeComplete(ExecOp op, ExecContext execContext, UserAction userAction, ExecResult execResult) throws Exception {
        Table tbl = (Table) Modules.getInstance().get(op.table_id, true);
        FlowEdge edge = tbl.getEdge(op.flow_edge_id);
        List<Integer> receivers = null;
        if (Objects.equals(edge.assign_type, "insert-rows-with-manual-receivers")) {
            //从submit中获取
            receivers = execContext.getSubmittedReceivers(op);
        } else if (Objects.equals(edge.assign_type, "insert-rows-with-defined-roles-users")) {
            ExecContext execContext_s = execContext.getActionContext(op, ExecContext.ENV_CFG_WHERE_LIMIT_IDS);
            receivers = CompUtils.getInstance().getScopedUsers(edge.userScope, execContext_s, userAction);
        }
        Map form = execContext.getContextRow(op.id, ExecContext.OP_ST_SUBMITTED, op.table_id, op.ds_id);
        form = form == null ? Lutils.genMap() : form;
        if (receivers == null && Lutils.nvl(edge.assign_required, false)) {
            String msg = "动作op_id=" + op.id + "，候选人为空";
            execResult.addResult(new ExecOpResult(op.id,0,true, msg));
            throw new Exception(msg);
        }
        //根据人员生成行
        List<Map> insertRows = new ArrayList<>();
        if (receivers != null && receivers.size() > 0) {
            for (Integer receiver_ : receivers) {
                Map row = Lutils.copyMap(form);
                row.put("poster_", userAction.user_id);
                row.put("posted_time_", userAction.action_time);
                row.put("prev_node_", edge.src);
                row.put("edge_", edge.id);
                row.put("node_", edge.dst);
                row.put("receiver_", receiver_);
                row.put("create_user_", userAction.user_id);
                row.put("create_time_", userAction.action_time);
                insertRows.add(row);
            }
        }
        /** 为每行加入外联id，补充外联表z_table123_id,如果session中出现了多个id，则取第一个 */
        ExecOpUtil.FillForeignKeyIntoRows(insertRows, op, execContext);
        //如果当前是子流程，行中需要添加主表的信息，从会话设置中取：pri_table_id
        ExecOpUtil.FillCountersignIntoRows(insertRows, op, execContext, db);

        //插入数据库
        List<Map> afterInsertedRows = db.insertMapListRetRows("z_" + tbl.id, insertRows);
        execContext.addContexts(afterInsertedRows,
                op.id, ExecContext.OP_ST_AFTER,
                tbl.id, ExecContext.OP_TP_COMPLETE, null);
        execContext.addContextsForeignTableIds(afterInsertedRows,
                op.id, ExecContext.OP_ST_AFTER,
                tbl.id, ExecContext.OP_TP_RELATED_FOREIGN);
        List insertIds = afterInsertedRows.stream().map(o -> o.get("id_")).collect(Collectors.toList());
        //要先执行，记录日志和流水，然后子流程信息更新才能获取到信息
        handlerFlowAndLog(tbl, insertIds,
                Lutils.genMap(
                        "log_time_", userAction.action_time,
                        "action_id_", userAction.id,
                        "action_type_", userAction.action_type
                ));

        /** 当前操作的表是子流程，子流程执行完后，要汇聚主流程信息，并执行主流程事件 * */
        HandleNodeEventSQL(op.table_id, afterInsertedRows, op, execContext, userAction);
//        IfTriggerPriComplete(table_id,);
    }

    public void CacheDSRows(ExecOp op, ExecContext execContext, UserAction userAction, ExecResult execResult) throws Exception {
        CompDataSource ds = (CompDataSource) Modules.getInstance().get(op.ds_id, true);
        if (ds == null) {
            execResult.addResult(new ExecOpResult(op.id,0,true, "未找到数据源"));
            throw new Exception("未找到缓存的数据源");
        }
        //使用数据源的tale_id连接
        ExecContext execContext_s = execContext.getActionContext(op, ExecContext.ENV_CFG_WHERE_LIMIT_IDS);
        //取环境变量
        Map cfgVars = execContext.getEnvironmentVariables(op);
        TableDataInfo page = CompUtils.getInstance().get_ds_data(ds, cfgVars, execContext_s, userAction, ds.enable_total);
        if(page!=null) {
            List<Map> rows = (List) page.getRows();
            if (rows != null) {
                //是否需要左连接
                if(Objects.equals(op.left_join, true)) {
                    List<Map> leftRows = execContext.getEnvCfgMatchedRows(op, ExecContext.ENV_CFG_CACHE_ROWS);
                    //进行左连接动作，补充右表字段到leftRows
                    for (Map row : leftRows) {
                        List<Map> fd = rows.stream()
                                .filter(o -> Objects.equals(row.get("z_" + ds.table_id + "_id"), o.get("z_" + ds.table_id + "_id")))
                                .collect(Collectors.toList());
                        //将连接匹配到的数据，添加到左表中。
                        if (!fd.isEmpty()) {
                            Map<String, Object> match = fd.get(0);
                            for (Map.Entry<String, Object> entry : match.entrySet()) {
                                String key = entry.getKey();
                                Object value = entry.getValue();
                                // 处理每个键值对的逻辑
                                if (!row.containsKey(key)) {
                                    row.put(key, value);
                                }
                            }
                        }

                    }
                    execContext.addContexts(leftRows,
                            op.id, ExecContext.OP_ST_AFTER,
                            ds.table_id, op.op_type, op.ds_id);
                }
                else{
                    execContext.addContexts(rows,
                            op.id, ExecContext.OP_ST_AFTER,
                            ds.table_id, op.op_type, op.ds_id);
                }
            }
        }
        execResult.addResult(new ExecOpResult(op.id,0,true, ExecContext.RS_MSG_SUCCESS));
    }
}
