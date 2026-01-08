package tcdx.uap.Interceptor;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import tcdx.uap.mapper.SystemMapper;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.PrintWriter;
import java.util.Map;

/**
 * 登录拦截器
 */
@Component
public class AuthInterceptor implements HandlerInterceptor {

    @Autowired
    private SystemMapper systemMapper;

    private static final String[] EXCLUDED_PATHS = {"/login","/uap/service/get_views", "/static", "/public", "/favicon.ico","/uap/service/btn_click"}; // 免拦截路径

    @Override
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response,
                             Object handler) throws Exception {
        // 获取请求的URL路径
        String requestURI = request.getRequestURI();

        // 判断请求路径是否在免拦截列表中
        for (String excludedPath : EXCLUDED_PATHS) {
            if (requestURI.startsWith(excludedPath)) {
                return true; // 如果匹配免拦截路径，直接返回true，跳过认证检查
            }
        }

        HttpSession session = request.getSession(false); // 不自动创建新Session
        if (session == null || session.getAttribute("userId") == null) {
            // 报错：未登录
            returnError(response, "未登录");
            return false;
        } else {
            // 校验userId是否合法
            Integer userId;
            try {
                userId = (Integer) session.getAttribute("userId");
            } catch (Exception e) {
                // 报错：登录信息异常，请重新登录
                returnError(response, "登录信息异常，请重新登录");
                return false;
            }

            // 查询数据库，判断userId是否存在
            Map map = systemMapper.selectUserById(userId);
            if (map == null) {
                // 报错：登录信息异常，请重新登录
                returnError(response, "登录信息异常，请重新登录");
                return false;
            }
        }
        return true;
    }

    // 返回code=401，让前端跳转到登录页面
    private void returnError(HttpServletResponse response, String message) throws Exception {
        response.setContentType("application/json;charset=UTF-8");
        PrintWriter out = response.getWriter();
        out.write("{\"code\":401, \"message\":\"" + message + "\", \"redirect\":\"/login\"}");
        out.flush();
    }
}
