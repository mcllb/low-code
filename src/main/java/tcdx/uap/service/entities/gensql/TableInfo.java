package tcdx.uap.service.entities.gensql;

import lombok.Data;

import java.util.List;

// 表信息
@Data
public class TableInfo {
    private String tableName;
    private List<ColumnInfo> columns;
    private List<ForeignKeyInfo> foreignKeys;
}
