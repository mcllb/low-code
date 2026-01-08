package tcdx.uap.service.store;

import lombok.Getter;
import tcdx.uap.common.utils.Lutils;
import tcdx.uap.mapper.ServiceConfigMapper;
import tcdx.uap.service.BaseDBService;

import java.util.*;

import static tcdx.uap.common.utils.PageUtils.startPage;

public class CompValueRenderStore {


    @Getter
    private static CompValueRenderStore instance = new CompValueRenderStore();

    private CompValueRenderStore()
    {
        System.out.println("单例模式实现一-饿汉式");
    }
    private Map<Object,Map> store = new HashMap();
    BaseDBService db;
    ServiceConfigMapper cfgMapper;
    public void InitAll(BaseDBService db,ServiceConfigMapper cfgMapper){
        this.db = db;
        this.cfgMapper = cfgMapper;
        InitAll();
    }

    public void InitAll(){
        List<Map> v_comp_value_render = db.selectByCauses("v_comp_value_render", null, null);
        assemble(v_comp_value_render);
    }

    public void set(Object key){
        if(key instanceof String){
            String keyStr = key.toString();
            Integer obj_id = null;
            String obj_type = null;
            if(keyStr.startsWith("comp_grid_col")){
                obj_type = "comp_grid_col";
                obj_id = Integer.parseInt(keyStr.replace("comp_grid_col",""));
            }
            else if(keyStr.startsWith("view")){
                obj_type = "view";
                obj_id = Integer.parseInt(keyStr.replace("view",""));
            }
            set( obj_type,  obj_id);
        }
        else if(key instanceof Integer){
            List<Map> v_comp_value_render = db.selectEq("v_comp_value_render", Lutils.genMap("id", key));
            assemble(v_comp_value_render);
        }
    }


    public void set(String obj_type, Integer obj_id){
        List<Map> v_comp_value_render = db.selectEq("v_comp_value_render", Lutils.genMap("obj_id", obj_id,"obj_type", obj_type));
        assemble(v_comp_value_render);
    }

    public void assemble(List<Map> v_comp_value_render){
        for(Map comp : v_comp_value_render){
            store.put(comp.get("id"), comp);
            store.put((String)comp.get("obj_type")+comp.get("obj_id"), comp);
        }
    }

    /**
     * key 可以是id，也可以是obj_type+id
     * */
    public Map get(Object key){
        if(!store.containsKey(key))
            set(key);
        Map comp = store.get(key);
        //获取一些属性
        if(comp != null){
            //如果使用的值是数据源的，获取数据源字段信息
            if(Objects.equals(Lutils.nvl(comp.get("use_defined_value"),false),false)) {
                if(comp.get("ds_id")!=null){
                    Map field = DSStore.getInstance().getFields((Integer) comp.get("ds_id"), (Integer) comp.get("ds_field_id"));
                    Map compDataSource = DSStore.getInstance().get( comp.get("ds_id"));
                    //一些加载了一半的数据源，会出现字段未配置的情况
                    if(field!=null) {
                        comp.put("field", field.get("field"));
                    }
                    comp.put("compDataSource_obj_type", compDataSource.get("obj_type"));
                    comp.put("ds_id_", "ds"+comp.get("ds_id"));
                }
            }
            List<Map> execs = ExecObjStore.getInstance().getByObj("comp_value_render" + comp.get("id"));
            comp.put("exec", execs.isEmpty()?null:execs.get(0));
        }

        return comp;
    }

}
