package tcdx.uap.service.entities;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import tcdx.uap.constant.Constants;

import java.io.Serializable;
import java.util.*;
import java.util.stream.Collectors;

@JsonIgnoreProperties(ignoreUnknown = true)
@AllArgsConstructor
@NoArgsConstructor
public class Exec implements Serializable {
    public String id;
    public String name;
    public String style;
    public String type;
    public String icon;
    public String size;
    public Boolean plain;
    public Boolean round;
    public Boolean circle;
    public String show_in_session_nodes;
    public String defined_session;
    public List<ExecOp> ops = new ArrayList<>();
    public Map<String,String> alias = new HashMap<>();
    public void create(){
        id= Constants.getTimeFormatId();
        ops = new ArrayList<>();
    }

    public void setOpInfo(){
        for(ExecOp op : ops){
            op.setInfo();
        }
    }
}
