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
import java.util.*;
import java.util.stream.Collectors;

@CommandAnnotation(
    name = "copy",
    aliases = {"cp"},
    description = "复制现有服务器或从模板复制文件夹"
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
        
        String firstArg = args[0].toLowerCase();
        
        // 检查是否是模板模式
        if ("template".equals(firstArg) || "tmpl".equals(firstArg)) {
            return handleTemplateMode(args);
        }
        
        // 否则执行完整复制模式
        if (args.length < 2) {
            Logger.println("错误: 请指定源服务器和目标服务器");
            showUsage();
            return true;
        }
        
        String sourceServerName = args[0];
        String targetServerName = args[1];
        
        try {
            copyServer(sourceServerName, targetServerName);
        } catch (Exception e) {
            Logger.error("复制服务器失败: " + e.getMessage(), e);
        }
        
        return true;
    }
    
    /**
     * 处理模板复制模式
     */
    private boolean handleTemplateMode(String[] args) {
        if (args.length < 3) {
            Logger.println("错误: 模板模式需要源服务器和目标服务器");
            Logger.println("用法: copy template <源服务器> <目标服务器> [options]");
            Logger.println("选项:");
            Logger.println("  --folders <folder1,folder2,...>  指定要复制的文件夹，可选值: mods, config, world, plugins");
            Logger.println("  --overwrite                       覆盖已存在的文件");
            return true;
        }
        
        String sourceServerName = args[1];
        String targetServerName = args[2];
        
        // 解析选项
        List<String> foldersToCopy = new ArrayList<>(Arrays.asList("mods", "config", "world", "plugins"));
        boolean overwrite = false;
        
        for (int i = 3; i < args.length; i++) {
            if ("--folders".equals(args[i]) && i + 1 < args.length) {
                i++;
                String folderArg = args[i];
                foldersToCopy = Arrays.stream(folderArg.split(","))
                    .map(String::trim)
                    .filter(f -> !f.isEmpty())
                    .collect(Collectors.toList());
            } else if ("--overwrite".equals(args[i])) {
                overwrite = true;
            }
        }
        
        if (foldersToCopy.isEmpty()) {
            Logger.println("错误: 没有指定要复制的文件夹");
            return true;
        }
        
        try {
            copyFromTemplate(sourceServerName, targetServerName, foldersToCopy, overwrite);
        } catch (Exception e) {
            Logger.error("复制模板文件夹失败: " + e.getMessage(), e);
        }
        
        return true;
    }
    
    /**
     * 从模板服务器复制指定的文件夹
     */
    private void copyFromTemplate(String sourceServerName, String targetServerName, 
                                   List<String> foldersToCopy, boolean overwrite)
            throws ServerOperationException, ConfigurationException {
        
        // 检查源服务器和目标服务器是否存在
        Optional<Server> sourceOpt = serverService.getConfigManager().findServerByName(sourceServerName);
        Optional<Server> targetOpt = serverService.getConfigManager().findServerByName(targetServerName);
        
        if (!sourceOpt.isPresent()) {
            throw new ServerOperationException("模板服务器不存在: " + sourceServerName);
        }
        if (!targetOpt.isPresent()) {
            throw new ServerOperationException("目标服务器不存在: " + targetServerName);
        }
        
        Server sourceServer = sourceOpt.get();
        Server targetServer = targetOpt.get();
        
        // 获取服务器根目录
        Path sourceServerDir = Paths.get(sourceServer.getCorePath()).getParent();
        Path targetServerDir = Paths.get(targetServer.getCorePath()).getParent();
        
        if (!Files.exists(sourceServerDir)) {
            throw new ServerOperationException("模板服务器目录不存在: " + sourceServerDir);
        }
        if (!Files.exists(targetServerDir)) {
            throw new ServerOperationException("目标服务器目录不存在: " + targetServerDir);
        }
        
        Logger.println("开始从 " + sourceServerName + " 复制文件夹到 " + targetServerName);
        Logger.println("要复制的文件夹: " + String.join(", ", foldersToCopy));
        Logger.println("覆盖意存在的文件: " + (overwrite ? "是" : "否"));
        
        // 复制每个指定的文件夹
        for (String folderName : foldersToCopy) {
            Path sourceFolder = sourceServerDir.resolve(folderName);
            Path targetFolder = targetServerDir.resolve(folderName);
            
            if (!Files.exists(sourceFolder)) {
                Logger.warn("模板服务器中不存在文件夹: " + folderName + "，跳过");
                continue;
            }
            
            try {
                if (Files.exists(targetFolder) && !overwrite) {
                    Logger.warn("目标服务器中文件夹 " + folderName + " 已存在且不覆盖，跳过");
                    continue;
                }
                
                // 删除目标文件夹（如果存在且需要覆盖）
                if (Files.exists(targetFolder) && overwrite) {
                    FileUtils.deleteDirectory(targetFolder);
                    Logger.info("已删除目标服务器中的旧文件夹: " + folderName);
                }
                
                // 复制文件夹
                FileUtils.copyDirectory(sourceFolder, targetFolder);
                Logger.println("✓ 已成功复制文件夹: " + folderName);
                
            } catch (Exception e) {
                Logger.error("复制文件夹失败: " + folderName + ", 错误: " + e.getMessage());
                throw new ServerOperationException("复制文件夹 " + folderName + " 失败: " + e.getMessage(), e);
            }
        }
        
        Logger.println("✓ 服务器文件夹复制完成");
    }
    
    private void showUsage() {
        Logger.println("复制服务器命令用法:");
        Logger.println("  copy <源服务器> <目标服务器>           - 完整复制服务器");
        Logger.println("  copy template <源> <目标> [options]   - 从模板复制指定文件夹");
        Logger.println("");
        Logger.println("完整复制示例:");
        Logger.println("  copy server1 server1_backup");
        Logger.println("");
        Logger.println("模板复制示例:");
        Logger.println("  copy template server1 server2");
        Logger.println("  copy template server1 server2 --folders mods,config");
        Logger.println("  copy template server1 server2 --folders mods config world --overwrite");
        Logger.println("");
        Logger.println("可用的文件夹: mods, config, world, plugins");
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
        
        Logger.println("✓ 服务器 " + sourceServerName + " 已成功复制为 " + targetServerName);
    }
    
    @Override
    public String getDescription() {
        return "复制现有服务器或从模板复制文件夹";
    }
    
    @Override
    public CommandAnnotation getAnnotation() {
        return getClass().getAnnotation(CommandAnnotation.class);
    }
}