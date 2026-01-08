package tcdx.uap.common.utils;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * sql操作工具类
 * 
 * @author ruoyi
 */
public class SqlUtil
{
    /**
     * 定义常用的 sql关键字
     */
    public static String SQL_REGEX = "and |extractvalue|updatexml|exec |insert |select |delete |update |drop |count |chr |mid |master |truncate |char |declare |or |+|user()";

    /**
     * 仅支持字母、数字、下划线、空格、逗号、小数点（支持多个字段排序）
     */
    public static String SQL_PATTERN = "[a-zA-Z0-9_\\ \\,\\.]+";

    /**
     * 限制orderBy最大长度
     */
    private static final int ORDER_BY_MAX_LENGTH = 500;

    /**
     * 检查字符，防止注入绕过
     */
    public static String escapeOrderBySql(String value)
    {
        if (StringUtils.isNotEmpty(value) && !isValidOrderBySql(value))
        {
            throw new UtilException("参数不符合规范，不能进行查询");
        }
        if (StringUtils.length(value) > ORDER_BY_MAX_LENGTH)
        {
            throw new UtilException("参数已超过最大限制，不能进行查询");
        }
        return value;
    }

    /**
     * 验证 order by 语法是否符合规范
     */
    public static boolean isValidOrderBySql(String value)
    {
        return value.matches(SQL_PATTERN);
    }

    /**
     * SQL关键字检查
     */
    public static void filterKeyword(String value)
    {
        if (StringUtils.isEmpty(value))
        {
            return;
        }
        String[] sqlKeywords = StringUtils.split(SQL_REGEX, "\\|");
        for (String sqlKeyword : sqlKeywords)
        {
            if (StringUtils.indexOfIgnoreCase(value, sqlKeyword) > -1)
            {
                throw new UtilException("参数存在SQL注入风险");
            }
        }
    }


    /**
     * 创建匹配mybatis结构的and条件
     *
     * @return
     */
    public static Map<String, Object> and(Map<String,Object>...args){
        List<Map<String, Object>> argList = new ArrayList<Map<String, Object>>();
        for(Map<String,Object> a: args){
            argList.add(a);
        }
        return and(argList);
    }

    public static Map<String, Object> and(List<Map<String,Object>> args){
        return MapUtils.G("tp", "a", "cas", args);
    }



    /**
     * 创建匹配mybatis结构的and条件
     *
     * @return
     */
    public static Map<String, Object> or(Map<String,Object>...args){
        return MapUtils.G("tp","o", "cas", args);
    }

    /**
     * 创建匹配mybatis结构的lk条件
     *
     * @return
     */
    public static Map<String, Object> lk(String col,String val){return MapUtils.Combine(MapUtils.New("tp","lk"),MapUtils.New("col", col),MapUtils.New("val", val));}
    public static Map<String, Object> eq(String col,Object val){return MapUtils.G("tp","eq","col", col,"val", val);}
    public static Map<String, Object> isnull(String col){return MapUtils.Combine(MapUtils.New("tp","isnull"),MapUtils.New("col", col));}
    public static Map<String, Object> notnull(String col){return MapUtils.Combine(MapUtils.New("tp","notnull"),MapUtils.New("col", col));}
    public static Map<String, Object> in(String col, List vals){return MapUtils.Combine(MapUtils.New("tp","in"),MapUtils.New("col", col),MapUtils.New("vals", vals));}
    public static Map<String, Object> notin(String col, List vals){return MapUtils.Combine(MapUtils.New("tp","notin"),MapUtils.New("col", col),MapUtils.New("vals", vals));}
}
