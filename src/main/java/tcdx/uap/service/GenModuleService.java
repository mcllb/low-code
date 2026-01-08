package tcdx.uap.service;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tcdx.uap.common.utils.Lutils;
import tcdx.uap.common.utils.StringUtils;
import tcdx.uap.constant.Constants;
import tcdx.uap.mapper.BusinessMapper;
import tcdx.uap.service.entities.*;
import tcdx.uap.service.store.Modules;
import tcdx.uap.service.vo.GenerateModuleReq;
import tcdx.uap.service.vo.TableColInfoResp;

import javax.annotation.Resource;
import javax.sql.DataSource;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class GenModuleService {

    @Resource
    private BaseDBService baseDBService;

    @Autowired
    private DataSource dataSource;

    @Resource
    private GenSqlService genSqlService;

    @Resource
    private BusinessMapper businessMapper;

    @Transactional
    public void generateModule(GenerateModuleReq req) throws Exception {
        req.setGroupName(req.getViewName());
        // 创建分组
        String groupId = createGroup(req.getGroupName());

        /* ===============   表格     =============== */
        // 创建窗口
        String viewId = createWindow(req.getViewName(), groupId, "indexTab");
        // 创建标签
        String tabViewId = createView(viewId, "tabs", "tabs");
        // 创建表格
        String tableViewId = createView(tabViewId, req.getViewName(), "comp");
        String tableCompId = createComp(viewId, tableViewId, "CompGrid", req.getViewName());
        // 刷新
        Modules.getInstance().InitAll();
        // 更新数据源
        updateDataSource(req.getTableName(), tableCompId, "all", "defined", CompGrid.class, false, false, req.getTableName(), null);

        /* ===============  新增表单  =============== */
        // 创建窗口
        String addViewId = createWindow(req.getViewName() + "新增", groupId, "modal");
        // 创建数据源
//        String dataSourceViewId = createView(addViewId, "数据源", "comp");
//        String dataSourceCompId = createComp(addViewId, dataSourceViewId, "CompDataSource", "数据源");
        // 左 视图ID 右 数据源ID
        ImmutablePair<String, String> addDataSourcePair = createDataSource(addViewId);
        // 刷新
        Modules.getInstance().InitAll();
        // 更新数据源
        updateDataSource(req.getTableName(), addDataSourcePair.getRight(), "none", "table", CompDataSource.class, false, false, req.getTableName(), addViewId);
        // 生成表单列
        createFormCol(addViewId, addDataSourcePair.getRight(), req.getMainTableFields(), "CompValueEditor", "add");
        // 添加表单保存按钮
        createAddFormBtn(addViewId, addDataSourcePair.getLeft(), tableViewId, null, null);

        /* ===============  修改表单  =============== */
        // 创建窗口
        String editViewId = createWindow(req.getViewName() + "修改", groupId, "modal");
        // 创建数据源
//        String editDataSourceViewId = createView(editViewId, "数据源", "comp");
//        String editDataSourceCompId = createComp(editViewId, editDataSourceViewId, "CompDataSource", "数据源");
        // 左 视图ID 右 数据源ID
        ImmutablePair<String, String> editDataSourcePair = createDataSource(editViewId);
        // 刷新
        Modules.getInstance().InitAll();
        // 更新数据源
        updateDataSource(req.getTableName(), editDataSourcePair.getRight(), "all", "table", CompDataSource.class, true, false, req.getTableName(), editViewId);
        // 生成表单列
        createFormCol(editViewId, editDataSourcePair.getRight(), req.getMainTableFields(), "CompValueEditor", "edit");
        // 创建按钮
        createEditFormBtn(editViewId, editDataSourcePair.getLeft(), tableViewId, req.getTableName(), "where-limit-ids");

        /* ===============   详情     =============== */
        List<String> orderList = new ArrayList<>();
        // 创建窗口
        String detailViewId = createWindow(req.getViewName() + "详情", groupId, "drawer");
        // 创建数据源
//        String detailDataSourceViewId = createView(detailViewId, "数据源", "comp");
//        String detailDataSourceCompId = createComp(detailViewId, detailDataSourceViewId, "CompDataSource", "数据源");
//        orderList.add(detailDataSourceViewId);
        // 左 视图ID 右 数据源ID
        ImmutablePair<String, String> detailDataSourcePair = createDataSource(detailViewId);
        // 刷新
        Modules.getInstance().InitAll();
        // 更新数据源
        updateDataSource(req.getTableName(), detailDataSourcePair.getRight(), "all", "defined", CompDataSource.class, true, true, req.getTableName(), detailViewId);
        // 生成详情列
        String formRootViewId = createFormCol(detailViewId, detailDataSourcePair.getRight(), req.getMainTableFields(), "CompValueRender", "detail");
        orderList.add(formRootViewId);
        // 有子表再生成
        if (CollUtil.isNotEmpty(req.getSubTableFields())) {
            // 详情tab栏
            String detailTabViewId = createView(detailViewId, "tabs", "tabs");
            orderList.add(detailTabViewId);
            // 创建子表表格
            req.getSubTableFields().forEach((tableName, fieldConfigs) -> {
                if (CollUtil.isEmpty(fieldConfigs)) {
                    return;
                }
                String viewName = fieldConfigs.get(0).getTableDisplayName();
                // 创建表格
                String subTableViewId = createView(detailTabViewId, viewName, "comp");
                String subTableCompId = createComp(detailTabViewId, subTableViewId, "CompGrid", viewName);
                // 创建子表新增表单
                if (req.getSubAdd()) {
                    /* ===============  新增表单  =============== */
                    // 创建窗口
                    String subAddViewId = createWindow(viewName + "新增", groupId, "modal");
                    // 创建数据源
//                    String subAddDataSourceViewId = createView(subAddViewId, "数据源", "comp");
//                    String subAddDataSourceCompId = createComp(subAddViewId, subAddDataSourceViewId, "CompDataSource", "数据源");
                    ImmutablePair<String, String> subAddDataSourcePair = createDataSource(subAddViewId);
                    // 刷新
                    Modules.getInstance().InitAll();
                    // 更新数据源
                    try {
                        updateDataSource(tableName, subAddDataSourcePair.getRight(), "none", "table", CompDataSource.class, false, false, tableName, subAddViewId);
                    } catch (Exception e) {
                        log.error("更新数据源异常", e);
                    }
                    // 生成表单列
                    createFormCol(subAddViewId, subAddDataSourcePair.getRight(), fieldConfigs, "CompValueEditor", "add");
                    // 添加表单保存按钮
                    String mainTableName = req.getTableName().replace("z_", "");
                    createAddFormBtn(subAddViewId, subAddDataSourcePair.getLeft(), subTableViewId, mainTableName, "fill-foreign");
                    // 创建新增按钮
                    createTableBtn(subTableCompId, subAddViewId);
                }

                // 生成表格列
                try {
                    // 刷新
                    Modules.getInstance().InitAll();
                    // 更新数据源
                    String proceedTableName = tableName.replace("table", "z_table");
                    updateDataSource(proceedTableName, subTableCompId, "all", "defined", CompGrid.class, false, true, req.getTableName(), null);
                    // 创建表格列
                    createTableCol(subTableCompId, fieldConfigs, null, null, tableName, subTableViewId, req.getSubAdd());
                } catch (Exception e) {
                    log.error("生成详情列异常", e);
                }
            });
            // 调整下顺序
            for(int i = 0;i < orderList.size(); i++){
                baseDBService.updateEq("v_tree_view", Lutils.genMap("ord", i+1), Lutils.genMap("id", orderList.get(i)));
            }
        }

        /* ===============   主表表格列和按钮   =============== */
        // 创建并绑定表格按钮
        createTableBtn(tableCompId, addViewId);
        // 生成表格列
        List<TableColInfoResp> fieldConfigs = new ArrayList<>();
        fieldConfigs.addAll(req.getMainTableFields());
        fieldConfigs.addAll(req.getPriTableFields());
        // 同时添加了修改、详情和删除按钮
        createTableCol(tableCompId, fieldConfigs, editViewId, detailViewId, req.getTableName(), tableViewId, true);

        // 最后刷新内存
        Modules.getInstance().InitAll();
    }

    /**
     * 创建分组
     *
     * @param groupName 分组名称
     */
    private String createGroup(String groupName) {
        // 创建视图
        View view = new View();
        view.name = groupName;
        view.parent_id = "view0";
        view.view_type = "folder";
        return createView(view);
    }

    /**
     * 创建窗口
     *
     * @param viewName 窗口名称
     * @param groupId  分组ID
     */
    private String createWindow(String viewName, String groupId, String viewType) {
        View view = new View();
        view.is_show = true;
        view.name = viewName;
        view.parent_id = groupId;
        view.view_type = viewType;
        return createView(view);
    }

    /**
     * 基础方法 创建组件
     *
     * @param parentId 父窗口ID
     * @param viewId   组件ID
     * @param type     组件类型
     * @param compName 组件名称
     */
    private String createComp(String parentId, String viewId, String type, String compName) {
        // 生成ID
        String compId = type + Constants.getTimeFormatId();
        Modules.getInstance().createEmptyComp(compId, type);
        View view = new View();
        view.create();
        view.id = viewId;
        view.parent_id = parentId;
        view.name = compName;
        view.view_type = "comp";
        view.height = 64;
        view.comp_name = type;
        view.col_span = 3;
        view.gutter = 16;
        view.is_show = true;
        view.comp_id = compId;

        baseDBService.updateEq("v_module",
                Lutils.genMap("json", JSON.toJSONString(view)),
                Lutils.genMap("id", viewId));
        Modules.getInstance().loadFromDB(viewId);
        baseDBService.updateEq("v_tree_view",
                Lutils.genMap("name", compName, "view_type", "comp"),
                Lutils.genMap("id", viewId));
        return compId;
    }

    private ImmutablePair<String, String> createDataSource(String viewId) {
        Modules.getInstance().InitAll();
        CompDataSource ds = new CompDataSource();
        ds.id = "CompDataSource"+Constants.getTimeFormatId();
        CompDataSource dss = new CompDataSource();
        dss.id = ds.id;
        dss.view_id = "view" + Constants.getTimeFormatId();
        dss.name = "数据源";
        View v = (View)Modules.getInstance().get(viewId, false);
        if(v.dsList==null)
            v.dsList = new ArrayList<>();
        v.dsList.add(dss);
        Modules.getInstance().create(dss.id, "CompDataSource", ds);
        Modules.getInstance().save(v.id, v);
        return new ImmutablePair<>(dss.view_id, dss.id);
    }

    /**
     * 创建表格列
     */
    private void createTableCol(String tableCompId, List<TableColInfoResp> fieldConfigs, String editViewId, String detailViewId, String tableName, String tableViewId, Boolean createOperationCol) throws Exception {
        // 处理表名
        String proceedTableName = tableName.replace("z_table", "table");
        // 存储更新的字段信息
        List<CompGridCol> gridCols = new ArrayList<>();
        // 表格组件
        CompGrid table = (CompGrid) Modules.getInstance().get(tableCompId, true);
        // 表格数据源字段
        List<CompDataSourceField> fields = table.compDataSource.fields;
        Map<String, CompDataSourceField> fieldMap = fields.stream().collect(Collectors.toMap(CompDataSourceField::getField, v -> v));

        for (TableColInfoResp tableColInfoResp : fieldConfigs) {
            if (!tableColInfoResp.getShowInTable()) {
                continue;
            }
            String tableAlias = tableColInfoResp.getTableName();
            tableAlias = tableAlias.replace("table", "t");
            CompDataSourceField field = fieldMap.get(tableAlias + "_" + tableColInfoResp.getColumnName());
            if (Objects.isNull(field)) {
                continue;
            }
            CompGridCol saveCol = new CompGridCol();
            saveCol.title = tableColInfoResp.getDisplayName();
            saveCol.min_width = tableColInfoResp.getWidth();
            // 根据数据类型生成不同的渲染器
            CompValueRender compValueRender = new CompValueRender();
            compValueRender.create(null);
            compValueRender.use_defined_value = false;
            switch (tableColInfoResp.getRenderType()) {
                case "datetime":
                    compValueRender.render_type = "datetime";
                    compValueRender.datetime_fmt = "yyyy-MM-dd";
                    break;
                case "file":
                    compValueRender.render_type = "file";
                    break;
                case "area":
                    compValueRender.render_type = "area";
                    break;
                case "user":
                    compValueRender.render_type = "user";
                    break;
            }
            saveCol.compValueRender = compValueRender;
            saveCol.btns = Collections.emptyList();
            saveCol.exec = new Exec();
            saveCol.setDsFieldInfo();
            compValueRender.ds_id = field.getDs_id();
            compValueRender.ds_field_id = field.getId();
            gridCols.add(saveCol);
        }

        // 创建操作列
        if (createOperationCol) {
            CompGridCol saveCol = new CompGridCol();
            saveCol.title = "操作";
            saveCol.min_width = 100;
            CompValueRender compValueRender = new CompValueRender();
            compValueRender.create(null);
            compValueRender.use_defined_value = false;
            saveCol.compValueRender = compValueRender;
            saveCol.exec = new Exec();
            compValueRender.ds_id = "CompDataSource" + Constants.getTimeFormatId();
            List<Exec> buttons = new ArrayList<>();
            if (StringUtils.isNotEmpty(editViewId)) {
                // 创建修改按钮
                Exec exec = new Exec();
                exec.id = "Exec" + Constants.getTimeFormatId();
                exec.name = "修改";
                exec.style = "button";
                exec.type = "text";
                exec.size = "small";
                exec.round = true;
                exec.icon = "fa fa-edit";
                // 按钮的点击动作列表
                List<ExecOp> ops = new ArrayList<>();
                ops.add(createOp("view", editViewId, "open", "modal", null, null));
                exec.ops = ops;
                // 加入按钮列表
                buttons.add(exec);
            }
            // 创建删除按钮
            Exec deleteExec = new Exec();
            deleteExec.id = "Exec" + Constants.getTimeFormatId();
            deleteExec.name = "删除";
            deleteExec.style = "button";
            deleteExec.type = "text";
            deleteExec.size = "small";
            deleteExec.round = true;
            deleteExec.icon = "fa fa-minus";
            // 按钮的点击动作列表
            List<ExecOp> deleteOps = new ArrayList<>();
            ExecOp op1 = new ExecOp();
            op1.id = Constants.getTimeFormatId();
            op1.op_type = "delete";
            op1.op_obj_type = "view";
            op1.table_id = proceedTableName;
            ExecOpAction deleteOpAction = new ExecOpAction();
            deleteOpAction.from_table_id = proceedTableName;
            deleteOpAction.session_type = "where-limit-ids";
            deleteOpAction.from_op_id = "-1";
            op1.opActions.add(deleteOpAction);
            deleteOps.add(op1);

            ExecOp op2 = new ExecOp();
            op2.id = Constants.getTimeFormatId();
            op2.op_type = "refresh";
            op2.op_obj_type = "view";
            op2.viewType = "comp";
            op2.view_id = tableViewId;
            deleteOps.add(op2);
            deleteExec.ops = deleteOps;
            // 加入按钮列表
            buttons.add(deleteExec);
            // 表格按钮
            saveCol.btns = buttons;
            gridCols.add(saveCol);
        }

        // 绑定详情点击事件
        if (StringUtils.isNotEmpty(detailViewId)) {
            CompGridCol compGridCol = gridCols.get(0);
            compGridCol.enable_click = true;
            ExecOp op = new ExecOp();
            op.id = Constants.getTimeFormatId();
            op.op_type = "open";
            op.op_obj_type = "view";
            op.viewType = "drawer";
            op.view_id = detailViewId;
            compGridCol.exec.ops.add(op);
            compGridCol.compValueRender.enable_click = false;
            // compGridCol.compValueRender.exec.ops.add(op);
        }
        table.gridCols = gridCols;
        Modules.getInstance().update(BeanUtil.beanToMap(table), dataSource);
    }

    private void createAddFormBtn(String formViewId, String dataSourceViewId, String tableViewId, String fromTableName, String sessionType) {
        View view = (View) Modules.getInstance().get(formViewId, true);
        List<Exec> viewBtns = new ArrayList<>();
        Exec exec = new Exec();
        exec.id = "Exec" + Constants.getTimeFormatId();
        exec.name = "保存";

        List<ExecOp> ops = new ArrayList<>();
        ops.add(createOp("view", dataSourceViewId, "insert", "comp", fromTableName, sessionType));
        ops.add(createOp("view", formViewId, "close", "modal", null, null));
        ops.add(createOp("view", tableViewId, "refresh", "comp", null, null));
        exec.ops = ops;
        viewBtns.add(exec);
        view.viewBtns = viewBtns;
        Modules.getInstance().update(BeanUtil.beanToMap(view), dataSource);
    }

    private void createEditFormBtn(String formViewId, String dataSourceViewId, String tableViewId, String fromTableName, String sessionType) {
        fromTableName = fromTableName.replace("z_", "");
        View view = (View) Modules.getInstance().get(formViewId, true);
        List<Exec> viewBtns = new ArrayList<>();
        Exec exec = new Exec();
        exec.id = "Exec" + Constants.getTimeFormatId();
        exec.name = "保存";

        List<ExecOp> ops = new ArrayList<>();
        ops.add(createOp("view", dataSourceViewId, "update", "comp", fromTableName, sessionType));
        ops.add(createOp("view", formViewId, "close", "modal", null, null));
        ops.add(createOp("view", tableViewId, "refresh", "comp", null, null));
        exec.ops = ops;
        viewBtns.add(exec);
        view.viewBtns = viewBtns;
        Modules.getInstance().update(BeanUtil.beanToMap(view), dataSource);
    }

    private ExecOp createOp(String opObjType, String viewId, String opType, String viewType, String fromTableName, String sessionType) {
        ExecOp op = new ExecOp();
        op.id = Constants.getTimeFormatId();
        op.op_obj_type = opObjType;
        op.view_id = viewId;
        op.op_type = opType;
        op.viewType = viewType;
        if (Objects.nonNull(fromTableName)) {
            ExecOpAction execOpAction = new ExecOpAction();
            execOpAction.from_op_id = "-1";
            execOpAction.from_table_id = fromTableName;
            execOpAction.id = Constants.getTimeFormatId();
//            execOpAction.session_type = "where-limit-ids";
            execOpAction.session_type = sessionType;
            op.opActions.add(execOpAction);
        }
        return op;
    }

    /**
     * 创建表格按钮
     *
     * @param tableCompId 表格组件ID
     * @param addViewId   新增表单ID
     */
    private void createTableBtn(String tableCompId, String addViewId) {
        CompGrid table = (CompGrid) Modules.getInstance().get(tableCompId, true);
        List<Exec> btns = new ArrayList<>();
        // 新增按钮
        Exec exec = new Exec();
        exec.id = "Exec" + Constants.getTimeFormatId();
        exec.name = "新增";
        exec.size = "small";
        exec.style = "button";
        exec.type = "primary";
        exec.round = true;
        // 创建动作
        ExecOp op = new ExecOp();
        op.id = Constants.getTimeFormatId();
        op.op_obj_type = "view";
        op.view_id = addViewId;
        op.op_type = "open";
        exec.ops.add(op);
        // 添加按钮
        btns.add(exec);
        table.topBtns = btns;
        Modules.getInstance().update(BeanUtil.beanToMap(table), dataSource);
    }

    /**
     * 创建新增表单列
     */
    private String createFormCol(String parentId, String dataSourceCompId, List<TableColInfoResp> fieldConfigs, String compName, String type) {
        CompDataSource compDataSource = (CompDataSource) Modules.getInstance().get(dataSourceCompId, true);
        Map<String, TableColInfoResp> configMap = new HashMap<>();
        int fieldNum = 0;
        if (Objects.equals(type, "add")) {
            fieldNum = (int) fieldConfigs.stream().filter(TableColInfoResp::getShowInAdd).count();
            configMap = fieldConfigs.stream().filter(TableColInfoResp::getShowInAdd).collect(Collectors.toMap(TableColInfoResp::getColId, v -> v));
        } else if (Objects.equals(type, "edit")) {
            fieldNum = (int) fieldConfigs.stream().filter(TableColInfoResp::getShowInEdit).count();
            configMap = fieldConfigs.stream().filter(TableColInfoResp::getShowInEdit).collect(Collectors.toMap(TableColInfoResp::getColId, v -> v));
        } else if (Objects.equals(type, "detail")) {
            fieldNum = (int) fieldConfigs.stream().filter(TableColInfoResp::getShowInDetail).count();
            configMap = fieldConfigs.stream().filter(TableColInfoResp::getShowInDetail).collect(Collectors.toMap(k -> {
                String tName = k.getTableName().replace("table", "t");
                return tName + "_" + k.getColumnName();
            }, v -> v));
        }
        if (fieldNum == 0) {
            return null;
        }
        // 创建根节点
        View root = new View();
        root.view_type = "form";
        root.display_style = "none";
        root.is_show = true;
        root.parent_id = parentId;
        root.id = "view" + Constants.getTimeFormatId();
        root.name = "新表单" + root.id;
        root.form_col_num = 2; // 默认两列
        root.item_border_css = "font-size:0.9rem;border:1px solid #e7e9ea; border-radius: 8px; overflow:hidden;";
        root.form_label_td_style = "width:80px;border-right:1px solid #f1f2f7;border-bottom:1px solid #f1f2f7;padding:10px;font-size:0.9rem;background:#f9faff;color:#888;";
        root.form_content_td_style = "border-right:1px solid #f1f2f7;border-bottom:1px solid #f1f2f7;padding:10px;font-size:0.9rem;";

        //添加根节点v_tree_view - 先获取当前最大ord
        Integer currentMaxOrd = baseDBService.selectMaxColEq("v_tree_view", "ord", Lutils.genMap("parent_id", parentId));
        if (currentMaxOrd == null) {
            currentMaxOrd = 0;
        }
        baseDBService.insertMapWithSpecifiedOrd("v_tree_view",
                Lutils.genMap("id", root.id, "parent_id", parentId, "view_type", "form", "name", root.name, "is_deleted", false),
                "ord", currentMaxOrd + 1);

        //添加根节点module
        Modules.getInstance().create(root.id, "View", root);

        //添加字段组合
        for (int i = 0; i < compDataSource.fields.size(); i++) {
            CompDataSourceField fd = compDataSource.fields.get(i);
            if (Objects.isNull(fd.getTable_id())) {
                continue;
            }
            TableColInfoResp tableColInfoResp;
            // detail的key是t139_xxx
            if (Objects.equals(type, "detail")) {
                tableColInfoResp = configMap.get(fd.getField());
            } else {
                tableColInfoResp = configMap.get(fd.getTable_col_id());
            }
            if (Objects.isNull(tableColInfoResp)) {
                continue;
            }
            if (Objects.equals(type, "add") && !tableColInfoResp.getShowInAdd()) {
                continue;
            }
            if (Objects.equals(type, "edit") && !tableColInfoResp.getShowInEdit()) {
                continue;
            }
            if (Objects.equals(type, "detail") && !tableColInfoResp.getShowInDetail()) {
                continue;
            }
            // 计算当前字段的ord值
            int baseOrd = i * 2 + 1; // 每个字段占用2个ord位置

            // 添加标签
            View label = new View();
            label.id = "view" + Constants.getTimeFormatId();
            label.name = tableColInfoResp.getDisplayName();
            label.view_type = "text";
            label.is_show = true;
            label.parent_id = root.id;
            baseDBService.insertMapWithSpecifiedOrd("v_tree_view",
                    Lutils.genMap("id", label.id, "parent_id", root.id, "view_type", label.view_type, "name", label.name, "is_deleted", false),
                    "ord", baseOrd);
            Modules.getInstance().create(label.id, "View", label);

            // 添加值编辑器
            View value = new View();
            value.id = "view" + Constants.getTimeFormatId();
            value.view_type = "comp";
            value.comp_name = compName;
            value.is_show = true;
            value.parent_id = root.id;
            baseDBService.insertMapWithSpecifiedOrd("v_tree_view",
                    Lutils.genMap("id", value.id, "parent_id", root.id, "view_type", value.view_type, "name", value.name, "is_deleted", false),
                    "ord", baseOrd + 1);

            if (value.comp_name.equals("CompValueEditor")) {
                value.name = fd.fieldName + "-编辑器";
                CompValueEditor edt = new CompValueEditor();
                edt.create("CompValueEditor" + Constants.getTimeFormatId());
                edt.ds_id = compDataSource.id;
                edt.ds_field_id = fd.id;
                // 匹配不同的编辑器
                switch (tableColInfoResp.getRenderType()) {
                    case "datetime":
                        edt.editor_type = "datetime-editor";
                        break;
                    case "uuid":
                        edt.editor_type = "uuid-editor";
                        break;
                    case "number":
                        edt.editor_type = "number-editor";
                        break;
                    case "file":
                        edt.editor_type = "file-editor";
                        break;
                    case "area":
                        edt.editor_type = "area-selector";
                        break;
                    case "single-select":
                        edt.editor_type = "single-select-editor";
                        if (fd.getField().contains("shifou")) {
                            edt.single_options = "[\"是\",\"否\"]";
                        }
                        if (fd.getField().contains("shenpizhuangtai") || fd.getField().contains("shenhezhuangtai")) {
                            edt.single_options = "[\"通过\",\"不通过\"]";
                        }
                        break;
                    case "foreign-key":
                        edt.editor_type = "foreign-key-editor";
                        // 要创建表格
                        String grid_id = "CompGrid" + Constants.getTimeFormatId();
                        Modules.getInstance().createEmptyComp(grid_id, "CompGrid");
                        Modules.getInstance().InitAll();
                        String foreign_table_id = fd.field.replace("z_", "").replace("_id", "");
                        // 更新数据源
                        try {
                            updateDataSource(foreign_table_id, grid_id, "all", "table", CompGrid.class, false, false, foreign_table_id, null);
                        } catch (Exception e) {
                            log.error("更新数据源失败", e);
                        }
                        Modules.getInstance().InitAll();
                        // 添加表格列（默认取前3个字段）
                        CompGrid compGrid = (CompGrid) Modules.getInstance().get(grid_id, true);
                        List<CompGridCol> gridCols = compGrid.gridCols;
                        // 过滤掉前台总数和后台总数后3个字段
                        List<CompDataSourceField> fields = compGrid.compDataSource.fields.stream().filter(o -> !o.field.contains("ds_")).collect(Collectors.toList());
                        for (int j = 0; j < Math.min(3, fields.size()); j++) {
                            CompDataSourceField field = fields.get(j);
                            CompGridCol saveCol = new CompGridCol();
                            saveCol.title = field.fieldName;
                            saveCol.min_width = 100;
                            // 根据数据类型生成不同的渲染器
                            CompValueRender compValueRender = new CompValueRender();
                            compValueRender.create(null);
                            compValueRender.use_defined_value = false;
                            switch (genSqlService.inferRenderType(field.data_type, field.fieldName)) {
                                case "datetime":
                                    compValueRender.render_type = "datetime";
                                    compValueRender.datetime_fmt = "yyyy-MM-dd";
                                    break;
                                case "file":
                                    compValueRender.render_type = "file";
                                    break;
                                case "area":
                                    compValueRender.render_type = "area";
                                    break;
                                case "user":
                                    compValueRender.render_type = "user";
                                    break;
                            }
                            saveCol.compValueRender = compValueRender;
                            saveCol.btns = Collections.emptyList();
                            saveCol.exec = new Exec();
                            saveCol.setDsFieldInfo();
                            compValueRender.ds_id = compGrid.ds_id;
                            compValueRender.ds_field_id = field.getId();
                            gridCols.add(saveCol);
                        }
                        compGrid.gridCols = gridCols;
                        Modules.getInstance().update(BeanUtil.beanToMap(compGrid), dataSource);
                        edt.compGrid = compGrid;
                        edt.grid_id = grid_id;
                        break;
                    default:
                        edt.editor_type = "text-editor";
                        break;
                }
                value.comp_id = edt.id;
                Modules.getInstance().create(edt.id, "CompValueEditor", edt);
            } else {
                value.name = fd.fieldName + "-渲染器";
                CompValueRender vr = new CompValueRender();
                vr.create("CompValueRender" + Constants.getTimeFormatId());
                vr.ds_id = compDataSource.id;
                vr.use_defined_value = false;
                vr.ds_field_id = fd.id;
                // 匹配不同的编辑器
                switch (tableColInfoResp.getRenderType()) {
                    case "datetime":
                        vr.render_type = "datetime";
                        vr.datetime_fmt = "yyyy-MM-dd hh:mm";
                        break;
                    case "file":
                        vr.render_type = "file";
                        break;
                    case "area":
                        vr.render_type = "area";
                        break;
                    default:
                        vr.render_type = "text";
                        break;
                }
                value.comp_id = vr.id;
                Modules.getInstance().create(vr.id, "CompValueRender", vr);
            }
            Modules.getInstance().create(value.id, "View", value);
        }

        List<Map> ll = businessMapper.getViewTree(Lutils.genMap("view_id", root.id));
        List<Object> re = new ArrayList<>();
        for (Map node : ll) {
            if (!Objects.equals(node.get("view_type"), "folder")) {
                View v = (View) Modules.getInstance().get(node.get("id"), false);
                if (v != null) {
                    v.parent_id = (String) node.get("parent_id");
                    re.add(v);
                    if (v.view_type.equals("comp") && v.comp_id != null) {
                        v.comp = Modules.getInstance().get(v.comp_id, false);
                    }
                }
            } else {
                re.add(node);
            }
        }
        return root.id;
    }

    /**
     * 基础方法 创建视图
     *
     * @param parentId 父ID
     */
    private String createView(String parentId, String name, String type) {
        View view = new View();
        view.create();
        view.col_span = 3;
        view.gutter = 16;
        view.is_show = true;
        view.name = name;
        view.parent_id = parentId;
        view.view_type = type;
        return createView(view);
    }

    /**
     * 基础方法 创建视图
     */
    private String createView(View view) {
        // 生成随机ID
        String viewId = Constants.getTimeFormatId();
        viewId = "view" + viewId;
        view.id = viewId;
        baseDBService.insertMapAutoFillMaxOrd("v_tree_view",
                Lutils.genMap("id", view.id, "parent_id", view.parent_id, "name",
                        view.name, "view_type", view.view_type,
                        "is_deleted", false),
                "ord",
                Lutils.genMap("parent_id", view.parent_id));
        Modules.getInstance().create(viewId, "View", view);
        return viewId;
    }

    /**
     * 基础方法 更新数据源
     */
    private <T> void updateDataSource(String tableName, String tableCompId, String dataScope, String dataType, Class<T> compType, Boolean sessionLimit, Boolean addPlaceholder, String mainTableName, String windowViewId) throws Exception {
        T compInstance = compType.cast(Modules.getInstance().get(tableCompId, true));
        if (compInstance instanceof CompGrid) {
            CompGrid compGrid = (CompGrid) compInstance;
            CompDataSource compDataSource = compGrid.compDataSource;
            if (Objects.isNull(compDataSource)) {
                String dsId = "CompDataSource" + Constants.getTimeFormatId();
                compGrid.ds_id = dsId;
                compDataSource = new CompDataSource();
                compDataSource.id = dsId;
                compDataSource.fields = new ArrayList<>();
                CompDataSourceField field = new CompDataSourceField();
                field.id = "ds_total";
                field.field = "ds_total";
                field.field_type = "ds_total";
                CompDataSourceField field1 = new CompDataSourceField();
                field1.id = "ds_rows_length";
                field1.field = "ds_rows_length";
                field1.field_type = "ds_rows_length";
                compDataSource.fields.add(field);
                compDataSource.fields.add(field1);
                Modules.getInstance().create(dsId, "CompDataSource", compDataSource);
            }
            updateCompDataSource(compDataSource, dataScope, dataType, tableName, sessionLimit, addPlaceholder, mainTableName, null);
            Modules.getInstance().InitAll();
            compGrid.compDataSource = (CompDataSource) Modules.getInstance().get(compDataSource.id, true);
            Modules.getInstance().update(BeanUtil.beanToMap(compGrid), dataSource);
        } else if (compInstance instanceof CompDataSource) {
            CompDataSource compDataSource = (CompDataSource) compInstance;
            updateCompDataSource(compDataSource, dataScope, dataType, tableName, sessionLimit, addPlaceholder, mainTableName, windowViewId);
        }
    }

    private void updateCompDataSource(CompDataSource compDataSource, String dataScope, String dataType, String tableName, Boolean sessionLimit, Boolean addPlaceholder, String mainTableName, String windowViewId) throws Exception {
        String processedTableName = tableName.replace("z_", "");
        String orderTableName = processedTableName.replace("table", "t");
        compDataSource.data_access_scope = dataScope;
        compDataSource.data_type = dataType;
        compDataSource.table_id = processedTableName;
        compDataSource.enable_total = true;
        if (sessionLimit) {
            compDataSource.limit_session_table_id = processedTableName;
        }
        // 如果是自定义SQL则生成SQL并赋值，否则是表格模式直接创建字段
        if (Objects.equals("defined", dataType)) {
            String sql = genSqlService.generateSelectWithLeftJoins(tableName);
            // 如果需要添加主表占位符
            if (addPlaceholder) {
                String tName = tableName.replace("z_table", "t");
                if (Objects.equals(tableName, mainTableName)) {
                    sql += " \nAND " + tName + ".id_ IN " + "(..." + mainTableName + "_ids)";
                } else {
                    sql += " \nAND " + tName + "." + mainTableName + "_id IN " + "(..." + mainTableName + "_ids)";
                }
            }
            compDataSource.data_sql = sql;
            compDataSource.order_sql = "order by " + orderTableName + ".create_time_ desc";
        } else {
            Table table = (Table) Modules.getInstance().get(processedTableName, false);
            // 添加数据源字段
            List<CompDataSourceField> fields = table.cols.stream().map(col -> {
                CompDataSourceField field = new CompDataSourceField();
                field.field_type = "table_field";
                field.id = Constants.getTimeFormatId();
                field.table_col_id = col.id;
                field.table_id = processedTableName;
                return field;
            }).collect(Collectors.toList());
            compDataSource.fields.addAll(fields);
        }
        Modules.getInstance().update(BeanUtil.beanToMap(compDataSource), dataSource);
//        if (StringUtils.isNotEmpty(windowViewId)) {
//            View windowView = (View) Modules.getInstance().get(windowViewId, true);
//            CompDataSourceSimple dss = new CompDataSourceSimple();
//            if (CollUtil.isNotEmpty(windowView.dsList)) {
//                dss = windowView.dsList.get(0);
//                dss.id = compDataSource.id;
//                dss.name = "数据源";
//            } else {
//                dss.id = compDataSource.id;
//                dss.name = "数据源";
//                windowView.dsList.add(dss);
//            }
//            Modules.getInstance().save(windowView.id, windowView);
//        }
    }
}
