package exmo.cy.command.impl;

import exmo.cy.command.AnnotatedCommand;
import exmo.cy.command.CommandAnnotation;
import exmo.cy.scheduler.ScheduledTask;
import exmo.cy.scheduler.TaskScheduler;
import exmo.cy.service.ServerService;
import exmo.cy.util.Logger;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Scanner;

@CommandAnnotation(
    name = "advanced-schedule",
    aliases = {"asched", "atask"},
    description = "高级计划任务管理"
)
public class AdvancedScheduleCommand extends AnnotatedCommand {
    private final ServerService serverService;
    private final TaskScheduler taskScheduler;
    private final Scanner scanner = new Scanner(System.in);
    
    public AdvancedScheduleCommand(ServerService serverService, TaskScheduler taskScheduler) {
        this.serverService = serverService;
        this.taskScheduler = taskScheduler;
    }
    
    @Override
    public boolean execute(String[] args) {
        if (args.length == 0) {
            showUsage();
            return true;
        }
        
        String operation = args[0].toLowerCase();
        
        switch (operation) {
            case "enable":
                return enableTask(args);
            case "disable":
                return disableTask(args);
            case "reschedule":
                return rescheduleTask(args);
            case "list-enabled":
                return listEnabledTasks();
            case "list-disabled":
                return listDisabledTasks();
            case "clear-all":
                return clearAllTasks();
            default:
                Logger.println("未知的高级计划任务操作: " + operation);
                showUsage();
                return true;
        }
    }
    
    private void showUsage() {
        Logger.println("高级计划任务命令用法:");
        Logger.println("  advanced-schedule enable <任务ID>             - 启用任务");
        Logger.println("  advanced-schedule disable <任务ID>            - 禁用任务");
        Logger.println("  advanced-schedule reschedule <任务ID> <新时间>   - 重新安排任务时间");
        Logger.println("  advanced-schedule list-enabled               - 列出所有启用的任务");
        Logger.println("  advanced-schedule list-disabled              - 列出所有禁用的任务");
        Logger.println("  advanced-schedule clear-all                  - 清空所有任务");
        Logger.println("时间格式: yyyy-MM-dd HH:mm:ss (例如: 2023-12-25 10:30:00)");
        Logger.println("示例:");
        Logger.println("  advanced-schedule disable task_123456789");
        Logger.println("  advanced-schedule reschedule task_123456789 \"2023-12-26 10:00:00\"");
    }
    
    private boolean enableTask(String[] args) {
        if (args.length < 2) {
            Logger.println("错误: 请指定任务ID。用法: advanced-schedule enable <任务ID>");
            return true;
        }
        
        String taskId = args[1];
        ScheduledTask task = taskScheduler.getTaskById(taskId);
        
        if (task == null) {
            Logger.println("错误: 未找到任务ID: " + taskId);
            return true;
        }
        
        task.setEnabled(true);
        Logger.println("任务已启用: " + taskId);
        Logger.println("任务名称: " + task.getTaskName());
        Logger.println("服务器: " + task.getServerName());
        Logger.println("类型: " + task.getTaskType());
        
        return true;
    }
    
    private boolean disableTask(String[] args) {
        if (args.length < 2) {
            Logger.println("错误: 请指定任务ID。用法: advanced-schedule disable <任务ID>");
            return true;
        }
        
        String taskId = args[1];
        ScheduledTask task = taskScheduler.getTaskById(taskId);
        
        if (task == null) {
            Logger.println("错误: 未找到任务ID: " + taskId);
            return true;
        }
        
        task.setEnabled(false);
        Logger.println("任务已禁用: " + taskId);
        Logger.println("任务名称: " + task.getTaskName());
        Logger.println("服务器: " + task.getServerName());
        Logger.println("类型: " + task.getTaskType());
        
        return true;
    }
    
    private boolean rescheduleTask(String[] args) {
        if (args.length < 3) {
            Logger.println("错误: 请指定任务ID和新时间。用法: advanced-schedule reschedule <任务ID> <新时间>");
            return true;
        }
        
        String taskId = args[1];
        String newTimeStr = args[2];
        
        try {
            LocalDateTime newTime = parseDateTime(newTimeStr);
            
            ScheduledTask task = taskScheduler.getTaskById(taskId);
            if (task == null) {
                Logger.println("错误: 未找到任务ID: " + taskId);
                return true;
            }
            
            // 取消当前任务
            taskScheduler.cancelTask(taskId);
            
            // 创建新任务
            ScheduledTask newTask = new ScheduledTask(
                task.getTaskId(),
                task.getTaskName(),
                task.getServerName(),
                task.getCommand(),
                newTime,
                task.isRecurring(),
                task.getCronExpression(),
                task.getTaskType()
            );
            newTask.setEnabled(task.isEnabled());
            
            // 重新安排任务
            taskScheduler.scheduleTask(newTask);
            
            Logger.println("任务已重新安排:");
            Logger.println("  任务ID: " + taskId);
            Logger.println("  任务名称: " + task.getTaskName());
            Logger.println("  新时间: " + newTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            Logger.println("  服务器: " + task.getServerName());
            Logger.println("  类型: " + task.getTaskType());
            
        } catch (DateTimeParseException e) {
            Logger.println("错误: 时间格式不正确。请使用格式: yyyy-MM-dd HH:mm:ss");
        }
        
        return true;
    }
    
    private boolean listEnabledTasks() {
        List<ScheduledTask> allTasks = taskScheduler.getAllTasks();
        List<ScheduledTask> enabledTasks = new java.util.ArrayList<>();
        
        for (ScheduledTask task : allTasks) {
            if (task.isEnabled()) {
                enabledTasks.add(task);
            }
        }
        
        if (enabledTasks.isEmpty()) {
            Logger.println("没有启用的计划任务");
            return true;
        }
        
        Logger.println("启用的计划任务列表 (" + enabledTasks.size() + " 个):");
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        
        for (ScheduledTask task : enabledTasks) {
            Logger.println("  ID: " + task.getTaskId());
            Logger.println("    名称: " + task.getTaskName());
            Logger.println("    服务器: " + task.getServerName());
            Logger.println("    类型: " + task.getTaskType());
            Logger.println("    时间: " + task.getScheduledTime().format(formatter));
            if (task.getCommand() != null && !task.getCommand().isEmpty()) {
                Logger.println("    命令: " + task.getCommand());
            }
            Logger.println("  ---");
        }
        
        return true;
    }
    
    private boolean listDisabledTasks() {
        List<ScheduledTask> allTasks = taskScheduler.getAllTasks();
        List<ScheduledTask> disabledTasks = new java.util.ArrayList<>();
        
        for (ScheduledTask task : allTasks) {
            if (!task.isEnabled()) {
                disabledTasks.add(task);
            }
        }
        
        if (disabledTasks.isEmpty()) {
            Logger.println("没有禁用的计划任务");
            return true;
        }
        
        Logger.println("禁用的计划任务列表 (" + disabledTasks.size() + " 个):");
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        
        for (ScheduledTask task : disabledTasks) {
            Logger.println("  ID: " + task.getTaskId());
            Logger.println("    名称: " + task.getTaskName());
            Logger.println("    服务器: " + task.getServerName());
            Logger.println("    类型: " + task.getTaskType());
            Logger.println("    时间: " + task.getScheduledTime().format(formatter));
            if (task.getCommand() != null && !task.getCommand().isEmpty()) {
                Logger.println("    命令: " + task.getCommand());
            }
            Logger.println("  ---");
        }
        
        return true;
    }
    
    private boolean clearAllTasks() {
        Logger.print("确定要清空所有计划任务吗？(输入 'yes' 确认): ");
        String confirm = scanner.nextLine().trim();
        
        if ("yes".equalsIgnoreCase(confirm)) {
            List<ScheduledTask> allTasks = taskScheduler.getAllTasks();
            int count = allTasks.size();
            
            for (ScheduledTask task : allTasks) {
                taskScheduler.cancelTask(task.getTaskId());
            }
            
            Logger.println("已清空所有 " + count + " 个计划任务");
        } else {
            Logger.println("操作已取消");
        }
        
        return true;
    }
    
    private LocalDateTime parseDateTime(String dateTimeStr) throws DateTimeParseException {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        return LocalDateTime.parse(dateTimeStr, formatter);
    }
    
    @Override
    public String getDescription() {
        return "高级计划任务管理";
    }
    
    @Override
    public CommandAnnotation getAnnotation() {
        return getClass().getAnnotation(CommandAnnotation.class);
    }
}