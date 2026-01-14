package exmo.cy.command.impl;

import exmo.cy.command.AnnotatedCommand;
import exmo.cy.command.CommandAnnotation;
import exmo.cy.model.Server;
import exmo.cy.service.ServerService;
import exmo.cy.util.Logger;

import java.util.List;
import java.util.Scanner;

@CommandAnnotation(
    name = "delete",
    aliases = {},
    description = "删除服务器配置和本地文件"
)
public class DeleteCommand extends AnnotatedCommand {
    private final ServerService serverService;
    private final Scanner scanner = new Scanner(System.in);
    
    public DeleteCommand(ServerService serverService) {
        this.serverService = serverService;
    }
    
    @Override
    public boolean execute(String[] args) {
        try {
            List<Server> servers = serverService.getConfigManager().loadServers();
            if (servers.isEmpty()) {
                Logger.println("错误: 没有可用服务器");
                return true;
            }
            
            Logger.println("可用服务器：");
            for (int i = 0; i < servers.size(); i++) {
                Logger.println((i + 1) + ". " + servers.get(i).getName());
            }
            
            Logger.print("选择要删除的服务器编号: ");
            int choice = Integer.parseInt(scanner.nextLine()) - 1;
            if (choice < 0 || choice >= servers.size()) {
                Logger.println("错误: 无效的选择");
                return true;
            }
            
            Server server = servers.get(choice);
            Logger.print("确定要删除服务器配置和本地文件? (y/n): ");
            
            if ("y".equalsIgnoreCase(scanner.nextLine().trim())) {
                serverService.deleteServer(server.getName(), true);
                Logger.println("服务器 " + server.getName() + " 已删除");
            } else {
                Logger.println("操作已取消");
            }
        } catch (Exception e) {
            Logger.error("删除服务器时出错: " + e.getMessage(), e);
        }
        
        return true;
    }
    
    @Override
    public String getDescription() {
        return "删除服务器配置和本地文件";
    }
    
    @Override
    public CommandAnnotation getAnnotation() {
        return getClass().getAnnotation(CommandAnnotation.class);
    }
}