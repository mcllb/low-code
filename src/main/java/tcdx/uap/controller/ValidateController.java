//package tcdx.uap.controller;
//
//import lombok.Builder;
//import lombok.Data;
//
//import java.util.Map;
//
//@Data
//public class ValidationRequest {
//    // 要校验的数据
//    private Map<String, Object> data;
//    // 校验规则：字段名 -> 表达式
//    private Map<String, String> rules;
//    // 自定义错误消息
//    private Map<String, String> errorMessages;
//}
//
//// 字段校验结果
//@Data
//@Builder
//public class FieldValidationResult {
//    private String field;
//    private Object value;
//    private boolean valid;
//    private String message;
//    private String expression;
//}
//
//// 整体校验结果
//@Data
//@Builder
//public class ValidationResult {
//    private boolean valid;
//    private String message;
//    private List<FieldValidationResult> fieldResults;
//    private Map<String, Object> originalData;
//}
//
//// API响应格式
//@Data
//@Builder
//public class ApiResult<T> {
//    private boolean success;
//    private String message;
//    private T data;
//    private Long timestamp;
//
//    public static <T> ApiResult<T> success(T data) {
//        return ApiResult.<T>builder()
//                .success(true)
//                .message("成功")
//                .data(data)
//                .timestamp(System.currentTimeMillis())
//                .build();
//    }
//
//    public static <T> ApiResult<T> error(String message) {
//        return ApiResult.<T>builder()
//                .success(false)
//                .message(message)
//                .timestamp(System.currentTimeMillis())
//                .build();
//    }
//}