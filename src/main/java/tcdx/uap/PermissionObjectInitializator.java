package tcdx.uap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import tcdx.uap.common.utils.AreaUtils;
import tcdx.uap.mapper.BusinessMapper;
import tcdx.uap.mapper.ServiceConfigMapper;
import tcdx.uap.mapper.SystemMapper;
import tcdx.uap.service.BaseDBService;
import tcdx.uap.service.BusinessService;
import tcdx.uap.service.ServiceConfigService;
import tcdx.uap.service.entities.CompUtils;
import tcdx.uap.service.store.*;

@Component
public class PermissionObjectInitializator implements ApplicationRunner
{

    @Autowired
    BaseDBService db;

    @Autowired
    ServiceConfigMapper cfgMapper;

    @Autowired
    private BusinessMapper businessMapper;

    @Autowired
    private BusinessService businessService;

    @Autowired
    private ServiceConfigService serviceConfigService;

    @Autowired
    private SystemMapper systemMapper;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        //打开---------------------
        RoleStore.getInstance().InitAll(db, cfgMapper);
        CompUtils.getInstance().Init(db, businessMapper, systemMapper);
        DictStore.getInstance().InitAll(db,cfgMapper);


        // 处理命令行参数
//        MenuStore.getInstance().InitAll(db, cfgMapper);
//        ApiStore.getInstance().InitAll(db, cfgMapper);

        //打开---------------------
        Modules.getInstance().InitAll(db, businessMapper, businessService);
        Modules.getInstance().convertCompFieldV20260102();
        Modules.getInstance().convertDSFieldV20260102();
        //转换数据源20260102

// ----------------start转换-----------------
        //流程相关
//        TableStore.getInstance().InitAll(db, cfgMapper);
//        UserScopeStore.getInstance().InitAll(db, systemMapper);
//        FlowStore.getInstance().InitAll(db, businessMapper);
//        ViewStore.getInstance().InitAll(db, businessService);
//        ExecObjStore.getInstance().InitAll(db,cfgMapper);
//        DSStore.getInstance().InitAll(db, businessMapper);
//        CompValueRenderStore.getInstance().InitAll(db, cfgMapper);
//        CompValueEditorStore.getInstance().InitAll(db, cfgMapper);
//        CompUserSelectorStore.getInstance().InitAll(db, cfgMapper);
//        CompCarouselStore.getInstance().InitAll(db,businessService);
//        CompGridStore.getInstance().InitAll(db, serviceConfigService);
//        CompCountAggrStore.getInstance().InitAll(db, businessService);
//        CompCardStore.getInstance().InitAll(db, businessService);
//        CompTimelineStore.getInstance().InitAll(db, businessService);
////        ////------------------新系统
//        businessService.convertTable();
//        businessService.convertViews();
//--------------------end-------------------

        // 加载地区列表
        //打开---------------------
        AreaUtils.getINSTANCE().InitAll();

//        List<Map> maps = db.selectEq("v_module", Lutils.genMap("type", "Table"));
//        for (Map map : maps){
//            String module_id = map.get("id").toString();
//            Table table = (Table) Modules.getInstance().get(module_id,false);
//            Modules.getInstance().updateTableFlowNodeEdges(table);
//        }
    }
}
