package tcdx.uap.common.utils;

import com.github.pagehelper.PageHelper;
import tcdx.uap.common.entity.page.PageDomain;
import tcdx.uap.common.entity.page.TableSupport;

import java.util.Map;

/**
 * 分页工具类
 * 
 * @author ruoyi
 */
public class PageUtils extends PageHelper
{
    /**
     * 设置请求分页数据
     */
    public static void startPage()
    {
        PageDomain pageDomain = TableSupport.buildPageRequest();
        Integer pageNum = pageDomain.getPageNum();
        Integer pageSize = pageDomain.getPageSize();
        String orderBy = SqlUtil.escapeOrderBySql(pageDomain.getOrderBy());
        Boolean reasonable = pageDomain.getReasonable();
        System.out.println("pageNum:"+pageNum+",pageSize:"+pageSize+",orderBy:"+orderBy+",reasonable:"+reasonable);
        PageHelper.startPage(pageNum, pageSize, orderBy).setReasonable(reasonable);
    }
    public static void startPage(Map m)
    {
        PageDomain pageDomain =TableSupport.getPageDomain(m);
        Integer pageNum = pageDomain.getPageNum();
        Integer pageSize = pageDomain.getPageSize();
        String orderBy = SqlUtil.escapeOrderBySql(pageDomain.getOrderBy());
        Boolean reasonable = pageDomain.getReasonable();
        System.out.println("pageNum:"+pageNum+",pageSize:"+pageSize+",orderBy:"+orderBy+",reasonable:"+reasonable);
        PageHelper.startPage(pageNum, pageSize, orderBy).setReasonable(reasonable);
    }

    /**
     * 清理分页的线程变量
     */
    public static void clearPage()
    {
        PageHelper.clearPage();
    }
}
