package tcdx.uap.service.entities;

import tcdx.uap.common.utils.Lutils;
import tcdx.uap.service.BaseDBService;

import javax.servlet.http.HttpSession;
import java.util.*;
import java.util.stream.Collectors;

public class UserAction {
    public Integer id;
    public Integer user_id;
    public String staff_nm;
    public String exec_id;
    public String action_type;
    public String op_id;
    public Date action_time;
    public List<Integer> userGroupIds;
    public List<Integer> userRoleIds;
    public BaseDBService db;

    public UserAction(){}

    public void setUserInfo(HttpSession httpSession){
        this.user_id = (Integer)httpSession.getAttribute("userId");
        this.staff_nm = Lutils.nvl(httpSession.getAttribute("userName"),"");
        this.userGroupIds = (List)httpSession.getAttribute("userGroupIds");
        this.userRoleIds = (List)httpSession.getAttribute("userRoleIds");
        if(this.user_id == null){
            this.user_id = 879;
            this.staff_nm = "系统管理员";
            this.userGroupIds = new ArrayList<Integer>();
            this.userRoleIds  = new ArrayList<Integer>();
            this.userGroupIds.add(40);
            this.userRoleIds.add(35);
        }
    }

    public Map getMap(){
        return Lutils.genMap("user_id", this.user_id, "staff_nm", this.staff_nm,
                "role_ids", this.userRoleIds, "group_ids", this.userGroupIds);
    }

    public UserAction(String exec_id, BaseDBService baseDBService) {
        this.exec_id = exec_id;
        this.action_time = new Date();
        this.db = baseDBService;
    }

    public void saveToDB(){
        Map re = db.insertMapRetRow("v_user_action",
                Lutils.genMap(
                        "user_id", this.user_id,
                        "exec_id", this.exec_id,
                        "action_type", this.action_type,
                        "action_time", this.action_time
                ));
        this.id = (Integer)re.get("id");
    }

}

