package tcdx.uap.service.store;

import lombok.Getter;
import tcdx.uap.common.utils.Lutils;
import tcdx.uap.common.utils.SqlUtil;
import tcdx.uap.service.BaseDBService;
import tcdx.uap.service.ServiceConfigService;

import java.util.*;

public class CompGridStore {

    @Getter
    private static CompGridStore instance = new CompGridStore();

    private CompGridStore()
    {
        System.out.println("单例模式初始化CompGridStore");
    }

    private final Map<Object, Map> store = new HashMap();

    BaseDBService db;
    ServiceConfigService cfgService;
    public void InitAll(BaseDBService db, ServiceConfigService cfgService){
        this.db = db;
        this.cfgService = cfgService;
        InitAll();
    }

    public void InitAll(){
        List<Map> v_comp_grid = db.selectByCauses("v_comp_grid", null, null);
        List<Map> v_comp_grid_col = db.selectByCauses("v_comp_grid_col", null, Lutils.genMap("ord","asc"));
        assemble(v_comp_grid, v_comp_grid_col);
    }

    public void Init(String obj_type, Integer obj_id){

    }

    public void set(Object comp_id){
        store.remove(comp_id);
        List<Map> v_comp_grid = db.selectEq("v_comp_grid", Lutils.genMap("id", comp_id));
        List<Map> v_comp_grid_col = db.selectByCauses("v_comp_grid_col", SqlUtil.eq("comp_id", comp_id), Lutils.genMap("ord","asc"));
        assemble(v_comp_grid, v_comp_grid_col);
    }

    public void assemble(List<Map> v_comp_grid,List<Map> v_comp_grid_col){
        for(Map comp : v_comp_grid){
            comp.put("gridCols", new ArrayList<Map>());
            store.put(comp.get("id"), comp);
            store.put((String)comp.get("obj_type")+comp.get("obj_id"), comp);
        }
        for(Map col : v_comp_grid_col){
            Map grid = store.get(col.get("comp_id"));
            if(grid !=null) {
                List<Map> gridCols = (List) grid.get("gridCols");
                gridCols.add(col);
            }
        }
    }

    /**
     * 浅获取，即获取基本属性，不深入嵌套获取按钮等动作。用于有按钮的组件方法中，防止嵌套错误
     * */
    public Map getSimplify(Object key){
        Map comp = store.get(key);
        if(comp==null)
            return null;
        Integer comp_id = (Integer) comp.get("id");
        //获取表格列，含渲染器
//        List<Map> gridCols = (List<Map>) comp.get("gridCols");
//        for (Map col : gridCols) {
//            Map render = CompValueRenderStore.getInstance().get("comp_grid_col"+col.get("id"));
//            if(render!=null)
//                col.put("CompValueRender", render);
//        }
        //获取数据源，含数据源列
        comp.put("compDataSource", DSStore.getInstance().getSimplify("comp_grid"+comp_id));
        /** 需要优化 */
//        List<Map> searchGridCols = cfgService.getSearchGridCols(gridCols, comp_id);
//        comp.put("searchGridCols", searchGridCols);
        //添加表格组件中的按钮

//        //将列的按钮，添加到关联的列中
//        for (Map col : gridCols) {
//            col.put("btns", ExecStore.getInstance().getByObj("comp_grid_col_btn"+col.get("id")));
//        }
        //提取出顶部按钮
//        comp.put("topBtns", ExecStore.getInstance().getByObj("comp_grid_top_btn"+comp.get("id")));
        return comp;
    }


    public Map get(Object key){
        Map comp = store.get(key);
        if(comp==null)
            return null;
        Integer comp_id = (Integer) comp.get("id");
        //获取表格列，含渲染器
        List<Map> gridCols = (List<Map>) comp.get("gridCols");
        for (Map col : gridCols) {
            Map render = CompValueRenderStore.getInstance().get("comp_grid_col"+col.get("id"));
            if(render!=null)
                col.put("compValueRender", render);
        }
        //获取数据源，含数据源列
        comp.put("compDataSource", DSStore.getInstance().get("comp_grid"+comp_id));
        /** 需要优化 */
        List<Map> searchGridCols = cfgService.getSearchGridCols(gridCols,comp_id);
        comp.put("searchGridCols", searchGridCols);
        //添加表格组件中的按钮

        //将列的按钮，添加到关联的列中
        for (Map col : gridCols) {
            col.put("btns", ExecObjStore.getInstance().getByObj("comp_grid_col_btn"+col.get("id")));
            col.put("click_execs", ExecObjStore.getInstance().getByObj("comp_grid_col_cell_click"+col.get("id")));
        }
        //提取出顶部按钮
        comp.put("topBtns", ExecObjStore.getInstance().getByObj("comp_grid_top_btn"+comp.get("id")));
        return comp;
    }

}
