package tcdx.uap.service.store;

import lombok.Getter;
import tcdx.uap.common.utils.SqlUtil;
import tcdx.uap.service.BaseDBService;
import tcdx.uap.service.BusinessService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CompTimelineStore {
    @Getter
    private static CompTimelineStore instance = new CompTimelineStore();

    private CompTimelineStore()  {
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
        List<Map> v_comp_timeline = db.selectByCauses("v_comp_timeline", null, null);
        assemble(v_comp_timeline);
    }

    public void set(Integer id) {
        List<Map> v_comp_timeline = db.selectByCauses("v_comp_timeline", SqlUtil.eq("id", id), null);
        assemble(v_comp_timeline);
    }

    public void assemble(List<Map> v_comp_timeline) {
        for (Map comp : v_comp_timeline) {
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
        List execs = ExecObjStore.getInstance().getByObj("comp_timeline"+comp.get("id"));
        comp.put("execObj", execs.size()>0?execs.get(0):null);
        if(comp.get("ds_id")!=null) {
            comp.put("compDataSource", DSStore.getInstance().get(comp.get("ds_id")));
            Map edge_field_obj = DSStore.getInstance().getFields((Integer)comp.get("ds_id"), (Integer)comp.get("edge_ds_field_id"));
            if(edge_field_obj!=null) {
                comp.put("edgeField", edge_field_obj.get("field"));
                comp.put("edgeFieldType", edge_field_obj.get("field_type"));
                comp.put("edgeFieldObj", edge_field_obj);
            }
            Map node_field_obj = DSStore.getInstance().getFields((Integer)comp.get("ds_id"), (Integer)comp.get("node_ds_field_id"));
            if(node_field_obj!=null) {
                comp.put("nodeField", node_field_obj.get("field"));
                comp.put("nodeFieldType", node_field_obj.get("field_type"));
                comp.put("nodeFieldObj", node_field_obj);
            }
            Map user_field_obj = DSStore.getInstance().getFields((Integer)comp.get("ds_id"), (Integer)comp.get("user_ds_field_id"));
            if(user_field_obj!=null) {
                comp.put("userField", user_field_obj.get("field"));
                comp.put("userFieldType", user_field_obj.get("field_type"));
                comp.put("userFieldObj", user_field_obj);
            }
            Map group_field_obj = DSStore.getInstance().getFields((Integer)comp.get("ds_id"), (Integer)comp.get("group_ds_field_id"));
            if(group_field_obj!=null) {
                comp.put("groupField", group_field_obj.get("field"));
                comp.put("groupFieldType", group_field_obj.get("field_type"));
                comp.put("groupFieldObj", group_field_obj);
            }
            Map timestamp_field_obj = DSStore.getInstance().getFields((Integer)comp.get("ds_id"), (Integer)comp.get("timestamp_ds_field_id"));
            if(timestamp_field_obj!=null) {
                comp.put("timestampField", timestamp_field_obj.get("field"));
                comp.put("timestampFieldType", timestamp_field_obj.get("field_type"));
                comp.put("timestampFieldObj", timestamp_field_obj);
            }
        }
        return comp;
    }
}
