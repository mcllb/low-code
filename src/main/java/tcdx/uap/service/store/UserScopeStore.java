package tcdx.uap.service.store;

import lombok.Getter;
import tcdx.uap.common.utils.Lutils;
import tcdx.uap.common.utils.SqlUtil;
import tcdx.uap.mapper.SystemMapper;
import tcdx.uap.service.BaseDBService;
import tcdx.uap.service.entities.UserScope;

import javax.servlet.http.HttpSession;
import java.util.*;
import java.util.stream.Collectors;

public class UserScopeStore {

    @Getter
    private static final UserScopeStore instance = new UserScopeStore();

    private UserScopeStore()
    {
        System.out.println("单例模式初始化权限对象仓库");
    }

    private BaseDBService db;
    private SystemMapper sysMapper;

    private Map<Object, Map> store = null;


    public synchronized void InitAll(BaseDBService db, SystemMapper sysMapper){
        this.db = db;
        this.sysMapper = sysMapper;
        InitAll();
    }

    public synchronized void InitAll(){
        store = new HashMap();
        List<Map> v_user_scope = db.selectByCauses("v_user_scope", SqlUtil.and(), null);
        //菜单对应角色
        List<Map> v_user_scope_group = db.selectByCauses("v_user_scope_group", null, null);
        List<Map> v_user_scope_role = db.selectByCauses("v_user_scope_role", null, null);
        assemble( v_user_scope, v_user_scope_role, v_user_scope_group);
    }

    public synchronized void set(Integer scope_id){
        List<Map> v_user_scope = db.selectByCauses("v_user_scope", SqlUtil.eq("id", scope_id), null);
        //菜单对应角色
        List<Map> v_user_scope_group = db.selectEq("v_user_scope_group", Lutils.genMap("scope_id", scope_id));
        List<Map> v_user_scope_role = db.selectEq("v_user_scope_role", Lutils.genMap("scope_id", scope_id));
        assemble( v_user_scope, v_user_scope_role, v_user_scope_group);
    }

    //根据用户角色，获取有权限的菜单
    public void assemble(List<Map> v_user_scope, List<Map> v_user_scope_role, List<Map> v_user_scope_group){
        for(Map scope:v_user_scope){
            scope.put("roles", v_user_scope_role.stream().filter(o->Objects.equals(o.get("scope_id"), scope.get("id"))).collect(Collectors.toList()));
            scope.put("groups", v_user_scope_group.stream().filter(o->Objects.equals(o.get("scope_id"), scope.get("id"))).collect(Collectors.toList()));
            store.put(scope.get("id"), scope);
            store.put((String)scope.get("obj_type")+scope.get("obj_id"), scope);
        }
    }

    public Map get(Object key){
        return store.get(key);
    }

    /**
     * 反馈行结构： scope_id, user_id, role_id, group_id,及中文信息
     * */
    public List<Map> getScopedUsers(Object key){
        Map scope = store.get(key);
        List users = sysMapper.selectScopedUsers(Lutils.genMap("scopeIds", Lutils.genList(scope.get("id"))));
        return users;
    }

}
