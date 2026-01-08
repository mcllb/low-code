package tcdx.uap.common.utils;

import com.aliyun.tea.*;
import lombok.var;

public class Sample {

    /**
     * <b>description</b> :
     * <p>使用凭据初始化账号Client</p>
     * @return Client
     *
     * @throws Exception
     */
    public static com.aliyun.dysmsapi20170525.Client createClient() throws Exception {
        // 工程代码建议使用更安全的无AK方式，凭据配置方式请参见：https://help.aliyun.com/document_detail/378657.html。
        com.aliyun.credentials.Client credential = new com.aliyun.credentials.Client();
        com.aliyun.teaopenapi.models.Config config = new com.aliyun.teaopenapi.models.Config()
                .setCredential(credential);
        // Endpoint 请参考 https://api.aliyun.com/product/Dysmsapi
        config.endpoint = "dysmsapi.aliyuncs.com";
        return new com.aliyun.dysmsapi20170525.Client(config);
    }

    public static void main(String[] args_) throws Exception {

        // 发送验证码短信
//        var res1 = SmsUtils.sendCode("18036828670", "1234");
//        if (res1 != null) {
//            System.out.println("验证码短信发送结果: " + res1.getBody().getMessage());
//        }

        var res2 = SmsUtils.sendMessage("18036828670", "公文流转");
        if (res2 != null) {
            System.out.println("通知短信发送结果: " + res2.getBody().getMessage());
        }
    }
}