package tcdx.uap.common.entity.page;


import tcdx.uap.common.utils.ServletUtils;
import tcdx.uap.common.utils.text.Convert;

import java.util.Map;

/**
 * 表格数据处理
 * 
 * @author ruoyi
 */
public class TableSupport
{
    /**
     * 当前记录起始索引
     */
    public static final String PAGE_NUM = "pageNum";

    /**
     * 每页显示记录数
     */
    public static final String PAGE_SIZE = "pageSize";

    /**
     * 排序列
     */
    public static final String ORDER_BY_COLUMN = "orderByColumn";

    /**
     * 排序的方向 "desc" 或者 "asc".
     */
    public static final String IS_ASC = "isAsc";

    /**
     * 分页参数合理化
     */
    public static final String REASONABLE = "reasonable";

    /**
     * 封装分页对象
     */
    public static PageDomain getPageDomain()
    {
        PageDomain pageDomain = new PageDomain();
        pageDomain.setPageNum(Convert.toInt(ServletUtils.getParameter(PAGE_NUM), 1));
        pageDomain.setPageSize(Convert.toInt(ServletUtils.getParameter(PAGE_SIZE), 10));
        pageDomain.setOrderByColumn(ServletUtils.getParameter(ORDER_BY_COLUMN));
        pageDomain.setIsAsc(ServletUtils.getParameter(IS_ASC));
        pageDomain.setReasonable(ServletUtils.getParameterToBool(REASONABLE));
        return pageDomain;
    }

    public static PageDomain getPageDomain(Map m)
    {
        PageDomain pageDomain = new PageDomain();
        pageDomain.setPageNum(Convert.toInt(m.get(PAGE_NUM), 1));
        pageDomain.setPageSize(Convert.toInt(m.get(PAGE_SIZE), 10));
        if(m.get(ORDER_BY_COLUMN)!=null)
            pageDomain.setOrderByColumn(m.get(ORDER_BY_COLUMN).toString());
        if(m.get(IS_ASC)!=null)
            pageDomain.setIsAsc(m.get(IS_ASC).toString());
        if(m.get(REASONABLE)!=null)
            pageDomain.setReasonable(Convert.toBool(m.get(REASONABLE).toString()));
        return pageDomain;
    }

    public static PageDomain buildPageRequest()
    {
        return getPageDomain();
    }

}
