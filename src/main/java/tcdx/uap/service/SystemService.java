package tcdx.uap.service;

import cn.hutool.core.collection.CollUtil;
import com.alibaba.excel.util.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import tcdx.uap.common.utils.Lutils;
import tcdx.uap.common.utils.SecurityUtils;
import tcdx.uap.mapper.BaseDBMapper;
import tcdx.uap.mapper.ServiceConfigMapper;
import tcdx.uap.mapper.SystemMapper;
import tcdx.uap.service.entities.*;
import tcdx.uap.service.store.ExecObjStore;
import tcdx.uap.service.store.UserScopeStore;

import javax.servlet.http.HttpSession;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 参数配置 服务层实现
 * 
 * @author ruoyi
 */
@Service
public class SystemService {

    @Autowired
    private ServiceConfigMapper serviceConfigMapper;

    @Autowired
    BaseDBMapper baseDBMapper;

    @Autowired
    BaseDBService baseDBService;
    @Autowired
    private SystemMapper systemMapper;

    public List<Integer> gen_ids(int count) {
        List<Map> l = baseDBService.selectSql("select nextval('tcdx_datatable_id_seq') id,generate_series(1, " + count + ") num");
        return l.stream().map(o -> Integer.parseInt(o.get("id").toString())).collect(Collectors.toList());
    }


    /**
     * 根据依赖表的ids所在行 关联当前表match_column的值, 与当前表match_column匹配的ids,ids即当前表要依赖更新的范围.
     * @param columnRule  当前列的操作对象ColumnRuleOperation
     * */
    public void addInIds_ByDependMatchColValues(ColumnRuleOperation columnRule){
        List<Map> dependDependFieldOldValues = baseDBService.selectIn(
                columnRule.dependTableName,
                true,
                columnRule.updateRelation.getDependMatchFields(),
                "id_",
                columnRule.dependIds);
        ///------------------------------------------------------------------------
        for (Map dependTableOldRow : dependDependFieldOldValues) {
            Map triggerTableOldMatchValue = new HashMap();
            if(Lutils.nvl(columnRule.updateRelation.match_column1,"").length()>0){
                triggerTableOldMatchValue.put(columnRule.updateRelation.match_column1, dependTableOldRow.get(columnRule.updateRelation.depend_table_match_column1));
            }
            if(Lutils.nvl(columnRule.updateRelation.match_column2,"").length()>0){
                triggerTableOldMatchValue.put(columnRule.updateRelation.match_column2, dependTableOldRow.get(columnRule.updateRelation.depend_table_match_column2));
            }
            if(Lutils.nvl(columnRule.updateRelation.match_column3,"").length()>0){
                triggerTableOldMatchValue.put(columnRule.updateRelation.match_column3, dependTableOldRow.get(columnRule.updateRelation.depend_table_match_column3));
            }
            columnRule.orWhereOfRowsList.add(triggerTableOldMatchValue);
        }
        //查匹配的行的ids
        List<Integer> inIds = baseDBService.selectByCauses(columnRule.tableName,
                Lutils.genList("id_"),
                columnRule.getWhereCause(),
                null).stream().map(o->(Integer)o.get("id_")).collect(Collectors.toList());
        columnRule.addInIds(inIds);
    }

    public void addDependIds_FromDependColumnOperations(ColumnRuleOperation upperTableColumnOperation, List<ColumnRuleOperation> dependTableColOperationList)
    {
        for (String column : upperTableColumnOperation.updateRelation.getDependFields()) {
            //根据每个关联的列，找到匹配的操作，合并操作中的where条件
            for (ColumnRuleOperation dependColumnRuleOperation : dependTableColOperationList) {
                if (column.equals(dependColumnRuleOperation.column)) {
                    upperTableColumnOperation.dependIds.addAll(dependColumnRuleOperation.inIds);
                }
            }
        }
    }

//    public List<Map> getScopedUsers(Integer obj_id, String obj_type){
//        List<Map> scopeList = baseDBService.selectEq("v_user_scope", Lutils.genMap("obj_type", obj_type,"obj_id", obj_id));
//        if(scopeList!=null&&scopeList.size()>0) {
//            return getScopedUsers(scopeList.get(0));
//        }
//        return null;
//    }


//    public List<Map> getScopedUsers(Map scope){
//        List<Map> users = null;
//        if(scope!=null&&scope.size()>0) {
//            users = systemMapper.selectScopedUsers(Lutils.genMap(
//                    "scope_id", scope.get("id"),
//                    "contains_all_groups", scope.get("contains_all_groups"),
//                    "contains_all_roles_posts", scope.get("contains_all_roles_posts")
//            ));
//        }
//        return users;
//    }

    public Map getScopedUsersOfOp(Integer op_id) throws Exception {
        Map op = ExecObjStore.getInstance().getOp(op_id);
        if(op==null){
            throw new Exception("绑定的动作op_id不存在。请检查人员选择器绑定的执行动作。");
        }
        Map scope = UserScopeStore.getInstance().get("edge-assign-scope"+op.get("flow_edge_id"));
        Map re = new HashMap();
        if (scope!= null) {
            List<Map> users = UserScopeStore.getInstance().getScopedUsers(scope.get("id"));
            List<Map> groups = new ArrayList<>();
            //获取上级组织，用于属性列表的构建
            List userGroupIds = users.stream().map(o -> o.get("group_id")).collect(Collectors.toList());
            if (userGroupIds.size() > 0) {
                groups = systemMapper.selectUpperGroups(Lutils.genMap("groupIds", userGroupIds));
            }
            //获取角色信息，用于列表的角色属性显示
            List<Map> roles = new ArrayList<>();
            List userIds = users.stream().map(o -> o.get("id")).collect(Collectors.toList());
            if (userIds.size() > 0) {
                roles = systemMapper.selectUserRoles(Lutils.genMap("userIds", userIds));
            }
            for (int i = 0; i < users.size(); i++) {
                Map user = users.get(i);
                user.put("name", user.get("staff_nm"));
                user.put("node_type", "staff");
                user.put("user_id", user.get("id"));
                user.put("node_key", "user" + user.get("id"));
                user.put("parent_key", "group" + user.get("group_id"));
                user.put("roles", roles.stream().filter(o -> o.get("user_id").equals(user.get("user_id"))).collect(Collectors.toList()));
            }
            for (Map group : groups) {
                group.put("node_type", "group");
                group.put("group_id", group.get("id"));
                group.put("node_key", "group" + group.get("id"));
                group.put("parent_key", "group" + group.get("parent_id"));
            }
            re = Lutils.genMap("users", users, "groups", groups, "roles", roles);
        }
        return re;
    }

    public Map<String, Object> validateUser(String username, String password, HttpSession session) {
        Map<String, Object> resultMap = new HashMap<>();
        // 查询出该工号的所有用户
        List<Map> userList = systemMapper.selectSysUserByUsername(username);
        // 校验
        if (userList == null || userList.size() == 0) {
            // 登录失败，用户名或密码错误
            resultMap.put("code", 401);
            resultMap.put("msg", "用户名或密码错误");
        } else if (userList.size() == 1) {
            Map user = userList.get(0);
            // 校验密码
            boolean psw = SecurityUtils.matchesPassword(password, String.valueOf(user.get("psw")));
            if (psw) {
                Integer userId = (Integer)user.get("id");
                Map<String, Object> result = new HashMap<>();
                result.put("user_id", userId);
                result.put("staff_nm", (String) user.get("staff_nm"));
                //设置角色
                setSessionUserInfo(userId, session);

                resultMap.put("code", 0);
                resultMap.put("msg", "登录成功");
                resultMap.put("data", result);
            } else {
                // 登录失败，用户名或密码错误
                resultMap.put("code", 401);
                resultMap.put("msg", "用户名或密码错误");
            }
        } else {
            // 登录失败，用户存在多个
            resultMap.put("code", 401);
            resultMap.put("msg", "用户存在多个，请联系管理员");
        }
        //返回
        return resultMap;
    }

    public void setSessionUserInfo(Integer userId, HttpSession session) {
        Map user = systemMapper.selectUserById(userId);
        session.setAttribute("user", user);
        session.setAttribute("userId", userId);
        session.setAttribute("userName", user.get("staff_nm"));
        List<Map> userRoleRows = systemMapper.get_user_roles(Lutils.genMap("user_id", userId));
        session.setAttribute("userRoles", userRoleRows);
        session.setAttribute("userRoleIds", userRoleRows.stream().map(o->o.get("role_id")).collect(Collectors.toList()));
        Map rr = new HashMap();
        for(Map tmp: userRoleRows){
            rr.put("r"+tmp.get("role_id"), tmp.get("role_id"));
        }
        session.setAttribute("userRoleBoolMap", rr);
        List<Map> user_groups = systemMapper.get_user_groups(Lutils.genMap("user_ids", Arrays.asList(userId)));
        if (CollUtil.isNotEmpty(user_groups)) {
            Map group = user_groups.get(0);
        }
        List<Integer> groupIds = user_groups.stream().map(f->(Integer)f.get("group_id")).collect(Collectors.toList());
        session.setAttribute("userGroups", user_groups);
        session.setAttribute("userGroupIds", groupIds);
        //获取用户组织所属SAS系统
        List<Map> sasSystems = systemMapper.get_user_sas_system(Lutils.genMap("group_ids", groupIds));
        session.setAttribute("systems", sasSystems);
    }

    public void setSystemInfo(HttpSession session, Map<String, Object> result) {

    }

    public Map<String, Object> editPassword(Integer userId, String oldPassword, String newPassword) {

        Map<String, Object> resultMap = new HashMap<>();

        // 查询用户信息
        Map map = systemMapper.selectUserById(userId);

        // 校验旧密码
        boolean psw = SecurityUtils.matchesPassword(oldPassword, String.valueOf(map.get("psw")));
        if (psw) {
            // 校验成功
            if (StringUtils.isBlank(newPassword)) {
                resultMap.put("code", 500);
                resultMap.put("msg", "新密码不能为空");
            } else {
                // 修改密码
                systemMapper.updateUserPasswordByUserId(userId, SecurityUtils.encryptPassword(newPassword));
                resultMap.put("code", 0);
                resultMap.put("msg", "密码修改成功");
            }
        } else {
            // 校验失败
            resultMap.put("code", 500);
            resultMap.put("msg", "密码错误");
        }

        // 返回
        return resultMap;
    }

    public Map<String,Object> forgetPassword(String username, String phone, String certifycode, String newPassword){
       
        Map<String, Object> resultMap = new HashMap<>();

        Map map = systemMapper.selectSysFirstUserByUsername(username);
        
        if (map == null) {
            resultMap.put("code", 500);
            resultMap.put("msg", "用户不存在");
            return resultMap;
        }
        // 校验手机号
        boolean phonejudge = phone != null && phone.equals(String.valueOf(map.get("phone")));
        boolean certifycodejudge = false;//现在默认为false
        //校验验证码还没写。
        if(phonejudge && certifycodejudge){
            //校验成功
            if(StringUtils.isBlank(newPassword) || StringUtils.isBlank(certifycode.toString())){
                resultMap.put("code", 500);
                resultMap.put("msg", "新密码或验证码不能为空");
            }
            else{
                //修改密码
                systemMapper.updateUserPasswordByUsername(String.valueOf(map.get("staff_no")), SecurityUtils.encryptPassword(newPassword));
                resultMap.put("code", 0);
                resultMap.put("msg", "密码修改成功");
            }
        }
        else{
            //校验失败
            resultMap.put("code", 500);
            resultMap.put("msg", "手机号或验证码错误");
        }

        return resultMap;
    }

}
