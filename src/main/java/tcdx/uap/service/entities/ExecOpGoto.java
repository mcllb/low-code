package tcdx.uap.service.entities;

//import com.aspose.cad.internal.P.M;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import tcdx.uap.service.store.Modules;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
@AllArgsConstructor
@NoArgsConstructor
public class ExecOpGoto implements Serializable {
    public String goto_op_id; //stop op20251207232607111
    public String type;  //any   script
    public String script;
}
