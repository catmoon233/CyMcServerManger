package exmo.cy.scheduler;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * 计划任务实体
 */
public class ScheduledTask {
    private String taskId;
    private String taskName;
    private String serverName;
    private String command;
    private LocalDateTime scheduledTime;
    private LocalDateTime createdTime;
    private boolean recurring;
    private String cronExpression; // 用于重复任务
    private boolean enabled;
    private TaskType taskType;
    
    public enum TaskType {
        START_SERVER,     // 启动服务器
        STOP_SERVER,      // 停止服务器
        SEND_COMMAND,     // 发送命令
        CREATE_BACKUP,    // 创建备份
        RESTART_SERVER    // 重启服务器
    }
    
    public ScheduledTask() {
    }
    
    public ScheduledTask(String taskId, String taskName, String serverName, String command, 
                        LocalDateTime scheduledTime, TaskType taskType) {
        this.taskId = taskId;
        this.taskName = taskName;
        this.serverName = serverName;
        this.command = command;
        this.scheduledTime = scheduledTime;
        this.createdTime = LocalDateTime.now();
        this.recurring = false;
        this.enabled = true;
        this.taskType = taskType;
    }
    
    public ScheduledTask(String taskId, String taskName, String serverName, String command, 
                        LocalDateTime scheduledTime, boolean recurring, String cronExpression, 
                        TaskType taskType) {
        this(taskId, taskName, serverName, command, scheduledTime, taskType);
        this.recurring = recurring;
        this.cronExpression = cronExpression;
    }
    
    // Getters and Setters
    public String getTaskId() {
        return taskId;
    }
    
    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }
    
    public String getTaskName() {
        return taskName;
    }
    
    public void setTaskName(String taskName) {
        this.taskName = taskName;
    }
    
    public String getServerName() {
        return serverName;
    }
    
    public void setServerName(String serverName) {
        this.serverName = serverName;
    }
    
    public String getCommand() {
        return command;
    }
    
    public void setCommand(String command) {
        this.command = command;
    }
    
    public LocalDateTime getScheduledTime() {
        return scheduledTime;
    }
    
    public void setScheduledTime(LocalDateTime scheduledTime) {
        this.scheduledTime = scheduledTime;
    }
    
    public LocalDateTime getCreatedTime() {
        return createdTime;
    }
    
    public void setCreatedTime(LocalDateTime createdTime) {
        this.createdTime = createdTime;
    }
    
    public boolean isRecurring() {
        return recurring;
    }
    
    public void setRecurring(boolean recurring) {
        this.recurring = recurring;
    }
    
    public String getCronExpression() {
        return cronExpression;
    }
    
    public void setCronExpression(String cronExpression) {
        this.cronExpression = cronExpression;
    }
    
    public boolean isEnabled() {
        return enabled;
    }
    
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
    
    public TaskType getTaskType() {
        return taskType;
    }
    
    public void setTaskType(TaskType taskType) {
        this.taskType = taskType;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ScheduledTask that = (ScheduledTask) o;
        return Objects.equals(taskId, that.taskId);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(taskId);
    }
    
    @Override
    public String toString() {
        return "ScheduledTask{" +
                "taskId='" + taskId + '\'' +
                ", taskName='" + taskName + '\'' +
                ", serverName='" + serverName + '\'' +
                ", command='" + command + '\'' +
                ", scheduledTime=" + scheduledTime +
                ", createdTime=" + createdTime +
                ", recurring=" + recurring +
                ", cronExpression='" + cronExpression + '\'' +
                ", enabled=" + enabled +
                ", taskType=" + taskType +
                '}';
    }
}