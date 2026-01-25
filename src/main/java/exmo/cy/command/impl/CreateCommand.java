package exmo.cy.command.impl;

import exmo.cy.command.AnnotatedCommand;
import exmo.cy.command.CommandAnnotation;
import exmo.cy.config.Constants;
import exmo.cy.service.ServerService;
import exmo.cy.util.Logger;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;
import java.util.stream.Collectors;

@CommandAnnotation(
    name = "create",
    aliases = {"ct"},
    description = "创建新服务器"
)
public class CreateCommand extends AnnotatedCommand {
    private final ServerService serverService;
    
    public CreateCommand(ServerService serverService) {
        this.serverService = serverService;
    }
    
    @Override
    public boolean execute(String[] args) {
        // 实现创建服务器逻辑
        Logger.println("=== 创建新服务器 ===");
        
        try {
            // 列出可用核心文件
            List<String> coreFiles = listAvailableCores();
            if (coreFiles.isEmpty()) {
                Logger.println("错误: 在 " + Constants.CORES_DIR + " 目录中未找到核心文件 (.jar文件)");
                Logger.println("请先将服务器核心文件放入 cores 目录中");
                return true;
            }
            
            Logger.println("可用的核心文件:");
            for (int i = 0; i < coreFiles.size(); i++) {
                Logger.println((i + 1) + ". " + coreFiles.get(i));
            }
            
            Scanner scanner = new Scanner(System.in);
            Logger.print("请选择核心文件 (输入编号): ");
            String input = scanner.nextLine().trim();
            
            int selectedIndex;
            try {
                selectedIndex = Integer.parseInt(input) - 1;
                if (selectedIndex < 0 || selectedIndex >= coreFiles.size()) {
                    Logger.println("错误: 无效的选择");
                    return true;
                }
            } catch (NumberFormatException e) {
                Logger.println("错误: 请输入有效的数字");
                return true;
            }
            
            String coreName = coreFiles.get(selectedIndex);
            Logger.println("您选择了: " + coreName);
            
            Logger.print("输入服务器名称: ");
            String serverName = scanner.nextLine().trim();
            
            Logger.print("输入服务器描述: ");
            String description = scanner.nextLine().trim();
            
            Logger.print("输入服务器版本 (可选，默认为1.0.0): ");
            String version = scanner.nextLine().trim();
            if (version.isEmpty()) {
                version = "1.0.0";
            }
            
            Logger.print("输入默认JVM参数 (可选，直接回车跳过): ");
            String defaultJvmArgs = scanner.nextLine().trim();
            if (defaultJvmArgs.isEmpty()) {
                defaultJvmArgs = null;
            }
            
            Logger.print("输入默认服务器参数 (可选，直接回车跳过): ");
            String defaultServerArgs = scanner.nextLine().trim();
            if (defaultServerArgs.isEmpty()) {
                defaultServerArgs = null;
            }
            
            serverService.createServer(coreName, serverName, description, version, "", defaultJvmArgs, defaultServerArgs);
            Logger.println("服务器 " + serverName + " 创建成功！");
        } catch (Exception e) {
            Logger.error("创建服务器失败: " + e.getMessage(), e);
        }
        
        return true;
    }
    
    /**
     * 列出可用的核心文件
     * @return 核心文件列表
     */
    private List<String> listAvailableCores() {
        File coresDir = new File(Constants.CORES_DIR);
        if (!coresDir.exists() || !coresDir.isDirectory()) {
            return Arrays.asList();
        }
        
        File[] jarFiles = coresDir.listFiles((dir, name) -> name.toLowerCase().endsWith(".jar"));
        if (jarFiles == null) {
            return Arrays.asList();
        }
        
        return Arrays.stream(jarFiles)
                .map(File::getName)
                .sorted()
                .collect(Collectors.toList());
    }
    
    @Override
    public String getDescription() {
        return "创建新服务器";
    }
    
    @Override
    public CommandAnnotation getAnnotation() {
        return getClass().getAnnotation(CommandAnnotation.class);
    }
}