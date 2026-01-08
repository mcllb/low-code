package tcdx.uap.service.entities.gensql;

import lombok.Data;

@Data
public class TableJoinInfo {
    private final String tableName;
    private final String alias;
    private final boolean mainTable;
}
