package exmo.cy.command.impl;

import exmo.cy.command.AnnotatedCommand;
import exmo.cy.command.CommandAnnotation;
import exmo.cy.model.ServerInstance;
import exmo.cy.service.ServerService;
import exmo.cy.util.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

@CommandAnnotation(
    name = "stop-server",
    aliases = {"ss"},
    description = "正常停止指定服务器"
)
public class StopServerCommand extends AnnotatedCommand {
    private final ServerService serverService;
    private final Scanner scanner = new Scanner(System.in);
    
    public StopServerCommand(ServerService serverService) {
        this.serverService = serverService;
    }
    
    @Override
    public boolean execute(String[] args) {
        Map<String, ServerInstance> activeServers = serverService.getActiveServers();
        if (activeServers.isEmpty()) {
            Logger.println("没有运行中的服务器");
            return true;
        }
        
        List<ServerInstance> serverList = new ArrayList<>(activeServers.values());
        Logger.println("运行中的服务器：");
        for (int i = 0; i < serverList.size(); i++) {
            Logger.println((i + 1) + ". " + serverList.get(i).getServer().getName());
        }
        
        Logger.print("输入要停止的服务器序号: ");
        try {
            int choice = Integer.parseInt(scanner.nextLine()) - 1;
            if (choice < 0 || choice >= serverList.size()) {
                Logger.println("错误: 无效的选择");
                return true;
            }
            
            ServerInstance instance = serverList.get(choice);
            serverService.stopServer(instance.getServer().getName());
            Logger.println("已发送停止指令到服务器 " + instance.getServer().getName());
        } catch (Exception e) {
            Logger.error("停止服务器时出错: " + e.getMessage());
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