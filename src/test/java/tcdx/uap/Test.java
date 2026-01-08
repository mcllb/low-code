package tcdx.uap;

import org.springframework.stereotype.Component;
//import java.util.*;
//import org.kie.api.KieServices;
//import org.kie.api.runtime.KieContainer;
//import org.kie.api.runtime.KieSession;
//import org.springframework.beans.factory.annotation.Autowired;
import javax.annotation.Resource;

@Component
public class Test {
//
//    @Autowired
//    ProcessEngine processEngine;

    @org.junit.jupiter.api.Test
    public void test() {
//        KieServices kieServices = KieServices.Factory.get();
//        //获得Kie容器对象
//        KieContainer kieClasspathContainer = kieServices.getKieClasspathContainer();
//        //会话对象，用于和规则引擎交互
//        KieSession kieSession = kieClasspathContainer.newKieSession();
//        //构造订单对象，设置原始价格，由规则引擎根据优惠规则计算优惠后的价格
//        Map order = new HashMap();
//        order.put("price", 99);
//        order.put("do", true);
//        //将数据提供给规则引擎，规则引擎会根据提供的数据进行规则匹配
//        kieSession.insert(order);
//        //激活规则引擎，如果规则匹配成功则执行规则
//        int rs = kieSession.fireAllRules();
//        //关闭会话
//        kieSession.dispose();
//        System.out.println("fired rules：" + rs);
//        System.out.println("优惠前原始价格：" + order);
    }

    @org.junit.jupiter.api.Test
    public void testConfig(){
    }
}
