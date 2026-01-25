package exmo.cy.command.impl;

import exmo.cy.command.AnnotatedCommand;
import exmo.cy.command.CommandAnnotation;
import exmo.cy.model.ServerInstance;
import exmo.cy.service.ServerService;
import exmo.cy.util.ConsoleColor;
import exmo.cy.util.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

@CommandAnnotation(
    name = "n-stop",
    aliases = {"terminate", "stop-server", "ss"},
    description = "正常停止指定服务器"
)
public class StopCommand extends AnnotatedCommand {
    private final ServerService serverService;
    private final Scanner scanner = new Scanner(System.in);
    
    public StopCommand(ServerService serverService) {
        this.serverService = serverService;
    }
    
    @Override
    public boolean execute(String[] args) {
        Map<String, ServerInstance> activeServers = serverService.getActiveServers();
        if (activeServers.isEmpty()) {
            System.out.println(ConsoleColor.colorize(ConsoleColor.YELLOW, "没有运行中的服务器"));
            return true;
        }
        
        List<ServerInstance> serverList = new ArrayList<>(activeServers.values());
        System.out.println(ConsoleColor.colorize(ConsoleColor.BRIGHT_GREEN, "运行中的服务器："));
        for (int i = 0; i < serverList.size(); i++) {
            String serverInfo = (i + 1) + ". " + ConsoleColor.colorize(ConsoleColor.BRIGHT_CYAN, serverList.get(i).getServer().getName());
            System.out.println(serverInfo);
        }
        
        System.out.print(ConsoleColor.colorize(ConsoleColor.BRIGHT_BLUE, "输入要停止的服务器序号: "));
        try {
            int choice = Integer.parseInt(scanner.nextLine()) - 1;
            if (choice < 0 || choice >= serverList.size()) {
                System.out.println(ConsoleColor.colorize(ConsoleColor.RED, "错误: 无效的选择"));
                return true;
            }
            
            ServerInstance instance = serverList.get(choice);
            serverService.stopServer(instance.getServer().getName());
            System.out.println(ConsoleColor.colorize(ConsoleColor.GREEN, "已发送停止指令到服务器 " + instance.getServer().getName()));
        } catch (Exception e) {
            System.out.println(ConsoleColor.colorize(ConsoleColor.RED, "停止服务器时出错: " + e.getMessage()));
        }
        
        return true;
    }
    
    @Override
    public String getDescription() {
        return "正常停止指定服务器";
    }
    
    @Override
    public CommandAnnotation getAnnotation() {
        return getClass().getAnnotation(CommandAnnotation.class);
    }
}