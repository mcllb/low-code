package tcdx.uap.common;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import tcdx.uap.service.TaskScheduleService;

import javax.servlet.http.HttpSession;

@Component
@EnableScheduling
public class TaskScheduleRunner {

    @Autowired
    private TaskScheduleService taskScheduleService;

    // 每60秒扫描一次
    @Scheduled(fixedDelay = 60_000)
    public void scanAndRunTasks() {
        taskScheduleService.executeDueTasks();
    }
}