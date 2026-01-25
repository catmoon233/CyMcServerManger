package exmo.cy.command;

import exmo.cy.service.ServerGroupService;
import exmo.cy.service.ServerService;
import exmo.cy.util.ConsoleColor;
import exmo.cy.util.Logger;

import java.util.Scanner;

/**
 * 命令处理器
 * 处理用户输入的各种命令 - 模块化和事件驱动系统
 */
public class CommandHandler {
    
    private final CommandManager commandManager;
    private final Scanner scanner;
    private boolean running;

    /**
     * 构造函数
     * @param scanner 输入扫描器
     */
    public CommandHandler(Scanner scanner) {
        this.scanner = scanner;
        this.commandManager = new CommandManager(new ServerService());
        this.running = true;
    }

    /**
     * 构造函数，支持ServerGroupService
     * @param scanner 输入扫描器
     * @param serverGroupService 服务器群组服务
     */
    public CommandHandler(Scanner scanner, ServerGroupService serverGroupService) {
        this.scanner = scanner;
        this.commandManager = new CommandManager(new ServerService(), serverGroupService);
        this.running = true;
    }

    /**
     * 设置服务器群组服务
     * @param serverGroupService 服务器群组服务
     */
    public void setServerGroupService(ServerGroupService serverGroupService) {
        commandManager.setServerGroupService(serverGroupService);
    }

    /**
     * 开始处理命令循环
     */
    public void startHandling() {
        // 显示欢迎信息和命令提示
        showWelcomeMessage();
        
        while (running) {
            try {
                // 显示彩色命令提示符
                System.out.print(ConsoleColor.colorize(ConsoleColor.BRIGHT_CYAN, "\nCyMc> "));
                String input = scanner.nextLine().trim();
                
                if (input.isEmpty()) {
                    continue;
                }
                
                // 解析并执行命令
                executeCommand(input);
                
            } catch (Exception e) {
                Logger.error("处理命令时发生错误", e);
            }
        }
    }

    /**
     * 显示欢迎信息
     */
    private void showWelcomeMessage() {
        System.out.println(ConsoleColor.colorize(ConsoleColor.GREEN, 
            "\n================================="));
        System.out.println(ConsoleColor.colorize(ConsoleColor.BRIGHT_GREEN, 
            "   欢迎使用 CyMc 服务器管理器"));
        System.out.println(ConsoleColor.colorize(ConsoleColor.BRIGHT_CYAN, 
            "   输入 'help' 查看可用命令"));
        System.out.println(ConsoleColor.colorize(ConsoleColor.GREEN, 
            "================================="));
    }

    /**
     * 执行命令
     * @param input 用户输入
     */
    private void executeCommand(String input) {
        String[] parts = input.split("\\s+");
        String commandName = parts[0].toLowerCase();
        String[] args = new String[parts.length - 1];
        System.arraycopy(parts, 1, args, 0, parts.length - 1);

        // 彩色输出命令执行信息
        System.out.println(ConsoleColor.colorize(ConsoleColor.BRIGHT_BLUE, 
            "执行命令: " + commandName));

        if ("exit".equals(commandName) || "quit".equals(commandName)) {
            exit();
            return;
        }

        CommandInterface command = commandManager.getCommands().get(commandName);
        if (command != null) {
            try {
                boolean success = command.execute(args);
                if (success) {
                    System.out.println(ConsoleColor.colorize(ConsoleColor.GREEN, 
                        "命令执行成功"));
                } else {
                    System.out.println(ConsoleColor.colorize(ConsoleColor.RED, 
                        "命令执行失败"));
                }
            } catch (Exception e) {
                Logger.error("执行命令 '" + commandName + "' 时发生错误", e);
            }
        } else {
            System.out.println(ConsoleColor.colorize(ConsoleColor.RED, 
                "未知命令: " + commandName));
            System.out.println(ConsoleColor.colorize(ConsoleColor.YELLOW, 
                "输入 'help' 查看可用命令"));
        }
    }

    /**
     * 退出程序
     */
    private void exit() {
        System.out.println(ConsoleColor.colorize(ConsoleColor.YELLOW, 
            "正在退出 CyMc 服务器管理器..."));
        running = false;
    }

    /**
     * 获取命令管理器
     * @return 命令管理器
     */
    public CommandManager getCommandManager() {
        return commandManager;
    }

    /**
     * 检查处理器是否正在运行
     * @return 如果正在运行返回true
     */
    public boolean isRunning() {
        return running;
    }
}