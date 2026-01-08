package tcdx.uap.service.store;


import lombok.Getter;
import tcdx.uap.common.utils.SqlUtil;
import tcdx.uap.service.BaseDBService;
import tcdx.uap.service.BusinessService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CompEchartsStore {

    @Getter
    private static CompEchartsStore instance = new CompEchartsStore();

    private CompEchartsStore() {
        System.out.println("单例模式初始化ViewStore");
    }

    //indexPage/modal/drawer
    private final Map<Object,Map> store = new HashMap();

    BaseDBService db;
    BusinessService bs;

    public void InitAll(BaseDBService db, BusinessService bs) {
        this.db = db;
        this.bs = bs;
        InitAll();
    }

    public void InitAll() {
        List<Map> v_comp_echarts = db.selectByCauses("v_comp_echarts", null, null);
        assemble(v_comp_echarts);
    }

    public void set(Integer id) {
        List<Map> v_comp_echarts = db.selectByCauses("v_comp_echarts", SqlUtil.eq("id", id), null);
        assemble(v_comp_echarts);
    }

    public void assemble(List<Map> v_comp_echarts) {
        for (Map comp : v_comp_echarts) {
            store.put(comp.get("id"), comp);
            store.put((String) comp.get("obj_type") + comp.get("obj_id"), comp);
        }
    }

    /**
     * 获取组件，同时装配动态数据
     * @param key key可以是Id，也可以是ObjType+Id
     * */
    public Map get(Object key) {
        //在组件中添加数据源
        Map comp = store.get(key);
        if(comp.get("ds_id")!=null) {
            comp.put("ds", DSStore.getInstance().get(comp.get("ds_id")));
            List execs = ExecObjStore.getInstance().getByObj("comp_echarts"+comp.get("id"));
            comp.put("exec", execs.size()>0?execs.get(0): null);
            Map label_field_obj = DSStore.getInstance().getFields((Integer)comp.get("ds_id"), (Integer)comp.get("label_ds_field_id"));
            if(label_field_obj!=null) {
                comp.put("label_field", label_field_obj.get("field"));
                comp.put("label_field_type", label_field_obj.get("field_type"));
                comp.put("label_field_obj", label_field_obj);
            }
            Map value_field_obj = DSStore.getInstance().getFields((Integer)comp.get("ds_id"), (Integer)comp.get("value_ds_field_id"));
            if(value_field_obj!=null) {
                comp.put("value_field", value_field_obj.get("field"));
                comp.put("value_field_type", value_field_obj.get("field_type"));
                comp.put("value_field_obj", value_field_obj);
            }
            Map total_field_obj = DSStore.getInstance().getFields((Integer)comp.get("ds_id"), (Integer)comp.get("total_ds_field_id"));
            if(total_field_obj!=null) {
                comp.put("total_field", total_field_obj.get("field"));
                comp.put("total_field_type", total_field_obj.get("field_type"));
                comp.put("total_field_obj", total_field_obj);
            }
            Map unit_field_obj = DSStore.getInstance().getFields((Integer)comp.get("ds_id"), (Integer)comp.get("unit_ds_field_id"));
            if(unit_field_obj!=null) {
                comp.put("unit_field", unit_field_obj.get("field"));
                comp.put("unit_field_type", unit_field_obj.get("field_type"));
                comp.put("unit_field_obj", unit_field_obj);
            }
        }
        return comp;
    }
}


