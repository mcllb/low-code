//package tcdx.uap.common.utils;
//
//import org.springframework.stereotype.Component;
//
//import javax.annotation.PostConstruct;
//import java.beans.Expression;
//import java.util.Map;
//import java.util.concurrent.ConcurrentHashMap;
//
//@Component
//public class AviatorValidator {
//
//    private final Map<String, Expression> expressionCache = new ConcurrentHashMap<>();
//
//    /**
//     * 注册自定义函数
//     */
//    @PostConstruct
//    public void registerCustomFunctions() {

//        // 正则表达式校验
//        AviatorEvaluator.addFunction(new AbstractFunction() {
//            @Override
//            public String getName() { return "matches"; }
//            @Override
//            public AviatorObject call(Map<String, Object> env, AviatorObject arg1,
//                                      AviatorObject arg2) {
//                String value = FunctionUtils.getStringValue(arg1, env);
//                String regex = FunctionUtils.getStringValue(arg2, env);
//                return AviatorBoolean.valueOf(value != null && value.matches(regex));
//            }
//        });
//    }
//
//    /**
//     * 使用前端传来的规则进行校验
//     */
//    public DynamicValidationResult validateWithRules(Map<String, Object> data,
//                                                     List<ValidationRule> rules) {
//        List<FieldValidationResult> fieldResults = new ArrayList<>();
//        List<RuleValidationResult> ruleResults = new ArrayList<>();
//        boolean allValid = true;
//
//        for (ValidationRule rule : rules) {
//            try {
//                // 准备执行环境
//                Map<String, Object> env = new HashMap<>(data);
//                env.put("value", data.get(rule.getField()));
//
//                // 编译并执行表达式
//                Expression expression = expressionCache.computeIfAbsent(
//                        rule.getExpression(), AviatorEvaluator::compile
//                );
//
//                Object result = expression.execute(env);
//                boolean isValid = Boolean.TRUE.equals(result);
//
//                // 构建结果
//                FieldValidationResult fieldResult = FieldValidationResult.builder()
//                        .field(rule.getField())
//                        .value(data.get(rule.getField()))
//                        .valid(isValid)
//                        .message(isValid ? "校验通过" :
//                                (rule.getMessage() != null ? rule.getMessage() : "校验失败"))
//                        .expression(rule.getExpression())
//                        .ruleType(rule.getType())
//                        .build();
//
//                fieldResults.add(fieldResult);
//
//                if (!isValid) {
//                    allValid = false;
//                }
//
//            } catch (Exception e) {
//                FieldValidationResult errorResult = FieldValidationResult.builder()
//                        .field(rule.getField())
//                        .value(data.get(rule.getField()))
//                        .valid(false)
//                        .message("规则执行错误: " + e.getMessage())
//                        .expression(rule.getExpression())
//                        .ruleType(rule.getType())
//                        .build();
//
//                fieldResults.add(errorResult);
//                allValid = false;
//            }
//        }
//
//        return DynamicValidationResult.builder()
//                .valid(allValid)
//                .message(allValid ? "所有校验通过" : "存在校验失败的字段")
//                .fieldResults(fieldResults)
//                .ruleResults(ruleResults)
//                .build();
//    }
//
//    /**
//     * 批量字段校验
//     */
//    public BatchValidationResult validateBatchFields(BatchFieldValidationRequest request) {
//        List<FieldValidationResult> results = new ArrayList<>();
//        boolean allValid = true;
//
//        for (FieldValidationItem item : request.getFields()) {
//            try {
//                Map<String, Object> env = new HashMap<>();
//                env.put("value", item.getValue());
//
//                Expression expression = expressionCache.computeIfAbsent(
//                        item.getExpression(), AviatorEvaluator::compile
//                );
//
//                Object result = expression.execute(env);
//                boolean isValid = Boolean.TRUE.equals(result);
//
//                FieldValidationResult fieldResult = FieldValidationResult.builder()
//                        .field(item.getField())
//                        .value(item.getValue())
//                        .valid(isValid)
//                        .message(isValid ? "校验通过" :
//                                (item.getMessage() != null ? item.getMessage() : "校验失败"))
//                        .expression(item.getExpression())
//                        .build();
//
//                results.add(fieldResult);
//
//                if (!isValid) {
//                    allValid = false;
//                }
//
//            } catch (Exception e) {
//                FieldValidationResult errorResult = FieldValidationResult.builder()
//                        .field(item.getField())
//                        .value(item.getValue())
//                        .valid(false)
//                        .message("规则执行错误: " + e.getMessage())
//                        .expression(item.getExpression())
//                        .build();
//
//                results.add(errorResult);
//                allValid = false;
//            }
//        }
//
//        return BatchValidationResult.builder()
//                .valid(allValid)
//                .message(allValid ? "所有字段校验通过" : "存在校验失败的字段")
//                .results(results)
//                .build();
//    }
//
//    /**
//     * 使用预定义规则组校验
//     */
//    public DynamicValidationResult validateWithPredefinedRules(Map<String, Object> data,
//                                                               String ruleGroup) {
//        // 获取预定义规则
//        List<ValidationRule> rules = getPredefinedRules(ruleGroup);
//        return validateWithRules(data, rules);
//    }
//
//    /**
//     * 获取预定义规则
//     */
//    private List<ValidationRule> getPredefinedRules(String ruleGroup) {
//        Map<String, List<ValidationRule>> predefinedRules = new HashMap<>();
//        // 用户注册规则
//        predefinedRules.put("userRegister", Arrays.asList(
//                ValidationRule.builder()
//                        .field("username")
//                        .expression("lengthBetween(value, 3, 20) && matches(value, '^[a-zA-Z0-9_]+$')")
//                        .message("用户名必须为3-20位的字母、数字或下划线")
//                        .type("format")
//                        .build(),
//                ValidationRule.builder()
//                        .field("password")
//                        .expression("lengthBetween(value, 6, 20) && matches(value, '^(?=.*[a-z])(?=.*[A-Z])(?=.*\\\\d).+$')")
//                        .message("密码必须包含大小写字母和数字，长度6-20位")
//                        .type("format")
//                        .build(),
//                ValidationRule.builder()
//                        .field("email")
//                        .expression("isEmail(value)")
//                        .message("邮箱格式不正确")
//                        .type("format")
//                        .build(),
//                ValidationRule.builder()
//                        .field("phone")
//                        .expression("value == null || value == '' || isMobile(value)")
//                        .message("手机号格式不正确")
//                        .type("format")
//                        .build()
//        ));
//
//        // 产品规则
//        predefinedRules.put("product", Arrays.asList(
//                ValidationRule.builder()
//                        .field("productName")
//                        .expression("required(value) && lengthBetween(value, 2, 100)")
//                        .message("产品名称不能为空，长度2-100位")
//                        .type("required")
//                        .build(),
//                ValidationRule.builder()
//                        .field("price")
//                        .expression("numberBetween(value, 0.01, 1000000)")
//                        .message("价格必须在0.01-1000000之间")
//                        .type("business")
//                        .build(),
//                ValidationRule.builder()
//                        .field("stock")
//                        .expression("numberBetween(value, 0, 100000)")
//                        .message("库存必须在0-100000之间")
//                        .type("business")
//                        .build()
//        ));
//
//        return predefinedRules.getOrDefault(ruleGroup, new ArrayList<>());
//    }
//
//    // 数据结构
//    @Data
//    @Builder
//    public static class DynamicValidationResult {
//        private boolean valid;
//        private String message;
//        private List<FieldValidationResult> fieldResults;
//        private List<RuleValidationResult> ruleResults;
//    }
//
//    @Data
//    @Builder
//    public static class BatchValidationResult {
//        private boolean valid;
//        private String message;
//        private List<FieldValidationResult> results;
//    }
//
//    @Data
//    @Builder
//    public static class FieldValidationResult {
//        private String field;
//        private Object value;
//        private boolean valid;
//        private String message;
//        private String expression;
//        private String ruleType;
//    }
//
//    @Data
//    @Builder
//    public static class RuleValidationResult {
//        private String ruleName;
//        private boolean valid;
//        private String message;
//    }
//}
//
//@Data
//@Builder
//class ApiResult<T> {
//    private boolean success;
//    private String message;
//    private T data;
//    private Long timestamp;
//}