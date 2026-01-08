package tcdx.uap.service.vo;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class GenerateModuleReq {
    /**
     * 分组名称
     */
    private String groupName;
    /**
     * 视图名称
     */
    private String viewName;
    /**
     * 数据表名称
     */
    private String tableName;
    /**
     * 子表新增表单生成
     */
    private Boolean subAdd;

    /**
     * 字段配置
     */
    private List<TableColInfoResp> mainTableFields;

    private List<TableColInfoResp> priTableFields;

    private Map<String, List<TableColInfoResp>> subTableFields;
}
