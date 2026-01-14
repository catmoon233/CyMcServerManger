package exmo.cy.command.impl;

import exmo.cy.command.AnnotatedCommand;
import exmo.cy.command.CommandAnnotation;
import exmo.cy.command.CommandManager;
import exmo.cy.model.ServerInstance;
import exmo.cy.service.ServerService;
import exmo.cy.util.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

@CommandAnnotation(
    name = "attach",
    aliases = {"at"},
    description = "连接到运行中的服务器控制台"
)
public class AttachCommand extends AnnotatedCommand {
    private final ServerService serverService;
    private final CommandManager commandManager;
    private final Scanner scanner = new Scanner(System.in);
    
    public AttachCommand(ServerService serverService, CommandManager commandManager) {
        this.serverService = serverService;
        this.commandManager = commandManager;
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
        
        Logger.print("输入要连接的服务器序号: ");
        try {
            int choice = Integer.parseInt(scanner.nextLine()) - 1;
            if (choice < 0 || choice >= serverList.size()) {
                Logger.println("错误: 无效的选择");
                return true;
            }
            
            commandManager.setAttachedServer(serverList.get(choice));
            Logger.println("已连接到服务器 " + serverList.get(choice).getServer().getName());
            Logger.println("输入 'detach' 返回主控制台，'force-stop' 强制停止服务器");
        } catch (NumberFormatException e) {
            Logger.println("错误: 请输入有效的数字");
        }
        
        return true;
    }
    
    @Override
    public String getDescription() {
        return "连接到运行中的服务器控制台";
    }
    
    @Override
    public CommandAnnotation getAnnotation() {
        return getClass().getAnnotation(CommandAnnotation.class);
    }
}