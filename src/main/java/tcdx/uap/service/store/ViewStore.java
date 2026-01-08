package tcdx.uap.service.store;

import com.alibaba.fastjson.JSON;
import lombok.Getter;
import org.springframework.transaction.annotation.Transactional;
import tcdx.uap.common.utils.Lutils;
import tcdx.uap.service.BaseDBService;
import tcdx.uap.service.BusinessService;

import java.util.*;
import java.util.stream.Collectors;

public class ViewStore {

    @Getter
    private static final ViewStore instance = new ViewStore();

    private ViewStore()
    {
        System.out.println("单例模式初始化权限对象仓库");
    }

    private BaseDBService db;
    private BusinessService bs;
    private Map<Object, List> viewRoleStore = new HashMap(); ;
    private Map<Object, Map> viewStore = new HashMap(); ;

    public synchronized void InitAll(BaseDBService db, BusinessService bs){
        this.db = db;
        this.bs = bs;
        InitAll();
    }

    public synchronized void InitAll(){
        List<Map> v_view = db.selectByCauses("v_view", Lutils.genMap(), Lutils.genMap("ord", "asc"));
        //菜单对应角色
        List<Map> roleMenus = db.selectEq("v_role_perm_obj", Lutils.genMap("obj_type", "view"));
        //获得权限
        for(Map view : v_view){
            List<Map> mRoles = roleMenus.stream().filter(r-> Objects.equals(r.get("obj_id"), view.get("id"))).collect(Collectors.toList());
            List<Integer> roles = mRoles.stream().map(r-> (Integer)r.get("role_id")).collect(Collectors.toList());
            viewRoleStore.put(view.get("id"),roles);
            viewStore.put(view.get("id"), view);
        }
    }


    public void set(Integer view_id){
        List<Map> v_view = db.selectEq("v_view", Lutils.genMap("id", view_id));
        //菜单对应角色
        List<Map> roleMenus = db.selectEq("v_role_perm_obj", Lutils.genMap("obj_type", "view", "obj_id", view_id));
        //获得权限
        for(Map view : v_view){
            List<Map> mRoles = roleMenus.stream().filter(r-> Objects.equals(r.get("obj_id"), view.get("id"))).collect(Collectors.toList());
            List<Integer> roles = mRoles.stream().map(r-> (Integer)r.get("role_id")).collect(Collectors.toList());
            viewRoleStore.put(view.get("id"), roles);
            viewStore.put(view.get("id"), view);
        }
    }

    /**
     * 装配视图，如果是组件，则将组件的属性装配进来
     * */
    public Map getSimplify(Object view_id){
        if(view_id==null) return null;
        if(!viewStore.containsKey(view_id))
            set((Integer)view_id);
        Map view = viewStore.get(view_id);
        if(view==null)
            return null;
        if(Objects.equals(view.get("view_type"),"comp")){
            if(Objects.equals(view.get("comp_name"), "CompDataSource")){
                //浅层获取视图，防止嵌套错误
                Map compDataSource = DSStore.getInstance().getSimplify("view"+view.get("id"));
                view.put("compDataSource_id", compDataSource!=null?compDataSource.get("id"):null);
                view.put("compDataSource_table_id", compDataSource!=null?compDataSource.get("table_id"):null);
            }
            else if(Objects.equals(view.get("comp_name"), "CompGrid")){
                Map comp = CompGridStore.getInstance().getSimplify("view"+view.get("id"));
                if(comp!=null) {
                    Map compDataSource =  DSStore.getInstance().getSimplify("comp_grid" +comp.get("id"));
                    view.put("compDataSource_id", compDataSource!=null?compDataSource.get("id"):null);
                    view.put("compDataSource_table_id", compDataSource!=null?compDataSource.get("table_id"):null);
                }
            }
            else if(Objects.equals(view.get("comp_name"), "CompCarousel")){
                Map comp = CompCarouselStore.getInstance().getSimplify("view"+view.get("id"));
                if(comp!=null) {
                    Map compDataSource =  DSStore.getInstance().getSimplify("comp_carousel" +comp.get("id"));
                    view.put("compDataSource_id", compDataSource!=null?compDataSource.get("id"):null);
                    view.put("compDataSource_table_id", compDataSource!=null?compDataSource.get("table_id"):null);
                }
            }
        }
        return view;
    }

    //根据用户角色，获取有权限的视图
    public boolean HasViewPermission(Object view_id, List<Integer> userRoleIds){
        if(view_id==null) return false;
        List<Integer> roles = (List<Integer>) viewRoleStore.get(view_id);
        if(roles!=null ) {
            List<Integer> intersection = roles.stream().filter(userRoleIds::contains).collect(Collectors.toList());
            return !intersection.isEmpty();
        }
        return false;
    }

    public Map get(Object view_id){
        if(view_id==null) return null;
        if(!viewStore.containsKey(view_id))
            set((Integer)view_id);
        Map view = viewStore.get(view_id);
        if(view==null)
            return null;
        if(Objects.equals(view.get("view_type"),"comp")){
            if(Objects.equals(view.get("comp_name"), "CompDataSource")){
                //浅层获取视图，防止嵌套错误
                Map compDataSource = DSStore.getInstance().getSimplify("view"+view.get("id"));
                view.put("compDataSource_id", compDataSource!=null?compDataSource.get("id"):null);
                view.put("compDataSource_table_id", compDataSource!=null?compDataSource.get("table_id"):null);
            }
            else if(Objects.equals(view.get("comp_name"), "CompGrid")){
                Map comp = CompGridStore.getInstance().getSimplify("view"+view.get("id"));
                if(comp!=null) {
                    Map compDataSource =  DSStore.getInstance().getSimplify("comp_grid" +comp.get("id"));
                    view.put("compDataSource_id", compDataSource!=null?compDataSource.get("id"):null);
                    view.put("compDataSource_table_id", compDataSource!=null?compDataSource.get("table_id"):null);
                }
            }
            else if(Objects.equals(view.get("comp_name"), "CompCarousel")){
                Map comp = CompCarouselStore.getInstance().getSimplify("view"+view.get("id"));
                if(comp!=null) {
                    Map compDataSource =  DSStore.getInstance().getSimplify("comp_carousel" +comp.get("id"));
                    view.put("compDataSource_id", compDataSource!=null?compDataSource.get("id"):null);
                    view.put("compDataSource_table_id", compDataSource!=null?compDataSource.get("table_id"):null);
                }
            }
        }
        return view;
    }

    @Transactional
    public void copyTo(Integer view_id, Integer parent_id){
        List<Map> views = bs.getViewsFromParent(view_id);
        //复制并构建属性上下级关系
        List<Map> roots = views.stream().filter(
                o->o.get("view_type").equals("indexTab")
                ||o.get("view_type").equals("modal")
                ||o.get("view_type").equals("drawer")
            ).collect(Collectors.toList());
        if(!roots.isEmpty()){
            Map node = roots.get(0);
            node.put("parent_id", parent_id);
            createTree(node, views);
        }
    }

    public Object createTree(Map node, List<Map> views){
        Map data = (Map)JSON.parse(JSON.toJSONString(node));
        data.remove("id");
        data.remove("tableObj");
        data.remove("compDataSource_id");
        data.remove("compDataSource_table_id");
        Map newNode = db.insertMapRetRow("v_view", data);
        //复制组件信息
        if(node.get("comp_name").equals("CompDataSource")){
            DSStore.getInstance().copy(node, newNode);
        }

        List<Map> children = views.stream().filter(o->Objects.equals(node.get("id"), o.get("parent_id"))).collect(Collectors.toList());
        for(Map child : children){
            child.put("parent_id", newNode.get("id"));
            createTree(child, views);
        }
        return newNode.get("id");
    }

}
