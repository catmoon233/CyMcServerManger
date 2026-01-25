package exmo.cy.command.impl;

import exmo.cy.command.AnnotatedCommand;
import exmo.cy.command.CommandAnnotation;
import exmo.cy.service.ServerService;
import exmo.cy.util.Logger;

import java.text.SimpleDateFormat;
import java.util.Date;

@CommandAnnotation(
    name = "stats",
    aliases = {"stat", "status"},
    description = "显示服务器统计信息"
)
public class StatsCommand extends AnnotatedCommand {
    private final ServerService serverService;
    
    public StatsCommand(ServerService serverService) {
        this.serverService = serverService;
    }
    
    @Override
    public boolean execute(String[] args) {
        Logger.println("=== 服务器管理器统计信息 ===");
        Logger.println("时间: " + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
        
        try {
            // 获取服务器统计信息
            var allServers = serverService.getConfigManager().loadServers();
            var activeServers = serverService.getActiveServers();
            
            Logger.println("服务器总数: " + allServers.size());
            Logger.println("运行中服务器: " + activeServers.size());
            Logger.println("已停止服务器: " + (allServers.size() - activeServers.size()));
            
            Logger.println("\n详细状态:");
            for (var server : allServers) {
                String status = activeServers.containsKey(server.getName()) ? "运行中" : "已停止";
                Logger.println("  - " + server.getName() + " (" + status + ")");
            }
            
            // 显示被屏蔽的服务器
            var blockedServers = serverService.getBlockedServers();
            if (!blockedServers.isEmpty()) {
                Logger.println("\n被屏蔽的服务器: " + blockedServers.size());
                for (String serverName : blockedServers) {
                    Logger.println("  - " + serverName);
                }
            }
            
            // 显示系统信息
            Runtime runtime = Runtime.getRuntime();
            long totalMemory = runtime.totalMemory();
            long freeMemory = runtime.freeMemory();
            long usedMemory = totalMemory - freeMemory;
            
            Logger.println("\n系统资源:");
            Logger.println("  可用处理器: " + runtime.availableProcessors());
            Logger.println("  总内存: " + formatBytes(totalMemory));
            Logger.println("  已用内存: " + formatBytes(usedMemory));
            Logger.println("  可用内存: " + formatBytes(freeMemory));
            Logger.println("  最大内存: " + formatBytes(runtime.maxMemory()));
            
        } catch (Exception e) {
            Logger.println("获取统计信息失败: " + e.getMessage());
            Logger.error("获取统计信息失败", e);
        }
        
        return true;
    }
    
    private String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.2f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.2f MB", bytes / (1024.0 * 1024.0));
        return String.format("%.2f GB", bytes / (1024.0 * 1024.0 * 1024.0));
    }
    
    @Override
    public String getDescription() {
        return "显示服务器统计信息";
    }
    
    @Override
    public CommandAnnotation getAnnotation() {
        return getClass().getAnnotation(CommandAnnotation.class);
    }
}