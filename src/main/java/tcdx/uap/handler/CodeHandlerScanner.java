package tcdx.uap.handler;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.context.annotation.Description;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.*;

/**
 * 处理器扫描工具类
 */
@Component
@Slf4j
public class CodeHandlerScanner {

    private static final String BASE_PACKAGE = "tcdx.uap.handler.impl";

    private List<Map<String, String>> handlerOptions;

    /**
     * 获取所有处理器的下拉框选项
     * @return List<Map> 包含value和label的列表
     */
    public List<Map<String, String>> getHandlerOptions() {
        return new ArrayList<>(handlerOptions);
    }

    @PostConstruct
    public void init() {
        scanHandlers();
    }

    /**
     * 扫描指定包下的所有处理器类
     */
    private void scanHandlers() {
        handlerOptions = new ArrayList<>();

        try {
            ClassPathScanningCandidateComponentProvider scanner =
                    new ClassPathScanningCandidateComponentProvider(false);

            // 添加过滤器，只扫描带有@HandlerName注解的类
            scanner.addIncludeFilter(new AnnotationTypeFilter(Description.class));

            // 扫描指定包
            Set<BeanDefinition> beanDefinitions = scanner.findCandidateComponents(BASE_PACKAGE);

            for (BeanDefinition beanDefinition : beanDefinitions) {
                String className = beanDefinition.getBeanClassName();
                try {
                    Class<?> clazz = Class.forName(className);

                    // 获取@HandlerName注解的值作为label
                    Description handlerNameAnnotation = clazz.getAnnotation(Description.class);
                    if (handlerNameAnnotation != null) {
                        Map<String, String> option = new HashMap<>();
                        option.put("value", clazz.getSimpleName());  // 类名作为value
                        option.put("label", handlerNameAnnotation.value());  // 注解值作为label
                        handlerOptions.add(option);
                    }
                } catch (ClassNotFoundException e) {
                    log.error("无法加载类: {}", className, e);
                }
            }

            // 按label排序
            handlerOptions.sort(Comparator.comparing(o -> o.get("label")));

        } catch (Exception e) {
            log.error("扫描处理器类失败", e);
        }
    }
}
