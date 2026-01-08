package tcdx.uap.service.entities;

import lombok.val;
import tcdx.uap.common.utils.Lutils;
import tcdx.uap.service.store.Modules;

import java.io.Serializable;
import java.util.*;
import java.util.stream.Collectors;

public class ExecContext implements Serializable {

    public static String ENV_CFG_WHERE_LIMIT_IDS = "where-limit-ids";
    public static String ENV_CFG_FILL_FOREIGN = "fill-foreign";
    public static String ENV_CFG_FILL_COUNTERSIGN = "fill-countersign";
    public static String ENV_CFG_CACHE_VAR = "cache-var";
    public static String ENV_CFG_CACHE_VAR_LIST = "cache-var-list";
    public static String ENV_CFG_CACHE_ROW = "cache-row";
    public static String ENV_CFG_CACHE_ROWS = "cache-rows";

    public static String OP_TP_INSERT = "insert";
    public static String OP_TP_UPDATE = "update";
    public static String OP_TP_RELATED_FOREIGN = "related_foreign";
    public static String OP_TP_COMPLETE = "complete";
    public static String OP_TP_MANUAL_TO_NODE = "manual_to_node";
    public static String OP_TP_EXECUTE_SQL = "execute_sql";
    public static String OP_TP_DELETE = "delete";

    public static String OP_ST_BEFORE = "before";
    public static String OP_ST_AFTER = "after";
    public static String OP_ST_AFTER_SUB_COMPLETE_FINISHED_BEFORE_PRI_COMPLETE_START = "after-sub-complete-finished-before-pri-complete-start";
    public static String OP_ST_SUBMITTED = "submitted";
    public static String OP_ST_AFTER_UPDATED_BEFORE_FILL_FOREIGN = "after-updated-before-fill-foreign";
    public static String OP_ST_AFTER_FILL_FOREIGN_BEFORE_FILL_COUNTERSIGN = "after-fill-foreign-before-fill-countersign";

    public static String VLD_FAILED = "校验失败";
    public static String VLD_SUCCESS = "校验成功";
    public static String RS_MSG_SUCCESS = "操作成功";

    private List<Map> contexts = new ArrayList<>();

    public ExecContext() {
    }

    public ExecContext(List<Map> list) {
        addContexts(list);
    }

    public void PutSubmitSessionIntoContextList(List<Map> list) {
        if (list == null || list.isEmpty()) {
            return;
        }
        //z_table46_ids,切分，存入列表中
        for (Map row : list) {
            row.put("__op_id__", "-1");   //-1表示前端session环境上传的
            row.put("__op_tp__", "submit");
            row.put("__op_st__", "after");
        }
        addContexts(list);
    }

    public void addContexts(List<Map> list) {
        if (list != null) {
            for (Map row : list) {
                if (row.get("__ds_id__") != null && row.get("__table_id__") == null) {
                    CompDataSource ds = (CompDataSource) Modules.getInstance().get(row.get("__ds_id__"), false);
                    row.put("__table_id__", ds.table_id);
                }
            }
            contexts.addAll(list);
        }
    }

    public void addContexts(List<Map> list, String op_id, String op_st, String table_id, String op_tp, String ds_id) {
        if (list != null) {
            //添加到会话
            for (Map row : list) {
                row.put("__op_id__", op_id);
                row.put("__op_st__", op_st);
                row.put("__table_id__", table_id);
                row.put("__op_tp__", op_tp);
                row.put("__ds_id__", ds_id);
            }
            addContexts(list);
        }
    }


    public void addContextsForeignTableIds(List<Map> list, String op_id, String op_st, String sub_table_id, String op_tp) {
        if (list != null) {
            Table tbl = (Table) Modules.getInstance().get(sub_table_id, false);
            if (Objects.isNull(tbl)) {
                return;
            }
            for (String f_table_id : tbl.priTableIds) {
                List ids = list.stream()
                        .filter(o -> o.get("z_" + f_table_id + "_id") != null)
                        .map(o -> o.get("z_" + f_table_id + "_id"))
                        .distinct()
                        .collect(Collectors.toList());
                List<Map> tmpContexts = new ArrayList<>();
                //添加到会话
                for (Object id : ids) {
                    Map row = new HashMap<>();
                    row.put("__op_id__", op_id);
                    row.put("__op_st__", op_st);
                    row.put("__table_id__", f_table_id);
                    row.put("__op_tp__", op_tp);
                    row.put("id_", id);
                    tmpContexts.add(row);
                }
                addContexts(tmpContexts);
            }
        }
    }

    public List<Map> getContextRows(ExecOpAction cfg) {
        if(cfg.from_ds_id!=null) {
            List<Map> list = contexts.stream()
                    .filter(o -> (
                            (Objects.equals(o.get("__op_id__"), cfg.from_op_id) || Objects.equals(Lutils.nvl(cfg.from_op_id, "any"), "any")) &&
                                    (Objects.equals(o.get("__op_st__"), cfg.before_or_after) || Objects.equals(Lutils.nvl(cfg.before_or_after, "any"), "any")) &&
                                    (Objects.equals(o.get("__ds_id__"), cfg.from_ds_id) || Objects.equals(Lutils.nvl(cfg.from_ds_id, "any"), "any"))
                    ))
                    .collect(Collectors.toList());
            return list;
        }
        else if(cfg.from_table_id!=null) {
            List<Map> list = contexts.stream()
                    .filter(o -> (
                            (Objects.equals(o.get("__op_id__"), cfg.from_op_id) || Objects.equals(Lutils.nvl(cfg.from_op_id, "any"), "any")) &&
                                    (Objects.equals(o.get("__op_st__"), cfg.before_or_after) || Objects.equals(Lutils.nvl(cfg.before_or_after, "any"), "any")) &&
                                    (Objects.equals(o.get("__table_id__"), cfg.from_table_id) || Objects.equals(Lutils.nvl(cfg.from_table_id, "any"), "any"))
                    ))
                    .collect(Collectors.toList());
            return list;
        }
        return new ArrayList<>();
    }

    public List<Map> getContextRows(String op_id, String op_st, String table_id,String ds_id) {
        List<Map> list = contexts.stream()
                .filter(o -> (
                        (Objects.equals(o.get("__op_id__"), op_id) || op_id==null || Objects.equals(op_id, "any")) &&
                                (Objects.equals(o.get("__op_st__"), op_st) || op_st==null || Objects.equals(op_st, "any")) &&
                                (Objects.equals(o.get("__table_id__"), table_id) || table_id==null || Objects.equals(table_id, "any")) &&
                                (Objects.equals(o.get("__ds_id__"), ds_id) || ds_id==null || Objects.equals(ds_id, "any"))
                ))
                .collect(Collectors.toList());
        return list;
    }

    public Map getContextRow(String op_id, String op_st, String table_id, String ds_id) {
        List<Map> list = getContextRows(op_id, op_st, table_id, ds_id);
        if (list.size() > 0) {
            return list.get(0);
        } else {
            return null;
        }
    }

    public Object getFieldValue(String op_id, String op_st, String table_id,String ds_id, String field) {
        Map row = getContextRow(op_id, op_st, table_id, ds_id);
        if (row != null) {
            return row.get(field);
        } else {
            return null;
        }
    }

    public List getFieldValues(String op_id, String op_st, String table_id, String ds_id, String field) {
        List<Map> list = getContextRows(op_id, op_st, table_id, ds_id);
        return list.stream().map(o -> o.get(field)).collect(Collectors.toList());
    }

    public List<Map> getEnvCfgMatchedRows(ExecOp op, String env_type) {
        List<Map> re = new ArrayList<>();
        for (ExecOpAction s : op.opActions.stream().filter(o -> o.session_type.equals(env_type)).collect(Collectors.toList())) {
            if (Objects.equals(s.from_op_id, "-1")) {
                re.addAll(getContextRows(s.from_op_id, null, s.from_table_id, s.from_ds_id));
            } else {
                re.addAll(getContextRows(s.from_op_id, s.before_or_after, s.from_table_id, s.from_ds_id));
            }
        }
        return re;
    }

    public List getWhereLimitedIds(ExecOp op, String table_id) {
        if (op == null)
            return new ArrayList<>();
        List<Map> all_table_ids = getEnvCfgMatchedRows(op, ENV_CFG_WHERE_LIMIT_IDS);
        return all_table_ids.stream()
                .filter(o -> Objects.equals(o.get("__table_id__"), table_id))
                .map(o -> o.get("id_"))
                .distinct()
                .collect(Collectors.toList());
    }


    public static Map listToWhereIdsMap(List<Map> list){
        Map<String,List> tableIdsMap = new HashMap();
        for (Map row: list) {
            //提取行里的z_table123_id
            for (Object key: row.keySet()) {
                String field = key.toString();
                Object val = row.get(field);
                if(field.matches("z_table\\d+_id")){
                    String table_id = field.replace("z_","").replace("_id","");
                    putIntoIdsMap(tableIdsMap, table_id, val);
                }
            }
            //判断数据源
            if (row.get("__ds_id__") != null) {
                CompDataSource ds = (CompDataSource) Modules.getInstance().get(row.get("__ds_id__"),false);
                if(ds!=null&&row.get("id_")!=null){
                    putIntoIdsMap(tableIdsMap, ds.table_id, row.get("id_"));

                }
            }
            //判断数据表
            if (row.get("__table_id__") != null&&row.get("id_")!=null){
                putIntoIdsMap(tableIdsMap, row.get("__table_id__").toString(), row.get("id_"));

            }
        }
        for(String key: tableIdsMap.keySet()){
            List ids = tableIdsMap.get(key);
            tableIdsMap.put(key, (List)ids.stream().distinct().collect(Collectors.toList()));
        }
        return tableIdsMap;
    }

    public Map getWhereIdsMap() {
        return listToWhereIdsMap(this.contexts);
    }

    /**
     * 获取限制ID的Map<table_id, ids>
     * @param op 动作
     * */
    public Map getWhereIdsMap(ExecOp op) {
        List<Map> list = new ArrayList<>();
        if (op != null) {
            for (ExecOpAction s : op.opActions.stream().filter(o -> o.session_type.equals(ExecContext.ENV_CFG_WHERE_LIMIT_IDS)).collect(Collectors.toList())) {
                if (Objects.equals(s.from_op_id, "-1")) {
                    list.addAll(getContextRows(s.from_op_id, null, null, null));
                } else {
                    list.addAll(getContextRows(s.from_op_id, s.before_or_after, null, null));
                }
            }
        }
        return listToWhereIdsMap(list);
    }

    public static void putIntoIdsMap(Map idMap, String table_id,Object id){
        List ids = new ArrayList();
        if(idMap.containsKey(table_id)){
            ids = (List)idMap.get(table_id);
        }
        else{
            idMap.put(table_id, ids);
        }
        ids.add(id);
    }


    public ExecContext getActionContext(ExecOp op, String op_action_type) {
        ExecContext ctx = new ExecContext();
        if (op != null) {
            List<Map> all_table_ids = getEnvCfgMatchedRows(op, op_action_type);
            ctx.addContexts(all_table_ids);
        }
        return ctx;
    }

    public List<Integer> getSubmittedReceivers(ExecOp op) {
        return contexts.stream()
                .filter(o -> (
                        (Objects.equals(o.get("__op_id__"), op.id)) &&
                                (Objects.equals(o.get("__op_st__"), OP_ST_SUBMITTED))
                )).map(o -> (Integer) o.get("receiver_")).collect(Collectors.toList());
    }


    public String getSubmittedFlowEdgeId(ExecOp op) {
        List ls = contexts.stream()
                .filter(o -> (
                        (Objects.equals(o.get("__op_id__"), op.id)) &&
                                (Objects.equals(o.get("__op_st__"), OP_ST_SUBMITTED))
                )).map(o -> o.get("edge_")).collect(Collectors.toList());
        if (ls.size() > 0) {
            return (String) ls.get(0);
        } else {
            return null;
        }
    }


    public String getSubmittedFlowNodeId(ExecOp op) {
        List ls = contexts.stream()
                .filter(o -> (
                        (Objects.equals(o.get("__op_id__"), op.id)) &&
                                (Objects.equals(o.get("__op_st__"), OP_ST_SUBMITTED))
                )).map(o -> o.get("node_")).collect(Collectors.toList());
        if (ls.size() > 0) {
            return (String) ls.get(0);
        } else {
            return null;
        }
    }

    public Map<String, List> getFillForeignTableIds(ExecOp op) {
        Map<String, List> result = new HashMap<>();
        List<Map> all_table_ids = getEnvCfgMatchedRows(op, ENV_CFG_FILL_FOREIGN);
        for (String table_id : all_table_ids.stream().map(o -> (String) o.get("__table_id__")).collect(Collectors.toList())) {
            List ids = all_table_ids.stream()
                    .filter(o -> Objects.equals(o.get("__table_id__"), table_id))
                    .distinct()
                    .collect(Collectors.toList());
            result.put(table_id, ids);
        }
        return result;
    }

    public Map<String, List> getFillCountersignTableIds(ExecOp op) {
        Map<String, List> result = new HashMap<>();
        List<Map> all_table_ids = getEnvCfgMatchedRows(op, ENV_CFG_FILL_COUNTERSIGN);
        for (String table_id : all_table_ids.stream().map(o -> (String) o.get("__table_id__")).collect(Collectors.toList())) {
            List ids = all_table_ids.stream().filter(o -> Objects.equals(o.get("__table_id__"), table_id)).distinct().collect(Collectors.toList());
            result.put(table_id, ids);
        }
        return result;
    }

    //    public List<Integer> getTableIds(String table_id){
//        val collect = contexts.stream()
//                .filter(o -> (
//                        (Objects.equals(o.get("__table_id__"), table_id))
//                )).map(o -> (Integer) o.get("id_")).collect(Collectors.toList());
//        return collect;
//    }
    public List<Integer> getTableIds(String table_id) {
        val collect = contexts.stream()
                .filter(o -> {
                    // 支持两种键名
                    Object tableIdValue = o.get("__table_id__");
                    if (tableIdValue == null) {
                        tableIdValue = o.get("table_id");
                    }
                    return Objects.equals(tableIdValue, table_id);
                })
                .map(o -> (Integer) o.get("id_"))
                .collect(Collectors.toList());
        return collect;
    }

    //获取所有的table行
    public List<Map> getTableRows(String table_id) {
        return contexts.stream()
                .filter(o -> (
                        (Objects.equals(o.get("__table_id__"), table_id))
                )).collect(Collectors.toList());
    }

    public Map<String, Object> getEnvironmentVariables(ExecOp op) {
        Map<String, Object> envVariables = new HashMap<>();
        for (ExecOpAction envCfg : op.opActions) {
            //缓存变量
            if (Objects.equals(envCfg.session_type, ExecContext.ENV_CFG_CACHE_VAR)) {
                List<Map> list = getContextRows(envCfg.from_op_id, envCfg.before_or_after, envCfg.from_table_id, envCfg.from_ds_id);
                String alias = envCfg.cache_value_alias;
                if(envCfg.from_ds_id!=null){
                    if (!list.isEmpty()) {
                        envVariables.put(alias, list.get(0).get(envCfg.from_ds_field_id));
                    }
                }
                else{
                    if (!list.isEmpty()) {
                        envVariables.put(alias, list.get(0).get(envCfg.from_table_col_id));
                    }
                }
            }
            //缓存值列表
            else if (Objects.equals(envCfg.session_type, ExecContext.ENV_CFG_CACHE_VAR_LIST)) {
                List<Map> list = getContextRows(envCfg.from_op_id, envCfg.before_or_after, envCfg.from_table_id, envCfg.from_ds_id);
                String alias = envCfg.cache_value_alias;
                Table tbl = (Table) Modules.getInstance().get(envCfg.from_table_id, false);
                TableCol field = tbl.getCol(envCfg.from_table_col_id);
                if (!list.isEmpty()) {
                    List values = list.stream().map(o -> o.get(field)).collect(Collectors.toList());
                    //数据集合做增补
                    if (envVariables.get(alias) != null) {
                        List exist = (List) envVariables.get(alias);
                        exist.addAll(values);
                        envVariables.put(alias, exist);
                    } else {
                        envVariables.put(alias, values);
                    }
                }
            }
            //缓存第一行
            else if (Objects.equals(envCfg.session_type, ExecContext.ENV_CFG_CACHE_ROW)) {
                List<Map> list = getContextRows(envCfg.from_op_id, envCfg.before_or_after, envCfg.from_table_id, envCfg.from_ds_id);
                String alias = envCfg.cache_value_alias;
                if (!list.isEmpty()) {
                    envVariables.put(alias, list.get(0));
                }
            }
        }
        return envVariables;
    }
}
