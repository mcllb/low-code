package tcdx.uap.service.store;

import lombok.Getter;
import tcdx.uap.common.utils.Lutils;
import tcdx.uap.common.utils.SqlUtil;
import tcdx.uap.mapper.ServiceConfigMapper;
import tcdx.uap.service.BaseDBService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class CompValueEditorStore {


    @Getter
    private static CompValueEditorStore instance = new CompValueEditorStore();

    private CompValueEditorStore()
    {
        System.out.println("单例模式实现一-饿汉式");
    }

    private Map<Object,Map> store = new HashMap();
    private Map<Object,List> storeByDatasourceField = new HashMap();
    BaseDBService db;
    ServiceConfigMapper cfgMapper;
    public void InitAll(BaseDBService db,ServiceConfigMapper cfgMapper){
        this.db = db;
        this.cfgMapper = cfgMapper;
        InitAll();
    }

    public void InitAll(){
        List<Map> v_comp_value_editor = db.selectByCauses("v_comp_value_editor", null, null);
        assemble(v_comp_value_editor);
    }

    public void set(Integer comp_id){
        List<Map> v_comp_value_editor = db.selectByCauses("v_comp_value_editor", SqlUtil.eq("id", comp_id), null);
        assemble(v_comp_value_editor);
    }

    public void assemble(List<Map> v_comp_value_editor){
        for(Map comp : v_comp_value_editor){
            store.put(comp.get("id"), comp);
            store.put((String)comp.get("obj_type")+comp.get("obj_id"), comp);
            //添加到集合中storeByDatasourceField
            Object ds_field_id = comp.get("ds_field_id");
            if(ds_field_id!=null){
                List<Map> editors = storeByDatasourceField.get(ds_field_id);
                if(editors!=null){
                    //如果已存在，则替换
                    boolean fd = false;
                    for(int i=0;i<editors.size();i++){
                        if(Objects.equals(editors.get(i).get("ds_field_id"), ds_field_id)){
                            editors.set(i,comp);
                            fd = true;
                            continue;
                        }
                    }
                    if(!fd)
                        editors.add(comp);
                }
                else{
                    storeByDatasourceField.put(ds_field_id, Lutils.genList(comp));
                }
            }
        }
    }

    public List getByField(Object ds_field_id){
        return storeByDatasourceField.get(ds_field_id);
    }

    /**
     * key 可以是id，也可以是obj_type+id
     * */
    public Map get(Object key){
        Map comp = store.get(key);
        //获取一些属性
        if(comp != null){
            //数据源信息
            Map compDataSource = DSStore.getInstance().get( comp.get("ds_id"));
            if(compDataSource!=null) {
                comp.put("compDataSource_obj_type", compDataSource.get("obj_type"));
                Map field = DSStore.getInstance().getFields((Integer) comp.get("ds_id"), (Integer) comp.get("ds_field_id"));
                //编辑了一半的数据源，会出现字段未配置的情况
                if (field != null) {
                    comp.put("field", field.get("field"));
                }
                if (Objects.equals(comp.get("editor_type"), "location-editor")) {
                    comp.put("latField", DSStore.getInstance().getFields((Integer) comp.get("ds_id"), (Integer) comp.get("lat_ds_field_id")));
                    comp.put("lngField", DSStore.getInstance().getFields((Integer) comp.get("ds_id"), (Integer) comp.get("lng_ds_field_id")));
                }
                if (comp.get("editor_type").equals("foreign-key-editor")) {
                    comp.put("compGrid", CompGridStore.getInstance().get("comp_value_editor" + comp.get("id")));
                }
            }
        }

        return comp;
    }

}
