package exmo.cy.command.impl;

import exmo.cy.command.AnnotatedCommand;
import exmo.cy.command.CommandAnnotation;
import exmo.cy.service.ServerService;
import exmo.cy.util.Logger;

import java.util.Map;

@CommandAnnotation(
    name = "health",
    aliases = {"check", "ping"},
    description = "检查服务器健康状态"
)
public class HealthCheckCommand extends AnnotatedCommand {
    private final ServerService serverService;
    
    public HealthCheckCommand(ServerService serverService) {
        this.serverService = serverService;
    }
    
    @Override
    public boolean execute(String[] args) {
        Logger.println("=== 服务器健康检查 ===");
        
        try {
            var allServers = serverService.getConfigManager().loadServers();
            var activeServers = serverService.getActiveServers();
            
            if (allServers.isEmpty()) {
                Logger.println("没有配置任何服务器");
                return true;
            }
            
            Logger.println("总服务器数量: " + allServers.size());
            Logger.println("运行中服务器: " + activeServers.size());
            Logger.println("离线服务器: " + (allServers.size() - activeServers.size()));
            
            Logger.println("\n详细健康状态:");
            for (var server : allServers) {
                String serverName = server.getName();
                boolean isRunning = activeServers.containsKey(serverName);
                
                if (isRunning) {
                    // 检查服务器进程是否真的在运行
                    var serverInstanceOpt = serverService.getActiveServer(serverName);
                    if (serverInstanceOpt.isPresent()) {
                        Logger.println("  [OK] " + serverName + " (运行中)");
                    } else {
                        Logger.println("  [WARN] " + serverName + " (标记为运行但实例不存在)");
                    }
                } else {
                    Logger.println("  [OFFLINE] " + serverName + " (未运行)");
                }
            }
            
            // 检查被屏蔽的服务器
            var blockedServers = serverService.getBlockedServers();
            if (!blockedServers.isEmpty()) {
                Logger.println("\n被屏蔽的服务器 (" + blockedServers.size() + "):");
                for (String serverName : blockedServers) {
                    Logger.println("  [BLOCKED] " + serverName);
                }
            }
            
            // 系统健康指标
            Runtime runtime = Runtime.getRuntime();
            long maxMemory = runtime.maxMemory();
            long totalMemory = runtime.totalMemory();
            long freeMemory = runtime.freeMemory();
            long usedMemory = totalMemory - freeMemory;
            
            double memoryUsagePercent = (double) usedMemory / maxMemory * 100;
            
            Logger.println("\n系统健康指标:");
            Logger.println("  内存使用率: " + String.format("%.2f%%", memoryUsagePercent) + 
                         " (" + formatBytes(usedMemory) + "/" + formatBytes(maxMemory) + ")");
            Logger.println("  CPU核心数: " + runtime.availableProcessors());
            
            if (memoryUsagePercent > 80) {
                Logger.println("  [WARNING] 内存使用率较高，建议监控或增加内存分配");
            } else if (memoryUsagePercent > 90) {
                Logger.println("  [CRITICAL] 内存使用率非常高，可能导致性能问题");
            } else {
                Logger.println("  [OK] 内存使用正常");
            }
            
        } catch (Exception e) {
            Logger.println("执行健康检查时发生错误: " + e.getMessage());
            Logger.error("健康检查错误", e);
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
        return "检查服务器健康状态";
    }
    
    @Override
    public CommandAnnotation getAnnotation() {
        return getClass().getAnnotation(CommandAnnotation.class);
    }
}