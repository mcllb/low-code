package tcdx.uap.service.store;

import com.github.pagehelper.PageInfo;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;
import tcdx.uap.common.entity.page.TableDataInfo;
import tcdx.uap.common.utils.Lutils;
import tcdx.uap.common.utils.SqlUtil;
import tcdx.uap.common.utils.xss.SQLParser;
import tcdx.uap.constant.Constants;
import tcdx.uap.mapper.BusinessMapper;
import tcdx.uap.service.BaseDBService;
import tcdx.uap.service.entities.*;

import javax.servlet.http.HttpSession;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import static tcdx.uap.common.utils.PageUtils.startPage;

public class DSStore {

    private HashMap<Object,Map> store;
    String [] tableTypedCols= new String[] {"table_field", "flow_field", "ds_rows_length", "ds_total", "pri_sub_receiver_info"};
    String [] definedTypedCols= new String[] {"defined_field", "ds_rows_length", "ds_total", "pri_sub_receiver_info"};

    @Getter
    private static DSStore instance = new DSStore();

    private DSStore()
    {
        System.out.println("单例模式实现一-饿汉式");
    }

    BaseDBService db;
    BusinessMapper businessMapper;
    public void InitAll(BaseDBService db, BusinessMapper businessMapper){
        this.db = db;
        this.businessMapper = businessMapper;
        InitAll();
    }

    public synchronized void InitAll(){
        //集合缓存，初始化数据源字段
        store = new HashMap();
        //所有数据源
        List<Map> v_datasource = db.selectByCauses("v_datasource", null, null);
        if(v_datasource.size()>0) {
            //所有数据源
           //获取字段
            List<Map> v_datasource_field = db.selectByCauses("v_datasource_field",
                    SqlUtil.and(SqlUtil.eq("is_deleted", false)
                    ), Lutils.genMap("ord", "asc"));
            //编辑器
            List<Map> v_comp_value_editor = db.selectByCauses("v_comp_value_editor",
                    SqlUtil.in("ds_field_id", v_datasource_field.stream().map(o -> o.get("id")).collect(Collectors.toList())),
                    null);
            //组装数据源
            assemble(v_datasource, v_datasource_field, v_comp_value_editor);
        }
    }

    public void copy( Map oldNode, Map newNode ) {

    }

    public synchronized void set(Object key){
        List<Map> v_datasource = new ArrayList<>();
        if(key instanceof String){
            String keyStr = key.toString();
            Integer obj_id = null;
            String obj_type = null;
            if(keyStr.startsWith("comp_log")){
                obj_type = "comp_log";
                obj_id = Integer.parseInt(keyStr.replace("comp_log",""));
            }
            else if(keyStr.startsWith("comp_huiqian")){
                obj_type = "comp_huiqian";
                obj_id = Integer.parseInt(keyStr.replace("comp_huiqian",""));
            }
            else if(keyStr.startsWith("task_plan")){
                obj_type = "task_plan";
                obj_id = Integer.parseInt(keyStr.replace("task_plan",""));
            }
            else if(keyStr.startsWith("comp_carousel")){
                obj_type = "comp_carousel";
                obj_id = Integer.parseInt(keyStr.replace("comp_carousel",""));
            }
            else if(keyStr.startsWith("view")){
                obj_type = "view";
                obj_id = Integer.parseInt(keyStr.replace("view",""));
            }
            else if(keyStr.startsWith("comp_grid")){
                obj_type = "comp_grid";
                obj_id = Integer.parseInt(keyStr.replace("comp_grid",""));
            }
            else if(keyStr.startsWith("comp_echarts")){
                obj_type = "comp_echarts";
                obj_id = Integer.parseInt(keyStr.replace("comp_echarts",""));
            }
            else if(keyStr.startsWith("comp_echarts_gantt")){
                obj_type = "comp_echarts_gantt";
                obj_id = Integer.parseInt(keyStr.replace("comp_echarts_gantt",""));
            }
            v_datasource = db.selectEq("v_datasource", Lutils.genMap("obj_type",obj_type, "obj_id", obj_id));
        }
        else if(key instanceof Integer){//所有数据源
             v_datasource = db.selectEq("v_datasource", Lutils.genMap("id", key));
        }
        if(v_datasource.size()>0) {
            //获取字段
            List<Map> v_datasource_field = db.selectByCauses("v_datasource_field",
                    SqlUtil.and(SqlUtil.in("ds_id", v_datasource.stream().map(o -> o.get("id")).collect(Collectors.toList())),
                            SqlUtil.eq("is_deleted", false)
                    ),
                    Lutils.genMap("ord", "asc"));
            //编辑器
            List<Map> v_comp_value_editor = db.selectByCauses("v_comp_value_editor",
                    SqlUtil.in("ds_field_id", v_datasource_field.stream().map(o -> o.get("id")).collect(Collectors.toList())),
                    null);
            //组装数据源
            assemble(v_datasource, v_datasource_field, v_comp_value_editor);
        }
    }

    private void assemble( List<Map>v_datasource, List<Map> v_datasource_field, List<Map> v_comp_value_editor){
        //数据源缓存
        for(Map map : v_datasource){
            map.put("fields", new ArrayList<Map>());
            store.put(map.get("id"), map);
            //view123、comp_grid123、comp_carousel123
            store.put((String)map.get("obj_type")+map.get("obj_id"), map);
        }
        //集合缓存
        Map <Object,Map> editorsMap = new HashMap();
        for(Map map:v_comp_value_editor){
            editorsMap.put(map.get("id"), map);
        }
        /** 将数据列加入到数据源 */
        for(Map f : v_datasource_field){
            Integer ds_id = (Integer)f.get("ds_id");
            String field_type = (String)f.get("field_type");
            Map ds = store.get(ds_id);
            if(ds==null)
                continue;
            //将字段添加到对应的数据源fields中
            List<Map> fields = (List<Map>)ds.get("fields");
            if(Objects.equals(ds.get("data_type"),"defined") && Arrays.stream(definedTypedCols).anyMatch(m->m.equals(field_type))) {
                fields.add(f);
            }
            else if(Objects.equals(ds.get("data_type"),"table") && Arrays.stream(tableTypedCols).anyMatch(m->m.equals(field_type))) {
                fields.add(f);
            }
        }
    }

    public Map create(Object obj_type, Object obj_id){
        Map ds = db.insertWhenNotExist("v_datasource", Lutils.genMap("obj_id", obj_id, "obj_type", obj_type), Lutils.genMap("obj_id", obj_id, "obj_type", obj_type));
        db.insertWhenNotExist("v_datasource_field",
                Lutils.genMap("ds_id", ds.get("id"), "is_deleted", false, "field_type", "ds_total"),
                Lutils.genMap("ds_id", ds.get("id"), "field_type", "ds_total"));
        db.insertWhenNotExist("v_datasource_field",
                Lutils.genMap("ds_id", ds.get("id"), "is_deleted", false, "field_type", "ds_rows_length"),
                Lutils.genMap("ds_id", ds.get("id"), "field_type", "ds_rows_length"));
        DSStore.getInstance().set(ds.get("id"));
        return DSStore.getInstance().get(ds.get("id"));
    }

    /**
     * 获取组件，同时装配动态数据
     * @param key key可以是Id，也可以是ObjType+Id
     * */
    public synchronized Map getSimplify(Object key){
        Map compDataSource = store.get(key);
        if(compDataSource==null)
            return null;
        List<Map> fields = (List<Map>)compDataSource.get("fields");
        if(fields==null||fields.size()==0)
            return compDataSource;
        /** 将数据列纳入数据源 */
        for(Map f : fields){
            String field_type = (String)f.get("field_type");
            if(compDataSource==null)
                continue;
            String field = null;
            if(Objects.equals(compDataSource.get("data_type"),"table") && Arrays.stream(tableTypedCols).anyMatch(m->m.equals(field_type))){
                if(field_type.equals("table_field")){
                    Integer table_id = (Integer)f.get("table_id");
                    Integer table_col_id = (Integer)f.get("table_col_id");
                    f.put("table_id_", "table"+table_id);
                    f.put("table_col_id_", "table_col"+table_col_id);
                    Map tableCol = TableStore.getInstance().getTableCol(table_col_id);
                    if(tableCol==null)
                        continue;
                    field = (String)tableCol.get("field");
                    f.put("data_type", tableCol.get("data_type"));
                    f.put("rel_dict_id", tableCol.get("rel_dict_id"));
//                    f.put("tableColObj", tableCol);
//                    f.put("tableObj", TableStore.getInstance().get(table_id));
                }
                else if(field_type.equals("flow_field")){
                    Integer flow_edge_id = (Integer)f.get("flow_edge_id");
                    field = Objects.equals(flow_edge_id,-1)?(String)f.get("flow_field"):("f"+flow_edge_id+f.get("flow_field"));
                }
            }
            if(Objects.equals(compDataSource.get("data_type"),"defined") && Arrays.stream(definedTypedCols).anyMatch(m->m.equals(field_type))){
                //如果不符合defined数据源的字段，丢弃
                if(f.get("defined_field")==null){
                    continue;
                }
                field = (String) f.get("defined_field");
            }
            if(field_type.equals("ds_rows_length")){
                field = "ds_rows_length";
            }
            else if(field_type.equals("ds_total")){
                field = "ds_total";
            }
            else if(field_type.equals("pri_sub_receiver_info")){
                field = "pri_sub_receiver_info";
            }
            f.put("field", field);
//            f.put("editors", CompValueEditorStore.getInstance().getByField(f.get("id")));
            //将字段添加到对应的数据源fields中
        }
        return compDataSource;
    }

    /**
     * 获取组件，同时装配动态数据
     * @param key key可以是Id，也可以是ObjType+Id
     * */
    public synchronized Map get(Object key){
        if(!store.containsKey(key))
            return null;
        Map compDataSource = store.get(key);
        if(compDataSource==null) {
            System.out.println("#############CompDataSource key: "+key+", is null");
            return null;
        }
        List<Map> fields = (List<Map>)compDataSource.get("fields");
        if(fields==null||fields.size()==0)
            return compDataSource;
        /** 将数据列纳入数据源 */
        for(Map f : fields){
            String field_type = (String)f.get("field_type");
            if(compDataSource==null)
                continue;
            String field = null;
            if(Objects.equals(compDataSource.get("data_type"),"table") && Arrays.stream(tableTypedCols).anyMatch(m->m.equals(field_type))){
                if(field_type.equals("table_field")){
                    Integer table_id = (Integer)f.get("table_id");
                    Integer table_col_id = (Integer)f.get("table_col_id");
                    f.put("table_id_", "table"+table_id);
                    f.put("table_col_id_", "table_col"+table_col_id);
//                    Map tableCol = TableStore.getInstance().getTableCol(table_col_id);
//                    if(tableCol==null)
//                        continue;
//                    field = (String)tableCol.get("field");
//                    f.put("data_type", tableCol.get("data_type"));
//                    f.put("rel_dict_id", tableCol.get("rel_dict_id"));
//                    f.put("tableColObj", tableCol);
//                    f.put("tableObj", TableStore.getInstance().get(table_id));
                }
                else if(field_type.equals("flow_field")){
                    Integer flow_edge_id = (Integer)f.get("flow_edge_id");
                    field = Objects.equals(flow_edge_id,-1)?(String)f.get("flow_field"):("f"+flow_edge_id+f.get("flow_field"));
                }
            }
            if(Objects.equals(compDataSource.get("data_type"),"defined") && Arrays.stream(definedTypedCols).anyMatch(m->m.equals(field_type))){
                //如果不符合defined数据源的字段，丢弃
                if(f.get("defined_field")==null){
                    continue;
                }
                field =  (String) f.get("defined_field");
                Integer table_id = (Integer)f.get("table_id");
                Integer table_col_id = (Integer)f.get("table_col_id");
                Map tableCol = TableStore.getInstance().getTableCol(table_col_id);
                if(tableCol==null)
                    continue;
                f.put("data_type", tableCol.get("data_type"));
                f.put("rel_dict_id", tableCol.get("rel_dict_id"));
                f.put("tableColObj", tableCol);
                f.put("tableObj", TableStore.getInstance().get(table_id));
            }
            if(field_type.equals("ds_rows_length")){
                field = "ds_rows_length";
            }
            else if(field_type.equals("ds_total")){
                field = "ds_total";
            }
            else if(field_type.equals("pri_sub_receiver_info")){
                field = "pri_sub_receiver_info";
            }
            f.put("field", field);
            f.put("editors", CompValueEditorStore.getInstance().getByField(f.get("id")));
            //将字段添加到对应的数据源fields中
        }
        return compDataSource;
    }

    public Map getFields(Integer ds_id, Integer ds_field_id){
        // if(ds_id==null) return null;
        Map compDataSource = store.get(ds_id);
        List<Map> fields = (List<Map>) compDataSource.get("fields");
        List<Map> finds = fields.stream().filter(f->Objects.equals(f.get("id"), ds_field_id)).collect(Collectors.toList());
        if(!finds.isEmpty())
            return finds.get(0);
        return null;
    }

//
//    public List<Map> get_ds_fields(BusinessMapper businessMapper, Map datasource){
//        if (datasource.get("data_type")!=null&&datasource.get("data_type").equals("defined")) {
//            return businessMapper.get_defined_datasuorce_fields(Lutils.genMap( "ds_id", datasource.get("id")));
//        } else {
//            return businessMapper.get_table_datasource_fields(Lutils.genMap("ds_id", datasource.get("id")));
//        }
//    }

    public Map get_comp_ds( String obj_type,Integer obj_id){
        Map op_view_ds = null;
        if(obj_type!=null&&obj_id!=null){
            //获取操作视图绑定的数据源
            if(obj_type.equals("CompDataSource")){
                op_view_ds = DSStore.getInstance().get("view"+obj_id);
            }
            else if(obj_type.equals("CompGrid")){
                //找到comp_grid
                Map comp_grid = db.selectEq("v_comp_grid", Lutils.genMap("obj_id", obj_id, "obj_type", "view")).get(0);
                op_view_ds = DSStore.getInstance().get("comp_grid"+comp_grid.get("id"));
            }
            else if(obj_type.equals("CompReportForms")){
                Map comp_grid = db.selectEq("v_comp_grid", Lutils.genMap("obj_id", obj_id, "obj_type", "view")).get(0);
                op_view_ds = DSStore.getInstance().get("comp_grid"+comp_grid.get("id"));
            }
        }
        return op_view_ds;
    }



    public boolean isRowField(Object field_type){
        return Objects.equals(field_type, "flow_field") ||Objects.equals(field_type,"table_field")
                ||Objects.equals(field_type,"defined_field");
    }
}
