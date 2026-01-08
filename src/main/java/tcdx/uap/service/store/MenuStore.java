package tcdx.uap.service.store;

import lombok.Getter;
import tcdx.uap.common.utils.Lutils;
import tcdx.uap.mapper.ServiceConfigMapper;
import tcdx.uap.service.BaseDBService;

import java.util.*;
import java.util.stream.Collectors;

public class MenuStore {

    @Getter
    private static final MenuStore instance = new MenuStore();

    private MenuStore()
    {
        System.out.println("单例模式初始化权限对象仓库");
    }

    private BaseDBService db;
    private ServiceConfigMapper cfgMapper;

    private List<Map> menuList = null;
    private Map<Object,List> menuRoleMap = null;


    public synchronized void InitAll(BaseDBService db, ServiceConfigMapper cfg){
        this.db = db;
        this.cfgMapper = cfg;
        InitAll();
    }

    public synchronized void InitAll(){
        menuList = new ArrayList<>();
        menuRoleMap = new HashMap();
        List<Map> v_menu = db.selectEq("v_menu", Lutils.genMap("is_deleted", false), Lutils.genMap("ord", "asc"));
        //菜单对应角色
        List<Map> v_role_perm_obj = db.selectEq("v_role_perm_obj", Lutils.genMap("obj_type", "menu"));
        //获得权限
        for(Map menu : v_menu){
            List<Map> mRoles = v_role_perm_obj.stream().filter(r-> Objects.equals(r.get("obj_id"), menu.get("id"))).collect(Collectors.toList());
            List<Integer> roles = mRoles.stream().map(r-> (Integer)r.get("role_id")).collect(Collectors.toList());
            menu.put("roles", roles);
            this.menuList.add(menu);
            menuRoleMap.put(menu.get("id"), roles);
        }
    }

    //根据用户角色，获取有权限的菜单
    public List<Map> getMenuList(List<Integer> userRoleIds){
        if(menuList ==null){
            InitAll();
        }
        List<Map> rList = new ArrayList<>();
        for(Map menu: menuList){
            List<Integer> roles = (List)menu.get("roles");
            List<Integer> intersection = roles.stream().filter(userRoleIds::contains).collect(Collectors.toList());
            if(intersection.size()>0){
                Map cp = Lutils.copyMap(menu);
                cp.remove("roles");
                rList.add(cp);
            }
        }
        return rList;
    }

}
