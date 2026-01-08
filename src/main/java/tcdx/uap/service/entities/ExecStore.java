package tcdx.uap.service.entities;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import tcdx.uap.constant.Constants;
import tcdx.uap.service.store.CompCardStore;
import tcdx.uap.service.store.Modules;

import java.util.*;
import java.util.stream.Collectors;


public class ExecStore {
    @Getter
    private static ExecStore instance = new ExecStore();

    private ExecStore()  {
        System.out.println("单例模式初始化ExecStore");
    }
    public Map<String,Exec> store = new HashMap<>();

    public void set(Exec exec) {
        if(exec!=null) {
            exec.setOpInfo();
            store.put(exec.id, exec);
        }
    }

    public Exec get(String id) {
        return store.get(id);
    }

    public void setCompExec(Object obj){
        if(obj instanceof View){
            View view = (View)obj;
            for(Exec exec:view.viewBtns){
                set(exec);
            }
            for(Exec exec:view.viewTitleBtns){
                set(exec);
            }
            if(Objects.equals(view.view_type,"comp")){
                Object comp = Modules.getInstance().get(view.comp_id, false);
                setCompExec(comp);
            }
        }
        else if(obj instanceof CompGrid){
            CompGrid comp = (CompGrid)obj;
            for(Exec exec:comp.topBtns){
                set(exec);
            }
            //设置列
            for(CompGridCol col: comp.gridCols){
                //列点击事件
                set(col.exec);
                for(Exec exec: col.btns){
                    //列的按钮
                    set(exec);
                }
                //列的渲染器点击事件
                set(col.compValueRender.exec);
            }
        }
        else if(obj instanceof CompCard){
            CompCard compCard = (CompCard)obj;
            set(compCard.exec);
        }
        else if(obj instanceof CompCountAggr){
            CompCountAggr comp = (CompCountAggr)obj;
            set(comp.exec);
        }
        else if(obj instanceof CompCarousel){
            CompCarousel comp = (CompCarousel)obj;
            set(comp.exec);
        }
    }
}
