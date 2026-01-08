//package tcdx.uap.common.utils;
//
//import com.googlecode.aviator.AviatorEvaluator;
//import org.springframework.stereotype.Component;
//
//import java.util.HashMap;
//import java.util.Map;
//
//@Component
//public class AviatorRules {
//
//    /**
//     * 自定义规则校验
//     */
//    public boolean validateCustomRule(String rule, Map<String, Object> env) {
//        try {
//            Object result = AviatorEvaluator.execute(rule, env);
//            return Boolean.TRUE.equals(result);
//        } catch (Exception e) {
//            return false;
//        }
//    }
//}
