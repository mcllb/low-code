package tcdx.uap.common.utils;


import com.aliyun.dysmsapi20170525.Client;
import com.aliyun.dysmsapi20170525.models.SendSmsRequest;
import com.aliyun.dysmsapi20170525.models.SendSmsResponse;
import com.aliyun.teautil.models.RuntimeOptions;
import com.aliyun.teaopenapi.models.Config;
import com.aliyun.tea.TeaException;
import java.util.concurrent.ThreadLocalRandom;
import javax.servlet.http.HttpSession;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 阿里云短信工具类
 */
public class SmsUtils {

    // 固定签名
    private static final String SIGN_NAME = "苏州零壹极";

    // 模板 ID
    private static final String TEMPLATE_CODE_CODE = "SMS_498170321";   // 参数: code
    private static final String TEMPLATE_CODE_MSG  = "SMS_495765044";   // 参数: message, time

    private static Client client;

    static {
        try {
            client = createClient();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 创建短信 Client
     */
    private static Client createClient() throws Exception {
        Config config = new Config();
        config.credential = new com.aliyun.credentials.Client(); // 使用默认凭证（环境变量/配置文件）
        config.endpoint = "dysmsapi.aliyuncs.com";
        return new Client(config);
    }

    /**
     * 发送验证码短信
     *
     * 模板: SMS_325990785
     * 参数: ${code}
     */

    public static SendSmsResponse sendCode(String phoneNumber) {
        int randomNum = ThreadLocalRandom.current().nextInt(10000) % 10000;
        String fourDigitNum = String.format("%04d", randomNum);
        String param = String.format("{\"code\":\"%s\"}", fourDigitNum );
//        session.setAttribute("random", random);
        return sendSms(phoneNumber, TEMPLATE_CODE_CODE, param);
    }
    /**
     * 发送通知短信
     *
     * 模板: SMS_495765044
     * 参数: ${message}, ${time}
     */
    public static SendSmsResponse sendMessage(String phoneNumber, String message) {
        String param = String.format("{\"display_name\":\"%s\"}", message);
        return sendSms(phoneNumber, TEMPLATE_CODE_MSG, param);
    }

    /**
     * 公共方法
     */
    private static SendSmsResponse sendSms(String phoneNumber, String templateCode, String templateParam) {
        try {
            SendSmsRequest request = new SendSmsRequest()
                    .setSignName(SIGN_NAME)
                    .setTemplateCode(templateCode)
                    .setPhoneNumbers(phoneNumber)
                    .setTemplateParam(templateParam);
            RuntimeOptions runtime = new RuntimeOptions();
            return client.sendSmsWithOptions(request, runtime);
        } catch (TeaException e) {
            System.err.println("Aliyun SMS error: " + e.getMessage());
            return null;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}

