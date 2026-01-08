package tcdx.uap.controller;

import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import tcdx.uap.common.entity.AjaxResult;
import tcdx.uap.common.entity.page.TableDataInfo;
import tcdx.uap.common.utils.Lutils;
import tcdx.uap.common.utils.MapUtils;
import tcdx.uap.common.utils.SqlUtil;
import tcdx.uap.mapper.BaseDBMapper;
import tcdx.uap.service.BaseDBService;

import java.util.List;
import java.util.Map;

/**
 * 通用请求处理
 * 
 * @author tctelecom
 */
@Controller
@RequestMapping("/uap/base_db/")
public class BaseDBController extends BaseController
{
    private static final Logger log = LoggerFactory.getLogger(BaseDBController.class);
    private String prefix = "";

    @Autowired
    BaseDBMapper baseDBMapper;

    @Autowired
    BaseDBService baseDBService;
    /**
     * 查询运行时的任务列表
     * @param map
     * @return 数据表列表
     *
     */
//    @RequiresPermissions("uap:base_db:select_by_id")
    @PostMapping("/select_by_id")
    @ResponseBody
    public TableDataInfo select_by_id(@RequestParam Map<String, Object> map)
    {
        System.out.println(map);
        String tb = map.get("tn").toString();
        SqlUtil.filterKeyword(tb);
        startPage(map);
        List<Map> list = baseDBMapper.selectById(map);
        return getDataTable(list);
    }

    //    @RequiresPermissions("uap:base_db:select_by_id")
    @PostMapping("/select_eq")
    @ResponseBody
    public TableDataInfo select_eq(@RequestBody Map<String, Object> map)
    {
        System.out.println(map);
        String tb = map.get("tn").toString();
        SqlUtil.filterKeyword(tb);
        startPage(map);
        List<Map> list = baseDBMapper.selectEq(map);
        return getDataTable(list);
    }

    /**
     * 查询运行时的任务列表
     * @param map
     * @return 数据表列表
     *
     */
//    @RequiresPermissions("uap:base_db:select_by_causes")
    @PostMapping("/select_by_causes")
    @ResponseBody
    public TableDataInfo select_by_causes(@RequestBody Map<String, Object> map)
    {
        System.out.println(map);
        String tb = map.get("tn").toString();
        SqlUtil.filterKeyword(tb);
        startPage(map);
        List<Map> list = baseDBMapper.selectByCauses(map);
        return getDataTable(list);
    }

    /**
     * 新增数据
     * @param map
     * @return 数据表列表
     *
     */
//    @RequiresPermissions("uap:base_db:select_by_causes")
    @PostMapping("/insert_map")
    @ResponseBody
    public int insert_map(@RequestBody Map<String, Object> map)
    {
        System.out.println(map);
        //map中key包含time或者date，并且value是Long形的字段，转换成timestamp
        String tb = map.get("tn").toString();
        SqlUtil.filterKeyword(tb);
        //如果批量插入，则取insertMaps的数据
        int insertCount = 0;
        //如果有insertMap，插入insertMap数据
        if(map.get("insertMap")!=null) {
            insertCount += baseDBMapper.insertMap(map);
        }
        //如果有列表，怎插入列表
        List<Map> insertMapList = (List<Map>) map.get("insertMapList");
        if(insertMapList!=null && insertMapList.size()>0){
            for(Map map1: insertMapList){
                insertCount += baseDBMapper.insertMap(MapUtils.Combine(
                        MapUtils.New("tn",map.get("tn")),
                        MapUtils.New("insertMap",map1)
                        )
                );
            }
        }
        return insertCount;
    }

    @PostMapping("/insert_map_autofill_ord")
    @ResponseBody
    public AjaxResult insert_map_autofill_ord(@RequestBody Map<String, Object> map)
    {
        System.out.println(map);
        String tb = map.get("tn").toString();
        Map insertMap = (Map)map.get("insertMap");
        String ordCol = map.get("ordCol").toString();
        Map ordEqualMap = (Map)map.get("ordEqualMap");
        SqlUtil.filterKeyword(tb);
        //如果批量插入，则取insertMaps的数据
        //如果有insertMap，插入insertMap数据
        if(map.get("insertMap")!=null) {
             Map re = baseDBService.insertMapAutoFillMaxOrd(tb, insertMap, ordCol, ordEqualMap);
            return  AjaxResult.success("success", re);
        }
        else{
            return AjaxResult.success("failed");
        }
    }

    /**
     * 插入后，返回最新id的数据
     * */
    @PostMapping("/insert_map_return_new")
    @ResponseBody
    @Transactional
    public Map insert_map_return_new(@RequestBody Map<String, Object> map)
    {
        String tn = map.get("tn").toString();
        SqlUtil.filterKeyword(tn);
        //如果批量插入，则取insertMaps的数据
        int insertCount = 0;
        //如果有insertMap，插入insertMap数据
        if(map.get("insertMap")!=null) {
            return baseDBService.insertMapRetRow((String)map.get("tn"), (Map)map.get("insertMap"));
        }
        else{
            return null;
        }
    }


    @PostMapping("/insert_not_exist_and_return")
    @ResponseBody
    @Transactional
    public Map insert_not_exist_and_return(@RequestBody Map<String, Object> map)
    {
        String tn = map.get("tn").toString();
        SqlUtil.filterKeyword(tn);
        return baseDBService.insertWhenNotExist(tn, (Map)map.get("insertMap"), (Map)map.get("equalMap"));
    }

    @PostMapping("/insert_not_exist_update_exist_and_return")
    @ResponseBody
    @Transactional
    public Map insert_not_exist_update_exist_and_return(@RequestBody Map<String, Object> map)
    {
        String tn = map.get("tn").toString();
        SqlUtil.filterKeyword(tn);
        Map re = baseDBService.insertWhenNotExistUpdateWhenExists(tn, (Map)map.get("insertMap"), (Map)map.get("equalMap"));
        return re;
    }


    @PostMapping("/insert_not_exist_update_exist_and_return_auto_fill_ord")
    @ResponseBody
    @Transactional
    public Map insert_not_exist_update_exist_and_return_auto_fill_ord(@RequestBody Map<String, Object> map)
    {
        String tn = map.get("tn").toString();
        SqlUtil.filterKeyword(tn);
        Map re = baseDBService.insertWhenNotExistUpdateWhenExists(tn, (Map)map.get("insertMap"), (Map)map.get("equalMap"));
        return re;
    }

    /**
     * 新增数据
     * @param map
     * @return 数据表列表
     *
     */
//    @RequiresPermissions("uap:base_db:select_by_causes")
    @PostMapping("/update_eq")
    @ResponseBody
    public int update_eq(@RequestBody Map<String, Object> map)
    {
        System.out.println(map);
        String tb = map.get("tn").toString();
        SqlUtil.filterKeyword(tb);
        int rs= baseDBMapper.updateEq(map);
        return rs;
    }

    /**
     * 删除数据
     * @param map ajax
     * @return 数据表列表
     *
     */
//    @RequiresPermissions("uap:base_db:select_by_causes")
    @PostMapping("/delete_eq")
    @ResponseBody
    public int delete_eq(@RequestBody Map<String, Object> map)
    {
        System.out.println(map);
        String tb = map.get("tn").toString();
        SqlUtil.filterKeyword(tb);
        int rs= baseDBMapper.deleteEq(map);
        return rs;
    }


    /**
     * 删除数据
     * @param map ajax
     * @return 数据表列表
     *
     */
    //    @RequiresPermissions("uap:base_db:select_by_causes")
    @PostMapping("/delete_by_ause")
    @ResponseBody
    public int delete_by_ause(@RequestBody Map<String, Object> map)
    {
        System.out.println(map);
        String tb = map.get("tn").toString();
        SqlUtil.filterKeyword(tb);
        int rs= baseDBMapper.deleteByCause(map);
        return rs;
    }

    @PostMapping("/get_form")
    @ResponseBody
    public List<Map> get_form(@RequestBody int fId)
    {
        List<Map> maps = baseDBMapper.selectEq(MapUtils.Combine(
                MapUtils.New("tn", "v_form_field"),
                MapUtils.New("equalMap", MapUtils.Combine(MapUtils.New("form_id", fId)))
        ));
        return maps;
    }

    @Autowired
    SqlSessionFactory sqlSessionFactory;

    @PostMapping("/select_sql")
    @ResponseBody
    public TableDataInfo select_sql(@RequestBody Map<String, Object> map)
    {
        List<Map> sqls = baseDBService.selectEq("v_defined_sql", Lutils.genMap("id", map.get("defined_sql_id")));
        map.remove("defined_sql_id");
        map.put("sql", sqls.get(0).get("sql").toString());
        startPage(map);
        List<Map> list = baseDBService.selectSql(map);
        return getDataTable(list);
    }
    @PostMapping("/update_priv")
    @ResponseBody
    public int update_priv(@RequestBody Map<String, Object> map)
    {
        System.out.println(map);
        String tb = map.get("tn").toString();
        SqlUtil.filterKeyword(tb);
        int rs= baseDBMapper.updateEq(map);
        return rs;
    }

}
