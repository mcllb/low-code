package tcdx.uap.handler.impl;

import org.springframework.context.annotation.Description;
import org.springframework.stereotype.Service;
import tcdx.uap.handler.CodeHandler;

import javax.servlet.http.HttpSession;
import java.util.List;
import java.util.Map;

@Service
@Description("测试处理器2:只在控制台打印")
public class Test2Handler implements CodeHandler {
    @Override
    public String handle(List<Map> contextList, HttpSession httpSession, Integer userId, Map map) {
        return "====================执行了Test2Handler====================";
    }

    @Override
    public String getHandler() {
        return Test2Handler.class.getSimpleName();
    }
}
