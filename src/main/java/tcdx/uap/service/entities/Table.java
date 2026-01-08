package tcdx.uap.service.entities;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import tcdx.uap.common.utils.Lutils;
import tcdx.uap.constant.Constants;
import tcdx.uap.service.store.Modules;

import java.io.Serializable;
import java.util.*;
import java.util.stream.Collectors;

@JsonIgnoreProperties(ignoreUnknown = true)
@AllArgsConstructor
@NoArgsConstructor
public class Table implements Serializable {
    public String id;
    public String name;
    public String table_name;
    public List<TableCol> cols = new ArrayList<>();
    public List<String> priTableIds = new ArrayList<>();
    public List<String> subTableIds = new ArrayList<>();
    //流程相关
    public List<FlowNode> nodes = new ArrayList<>();
    public List<FlowEdge> edges = new ArrayList<>();
    //深拷贝时获取
    public List<TableSimple> priTableObjs = new ArrayList<>();
    public List<TableSimple> subTableObjs = new ArrayList<>();

    public Table(Map map){
        String rndId = Constants.getTimeFormatId();
        this.id = "table"+rndId;
        this.name = (String) map.get("name");
        this.table_name = "z_table"+ rndId;
    }

    public void setDeepInfo(){
        subTableObjs = new ArrayList<>();
        priTableObjs = new ArrayList<>();
        //获取主表子表关系
        for (String tid : priTableIds) {
            Table priTable = (Table) Modules.getInstance().get(tid, false);
            TableSimple ts = Lutils.ObjToClass(priTable, TableSimple.class);
            priTableObjs.add(ts);
        }
        //获取主表子表关系
        for (String subTable : subTableIds) {
            Table fd = (Table) Modules.getInstance().get(subTable, false);
            TableSimple ts = Lutils.ObjToClass(fd, TableSimple.class);
            subTableObjs.add(ts);
        }
        //节点与边的信息
        setNode_inEdges_outEdges_events();
        setEdge_srcLabel_dstLabel();
    }

    public void setCol(String id, TableCol col){
        for(int i=0; i<cols.size(); i++){
            if(cols.get(i).id.equals(id)){
                cols.set(i,col);
            }
        }
    }

    public void removeColById(String col_id){
        cols = cols.stream().filter(c->!c.id.equals(col_id)).collect(Collectors.toList());
    }


    public void removeColByField(String field){
        cols = cols.stream().filter(c->!c.field.equals(field)).collect(Collectors.toList());
    }

    //根据table_col_id或field查找
    public TableCol getCol(String query){
        for(int i=0; i<cols.size(); i++){
            if(Objects.equals(cols.get(i).id,query)||Objects.equals(cols.get(i).field,query)){
                return cols.get(i);
            }
        }
        return null;
    }


    public void removeCol(String rmv_id){
        cols = cols.stream().filter(c->!c.id.equals(rmv_id)).collect(Collectors.toList());
    }


    public FlowNode getNode(String id){
        List<FlowNode> nodes = this.nodes.stream().filter(n->n.id.equals(id)).collect(Collectors.toList());
        if(nodes.size()>0){
            return nodes.get(0);
        }
        else
            return null;
    }


    public FlowEdge getEdge(String id){
        List<FlowEdge> edges = this.edges.stream().filter(e->e.id.equals(id)).collect(Collectors.toList());
        if(edges.size()>0){
            return edges.get(0);
        }
        else
            return null;
    }

    public void setNode_inEdges_outEdges_events(){
        for(FlowNode node : nodes){
            node.inEdges = edges.stream().filter(e->e.dst.equals(node.id)).collect(Collectors.toList());
            Collections.sort(node.inEdges);
            node.outEdges = edges.stream().filter(e->e.src.equals(node.id)).collect(Collectors.toList());
            Collections.sort(node.outEdges);
            for(FlowNodeEvent event: node.events){
//                event.priNodeLabel = ((Table) Modules.getInstance().get(event.pri_table_id, false)).getNode(event.pri_node).label;
//                event.subNodeLabel = ((Table)ModuleStore.getInstance().get(event.sub_table_id, false)).getNode(event.when_sub_node).label;
//                event.priTableName = ((Table) Modules.getInstance().get(event.pri_table_id, false)).name;
//                event.subTableName = ((Table)ModuleStore.getInstance().get(event.sub_table_id, false)).name;
            }
        }
    }

    public void setEdge_srcLabel_dstLabel(){
        System.out.println(this.id+this.name);
        for(FlowEdge edge : edges){
            FlowNode src = this.getNode(edge.src);
            FlowNode dst = this.getNode(edge.dst);
            edge.srcLabel = src!=null?src.label:"";
            edge.dstLabel = dst!=null?dst.label:"";
            edge.srcType = src!=null?src.type:"";
            edge.dstType = dst!=null?dst.type:"";
        }
    }

//    public List<Map> getSubPriMap(){
//        List<Map> priIds = new ArrayList<>();
//        if(priTableIds.size()>0){
//            for(String pid: priTableIds){
//                priIds.add(Lutils.genMap("sid",id,"pid", pid));
//                Table ptbl = (Table)Modules.getInstance().get(pid, false);
//                if(ptbl!=null&&ptbl.priTableIds.size()>0){
//                    for(String pid2: priTableIds){
//                        priIds.add(Lutils.genMap("sid",pid,"pid", pid2));
//                        Table ptbl2 = (Table)Modules.getInstance().get(pid2, false);
//                        if(ptbl2!=null&&ptbl2.priTableIds.size()>0){
//                            for(String pid3: priTableIds){
//                                priIds.add(Lutils.genMap("sid",pid2,"pid", pid3));
//                                Table ptbl3 = (Table)Modules.getInstance().get(pid3, false);
//                                if(ptbl3!=null&&ptbl3.priTableIds.size()>0){
//                                    for(String pid4: priTableIds){
//                                        priIds.add(Lutils.genMap("sid",pid3,"pid", pid4));
//                                        Table ptbl4 = (Table)Modules.getInstance().get(pid4, false);
//                                        if(ptbl4!=null&&ptbl4.priTableIds.size()>0){
//                                            for(String pid5: priTableIds){
//                                                priIds.add(Lutils.genMap("sid",pid4,"pid", pid5));
//                                            }
//                                        }
//                                    }
//                                }
//                            }
//                        }
//                    }
//                }
//            }
//        }
//        return priIds;
//    }

    public List<Map> getSubPriMap() {
        List<Map> priIds = new ArrayList<>();
        if (priTableIds == null || priTableIds.isEmpty()) return priIds;

        Set<String> seenEdges = new LinkedHashSet<>(); // 全局去重，保证不会重复加入
        final int maxDepth = 128;

        for (String pid : priTableIds) {
            // 先加入根到第一层的正向边
            addEdgeOnce(priIds, seenEdges, id, pid);

            // ⭐ 关键：把根 id（例如 table43）预先放入“当前路径”
            LinkedHashSet<String> path = new LinkedHashSet<>();
            path.add(id);

            // 从第一层开始下钻
            dfsCollect(priIds, pid, path, seenEdges, 1, maxDepth);
        }
        return priIds;
    }

    private void dfsCollect(List<Map> priIds,
                            String currentSid,
                            Set<String> path,          // 当前路径（祖先集合）
                            Set<String> seenEdges,
                            int depth,
                            int maxDepth) {

        if (depth > maxDepth) return;

        // 把当前节点加入路径；如果已在路径，说明出现环，直接返回
        if (!path.add(currentSid)) return;

        Table table = (Table) Modules.getInstance().get(currentSid, false);
        if (table == null || table.priTableIds == null || table.priTableIds.isEmpty()) {
            path.remove(currentSid);
            return;
        }

        for (String pid : table.priTableIds) {
            if (pid == null || pid.isEmpty() || pid.equals(currentSid)) continue;

            // ⭐ 如果指向的是“祖先”，这是回边（如 table44 -> table43），跳过，不加入
            if (path.contains(pid)) {
                // 可选：log.debug("skip back-edge {} -> {}", currentSid, pid);
                continue;
            }

            addEdgeOnce(priIds, seenEdges, currentSid, pid);
            dfsCollect(priIds, pid, path, seenEdges, depth + 1, maxDepth);
        }

        // 回溯
        path.remove(currentSid);
    }

    private void addEdgeOnce(List<Map> priIds,
                             Set<String> seenEdges,
                             String sid, String pid) {
        String edgeKey = sid + "->" + pid;
        if (seenEdges.add(edgeKey)) {
            priIds.add(Lutils.genMap("sid", sid, "pid", pid));
        }
    }


    public FlowEdge getFirstEdgeOfNode(String node_id){
        List<FlowEdge> edges = this.edges.stream()
                .filter(e->Objects.equals(e.src,node_id))
                .collect(Collectors.toList());
        Collections.sort(edges);
        if(edges.size()>0){
            return edges.get(0);
        }
        else
            return null;
    }

    public List<Map> listUpDownRel(){
        String table_id = this.id;
        List<Map> rel_tables = new ArrayList<>();
        rel_tables.add(Lutils.genMap(
                "id", table_id,
                "table_id", table_id,
                "op_table", true,  //当前操作表
                "name", name,
                "parent_id", priTableIds.size()>0?priTableIds.get(0):"-1"));
        //取上级表和下级表
        for(String ptblid: priTableIds){
            Table ptbl = (Table) Modules.getInstance().get(ptblid, false);
            if (rel_tables.stream().filter(o -> Objects.equals(ptblid, o.get("id"))).collect(Collectors.toList()).isEmpty()) {
                rel_tables.add(Lutils.genMap(
                        "id", ptblid,
                        "table_id", ptblid,
                        "op_table", false,  //当前操作表
                        "name", ptbl.name,
                        "parent_id", ptbl.priTableIds.size() > 0 ? ptbl.priTableIds.get(0) : "-1"));
                //取上级表和下级表
                for (String ptblid2 : ptbl.priTableIds) {
                    Table ptbl2 = (Table) Modules.getInstance().get(ptblid2, false);
                    if (rel_tables.stream().filter(o -> Objects.equals(ptblid2, o.get("id"))).collect(Collectors.toList()).isEmpty()) {
                        rel_tables.add(Lutils.genMap(
                                "id", ptblid2,
                                "table_id", ptblid2,
                                "op_table", false,  //当前操作表
                                "name", ptbl2.name,
                                "parent_id", ptbl2.priTableIds.size() > 0 ? ptbl2.priTableIds.get(0) : "-1"));
                    }
                }
            }
        }
        List<String> flist = rel_tables.stream().map(o->(String)o.get("table_id")).collect(Collectors.toList());
        //取所有下级节点
        for(String ftid: flist){
            Table ftbl = (Table) Modules.getInstance().get(ftid, false);
            //添加下级
            for(String stid: ftbl.subTableIds) {
                Table stbl = (Table) Modules.getInstance().get(stid, false);
                if (rel_tables.stream().filter(o -> Objects.equals(stid, o.get("id"))).collect(Collectors.toList()).isEmpty()) {
                    rel_tables.add(Lutils.genMap(
                            "id", stid,
                            "table_id", stid,
                            "op_table", false,  //当前操作表
                            "name", stbl.name,
                            "parent_id", ftid));
                }
                for(String stid2: stbl.subTableIds) {
                    Table stbl2 = (Table) Modules.getInstance().get(stid2, false);
                    if (rel_tables.stream().filter(o -> Objects.equals(stid2, o.get("id"))).collect(Collectors.toList()).isEmpty()) {
                        rel_tables.add(Lutils.genMap(
                                "id", stid2,
                                "table_id", stid2,
                                "op_table", false,  //当前操作表
                                "name", stbl2.name,
                                "parent_id", stid));
                    }
                    for(String stid3: stbl2.subTableIds) {
                        Table stbl3 = (Table) Modules.getInstance().get(stid3, false);
                        if (rel_tables.stream().filter(o -> Objects.equals(stid3, o.get("id"))).collect(Collectors.toList()).isEmpty()) {
                            rel_tables.add(Lutils.genMap(
                                    "id", stid3,
                                    "table_id", stid3,
                                    "op_table", false,  //当前操作表
                                    "name", stbl3.name,
                                    "parent_id", stid2));
                        }
                        for (String stid4 : stbl3.subTableIds) {
                            Table stbl4 = (Table) Modules.getInstance().get(stid4, false);
                            if (rel_tables.stream().filter(o -> Objects.equals(stid4, o.get("id"))).collect(Collectors.toList()).isEmpty()) {
                                rel_tables.add(Lutils.genMap(
                                        "id", stid4,
                                        "table_id", stid4,
                                        "op_table", false,  //当前操作表
                                        "name", stbl4.name,
                                        "parent_id", stid3));
                            }
                        }
                    }
                }
            }
        }
        return rel_tables;
    }
}
