package exmo.cy.command.impl;

import exmo.cy.command.AnnotatedCommand;
import exmo.cy.command.CommandAnnotation;
import exmo.cy.service.ServerService;
import exmo.cy.util.Logger;

@CommandAnnotation(
    name = "create",
    aliases = {},
    description = "创建新服务器"
)
public class CreateCommand extends AnnotatedCommand {
    private final ServerService serverService;
    
    public CreateCommand(ServerService serverService) {
        this.serverService = serverService;
    }
    
    @Override
    public boolean execute(String[] args) {
        // 实现创建服务器逻辑
        Logger.println("=== 创建新服务器 ===");
        
        // TODO: 列出可用核心文件并让用户选择
        // TODO: 获取服务器名称和描述
        // TODO: 调用serverService.createServer()
        
        Logger.println("此功能正在实现中...");
        return true;
    }
    
    @Override
    public String getDescription() {
        return "创建新服务器";
    }
    
    @Override
    public CommandAnnotation getAnnotation() {
        return getClass().getAnnotation(CommandAnnotation.class);
    }
}