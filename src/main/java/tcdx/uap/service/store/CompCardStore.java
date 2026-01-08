package tcdx.uap.service.store;

import lombok.Getter;
import tcdx.uap.common.utils.SqlUtil;
import tcdx.uap.service.BaseDBService;
import tcdx.uap.service.BusinessService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CompCardStore {
    
    @Getter
    private static CompCardStore instance = new CompCardStore();
    
    private CompCardStore()  {
        System.out.println("单例模式初始化ViewStore");
    }

    //indexPage/modal/drawer
    private final Map<Object, Map> store = new HashMap();

    BaseDBService db;
    BusinessService bs;

    public void InitAll(BaseDBService db, BusinessService bs) {
        this.db = db;
        this.bs = bs;
        InitAll();
    }

    public void InitAll() {
        List<Map> v_comp_card = db.selectByCauses("v_comp_card", null, null);
        assemble(v_comp_card);
    }

    public void set(Integer id) {
        List<Map> v_comp_card = db.selectByCauses("v_comp_card", SqlUtil.eq("id", id), null);
        assemble(v_comp_card);
    }

    public void assemble(List<Map> v_comp_card) {
        for (Map comp : v_comp_card) {
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
        List execs = ExecObjStore.getInstance().getByObj("comp_card"+comp.get("id"));
        comp.put("execObj", execs.size()>0?execs.get(0):null);
        if(comp.get("ds_id")!=null) {
            comp.put("ds_id_","ds"+comp.get("ds_id"));
            comp.put("compDataSource", DSStore.getInstance().get(comp.get("ds_id")));
            Map value_field_obj = DSStore.getInstance().getFields((Integer)comp.get("ds_id"), (Integer)comp.get("value_ds_field_id"));
            if(value_field_obj!=null) {
                comp.put("valueField", value_field_obj.get("field"));
                comp.put("valueFieldType", value_field_obj.get("field_type"));
                comp.put("valueFieldObj", value_field_obj);
            }
        }
        return comp;
    }
}
