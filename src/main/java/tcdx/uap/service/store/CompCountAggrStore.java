package tcdx.uap.service.store;

import lombok.Getter;
import tcdx.uap.common.utils.SqlUtil;
import tcdx.uap.service.BaseDBService;
import tcdx.uap.service.BusinessService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CompCountAggrStore {

    @Getter
    private static CompCountAggrStore instance = new CompCountAggrStore();

    private CompCountAggrStore() {
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
        List<Map> v_comp_count_aggr = db.selectByCauses("v_comp_count_aggr", null, null);
        assemble(v_comp_count_aggr);
    }

    public void set(Integer id) {
        List<Map> v_comp_count_aggr = db.selectByCauses("v_comp_count_aggr", SqlUtil.eq("id", id), null);
        assemble(v_comp_count_aggr);
    }

    public void assemble(List<Map> v_comp_count_aggr) {
        for (Map comp : v_comp_count_aggr) {
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
        List execs = ExecObjStore.getInstance().getByObj("comp_count_aggr"+comp.get("id"));
        comp.put("execObj", execs.size()>0?execs.get(0):null);
        if(comp.get("ds_id")!=null) {
            comp.put("compDataSource", DSStore.getInstance().get(comp.get("ds_id")));
            Map label_field_obj = DSStore.getInstance().getFields((Integer)comp.get("ds_id"), (Integer)comp.get("label_ds_field_id"));
            if(label_field_obj!=null) {
                comp.put("labelField", label_field_obj.get("field"));
                comp.put("labelFieldType", label_field_obj.get("field_type"));
                comp.put("labelFieldObj", label_field_obj);
            }
            Map value_field_obj = DSStore.getInstance().getFields((Integer)comp.get("ds_id"), (Integer)comp.get("value_ds_field_id"));
            if(value_field_obj!=null) {
                comp.put("valueField", value_field_obj.get("field"));
                comp.put("valueFieldType", value_field_obj.get("field_type"));
                comp.put("valueFieldObj", value_field_obj);
            }
            Map total_field_obj = DSStore.getInstance().getFields((Integer)comp.get("ds_id"), (Integer)comp.get("total_ds_field_id"));
            if(total_field_obj!=null) {
                comp.put("totalField", total_field_obj.get("field"));
                comp.put("totalFieldType", total_field_obj.get("field_type"));
                comp.put("totalFieldObj", total_field_obj);
            }
            Map unit_field_obj = DSStore.getInstance().getFields((Integer)comp.get("ds_id"), (Integer)comp.get("unit_ds_field_id"));
            if(unit_field_obj!=null) {
                comp.put("unitField", unit_field_obj.get("field"));
                comp.put("unitFieldType", unit_field_obj.get("field_type"));
                comp.put("unitFieldObj", unit_field_obj);
            }
        }
        return comp;
    }
}

