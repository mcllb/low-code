package tcdx.uap.common.utils.Listenser;

import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.event.AnalysisEventListener;
import com.alibaba.fastjson2.JSON;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tcdx.uap.service.BaseDBService;

import java.text.SimpleDateFormat;
import java.util.*;


/**
 * 直接用map接收数据
 *
 */

public class NoModleDataListener extends AnalysisEventListener<Map<Integer, String>>{

    private List<Map> task_link_Maps;
    private BaseDBService commonDBService;
    private String tableName;
    private String create_staff_nm;
    private String create_staff_no;
    private Object task_id;
    private int dept_id;
    private String dept_nm;


    
    private static final Logger LOGGER = LoggerFactory.getLogger(NoModleDataListener.class);
    /**
     * 每隔5条存储数据库，实际使用中可以3000条，然后清理list ，方便内存回收
     */
    private static final int BATCH_COUNT = 3000;
    List<Map<Integer, String>> list = new ArrayList<Map<Integer, String>>();



    public NoModleDataListener(List<Map> task_link_Maps, String tableName, String create_staff_nm,
                               String create_staff_no, Object task_id, int dept_id, String dept_nm, BaseDBService commonDBService) {
        this.task_link_Maps = task_link_Maps;
        this.commonDBService = commonDBService;
        this.create_staff_nm = create_staff_nm;
        this.create_staff_no = create_staff_no;
        this.tableName = tableName;
        this.task_id = task_id;
        this.dept_id = dept_id;
        this.dept_nm = dept_nm;
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
        //添加管理员  ly_sys_task_manager
        Map map = new HashMap();
        map.put("task_id",task_id);
        map.put("staff_no",create_staff_no);
        map.put("org_id",dept_id);
        System.out.println(task_id+","+create_staff_no+",,,,,,,,,,,,,,,,,,,,,,,,,,,");
        commonDBService.insertMap("ly_sys_task_manager",map);
        //int i = 1/0;
    }

    /**
     * 加上存储数据库
     */
    private void saveData() {
        Object link_id = task_link_Maps.get(0).get("id");
        List<Map> insertMapList = new ArrayList<>();
        for (int i = 0; i < list.size(); i++) {
            Map map = list.get(i);
            Map insertMap = new HashMap();
            insertMap.put("link_id",link_id);
            Set set = map.keySet();
            for (Object key:set) {
                Object o = map.get(key);
                int seq = (int) key;
                seq = seq+1;
                String name = "col"+ seq;
                insertMap.put(name,o);
            }
            String link1001_staff  = "link"+link_id+"_staff";
            String link1001_staff_nm  = "link"+link_id+"_staff_nm";
            String link1001_rec_staff  = "link"+link_id+"_rec_staff";
            String link1001_rec_staff_nm  = "link"+link_id+"_rec_staff_nm";
            insertMap.put(link1001_staff,create_staff_no);
            insertMap.put(link1001_staff_nm,create_staff_nm);
            insertMap.put(link1001_rec_staff,create_staff_no);
            insertMap.put(link1001_rec_staff_nm,create_staff_nm);
            String link1001_staff_dept = "link"+link_id+"_staff_dept";
            String link1001_staff_dept_nm = "link"+link_id+"_staff_dept_nm";
            String link1001_rec_staff_dept = "link"+link_id+"_rec_staff_dept";
            String link1001_rec_staff_dept_nm = "link"+link_id+"_rec_staff_dept_nm";
            insertMap.put(link1001_staff_dept,dept_id);
            insertMap.put(link1001_staff_dept_nm,dept_nm);
            insertMap.put(link1001_rec_staff_dept,dept_id);
            insertMap.put(link1001_rec_staff_dept_nm,dept_nm);
            String link1001_time  = "link"+link_id+"_time";
            Date date = new Date();
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
            String time = sdf.format(date);
            insertMap.put(link1001_time,time);
            insertMap.put("link_time",time);
            insertMap.put("link_rec_staff",create_staff_no);
            insertMap.put("link_rec_staff_nm",create_staff_nm);
            insertMap.put("link_staff",create_staff_no);
            insertMap.put("link_staff_nm",create_staff_nm);
            insertMap.put("link_staff_dept",dept_id);
            insertMap.put("link_staff_dept_nm",dept_nm);
            insertMap.put("link_rec_staff_dept",dept_id);
            insertMap.put("link_rec_staff_dept_nm",dept_nm);
            insertMap.put("link_from_key_nm","开始");
            insertMap.put("link_to_key_nm","待处理");
            insertMap.put("link_text","创建");
            insertMapList.add(insertMap);
        }
        commonDBService.insertByBatch(tableName,insertMapList);
        commonDBService.insertByBatch(tableName+"_log",insertMapList);
    }

    @Override
    public void invokeHeadMap(Map headMap, AnalysisContext context) {
        LOGGER.info("解析到的表头数据: {}", headMap);
        Map resultMap = new HashMap();
        resultMap.put("id","numeric(20)");
        Set set = headMap.keySet();
        for (Object key:set) {
            Object o = headMap.get(key);
            int seq = (int) key;
            seq = seq+1;
            String name = "col"+ seq;
            //添加表格字段信息 插入task_col_def表格
            Map task_col_def_insertMap = new HashMap();
            task_col_def_insertMap.put("name",name);
            task_col_def_insertMap.put("display_name",o);
            task_col_def_insertMap.put("task_id",task_id);
            task_col_def_insertMap.put("column_type","文本");
            task_col_def_insertMap.put("field_type","文本");
            task_col_def_insertMap.put("col_show",1);
            task_col_def_insertMap.put("field_show",1);
            commonDBService.insertMap("ly_sys_task_col_def",task_col_def_insertMap);
            Map task_link_field_def_insertMap = new HashMap();
            task_link_field_def_insertMap.put("task_id",task_id);
            task_link_field_def_insertMap.put("link_id",-1);
            task_link_field_def_insertMap.put("name",name);
            task_link_field_def_insertMap.put("field_type","文本");
            task_link_field_def_insertMap.put("field_show",1);
            commonDBService.insertMap("ly_sys_task_link_field_def",task_link_field_def_insertMap);
            resultMap.put(name,"varchar(1000)");
            resultMap.put("time_if_delete","varchar(100)");
        }
        resultMap.put("link_id","numeric(6)");
        for (int i = 0; i < task_link_Maps.size(); i++) {
            Map task_link = task_link_Maps.get(i);
            if(task_link.get("id")!=null){
                String link1001_staff  = "link"+task_link.get("id")+"_staff";
                String link1001_time  = "link"+task_link.get("id")+"_time";
                String link_1001_rec_staff  = "link"+task_link.get("id")+"_rec_staff";
                String link_staff_nm = "link"+task_link.get("id")+"_staff_nm";
                String link_rec_staff_nm = "link"+task_link.get("id")+"_rec_staff_nm";
                String link_from_key = "link"+task_link.get("id")+"_from_key";
                String link_to_key = "link"+task_link.get("id")+"_to_key";
                String link_staff_dept = "link"+task_link.get("id")+"_staff_dept";
                String link_staff_dept_nm = "link"+task_link.get("id")+"_staff_dept_nm";
                String link_rec_staff_dept = "link"+task_link.get("id")+"_rec_staff_dept";
                String link_rec_staff_dept_nm = "link"+task_link.get("id")+"_rec_staff_dept_nm";
                resultMap.put(link_from_key,"numeric(6)");
                resultMap.put(link_to_key,"numeric(6)");
                resultMap.put(link_staff_nm,"varchar(20)");
                resultMap.put(link_rec_staff_nm,"varchar(20)");
                resultMap.put(link_staff_dept,"varchar(20)");
                resultMap.put(link_staff_dept_nm,"varchar(20)");
                resultMap.put(link_rec_staff_dept,"varchar(20)");
                resultMap.put(link_rec_staff_dept_nm,"varchar(20)");
                resultMap.put(link1001_staff,"varchar(20)");
                resultMap.put(link1001_time,"varchar(100)");
                resultMap.put(link_1001_rec_staff,"varchar(20)");
                Map map = new HashMap();
                map.put("task_id",task_id);
                map.put("column_type","文本");
                map.put("field_type","文本");
                if(task_link.get("text").equals("创建")){
                    map.put("name",link1001_time);
                    map.put("display_name","[创建]时间");
                    commonDBService.insertMap("ly_sys_task_col_def",map);
                    map.put("name",link_staff_nm);
                    map.put("display_name","[创建]操作人名称");
                    commonDBService.insertMap("ly_sys_task_col_def",map);
                    map.put("name",link_rec_staff_nm);
                    map.put("display_name","[创建]接收人名称");
                    commonDBService.insertMap("ly_sys_task_col_def",map);
                    map.put("name",link_staff_dept_nm);
                    map.put("display_name","[创建]操作部门名称");
                    commonDBService.insertMap("ly_sys_task_col_def",map);
                    map.put("name",link_rec_staff_dept_nm);
                    map.put("display_name","[创建]接收部门名称");
                    commonDBService.insertMap("ly_sys_task_col_def",map);
                }
                if(task_link.get("text").equals("修改")){
                    map.put("name",link1001_time);
                    map.put("display_name","[修改]时间");
                    commonDBService.insertMap("ly_sys_task_col_def",map);
                    map.put("name",link_staff_nm);
                    map.put("display_name","[修改]操作人名称");
                    commonDBService.insertMap("ly_sys_task_col_def",map);
                    map.put("name",link_rec_staff_nm);
                    map.put("display_name","[修改]接收人名称");
                    commonDBService.insertMap("ly_sys_task_col_def",map);
                    map.put("name",link_staff_dept_nm);
                    map.put("display_name","[修改]操作部门名称");
                    commonDBService.insertMap("ly_sys_task_col_def",map);
                    map.put("name",link_rec_staff_dept_nm);
                    map.put("display_name","[修改]接收部门名称");
                    commonDBService.insertMap("ly_sys_task_col_def",map);
                }
                if(task_link.get("text").equals("转派")){
                    map.put("name",link1001_time);
                    map.put("display_name","[转派]时间");
                    commonDBService.insertMap("ly_sys_task_col_def",map);
                    map.put("name",link_staff_nm);
                    map.put("display_name","[转派]操作人名称");
                    commonDBService.insertMap("ly_sys_task_col_def",map);
                    map.put("name",link_rec_staff_nm);
                    map.put("display_name","[转派]接收人名称");
                    commonDBService.insertMap("ly_sys_task_col_def",map);
                    map.put("name",link_staff_dept_nm);
                    map.put("display_name","[转派]操作部门名称");
                    commonDBService.insertMap("ly_sys_task_col_def",map);
                    map.put("name",link_rec_staff_dept_nm);
                    map.put("display_name","[转派]接收部门名称");
                    commonDBService.insertMap("ly_sys_task_col_def",map);
                }
            }
        }
        resultMap.put("from_key","numeric(6) DEFAULT -1");
        resultMap.put("to_key","numeric(6) DEFAULT -2");
        resultMap.put("link_staff","varchar(20)");
        resultMap.put("link_time","varchar(100)");
        resultMap.put("link_rec_staff","varchar(20)");
        resultMap.put("link_staff_nm","varchar(20)");
        resultMap.put("link_rec_staff_nm","varchar(20)");
        resultMap.put("link_staff_dept","varchar(20)");
        resultMap.put("link_staff_dept_nm","varchar(20)");
        resultMap.put("link_rec_staff_dept","varchar(20)");
        resultMap.put("link_rec_staff_dept_nm","varchar(20)");
        resultMap.put("link_from_key_nm","varchar(20)");
        resultMap.put("link_to_key_nm"," varchar(20)");
        resultMap.put("link_text","varchar(20)");
        //commonDBService.createTable(tableName, resultMap);
        Map resultLogMap = resultMap;
        resultLogMap.put("log_id","numeric(20)");
        String logTableName = tableName+"_log";
        //commonDBService.createLogTable(logTableName,resultLogMap);
        Map map = new HashMap();
        map.put("task_id",task_id);
        map.put("column_type","文本");
        map.put("field_type","文本");
        map.put("name","link_time");
        map.put("display_name","当前动作创建时间");
        commonDBService.insertMap("ly_sys_task_col_def",map);
        map.put("name","link_staff_nm");
        map.put("display_name","当前操作人姓名");
        commonDBService.insertMap("ly_sys_task_col_def",map);
        map.put("name","link_rec_staff_nm");
        map.put("display_name","当前接收人姓名");
        commonDBService.insertMap("ly_sys_task_col_def",map);
        map.put("name","link_from_key_nm");
        map.put("display_name","前一节点名称");
        commonDBService.insertMap("ly_sys_task_col_def",map);
        map.put("name","link_to_key_nm");
        map.put("display_name","当前节点名称");
        commonDBService.insertMap("ly_sys_task_col_def",map);
        map.put("name","link_text");
        map.put("display_name","当前动作信息");
        commonDBService.insertMap("ly_sys_task_col_def",map);
        map.put("name","link_staff_dept_nm");
        map.put("display_name","当前操作部门");
        commonDBService.insertMap("ly_sys_task_col_def",map);
        map.put("name","link_rec_staff_dept_nm");
        map.put("display_name","当前接收部门");
        commonDBService.insertMap("ly_sys_task_col_def",map);
    }
}