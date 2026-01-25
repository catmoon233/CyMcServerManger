package exmo.cy.scheduler;

import exmo.cy.service.ServerService;
import exmo.cy.util.Logger;

/**
 * 任务调度管理器
 * 用于初始化和管理任务调度器
 */
public class SchedulerManager {
    private static SchedulerManager instance;
    private TaskScheduler taskScheduler;
    
    private SchedulerManager() {
    }
    
    public static synchronized SchedulerManager getInstance() {
        if (instance == null) {
            instance = new SchedulerManager();
        }
        return instance;
    }
    
    /**
     * 初始化调度器
     */
    public void initialize(ServerService serverService) {
        if (taskScheduler == null) {
            taskScheduler = new TaskScheduler(serverService);
            Logger.info("任务调度器已初始化");
        }
    }
    
    /**
     * 获取任务调度器
     */
    public TaskScheduler getTaskScheduler() {
        if (taskScheduler == null) {
            throw new IllegalStateException("调度器未初始化，请先调用initialize方法");
        }
        return taskScheduler;
    }
    
    /**
     * 关闭调度器
     */
    public void shutdown() {
        if (taskScheduler != null) {
            taskScheduler.shutdown();
            taskScheduler = null;
            Logger.info("任务调度器已关闭");
        }
    }
}