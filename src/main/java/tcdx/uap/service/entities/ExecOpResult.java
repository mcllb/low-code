package tcdx.uap.service.entities;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.*;

@JsonIgnoreProperties(ignoreUnknown = true)
@NoArgsConstructor
public class ExecOpResult implements Serializable {
    public String op_id;
    public int index = 0;
    public String field;
    public boolean state = true;
    public String message;

    public ExecOpResult(String op_id, int index, boolean state, String msg) {
        this.op_id = op_id;
        this.state = state;
        this.message = msg;
        this.index = index;
    }

    public ExecOpResult(String op_id, int index, boolean state, String msg,String field) {
        this.op_id = op_id;
        this.state = state;
        this.message = msg;
        this.index = index;
        this.field = field;
    }
}
