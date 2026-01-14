package exmo.cy.command.impl;

import exmo.cy.command.AnnotatedCommand;
import exmo.cy.command.CommandAnnotation;
import exmo.cy.service.ServerService;
import exmo.cy.util.Logger;

import java.util.Scanner;

@CommandAnnotation(
    name = "add",
    aliases = {},
    description = "添加现有服务器目录"
)
public class AddCommand extends AnnotatedCommand {
    private final ServerService serverService;
    private final Scanner scanner = new Scanner(System.in);
    
    public AddCommand(ServerService serverService) {
        this.serverService = serverService;
    }
    
    @Override
    public boolean execute(String[] args) {
        Logger.println("=== 添加现有服务器 ===");
        
        Logger.print("输入服务器目录路径: ");
        String path = scanner.nextLine().trim();
        
        Logger.print("输入服务器名称: ");
        String name = scanner.nextLine().trim();
        
        Logger.print("输入服务器描述: ");
        String description = scanner.nextLine().trim();
        
        Logger.print("输入服务器版本: ");
        String version = scanner.nextLine().trim();
        
        try {
            serverService.addExistingServer(path, name, version, description);
            Logger.println("服务器 " + name + " 添加成功！");
        } catch (Exception e) {
            Logger.error("添加服务器失败: " + e.getMessage(), e);
        }
        
        return true;
    }
    
    @Override
    public String getDescription() {
        return "添加现有服务器目录";
    }
    
    @Override
    public CommandAnnotation getAnnotation() {
        return getClass().getAnnotation(CommandAnnotation.class);
    }
}