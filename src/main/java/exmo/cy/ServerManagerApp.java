package exmo.cy;

import exmo.cy.command.CommandHandler;
import exmo.cy.service.ServerGroupService;
import exmo.cy.service.ServerService;
import exmo.cy.util.ConsoleColor;
import exmo.cy.util.Logger;
import exmo.cy.web.WebApplication;

import org.springframework.boot.SpringApplication;

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
        // 初始化控制台颜色支持
        initializeConsoleColors();

        // 检查是否启动Web模式
        if (args.length > 0 && ("-web".equalsIgnoreCase(args[0]) || "--web".equalsIgnoreCase(args[0]))) {
            startWebMode();
        } else if (args.length > 0 && ("-console".equalsIgnoreCase(args[0]) || "--console".equalsIgnoreCase(args[0]))) {
            startCommandLineMode();
        } else {
            // 显示欢迎信息
            System.out.println(ConsoleColor.colorize(ConsoleColor.GREEN, WELCOME_MESSAGE));
            
            // 显示启动选项
            System.out.println("\n" + ConsoleColor.colorize(ConsoleColor.BRIGHT_BLUE, "启动选项:"));
            System.out.println(ConsoleColor.colorize(ConsoleColor.CYAN, "  -web 或 --web    启动Web界面模式"));
            System.out.println(ConsoleColor.colorize(ConsoleColor.CYAN, "  -console 或 --console  启动命令行模式"));
            System.out.println(ConsoleColor.colorize(ConsoleColor.YELLOW, "  直接运行（无参数）  显示此帮助信息"));

            System.out.println("\n" + ConsoleColor.colorize(ConsoleColor.GREEN, "默认使用命令行模式"));
            startCommandLineMode();

        }
    }

    /**
     * 初始化控制台颜色支持
     */
    private static void initializeConsoleColors() {
        // 检测并初始化ANSI颜色支持
        boolean isAnsiSupported = ConsoleColor.isAnsiSupported();
        if (isAnsiSupported) {
            System.setProperty("terminal.ansi", "true");
        }
        // 记录颜色初始化结果
        System.out.println(ConsoleColor.colorize(ConsoleColor.BRIGHT_BLACK, 
            "控制台颜色支持: " + (isAnsiSupported ? "已启用" : "已禁用")));
    }

    /**
     * 启动Web模式
     */
    private static void startWebMode() {
        System.out.println(ConsoleColor.colorize(ConsoleColor.GREEN, "正在启动Web界面模式..."));
        SpringApplication.run(WebApplication.class, "-web");
    }

    /**
     * 启动命令行模式
     */
    private static void startCommandLineMode() {
        System.out.println(ConsoleColor.colorize(ConsoleColor.GREEN, "正在启动命令行模式..."));
        Logger.info("启动命令行模式");
        
        try (Scanner scanner = new Scanner(System.in)) {
            CommandHandler handler = new CommandHandler(scanner);
            handler.startHandling();
        } catch (Exception e) {
            Logger.error("命令行模式出现错误", e);
        }
    }
}