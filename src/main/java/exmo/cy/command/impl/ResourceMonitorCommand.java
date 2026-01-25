package exmo.cy.command.impl;

import exmo.cy.command.AnnotatedCommand;
import exmo.cy.command.CommandAnnotation;
import exmo.cy.model.Server;
import exmo.cy.service.ServerService;
import exmo.cy.util.Logger;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.lang.management.OperatingSystemMXBean;
import java.util.List;
import java.util.Map;

@CommandAnnotation(
    name = "resource",
    aliases = {"monitor", "usage"},
    description = "监控服务器资源使用情况"
)
public class ResourceMonitorCommand extends AnnotatedCommand {
    private final ServerService serverService;
    
    public ResourceMonitorCommand(ServerService serverService) {
        this.serverService = serverService;
    }
    
    @Override
    public boolean execute(String[] args) {
        Logger.println("=== 系统资源使用情况 ===");
        
        // 显示系统资源
        displaySystemResources();
        
        Logger.println("");
        
        // 显示活跃服务器资源
        displayActiveServersResources();
        
        return true;
    }
    
    private void displaySystemResources() {
        OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();
        MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
        
        Logger.println("系统资源:");
        Logger.println("  操作系统: " + osBean.getName() + " " + osBean.getVersion());
        Logger.println("  CPU 核心数: " + osBean.getAvailableProcessors());
        Logger.println("  系统负载: " + String.format("%.2f", osBean.getSystemLoadAverage()));
        
        MemoryUsage heapMemory = memoryBean.getHeapMemoryUsage();
        MemoryUsage nonHeapMemory = memoryBean.getNonHeapMemoryUsage();
        
        Logger.println("  堆内存使用: " + formatBytes(heapMemory.getUsed()) + " / " + formatBytes(heapMemory.getMax()));
        Logger.println("  非堆内存使用: " + formatBytes(nonHeapMemory.getUsed()));
    }
    
    private void displayActiveServersResources() {
        Map<String, ?> activeServers = serverService.getActiveServers();
        
        if (activeServers.isEmpty()) {
            Logger.println("当前没有运行中的服务器");
            return;
        }
        
        Logger.println("活跃服务器:");
        for (Object serverNameObj : activeServers.keySet()) {
            String serverName = (String) serverNameObj;
            Logger.println("  - " + serverName + " (运行中)");
        }
    }
    
    private String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.2f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.2f MB", bytes / (1024.0 * 1024.0));
        return String.format("%.2f GB", bytes / (1024.0 * 1024.0 * 1024.0));
    }
    
    @Override
    public String getDescription() {
        return "监控服务器资源使用情况";
    }
    
    @Override
    public CommandAnnotation getAnnotation() {
        return getClass().getAnnotation(CommandAnnotation.class);
    }
}