package tcdx.uap.service.entities;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import tcdx.uap.common.utils.Lutils;
import tcdx.uap.constant.Constants;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
@AllArgsConstructor
@NoArgsConstructor
public class UserScope implements Serializable {
    public Boolean is_defined_sql = false;
    public String sql_str = "";
    public List roles = new ArrayList<>();
    public List groups = new ArrayList<>();
    public UserScope(Map map){
        this.is_defined_sql = Lutils.nvl(map.get("is_defined_sql"),false);
        this.sql_str = (String) map.get("sql_str");
        this.roles = (List) map.get("roles");
        this.groups = (List) map.get("groups");
    }

}

