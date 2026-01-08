package tcdx.uap;

import com.googlecode.aviator.AviatorEvaluator;
import com.googlecode.aviator.Expression;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import tcdx.uap.liteflow.ExecOpRecord;
import tcdx.uap.service.BusinessService;

import javax.annotation.Resource;

@SpringBootTest
class NewUapApplicationTests {
//
//    @Autowired
//    ProcessEngine processEngine;
//
//    @Autowired
//    RepositoryService repositoryService;
//
//    @Autowired
//    RuntimeService runtimeService;
//
//    @Autowired
//    TaskService taskService;
//
//    @org.junit.jupiter.api.Test
//    public void test() {
////        List<ProcessDefinition> l= repositoryService.createProcessDefinitionQuery().processDefinitionNameLike("%o%").list();
//        List l = taskService.createTaskQuery().taskAssigneeLike("%%").listPage(1,10);
//        System.out.println(l);
//
//    }
//
//
//    @org.junit.jupiter.api.Test
//    public void test1(){
//        String proc_inst_id_ = "3af127aa-8af8-11ef-9f9e-f8fe5eb7ca24";
//        HistoryService historyService = processEngine.getHistoryService();
//        List<HistoricActivityInstance> activities =
//                historyService.createHistoricActivityInstanceQuery()
//                        .processInstanceId(proc_inst_id_)
//                        .finished()
//                        .orderByHistoricActivityInstanceEndTime().asc()
//                        .list();
//
//        for (HistoricActivityInstance activity : activities) {
//            System.out.println(toMap(activity));
//        }
//    }
//
//
//    public Map toMap(HistoricActivityInstance act){
//        Map re = new HashMap();
//        re.put("act_ru_task_id_", act.getTaskId());
//        re.put("act_id_", act.getActivityId());
//        re.put("name_", act.getActivityName());
//        re.put("executed_id_", act.getExecutionId());
//        re.put("act_type_", act.getActivityType());
//        re.put("耗时", act.getDurationInMillis());
//        return re;
//    }
//    public Map toMap(ProcessDefinition pf){
//        Map re = new HashMap();
//        re.put("id", pf.getId());
//        re.put("deployment_id_", pf.getDeploymentId());
//        re.put("name_", pf.getName());
//        re.put("key_", pf.getKey());
//        return re;
//    }

    @org.junit.jupiter.api.Test
    public void testConfig(){

    }

    @Autowired
    BusinessService businessService;

    @org.junit.jupiter.api.Test
    public void SetViewsDsListToNull(){
        //将视图中的数据源。
        businessService.SetViewsDsListToNull();
    }

    @org.junit.jupiter.api.Test
    public void SetViewsDsToRootView(){
        //将视图中的数据源统一归类到dsList中。
        businessService.SetViewsDsToRootView();
    }

    @org.junit.jupiter.api.Test
    public void testAvaitor(){
        //将视图中的数据源。
        String expression = "a-(b-c) > 100";
        AviatorEvaluator.validate(expression);
        Expression compiledExp = AviatorEvaluator.compile(expression, true);
        // Execute with injected variables.
        Boolean result = (Boolean) compiledExp.execute(compiledExp.newEnv("a", 100.3, "b", 45, "c", -199.100));
        System.out.println(result);
    }


}
