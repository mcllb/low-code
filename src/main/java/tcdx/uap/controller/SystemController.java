package tcdx.uap.controller;

import com.alibaba.fastjson.JSON;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import tcdx.uap.common.entity.AjaxResult;
import tcdx.uap.common.entity.page.TableDataInfo;
import tcdx.uap.common.utils.Lutils;
import tcdx.uap.common.utils.SqlUtil;
import tcdx.uap.constant.Constants;
import tcdx.uap.mapper.BaseDBMapper;
import tcdx.uap.mapper.BusinessMapper;
import tcdx.uap.mapper.ServiceConfigMapper;
import tcdx.uap.mapper.SystemMapper;
import tcdx.uap.service.BaseDBService;
import tcdx.uap.service.SystemService;
import tcdx.uap.service.entities.UserAction;
import tcdx.uap.service.store.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.lang.reflect.Method;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 通用请求处理
 *
 * @author ruoyi
 */
@Controller
@RequestMapping("/uap/system")
public class SystemController extends BaseController {
    private static final Logger log = LoggerFactory.getLogger(SystemController.class);
    @Autowired
    SystemMapper systemMapper;
    @Autowired
    SystemService systemService;
    @Autowired
    BaseDBMapper baseDBMapper;
    @Autowired
    private BaseDBService db;
    @Autowired
    private BusinessMapper businessMapper;
    @Autowired
    private ServiceConfigMapper serviceConfigMapper;

    /**
     * 查询运已经部署的流程定义
     *
     * @param map ajax传入的参数
     * @return AjaxResult
     */
//    @RequiresPermissions("system:system:get_sys_menu")
    @PostMapping("/get_sys_data")
    @ResponseBody
    public AjaxResult get_sys_data(@RequestBody Map<String, Object> map, HttpServletRequest request,HttpSession session) {
        List<Integer> userRoleIds = (List<Integer>) session.getAttribute("userRoleIds");
        List<Map> menus = db.selectByCauses("v_menu", SqlUtil.and(SqlUtil.eq("is_deleted", false)), Lutils.genMap("ord", "asc"));
        Integer userId = (Integer) session.getAttribute("userId");
        if(!Objects.equals(userId,879)) {
            menus = menus.stream().filter(m ->
                    RoleStore.getInstance().isMenuPermed(userRoleIds, m.get("id"))
            ).collect(Collectors.toList());
        }
        Map re = new HashMap();
        re.put("menus", menus);
        re.put("user", session.getAttribute("user"));
        re.put("userGroups", session.getAttribute("userGroups"));
        re.put("systems", session.getAttribute("systems"));
        return AjaxResult.success("success", re);
    }

    @PostMapping("/deploy_obj_permission")
    @ResponseBody
    public AjaxResult deploy_obj_permission(@RequestBody Map<String, Object> map) {
        List<String> types = (List<String>)map.get("types");
        if(types.contains("menu")) {
            MenuStore.getInstance().InitAll();
        }
        if(types.contains("view")) {
            ViewStore.getInstance().InitAll();
        }
        if(types.contains("api")) {
            ApiStore.getInstance().InitAll();
        }
        if(types.contains("btn")) {
            ExecObjStore.getInstance().InitAll();
        }
        return AjaxResult.success("success");
    }

    @PostMapping("/get_exec_obj")
    @ResponseBody
    public AjaxResult get_exec_obj(@RequestBody Map<String, Object> map) {
        System.out.println(map);
        List list = systemMapper.selectExec(map);
        return AjaxResult.success("suc", list);
    }

    //    @RequiresPermissions("system:system:get_sys_menu")
    @PostMapping("/get_userlist")
    @ResponseBody
    public TableDataInfo get_userlist(@RequestBody Map<String, Object> map) {
        //查询表单
        startPage(map);
        List<Map> users = systemMapper.selectSysUser(map);
        if(users.size()>0) {
            //获取用户的所在部门
            List<Map> user_groups = systemMapper.get_user_groups(Lutils.genMap("user_ids", users.stream().map(u -> u.get("id")).collect(Collectors.toList())));
            //再用户管理界面，这两个字段去除，否则保存用户时失败
            for(Map m:user_groups){
                m.remove("belong_group_id");
                m.remove("rn");
            }
            List<Map> user_roles = db.selectIn("v_user_role", "user_id", users.stream().map(u -> u.get("id")).collect(Collectors.toList()));
            for (Map user : users) {
                user.put("groups", user_groups.stream().filter(u -> Objects.equals(user.get("id"), u.get("user_id"))).collect(Collectors.toList()));
                user.put("roles", user_roles.stream().filter(u -> Objects.equals(user.get("id"), u.get("user_id"))).collect(Collectors.toList()));
            }
        }
        return getDataTable(users);
    }

    //    @RequiresPermissions("system:system:get_sys_menu")
    @PostMapping("/update_user")
    @ResponseBody
    public AjaxResult update_user(@RequestBody Map<String, Object> map) {
        //查询表单\
        Map user = (Map) map.get("user");
        List<Map> userRoles = (List<Map>)map.get("userRoles");
        List<Map> userGroups = (List<Map>)map.get("userGroups");
        Integer user_id = (Integer)user.get("id");
        db.updateEq("v_user", user, Lutils.genMap("id", user_id));
        //更新角色
        db.deleteEq("v_user_role", Lutils.genMap("user_id", user_id));
        for(Object role_id: userRoles){
            db.insertMap("v_user_role", Lutils.genMap("role_id", role_id, "user_id", user_id));
        }
        //更新组织
        db.deleteEq("v_user_group", Lutils.genMap("user_id", user_id));
        for(Map group: userGroups){
            group.put("user_id", user_id);
            group.remove("name");
            db.insertMap("v_user_group",group);
        }
        return new AjaxResult(AjaxResult.Type.SUCCESS, "修改成功");
    }

    @PostMapping("/add_user")
    @ResponseBody
    @Transactional
    public AjaxResult add_user(@RequestBody Map<String, Object> map) {
        //查询表单\
        Map user = (Map) map.get("user");
        List<Map> userRoles = (List<Map>)map.get("userRoles");
        List<Map> userGroups = (List<Map>)map.get("userGroups");
        user.put("is_deleted", false);
        user = db.insertMapAutoFillMaxOrd("v_user", user, "ord",
                Lutils.genMap("belong_group_id", user.get("belong_group_id")));
        Integer user_id = (Integer)user.get("id");
        //更新角色
        for(Object role_id: userRoles){
            db.insertMap("v_user_role", Lutils.genMap("role_id", role_id, "user_id", user_id));
        }
        //更新组织
        for(Map group: userGroups){
            db.insertMap("v_user_group", Lutils.genMap("group_id", group.get("group_id"), "user_id", user_id, "post_id", group.get("post_id")));
        }
        return new AjaxResult(AjaxResult.Type.SUCCESS, "修改成功");
    }


    //    @RequiresPermissions("system:system:get_sys_menu")
    @PostMapping("/get_user_scope_selected_data")
    @ResponseBody
    public Map get_user_scope_selected_data(@RequestBody Map<String, Object> map) {
        Object obj_id = map.get("obj_id");
        Object obj_type = map.get("obj_type");
        Map scopeObj = db.selectEq("v_user_scope", Lutils.genMap("obj_id", obj_id, "obj_type", obj_type)).get(0);
        Map re = new HashMap();
        List<Map> groups = db.selectEq("v_user_scope_group", Lutils.genMap("scope_id", scopeObj.get("id")));
        for (Map g : groups) {
            g.put("contains_children", g.get("contains_children") == null ? false : g.get("contains_children"));
        }
        List roles = db.selectEq("v_user_scope_role", Lutils.genMap("scope_id", scopeObj.get("id")));
        List posts = db.selectEq("v_user_scope_post", Lutils.genMap("scope_id", scopeObj.get("id")));
        re.put("scopeObj", scopeObj);
        re.put("groups", groups);
        re.put("posts", posts);
        re.put("roles", roles);
        return re;
    }


//    @PostMapping("/get_users_with_role_info")
//    @ResponseBody
//    public List get_users_with_role_info(@RequestBody Map<String, Object> map) {
//        List<Map> users = systemMapper.selectUserByIds(Lutils.genMap("ids", map.get("ids")));
//        List<Map> roles = systemMapper.selectUserRoles(Lutils.genMap("users", users.stream().map(o -> o.get("id")).collect(Collectors.toList())));
//        for (Map user : users) {
//            user.put("name", user.get("staff_nm"));
//            user.put("node_type", "staff");
//            user.put("user_id", user.get("id"));
//            user.put("node_key", "user" + user.get("id"));
//            user.put("parent_key", "group" + user.get("group_id"));
//            user.put("roles", roles.stream().filter(o -> o.get("user_id").equals(user.get("user_id"))).collect(Collectors.toList()));
//        }
//        //找到表名
//        return users;
//    }

    @PostMapping("/get_scoped_user___del")
    @ResponseBody
    public Map get_scoped_user___del(@RequestBody Map<String, Object> map) {
        String obj_type = (String) map.get("obj_type");
        Integer obj_id = (Integer) map.get("obj_id");  //op_id
//        List<Map> scopeList = baseDBService.selectEq("v_user_scope", Lutils.genMap("obj_type", obj_type, "obj_id", obj_id));
//        Map re = new HashMap();
//        if (scopeList != null && scopeList.size() > 0) {
//            Map scope = scopeList.get(0);
//            List<Map> users = systemService.getScopedUsers(scope);
//            List<Map> groups = new ArrayList<>();
//            //获取上级组织，用于属性列表的构建
//            List userGroupIds = users.stream().map(o -> o.get("group_id")).collect(Collectors.toList());
//            if (userGroupIds.size() > 0) {
//                groups = systemMapper.selectUpperGroups(Lutils.genMap("groupIds", userGroupIds));
//                for (Map group : groups) {
//                    group.put("node_type", "group");
//                    group.put("group_id", group.get("id"));
//                    group.put("node_key", "group" + group.get("id"));
//                    group.put("parent_key", "group" + group.get("parent_id"));
//                }
//            }
//            //获取角色信息，用于列表的角色属性显示
//            List userIds = users.stream().map(o -> o.get("id")).collect(Collectors.toList());
//            for (int i = 0; i < users.size(); i++) {
//                Map user = users.get(i);
//                user.put("name", user.get("staff_nm"));
//                user.put("node_type", "staff");
//                user.put("user_id", user.get("id"));
//                user.put("node_key", "user" + user.get("id"));
//                user.put("parent_key", "group" + user.get("group_id"));
//            }
//            re = Lutils.genMap("users", users, "groups", groups);
//        }
        //找到表名
        return null;
    }


    @PostMapping("/get_scoped_user_of_op")
    @ResponseBody
    public Map get_scoped_user_of_op(@RequestBody Map<String, Object> map) throws Exception {
        Integer op_id = (Integer) map.get("op_id");  //op_id
        Map re = systemService.getScopedUsersOfOp(op_id);
        //找到表名
        return re;
    }



    @RequestMapping("/save_role_perm")
    @ResponseBody
    @Transactional
    public AjaxResult save_role_perm(@RequestBody Map<String, Object> map) {
        Integer role_id = (Integer)map.get("role_id");
        List<Integer> checkedMenuKeys = (List<Integer>) map.get("checkedMenuKeys");
        List<Integer> checkedViewKeys = (List<Integer>) map.get("checkedViewKeys");
        List<Integer> checkedApiKeys = (List<Integer>) map.get("checkedApiKeys");
        List<Integer> checkedBtnKeys = (List<Integer>) map.get("checkedBtnKeys");
        db.updateEq("v_role",
                Lutils.genMap("menus", JSON.toJSONString(checkedMenuKeys),
                        "views", JSON.toJSONString(checkedViewKeys),
                        "apis", JSON.toJSONString(checkedApiKeys),
                        "btns", JSON.toJSONString(checkedBtnKeys)
                ) ,
                Lutils.genMap("id", role_id));
//        baseDBService.deleteEq("v_role_perm_obj", Lutils.genMap("role_id", role_id));
//        if(checkedMenuKeys.size()>0)
//            systemMapper.insert_role_perm(Lutils.genMap( "role_id", role_id, "obj_type","menu", "obj_ids", checkedMenuKeys.stream().distinct().collect(Collectors.toList())));
//        if(checkedViewKeys.size()>0)
//            systemMapper.insert_role_perm(Lutils.genMap( "role_id", role_id, "obj_type","view", "obj_ids", checkedViewKeys.stream().distinct().collect(Collectors.toList())));
//        if(checkedApiKeys.size()>0)
//            systemMapper.insert_role_perm(Lutils.genMap("role_id", role_id, "obj_type","api", "obj_ids", checkedApiKeys.stream().distinct().collect(Collectors.toList())));
//        if(checkedBtnKeys.size()>0)
//            systemMapper.insert_role_perm(Lutils.genMap("role_id", role_id, "obj_type","btn", "obj_ids", checkedBtnKeys.stream().distinct().collect(Collectors.toList())));
        //重新加载菜单权限
        RoleStore.getInstance().set(role_id);
        return new AjaxResult(AjaxResult.Type.SUCCESS,"修改成功");
    }

//    @RequestMapping("/create_user_by_id")
//    @ResponseBody
//    public Map create_user_by_id(@RequestBody Map<String, Object> map) {
//        map = (Map<String, Object>) map.get("map");
//        List<String> user = new ArrayList<String>();
//        user = (List<String>) map.get("user");
//        int role_id = (Integer) map.get("role_id");
//        baseDBService.updateEq("v_role_user", Lutils.genMap("is_show", false),
//                Lutils.genMap("role_id", role_id));
//        for (int i = 0; i < user.size(); i++) {
//            List<Map> maps = baseDBService.selectEq("v_role_user", Lutils.genMap("role_id", role_id, "user_id", user.get(i)));
//            if (maps.size() == 0) {
//                baseDBMapper.insertMapRetRow(Lutils.genMap("tn", "v_role_user", "insertMap", Lutils.genMap("role_id", role_id, "user_id", user.get(i), "is_show", true)));
//            } else {
//                //如果已存在更新is_show为t
//                baseDBService.updateEq("v_role_user", Lutils.genMap("is_show", true),
//                        Lutils.genMap("user_id", user.get(i)));
//            }
//        }
//
//        return Lutils.genMap("v_role_perm", map);
//    }

//    @RequestMapping("/create_role_by_id")
//    @ResponseBody
//    public Map create_role_by_id(@RequestBody Map<String, Object> map,HttpSession session) {
//        map = (Map<String, Object>) map.get("map");
//        List<String> role = new ArrayList<String>();
//        role = (List<String>) map.get("role");
//        Integer user_id = (Integer) session.getAttribute("userId");
//        baseDBService.updateEq("v_role_user", Lutils.genMap("is_show", false),
//                Lutils.genMap("user_id", user_id));
//        for (int i = 0; i < role.size(); i++) {
//            List<Map> maps = baseDBService.selectEq("v_role_user", Lutils.genMap("user_id", user_id, "role_id", role.get(i)));
//            if (maps.size() == 0) {
//                baseDBMapper.insertMapRetRow(Lutils.genMap("tn", "v_role_user", "insertMap", Lutils.genMap("user_id", user_id, "role_id", role.get(i), "is_show", true)));
//            } else {
//                //如果已存在更新is_show为t
//                baseDBService.updateEq("v_role_user", Lutils.genMap("is_show", true),
//                        Lutils.genMap("role_id", role.get(i)));
//            }
//        }
//        return Lutils.genMap("v_role_perm", map);
//    }


    /**
     * 登录
     */
    @PostMapping("/login")
    @ResponseBody
    public AjaxResult login(@RequestBody Map<String, Object> map, HttpSession session) {
        // 校验用户名和密码
        Map<String, Object> resultMap = systemService.validateUser(String.valueOf(map.get("username")), String.valueOf(map.get("password")), session);
        // 返回
        if (0 == (int)resultMap.get("code")) {
            // 登录成功
            return success(resultMap.get("data"));
        } else if (401 == (int)resultMap.get("code")) {
            // 登录失败
            return error(AjaxResult.Type.NOTLOGGEDIN, String.valueOf(resultMap.get("msg")));
        } else {
            // 暂时不会进入到这，随便返回一个错误
            return error((String) resultMap.get("msg"));
        }
    }

    @PostMapping("/resetUser")
    @ResponseBody
    public AjaxResult resetUser(@RequestBody Map<String, Object> map,
                            HttpSession session) {
        // 校验用户名和密码
        Integer userId = (Integer)map.get("userId");
        systemService.setSessionUserInfo(userId, session);
        return success();
    }

    /**
     * 退出登录
     */
    @GetMapping("/logout")
    @ResponseBody
    public AjaxResult logout(HttpServletRequest request) {
        // 获取session
        HttpSession session = request.getSession(false); // 避免自动创建新Session
        // 销毁Session
        if (session != null) {
            session.invalidate();
        }
        // 返回
        return success("退出成功");
    }

    /**
     * 修改密码
     */
    @PutMapping("/editPassword")
    @ResponseBody
    public AjaxResult editPassword(@RequestBody Map<String, Object> map, HttpServletRequest request) {
        // 获取session
        HttpSession session = request.getSession(false); // 避免自动创建新Session
        // 修改密码
        Map<String, Object> resultMap = systemService.editPassword((int) map.get("userId"), (String) map.get("oldPassword"), (String) map.get("newPassword"));
        // 返回
        if (0 == (int)resultMap.get("code")) {
            // 密码修改成功
//            // 销毁Session
//            if (session != null) {
//                session.invalidate();
//            }
            // 返回
            return success("密码修改成功");
        } else if (500 == (int)resultMap.get("code")) {
            // 密码修改失败
            return error(AjaxResult.Type.NOTLOGGEDIN, String.valueOf(resultMap.get("msg")));
        } else {
            // 暂时不会进入到这，随便返回一个错误
            return error((String) resultMap.get("msg"));
        }
    }

    @PutMapping("/forgetPassword")
    @ResponseBody
    public AjaxResult forget_password(@RequestBody Map<String, Object> map, HttpServletRequest request) {
        // 获取session
        HttpSession session = request.getSession(false); // 避免自动创建新Session
        // 修改密码
        Map<String, Object> resultMap = systemService.forgetPassword(String.valueOf(map.get("username")), String.valueOf(map.get("phone")),String.valueOf(map.get("certifycode")), String.valueOf(map.get("newPassword")));
        
        if(0==(int)resultMap.get("code")){
            return success("密码修改成功");
        }else if (500 == (int)resultMap.get("code")) {
            // 密码修改失败
            return error(AjaxResult.Type.NOTLOGGEDIN, String.valueOf(resultMap.get("msg")));
        } else {
            // 暂时不会进入到这，随便返回一个错误
            return error((String) resultMap.get("msg"));
        }
        
    }

    @PostMapping("/get_mapper_list")
    @ResponseBody
    public AjaxResult get_mapper_list(@RequestBody Map<String, Object> map, HttpSession httpSession) {
        String mapperName = (String) map.get("mapper");
        String methodName = (String) map.get("method");
        Map params = (Map) map.get("params");
        UserAction ua = new UserAction();
        ua.setUserInfo(httpSession);
        params.put("user", ua);
        try {
            if(Objects.equals(mapperName, "system")) {
                Method method = systemMapper.getClass().getMethod(methodName, Map.class);
                List list = (List<?>) method.invoke(systemMapper, params);
                return AjaxResult.success("suc", list);
            }
            else if (Objects.equals(mapperName, "base_db")) {
                Method method = BaseDBMapper.class.getMethod(methodName, Map.class);
                List list = (List<?>) method.invoke(baseDBMapper, params);
                return AjaxResult.success("suc", list);
            }
            else if (Objects.equals(mapperName, "business")) {
                Method method = businessMapper.getClass().getMethod(methodName, Map.class);
                List list = (List<?>) method.invoke(businessMapper, params);
                return AjaxResult.success("suc", list);
            }
            else if (Objects.equals(mapperName, "service_config")) {
                Method method = serviceConfigMapper.getClass().getMethod(methodName, Map.class);
                List list = (List<?>) method.invoke(serviceConfigMapper, params);
                return AjaxResult.success("suc", list);
            }
            else {
                return AjaxResult.success("failed", "no such mapper");
            }
        }catch (Exception e){
            e.printStackTrace();
            return AjaxResult.success("suc", e.getMessage());
        }
    }

    //    @RequiresPermissions("system:system:")
    @PostMapping("/get_groups")
    @ResponseBody
    public AjaxResult get_groups(@RequestBody Map<String, Object> map) {
        //查询表单
        Integer sas_system_id = (Integer) map.get("sas_system_id");
        List<Map> groups = new ArrayList<>();
        if(sas_system_id==null) {
            groups = db.selectByCauses("v_group", SqlUtil.eq("is_deleted", false), Lutils.genMap("ord", "asc"));
        }
        else{
            groups = systemMapper.get_sas_system_groups(Lutils.genMap("sas_system_id", sas_system_id));
        }
        if (groups.size() > 0) {
            //获取样式的部门
            List<Map> roles = db.selectIn("v_group_role", "group_id", groups.stream().map(u -> u.get("id")).collect(Collectors.toList()));
            for (Map user : groups) {
                user.put("roles", roles.stream().filter(u -> Objects.equals(user.get("id"), u.get("group_id"))).collect(Collectors.toList()));
            }
        }
        return AjaxResult.success("success", groups);
    }

    @PostMapping("/add_group")
    @ResponseBody
    @Transactional
    public AjaxResult add_group(@RequestBody Map<String, Object> map) {
        //查询表单\
        Map group = (Map) map.get("group");
        List<Map> roles = (List<Map>)map.get("roles");
        group.put("is_deleted", false);
        db.insertMapAutoFillMaxOrd("v_group", group, "ord",
                Lutils.genMap("parent_id", group.get("parent_id")));
        Integer group_id = (Integer)group.get("id");
        //更新角色
        for(Object role_id: roles){
            db.insertMap("v_group_role", Lutils.genMap("role_id", role_id, "group_id", group_id));
        }
        return new AjaxResult(AjaxResult.Type.SUCCESS, "修改成功");
    }

    @PostMapping("/update_group")
    @ResponseBody
    @Transactional
    public AjaxResult update_group(@RequestBody Map<String, Object> map) {
        //查询表单\
        Map group = (Map) map.get("group");
        List<Map> roles = (List<Map>)map.get("roles");
        db.updateEq("v_group", group, Lutils.genMap("id", group.get("id")));
        Integer group_id = (Integer)group.get("id");
        db.deleteEq("v_group_role", Lutils.genMap("group_id", group_id));
        for(Object role_id: roles){
            db.insertMap("v_group_role", Lutils.genMap("role_id", role_id, "group_id", group_id));
        }
        return new AjaxResult(AjaxResult.Type.SUCCESS, "修改成功");
    }

    @PostMapping("/set_group_chd_role")
    @ResponseBody
    public AjaxResult set_group_chd_role(@RequestBody Map<String, Object> map) {
        //查询表单
        Integer parent_id = (Integer) map.get("parent_id");
        Integer role_id = (Integer) map.get("role_id");
        boolean state = (boolean) map.get("state");
        systemMapper.set_group_chd_role(Lutils.genMap("parent_id", parent_id, "role_id", role_id, "state", false));
        if(state)
            systemMapper.set_group_chd_role(Lutils.genMap("parent_id", parent_id, "role_id", role_id, "state", true));
        return new AjaxResult(AjaxResult.Type.SUCCESS, "修改成功");
    }


    @PostMapping("/add_sas_system")
    @ResponseBody
    @Transactional(rollbackFor = Exception.class)
    public AjaxResult add_sas_system(@RequestBody Map<String, Object> map) {
        //查询表单
        map.put("is_deleted", false);
        Map sys = db.insertMapAutoFillMaxOrd("v_sys_param", map,"ord", Lutils.genMap("1",1));
        //增加默认角色分类
        Map role = db.insertMapAutoFillMaxOrd("v_role",
                Lutils.genMap("parent_id", 0, "name", sys.get("sys_name"), "sas_system_id", sys.get("id"),"menus","[]", "views", "[]", "apis","[]", "btns", "[]", "is_deleted", false),
                "ord", Lutils.genMap("1",1));
        db.insertMapAutoFillMaxOrd("v_role",
                Lutils.genMap("parent_id", role.get("id"), "name", "职位角色", "group_attr", "user_title", "menus","[]", "views", "[]", "apis","[]", "btns", "[]", "is_deleted", false),
                "ord", Lutils.genMap("parent_id", role.get("id")));
        db.insertMapAutoFillMaxOrd("v_role",
        Lutils.genMap("parent_id", role.get("id"), "name", "岗位角色", "group_attr", "user_post", "menus","[]", "views", "[]", "apis","[]", "btns", "[]", "is_deleted", false),
                "ord", Lutils.genMap("parent_id", role.get("id")));
        db.insertMapAutoFillMaxOrd("v_role",
        Lutils.genMap("parent_id", role.get("id"), "name", "雇佣角色", "group_attr","user_employee", "menus","[]", "views", "[]", "apis","[]", "btns", "[]", "is_deleted", false),
                "ord", Lutils.genMap("parent_id", role.get("id")));
        //增加菜单
        db.insertMapAutoFillMaxOrd("v_menu", Lutils.genMap("parent_id", 0, "name", sys.get("sys_name"), "type", "目录", "show", true,"icon", "el-icon-setting","sas_system_id", sys.get("id"),"is_deleted", false),
                "ord", Lutils.genMap("parent_id",0));
        //增加组织
        db.insertMapAutoFillMaxOrd("v_group", Lutils.genMap("parent_id", 0, "name", sys.get("sys_name"), "type", "org", "sas_system_id", sys.get("id"), "is_deleted", false),
                "ord", Lutils.genMap("parent_id", 0));
        //增加模块目录
        db.insertMapAutoFillMaxOrd("v_tree_view", Lutils.genMap("id", "view"+Constants.getTimeFormatId(), "parent_id", "view0", "name", sys.get("sys_name"), "view_type", "folder", "sas_system_id", sys.get("id"), "is_deleted", false),
                "ord", Lutils.genMap("parent_id", "view0"));
        //增加数据表目录
        db.insertMapAutoFillMaxOrd("v_tree_table", Lutils.genMap("id", "table"+Constants.getTimeFormatId(),"parent_id", "table0", "name", sys.get("sys_name"), "type", "folder", "sas_system_id", sys.get("id"), "is_deleted", false),
                "ord", Lutils.genMap("parent_id", "table0"));
        return new AjaxResult(AjaxResult.Type.SUCCESS, "添加成功");
    }

    @PostMapping("/get_group_attr_role_list")
    @ResponseBody
    public AjaxResult get_group_attr_role_list(@RequestBody Map<String, Object> map, HttpSession httpSession) {
        UserAction ua = new UserAction();
        ua.setUserInfo(httpSession);
        map.put("user", ua);
        Map re = new HashMap();
        List<Map> roles = systemMapper.get_role_by_group_attr(map);
        List user_post_role_ids = roles.stream()
                .filter(o->Objects.equals(o.get("group_attr"),"user_post"))
                .map(o->o.get("id")).collect(Collectors.toList());
        List<Map> user_post_roles = systemMapper.get_recursive_roles(Lutils.genMap("ids", user_post_role_ids));
        List user_title_role_ids = roles.stream()
                .filter(o->Objects.equals(o.get("group_attr"),"user_title"))
                .map(o->o.get("id")).collect(Collectors.toList());
        List<Map> user_title_roles = systemMapper.get_recursive_roles(Lutils.genMap("ids", user_title_role_ids));
        List user_employee_role_ids = roles.stream()
                .filter(o->Objects.equals(o.get("group_attr"),"user_employee"))
                .map(o->o.get("id")).collect(Collectors.toList());
        List<Map> user_employee_roles = systemMapper.get_recursive_roles(Lutils.genMap("ids", user_employee_role_ids));
        re.put("user_post_roles", user_post_roles);
        re.put("user_title_roles", user_title_roles);
        re.put("user_employee_roles", user_employee_roles);
        return AjaxResult.success("success", re);
    }
}
