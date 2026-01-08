package tcdx.uap.service.store;

import lombok.Getter;
import tcdx.uap.common.utils.Lutils;
import tcdx.uap.service.BaseDBService;
import tcdx.uap.service.BusinessService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CompCarouselStore {

    @Getter
    private static CompCarouselStore instance = new CompCarouselStore();

    private CompCarouselStore() {
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
        List<Map> v_comp_carousel = db.selectByCauses("v_comp_carousel", null, null);
        assemble(v_comp_carousel);
    }

    public void set(Integer comp_id) {
        List<Map> v_comp_carousel = db.selectEq("v_comp_carousel", Lutils.genMap("id", comp_id));
        assemble(v_comp_carousel);
    }

    public void assemble(List<Map> v_comp_carousel ){
        for (Map comp : v_comp_carousel) {
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
        Map compDataSource = DSStore.getInstance().get("comp_carousel" + comp.get("id"));
        comp.put("compDataSource", compDataSource);
        List execs = ExecObjStore.getInstance().getByObj("comp_carousel"+comp.get("id"));
        comp.put("execObj", execs.size()>0?execs.get(0):null);
        Map titleFieldObj = DSStore.getInstance().getFields((Integer)compDataSource.get("id"), (Integer)comp.get("title_ds_field_id"));
        if(titleFieldObj!=null) {
            comp.put("titleField", titleFieldObj.get("field"));
            comp.put("titleFieldType", titleFieldObj.get("field_type"));
            comp.put("titleFieldObj", titleFieldObj);
        }
        Map imgFieldObj = DSStore.getInstance().getFields((Integer)compDataSource.get("id"), (Integer)comp.get("img_ds_field_id"));
        if(imgFieldObj!=null) {
            comp.put("imgField", imgFieldObj.get("field"));
            comp.put("imgFieldType", imgFieldObj.get("field_type"));
            comp.put("imgFieldObj", imgFieldObj);
        }
        return comp;
    }

    /**
     * 浅获取组件，同时装配动态数据
     * @param key key可以是Id，也可以是ObjType+Id
     * */
    public Map getSimplify(Object key) {
        //在组件中添加数据源
        Map comp = store.get(key);
        Map compDataSource = DSStore.getInstance().getSimplify("comp_carousel" + comp.get("id"));
        comp.put("compDataSource", compDataSource);
        Map title_field_obj = DSStore.getInstance().getFields((Integer)compDataSource.get("id"), (Integer)comp.get("title_ds_field_id"));
        if(title_field_obj!=null) {
            comp.put("titleField", title_field_obj.get("field"));
            comp.put("titleFieldType", title_field_obj.get("field_type"));
            comp.put("titleFieldObj", title_field_obj);
        }
        Map img_field_obj = DSStore.getInstance().getFields((Integer)compDataSource.get("id"), (Integer)comp.get("img_ds_field_id"));
        if(img_field_obj!=null) {
            comp.put("imgField", img_field_obj.get("field"));
            comp.put("imgFieldType", img_field_obj.get("field_type"));
            comp.put("imgFieldObj", img_field_obj);
        }
        return comp;
    }
}

