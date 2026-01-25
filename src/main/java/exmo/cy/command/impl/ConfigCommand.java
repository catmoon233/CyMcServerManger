package exmo.cy.command.impl;

import exmo.cy.command.AnnotatedCommand;
import exmo.cy.command.CommandAnnotation;
import exmo.cy.exception.ConfigurationException;
import exmo.cy.model.Server;
import exmo.cy.service.ServerService;
import exmo.cy.util.Logger;

import java.util.List;
import java.util.Optional;
import java.util.Scanner;

@CommandAnnotation(
    name = "config",
    aliases = {"cfg"},
    description = "配置服务器默认启动参数"
)
public class ConfigCommand extends AnnotatedCommand {
    private final ServerService serverService;
    private final Scanner scanner = new Scanner(System.in);
    
    public ConfigCommand(ServerService serverService) {
        this.serverService = serverService;
    }
    
    @Override
    public boolean execute(String[] args) {
        if (args.length == 0) {
            showUsage();
            return true;
        }
        
        String action = args[0].toLowerCase();
        
        switch (action) {
            case "set-default-args":
            case "set-default-jvm":
            case "set-default-server":
                return setDefaultArgs(args);
            case "view":
            case "show":
                return viewConfig(args);
            default:
                Logger.println("未知的操作: " + action);
                showUsage();
                return true;
        }
    }
    
    private void showUsage() {
        Logger.println("配置服务器默认启动参数命令用法:");
        Logger.println("  config set-default-args <服务器名称> <JVM参数> <服务器参数>  - 设置服务器的默认JVM和服务器参数");
        Logger.println("  config set-default-jvm <服务器名称> <JVM参数>              - 设置服务器的默认JVM参数");
        Logger.println("  config set-default-server <服务器名称> <服务器参数>         - 设置服务器的默认服务器参数");
        Logger.println("  config view <服务器名称>                                   - 查看服务器的默认启动参数");
        Logger.println("示例:");
        Logger.println("  config set-default-args myserver \"-Xms2G -Xmx4G\" \"--world myworld\"");
        Logger.println("  config set-default-jvm myserver \"-Xms2G -Xmx4G -XX:+UseG1GC\"");
        Logger.println("  config view myserver");
    }
    
    private boolean setDefaultArgs(String[] args) {
        if (args.length < 2) {
            Logger.println("错误: 请指定服务器名称");
            showUsage();
            return true;
        }
        
        String serverName = args[1];
        
        // 检查服务器是否存在
        Optional<Server> serverOpt;
        try {
            serverOpt = serverService.getConfigManager().findServerByName(serverName);
        } catch (ConfigurationException e) {
            Logger.error("查找服务器配置失败: " + e.getMessage(), e);
            return true;
        }
        if (!serverOpt.isPresent()) {
            Logger.println("错误: 服务器 " + serverName + " 不存在");
            return true;
        }
        
        Server server = serverOpt.get();
        
        String action = args[0].toLowerCase();
        
        if ("set-default-args".equals(action)) {
            if (args.length < 4) {
                Logger.println("错误: 请指定JVM参数和服务器参数");
                showUsage();
                return true;
            }
            
            String jvmArgs = args[2];
            String serverArgs = args[3];
            
            server.setDefaultJvmArgs(jvmArgs);
            server.setDefaultServerArgs(serverArgs);
            
        } else if ("set-default-jvm".equals(action)) {
            if (args.length < 3) {
                Logger.println("错误: 请指定JVM参数");
                showUsage();
                return true;
            }
            
            String jvmArgs = args[2];
            server.setDefaultJvmArgs(jvmArgs);
            
        } else if ("set-default-server".equals(action)) {
            if (args.length < 3) {
                Logger.println("错误: 请指定服务器参数");
                showUsage();
                return true;
            }
            
            String serverArgs = args[2];
            server.setDefaultServerArgs(serverArgs);
        }
        
        try {
            serverService.getConfigManager().saveServer(server);
            Logger.println("服务器 " + serverName + " 的默认启动参数已更新");
        } catch (Exception e) {
            Logger.error("保存服务器配置失败: " + e.getMessage(), e);
        }
        
        return true;
    }
    
    private boolean viewConfig(String[] args) {
        if (args.length < 2) {
            Logger.println("错误: 请指定服务器名称");
            showUsage();
            return true;
        }
        
        String serverName = args[1];
        
        // 检查服务器是否存在
        Optional<Server> serverOpt;
        try {
            serverOpt = serverService.getConfigManager().findServerByName(serverName);
        } catch (ConfigurationException e) {
            Logger.error("查找服务器配置失败: " + e.getMessage(), e);
            return true;
        }
        if (!serverOpt.isPresent()) {
            Logger.println("错误: 服务器 " + serverName + " 不存在");
            return true;
        }
        
        Server server = serverOpt.get();
        
        Logger.println("服务器 " + serverName + " 的配置:");
        Logger.println("  名称: " + server.getName());
        Logger.println("  版本: " + server.getVersion());
        Logger.println("  描述: " + server.getDescription());
        Logger.println("  默认JVM参数: " + (server.getDefaultJvmArgs() != null ? server.getDefaultJvmArgs() : "(未设置)"));
        Logger.println("  默认服务器参数: " + (server.getDefaultServerArgs() != null ? server.getDefaultServerArgs() : "(未设置)"));
        
        return true;
    }
    
    @Override
    public String getDescription() {
        return "配置服务器默认启动参数";
    }
    
    @Override
    public CommandAnnotation getAnnotation() {
        return getClass().getAnnotation(CommandAnnotation.class);
    }
}