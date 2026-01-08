package tcdx.uap.common.utils;

import com.alibaba.fastjson.JSON;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;

import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.select.*;

import javax.servlet.http.HttpSession;
import java.io.*;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.TemporalAccessor;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.lang.reflect.Field;
import java.util.stream.Collectors;

public class Lutils {

    public static List<String> extractPlaceholders(String input) {
        List<String> placeholders = new ArrayList<>();
        Pattern pattern = Pattern.compile("#\\{(\\w+)\\}"); // 使用正则表达式匹配#{...}中的内容
        Matcher matcher = pattern.matcher(input);

        while (matcher.find()) { // 查找匹配项
            placeholders.add(matcher.group(1)); // 添加匹配到的内容到列表中
        }

        return placeholders;
    }

    public static Map[] MapListToArray(List<Map> list) {
        Map[] array = new Map[list.size()];
        for (int i = 0; i < list.size(); i++) {
            array[i] = list.get(i);
        }
        return array;
    }

    public static List getColumnValueList(List<Map> l1, String columnName){
        List valueList = new ArrayList<Object>();
        for(Map node:l1){
            valueList.add(node.get(columnName));
        }
        return valueList;
    }
    public static Map findItemInList(List<Map> l1,String field,String value){
        List valueList = new ArrayList<Object>();
        for(Map item:l1){
            if(item.containsKey(field)&&item.get(field).toString().equals(value))
                return item;
        }
        return null;
    }

    public static boolean isItemInList(List l1,int value){
        for(Object item:l1){
            if(Integer.parseInt(item.toString())==value)
                return true;
        }
        return false;
    }

    public static boolean isItemInList(List l1,String value){
        for(Object item:l1){
            if(item.toString().equals(value))
                return true;
        }
        return false;
    }

    public static Map findItemInList(List<Map> l1,String field,int value){
        List valueList = new ArrayList<Object>();
        for(Map item:l1){
            if(item.containsKey(field)&&Integer.parseInt(item.get(field).toString())==value)
                return item;
        }
        return null;
    }

    public static List<Map> findListInList(List<Map> l1,String field,int value){
        List<Map> valueList = new ArrayList<Map>();
        for(Map item:l1){
            if(item.containsKey(field)&&Integer.parseInt(item.get(field).toString())==value)
                valueList.add(item);
        }
        return valueList;
    }

    public static boolean safe_sql(String sql_seg){
        if(sql_seg.length()>63)
            return false;
        return sql_seg.matches("([A-Za-z0-9]|[\\u4e00-\\u9fa5]|_|\\.)+");
    }
    public static boolean safe_sql_list(List l){
        for(Object sql_seg:l){
            if(!safe_sql(sql_seg.toString()))
                return false;
        }
        return true;
    }


    public static boolean safe_sql_map(Map<String, Object> map){
        if(map==null)
            return true;
        for (Map.Entry<String, Object> entry : map.entrySet())
        {
            if(!safe_sql(entry.getKey().toString()))
                return false;
        }
        return true;
    }
    public static void insertToTree(Map p, Map node,String parentIdColumnName,String nodeIdColumnName,String childrenColumnName){
        //若是找到父节点
        if(node.get(parentIdColumnName).equals(p.get(nodeIdColumnName))){
            if(!p.containsKey(childrenColumnName)) p.put(childrenColumnName, new ArrayList());
            ((List)p.get(childrenColumnName)).add(node);
        }
        else{
            if(p.get(childrenColumnName)==null) return ;
            for(Map subP: ((List<Map>)p.get(childrenColumnName))){
                insertToTree(subP, node, parentIdColumnName, nodeIdColumnName,childrenColumnName);
            }
        }
    }
    //从叶子，往上创建树
    public static void buildTree(List<Map<String,Object>> children, Map root,String parentIdColumnName,String nodeIdColumnName,String childrenColumnName) {
        children = SortMapArrayByField(children,false,"order_id");
        List <Map> waitDeletedChild = new ArrayList<Map>();
        //在roots找到node的孩子节点，将孩子节点
        for (Map child : children) {
            if (child.get(parentIdColumnName).equals(root.get(nodeIdColumnName))) {
                //构建子树
                if(root.get(childrenColumnName)==null)
                    root.put(childrenColumnName, new ArrayList<Map>());
                ((List)root.get(childrenColumnName)).add(child);
                //从根集中，删除子节点
                waitDeletedChild.add(child);
            }
        }
        for(Map delc: waitDeletedChild)
            children.remove(delc);
        if( !root.get("tp").equals("folder") || root.get(childrenColumnName)!=null)
            children.add(root);
    }


    public static List<Map<String,Object>> SortMapArrayByField(List<Map<String,Object>> List,boolean type,String field){
        //排序
        Collections.sort(List, new Comparator<Map<String,Object>>() {
            @Override
            public int compare(Map<String,Object> o1, Map<String,Object> o2) {
                // 进行排序
                if (Integer.parseInt(String.valueOf(o1.get(field))) > Integer.parseInt(String.valueOf(o2.get(field)))) {
                    return 1;
                }
                if (Integer.parseInt(String.valueOf(o1.get(field))) == Integer.parseInt(String.valueOf(o2.get(field)))) {
                    return 0;
                }
                return -1;
            }
        });
        //降序
        if(type){
            Collections.reverse(List);
        }
        return List;
    }
    //快速生成Map
    public static Map<Integer,Object> genMap(Integer key,Object val){
        Map <Integer,Object> gm = new HashMap<Integer,Object>();
            gm.put(key, val);
        return gm;
    }
    //快速生成Map
    public static Map<String,Object> genMap(Object...args){
        Map <String,Object> gm = new HashMap<String,Object>();
        for(int i=0;i<args.length;i+=2){
            gm.put(args[i].toString(),args[i+1]);
        }
        return gm;
    }
    //快速生成List
    public static List<String> genList(String...args){
        List<String> re= new ArrayList<String>();
        for(int i=0;i<args.length;i++){
            re.add(args[i]);
        }
        return re;
    }

    //快速生成List
    public static List<Object> genList(Object...args){
        List<Object> re= new ArrayList<Object>();
        for(int i=0;i<args.length;i++){
            re.add(args[i]);
        }
        return re;
    }

    //快速生成List
    public static List<Integer> genList(Integer...args){
        List<Integer> re= new ArrayList<Integer>();
        for(int i=0;i<args.length;i++){
            re.add(args[i]);
        }
        return re;
    }

    public static List<Map> genList(Map...args){
        List<Map> re= new ArrayList<Map>();
        for(int i=0;i<args.length;i++){
            re.add(args[i]);
        }
        System.out.println(re.toString());
        return re;
    }

    public static List<List> genList(List<Map>...args){
        List re= new ArrayList<List<HashMap>>();
        for(int i=0;i<args.length;i++){
            re.add(args[i]);
        }
        System.out.println(re.toString());
        return re;
    }
    //快速生成List
    public static List<Long> genList(Long...args){
        List<Long> re= new ArrayList<Long>();
        for(int i=0;i<args.length;i++){
            re.add(args[i]);
        }
        return re;
    }

    public static Object copy(Object obj){
        return JSON.parse(JSON.toJSONString(obj));
    }

    public static Map<String,Object> copyMap(Map<String,Object> mp){
        HashMap <String,Object> gm = new HashMap<String,Object>();
        gm.putAll(mp);
        return gm;
    }

    public static String getTime(){
        Date nowTime=new Date();
        SimpleDateFormat time=new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
        return time.format(nowTime);
    }

    public static String nvl(Object o,String a){
        if(o==null)
            return a;
        return o.toString();
    }

    public static int nvl(Object o,int a){
        if(o==null)
            return a;
        return Integer.parseInt(o.toString());
    }

    public static boolean nvl(Object o,boolean a){
        if(o==null)
            return a;
        return Boolean.parseBoolean(o.toString());
    }


    public static final int JOIN_VALUE_TYPE_INT = 1;
    public static final int JOIN_VALUE_TYPE_STR = 2;
    public static List<Map> leftJoin(List<Map> left,List<Map> right,String left_on,String right_on,int valueType,String []putCols){
        List<Map> re = new ArrayList<Map>();
        for(Map le:left){
            Map em = Lutils.copyMap(le);
            Map find = null;
            if(valueType==JOIN_VALUE_TYPE_INT)
                find = Lutils.findItemInList(right,right_on,Integer.parseInt(le.get(left_on).toString()));
            else if(valueType==JOIN_VALUE_TYPE_STR)
                find = Lutils.findItemInList(right,right_on,le.get(left_on).toString());
            if(find!=null) {
                //更新字段值
                for (String col : putCols) {
                    em.put(col, find.get(col));
                }
            }
            re.add(em);
        }
        return re;
    }

    public static List<Map> leftJoin(List<Map> left,List<Map> right,String left_on,String right_on,int valueType,Map<String,Object> putCols){
        List<Map> re = new ArrayList<Map>();
        for(Map le:left){
            Map em = Lutils.copyMap(le);
            Map find = null;
            if(valueType==JOIN_VALUE_TYPE_INT)
                find = Lutils.findItemInList(right,right_on,Integer.parseInt(le.get(left_on).toString()));
            else if(valueType==JOIN_VALUE_TYPE_STR)
                find = Lutils.findItemInList(right,right_on,le.get(left_on).toString());
            if(find!=null) {
                //更新字段值
                for (Map.Entry<String, Object> entry : putCols.entrySet())
                {
                    em.put(entry.getValue(), find.get(entry.getKey()));
                }
            }
            re.add(em);
        }
        return re;
    }

    public static void listAddCol(List<Map> l, String colName, Object defaultVal){
        for(Map le:l){
            le.put(colName, defaultVal);
        }
    }

    public static boolean isInList(List<Object> l, int v){
        for(Object o:l){
            if(Integer.parseInt(o.toString())==v)
                return true;
        }
        return false;
    }

    public static boolean isInList(List<Object> l, String v){
        for(Object o:l){
            if(o.toString().equals(v))
                return true;
        }
        return false;
    }

    public static List<Map> makeTree(List<Map> datas,int startNodeParentId,int link_id) {
        List<Map> trees = new ArrayList<>();
        for (Map data : datas) {
            if ( startNodeParentId == Integer.parseInt(data.get("parent_org_id").toString())
                    && link_id== Integer.parseInt(data.get("link_id").toString())) {
                trees.add(data);
                data.put( "children",makeTree( datas, Integer.parseInt(data.get("id").toString()) ,link_id));
            }
        }
        return trees;
    }

    public static List<Map> makeTree2(List<Map> datas,String startNodeParentId) {
        List<Map> trees = new ArrayList<>();
        for (Map data : datas) {
            if (data.get("parentid")!=null && startNodeParentId.equals(data.get("parentid").toString())) {
                data.put("label",data.get("topic"));
                data.put("value",data.get("id"));
                trees.add(data);
                data.put( "children",makeTree2( datas,data.get("id").toString()));
            }
        }
        return trees;
    }


    public static <T extends Serializable> T deepCopy(T object) {
        try {
            ByteArrayOutputStream bao = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(bao);
            oos.writeObject(object);
            ByteArrayInputStream bis = new ByteArrayInputStream(bao.toByteArray());
            ObjectInputStream ois = new ObjectInputStream(bis);
            return (T) ois.readObject();
        } catch (IOException | ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

        public static <T> T CopyObjectFields(T obj) {
            T copyObj = null;
            try {
                Class<?> clazz = obj.getClass();
                copyObj = (T) clazz.newInstance(); // 创建实例
                Field[] fields = clazz.getDeclaredFields();
                for (Field field : fields) {
                    field.setAccessible(true); // 忽略访问权限，直接访问
                    Field copyField = clazz.getDeclaredField(field.getName());
                    copyField.setAccessible(true);
                    Object value = field.get(obj); // 获取原对象字段值
                    if (value != null) {
                        copyField.set(copyObj, value); // 设置复制对象字段值
                    }
                }
            } catch (InstantiationException | IllegalAccessException | NoSuchFieldException | SecurityException | IllegalArgumentException e) {
                e.printStackTrace();
            }
            return copyObj;
        }

    /**
     * 提取字段别名（返回 List<String>）
     */
    public static List<String> extractFieldAliases(String sql) {
        List<String> aliases = new ArrayList<>();
        // 提取 SELECT 子句内容
        int selectIndex = sql.toUpperCase().indexOf("SELECT");
        int fromIndex = sql.toUpperCase().indexOf("FROM");
        if (selectIndex != -1 && fromIndex != -1 && selectIndex < fromIndex) {
            String selectClause = sql.substring(selectIndex + 6, fromIndex).trim();
            // 定义更精确的正则表达式，匹配字段别名
            Pattern pattern = Pattern.compile("(?:\\w+\\.\\w+|\\w+)\\s+(\\w+)");
            Matcher matcher = pattern.matcher(selectClause);
            while (matcher.find()) {
                aliases.add(matcher.group(1));
            }
        }
        return aliases;
    }

    public static List<String> extractFieldAliasesByJSQL(String sql) throws JSQLParserException {
        List<String> aliases = new ArrayList<>();

        // 将所有自定义占位符(...z_table[]_ids)替换为(1)
        sql = sql.replaceAll("\\(\\.\\.\\.z_table\\d+_ids\\)", "(1)");

        Statement statement = CCJSqlParserUtil.parse(sql);
        if (!(statement instanceof Select)) {
            return aliases;
        }

        Select select = (Select) statement;
        SelectBody selectBody = select.getSelectBody();

        if (selectBody instanceof PlainSelect) {
            PlainSelect plainSelect = (PlainSelect) selectBody;
            List<SelectItem> selectItems = plainSelect.getSelectItems();

            for (SelectItem item : selectItems) {
                if (item instanceof SelectExpressionItem) {
                    SelectExpressionItem exprItem = (SelectExpressionItem) item;
                    if (exprItem.getAlias() != null) {
                        aliases.add(exprItem.getAlias().getName());
                    }
                }
            }
        }
        return aliases;
    }


    /**
     * 提取表名（返回 List<String>）
     */
    public static List<String> extractTableNames(String sql) throws JSQLParserException {
        List<String> tableNames = new ArrayList<>();
        Set<String> cteTables = new HashSet<>(); // 存储 WITH 子句的表名（不提取）

        // 将所有自定义占位符(...z_table[]_ids)替换为(1)
        sql = sql.replaceAll("\\(\\.\\.\\.z_table\\d+_ids\\)", "(1)");

        Statement statement = CCJSqlParserUtil.parse(sql);
        if (!(statement instanceof Select)) {
            return tableNames;
        }

        Select select = (Select) statement;
        SelectBody selectBody = select.getSelectBody();

        // 1. 先提取 WITH 子句的表名（不加入最终结果）
        if (select.getWithItemsList() != null) {
            for (WithItem withItem : select.getWithItemsList()) {
                cteTables.add(withItem.getName().toLowerCase()); // 记录 CTE 表名
            }
        }

        // 2. 提取 FROM/JOIN 里的表名（排除 CTE 表）
        selectBody.accept(new SelectVisitorAdapter() {
            @Override
            public void visit(PlainSelect plainSelect) {
                // 处理 FROM 子句
                if (plainSelect.getFromItem() != null) {
                    extractTable(plainSelect.getFromItem());
                }
                // 处理 JOIN 子句
                if (plainSelect.getJoins() != null) {
                    plainSelect.getJoins().forEach(join -> extractTable(join.getRightItem()));
                }
            }

            private void extractTable(FromItem fromItem) {
                if (fromItem instanceof Table) {
                    Table table = (Table) fromItem;
                    String tableName = table.getName();
                    // 如果表名不在 CTE 中，则加入结果
                    if (!cteTables.contains(tableName.toLowerCase())) {
                        tableNames.add(tableName);
                    }
                } else if (fromItem instanceof SubJoin) {
                    extractTable(((SubJoin) fromItem).getLeft());
                }
                // 子查询不处理（因为只关心物理表）
            }
        });

        return tableNames;
    }

    //获取表名中的数字 z_table17
    public static Integer extractNumberFromTableName(String tableName) {
        Pattern pattern = Pattern.compile("\\d+$");
        Matcher matcher = pattern.matcher(tableName);
        if (matcher.find()) {
            return Integer.parseInt(matcher.group());
        }
        return null;
    }

    /**
     * 判断一条 SQL 是否为“纯 SELECT”语句
     *
     * @param sql 原始 SQL（可能带换行、缩进、注释）
     * @return    true  → 可以视为只读 SELECT
     *            false → 不是纯 SELECT（可能含 DML/DCL/DDL）
     */
    public static boolean isPureSelect(String sql) {
        if (sql == null) return false;

        // 1) 去掉前后空白和常见行内注释
        String norm = sql
                .replaceAll("(?i)--.*?$", "")   // 去掉 -- 注释
                .replaceAll("(?i)/\\*.*?\\*/", "") // 去掉 /* */ 注释
                .trim()
                .toLowerCase(Locale.ROOT);

        // 2) 必须以 select 开头
        if (!norm.startsWith("select")) {
            return false;
        }

        // 3) 不允许出现任何“会修改数据”的关键字
        //    注意用 (?i) 忽略大小写，并加上 \\b 保证是独立单词
        Pattern forbidden = Pattern.compile(
                "\\b(insert|update|delete|merge|alter|drop|truncate|create|grant|revoke|call|exec)\\b",
                Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

        return !forbidden.matcher(norm).find();
    }

    public Instant parseBeijingTime(String timeStr) {
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


    /**
     * 人员解析 SQL 字符串，替换其中的占位符为实际的参数值
     *
     * @param sqlStr 原始的 SQL 查询字符串（包含 MyBatis 占位符，如 #{user_id}）
     * @param params 参数集合（包含占位符对应的值）
     * @return 解析后的 SQL 查询字符串
     */
    public static String parseUserDefinedSql(String sqlStr, Map<String, Object> params) {
        String parsedSql = sqlStr;

        // 遍历参数集合，替换 SQL 中的占位符
        for (Map.Entry<String, Object> entry : params.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();

            // 如果值为 null 或空列表，则跳过替换
            if (value == null || (value instanceof List && ((List) value).isEmpty())) {
                continue;
            }

            // 如果值是列表类型（处理 IN 子句）
            if (value instanceof List) {
                List<?> list = (List<?>) value;
                String inValues = list.stream()
                        .map(String::valueOf)  // 将每个值转为字符串
                        .collect(Collectors.joining(","));
                // 替换 SQL 中的 IN 子句
                parsedSql = parsedSql.replace("#{"+key+"}", inValues);
            } else {
                // 替换单个值（直接替换 #{key} 为实际的值）
                parsedSql = parsedSql.replace("#{"+key+"}", value.toString());
            }
        }

        return parsedSql;
    }

    public static Integer getUserId(HttpSession session) {
        if (session == null) return 879;
        Object obj = session.getAttribute("userId");
        if (obj == null) return 879;
        if (obj instanceof Number) return ((Number) obj).intValue();
        try {
            return Integer.parseInt(obj.toString());
        } catch (Exception e) {
            return 879;
        }
    }
    /**
     * 将Map转换成类
     * */
    public static <T> T ObjToClass(Object json, Class<T> toClass) {
        if(json instanceof String){
            return StringToClass(json, toClass);
        }
        else {
            ObjectMapper mapper = new ObjectMapper();
            try {
                String json1 = mapper.writeValueAsString(json);
                return mapper.readValue(json1, toClass);
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public static <T> T StringToClass(Object json, Class<T> toClass) {
        if(json == null){
            return null;
        }
        ObjectMapper mapper = new ObjectMapper();
        try {
            return mapper.readValue(json.toString(), toClass);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }


    public static String ObjectToJSON(Object obj) {
        ObjectMapper mapper = new ObjectMapper();
        try {
            mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
            return mapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public static String ExtraNumberChar(String str) {
        String regex = "\\d+";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(str);
        while (matcher.find()) {
            return matcher.group();
        }
        return "";
    }
}
