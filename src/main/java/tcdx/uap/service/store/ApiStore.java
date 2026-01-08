package tcdx.uap.service.store;

import lombok.Getter;
import tcdx.uap.common.utils.Lutils;
import tcdx.uap.mapper.ServiceConfigMapper;
import tcdx.uap.service.BaseDBService;

import java.util.*;
import java.util.stream.Collectors;

public class ApiStore {

    @Getter
    private static final ApiStore instance = new ApiStore();

    private ApiStore()
    {
        System.out.println("单例模式初始化权限对象仓库");
    }

    private BaseDBService db;
    private ServiceConfigMapper cfgMapper;

    private List<Map> apiList = null;
    private Map<Object,List> apiRoleMap = null;


    public synchronized void InitAll(BaseDBService db, ServiceConfigMapper cfg){
        this.db = db;
        this.cfgMapper = cfg;
        InitAll();
    }



    public synchronized void InitAll(){
        apiList = new ArrayList<>();
        apiRoleMap = new HashMap();
        List<Map> apiList = db.selectEq("v_api", Lutils.genMap("is_deleted", false), Lutils.genMap("ord", "asc"));
        //菜单对应角色
        List<Map> roleMenus = db.selectEq("v_role_perm_obj", Lutils.genMap("obj_type", "api"));
        //获得权限
        for(Map api: apiList){
            List<Map> mRoles = roleMenus.stream().filter(r-> Objects.equals(r.get("obj_id"), api.get("id"))).collect(Collectors.toList());
            List<Integer> roles = mRoles.stream().map(r-> (Integer)r.get("role_id")).collect(Collectors.toList());
            this.apiList.add(Lutils.genMap("obj", api, "roles", roles));
            apiRoleMap.put(api.get("url"), roles);
        }
    }

    //根据用户角色，获取有权限的视图
    public boolean HasApiPermission(String url, List<Integer> userRoleIds){
        if(apiList ==null || apiRoleMap ==null){
            InitAll();
        }
        List<Integer> roles = (List<Integer>) apiRoleMap.get(url);
        if(roles!=null ) {
            List<Integer> intersection = roles.stream().filter(userRoleIds::contains).collect(Collectors.toList());
            return !intersection.isEmpty();
        }
        return false;
    }

}
