package exmo.cy.command.impl;

import exmo.cy.command.AnnotatedCommand;
import exmo.cy.command.CommandAnnotation;
import exmo.cy.command.CommandExecuteEvent;
import exmo.cy.command.EventManager;
import exmo.cy.config.Constants;
import exmo.cy.model.Server;
import exmo.cy.service.ServerService;
import exmo.cy.util.ConsoleColor;
import exmo.cy.util.JavaPathFinder;
import exmo.cy.util.Logger;

import java.util.List;
import java.util.Optional;
import java.util.Scanner;

@CommandAnnotation(
    name = "start",
    aliases = {"st"},
    description = "启动服务器"
)
/**
 * 启动服务器命令
 * @author CyMcServerManager Team
 */
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
                System.out.println(ConsoleColor.colorize(ConsoleColor.RED, "错误: 没有可用服务器"));
                return true;
            }
            
            // 显示服务器列表
            System.out.println(ConsoleColor.colorize(ConsoleColor.BRIGHT_GREEN, "可用服务器："));
            for (int i = 0; i < servers.size(); i++) {
                Server server = servers.get(i);
                String serverInfo = (i + 1) + ". " + 
                    ConsoleColor.colorize(ConsoleColor.BRIGHT_CYAN, server.getName()) + 
                    " (" + ConsoleColor.colorize(ConsoleColor.BRIGHT_WHITE, server.getVersion()) + 
                    " | " + ConsoleColor.colorize(ConsoleColor.BRIGHT_YELLOW, server.getDescription()) + ")";
                System.out.println(serverInfo);
            }
            
            System.out.print(ConsoleColor.colorize(ConsoleColor.BRIGHT_BLUE, "选择服务器编号 (输入0返回): "));
            int choice;
            try {
                choice = Integer.parseInt(scanner.nextLine().trim()) - 1;
            } catch (NumberFormatException e) {
                System.out.println(ConsoleColor.colorize(ConsoleColor.RED, "错误: 请输入有效的数字"));
                return true;
            }
            
            if (choice == -1) { // 用户选择返回
                System.out.println(ConsoleColor.colorize(ConsoleColor.YELLOW, "返回主菜单"));
                return true;
            }
            
            if (choice < 0 || choice >= servers.size()) {
                System.out.println(ConsoleColor.colorize(ConsoleColor.RED, "错误: 无效的选择"));
                return true;
            }
            
            Server selectedServer = servers.get(choice);
            
            // 检查服务器是否已经在运行
            if (serverService.getActiveServer(selectedServer.getName()).isPresent()) {
                System.out.println(ConsoleColor.colorize(ConsoleColor.YELLOW, 
                    "服务器 " + selectedServer.getName() + " 已经在运行中"));
                return true;
            }
            
            // 询问启动模式
            System.out.println(ConsoleColor.colorize(ConsoleColor.BRIGHT_GREEN, 
                "\n选择启动模式:"));
            System.out.println("1. " + ConsoleColor.colorize(ConsoleColor.BRIGHT_WHITE, "核心模式 (推荐)") + 
                             " - " + ConsoleColor.colorize(ConsoleColor.BRIGHT_BLACK, "标准Minecraft服务器启动"));
            System.out.println("2. " + ConsoleColor.colorize(ConsoleColor.BRIGHT_WHITE, "模组包模式") + 
                             " - " + ConsoleColor.colorize(ConsoleColor.BRIGHT_BLACK, "适用于Forge/Fabric模组包"));
            System.out.println("3. " + ConsoleColor.colorize(ConsoleColor.BRIGHT_WHITE, "基础模式") + 
                             " - " + ConsoleColor.colorize(ConsoleColor.BRIGHT_BLACK, "最小化参数启动"));
            System.out.println("4. " + ConsoleColor.colorize(ConsoleColor.BRIGHT_WHITE, "基础模式(修复版)") + 
                             " - " + ConsoleColor.colorize(ConsoleColor.BRIGHT_BLACK, "包含额外修复参数"));
            System.out.println("5. " + ConsoleColor.colorize(ConsoleColor.BRIGHT_WHITE, "自定义模式") + 
                             " - " + ConsoleColor.colorize(ConsoleColor.BRIGHT_BLACK, "使用自定义JVM和服务器参数"));
            
            System.out.print(ConsoleColor.colorize(ConsoleColor.BRIGHT_BLUE, "选择启动模式 (1-5): "));
            int launchMode;
            try {
                launchMode = Integer.parseInt(scanner.nextLine().trim());
            } catch (NumberFormatException e) {
                System.out.println(ConsoleColor.colorize(ConsoleColor.RED, "错误: 请输入有效的数字"));
                return true;
            }
            
            if (launchMode < 1 || launchMode > 5) {
                System.out.println(ConsoleColor.colorize(ConsoleColor.RED, "错误: 无效的启动模式"));
                return true;
            }
            
            // 将用户选择的模式转换为常量
            int actualLaunchMode;
            switch (launchMode) {
                case 1: actualLaunchMode = Constants.LAUNCH_MODE_CORE; break;
                case 2: actualLaunchMode = Constants.LAUNCH_MODE_MODPACK; break;
                case 3: actualLaunchMode = Constants.LAUNCH_MODE_BASIC; break;
                case 4: actualLaunchMode = Constants.LAUNCH_MODE_BASIC_FIX; break;
                case 5: actualLaunchMode = Constants.LAUNCH_MODE_CUSTOM; break;
                default: actualLaunchMode = Constants.LAUNCH_MODE_CORE; break;
            }
            
            String javaPath = null;
            String jvmArgs = null;
            String serverArgs = null;
            
            // 如果是自定义模式，获取额外参数
            if (actualLaunchMode == Constants.LAUNCH_MODE_CUSTOM) {
                System.out.print(ConsoleColor.colorize(ConsoleColor.BRIGHT_BLUE, 
                    "输入自定义JVM参数 (可选，直接回车跳过): "));
                jvmArgs = scanner.nextLine().trim();
                if (jvmArgs.isEmpty()) {
                    jvmArgs = null;
                }
                
                System.out.print(ConsoleColor.colorize(ConsoleColor.BRIGHT_BLUE, 
                    "输入自定义服务器参数 (可选，直接回车跳过): "));
                serverArgs = scanner.nextLine().trim();
                if (serverArgs.isEmpty()) {
                    serverArgs = null;
                }
            }
            
            // 询问是否使用特定的Java路径
            System.out.print(ConsoleColor.colorize(ConsoleColor.BRIGHT_BLUE, 
                "使用特定Java路径? (y/N): "));
            String useSpecificJava = scanner.nextLine().trim().toLowerCase();
            if ("y".equals(useSpecificJava) || "yes".equals(useSpecificJava)) {
                System.out.print(ConsoleColor.colorize(ConsoleColor.BRIGHT_BLUE, 
                    "输入Java路径: "));
                javaPath = scanner.nextLine().trim();
            }
            
            // 确认启动
            System.out.println(ConsoleColor.colorize(ConsoleColor.BRIGHT_YELLOW, 
                "\n即将启动服务器:"));
            System.out.println("- 服务器: " + ConsoleColor.colorize(ConsoleColor.BRIGHT_CYAN, selectedServer.getName()));
            System.out.println("- 版本: " + ConsoleColor.colorize(ConsoleColor.BRIGHT_WHITE, selectedServer.getVersion()));
            System.out.println("- 启动模式: " + getLaunchModeName(actualLaunchMode));
            if (javaPath != null) {
                System.out.println("- Java路径: " + ConsoleColor.colorize(ConsoleColor.BRIGHT_WHITE, javaPath));
            }
            if (jvmArgs != null) {
                System.out.println("- JVM参数: " + ConsoleColor.colorize(ConsoleColor.BRIGHT_WHITE, jvmArgs));
            }
            if (serverArgs != null) {
                System.out.println("- 服务器参数: " + ConsoleColor.colorize(ConsoleColor.BRIGHT_WHITE, serverArgs));
            }
            
            System.out.print(ConsoleColor.colorize(ConsoleColor.BRIGHT_YELLOW, 
                "确认启动? (Y/n): "));
            String confirm = scanner.nextLine().trim().toLowerCase();
            if ("n".equals(confirm) || "no".equals(confirm)) {
                System.out.println(ConsoleColor.colorize(ConsoleColor.YELLOW, "启动已取消"));
                return true;
            }
            
            // 触发启动前事件
            eventManager.callEvent(new CommandExecuteEvent("start", args, "console"));
            
            // 启动服务器
            serverService.startServerWithDefaults(selectedServer, actualLaunchMode, javaPath);
            
            System.out.println(ConsoleColor.colorize(ConsoleColor.GREEN, 
                "服务器 " + selectedServer.getName() + " 启动请求已发送"));
                
            return true;
            
        } catch (Exception e) {
            System.out.println(ConsoleColor.colorize(ConsoleColor.RED, 
                "启动服务器时发生错误: " + e.getMessage()));
            e.printStackTrace();
            return false;
        }
    }
    
    @Override
    public CommandAnnotation getAnnotation() {
        return getClass().getAnnotation(CommandAnnotation.class);
    }
    
    @Override
    public String getDescription() {
        return "启动服务器";
    }
    
    private String getLaunchModeName(int launchMode) {
        switch (launchMode) {
            case Constants.LAUNCH_MODE_CORE: return ConsoleColor.colorize(ConsoleColor.BRIGHT_GREEN, "核心模式");
            case Constants.LAUNCH_MODE_MODPACK: return ConsoleColor.colorize(ConsoleColor.BRIGHT_YELLOW, "模组包模式");
            case Constants.LAUNCH_MODE_BASIC: return ConsoleColor.colorize(ConsoleColor.BRIGHT_WHITE, "基础模式");
            case Constants.LAUNCH_MODE_BASIC_FIX: return ConsoleColor.colorize(ConsoleColor.BRIGHT_CYAN, "基础模式(修复版)");
            case Constants.LAUNCH_MODE_CUSTOM: return ConsoleColor.colorize(ConsoleColor.BRIGHT_MAGENTA, "自定义模式");
            default: return "未知模式";
        }
    }
}