package exmo.cy;

import exmo.cy.command.CommandHandler;
import exmo.cy.util.Logger;
import exmo.cy.web.WebApplication;

import java.util.Scanner;

/**
 * 服务器管理器主程序
 * 提供命令行界面和Web界面用于管理Minecraft服务器
 *
 * @author CyMcServerManager Team
 * @version 2.0
 */
public class ServerManagerApp {

    private static final String VERSION = "2.0";
    private static final String WELCOME_MESSAGE =
        "=================================\n" +
        "   CyMc服务器管理器 v" + VERSION + "\n" +
        "   高性能，现代化服务器管理核心\n" +
        "=================================";

    /**
     * 程序入口
     * @param args 命令行参数
     */
    public static void main(String[] args) {
        // 显示欢迎信息
        System.out.println(WELCOME_MESSAGE);
        Logger.println("输入 'help' 查看可用指令\n");

        // 检查是否启动Web模式
        if (args.length > 0 && "-web".equalsIgnoreCase(args[0])) {
            startWebMode();
        } else {
            startCommandLineMode();
        }
    }

    /**
     * 启动Web模式
     */
    private static void startWebMode() {
        Logger.println("正在启动Web模式...");
        Logger.println("服务器地址: http://localhost:5244");
        Logger.println("按 Ctrl+C 停止服务器");
        
        // 直接使用SpringApplication启动Web应用程序
        org.springframework.boot.SpringApplication.run(exmo.cy.web.WebApplication.class, new String[0]);
    }

    /**
     * 启动命令行模式
     */
    private static void startCommandLineMode() {
        // 初始化命令处理器
        Scanner scanner = new Scanner(System.in);
        CommandHandler commandHandler = new CommandHandler(scanner);

        // 主命令循环
        boolean running = true;
        while (running) {
            try {
                if (commandHandler.isAttached()) {
                    // 服务器控制台模式
                    Logger.print(commandHandler.getAttachedServerName() + "> ");
                    if (scanner.hasNextLine()) {
                        String input = scanner.nextLine().trim();
                        commandHandler.handleConsoleCommand(input);
                    } else {
                        // 输入流结束，退出程序
                        running = false;
                    }
                } else {
                    // 主控制台模式
                    Logger.print("CyMcServer> ");
                    if (scanner.hasNextLine()) {
                        String input = scanner.nextLine().trim();

                        if (input.isEmpty()) {
                            continue;
                        }

                        running = commandHandler.handleCommand(input);
                    } else {
                        // 输入流结束，退出程序
                        running = false;
                    }
                }
            } catch (Exception e) {
                Logger.error("处理命令时出现错误", e);
                Logger.println("系统将继续运行，请重试或输入 'help' 查看帮助");
            }
        }

        // 清理资源
        scanner.close();
        Logger.println("感谢使用CyMc服务器管理器，再见！");
    }
}