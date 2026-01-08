package tcdx.uap.service.entities;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import tcdx.uap.constant.Constants;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;


@JsonIgnoreProperties(ignoreUnknown = true)
@AllArgsConstructor
@NoArgsConstructor
public class CompUserSelector  implements Serializable {
    public String id;
    public String exec_op_id;
    public String user_scope_for;
    public Boolean assign_multi_users;
    public Boolean required;
    public UserScope userScope;

    public Map initData = new HashMap();
    public void create(String id){
        this.id = id;
        userScope = new UserScope();
    }
}
