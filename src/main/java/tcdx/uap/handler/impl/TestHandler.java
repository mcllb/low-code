package tcdx.uap.handler.impl;

import com.aliyun.dysmsapi20170525.models.SendSmsResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Description;
import org.springframework.stereotype.Service;
import tcdx.uap.common.utils.SmsUtils;
import tcdx.uap.handler.CodeHandler;

import javax.servlet.http.HttpSession;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
@Description("测试处理器:发送短信给我")
public class TestHandler implements CodeHandler {

    @Override
    public String handle(List<Map> contextList, HttpSession httpSession, Integer userId, Map map) {
        // 在这里可以自定义操作 这里仅作演示拿来发个短信
        SendSmsResponse sendSmsResponse = SmsUtils.sendMessage("13776191878", "测试按钮");
        return "====================执行了TestHandler====================";
    }

    @Override
    public String getHandler() {
        return TestHandler.class.getSimpleName();
    }
}
