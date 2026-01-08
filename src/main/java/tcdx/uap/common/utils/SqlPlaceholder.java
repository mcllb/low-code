package tcdx.uap.common.utils;
import java.util.*;
import java.util.regex.*;

/**
 * 通过 “…字段名” 占位符，把 SQL 动态替换成列表中的实际值
 */
public final class SqlPlaceholder {

    private static final Pattern PLACEHOLDER = Pattern.compile("\\.\\.\\.([a-zA-Z]\\w*)");

    private SqlPlaceholder() {}   // 工具类不实例化

    /**
     * @param rawSql   原始 SQL，形如 “…>...shuliang”
     * @param rows     List<Map<String,Object>>，每行就是一条记录
     * @param resolver 如何把字段名映射成值（可选，null=默认：取第一行同名字段）
     * @return         已替换好的 SQL
     */
    public static String replace(String rawSql,
                                 List<Map> rows,
                                 FieldResolver resolver) {

        if (rawSql == null || rows == null) return rawSql;

        if (resolver == null) {
            // 默认：取第一行 row.get(field)
            resolver = (field, list) -> list.isEmpty() ? null : list.get(0).get(field);
        }

        Matcher m = PLACEHOLDER.matcher(rawSql);
        StringBuffer sb = new StringBuffer();

        while (m.find()) {
            String field = m.group(1);                 // 占位符里的字段名
            Object val   = resolver.resolve(field, rows);

            if (val == null) {
                throw new IllegalArgumentException(
                        "无法在列表中找到字段 '" + field + "' 的值用来替换占位符");
            }
            // 直接替换为 toString()；如果需要字符串加引号可自行判断类型
            m.appendReplacement(sb, Matcher.quoteReplacement(val.toString()));
        }
        m.appendTail(sb);
        return sb.toString();
    }

    /* -------- 可插拔的“字段 -> 值”策略 -------- */
    @FunctionalInterface
    public interface FieldResolver {
        Object resolve(String field, List<Map> rows);
    }
}
