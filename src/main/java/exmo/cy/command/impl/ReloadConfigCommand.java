package exmo.cy.command.impl;

import exmo.cy.command.AnnotatedCommand;
import exmo.cy.command.CommandAnnotation;
import exmo.cy.model.ServerInstance;
import exmo.cy.service.ServerService;
import exmo.cy.util.ConsoleColor;
import exmo.cy.util.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.Scanner;

@CommandAnnotation(
    name = "reloadconfig",
    aliases = {"rcfg", "reload"},
    description = "重载服务器配置文件"
)
public class ReloadConfigCommand extends AnnotatedCommand {
    private final ServerService serverService;
    private final Scanner scanner = new Scanner(System.in);
    
    // 常见的重载命令映射
    private static final Map<String, String> RELOAD_COMMANDS = new HashMap<>();
    static {
        RELOAD_COMMANDS.put("server.properties", "reload");
        RELOAD_COMMANDS.put("bukkit.yml", "reload");
        RELOAD_COMMANDS.put("spigot.yml", "reload");
        RELOAD_COMMANDS.put("paper.yml", "reload");
        RELOAD_COMMANDS.put("fabric-server-launcher.properties", "reload");
        RELOAD_COMMANDS.put("forge-server.properties", "reload");
    }
    
    public ReloadConfigCommand(ServerService serverService) {
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
            case "server":
            case "server.properties":
                return reloadServerProperties(args);
            case "all":
                return reloadAllConfigs(args);
            case "custom":
                return reloadCustomConfig(args);
            default:
                // 如果只有一个参数，假设是要重载指定服务器的server.properties
                if (args.length == 1) {
                    return reloadServerProperties(new String[]{"server", args[0]});
                }
                Logger.println("未知的操作: " + action);
                showUsage();
                return true;
        }
    }
    
    private void showUsage() {
        Logger.println("重载服务器配置文件命令用法:");
        Logger.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        Logger.println("  reloadconfig server <服务器名称>     - 重载指定服务器的server.properties");
        Logger.println("  reloadconfig all <服务器名称>       - 重载指定服务器的所有配置文件");
        Logger.println("  reloadconfig custom <服务器> <文件>   - 重载指定服务器的自定义配置文件");
        Logger.println("  reloadconfig <服务器名称>           - 快捷方式，重载server.properties");
        Logger.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        Logger.println("支持的配置文件:");
        Logger.println("  server.properties, bukkit.yml, spigot.yml, paper.yml");
        Logger.println("  fabric-server-launcher.properties, forge-server.properties");
        Logger.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        Logger.println("示例:");
        Logger.println("  reloadconfig server myserver");
        Logger.println("  reloadconfig myserver");
        Logger.println("  reloadconfig all myserver");
        Logger.println("  reloadconfig custom myserver bukkit.yml");
    }
    
    private boolean reloadServerProperties(String[] args) {
        String serverName;
        if (args.length == 1) {
            // reloadconfig <serverName>
            serverName = args[0];
        } else if (args.length >= 2) {
            // reloadconfig server <serverName>
            serverName = args[1];
        } else {
            Logger.println("错误: 请指定服务器名称");
            showUsage();
            return true;
        }
        
        return reloadConfigFile(serverName, "server.properties");
    }
    
    private boolean reloadAllConfigs(String[] args) {
        if (args.length < 2) {
            Logger.println("错误: 请指定服务器名称");
            showUsage();
            return true;
        }
        
        String serverName = args[1];
        
        // 检查服务器是否在运行
        Optional<ServerInstance> instanceOpt = serverService.getActiveServer(serverName);
        if (!instanceOpt.isPresent()) {
            Logger.println("错误: 服务器 " + serverName + " 未在运行中");
            return true;
        }
        
        Logger.println("正在重载服务器 " + serverName + " 的所有配置文件...");
        
        // 发送重载命令
        try {
            serverService.sendCommand(serverName, "reload");
            Logger.println("✓ 已发送重载命令到服务器 " + serverName);
            Logger.println("  服务器将重载所有配置文件");
        } catch (Exception e) {
            Logger.println("❌ 发送重载命令失败: " + e.getMessage());
            return true;
        }
        
        return true;
    }
    
    private boolean reloadCustomConfig(String[] args) {
        if (args.length < 3) {
            Logger.println("错误: 请指定服务器名称和配置文件名");
            showUsage();
            return true;
        }
        
        String serverName = args[1];
        String configFileName = args[2];
        
        return reloadConfigFile(serverName, configFileName);
    }
    
    private boolean reloadConfigFile(String serverName, String configFileName) {
        // 检查服务器是否存在
        try {
            Optional<exmo.cy.model.Server> serverOpt = serverService.getConfigManager().findServerByName(serverName);
            if (!serverOpt.isPresent()) {
                Logger.println("错误: 服务器 " + serverName + " 不存在");
                return true;
            }
            
            exmo.cy.model.Server server = serverOpt.get();
            Path serverDir = Paths.get(server.getCorePath()).getParent();
            Path configFile = serverDir.resolve(configFileName);
            
            // 检查配置文件是否存在
            if (!Files.exists(configFile)) {
                Logger.println("警告: 配置文件 " + configFileName + " 不存在");
                Logger.println("  服务器目录: " + serverDir);
                return true;
            }
            
            // 检查服务器是否在运行
            Optional<ServerInstance> instanceOpt = serverService.getActiveServer(serverName);
            if (!instanceOpt.isPresent()) {
                Logger.println("错误: 服务器 " + serverName + " 未在运行中");
                Logger.println("  请先启动服务器，然后重载配置");
                return true;
            }
            
            // 获取重载命令
            String reloadCommand = RELOAD_COMMANDS.getOrDefault(configFileName, "reload");
            
            // 确认操作
            Logger.println("即将重载服务器 " + serverName + " 的 " + configFileName + " 配置文件");
            Logger.println("重载命令: " + reloadCommand);
            Logger.print("确认重载? (Y/n): ");
            String confirm = scanner.nextLine().trim();
            if ("n".equalsIgnoreCase(confirm) || "no".equalsIgnoreCase(confirm)) {
                Logger.println("重载操作已取消");
                return true;
            }
            
            // 发送重载命令
            try {
                serverService.sendCommand(serverName, reloadCommand);
                Logger.println("✓ 已成功发送重载命令到服务器 " + serverName);
                Logger.println("  配置文件 " + configFileName + " 将被重载");
            } catch (Exception e) {
                Logger.println("❌ 发送重载命令失败: " + e.getMessage());
                return true;
            }
            
        } catch (Exception e) {
            Logger.println("❌ 重载配置文件失败: " + e.getMessage());
            return true;
        }
        
        return true;
    }
    
    @Override
    public String getDescription() {
        return "重载服务器配置文件";
    }
    
    @Override
    public CommandAnnotation getAnnotation() {
        return getClass().getAnnotation(CommandAnnotation.class);
    }
}