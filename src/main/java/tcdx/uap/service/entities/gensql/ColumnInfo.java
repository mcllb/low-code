package tcdx.uap.service.entities.gensql;

import lombok.Data;

// 列信息
@Data
public class ColumnInfo {
    private String columnName;
    private String dataType;
    private boolean isPrimaryKey;
    private boolean isForeignKey;
}
