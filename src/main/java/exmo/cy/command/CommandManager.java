package exmo.cy.command;

import exmo.cy.command.impl.*;
import exmo.cy.exception.ServerOperationException;
import exmo.cy.model.Server;
import exmo.cy.model.ServerInstance;
import exmo.cy.scheduler.TaskScheduler;
import exmo.cy.service.ServerGroupService;
import exmo.cy.service.ServerService;
import exmo.cy.util.Logger;

import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 命令管理器 - 模块化的命令系统核心
 */
public class CommandManager {
    private final ServerService serverService;
    private TaskScheduler taskScheduler;
    private final Map<String, CommandInterface> commands;
    private final Map<String, String> commandAliases;
    private final Map<Class<?>, List<RegisteredListener>> eventListeners;
    private ServerInstance attachedServer;
    private ServerGroupService serverGroupService;

    public CommandManager(ServerService serverService, ServerGroupService serverGroupService) {
        this.serverService = serverService;
        this.serverGroupService = serverGroupService;
        this.taskScheduler = new TaskScheduler(serverService);
        this.commands = new HashMap<>();
        this.commandAliases = new HashMap<>();
        this.eventListeners = new ConcurrentHashMap<>();
        this.attachedServer = null;
        
        // 注册内置命令
        registerBuiltInCommands();
    }
    
    public CommandManager(ServerService serverService) {
        this(serverService, null);
        this.taskScheduler = new TaskScheduler(serverService);
    }
    
    public void setServerGroupService(exmo.cy.service.ServerGroupService serverGroupService) {
        this.serverGroupService = serverGroupService;
    }
    
    /**
     * 注册内置命令
     */
    private void registerBuiltInCommands() {
        registerCommand(new CreateCommand(serverService));
        registerCommand(new AddCommand(serverService,serverGroupService));
        registerCommand(new SwitchCommand(serverService));
        registerCommand(new StartCommand(serverService));
        registerCommand(new LastCommand(serverService));
        registerCommand(new HelpCommand(this));
        registerCommand(new ListCommand(serverService));
        registerCommand(new MapCommand(serverService));
        registerCommand(new DeleteCommand(serverService));
        registerCommand(new AttachCommand(serverService, this));
        registerCommand(new ListRunningCommand(serverService));
        registerCommand(new BlockCommand(serverService));
        registerCommand(new ConfigCommand(serverService));
        registerCommand(new GroupCommand(serverGroupService, serverService));
        registerCommand(new CopyCommand(serverService));
        registerCommand(new StopCommand(serverService));
        registerCommand(new EStopCommand(serverService));
        registerCommand(new ForceStopCommand(serverService));
        registerCommand(new ResourceMonitorCommand(serverService));
        registerCommand(new BatchCommand(serverService));
        registerCommand(new ConfigManageCommand(serverService));
        registerCommand(new BackupRestoreCommand(serverService));
        registerCommand(new StatsCommand(serverService));
        registerCommand(new CleanupCommand(serverService));
        registerCommand(new HealthCheckCommand(serverService));
        registerCommand(new ScheduleCommand(serverService, taskScheduler));
        registerCommand(new AdvancedScheduleCommand(serverService, taskScheduler));
    }
    
    /**
     * 注册命令
     * @param command 命令实例
     */
    public void registerCommand(CommandInterface command) {
        if (command instanceof AnnotatedCommand) {
            AnnotatedCommand annotatedCmd = (AnnotatedCommand) command;
            CommandAnnotation annotation = annotatedCmd.getAnnotation();
            
            // 注册主命令名
            commands.put(annotation.name().toLowerCase(), command);
            
            // 注册别名
            for (String alias : annotation.aliases()) {
                commandAliases.put(alias.toLowerCase(), annotation.name().toLowerCase());
                commandAliases.put(alias.toUpperCase(), annotation.name().toLowerCase()); // 兼容大写输入
                commandAliases.put(capitalize(alias.toLowerCase()), annotation.name().toLowerCase()); // 首字母大写
            }
        } else {
            // 对于非注解命令，使用类名作为命令名
            String commandName = command.getClass().getSimpleName()
                    .replace("Command", "").toLowerCase();
            commands.put(commandName, command);
        }
    }
    
    /**
     * 执行命令
     * @param input 用户输入
     * @return 是否继续运行
     */
    public boolean executeCommand(String input) {
        String[] parts = input.trim().split("\\s+");
        if (parts.length == 0 || parts[0].isEmpty()) {
            Logger.println("请输入有效命令，输入 'help' 查看可用命令");
            return true;
        }
        
        String commandName = parts[0].toLowerCase();
        String[] args = Arrays.copyOfRange(parts, 1, parts.length);
        
        // 检查别名
        String actualCommandName = commandAliases.get(commandName);
        if (actualCommandName == null) {
            actualCommandName = commandName;
        }
        
        CommandInterface command = commands.get(actualCommandName);
        if (command != null) {
            try {
                return command.execute(args);
            } catch (Exception e) {
                Logger.error("执行命令时出错: " + e.getMessage(), e);
                return true;
            }
        } else {
            Logger.println("未知命令 '" + commandName + "'。输入 'help' 查看可用命令");
            return true;
        }
    }
    
    /**
     * 处理服务器控制台命令
     * @param input 输入
     * @return 是否继续运行
     */
    public boolean handleConsoleCommand(String input) {
        if ("detach".equalsIgnoreCase(input)) {
            attachedServer = null;
            Logger.println("已返回主控制台");
            return true;
        } else if ("force-stop".equalsIgnoreCase(input)) {
            handleForceStop();
            return true;
        }
        
        // 转发命令到服务器
        try {
            if (attachedServer != null) {
                serverService.sendCommand(attachedServer.getServer().getName(), input);
            } else {
                Logger.println("未连接到任何服务器，无法转发命令");
            }
        } catch (ServerOperationException e) {
            Logger.error("转发命令失败，服务器可能已关闭: " + e.getMessage());
            attachedServer = null;
        }
        return true;
    }
    
    /**
     * 处理强制停止命令
     */
    private void handleForceStop() {
        if (attachedServer == null) {
            Logger.println("错误：当前未连接到任何服务器");
            return;
        }
        
        try {
            String serverName = attachedServer.getServer().getName();
            Logger.println("正在强制终止服务器 " + serverName + "...");
            serverService.forceStopServer(serverName);
            Logger.println("服务器 " + serverName + " 已强制关闭");
            attachedServer = null;
        } catch (ServerOperationException e) {
            Logger.error("强制关闭服务器时出错: " + e.getMessage());
        }
    }
    
    /**
     * 检查是否连接到服务器
     * @return 如果已连接返回true
     */
    public boolean isAttached() {
        return attachedServer != null;
    }
    
    /**
     * 获取当前连接的服务器名称
     * @return 服务器名称
     */
    public String getAttachedServerName() {
        return attachedServer != null ? attachedServer.getServer().getName() : null;
    }
    
    /**
     * 设置连接的服务器
     * @param serverInstance 服务器实例
     */
    public void setAttachedServer(ServerInstance serverInstance) {
        this.attachedServer = serverInstance;
    }
    
    /**
     * 获取连接的服务器
     * @return 服务器实例
     */
    public ServerInstance getAttachedServer() {
        return attachedServer;
    }
    
    /**
     * 获取所有命令
     * @return 命令映射
     */
    public Map<String, CommandInterface> getCommands() {
        return new HashMap<>(commands);
    }
    
    private String capitalize(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        return Character.toUpperCase(str.charAt(0)) + str.substring(1);
    }
}