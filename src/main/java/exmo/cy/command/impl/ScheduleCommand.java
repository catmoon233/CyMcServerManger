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
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

@CommandAnnotation(
    name = "schedule",
    aliases = {"task", "cron"},
    description = "管理计划任务"
)
public class ScheduleCommand extends AnnotatedCommand {
    private final ServerService serverService;
    private final TaskScheduler taskScheduler;
    private final Scanner scanner = new Scanner(System.in);
    
    public ScheduleCommand(ServerService serverService, TaskScheduler taskScheduler) {
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
            case "add":
            case "create":
                return addTask(args);
            case "remove":
            case "delete":
                return removeTask(args);
            case "list":
                return listTasks();
            case "start":
                return scheduleStartTask(args);
            case "stop":
                return scheduleStopTask(args);
            case "command":
                return scheduleCommandTask(args);
            case "backup":
                return scheduleBackupTask(args);
            case "restart":
                return scheduleRestartTask(args);
            default:
                Logger.println("未知的计划任务操作: " + operation);
                showUsage();
                return true;
        }
    }
    
    private void showUsage() {
        Logger.println("计划任务命令用法:");
        Logger.println("  schedule list                                    - 列出所有计划任务");
        Logger.println("  schedule add <任务名> <服务器名> <类型> <时间> [命令] - 添加计划任务");
        Logger.println("  schedule start <服务器名> <时间>                  - 计划启动服务器");
        Logger.println("  schedule stop <服务器名> <时间>                   - 计划停止服务器");
        Logger.println("  schedule command <服务器名> <时间> <命令>          - 计划发送命令");
        Logger.println("  schedule backup <服务器名> <时间>                 - 计划创建备份");
        Logger.println("  schedule restart <服务器名> <时间>                - 计划重启服务器");
        Logger.println("  schedule remove <任务ID>                          - 删除计划任务");
        Logger.println("时间格式: yyyy-MM-dd HH:mm:ss (例如: 2023-12-25 10:30:00)");
        Logger.println("任务类型: start, stop, command, backup, restart");
        Logger.println("示例:");
        Logger.println("  schedule start myserver \"2023-12-25 09:00:00\"");
        Logger.println("  schedule command myserver \"2023-12-25 12:00:00\" \"say 服务器将在30分钟后重启\"");
        Logger.println("  schedule backup myserver \"2023-12-25 02:00:00\"");
    }
    
    private boolean addTask(String[] args) {
        if (args.length < 5) {
            Logger.println("错误: 参数不足。用法: schedule add <任务名> <服务器名> <类型> <时间> [命令]");
            return true;
        }
        
        String taskName = args[1];
        String serverName = args[2];
        String taskTypeStr = args[3];
        String timeStr = args[4];
        String command = args.length > 5 ? String.join(" ", Arrays.copyOfRange(args, 5, args.length)) : "";
        
        try {
            LocalDateTime scheduledTime = parseDateTime(timeStr);
            ScheduledTask.TaskType taskType = parseTaskType(taskTypeStr);
            
            ScheduledTask task = new ScheduledTask(null, taskName, serverName, command, scheduledTime, taskType);
            String taskId = taskScheduler.scheduleTask(task);
            
            Logger.println("已添加计划任务:");
            Logger.println("  任务ID: " + taskId);
            Logger.println("  任务名称: " + taskName);
            Logger.println("  服务器: " + serverName);
            Logger.println("  类型: " + taskType);
            Logger.println("  执行时间: " + scheduledTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            if (!command.isEmpty()) {
                Logger.println("  命令: " + command);
            }
            
        } catch (DateTimeParseException e) {
            Logger.println("错误: 时间格式不正确。请使用格式: yyyy-MM-dd HH:mm:ss");
        } catch (IllegalArgumentException e) {
            Logger.println("错误: " + e.getMessage());
        }
        
        return true;
    }
    
    private boolean scheduleStartTask(String[] args) {
        if (args.length < 3) {
            Logger.println("错误: 请指定服务器名称和时间。用法: schedule start <服务器名> <时间>");
            return true;
        }
        
        String serverName = args[1];
        String timeStr = args[2];
        
        try {
            LocalDateTime scheduledTime = parseDateTime(timeStr);
            
            ScheduledTask task = new ScheduledTask(null, "启动-" + serverName, serverName, "", scheduledTime, ScheduledTask.TaskType.START_SERVER);
            String taskId = taskScheduler.scheduleTask(task);
            
            Logger.println("已添加启动任务:");
            Logger.println("  任务ID: " + taskId);
            Logger.println("  服务器: " + serverName);
            Logger.println("  类型: 启动服务器");
            Logger.println("  执行时间: " + scheduledTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            
        } catch (DateTimeParseException e) {
            Logger.println("错误: 时间格式不正确。请使用格式: yyyy-MM-dd HH:mm:ss");
        }
        
        return true;
    }
    
    private boolean scheduleStopTask(String[] args) {
        if (args.length < 3) {
            Logger.println("错误: 请指定服务器名称和时间。用法: schedule stop <服务器名> <时间>");
            return true;
        }
        
        String serverName = args[1];
        String timeStr = args[2];
        
        try {
            LocalDateTime scheduledTime = parseDateTime(timeStr);
            
            ScheduledTask task = new ScheduledTask(null, "停止-" + serverName, serverName, "", scheduledTime, ScheduledTask.TaskType.STOP_SERVER);
            String taskId = taskScheduler.scheduleTask(task);
            
            Logger.println("已添加停止任务:");
            Logger.println("  任务ID: " + taskId);
            Logger.println("  服务器: " + serverName);
            Logger.println("  类型: 停止服务器");
            Logger.println("  执行时间: " + scheduledTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            
        } catch (DateTimeParseException e) {
            Logger.println("错误: 时间格式不正确。请使用格式: yyyy-MM-dd HH:mm:ss");
        }
        
        return true;
    }
    
    private boolean scheduleCommandTask(String[] args) {
        if (args.length < 4) {
            Logger.println("错误: 请指定服务器名称、时间和命令。用法: schedule command <服务器名> <时间> <命令>");
            return true;
        }
        
        String serverName = args[1];
        String timeStr = args[2];
        String command = String.join(" ", Arrays.copyOfRange(args, 3, args.length));
        
        try {
            LocalDateTime scheduledTime = parseDateTime(timeStr);
            
            ScheduledTask task = new ScheduledTask(null, "命令-" + serverName, serverName, command, scheduledTime, ScheduledTask.TaskType.SEND_COMMAND);
            String taskId = taskScheduler.scheduleTask(task);
            
            Logger.println("已添加命令任务:");
            Logger.println("  任务ID: " + taskId);
            Logger.println("  服务器: " + serverName);
            Logger.println("  类型: 发送命令");
            Logger.println("  执行时间: " + scheduledTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            Logger.println("  命令: " + command);
            
        } catch (DateTimeParseException e) {
            Logger.println("错误: 时间格式不正确。请使用格式: yyyy-MM-dd HH:mm:ss");
        }
        
        return true;
    }
    
    private boolean scheduleBackupTask(String[] args) {
        if (args.length < 3) {
            Logger.println("错误: 请指定服务器名称和时间。用法: schedule backup <服务器名> <时间>");
            return true;
        }
        
        String serverName = args[1];
        String timeStr = args[2];
        
        try {
            LocalDateTime scheduledTime = parseDateTime(timeStr);
            
            ScheduledTask task = new ScheduledTask(null, "备份-" + serverName, serverName, "", scheduledTime, ScheduledTask.TaskType.CREATE_BACKUP);
            String taskId = taskScheduler.scheduleTask(task);
            
            Logger.println("已添加备份任务:");
            Logger.println("  任务ID: " + taskId);
            Logger.println("  服务器: " + serverName);
            Logger.println("  类型: 创建备份");
            Logger.println("  执行时间: " + scheduledTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            
        } catch (DateTimeParseException e) {
            Logger.println("错误: 时间格式不正确。请使用格式: yyyy-MM-dd HH:mm:ss");
        }
        
        return true;
    }
    
    private boolean scheduleRestartTask(String[] args) {
        if (args.length < 3) {
            Logger.println("错误: 请指定服务器名称和时间。用法: schedule restart <服务器名> <时间>");
            return true;
        }
        
        String serverName = args[1];
        String timeStr = args[2];
        
        try {
            LocalDateTime scheduledTime = parseDateTime(timeStr);
            
            ScheduledTask task = new ScheduledTask(null, "重启-" + serverName, serverName, "", scheduledTime, ScheduledTask.TaskType.RESTART_SERVER);
            String taskId = taskScheduler.scheduleTask(task);
            
            Logger.println("已添加重启任务:");
            Logger.println("  任务ID: " + taskId);
            Logger.println("  服务器: " + serverName);
            Logger.println("  类型: 重启服务器");
            Logger.println("  执行时间: " + scheduledTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            
        } catch (DateTimeParseException e) {
            Logger.println("错误: 时间格式不正确。请使用格式: yyyy-MM-dd HH:mm:ss");
        }
        
        return true;
    }
    
    private boolean removeTask(String[] args) {
        if (args.length < 2) {
            Logger.println("错误: 请指定任务ID。用法: schedule remove <任务ID>");
            return true;
        }
        
        String taskId = args[1];
        
        if (taskScheduler.cancelTask(taskId)) {
            Logger.println("任务已取消: " + taskId);
        } else {
            Logger.println("未找到任务: " + taskId);
        }
        
        return true;
    }
    
    private boolean listTasks() {
        List<exmo.cy.scheduler.ScheduledTask> tasks = taskScheduler.getAllTasks();
        
        if (tasks.isEmpty()) {
            Logger.println("没有计划任务");
            return true;
        }
        
        Logger.println("计划任务列表 (" + tasks.size() + " 个):");
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        
        for (exmo.cy.scheduler.ScheduledTask task : tasks) {
            String status = task.isEnabled() ? "启用" : "禁用";
            Logger.println("  ID: " + task.getTaskId());
            Logger.println("    名称: " + task.getTaskName());
            Logger.println("    服务器: " + task.getServerName());
            Logger.println("    类型: " + task.getTaskType());
            Logger.println("    时间: " + task.getScheduledTime().format(formatter));
            Logger.println("    状态: " + status);
            if (task.getCommand() != null && !task.getCommand().isEmpty()) {
                Logger.println("    命令: " + task.getCommand());
            }
            Logger.println("  ---");
        }
        
        return true;
    }
    
    private LocalDateTime parseDateTime(String dateTimeStr) throws DateTimeParseException {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        return LocalDateTime.parse(dateTimeStr, formatter);
    }
    
    private ScheduledTask.TaskType parseTaskType(String taskTypeStr) {
        switch (taskTypeStr.toLowerCase()) {
            case "start":
                return ScheduledTask.TaskType.START_SERVER;
            case "stop":
                return ScheduledTask.TaskType.STOP_SERVER;
            case "command":
                return ScheduledTask.TaskType.SEND_COMMAND;
            case "backup":
                return ScheduledTask.TaskType.CREATE_BACKUP;
            case "restart":
                return ScheduledTask.TaskType.RESTART_SERVER;
            default:
                throw new IllegalArgumentException("未知的任务类型: " + taskTypeStr + 
                    ". 支持的类型: start, stop, command, backup, restart");
        }
    }
    
    @Override
    public String getDescription() {
        return "管理计划任务";
    }
    
    @Override
    public CommandAnnotation getAnnotation() {
        return getClass().getAnnotation(CommandAnnotation.class);
    }
}