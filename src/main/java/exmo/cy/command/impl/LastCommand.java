package exmo.cy.command.impl;

import exmo.cy.command.AnnotatedCommand;
import exmo.cy.command.CommandAnnotation;
import exmo.cy.model.LaunchConfig;
import exmo.cy.model.Server;
import exmo.cy.service.ServerService;
import exmo.cy.util.Logger;

import java.util.Optional;
import java.util.Scanner;

@CommandAnnotation(
    name = "last",
    aliases = {},
    description = "调用上次的参数启动服务器"
)
public class LastCommand extends AnnotatedCommand {
    private final ServerService serverService;
    private final Scanner scanner = new Scanner(System.in);
    
    public LastCommand(ServerService serverService) {
        this.serverService = serverService;
    }
    
    @Override
    public boolean execute(String[] args) {
        try {
            Optional<LaunchConfig> configOpt = serverService.getConfigManager().loadLastLaunchConfig();
            
            if (!configOpt.isPresent()) {
                Logger.println("错误: 未找到上次启动配置");
                return true;
            }
            
            LaunchConfig config = configOpt.get();
            Optional<Server> serverOpt = serverService.getConfigManager().findServerByName(config.getServerName());
            
            if (!serverOpt.isPresent()) {
                Logger.println("错误: 服务器 " + config.getServerName() + " 不存在");
                return true;
            }
            
            Server server = serverOpt.get();
            
            // 显示启动信息
            Logger.println("即将使用上次参数启动：");
            Logger.println("服务器: " + server.getName());
            Logger.println("模式: " + getLaunchModeName(config.getLaunchMode()));
            Logger.print("确定启动? (y/n): ");
            
            if (!"y".equalsIgnoreCase(scanner.nextLine().trim())) {
                Logger.println("启动已取消");
                return true;
            }
            
            // 启动服务器
            serverService.startServer(server, config.getLaunchMode(), config.getJavaPath(), 
                                     config.getJvmArgs(), config.getServerArgs());
            Logger.println("服务器 " + server.getName() + " 已启动");
            
        } catch (Exception e) {
            Logger.error("启动服务器时出错: " + e.getMessage(), e);
        }
        
        return true;
    }
    
    /**
     * 获取启动模式名称
     */
    private String getLaunchModeName(int mode) {
        switch (mode) {
            case 1: return "核心版本";
            case 2: return "整合包";
            case 3: return "基础核心版本";
            case 4: return "基础核心版本(修复)";
            case 5: return "自定义";
            default: return "未知";
        }
    }
    
    @Override
    public String getDescription() {
        return "调用上次的参数启动服务器";
    }
    
    @Override
    public CommandAnnotation getAnnotation() {
        return getClass().getAnnotation(CommandAnnotation.class);
    }
}