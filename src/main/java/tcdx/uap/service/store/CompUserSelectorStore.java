package tcdx.uap.service.store;

import lombok.Getter;
import tcdx.uap.common.utils.Lutils;
import tcdx.uap.mapper.ServiceConfigMapper;
import tcdx.uap.service.BaseDBService;

import java.util.*;

public class CompUserSelectorStore {


    @Getter
    private static CompUserSelectorStore instance = new CompUserSelectorStore();

    private CompUserSelectorStore()
    {
        System.out.println("单例模式实现一-饿汉式");
    }

    private Map<Object,Map> store = new HashMap();
    BaseDBService db;
    ServiceConfigMapper cfgMapper;
    public void InitAll(BaseDBService db,ServiceConfigMapper cfgMapper){
        this.db = db;
        this.cfgMapper = cfgMapper;
//        InitAll();
    }

    public void InitAll(){
        List<Map> v_comp_user_selector = db.selectByCauses("v_comp_user_selector", null, null);
        assemble(v_comp_user_selector);
    }


    public void set(Object key){
        List<Map> v_comp_user_selector = new ArrayList<>();
        if(key instanceof String){
            String keyStr = key.toString();
            Integer obj_id = null;
            String obj_type = null;
            if(keyStr.startsWith("view")){
                obj_type = "view";
                obj_id = Integer.parseInt(keyStr.replace("view",""));
            }
            v_comp_user_selector = db.selectEq("v_comp_user_selector", Lutils.genMap("obj_type",obj_type, "obj_id", obj_id));
        }
        else if(key instanceof Integer){//所有数据源
            v_comp_user_selector = db.selectEq("v_comp_user_selector", Lutils.genMap("id", key));
        }
        if(v_comp_user_selector.size()>0) {
            assemble(v_comp_user_selector);
        }
    }

    public void assemble(List<Map> v_comp_user_selector){
        for(Map comp : v_comp_user_selector){
            store.put(comp.get("id"), comp);
            store.put((String)comp.get("obj_type")+comp.get("obj_id"), comp);
        }
    }

    /**
     * key 可以是id，也可以是obj_type+id
     * */
    public Map get(Object key){
        if(!store.containsKey(key)){
            set(key);
        }
        Map comp = store.get(key);

        return comp;
    }

}
