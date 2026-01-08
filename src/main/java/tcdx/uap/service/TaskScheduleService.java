package tcdx.uap.service;

import com.alibaba.fastjson2.JSON;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tcdx.uap.common.entity.page.TableDataInfo;
import tcdx.uap.common.utils.Lutils;
import tcdx.uap.common.utils.SmsUtils;
import tcdx.uap.common.utils.SqlUtil;
import tcdx.uap.common.utils.xss.SQLParser;
import tcdx.uap.controller.BusinessController;
import tcdx.uap.mapper.BusinessMapper;
import tcdx.uap.mapper.SystemMapper;
import tcdx.uap.service.entities.*;
import tcdx.uap.service.store.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpSession;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.TemporalAccessor;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class TaskScheduleService {

    private static final Logger log = LoggerFactory.getLogger(TaskScheduleService.class);

    private static final String SCHEDULE_TN = "v_task_schedule";
    private static final String NOTIFY_TN   = "v_task_notify";

    @Resource
    private BaseDBService base;
    @Autowired
    private BaseDBService baseDBService;
    @Autowired
    private BusinessService businessService;
    @Autowired
    private BusinessMapper businessMapper;
    @Autowired
    private SystemService systemService;
    @Autowired
    private SystemMapper systemMapper;
    @Autowired
    private HttpSession httpSession;
    @Autowired
    private BusinessController businessController;

    /** 主入口：扫描并执行 */
    public void executeDueTasks() {

        List<Map> tasks = base.selectEq(SCHEDULE_TN, Lutils.genMap(1,1));

        long nowMillis = System.currentTimeMillis();
        Instant now = Instant.ofEpochMilli(nowMillis);

        for (Map task : tasks) {
            try {
                if (isDue(task, nowMillis)) {
                    runTask(task, now);
                }
            } catch (Exception ex) {
                log.error("执行任务 名称={} 出错", task.get("message_content"), ex);
            }
        }
    }

    /**
     * 判断任务是否满足时间条件（轮询触发）
     */
    private boolean isDue(Map<String, Object> task, long nowMillis) {
        Instant start = parseIso((String) task.get("start_time"));
        Instant end = parseIso((String) task.get("end_time"));

        if (start == null || end == null) {
            return false; // 配置错误，时间为空
        }

        Instant now = Instant.ofEpochMilli(nowMillis);
        if (now.isBefore(start) || now.isAfter(end)) {
            return false; // 当前时间不在执行区间内
        }

        // 获取轮询间隔
        String pollingStr = (String) task.get("polling_interval");
        Duration interval = parseInterval(pollingStr);
        if (interval == null) {
            return false; // 配置错误，轮询间隔无法解析
        }

        // 判断上次执行时间
        Instant lastExec = parseIso((String) task.get("last_executed_time"));

        // 如果从未执行过，立即执行
        if (lastExec == null) {
            return true;
        }

        // 判断间隔是否满足
        Duration sinceLast = Duration.between(lastExec, now);
        return sinceLast.compareTo(interval) >= 0;
    }


    /* -------- 真正执行 -------- */
    private void runTask(Map task, Instant now) throws Exception {
        Integer id   = (Integer) task.get("id");
        String action = task.get("action") == null ? "" : task.get("action").toString().toLowerCase();

        if ("sql".equals(action)) {
            execSql(task);
        } else if ("message".equals(action)) {
            //消息推送 （短信/个人事务/公众号/小程序/邮件）
            insertNotify(task);
        } else {
            log.warn("任务 id={} 未知 action={}", id, action);
        }

    }

    /* -------- 执行自定义 SQL -------- */
    private void execSql(Map task) {
        String sql = (String) task.get("defined_sql");
        if (sql == null || sql.trim().isEmpty()) {
            log.warn("任务 id={} 没有 defined_sql", task.get("id"));
            return;
        }
        log.info("任务 id={} 执行 SQL：{}", task.get("id"), sql);
        base.executeSql(sql);
    }

    /* -------- 写入通知表 -------- */
    //消息推送 （短信/个人事务/公众号/小程序/邮件）
    private void insertNotify(Map task) throws Exception {
        //获取数据源
        Integer id = (Integer) task.get("id");
        String ds_id =  task.get("ds_id").toString();
        String rec_staff_ds_field_id =  task.get("rec_staff_ds_field_id").toString();
        String dead_time_ds_field_id =  task.get("dead_time_ds_field_id").toString();
        String beforedays = task.get("before_time").toString();

        CompDataSource ds = (CompDataSource) Modules.getInstance().get(ds_id,true);
//        CompDataSourceField rec_staff_ds_field = (CompDataSourceField) Modules.getInstance().get(rec_staff_ds_field_id,true);
//        CompDataSourceField dead_time_ds_field = (CompDataSourceField) Modules.getInstance().get(dead_time_ds_field_id,true);

        String notificationMethod = task.get("notification_method").toString();
        Map submitMap =Lutils.genMap("pageSize",3000,"pageNum",1);
        ExecContext execContext = new ExecContext();
        UserAction userAction = new UserAction();
        TableDataInfo tableInfo= CompUtils.getInstance().get_ds_data(ds, submitMap, execContext, userAction, true);
        List<?> tmp = tableInfo.getRows();
        List<Map> ds_info = new ArrayList<>();

        for (Object obj : tmp) {
            if (obj instanceof Map) {
                ds_info.add((Map) obj);
            }
        }

        //数据包含receiver 且  fields

        /* 更新 last_executed_time（varchar，仍存 ISO 字符串） */
        base.updateEq(
                SCHEDULE_TN,
                Lutils.genMap("last_executed_time", Lutils.getTime()),
                Lutils.genMap("id", id)
        );

        if(notificationMethod.equals("personal_affairs")){
            makePersonalAffairMessage(ds,ds_info,task,rec_staff_ds_field_id,dead_time_ds_field_id,beforedays);

        }else if(notificationMethod.equals("sms")){
            makeSMSMessage(ds,ds_info,task,rec_staff_ds_field_id,dead_time_ds_field_id,beforedays);

        }else if(notificationMethod.equals("email")){

        }else if(notificationMethod.equals("wechat")){

        }else if(notificationMethod.equals("official")){

        }else if(notificationMethod.equals("small_program")){

        }


    }
//    @SuppressWarnings("unchecked")
//    private void makeSMSMessage(CompDataSource ds,
//                                List<Map> dsInfo,
//                                Map task,
//                                String recStaffDsField,
//                                String deadTimeDsField,
//                                String beforedays) {
//        if (ds == null || dsInfo == null || dsInfo.isEmpty() || task == null) return;
//
//        final String tableId = ds.table_id;
//
//        // --- 遍历获取对应字段 ---
//        String receiverKey = null;
//        String deadlineKey = null;
//
//        for (CompDataSourceField f : ds.fields) {
//            if (f == null) continue;
//            if (f.id != null) {
//                if (f.id.equals(recStaffDsField)) {
//                    receiverKey = f.field; // 接收人字段
//                } else if (f.id.equals(deadTimeDsField)) {
//                    deadlineKey = f.field; // 截止时间字段
//                }
//            }
//        }
//
//        // N 天内到期（含今天且未过期）
//        int days = 0;
//        try {
//            if (beforedays != null && beforedays.trim().length() > 0) {
//                days = Integer.parseInt(beforedays.trim());
//                if (days < 0) days = 0;
//            }
//        } catch (Exception ignore) { days = 0; }
//        final long windowMs = days * 24L * 60L * 60L * 1000L;
//
//        // is_repeat
//        boolean isRepeat = false;
//        Object repeatObj = task.get("is_repeat");
//        if (repeatObj instanceof Boolean) {
//            isRepeat = (Boolean) repeatObj;
//        } else if (repeatObj != null) {
//            isRepeat = Boolean.parseBoolean(String.valueOf(repeatObj));
//        }
//
//        // —— 1) 预备：挑选用于摘要的展示字段（避免把接收人和截止时间再写进摘要里）
//        List<CompDataSourceField> displayFields = new ArrayList<>();
//        if (ds.fields != null) {
//            for (CompDataSourceField f : ds.fields) {
//                if (f == null || f.fieldName == null) continue;
//                String key = f.field;
//                if (key == null) continue;
//                if (key.equals(receiverKey)) continue;
//                if (deadlineKey != null && key.equals(deadlineKey)) continue;
//                displayFields.add(f);
//            }
//        }
//
//        long now = System.currentTimeMillis();
//
//        // —— 2) 每个接收人：收集摘要行 + 本轮涉及的 rowId 列表
//        Map<Integer, List<String>> receiverLines = new LinkedHashMap<>();
//        Map<Integer, List<Integer>> receiverRowIds = new LinkedHashMap<>();
//
//        for (Map raw : dsInfo) {
//            if (raw == null) continue;
//            Map<String, Object> record = (Map<String, Object>) raw;
//
//            // 接收人（用户ID）
//            int receiver = safeParseInt(record.get(receiverKey), -1);
//            if (receiver <= 0 || receiver == 10000) continue;
//
//            // 截止时间过滤：未过期且在 N 天窗口内
//            if (deadlineKey != null) {
//                Object deadVal = record.get(deadlineKey);
//                if (deadVal == null) continue;
//                Long deadline = tryParseToEpochMillis(deadVal); // Use the updated method for parsing
//
//                if (deadline == null) continue;
//
//                long delta = deadline - now;
//                if (delta < 0) continue;        // 已过期
//                if (delta > windowMs) continue; // 超出窗口
//            }
//
//            int rowId = safeParseInt(record.get("id_"), -1);
//            if(rowId == -1){
//                rowId = safeParseInt(record.get("z_"+tableId+"_id"), -1);
//
//            }
//            String nodeId = defaultStr(record.get("node_"), "node1");
//
//            // 去重：非重复模式下，如该（表+行+节点+人）已存在，跳过
//            if (!isRepeat) {
//                List<Map> exist = baseDBService.selectEq(
//                        NOTIFY_TN,
//                        Lutils.genMap("table_id", tableId, "row_id", rowId, "node_id", nodeId, "user_id", receiver)
//                );
//                if (exist != null && !exist.isEmpty()) continue;
//            }
//
//            // 生成摘要行（尽量短）
//            String summary = buildSmsLine(record, displayFields, deadlineKey);
//
//            // 写一条去重标记数据（本条记录）到 v_task_notify，is_sent=false
//            Map<String, Object> row = new HashMap<>();
//            row.put("task_schedule_id", safeParseInt(task.get("id"), -1));
//            row.put("notification_time", Lutils.getTime());
//            row.put("user_id", receiver);
//            row.put("notification_content", summary);
//            row.put("is_sent", false);
//            row.put("row_id", rowId);
//            row.put("node_id", nodeId);
//            row.put("table_id", tableId);
//            base.insertMap(NOTIFY_TN, row);
//
//            // 聚合
//            receiverLines.computeIfAbsent(receiver, k -> new ArrayList<>()).add(summary);
//            receiverRowIds.computeIfAbsent(receiver, k -> new ArrayList<>()).add(rowId);
//        }
//
//        if (receiverLines.isEmpty()) return;
//
//        // —— 3) 聚合并发送（每个接收人 1 条短信）
//        String title = defaultStr(task.get("notify_title"), "系统通知");
//        for (Map.Entry<Integer, List<String>> e : receiverLines.entrySet()) {
//            Integer receiver = e.getKey();
//            List<String> lines = e.getValue();
//            if (lines == null || lines.isEmpty()) continue;
//
//            // 短信长度控制：最多 5 行，超出则提示“等 X 条”
//            int maxLines = 1;
//            List<String> toSend = (lines.size() > maxLines) ? lines.subList(0, maxLines) : lines;
//            String more = (lines.size() > maxLines) ? ("（等 " + lines.size() + " 条）") : "";
//
//            String content = title;
//
//            // 获取手机号
//            String phone = getUserMobile(receiver);
//            if (phone == null || phone.trim().isEmpty()) {
//                log.warn("用户 {} 未找到手机号，无法发送短信：{}", receiver, content);
//                continue;
//            }
//
//            // 发送
//            com.aliyun.dysmsapi20170525.models.SendSmsResponse resp = SmsUtils.sendMessage(phone, content);
//            boolean ok = isAliyunOk(resp);
//
//            // 发一条“汇总短信”的记录
//            Map<String, Object> agg = new HashMap<>();
//            agg.put("task_schedule_id", safeParseInt(task.get("id"), -1));
//            agg.put("notification_time", Lutils.getTime());
//            agg.put("user_id", receiver);
//            agg.put("notification_content", content);
//            agg.put("is_sent", ok);
//            agg.put("row_id", -1);       // 汇总行使用 -1
//            agg.put("node_id", "sms");   // 标记来源
//            agg.put("table_id", tableId);
//            base.insertMap(NOTIFY_TN, agg);
//
//            // 成功的话，把本轮该接收人的所有去重标记更新为 is_sent=true
//            if (ok) {
//                List<Integer> ids = receiverRowIds.getOrDefault(receiver, Collections.emptyList());
//                for (Integer rid : ids) {
//                    base.updateEq(NOTIFY_TN,
//                            Lutils.genMap("is_sent", true),
//                            Lutils.genMap("table_id", tableId, "user_id", receiver, "row_id", rid));
//                }
//            }
//        }
//    }

    @SuppressWarnings("unchecked")
    private void makeSMSMessage(CompDataSource ds,
                                List<Map> dsInfo,
                                Map task,
                                String recMobileDsField,  // 改为手机号字段
                                String deadTimeDsField,
                                String beforedays) {
        if (ds == null || dsInfo == null || dsInfo.isEmpty() || task == null) return;

        final String tableId = ds.table_id;

        // --- 遍历获取对应字段 ---
        String mobileKey = null;  // 改为手机号字段
        String deadlineKey = null;

        for (CompDataSourceField f : ds.fields) {
            if (f == null) continue;
            if (f.id != null) {
                if (f.id.equals(recMobileDsField)) {
                    mobileKey = f.field; // 手机号字段
                } else if (f.id.equals(deadTimeDsField)) {
                    deadlineKey = f.field; // 截止时间字段
                }
            }
        }

        // 验证手机号字段是否存在
        if (mobileKey == null) {
            log.warn("未找到配置的手机号字段: {}", recMobileDsField);
            return;
        }

        // N 天内到期（含今天且未过期）
        int days = 0;
        try {
            if (beforedays != null && beforedays.trim().length() > 0) {
                days = Integer.parseInt(beforedays.trim());
                if (days < 0) days = 0;
            }
        } catch (Exception ignore) { days = 0; }
        final long windowMs = days * 24L * 60L * 60L * 1000L;

        // is_repeat
        boolean isRepeat = false;
        Object repeatObj = task.get("is_repeat");
        if (repeatObj instanceof Boolean) {
            isRepeat = (Boolean) repeatObj;
        } else if (repeatObj != null) {
            isRepeat = Boolean.parseBoolean(String.valueOf(repeatObj));
        }

        // —— 1) 预备：挑选用于摘要的展示字段
        List<CompDataSourceField> displayFields = new ArrayList<>();
        if (ds.fields != null) {
            for (CompDataSourceField f : ds.fields) {
                if (f == null || f.fieldName == null) continue;
                String key = f.field;
                if (key == null) continue;
                if (key.equals(mobileKey)) continue;  // 跳过手机号字段
                if (deadlineKey != null && key.equals(deadlineKey)) continue;
                displayFields.add(f);
            }
        }

        long now = System.currentTimeMillis();

        // —— 2) 每个手机号：收集摘要行 + 本轮涉及的 rowId 列表
        Map<String, List<String>> mobileLines = new LinkedHashMap<>();  // key改为String(手机号)
        Map<String, List<Integer>> mobileRowIds = new LinkedHashMap<>(); // key改为String(手机号)

        for (Map raw : dsInfo) {
            if (raw == null) continue;
            Map<String, Object> record = (Map<String, Object>) raw;

            // 直接获取手机号
            Object mobileObj = record.get(mobileKey);
            if (mobileObj == null) continue;

            String mobile = String.valueOf(mobileObj).trim();
            // 简单的手机号格式验证
            if (mobile.isEmpty() || mobile.length() < 11 || !mobile.matches("\\d+")) {
                log.warn("无效的手机号格式: {}", mobile);
                continue;
            }

            // 截止时间过滤：未过期且在 N 天窗口内
            if (deadlineKey != null) {
                Object deadVal = record.get(deadlineKey);
                if (deadVal == null) continue;
                Long deadline = tryParseToEpochMillis(deadVal);

                if (deadline == null) continue;

                long delta = deadline - now;
                if (delta < 0) continue;        // 已过期
                if (delta > windowMs) continue; // 超出窗口
            }

            int rowId = safeParseInt(record.get("id_"), -1);
            if(rowId == -1){
                rowId = safeParseInt(record.get("z_"+tableId+"_id"), -1);
            }
            String nodeId = defaultStr(record.get("node_"), "node1");

            // 去重：非重复模式下，如该（表+行+节点+手机号）已存在，跳过
            if (!isRepeat) {
                List<Map> exist = baseDBService.selectEq(
                        NOTIFY_TN,
                        Lutils.genMap("table_id", tableId, "row_id", rowId, "node_id", nodeId, "mobile", mobile)  // 改为mobile字段
                );
                if (exist != null && !exist.isEmpty()) continue;
            }

            // 生成摘要行（尽量短）
            String summary = buildSmsLine(record, displayFields, deadlineKey);

            // 写一条去重标记数据（本条记录）到 v_task_notify，is_sent=false
            Map<String, Object> row = new HashMap<>();
            row.put("task_schedule_id", safeParseInt(task.get("id"), -1));
            row.put("notification_time", Lutils.getTime());
            row.put("mobile", mobile);  // 改为存储手机号
            row.put("user_id", 0);      // 用户ID设为0或保留原逻辑，根据需求调整
            row.put("notification_content", summary);
            row.put("is_sent", false);
            row.put("row_id", rowId);
            row.put("node_id", nodeId);
            row.put("table_id", tableId);
            base.insertMap(NOTIFY_TN, row);

            // 聚合
            mobileLines.computeIfAbsent(mobile, k -> new ArrayList<>()).add(summary);
            mobileRowIds.computeIfAbsent(mobile, k -> new ArrayList<>()).add(rowId);
        }

        if (mobileLines.isEmpty()) return;

        // —— 3) 聚合并发送（每个手机号 1 条短信）
        String title = defaultStr(task.get("notify_title"), "系统通知");
        for (Map.Entry<String, List<String>> e : mobileLines.entrySet()) {
            String mobile = e.getKey();  // 直接使用手机号
            List<String> lines = e.getValue();
            if (lines == null || lines.isEmpty()) continue;

            // 短信长度控制：最多 5 行，超出则提示"等 X 条"
            int maxLines = 1;
            List<String> toSend = (lines.size() > maxLines) ? lines.subList(0, maxLines) : lines;
            String more = (lines.size() > maxLines) ? ("（等 " + lines.size() + " 条）") : "";

            String content = title;

            // 直接使用配置的手机号，无需再查询
            if (mobile == null || mobile.trim().isEmpty()) {
                log.warn("手机号为空，无法发送短信：{}", content);
                continue;
            }

            // 发送
            com.aliyun.dysmsapi20170525.models.SendSmsResponse resp = SmsUtils.sendMessage(mobile, content);
            boolean ok = isAliyunOk(resp);

            // 发一条"汇总短信"的记录
            Map<String, Object> agg = new HashMap<>();
            agg.put("task_schedule_id", safeParseInt(task.get("id"), -1));
            agg.put("notification_time", Lutils.getTime());
            agg.put("mobile", mobile);  // 存储手机号
            agg.put("notification_content", content);
            agg.put("is_sent", ok);
            agg.put("row_id", -1);       // 汇总行使用 -1
            agg.put("node_id", "sms");   // 标记来源
            agg.put("table_id", tableId);
            base.insertMap(NOTIFY_TN, agg);

            // 成功的话，把本轮该手机号的所有去重标记更新为 is_sent=true
            if (ok) {
                List<Integer> ids = mobileRowIds.getOrDefault(mobile, Collections.emptyList());
                for (Integer rid : ids) {
                    base.updateEq(NOTIFY_TN,
                            Lutils.genMap("is_sent", true),
                            Lutils.genMap("table_id", tableId, "mobile", mobile, "row_id", rid));  // 改为mobile条件
                }
            }
        }
    }

    /**
     * Converts an Object (Timestamp, String, etc.) to epoch milliseconds.
     */
    private static Long tryParseToEpochMillis(Object val) {
        try {
            if (val instanceof Number) {
                long x = ((Number) val).longValue();
                if (x < 1_000_000_000_000L) x *= 1000L; // Treat as seconds
                return x;
            }
            if (val instanceof java.sql.Timestamp) {
                return ((java.sql.Timestamp) val).getTime(); // Timestamp to epoch millis
            }
            String s = String.valueOf(val).trim();
            if (s.isEmpty()) return null;
            if (s.matches("^\\d{10}$")) return Long.parseLong(s) * 1000L;
            if (s.matches("^\\d{13}$")) return Long.parseLong(s);

            // Parse known datetime formats
            String[] patterns = new String[] {
                    "yyyy-MM-dd HH:mm:ss",
                    "yyyy-MM-dd HH:mm",
                    "yyyy/MM/dd HH:mm:ss",
                    "yyyy/MM/dd HH:mm",
                    "yyyy-MM-dd",
                    "yyyy/MM/dd"
            };
            for (String p : patterns) {
                try {
                    java.time.format.DateTimeFormatter f = java.time.format.DateTimeFormatter.ofPattern(p);
                    if (p.endsWith("dd")) {
                        java.time.LocalDate d = java.time.LocalDate.parse(s, f);
                        return d.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli();
                    } else {
                        java.time.LocalDateTime dt = java.time.LocalDateTime.parse(s, f);
                        return dt.atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli();
                    }
                } catch (Exception ignore) { }
            }
        } catch (Exception ignore) { }
        return null;
    }



    /* ===================== 短信专用的小工具 ===================== */
    /** 简单的时间解析器，可根据你数据格式调整 */
    private LocalDateTime parseToDateTime(String value) {
        try {
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            return LocalDateTime.parse(value, fmt);
        } catch (Exception e) {
            try {
                DateTimeFormatter fmt2 = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");
                return LocalDateTime.parse(value, fmt2);
            } catch (Exception ex) {
                return LocalDateTime.now();
            }
        }
    }
    private String buildSmsLine(Map<String, Object> record,
                                List<CompDataSourceField> displayFields,
                                String deadlineKey) {
        // Define maximum length for the summary (e.g., 20 characters)
        int maxLength = 20;
        List<String> pairs = new ArrayList<>();

        // Loop through the fields and construct the pairs
        for (CompDataSourceField f : displayFields) {
            if (pairs.size() >= 2) break;  // Limit the number of fields to 2 for brevity

            String key = (f != null ? f.field : null);
            if (key == null) continue;
            Object val = record.get(key);
            if (val == null) continue;

            // Add key-value pair, e.g., "标题=xxx"
            pairs.add(safeTrim(f.fieldName) + "=" + safeTrim(val));
        }

        // Join the pairs, use comma as a separator
        String base = String.join("，", pairs);

        // Check deadline and add to the message
        if (deadlineKey != null) {
            Object deadVal = record.get(deadlineKey);
            Long ddl = tryParseToEpochMillis(deadVal);
            if (ddl != null) {
                String ddlStr = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm").format(new java.util.Date(ddl));
                long leftMs = ddl - System.currentTimeMillis();
                String left = (leftMs >= 0) ? humanLeft(leftMs) : "已过期";
                if (!base.isEmpty()) {
                    base = base + "；截止：" + ddlStr + "（剩余" + left + "）";
                } else {
                    base = "截止：" + ddlStr + "（剩余" + left + "）";
                }
            }
        }

        // If the content is too long, truncate it to the max length
        if (base.length() > maxLength) {
            base = base.substring(0, maxLength) + "…";  // Truncate and add ellipsis
        }

        if (base.isEmpty()) base = "一条事项";
        return base;
    }


    private boolean isAliyunOk(com.aliyun.dysmsapi20170525.models.SendSmsResponse resp) {
        try {
            return resp != null
                    && resp.getBody() != null
                    && "OK".equalsIgnoreCase(resp.getBody().getCode());
        } catch (Exception e) {
            return false;
        }
    }

    private String getUserMobile(Integer userId) {
        try {
            // 优先从 v_user 表取（字段可能是 mobile / phone / tel 之一）
            List<Map> list = baseDBService.selectEq("v_user", Lutils.genMap("id", userId));
            if (list != null && !list.isEmpty()) {
                Map u = list.get(0);
                Object m = u.get("mobile");
                if (m == null) m = u.get("phone");
                if (m == null) m = u.get("tel");
                if (m != null) return String.valueOf(m);
            }
        } catch (Exception ignore) { }
        return null;
    }

    private static String safeTrim(Object v) {
        return (v == null) ? "" : String.valueOf(v).trim();
    }

    private static String humanLeft(long ms) {
        // 转成 X天Y小时（不满1小时按1小时计，避免“0小时”）
        long days = ms / (24L * 3600_000L);
        long hours = (ms % (24L * 3600_000L)) / 3600_000L;
        if (days == 0 && hours == 0) hours = 1;
        if (days > 0 && hours > 0) return days + "天" + hours + "小时";
        if (days > 0) return days + "天";
        return hours + "小时";
    }


    @SuppressWarnings("unchecked")
    private void makePersonalAffairMessage(CompDataSource ds,
                                           List<Map> dsInfo,
                                           Map task,
                                           String recStaffDsField,
                                           String deadTimeDsField,
                                           String beforedays) {
        if (ds == null || dsInfo == null || dsInfo.isEmpty() || task == null) return;
        List<CompDataSourceField> fields = ds.fields;
        if (fields == null || fields.isEmpty()) return;

        final String tableId = ds.table_id;

        // --- 遍历获取对应字段 ---
        String receiverKey = null;
        String deadlineKey = null;

        for (CompDataSourceField f : fields) {
            if (f == null) continue;
            if (f.id != null) {
                if (f.id.equals(recStaffDsField)) {
                    receiverKey = f.field; // 接收人字段
                } else if (f.id.equals(deadTimeDsField)) {
                    deadlineKey = f.field; // 截止时间字段
                }
            }
        }

        // N 天内到期（含今天且未过期）
        int days = 0;
        try {
            if (beforedays != null && beforedays.trim().length() > 0) {
                days = Integer.parseInt(beforedays.trim());
                if (days < 0) days = 0;
            }
        } catch (Exception ignore) { days = 0; }
        final long windowMs = days * 24L * 60L * 60L * 1000L;

        boolean isRepeat = false;
        Object repeatObj = task.get("is_repeat");
        if (repeatObj instanceof Boolean) {
            isRepeat = (Boolean) repeatObj;
        } else if (repeatObj != null) {
            isRepeat = Boolean.parseBoolean(String.valueOf(repeatObj));
        }

        // —— 构建表头（仅展示有 fieldName 的列）
        StringBuilder tableHead = new StringBuilder();
        tableHead.append("<style>\n")
                .append(".custom-table { width: 100%; border-collapse: collapse; font-family: Arial, sans-serif; font-size: 14px;}\n")
                .append(".custom-table th, .custom-table td { border: 1px solid #ccc; padding: 8px 12px; text-align: left;}\n")
                .append(".custom-table th { background-color: #f5f5f5; font-weight: bold;}\n")
                .append(".custom-table tr:nth-child(even) { background-color: #fafafa;}\n")
                .append("</style>\n")
                .append("<table class=\"custom-table\">\n")
                .append("<thead><tr>\n");
        List<CompDataSourceField> displayFields = new ArrayList<>();
        for (CompDataSourceField f : fields) {
            if (f != null && f.fieldName != null) {
                displayFields.add(f);
                tableHead.append("<th>").append(escapeNullable(f.fieldName)).append("</th>\n");
            }
        }
        tableHead.append("</tr></thead>\n");

        // —— 每个接收人一个 StringBuilder（表头 + <tbody> + 多行）
        Map<Integer, StringBuilder> userTableMap = new LinkedHashMap<>();
        long now = System.currentTimeMillis();

        for (Map raw : dsInfo) {
            if (raw == null) continue;
            Map<String, Object> record = (Map<String, Object>) raw;

            // 1) 接收人
            int receiver = safeParseInt(record.get(receiverKey), -1);
            if (receiver <= 0 || receiver == 10000) continue;

            // 2) 截止时间过滤：仅通知未过期且在 N 天内到期的记录
            if (deadlineKey != null) {
                Object deadVal = record.get(deadlineKey);
                if (deadVal == null) continue;
                Long deadlineEpochMs = tryParseToEpochMillis(deadVal);
                if (deadlineEpochMs == null) continue;

                long delta = deadlineEpochMs - now;
                if (delta < 0) continue;                 // 已过期不发
                if (delta > windowMs) continue;          // 超出 N 天窗口不发
            }

            // 3) 去重（非重复模式）
            int rowId = safeParseInt(record.get("id_"), -1);
            String nodeId = defaultStr(record.get("node_"), "node1");
            if (!isRepeat) {
                List<Map> exist = baseDBService.selectEq(
                        NOTIFY_TN,
                        Lutils.genMap("table_id", tableId, "row_id", rowId, "node_id", nodeId, "user_id", receiver)
                );
                if (exist != null && !exist.isEmpty()) continue;
            }

            // 4) 取/建该接收人的表缓冲
            StringBuilder sb = userTableMap.get(receiver);
            if (sb == null) {
                sb = new StringBuilder(tableHead);
                sb.append("<tbody>\n");
                userTableMap.put(receiver, sb);
            }

            // 5) 写入一行
            sb.append("<tr>\n");
            for (CompDataSourceField f : displayFields) {
                String key = (f != null ? f.field : null);
                Object val = (key == null ? null : record.get(key));
                sb.append("<td>").append(escapeNullable(val)).append("</td>\n");
            }
            sb.append("</tr>\n");

            // 6) 入库通知记录（按你原逻辑：每条行各插一条快照）
            String htmlSnapshot = buildCurrentHtmlSnapshot(sb);
            Map<String, Object> row = new HashMap<>();
            row.put("task_schedule_id", safeParseInt(task.get("id"), -1));
            row.put("notification_time", Lutils.getTime());
            row.put("user_id", receiver);
            row.put("notification_content", htmlSnapshot.trim());
            row.put("is_sent", false);
            row.put("row_id", rowId);
            row.put("node_id", nodeId);
            row.put("table_id", tableId);
            base.insertMap(NOTIFY_TN, row);
        }

        // —— 合并发送
        if (!userTableMap.isEmpty()) {
            String title = defaultStr(task.get("notify_title"), "");
            for (Map.Entry<Integer, StringBuilder> e : userTableMap.entrySet()) {
                Integer receiver = e.getKey();
                StringBuilder sb = e.getValue();
                closeTable(sb);
                String htmlFinal = sb.toString();

                Map item_map = Lutils.genMap("exec_id", 242);
                item_map.put("receivers", Lutils.genMap(
                        "op492_receivers", null,
                        "op493_receivers", Lutils.genList(receiver)
                ));
                item_map.put("submits", Lutils.genMap("view251_rows", Lutils.genList(
                        Lutils.genMap("zhuti", "系统通知_" + title + ":" + Lutils.getTime(),
                                "neirong", htmlFinal.trim(),
                                "fujian", "[]")
                )));
                try {
                    ExecResult execResult = new ExecResult();
                    businessController.handle_btn_click(item_map,null, execResult);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        }
    }

    /* ========= 辅助方法（一起放进类里；若已存在相同方法可复用/去重） ========= */

    private static String defaultStr(Object v, String def) {
        return (v == null) ? def : String.valueOf(v);
    }

    private static int safeParseInt(Object v, int def) {
        if (v == null) return def;
        try {
            if (v instanceof Number) return ((Number) v).intValue();
            String s = String.valueOf(v).trim();
            if (s.isEmpty()) return def;
            return Integer.parseInt(s);
        } catch (Exception e) {
            return def;
        }
    }

    private static String escapeNullable(Object v) {
        if (v == null) return "";
        String s = String.valueOf(v);
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }

    private static String buildCurrentHtmlSnapshot(StringBuilder sb) {
        StringBuilder snap = new StringBuilder(sb);
        closeTable(snap);
        return snap.toString();
    }

    private static void closeTable(StringBuilder sb) {
        sb.append("</tbody>\n</table>\n");
    }






//    <!--        通知当前人员的上级领导：-->
//<!--        部门领导：收到本级部门人员的通知-->
//<!--        根据选中人员，以及当前单子的当前接收人，获取本级或上级部门的通知人员-->

    private void makePersonalAffairMessage(CompDataSource dataSource, List<Map> dsInfo,Map task,HttpSession session) throws Exception {
        List<CompDataSourceField> dataSourceFields = dataSource.fields;

        // 构建 field -> name 映射
        Map<String, String> fieldNameMap = new LinkedHashMap<>();
        for (CompDataSourceField field : dataSourceFields) {
            String fieldKey = field.field;
            String fieldName = field.fieldName;
            if (fieldKey != null && fieldName != null) {
                fieldNameMap.put(fieldKey, fieldName);
            }
        }

        // 结果列表，可根据需要返回或处理
        List<Map<String, Object>> resultList = new ArrayList<>();
        String table_id = dataSource.table_id;
        Integer task_id = Integer.parseInt(task.get("id").toString());
//        Map scopedUser = get_scoped_user(task_id);
//        List scoped_users = (List) scopedUser.get("users");
//        List scoped_user_ids = Lutils.getColumnValueList(scoped_users, "id");
        Map data_map = new HashMap();
        Map messageUserMap = new HashMap();
        for (Map record : dsInfo) {
            //获取receiver的本级及上级领导
            int row_id = record.containsKey("id_")?Integer.parseInt(record.get("id_").toString()):-1;
            String node_id = record.containsKey("node_")?record.get("node_").toString():"node1";
            //判断是否需要重复通知
            boolean is_repeat = (Boolean) task.get("is_repeat");
            if(!record.containsKey("receiver_")){continue;}
            int receiver = Integer.parseInt(record.get("receiver_").toString());
            if(receiver == 10000) continue;
            if(!is_repeat){
                List<Map> maps = baseDBService.selectEq(NOTIFY_TN,
                        Lutils.genMap("table_id", table_id, "row_id", row_id,
                                "node_id", node_id,"user_id",receiver));
                if(maps.size()>0) continue;
            }
            //关联通知的本级或上级部门用户
//            List<Integer> userIds = businessMapper.getRelatedUsers(
//                    Lutils.genMap("receiver", receiver,
//                    "scoped_user_ids", scoped_user_ids));
            String html ;
            Integer userId = receiver;
//            for (Integer userId : userIds) {
                if (messageUserMap.containsKey(userId)) {
                    System.out.println(userId + " 在 map 中");
                    String oldTable = messageUserMap.get(userId).toString();
                    //做拼接
                    StringBuilder htmlBuilder = new StringBuilder();
                    htmlBuilder.append("<tr>\n");
                    for (CompDataSourceField field : dataSourceFields) {
                        String fieldKey = field.field;
                        Object value = record.get(fieldKey);
                        htmlBuilder.append("<td>").append(value != null ? value.toString() : "").append("</td>\n");
                    }
                    htmlBuilder.append("</tr>\n");
                    String updatedTable = oldTable.replace("</tbody>", htmlBuilder.toString() + "</tbody>");
                    messageUserMap.put(userId, updatedTable);
                    html = updatedTable;
                } else {
                    System.out.println(userId + " 不在 map 中");
                    StringBuilder htmlBuilder = new StringBuilder();
                    htmlBuilder.append("<style>\n")
                            .append(".custom-table { width: 100%; border-collapse: collapse; font-family: Arial, sans-serif; font-size: 14px;}\n")
                            .append(".custom-table th, .custom-table td { border: 1px solid #ccc; padding: 8px 12px; text-align: left;}\n")
                            .append(".custom-table th { background-color: #f5f5f5; font-weight: bold;}\n")
                            .append(".custom-table tr:nth-child(even) { background-color: #fafafa;}\n")
                            .append("</style>\n");

                    htmlBuilder.append("<table class=\"custom-table\">\n");
                    // 拼接表头
                    htmlBuilder.append("<thead><tr>\n");
                    for (CompDataSourceField field : dataSourceFields) {
                        if(field.fieldName==null) continue;
                        String name = field.fieldName;
                        htmlBuilder.append("<th>").append(name).append("</th>\n");
                    }
                    htmlBuilder.append("</tr></thead>\n");
                    // 拼接数据
                    htmlBuilder.append("<tbody>\n");
                    htmlBuilder.append("<tr>\n");
                    for (CompDataSourceField field : dataSourceFields) {
                        if(field.fieldName == null) continue;
                        String fieldKey = field.field;
                        Object value = record.get(fieldKey);
                        htmlBuilder.append("<td>").append(value != null ? value.toString() : "").append("</td>\n");
                    }
                    htmlBuilder.append("</tr>\n");
                    htmlBuilder.append("</tbody>\n");
                    htmlBuilder.append("</table>\n");
                    messageUserMap.put(userId, htmlBuilder.toString());
                    html = htmlBuilder.toString();
                }
                //存储通知记录
                Map<String,Object> row = new HashMap<String, Object>();
                row.put("task_schedule_id", Integer.parseInt(task.get("id").toString()));
                row.put("notification_time", Lutils.getTime());
                row.put("user_id", receiver);
                row.put("notification_content", html.trim());
                row.put("is_sent", false);
                row.put("row_id", row_id);
                row.put("node_id", node_id);
                row.put("table_id", table_id);
                base.insertMap(NOTIFY_TN, row);
//            }
        }

        List<Integer> keyList = new ArrayList<>(messageUserMap.keySet());
        for (int i = 0; i < keyList.size(); i++) {
            Integer receiver = keyList.get(i);
            String html = messageUserMap.get(receiver).toString();
            Map item_map = Lutils.genMap("exec_id", 242);
            //获取通知标题
            String title = task.containsKey("notify_title")?(String) task.get("notify_title"):"";
            item_map.put("receivers",Lutils.genMap(
                    "op492_receivers",null,
                    "op493_receivers",Lutils.genList(receiver)));

            item_map.put("submits",Lutils.genMap("view251_rows",Lutils.genList(
                    Lutils.genMap("zhuti","系统通知_"+title+":"+Lutils.getTime(),
                            "neirong",html.trim(),
                            "fujian","[]")
            )));
            try {
                ExecResult execResult = new ExecResult();
                businessController.handle_btn_click(item_map,session, execResult);
            }catch(Exception e){
                e.printStackTrace();
            }
        }
    }



    /* -------- 工具：ISO 时间解析 -------- */
    private Instant parseIso(String timeStr) {
        if (timeStr == null || timeStr.trim().isEmpty()) return null;
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss")
                    .withZone(ZoneOffset.ofHours(8));
            TemporalAccessor accessor = formatter.parse(timeStr);
            // 转为 Instant
            LocalDateTime ldt = LocalDateTime.from(accessor);
            return ldt.toInstant(ZoneOffset.ofHours(8));
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    /* -------- 工具：解析 “3 minutes” -------- */
    private Duration parseInterval(String str) {
        if (str == null || str.trim().isEmpty()) return Duration.ofMinutes(1);

        String[] arr = str.trim().split("\\s+");
        long num = Long.parseLong(arr[0]);
        String unit = arr.length > 1 ? arr[1].toLowerCase() : "minutes";

        if (unit.startsWith("s"))      return Duration.ofSeconds(num);
        else if (unit.startsWith("h")) return Duration.ofHours(num);
        else if (unit.startsWith("d")) return Duration.ofDays(num);
        else                           return Duration.ofMinutes(num);
    }



}
