package tcdx.uap.service.entities;

import java.util.*;
import java.util.stream.Collectors;

public class TodoDataSources {
    List<Map> store = new ArrayList<>();
    public TodoDataSources() {}

    public void add1(Object view_id, Object ds_id, boolean justTotal){
//        = (Boolean)ds.get("justTotal");
        List fd = store.stream().filter(d->d.get("id").equals(ds_id)).collect(Collectors.toList());
        if(fd.size()>0){
            Map dsResult = (Map)fd.get(0);
            boolean innerJustTotal = (Boolean)dsResult.get("justTotal");
            dsResult.put("justTotal", innerJustTotal&&justTotal);
        }
        else{
        }
    }

    public List<Map> getList() {
        return store;
    }
}
