package tcdx.uap.controller;

import com.github.pagehelper.PageInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import tcdx.uap.common.entity.page.TableDataInfo;
import tcdx.uap.common.utils.Lutils;
import tcdx.uap.common.utils.PageUtils;
import tcdx.uap.mapper.BaseDBMapper;
import tcdx.uap.service.BaseDBService;
import tcdx.uap.mapper.BusinessMapper;

import java.util.*;

@Controller
@RequestMapping("/uap/form/")
public class FormController extends BaseController {


    @Autowired
    BaseDBMapper baseDBMapper;
    @Autowired
    private BaseDBService baseDBService;
    @Autowired
    private BusinessMapper businessMapper;


    @PostMapping("/sqlDefinedSearch")
    @ResponseBody
    public Map sqlDefinedSearch(@RequestBody Map<String, Object> map) {
        List<Map> queryParameter = (List) map.get("queryParameter");
        String sql = map.get("sqlRowsSelected").toString();
        for (int i = 0; i < queryParameter.size(); i++) {
            Map item = queryParameter.get(i);
            String label = item.get("label").toString();
            String value = item.get("value").toString();
            sql = sql.replace(label, value);
        }
        if (map.get("pageNum") != null && map.get("pageSize") != null)
            PageUtils.startPage(map);
        List<Map> list = baseDBMapper.selectSql(Lutils.genMap("sql", sql));
        List<Map> collist = new ArrayList<>();
        if (list.size() > 0) {
            Map map1 = list.get(0);
            Set<String> keys = map1.keySet();
            for (String key : keys) {
                Map item = new HashMap();
                item.put("field", key);
                item.put("name", key);
                item.put("field_type", "文本");
                collist.add(item);
            }
        }
        TableDataInfo rspData = new TableDataInfo();
        rspData.setCode(0);
        rspData.setRows(list);
        rspData.setTotal(new PageInfo(list).getTotal());
        Map<String, Object> map1 = Lutils.genMap("tableColumn", collist, "tableData", rspData);
        return map1;
    }

    @PostMapping("/get_table_col_data")
    @ResponseBody
    public Map get_table_col_data(@RequestBody Map<String, Object> map) {
        String table_name = map.get("table_name").toString();
        int table_id = (Integer) baseDBService.selectEq("v_table",
                Lutils.genMap("table_name", table_name), null).get(0).get("id");
        List<Map> table_col_list = baseDBService.selectEq("v_table_col", Lutils.genMap("table_id", table_id), null);
        List field = Lutils.getColumnValueList(table_col_list, "field");
        List data_list = baseDBService.selectEq(table_name, field, Lutils.genMap(1, 1));
        startPage(map);
        TableDataInfo dataTable = getDataTable(data_list);
        return Lutils.genMap("field", field, "table_data", dataTable);
    }

    @PostMapping("/get_search_table_col_data")
    @ResponseBody
    public TableDataInfo get_search_table_col_data(@RequestBody Map<String, Object> map) {
        String table_name = map.get("table_name").toString();
        String label = map.get("label").toString();
        String value = map.get("value").toString();
        List<Map> result_data = new ArrayList<>();
        startPage(map);

        if (label == null || label == "") {
            result_data = baseDBService.selectEq(table_name, Lutils.genMap(1, 1));
        } else {
            result_data = baseDBService.selectByCauses(table_name,
                    Lutils.genMap("tp", "a", "cas", Lutils.genList(
                            Lutils.genMap("tp", "lk", "col", label, "val", value)
                    )), null);
        }
        if (map.get("orderByColumn") == null) {
            map.put("orderByColumn", "id_");
            map.put("isAsc", "desc");
        }
        return getDataTable(result_data);
    }

    @RequestMapping("/create_field_by_form_field")
    @ResponseBody
    public Map create_field_by_form_field(@RequestBody Map<String, Object> map) {
        map = (Map<String, Object>) map.get("map");
//        String table_column = map.get("table_column").toString();
        int table_col_id = (Integer) map.get("table_col_id");
        Boolean is_show = (Boolean) map.get("is_show");
        int viewId = Integer.parseInt(map.get("view_id").toString());
        List<Map> maps = baseDBService.selectEq("v_form_field", Lutils.genMap("table_col_id", table_col_id, "view_id", viewId));
        if (maps.size() == 0) {
//            baseDBService.insertMap("v_form_field", map);
            List<Map> re = null;
            re= baseDBMapper.insertMapRetRow(Lutils.genMap("tn", "v_form_field", "insertMap", map));
            return  re.get(0);
        } else {
            //如果已存在更新is_show为t
            baseDBService.updateEq("v_form_field", Lutils.genMap("is_show", is_show),
                    Lutils.genMap("table_col_id", table_col_id, "view_id", viewId));
        }
        return Lutils.genMap("v_form_field", map);
    }

    @PostMapping("/get_form_config_item")
    @ResponseBody
    public List get_form_config_item(@RequestBody Map<String, Object> map) {
        //获取表单form
        int view_id = (Integer) map.get("view_id");
        int table_id = (Integer) map.get("table_id");
        List<Map> formList = businessMapper.get_form_fields1(Lutils.genMap("view_id", view_id, "table_id", table_id));
        //用table_col表的column_name替换form_field的field
        for (Map c : formList) {
            c.put("field", c.get("table_column") != null ? c.get("table_column").toString() : c.get("field"));
        }
        return formList;
    }

}
