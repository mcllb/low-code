package tcdx.uap.service.entities;

//import com.aspose.cad.internal.P.M;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
@AllArgsConstructor
@NoArgsConstructor
public class ExecResult implements Serializable {
    public boolean state = true;
    public List<ExecOpResult> results = new ArrayList<>();

    public void addResult(ExecOpResult r) {
        state = state && r.state;
        results.add(r);
    }
}
