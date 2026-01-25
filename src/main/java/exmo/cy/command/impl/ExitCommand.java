package exmo.cy.command.impl;

import exmo.cy.command.AnnotatedCommand;
import exmo.cy.command.CommandAnnotation;
import exmo.cy.command.CommandInterface;
import exmo.cy.service.ServerService;
import exmo.cy.util.ConsoleColor;
import exmo.cy.util.Logger;
@CommandAnnotation(
        name = "stop",
        aliases = {"est"},
        description = "紧急停止指定服务器（强制终止）"
)
public class ExitCommand extends AnnotatedCommand {
    private final ServerService serverService;

    public ExitCommand(ServerService serverService) {
        this.serverService = serverService;
    }



    @Override
    public boolean execute(String[] args) {
        System.out.println(ConsoleColor.colorize(ConsoleColor.YELLOW, "正在关闭CyMc Server Manager..."));
        
        // 关闭所有服务器
        serverService.shutdownAllServers();
        
        // 延迟退出，确保所有服务器都已关闭
        try {
            Thread.sleep(2000); // 等待2秒让服务器关闭
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        System.out.println(ConsoleColor.colorize(ConsoleColor.RED, "CyMc Server Manager已关闭"));
        System.exit(0);
        return false; // 返回false以停止命令循环
    }

    @Override
    public String getDescription() {
        return "关闭CyMc Server Manager主程序";
    }



    @Override
    public CommandAnnotation getAnnotation() {
        return getClass().getAnnotation(CommandAnnotation.class);
    }
}