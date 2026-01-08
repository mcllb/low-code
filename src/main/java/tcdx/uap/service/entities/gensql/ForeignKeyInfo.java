package tcdx.uap.service.entities.gensql;

import lombok.Data;

// 外键信息
@Data
public class ForeignKeyInfo {
    private String columnName;
    private String referencedTable;
    private String referencedColumn;
}
