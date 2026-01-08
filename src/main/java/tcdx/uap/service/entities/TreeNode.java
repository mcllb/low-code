package tcdx.uap.service.entities;

import tcdx.uap.common.utils.Lutils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class TreeNode{
    public int id;
    public int pid;
    private String idField;
    private String pidField;
    private Map data;
    TreeNode parentNode = null;
    private List<TreeNode> children;
 
    public TreeNode(Map data,String idField,String pidField) {
        this.id = (Integer)data.get(idField);
        this.pid = (Integer)data.get(pidField);
        this.data = data;
        this.children = new ArrayList<TreeNode>();
    }



    public Map getData() {
        return this.data;
    }
 
    public void setData(Map data) {
        this.data = Lutils.copyMap(data);
    }
 
    public List<TreeNode> getChildren() {
        return children;
    }
 


    public TreeNode findTreeNode(int id,List<TreeNode> list){
        for(TreeNode node : list){
            if(node.id==id)
                return node;
            else {
                TreeNode fd = findTreeNode(id, node.children);
                if(fd!=null)
                    return fd;
            }
        }
        return null;
    }

    public static List<TreeNode> createTree(List<Map> list, String idField, String pidField){
        if(list==null|| list.isEmpty())
            return null;
        List<TreeNode> tree = new ArrayList<>();
        for(Map map : list){
            tree.add(new TreeNode(map,idField,pidField));
        }
        for (TreeNode treeNode : tree) {
            TreeNode pNode = treeNode.findTreeNode(treeNode.pid, tree);
            if (pNode != null) {
                treeNode.parentNode = pNode;
                pNode.children.add(treeNode);
            }
        }

        return tree;
    }

}