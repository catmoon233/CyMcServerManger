package exmo.cy.command.impl;

import exmo.cy.command.AnnotatedCommand;
import exmo.cy.command.CommandAnnotation;
import exmo.cy.command.CommandExecuteEvent;
import exmo.cy.command.EventManager;
import exmo.cy.model.Server;
import exmo.cy.service.ServerService;
import exmo.cy.util.JavaPathFinder;
import exmo.cy.util.Logger;

import java.util.List;
import java.util.Optional;
import java.util.Scanner;

@CommandAnnotation(
    name = "start",
    aliases = {},
    description = "启动服务器"
)
public class StartCommand extends AnnotatedCommand {
    private final ServerService serverService;
    private final Scanner scanner = new Scanner(System.in);
    private final EventManager eventManager;
    
    public StartCommand(ServerService serverService) {
        this.serverService = serverService;
        this.eventManager = new EventManager(); // 在实际应用中，应该从外部传入
    }
    
    @Override
    public boolean execute(String[] args) {
        try {
            List<Server> servers = serverService.getConfigManager().loadServers();
            if (servers.isEmpty()) {
                Logger.println("错误: 没有可用服务器");
                return true;
            }
            
            // 显示服务器列表
            Logger.println("可用服务器：");
            for (int i = 0; i < servers.size(); i++) {
                Server server = servers.get(i);
                Logger.println((i + 1) + ". " + server.getName());
                Logger.println("   版本: " + server.getVersion());
                Logger.println("   描述: " + server.getDescription());
                Logger.println("   当前地图: " + (server.getMap() != null ? server.getMap() : "未设置"));
            }
            
            Logger.print("选择服务器编号: ");
            int choice = Integer.parseInt(scanner.nextLine()) - 1;
            if (choice < 0 || choice >= servers.size()) {
                Logger.println("错误: 无效的选择");
                return true;
            }
            
            Server selectedServer = servers.get(choice);
            
            // 选择Java路径
            String selectedJavaPath = selectJavaPath();
            
            // 选择启动模式
            int mode = selectLaunchMode();
            
            // 获取自定义参数（如果是自定义模式）
            String jvmArgs = null;
            String serverArgs = null;
            if (mode == 5) {
                Logger.print("输入JVM参数(以空格分隔): ");
                jvmArgs = scanner.nextLine().trim();
                Logger.print("输入服务器参数(以空格分隔): ");
                serverArgs = scanner.nextLine().trim();
            }
            
            // 触发服务器启动事件
            // eventManager.callEvent(new ServerStartEvent(selectedServer.getName()));
            
            // 启动服务器
            serverService.startServer(selectedServer, mode, selectedJavaPath, jvmArgs, serverArgs);
            Logger.println("服务器 " + selectedServer.getName() + " 已启动");
            
        } catch (Exception e) {
            Logger.error("启动服务器时出错: " + e.getMessage(), e);
        }
        
        return true;
    }
    
    /**
     * 选择Java路径
     */
    private String selectJavaPath() {
        List<String> javaPaths = JavaPathFinder.findAvailableJavaPaths();
        
        if (javaPaths.isEmpty()) {
            Logger.println("未找到可用的Java路径，将使用默认Java");
            return "java";
        }
        
        Logger.println("可用的Java路径：");
        Logger.println("0. 默认 (java)");
        for (int i = 0; i < javaPaths.size(); i++) {
            Logger.println((i + 1) + ". " + javaPaths.get(i));
        }
        
        Logger.print("请选择Java路径编号: ");
        int choice = Integer.parseInt(scanner.nextLine());
        
        if (choice < 0 || choice > javaPaths.size()) {
            Logger.println("无效的选择，使用默认Java");
            return "java";
        }
        
        return choice == 0 ? "java" : javaPaths.get(choice - 1);
    }
    
    /**
     * 选择启动模式
     */
    private int selectLaunchMode() {
        Logger.println("选择启动模式：");
        Logger.println("1. 核心版本启动");
        Logger.println("2. 整合包启动");
        Logger.println("3. 基础核心版本");
        Logger.println("4. 基础核心版本(修复)");
        Logger.println("5. 自定义启动模式");
        
        Logger.print("输入选项: ");
        return Integer.parseInt(scanner.nextLine());
    }
    
    @Override
    public String getDescription() {
        return "启动服务器";
    }
    
    @Override
    public CommandAnnotation getAnnotation() {
        return getClass().getAnnotation(CommandAnnotation.class);
    }
}