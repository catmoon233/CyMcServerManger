package exmo.cy.command.impl;

import exmo.cy.command.AnnotatedCommand;
import exmo.cy.command.CommandAnnotation;
import exmo.cy.model.Server;
import exmo.cy.model.ServerGroup;
import exmo.cy.service.ServerGroupService;
import exmo.cy.service.ServerService;
import exmo.cy.util.Logger;

import java.util.Scanner;

@CommandAnnotation(
    name = "add",
    aliases = {},
    description = "添加现有服务器目录"
)
public class AddCommand extends AnnotatedCommand {
    private final ServerService serverService;
    private final ServerGroupService serverGroupService;
    private final Scanner scanner = new Scanner(System.in);
    
    public AddCommand(ServerService serverService, ServerGroupService serverGroupService) {
        this.serverService = serverService;
        this.serverGroupService = serverGroupService;
    }
    
    @Override
    public boolean execute(String[] args) {
        Logger.println("=== 添加现有服务器 ===");
        
        Logger.print("输入服务器目录路径: ");
        String path = scanner.nextLine().trim();
        
        Logger.print("输入服务器名称: ");
        String name = scanner.nextLine().trim();
        
        Logger.print("输入服务器描述: ");
        String description = scanner.nextLine().trim();
        
        Logger.print("输入服务器版本: ");
        String version = scanner.nextLine().trim();
        
        Logger.print("输入默认JVM参数 (可选，直接回车跳过): ");
        String defaultJvmArgs = scanner.nextLine().trim();
        if (defaultJvmArgs.isEmpty()) {
            defaultJvmArgs = null;
        }
        
        Logger.print("输入默认启动服务器参数 (可选，直接回车跳过): ");
        String defaultServerArgs = scanner.nextLine().trim();
        if (defaultServerArgs.isEmpty()) {
            defaultServerArgs = null;
        }
        
        Logger.print("输入服务器所属群组 (可选，直接回车跳过): ");
        String groupName = scanner.nextLine().trim();
        if (groupName.isEmpty()) {
            groupName = null;
        }
        
        // 如果指定了群组，则使用群组的预设参数
        if (groupName != null) {
            try {
                ServerGroup group = serverGroupService.getGroup(groupName);
                if (group != null) {
                    // 使用群组的预设参数作为默认参数
                    if (defaultJvmArgs == null && group.getPresetJvmArgs() != null) {
                        defaultJvmArgs = group.getPresetJvmArgs();
                    }
                    if (defaultServerArgs == null && group.getPresetServerArgs() != null) {
                        defaultServerArgs = group.getPresetServerArgs();
                    }
                    Logger.println("服务器将使用群组 '" + groupName + "' 的预设参数");
                } else {
                    Logger.println("警告: 指定的群组不存在，将创建普通服务器");
                    groupName = null;
                }
            } catch (Exception e) {
                Logger.error("获取群组信息失败: " + e.getMessage(), e);
                groupName = null;
            }
        }
        
        try {
            Server server = serverService.addExistingServer(path, name, version, description, defaultJvmArgs, defaultServerArgs);
            
            // 如果指定了群组，则将服务器添加到该群组
            if (groupName != null) {
                server.setGroup(groupName);
                serverService.getConfigManager().saveServer(server); // 保存群组信息到服务器配置
            }
            
            Logger.println("服务器 " + name + " 添加成功！");
        } catch (Exception e) {
            Logger.error("添加服务器失败: " + e.getMessage(), e);
        }
        
        return true;
    }
    
    @Override
    public String getDescription() {
        return "添加现有服务器目录";
    }
    
    @Override
    public CommandAnnotation getAnnotation() {
        return getClass().getAnnotation(CommandAnnotation.class);
    }
}