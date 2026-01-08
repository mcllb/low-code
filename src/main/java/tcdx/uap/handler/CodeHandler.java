package tcdx.uap.handler;

import org.springframework.stereotype.Component;

import javax.servlet.http.HttpSession;
import java.util.List;
import java.util.Map;

/**
 * 后台代码接口
 */
@Component
public interface CodeHandler {
    /**
     * 处理代码
     * @param contextList 上下文
     * @param httpSession httpSession
     * @param userId 用户ID
     * @return 处理结果
     */
    String handle(List<Map> contextList, HttpSession httpSession, Integer userId, Map map);


    /**
     * 获取处理类
     */
    String getHandler();
}
