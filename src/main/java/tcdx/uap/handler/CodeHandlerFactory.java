package tcdx.uap.handler;

import cn.hutool.core.collection.CollUtil;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * 代码处理工厂类
 */
@Component
public class CodeHandlerFactory implements ApplicationContextAware {

    private static Map<String, CodeHandler> measureHandlerMap = new HashMap<>();

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        Map<String, CodeHandler> beanMap = applicationContext.getBeansOfType(CodeHandler.class);
        if (CollUtil.isNotEmpty(beanMap)) {
            beanMap.forEach((k, v) -> measureHandlerMap.put(v.getHandler(), v));
        }
    }

    public CodeHandler creatMeasureHandler(String codeName) {
        CodeHandler handler = measureHandlerMap.get(codeName);
        if (Objects.isNull(handler)) {
            return null;
        }
        return handler;
    }
}
