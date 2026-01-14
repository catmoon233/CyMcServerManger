package exmo.cy.command;

import exmo.cy.service.ServerService;
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
     * 处理命令
     * @param command 命令字符串
     * @return 是否继续运行
     */
    public boolean handleCommand(String command) {
        if (command.equalsIgnoreCase("stop")) {
            running = false;
            return false;
        }

        // 如果连接到服务器，则处理服务器控制台命令
        if (commandManager.isAttached()) {
            return commandManager.handleConsoleCommand(command);
        }

        // 执行普通命令
        return commandManager.executeCommand(command);
    }

    /**
     * 处理服务器控制台命令
     * @param input 输入
     * @return 是否继续运行
     */
    public boolean handleConsoleCommand(String input) {
        return commandManager.handleConsoleCommand(input);
    }

    /**
     * 检查是否连接到服务器
     * @return 如果已连接返回true
     */
    public boolean isAttached() {
        return commandManager.isAttached();
    }

    /**
     * 获取当前连接的服务器名称
     * @return 服务器名称
     */
    public String getAttachedServerName() {
        return commandManager.getAttachedServerName();
    }

    /**
     * 检查是否仍在运行
     * @return 如果仍在运行返回true
     */
    public boolean isRunning() {
        return running;
    }
}