package tcdx.uap.service.vo;

import lombok.Data;

@Data
public class TableColInfoResp {
    private String colId;
    // 字段名
    private String columnName;
    // 显示名称
    private String displayName;
    // 数据类型
    private String dataType;
    // 渲染类型
    private String renderType;
    // 是否主表
    private Boolean isMainTable = false;
    // 表名
    private String tableName;
    // 表格显示名
    private String tableDisplayName;
    // 是否在表格里展示
    private Boolean showInTable = true;
    // 是否在表格查询里展示
    private Boolean showInQuery = false;
    // 是否在新增表单展示
    private Boolean showInAdd = true;
    // 是否在编辑表单展示
    private Boolean showInEdit = true;
    // 是否在详情表单展示
    private Boolean showInDetail = true;
    // 是否必填
    private Boolean required = false;
    // 默认列宽
    private Integer width = 100;
}
