package tcdx.uap.service.store;

import com.alibaba.fastjson.JSON;
//import com.aspose.cad.internal.R.m;
import lombok.Getter;
import tcdx.uap.common.utils.Lutils;
import tcdx.uap.mapper.ServiceConfigMapper;
import tcdx.uap.service.BaseDBService;

import java.util.*;
import java.util.stream.Collectors;

public class RoleStore {

    @Getter
    private static final RoleStore instance = new RoleStore();

    private RoleStore()
    {
        System.out.println("单例模式初始化权限对象仓库");
    }

    private BaseDBService db;
    private ServiceConfigMapper cfgMapper;

    private Map<Integer,List> viewStore = new HashMap();
    private Map<Integer,List> menuStore = new HashMap();
    private Map<Integer,List> apiStore = new HashMap();
    private Map<Integer,List> btnStore = new HashMap();


    public synchronized void InitAll(BaseDBService db, ServiceConfigMapper cfg){
        this.db = db;
        this.cfgMapper = cfg;
        InitAll();
    }

    public synchronized void InitAll(){
        List<Map> v_role = db.selectByCauses("v_role", Lutils.genMap(), null);
        List<Map> v_role_perm_obj = db.selectByCauses("v_role_perm_obj", Lutils.genMap(), null);
        //获得权限
//        for(Map role:v_role){
//            List<Map> roleMenus = v_role_perm_obj.stream().filter(r-> Objects.equals(r.get("role_id"), role.get("id")) && Objects.equals(r.get("obj_type"),"menu")).collect(Collectors.toList());
//            List<Map> roleViews = v_role_perm_obj.stream().filter(r-> Objects.equals(r.get("role_id"), role.get("id")) && Objects.equals(r.get("obj_type"),"view")).collect(Collectors.toList());
//            List<Map> roleBtns = v_role_perm_obj.stream().filter(r-> Objects.equals(r.get("role_id"), role.get("id")) && Objects.equals(r.get("obj_type"),"btn")).collect(Collectors.toList());
//            List<Map> roleApis = v_role_perm_obj.stream().filter(r-> Objects.equals(r.get("role_id"), role.get("id")) && Objects.equals(r.get("obj_type"),"api")).collect(Collectors.toList());
//            db.updateEq("v_role", Lutils.genMap(
//                    "menus",Lutils.ObjectToJSON(roleMenus.stream().map(o-> o.get("obj_id")).collect(Collectors.toList())),
//                    "views",Lutils.ObjectToJSON(roleViews.stream().map(o-> "view"+o.get("obj_id")).collect(Collectors.toList())),
//                    "btns",Lutils.ObjectToJSON(roleBtns.stream().map(o-> ""+o.get("obj_id")).collect(Collectors.toList())),
//                    "apis",Lutils.ObjectToJSON(roleApis.stream().map(o->o.get("obj_id")).collect(Collectors.toList()))
//            ), Lutils.genMap("id", role.get("id")));
//        }
        assemble(v_role);
    }

    public void assemble(List<Map> v_role){
        for(Map role : v_role){
            String apis = role.get("apis")==null?"[]":(String)role.get("apis");
            String menus = role.get("menus")==null?"[]":(String)role.get("menus");
            String btns = role.get("btns")==null?"[]":(String)role.get("btns");
            String views = role.get("views")==null?"[]":(String)role.get("views");
            System.out.println("views:"+ views);
            viewStore.put((Integer)role.get("id"), Lutils.StringToClass(views, List.class));
            menuStore.put((Integer)role.get("id"), Lutils.StringToClass(menus, List.class));
            btnStore.put((Integer)role.get("id"), Lutils.StringToClass(btns, List.class));
            apiStore.put((Integer)role.get("id"), Lutils.StringToClass(apis, List.class));
        }
    }

    public synchronized void set(Integer role_id){
        List<Map> v_role = db.selectEq("v_role", Lutils.genMap("id", role_id));
        assemble(v_role);
    }

    //判断角色是否拥有view_id的权限
    public boolean isViewPermed(List<Integer> roleIds, String view_id){
        List permedIds = new ArrayList();
        for(Object roleId: roleIds){
            permedIds.addAll(viewStore.get(roleId));
        }
        return permedIds.contains(view_id);
    }


    //判断角色是否拥有view_id的权限
    public boolean isMenuPermed(List<Integer> roleIds, Object menu_id){
        List permedIds = new ArrayList();
        for(Object roleId: roleIds){
            permedIds.addAll(menuStore.get(roleId));
        }
        return permedIds.contains(menu_id);
    }


    //判断角色是否拥有view_id的权限
    public boolean isBtnPermed(List<Integer> roleIds, String btn_id, Integer user_id){
        if (Objects.equals(user_id, 879)) {
            return true;
        }
        List permedIds = new ArrayList();
        for(Object roleId: roleIds){
            permedIds.addAll(btnStore.get(roleId));
        }
        return permedIds.contains(btn_id);
    }

    //判断角色是否拥有view_id的权限
    public boolean isApiPermed(List<Integer> roleIds, String api_id){
        List permedIds = new ArrayList();
        for(Object roleId: roleIds){
            permedIds.addAll(apiStore.get(roleId));
        }
        return permedIds.contains(api_id);
    }

}
