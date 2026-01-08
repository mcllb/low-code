package tcdx.uap.service;

import lombok.var;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class TaskPollingService {
    private final ScheduledExecutorService executorService;
    private final int pollingIntervalSeconds;
    private final TaskDao taskDao;

    public TaskPollingService(int pollingIntervalSeconds, TaskDao taskDao) {
        this.pollingIntervalSeconds = pollingIntervalSeconds;
        this.taskDao = taskDao;
        // 创建单线程的定时执行器
        this.executorService = Executors.newSingleThreadScheduledExecutor();
    }

    // 启动轮询任务
    public void startPolling() {
        executorService.scheduleAtFixedRate(this::pollTasks,
                0,
                pollingIntervalSeconds,
                TimeUnit.SECONDS);
    }

    // 停止轮询任务
    public void stopPolling() {
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
        }
    }

    // 轮询获取任务的方法
    private void pollTasks() {
        try {
            // 从数据库获取待执行的任务
            var tasks = taskDao.getPendingTasks();

            // 处理获取到的任务
            for (var task : tasks) {
                processTask(task);
            }
        } catch (Exception e) {
            // 记录异常信息，避免轮询任务终止
            System.err.println("Error polling tasks: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // 处理单个任务的方法
    private void processTask(Task task) {
        try {
            // 执行业务逻辑
            System.out.println("Processing task: " + task.getId());

            // 更新任务状态
            taskDao.markTaskAsProcessed(task.getId());
        } catch (Exception e) {
            System.err.println("Error processing task " + task.getId() + ": " + e.getMessage());
            // 处理任务失败的逻辑
            taskDao.markTaskAsFailed(task.getId(), e.getMessage());
        }
    }

    // 示例：任务数据访问接口
    public interface TaskDao {
        List<Task> getPendingTasks();
        void markTaskAsProcessed(int taskId);
        void markTaskAsFailed(int taskId, String errorMessage);
    }

    // 示例：任务实体类
    public static class Task {
        private int id;
        private String action;
        // 其他字段和getter/setter方法

        public int getId() { return id; }
        public String getAction() { return action; }
    }
}