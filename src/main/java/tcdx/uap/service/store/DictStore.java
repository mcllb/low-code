package tcdx.uap.service.store;

import lombok.Getter;
import tcdx.uap.common.utils.Lutils;
import tcdx.uap.common.utils.SqlUtil;
import tcdx.uap.mapper.ServiceConfigMapper;
import tcdx.uap.service.BaseDBService;
import tcdx.uap.service.entities.CompUtils;

import java.util.*;
import java.util.stream.Collectors;

public class DictStore {

    @Getter
    private static final DictStore instance = new DictStore();

    private DictStore()
    {
        System.out.println("单例模式初始化权限对象仓库");
    }

    private BaseDBService db;
    private ServiceConfigMapper cfgMapper;

    private Map<Object, Map> store = null;
    private Map<Object, Map> itemStore = null;


    public synchronized void InitAll(BaseDBService db, ServiceConfigMapper cfg){
        this.db = db;
        this.cfgMapper = cfg;
        InitAll();
    }

    public synchronized void InitAll(){
        store = new HashMap();
        itemStore = new HashMap();
        List<Map> v_dict = db.selectByCauses("v_dict", SqlUtil.and(SqlUtil.eq("is_deleted", false)), null);
        //菜单对应角色
        List<Map> v_dict_item = db.selectByCauses("v_dict_item", null, Lutils.genMap("ord", "asc"));
        //菜单对应角色
        List<Map> v_user_scope = db.selectByCauses("v_user_scope",
                SqlUtil.and(SqlUtil.eq("obj_type", "dict_item"),
                        SqlUtil.in("obj_id", v_dict_item.stream().map(o->o.get("id")).collect(Collectors.toList()))),
                null);
        assemble(v_dict, v_dict_item, v_user_scope);
    }

    public synchronized void set(Integer dict_id){
        List<Map> v_dict = db.selectByCauses("v_dict", SqlUtil.eq("id", dict_id), null);
        //菜单对应角色
        List<Map> v_dict_item = db.selectByCauses("v_dict_item",
                SqlUtil.in("dict_id", v_dict.stream().map(t->t.get("id")).collect(Collectors.toList())),
                Lutils.genMap("ord", "asc"));
        //菜单对应角色
        List<Map> v_user_scope = db.selectByCauses("v_user_scope",
                SqlUtil.and(SqlUtil.eq("obj_type", "dict_item"),
                        SqlUtil.in("obj_id", v_dict_item.stream().map(o->o.get("id")).collect(Collectors.toList()))),
                null);
        assemble(v_dict, v_dict_item, v_user_scope);
    }


    //根据用户角色，获取有权限的菜单
    public void assemble(List<Map> v_dict, List<Map> v_dict_item, List<Map> v_user_scope){
        for(Map item:v_dict_item){
            List<Map> scope = v_user_scope.stream().filter(o->Objects.equals(item.get("id"), o.get("obj_id"))).collect(Collectors.toList());
            if(!scope.isEmpty()) {
                item.put("scope_id", scope.get(0).get("id"));
            }
            itemStore.put(item.get("id"), item);
        }
        for(Map dict:v_dict){
            dict.put("items", v_dict_item.stream().filter(o->Objects.equals(o.get("dict_id"), dict.get("id"))).collect(Collectors.toList()));
            store.put(dict.get("id"), dict);
        }
    }

    public Map get(Object key){
        return store.get(key);
    }

    public List<Map> getDictItems(Object dict_id){
        Map dict = store.get(dict_id);
        if(dict!=null)
            return (List)dict.get("items");
        return null;
    }

    //字典选项，对应的用户清单 { item_id:  scope_id:  user_id:  users:}
    public List<Map> getDictItemScopeUsers(Object dict_id){
        Map dict = store.get(dict_id);
        if(dict!=null) {
            //字典选项
            List<Map> items = (List) dict.get("items");
            //字典范围
            List scopeIds = items.stream().filter(o->o.get("scope_id")!=null).map(o->(Integer)o.get("scope_id")).collect(Collectors.toList());
            if(scopeIds.isEmpty())
                return new ArrayList<>();
            List re = new ArrayList();
            for(Map item : items){
                Map obj = new HashMap();
                obj.put("item_id", item.get("id"));
                obj.put("name",    item.get("name"));
                obj.put("rel_group_id", item.get("rel_group_id"));
                obj.put("rel_role_id",  item.get("rel_role_id"));
                obj.put("ord",      item.get("ord"));
                List<Map> users = CompUtils.getInstance().getScopesUsers(
                        Lutils.genList(item.get("rel_role_id")),
                        Lutils.genList(item.get("rel_group_id")));
                obj.put("users", users);
                if(!users.isEmpty()){
                    obj.put("user_id",       users.get(0).get("user_id"));
                    obj.put("first_user_id", users.get(0).get("user_id"));
                }
                re.add(obj);
            }
            return re;
        }
        return null;
    }

}
