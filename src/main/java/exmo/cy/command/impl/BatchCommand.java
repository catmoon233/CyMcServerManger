package exmo.cy.command.impl;

import exmo.cy.command.AnnotatedCommand;
import exmo.cy.command.CommandAnnotation;
import exmo.cy.service.ServerService;
import exmo.cy.util.Logger;

import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

@CommandAnnotation(
    name = "batch",
    aliases = {"multi", "bulk"},
    description = "批量操作多个服务器"
)
public class BatchCommand extends AnnotatedCommand {
    private final ServerService serverService;
    private final Scanner scanner = new Scanner(System.in);
    
    public BatchCommand(ServerService serverService) {
        this.serverService = serverService;
    }
    
    @Override
    public boolean execute(String[] args) {
        if (args.length == 0) {
            showUsage();
            return true;
        }
        
        String operation = args[0].toLowerCase();
        
        switch (operation) {
            case "start":
                return batchStart(args);
            case "stop":
                return batchStop(args);
            case "command":
                return batchSendCommand(args);
            case "list":
                return batchList(args);
            default:
                Logger.println("未知的批量操作: " + operation);
                showUsage();
                return true;
        }
    }
    
    private void showUsage() {
        Logger.println("批量操作命令用法:");
        Logger.println("  batch start <服务器名1> <服务器名2> ...      - 批量启动服务器");
        Logger.println("  batch stop <服务器名1> <服务器名2> ...       - 批量停止服务器");
        Logger.println("  batch command <服务器名1> <服务器名2> ... <命令>  - 批量发送命令到服务器");
        Logger.println("  batch list                                   - 列出所有服务器及其状态");
        Logger.println("  示例:");
        Logger.println("    batch start server1 server2 server3");
        Logger.println("    batch stop server1 server2");
        Logger.println("    batch command server1 server2 op player1");
    }
    
    private boolean batchStart(String[] args) {
        if (args.length < 2) {
            Logger.println("错误: 请指定至少一个服务器名称");
            return true;
        }
        
        Logger.println("正在批量启动服务器...");
        for (int i = 1; i < args.length; i++) {
            String serverName = args[i];
            try {
                // 尝试启动服务器（使用默认设置）
                var serverOpt = serverService.getConfigManager().findServerByName(serverName);
                if (serverOpt.isPresent()) {
                    serverService.startServerWithDefaults(serverOpt.get(), 1, null);
                    Logger.println("  - 服务器 " + serverName + " 启动命令已发送");
                } else {
                    Logger.println("  - 服务器 " + serverName + " 不存在");
                }
            } catch (Exception e) {
                Logger.println("  - 服务器 " + serverName + " 启动失败: " + e.getMessage());
            }
        }
        
        return true;
    }
    
    private boolean batchStop(String[] args) {
        if (args.length < 2) {
            Logger.println("错误: 请指定至少一个服务器名称");
            return true;
        }
        
        Logger.println("正在批量停止服务器...");
        for (int i = 1; i < args.length; i++) {
            String serverName = args[i];
            try {
                serverService.stopServer(serverName);
                Logger.println("  - 服务器 " + serverName + " 停止命令已发送");
            } catch (Exception e) {
                Logger.println("  - 服务器 " + serverName + " 停止失败: " + e.getMessage());
            }
        }
        
        return true;
    }
    
    private boolean batchSendCommand(String[] args) {
        if (args.length < 3) {
            Logger.println("错误: 请指定至少一个服务器名称和一个命令");
            return true;
        }
        
        // 最后一个参数是命令，其余是服务器名称
        String command = String.join(" ", Arrays.copyOfRange(args, args.length - 1, args.length));
        String[] serverNames = Arrays.copyOfRange(args, 1, args.length - 1);
        
        if (serverNames.length < 1) {
            Logger.println("错误: 请指定至少一个服务器名称");
            return true;
        }
        
        Logger.println("正在向服务器批量发送命令: " + command);
        for (String serverName : serverNames) {
            try {
                serverService.sendCommand(serverName, command);
                Logger.println("  - 命令已发送到服务器 " + serverName);
            } catch (Exception e) {
                Logger.println("  - 发送到服务器 " + serverName + " 失败: " + e.getMessage());
            }
        }
        
        return true;
    }
    
    private boolean batchList(String[] args) {
        try {
            var allServers = serverService.getConfigManager().loadServers();
            var activeServers = serverService.getActiveServers();
            
            Logger.println("服务器列表:");
            for (var server : allServers) {
                String status = activeServers.containsKey(server.getName()) ? "运行中" : "已停止";
                Logger.println("  - " + server.getName() + " (" + status + ")");
            }
            
            if (allServers.isEmpty()) {
                Logger.println("  没有配置任何服务器");
            }
        } catch (Exception e) {
            Logger.println("获取服务器列表失败: " + e.getMessage());
        }
        
        return true;
    }
    
    @Override
    public String getDescription() {
        return "批量操作多个服务器";
    }
    
    @Override
    public CommandAnnotation getAnnotation() {
        return getClass().getAnnotation(CommandAnnotation.class);
    }
}