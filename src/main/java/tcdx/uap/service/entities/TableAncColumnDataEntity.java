package tcdx.uap.service.entities;

import java.util.List;

public class TableAncColumnDataEntity {
    private String table_name;
    private List<String> update_column_list;
    private List<Integer> idList;

    public TableAncColumnDataEntity(String table_name, List<String> update_column_list, List<Integer> idList) {
        this.table_name = table_name;
        this.update_column_list = update_column_list;
        this.idList = idList;
    }

    public String getTable_name() {
        return table_name;
    }

    public void setTable_name(String table_name) {
        this.table_name = table_name;
    }

    public List<String> getUpdate_column_list() {
        return update_column_list;
    }

    public void setUpdate_column_list(List<String> update_column_list) {
        this.update_column_list = update_column_list;
    }

    public List<Integer> getIdList() {
        return idList;
    }

    public void setIdList(List<Integer> idList) {
        this.idList = idList;
    }
}
