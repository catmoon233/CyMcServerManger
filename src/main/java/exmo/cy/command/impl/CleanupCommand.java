package exmo.cy.command.impl;

import exmo.cy.command.AnnotatedCommand;
import exmo.cy.command.CommandAnnotation;
import exmo.cy.service.ServerService;
import exmo.cy.util.Logger;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.concurrent.atomic.AtomicInteger;

@CommandAnnotation(
    name = "cleanup",
    aliases = {"clean", "purge"},
    description = "清理旧文件和缓存"
)
public class CleanupCommand extends AnnotatedCommand {
    private final ServerService serverService;
    
    public CleanupCommand(ServerService serverService) {
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
            case "backups":
                return cleanupOldBackups(args);
            case "logs":
                return cleanupOldLogs();
            case "temp":
                return cleanupTempFiles();
            case "all":
                return cleanupAll();
            default:
                Logger.println("未知的清理操作: " + operation);
                showUsage();
                return true;
        }
    }
    
    private void showUsage() {
        Logger.println("清理命令用法:");
        Logger.println("  cleanup backups [天数]                    - 清理指定天数前的备份文件（默认7天）");
        Logger.println("  cleanup logs [天数]                       - 清理指定天数前的日志文件（默认7天）");
        Logger.println("  cleanup temp                            - 清理临时文件");
        Logger.println("  cleanup all                             - 清理所有可清理的文件");
        Logger.println("  示例:");
        Logger.println("    cleanup backups 30                     - 清理30天前的备份");
        Logger.println("    cleanup logs                           - 清理7天前的日志");
    }
    
    private boolean cleanupOldBackups(String[] args) {
        int days = 7; // 默认7天
        
        if (args.length > 1) {
            try {
                days = Integer.parseInt(args[1]);
            } catch (NumberFormatException e) {
                Logger.println("错误: 天数必须是数字");
                return true;
            }
        }
        
        Logger.println("正在清理 " + days + " 天前的备份文件...");
        
        Path backupsDir = Paths.get("backups");
        if (!Files.exists(backupsDir)) {
            Logger.println("备份目录不存在，无需清理");
            return true;
        }
        
        Date cutoffDate = new Date(System.currentTimeMillis() - (long)days * 24 * 60 * 60 * 1000);
        AtomicInteger cleanedCount = new AtomicInteger();
        
        try {
            int finalDays = days;
            Files.walk(backupsDir)
                .filter(Files::isDirectory)
                .filter(dir -> {
                    try {
                        // 检查目录名称是否符合日期格式 YYYYMMDD_hhmmss
                        String dirName = dir.getFileName().toString();
                        if (dirName.matches("\\d{8}_\\d{6}")) {
                            // 解析日期并比较
                            Instant dirTime = Instant.parse(dirName.replace("_", "T").replaceFirst("(.{4})(.{2})(.{2})", "$1-$2-$3") + ":00Z");
                            return dirTime.isBefore(cutoffDate.toInstant().minus(finalDays, ChronoUnit.DAYS));
                        }
                        return false;
                    } catch (Exception e) {
                        return false; // 如果解析失败，不删除
                    }
                })
                .forEach(dir -> {
                    try {
                        deleteDirectory(dir.toFile());
                        Logger.println("已删除备份目录: " + dir.toString());
                        cleanedCount.getAndIncrement();
                    } catch (Exception e) {
                        Logger.println("删除目录失败 " + dir.toString() + ": " + e.getMessage());
                    }
                });
                
            Logger.println("完成清理，共删除 " + cleanedCount + " 个备份目录");
        } catch (IOException e) {
            Logger.println("清理备份时发生错误: " + e.getMessage());
        }
        
        return true;
    }
    
    private boolean cleanupOldLogs() {
        Logger.println("日志清理功能需要根据实际日志系统实现");
        Logger.println("此功能将在日志系统完善后启用");
        return true;
    }
    
    private boolean cleanupTempFiles() {
        Logger.println("临时文件清理功能需要根据实际临时文件位置实现");
        Logger.println("当前系统中没有明确的临时文件目录");
        return true;
    }
    
    private boolean cleanupAll() {
        Logger.println("执行全面清理...");
        cleanupOldBackups(new String[]{"backups", "7"});
        cleanupOldLogs();
        cleanupTempFiles();
        Logger.println("全面清理完成");
        return true;
    }
    
    private void deleteDirectory(File directory) throws IOException {
        if (directory.isDirectory()) {
            File[] files = directory.listFiles();
            if (files != null) {
                for (File file : files) {
                    deleteDirectory(file);
                }
            }
        }
        Files.deleteIfExists(directory.toPath());
    }
    
    @Override
    public String getDescription() {
        return "清理旧文件和缓存";
    }
    
    @Override
    public CommandAnnotation getAnnotation() {
        return getClass().getAnnotation(CommandAnnotation.class);
    }
}