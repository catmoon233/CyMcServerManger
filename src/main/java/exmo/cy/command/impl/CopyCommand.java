package exmo.cy.command.impl;

import exmo.cy.command.AnnotatedCommand;
import exmo.cy.command.CommandAnnotation;
import exmo.cy.config.Constants;
import exmo.cy.exception.ConfigurationException;
import exmo.cy.exception.ServerOperationException;
import exmo.cy.model.Server;
import exmo.cy.service.ServerService;
import exmo.cy.util.FileUtils;
import exmo.cy.util.Logger;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.Scanner;

@CommandAnnotation(
    name = "copy",
    aliases = {"cp"},
    description = "复制现有服务器"
)
public class CopyCommand extends AnnotatedCommand {
    private final ServerService serverService;
    private final Scanner scanner = new Scanner(System.in);
    
    public CopyCommand(ServerService serverService) {
        this.serverService = serverService;
    }
    
    @Override
    public boolean execute(String[] args) {
        if (args.length == 0) {
            showUsage();
            return true;
        }
        
        String sourceServerName = args[0];
        
        String targetServerName;
        if (args.length > 1) {
            targetServerName = args[1];
        } else {
            Logger.print("请输入目标服务器名称: ");
            targetServerName = scanner.nextLine().trim();
        }
        
        if (targetServerName.isEmpty()) {
            Logger.println("错误: 目标服务器名称不能为空");
            return true;
        }
        
        if (sourceServerName.equals(targetServerName)) {
            Logger.println("错误: 源服务器和目标服务器名称不能相同");
            return true;
        }
        
        try {
            copyServer(sourceServerName, targetServerName);
        } catch (Exception e) {
            Logger.error("复制服务器失败: " + e.getMessage(), e);
        }
        
        return true;
    }
    
    private void showUsage() {
        Logger.println("复制服务器命令用法:");
        Logger.println("  copy <源服务器名> [目标服务器名]  - 复制现有服务器");
        Logger.println("示例:");
        Logger.println("  copy server1 server1_backup");
        Logger.println("  copy server2");
    }
    
    private void copyServer(String sourceServerName, String targetServerName) throws IOException, ConfigurationException {
        // 检查源服务器是否存在
        Optional<Server> sourceServerOpt = serverService.getConfigManager().findServerByName(sourceServerName);
        if (!sourceServerOpt.isPresent()) {
            Logger.println("错误: 源服务器 " + sourceServerName + " 不存在");
            return;
        }
        
        Server sourceServer = sourceServerOpt.get();
        
        // 检查目标服务器是否已存在
        if (serverService.getConfigManager().serverExists(targetServerName)) {
            Logger.println("错误: 目标服务器 " + targetServerName + " 已存在");
            return;
        }
        
        // 检查源服务器是否正在运行
        if (serverService.getActiveServers().containsKey(sourceServerName)) {
            Logger.println("警告: 源服务器 " + sourceServerName + " 正在运行，建议先停止后再复制");
            Logger.print("是否继续复制？(y/N): ");
            String confirm = scanner.nextLine().trim();
            if (!confirm.toLowerCase().startsWith("y")) {
                Logger.println("复制操作已取消");
                return;
            }
        }
        
        Logger.println("开始复制服务器 " + sourceServerName + " 到 " + targetServerName);
        
        // 复制服务器目录
        Path sourcePath = Paths.get(sourceServer.getCorePath()).getParent();
        Path targetPath = Paths.get(Constants.SERVERS_DIR, targetServerName);
        
        if (!Files.exists(sourcePath)) {
            Logger.println("错误: 源服务器目录不存在: " + sourcePath);
            return;
        }
        
        try {
            FileUtils.copyDirectory(sourcePath, targetPath);
        } catch (ServerOperationException e) {
            Logger.error("复制服务器目录失败: " + e.getMessage(), e);
            return;
        }
        
        // 创建新的服务器配置
        Server newServer = new Server();
        newServer.setName(targetServerName);
        newServer.setCorePath(targetPath.resolve(Constants.CORE_JAR).toString());
        newServer.setVersion(sourceServer.getVersion());
        newServer.setDescription(sourceServer.getDescription() + " (副本)");
        newServer.setModpack(sourceServer.isModpack());
        newServer.setMap(sourceServer.getMap());
        newServer.setDefaultJvmArgs(sourceServer.getDefaultJvmArgs());
        newServer.setDefaultServerArgs(sourceServer.getDefaultServerArgs());
        newServer.setGroup(sourceServer.getGroup()); // 保留原服务器的群组信息
        
        // 保存新服务器配置
        serverService.getConfigManager().saveServer(newServer);
        
        Logger.println("服务器 " + sourceServerName + " 已成功复制为 " + targetServerName);
    }
    
    @Override
    public String getDescription() {
        return "复制现有服务器";
    }
    
    @Override
    public CommandAnnotation getAnnotation() {
        return getClass().getAnnotation(CommandAnnotation.class);
    }
}