package exmo.cy.scheduler;

import exmo.cy.service.ServerService;
import exmo.cy.util.Logger;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 任务调度器
 */
public class TaskScheduler {
    private final ServerService serverService;
    private final ScheduledExecutorService scheduler;
    private final Map<String, java.util.Timer> recurringTimers;
    private final List<ScheduledTask> scheduledTasks;
    private final TaskConfigManager configManager;
    private final Object lock = new Object();
    
    public TaskScheduler(ServerService serverService) {
        this.serverService = serverService;
        this.scheduler = Executors.newScheduledThreadPool(5);
        this.recurringTimers = new HashMap<>();
        this.scheduledTasks = Collections.synchronizedList(new ArrayList<>());
        this.configManager = new TaskConfigManager();
        
        // 加载已保存的任务
        loadSavedTasks();
    }
    
    /**
     * 从配置文件加载已保存的任务
     */
    private void loadSavedTasks() {
        List<ScheduledTask> savedTasks = configManager.loadTasks();
        for (ScheduledTask task : savedTasks) {
            // 重新安排已保存的任务
            if (task.isEnabled() && !task.isRecurring()) {
                // 只重新安排一次性任务（如果是将来时间）
                if (task.getScheduledTime().isAfter(LocalDateTime.now())) {
                    scheduleOneTimeTask(task);
                    scheduledTasks.add(task);
                }
            } else if (task.isEnabled() && task.isRecurring()) {
                // 重新安排重复任务
                scheduleRecurringTask(task);
                scheduledTasks.add(task);
            }
        }
        Logger.info("已加载 " + savedTasks.size() + " 个已保存的计划任务");
    }
    
    /**
     * 添加一次性任务
     */
    public String scheduleTask(ScheduledTask task) {
        synchronized (lock) {
            String taskId = generateTaskId();
            task.setTaskId(taskId);
            
            if (task.isRecurring()) {
                // 添加重复任务
                scheduleRecurringTask(task);
            } else {
                // 添加一次性任务
                scheduleOneTimeTask(task);
            }
            
            scheduledTasks.add(task);
            configManager.addTask(task); // 保存到配置文件
            Logger.info("已添加计划任务: " + task.getTaskName() + " (ID: " + taskId + ")");
            return taskId;
        }
    }
    
    /**
     * 添加一次性任务
     */
    private void scheduleOneTimeTask(ScheduledTask task) {
        long delay = calculateDelay(task.getScheduledTime());
        if (delay <= 0) {
            Logger.warn("计划任务时间已过期: " + task.getTaskName());
            executeTask(task);
            return;
        }
        
        scheduler.schedule(() -> {
            if (task.isEnabled()) {
                executeTask(task);
                cancelTask(task.getTaskId());
            }
        }, delay, TimeUnit.MILLISECONDS);
    }
    
    /**
     * 添加重复任务
     */
    private void scheduleRecurringTask(ScheduledTask task) {
        // 简化实现：每分钟检查一次是否需要执行
        java.util.Timer timer = new java.util.Timer(true);
        TimerTask timerTask = new TimerTask() {
            @Override
            public void run() {
                if (task.isEnabled() && shouldExecuteNow(task)) {
                    executeTask(task);
                }
            }
        };
        
        timer.scheduleAtFixedRate(timerTask, 0, 60000); // 每分钟检查一次
        recurringTimers.put(task.getTaskId(), timer);
    }
    
    /**
     * 检查是否应该现在执行任务（简化版cron实现）
     */
    private boolean shouldExecuteNow(ScheduledTask task) {
        // 简化实现：这里只是检查当前时间是否接近计划时间
        // 在实际实现中，应该使用真正的cron表达式解析器
        LocalDateTime now = LocalDateTime.now();
        return now.isAfter(task.getScheduledTime()) || now.equals(task.getScheduledTime());
    }
    
    /**
     * 计算延迟时间（毫秒）
     */
    private long calculateDelay(LocalDateTime scheduledTime) {
        long currentTimeMillis = System.currentTimeMillis();
        long scheduledTimeMillis = scheduledTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
        return Math.max(0, scheduledTimeMillis - currentTimeMillis);
    }
    
    /**
     * 执行任务
     */
    private void executeTask(ScheduledTask task) {
        try {
            Logger.info("执行计划任务: " + task.getTaskName() + " (类型: " + task.getTaskType() + ")");
            
            switch (task.getTaskType()) {
                case START_SERVER:
                    startServer(task);
                    break;
                case STOP_SERVER:
                    stopServer(task);
                    break;
                case SEND_COMMAND:
                    sendCommand(task);
                    break;
                case CREATE_BACKUP:
                    createBackup(task);
                    break;
                case RESTART_SERVER:
                    restartServer(task);
                    break;
                default:
                    Logger.error("未知的任务类型: " + task.getTaskType());
            }
        } catch (Exception e) {
            Logger.error("执行计划任务失败: " + task.getTaskName() + ", 错误: " + e.getMessage(), e);
        }
    }
    
    private void startServer(ScheduledTask task) throws Exception {
        var serverOpt = serverService.getConfigManager().findServerByName(task.getServerName());
        if (serverOpt.isPresent()) {
            serverService.startServerWithDefaults(serverOpt.get(), 1, null);
            Logger.info("服务器 " + task.getServerName() + " 已启动 (由计划任务触发)");
        } else {
            Logger.error("服务器不存在: " + task.getServerName());
        }
    }
    
    private void stopServer(ScheduledTask task) throws Exception {
        serverService.stopServer(task.getServerName());
        Logger.info("服务器 " + task.getServerName() + " 已停止 (由计划任务触发)");
    }
    
    private void sendCommand(ScheduledTask task) throws Exception {
        serverService.sendCommand(task.getServerName(), task.getCommand());
        Logger.info("已向服务器 " + task.getServerName() + " 发送命令: " + task.getCommand());
    }
    
    private void createBackup(ScheduledTask task) throws Exception {
        serverService.createBackup(task.getServerName());
        Logger.info("已为服务器 " + task.getServerName() + " 创建备份");
    }
    
    private void restartServer(ScheduledTask task) throws Exception {
        stopServer(task);
        // 等待一段时间后再启动
        Thread.sleep(5000);
        startServer(task);
        Logger.info("服务器 " + task.getServerName() + " 已重启");
    }
    
    /**
     * 取消任务
     */
    public boolean cancelTask(String taskId) {
        synchronized (lock) {
            ScheduledTask task = getTaskById(taskId);
            if (task == null) {
                return false;
            }
            
            task.setEnabled(false);
            
            // 如果是重复任务，取消定时器
            if (task.isRecurring()) {
                java.util.Timer timer = recurringTimers.remove(taskId);
                if (timer != null) {
                    timer.cancel();
                }
            }
            
            boolean removed = scheduledTasks.remove(task);
            if (removed) {
                configManager.removeTask(taskId); // 从配置文件中删除
            }
            return removed;
        }
    }
    
    /**
     * 获取任务
     */
    public ScheduledTask getTaskById(String taskId) {
        synchronized (scheduledTasks) {
            return scheduledTasks.stream()
                    .filter(task -> task.getTaskId().equals(taskId))
                    .findFirst()
                    .orElse(null);
        }
    }
    
    /**
     * 获取所有任务
     */
    public List<ScheduledTask> getAllTasks() {
        synchronized (scheduledTasks) {
            return new ArrayList<>(scheduledTasks);
        }
    }
    
    /**
     * 生成任务ID
     */
    private String generateTaskId() {
        return "task_" + System.currentTimeMillis() + "_" + UUID.randomUUID().toString().substring(0, 8);
    }
    
    /**
     * 关闭调度器
     */
    public void shutdown() {
        scheduler.shutdown();
        for (java.util.Timer timer : recurringTimers.values()) {
            timer.cancel();
        }
        recurringTimers.clear();
    }
}