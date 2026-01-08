package tcdx.uap.constant;

import tcdx.uap.common.utils.Lutils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import java.math.BigInteger;
/**
 * 通用常量信息
 * 
 * @author tctelecom
 */
public class Constants
{
    /**
     * UTF-8 字符集
     */
    public static final String UTF8 = "UTF-8";

    /**
     * GBK 字符集
     */
    public static final String GBK = "GBK";

    /**
     * 系统语言
     */
    public static final Locale DEFAULT_LOCALE = Locale.SIMPLIFIED_CHINESE;

    /**
     * http请求
     */
    public static final String HTTP = "http://";

    /**
     * https请求
     */
    public static final String HTTPS = "https://";

    /**
     * 通用成功标识
     */
    public static final String SUCCESS = "0";

    /**
     * 通用失败标识
     */
    public static final String FAIL = "1";

    /**
     * 登录成功
     */
    public static final String LOGIN_SUCCESS = "Success";

    /**
     * 注销
     */
    public static final String LOGOUT = "Logout";

    /**
     * 注册
     */
    public static final String REGISTER = "Register";

    /**
     * 登录失败
     */
    public static final String LOGIN_FAIL = "Error";

    /**
     * 系统用户授权缓存
     */
    public static final String SYS_AUTH_CACHE = "sys-authCache";

    /**
     * 参数管理 cache name
     */
    public static final String SYS_CONFIG_CACHE = "sys-config";

    /**
     * 参数管理 cache key
     */
    public static final String SYS_CONFIG_KEY = "sys_config:";

    /**
     * 字典管理 cache name
     */
    public static final String SYS_DICT_CACHE = "sys-dict";

    /**
     * 字典管理 cache key
     */
    public static final String SYS_DICT_KEY = "sys_dict:";

    /**
     * 资源映射路径 前缀
     */
    public static final String RESOURCE_PREFIX = "/profile";

    /**
     * RMI 远程方法调用
     */
    public static final String LOOKUP_RMI = "rmi:";

    /**
     * LDAP 远程方法调用
     */
    public static final String LOOKUP_LDAP = "ldap:";

    /**
     * LDAPS 远程方法调用
     */
    public static final String LOOKUP_LDAPS = "ldaps:";

    /**
     * 定时任务白名单配置（仅允许访问的包名，如其他需要可以自行添加）
     */
    public static final String[] JOB_WHITELIST_STR = { "com.ruoyi.quartz.task" };

    /**
     * 定时任务违规的字符
     */
    public static final String[] JOB_ERROR_STR = { "java.net.URL", "javax.naming.InitialContext", "org.yaml.snakeyaml",
            "org.springframework", "org.apache", "com.ruoyi.common.utils.file", "com.ruoyi.common.config", "com.ruoyi.generator" };


    static String DateString = "";
    static int tmpTimeCount = 0;
    static String UUID = "";
    public static synchronized String getUUID(){
        // 获取当前日期时间
        LocalDateTime now = LocalDateTime.now();
        // 创建一个DateTimeFormatter对象，指定日期时间格式
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
        // 使用format方法将LocalDateTime对象转换为字符串
        String nowDateString = now.format(formatter);
        if(DateString.equals(nowDateString)){
            tmpTimeCount ++;
        }
        else{
            tmpTimeCount = 0;
        }
        char letter = (char) (tmpTimeCount%26 + 'A');
        DateString = nowDateString;
        UUID = DateString + (int)(tmpTimeCount/26)+letter;  //126  135
        // 输出转换后的字符串
        return UUID;
    }

    public static int innerIndex = 0;
    public static String lastTimeFormatStr = "";
    public static synchronized String getTimeFormatId(){
        // 获取当前日期时间
        LocalDateTime now = LocalDateTime.now();
        // 创建一个DateTimeFormatter对象，指定日期时间格式
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");
        // 使用format方法将LocalDateTime对象转换为字符串
        String nowDateString = now.format(formatter);
        if(!lastTimeFormatStr.equals(nowDateString)){
            lastTimeFormatStr = nowDateString;
            innerIndex = 1;
        }
        else{
            innerIndex ++;
        }
        // 输出转换后的字符串
        return nowDateString.substring(2,nowDateString.length())+innerIndex;
    }

        public static String NumberToHex(String number) {
            // 创建一个BigInteger对象
            BigInteger bigInt = new BigInteger(number);
            // 将BigInteger转换为十六进制字符串
            return bigInt.toString(16);
        }

    static List<Map> DataSourceFieldDefinition = new ArrayList<Map>();
    public static List<Map> getDsFieldDefinition(){
        if(DataSourceFieldDefinition.isEmpty()) {
            DataSourceFieldDefinition.add(Lutils.genMap("field_type", "flow_field", "parent", "当前表", "tableName", "", "field", "create_time_", "flow_edge_id", "edge-1", "name", "创建时间"));
            DataSourceFieldDefinition.add(Lutils.genMap("field_type", "flow_field", "parent", "当前表", "tableName", "", "field", "create_user_", "flow_edge_id", "edge-1", "name", "创建人ID"));
            DataSourceFieldDefinition.add(Lutils.genMap("field_type", "flow_field", "parent", "当前表", "tableName", "", "field", "create_user_staff_nm", "flow_edge_id", "edge-1", "name", "创建人"));
            DataSourceFieldDefinition.add(Lutils.genMap("field_type", "flow_field", "parent", "当前表", "tableName", "", "field", "create_user_phone", "flow_edge_id", "edge-1", "name", "创建人电话"));
            DataSourceFieldDefinition.add(Lutils.genMap("field_type", "flow_field", "parent", "当前表", "tableName", "", "field", "node_label_", "flow_edge_id", "edge-1", "name", "当前节点"));
            DataSourceFieldDefinition.add(Lutils.genMap("field_type", "flow_field", "parent", "当前表", "tableName", "", "field", "edge_label_", "flow_edge_id", "edge-1", "name", "当前动作"));
            DataSourceFieldDefinition.add(Lutils.genMap("field_type", "flow_field", "parent", "当前表", "tableName", "", "field", "posted_time_", "flow_edge_id", "edge-1", "name", "派单时间"));
            DataSourceFieldDefinition.add(Lutils.genMap("field_type", "flow_field", "parent", "当前表", "tableName", "", "field", "finished_time_", "flow_edge_id", "edge-1", "name", "完成时间"));
            DataSourceFieldDefinition.add(Lutils.genMap("field_type", "flow_field", "parent", "当前表", "tableName", "", "field", "poster_", "flow_edge_id", "edge-1", "name", "派单人ID"));
            DataSourceFieldDefinition.add(Lutils.genMap("field_type", "flow_field", "parent", "当前表", "tableName", "", "field", "poster_staff_nm", "flow_edge_id", "edge-1", "name", "派单人"));
            DataSourceFieldDefinition.add(Lutils.genMap("field_type", "flow_field", "parent", "当前表", "tableName", "", "field", "poster_phone", "flow_edge_id", "edge-1", "name", "派单人电话"));
            DataSourceFieldDefinition.add(Lutils.genMap("field_type", "flow_field", "parent", "当前表", "tableName", "", "field", "receiver_", "flow_edge_id", "edge-1", "name", "接单人ID"));
            DataSourceFieldDefinition.add(Lutils.genMap("field_type", "flow_field", "parent", "当前表", "tableName", "", "field", "receiver_staff_nm", "flow_edge_id", "edge-1", "name", "接单人"));
            DataSourceFieldDefinition.add(Lutils.genMap("field_type", "flow_field", "parent", "当前表", "tableName", "", "field", "receiver_phone", "flow_edge_id", "edge-1", "name", "接单人电话"));
            DataSourceFieldDefinition.add(Lutils.genMap("field_type", "flow_field", "parent", "当前表", "tableName", "", "field", "update_time_", "flow_edge_id", "edge-1", "name", "更新时间"));
            DataSourceFieldDefinition.add(Lutils.genMap("field_type", "flow_field", "parent", "当前表", "tableName", "", "field", "update_user_", "flow_edge_id", "edge-1", "name", "更新人ID"));
            DataSourceFieldDefinition.add(Lutils.genMap("field_type", "flow_field", "parent", "当前表", "tableName", "", "field", "update_user_staff_nm", "flow_edge_id", "edge-1", "name", "更新人"));
            DataSourceFieldDefinition.add(Lutils.genMap("field_type", "flow_field", "parent", "当前表", "tableName", "", "field", "update_user_phone", "flow_edge_id", "edge-1", "name", "更新人电话"));
        }
        return DataSourceFieldDefinition;
    }

}