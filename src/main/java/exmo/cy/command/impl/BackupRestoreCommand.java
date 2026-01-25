package exmo.cy.command.impl;

import exmo.cy.command.AnnotatedCommand;
import exmo.cy.command.CommandAnnotation;
import exmo.cy.service.ServerService;
import exmo.cy.util.Logger;

import java.nio.file.Path;

@CommandAnnotation(
    name = "backuprestore",
    aliases = {"backup", "restore"},
    description = "备份和恢复服务器"
)
public class BackupRestoreCommand extends AnnotatedCommand {
    private final ServerService serverService;
    
    public BackupRestoreCommand(ServerService serverService) {
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
            case "create":
            case "make":
                return createBackup(args);
            case "restore":
                return restoreBackup(args);
            case "list":
                return listBackups();
            default:
                Logger.println("未知的备份操作: " + operation);
                showUsage();
                return true;
        }
    }
    
    private void showUsage() {
        Logger.println("备份恢复命令用法:");
        Logger.println("  backuprestore create <服务器名>                - 创建服务器备份");
        Logger.println("  backuprestore list                           - 列出所有备份");
        Logger.println("  示例:");
        Logger.println("    backuprestore create myserver");
        Logger.println("    backuprestore list");
    }
    
    private boolean createBackup(String[] args) {
        if (args.length < 2) {
            Logger.println("错误: 请指定服务器名称");
            return true;
        }
        
        String serverName = args[1];
        
        try {
            Path backupPath = serverService.createBackup(serverName);
            Logger.println("服务器 '" + serverName + "' 的备份已创建: " + backupPath.toString());
        } catch (Exception e) {
            Logger.println("创建备份失败: " + e.getMessage());
            Logger.error("创建备份失败", e);
        }
        
        return true;
    }
    
    private boolean listBackups() {
        Logger.println("注意: 当前备份列表功能需要在ServerService中实现");
        Logger.println("备份目录位于: backups/");
        Logger.println("您可以直接查看该目录以浏览现有备份");
        return true;
    }
    
    private boolean restoreBackup(String[] args) {
        Logger.println("恢复功能尚未完全实现。当前版本只支持备份创建。");
        Logger.println("要恢复备份，请手动从backups目录复制备份文件到相应的服务器目录。");
        return true;
    }
    
    @Override
    public String getDescription() {
        return "备份和恢复服务器";
    }
    
    @Override
    public CommandAnnotation getAnnotation() {
        return getClass().getAnnotation(CommandAnnotation.class);
    }
}