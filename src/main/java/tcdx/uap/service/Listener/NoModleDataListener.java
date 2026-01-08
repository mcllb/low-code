package tcdx.uap.service.Listener;

import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.event.AnalysisEventListener;
import com.alibaba.fastjson2.JSON;
import lombok.SneakyThrows;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tcdx.uap.common.utils.Lutils;
import tcdx.uap.service.BaseDBService;
import tcdx.uap.service.entities.TableCol;

import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;
import java.util.*;
import java.util.regex.Pattern;


/**
 * 直接用map接收数据
 *
 */

public class NoModleDataListener extends AnalysisEventListener<Map<Integer, String>>{


    private BaseDBService baseDBService;
    private String table_name;
    private String create_staff_nm;
    private int create_staff_no;
    private int dept_id;
    private List<TableCol> columns_list;



    
    private static final Logger LOGGER = LoggerFactory.getLogger(NoModleDataListener.class);
    /**
     * 每隔5条存储数据库，实际使用中可以3000条，然后清理list ，方便内存回收
     */
    private static final int BATCH_COUNT = 3000;
    List<Map<Integer, String>> list = new ArrayList<Map<Integer, String>>();



    public NoModleDataListener(String table_name, String create_staff_nm,List<TableCol> columns_list,
                               int create_staff_no, int dept_id,  BaseDBService baseDBService) {
        this.baseDBService = baseDBService;
        this.create_staff_nm = create_staff_nm;
        this.create_staff_no = create_staff_no;
        this.table_name = table_name;
        this.dept_id = dept_id;
        this.columns_list = columns_list;
    }


    @Override
    public void invoke(Map<Integer, String> data, AnalysisContext context) {
        LOGGER.info("解析到一条数据:{}", JSON.toJSONString(data));
        list.add(data);
        if (list.size() >= BATCH_COUNT) {
            saveData();
            list.clear();
        }
    }
    @Override
    public void doAfterAllAnalysed(AnalysisContext context) {
        saveData();
        LOGGER.info("所有数据解析完成！");

    }

    /**
     * 加上存储数据库
     */
    private void saveData(){
        List<Map> insertMapList = new ArrayList<>();
        String nowTime = Lutils.getTime();
        Map<Integer, String> colmap = list.get(0);
        Map<Integer, String> coltype = list.get(1);

        for (int i = 2; i < list.size(); i++) {
            Map map = list.get(i);
            Map insertMap = new HashMap();
            for (int j = 0; j < colmap.size(); j++) {
                String field = colmap.get(j);
                String coltype1 = coltype.get(j);
                Object o = map.get(j);
                if(o==null) continue;
                if(coltype1.equals("varchar")){
                    insertMap.put(field,o);
                }else if(coltype1.equals("numeric")){
                    insertMap.put(field,Integer.parseInt(o.toString()));
                }else if(coltype1.equals("timestamp")){
                    String dateStr = convertTime(o.toString());
                    Timestamp timestamp = Timestamp.valueOf(dateStr);
                    insertMap.put(field,timestamp);
                }
            }
            insertMap.put("create_user_",create_staff_no);
            insertMap.put("create_time_",new Date());

            insertMapList.add(insertMap);
        }
        baseDBService.insertByBatch(table_name,insertMapList);
        baseDBService.insertByBatch(table_name+"_log",insertMapList);
    }

    public static String convertTime(String timeString) {
        String targetPattern = "yyyy-MM-dd HH:mm:ss";
        if (timeString == null || timeString.trim().isEmpty()) {
            return null;
        }

        timeString = timeString.trim();

        // 创建灵活的 DateTimeFormatter
        DateTimeFormatter flexibleFormatter = new DateTimeFormatterBuilder()
                .appendOptional(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
                .appendOptional(DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss"))
                .appendOptional(DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm:ss"))
                .appendOptional(DateTimeFormatter.ofPattern("yyyy年MM月dd日 HH时mm分ss秒"))
                .appendOptional(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                .appendOptional(DateTimeFormatter.ofPattern("yyyy/MM/dd"))
                .appendOptional(DateTimeFormatter.ofPattern("yyyy.MM.dd"))
                .appendOptional(DateTimeFormatter.ofPattern("yyyy年MM月dd日"))
                .appendOptional(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
                .appendOptional(DateTimeFormatter.ofPattern("yyyyMMdd"))
                .appendOptional(DateTimeFormatter.ofPattern("HH:mm:ss"))
                .appendOptional(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                .appendOptional(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
                .appendOptional(DateTimeFormatter.ISO_INSTANT)
                .appendOptional(DateTimeFormatter.ofPattern("EEE MMM dd HH:mm:ss zzz yyyy"))
                .appendOptional(DateTimeFormatter.ofPattern("MM/dd/yyyy HH:mm:ss"))
                .appendOptional(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"))
                .appendOptional(DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss"))
                .parseDefaulting(ChronoField.HOUR_OF_DAY, 0)
                .parseDefaulting(ChronoField.MINUTE_OF_HOUR, 0)
                .parseDefaulting(ChronoField.SECOND_OF_MINUTE, 0)
                .toFormatter();

        try {
            LocalDateTime dateTime = LocalDateTime.parse(timeString, flexibleFormatter);
            return dateTime.format(DateTimeFormatter.ofPattern(targetPattern));
        } catch (Exception e) {
            throw new RuntimeException("无法解析的时间字符串: " + timeString, e);
        }
    }

    @Override
    public void invokeHeadMap(Map headMap, AnalysisContext context) {

    }
}