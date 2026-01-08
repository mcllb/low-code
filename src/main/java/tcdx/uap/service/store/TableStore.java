package tcdx.uap.service.store;

import lombok.Getter;
import tcdx.uap.common.utils.Lutils;
import tcdx.uap.common.utils.SqlUtil;
import tcdx.uap.mapper.ServiceConfigMapper;
import tcdx.uap.service.BaseDBService;
import tcdx.uap.service.entities.FlowEdge;
import tcdx.uap.service.entities.FlowNode;
import tcdx.uap.service.entities.Table;

import java.util.*;
import java.util.stream.Collectors;

public class TableStore {

    @Getter
    private static final TableStore instance = new TableStore();

    private TableStore()
    {
        System.out.println("单例模式初始化权限对象仓库");
    }

    private BaseDBService db;
    private ServiceConfigMapper cfgMapper;

    private Map<Object, Map> tableStore = null;
    private Map<Object, Map> tableColStore = null;


    public synchronized void InitAll(BaseDBService db, ServiceConfigMapper cfg){
        this.db = db;
        this.cfgMapper = cfg;
        InitAll();
    }

    public synchronized void InitAll(){
        tableStore = new HashMap();
        tableColStore = new HashMap();
        List<Map> v_table = db.selectByCauses("v_table", SqlUtil.and(SqlUtil.eq("node_type", "table"),SqlUtil.eq("is_deleted", false)), null);
        //菜单对应角色
        List<Map> v_table_col = db.selectByCauses("v_table_col", null, Lutils.genMap("ord", "asc"));
        List<Map> v_table_rel = cfgMapper.get_table_relations(Lutils.genMap());
        assemble(v_table, v_table_col, v_table_rel);
    }

    public synchronized void set(Integer table_id){
        List<Map> v_table = db.selectByCauses("v_table", SqlUtil.eq("id", table_id), null);
        //菜单对应角色
        List<Map> v_table_col = db.selectByCauses("v_table_col",
                SqlUtil.eq("table_id", table_id),
                Lutils.genMap("ord", "asc"));
        List<Map> v_table_rel = cfgMapper.get_table_relations(Lutils.genMap( "table_id", table_id));
        assemble(v_table, v_table_col, v_table_rel);
    }


    //根据用户角色，获取有权限的菜单
    public void assemble(List<Map> v_table, List<Map> v_table_col, List<Map> v_table_rel){
        for(Map table:v_table){
            table.put("cols", v_table_col.stream().filter(o->Objects.equals(o.get("table_id"), table.get("id"))).collect(Collectors.toList()));
            table.put("priTableIds", v_table_rel.stream().filter(o->Objects.equals(o.get("table_id"), table.get("id"))).map(o->o.get("foreign_table_id")).collect(Collectors.toList()));
            table.put("priTableObjs", v_table_rel.stream()
                    .filter(o->Objects.equals(o.get("table_id"), table.get("id")))
                    .map(o->Lutils.genMap("table_id", o.get("foreign_table_id"), "table_display_name", o.get("foreign_table_display_name")))
                    .collect(Collectors.toList()));
            table.put("subTableIds", v_table_rel.stream().filter(o->Objects.equals(o.get("foreign_table_id"), table.get("id"))).map(o->o.get("table_id")).collect(Collectors.toList()));
            table.put("subTableObjs", v_table_rel.stream()
                    .filter(o->Objects.equals(o.get("foreign_table_id"), table.get("id")))
                    .map(o->Lutils.genMap("table_id", o.get("table_id"), "table_display_name", o.get("table_display_name")))
                    .collect(Collectors.toList()));
            tableStore.put(table.get("id"), table);
        }
        for(Map table_col:v_table_col){
            tableColStore.put(table_col.get("id"), table_col);
        }
    }

    public Map get(Object key){
        return tableStore.get(key);
    }

    public List<Map> getPriTableObjs(Object key){
        Map tbl = tableStore.get(key);
        List<Integer> map = (List)tbl.get("priTableIds");
        List<Map> re = new ArrayList<>();
        for(Integer tid:map){
            Map pri_tbl_obj = TableStore.getInstance().get(tid);
            re.add(Lutils.genMap("id", tid, "table_id", tid, "table_display_name", pri_tbl_obj.get("table_display_name")));
        }
        return re;
    }

    public List<Map> getSubTableObjs(Object key){
        Map tbl = tableStore.get(key);
        List<Integer> tbls = (List)tbl.get("subTableIds");
        List<Map> re = new ArrayList<>();
        for(Integer tid:tbls){
            Map tbl_obj = TableStore.getInstance().get(tid);
            re.add(Lutils.genMap("id", tid, "table_id", tid, "table_display_name", tbl_obj.get("table_display_name")));
        }
        return re;
    }

    public Map getTableCol(Object table_id, Object col_id){
        Map table = tableStore.get(table_id);
        List<Map> cols = (List<Map>) table.get("cols");
        List<Map> findCols = cols.stream().filter(c -> Objects.equals(c.get("id"), col_id)).collect(Collectors.toList());
        if(findCols.size()>0){
            return findCols.get(0);
        }
        return null;
    }

    //根据col_id查询col
    public Map getTableCol(Object key){
        Map col = tableColStore.get(key);
        return col;
    }
}
