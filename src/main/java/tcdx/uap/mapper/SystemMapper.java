package tcdx.uap.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

/**
 * 用户与角色关联表 数据层
 *
 * @author ruoyi
 */
@Mapper
public interface SystemMapper {
    public List<Map> selectSysMenu(Map<String, Object> m);

    public List<Map> selectSysUser(Map<String, Object> m);
    public List<Map> get_user_groups(Map<String, Object> m);
    public List<Map> get_sas_system_groups(Map<String, Object> m);

    public List<Map> selectSysUserByUsername(@Param("username") String username);

    public Map selectSysFirstUserByUsername(@Param("username") String username);

    public List<Map> selectScopedUsers(Map<String, Object> m);

    public Map selectUserById(@Param("id") Integer id);

    public Integer updateUserPasswordByUserId(@Param("userId") Integer userId, @Param("password") String password);

    public Integer updateUserPasswordByUsername(@Param("username") String username, @Param("password") String password);

    public List<Map> selectUserByIds(Map<String, Object> m);

    public List<Map> selectUpperGroups(Map<String, Object> m);

    public List<Map> selectUserRoles(Map<String, Object> m);

    public List<Map> selectExec(Map<String, Object> m);
    public List<Map> getUserScopeGroup(Map<String, Object> m);
    public List<Map> getUserScopeRolePost(Map<String, Object> m);
    public List<Map> get_role_by_group_attr(Map<String, Object> m);
    public List<Map> get_user_roles(Map<String, Object> m);
    public List<Map> get_recursive_roles(Map<String, Object> m);
    public List<Map> get_user_sas_system(Map<String, Object> m);
    public int set_group_chd_role(Map<String, Object> m);
    public int insert_role_perm(Map<String, Object> m);

    // 新增：执行自定义 SQL（只读）
    List<Map> runUserDefinedSql(Map<String, Object> params);
}
