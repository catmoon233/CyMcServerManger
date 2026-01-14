package exmo.cy.command.impl;

import exmo.cy.command.AnnotatedCommand;
import exmo.cy.command.CommandAnnotation;
import exmo.cy.service.ServerService;
import exmo.cy.util.Logger;

@CommandAnnotation(
    name = "switch",
    aliases = {},
    description = "切换服务器核心版本"
)
public class SwitchCommand extends AnnotatedCommand {
    private final ServerService serverService;
    
    public SwitchCommand(ServerService serverService) {
        this.serverService = serverService;
    }
    
    @Override
    public boolean execute(String[] args) {
        Logger.println("=== 切换服务器核心版本 ===");
        
        // TODO: 实现切换核心版本逻辑
        
        Logger.println("此功能正在实现中...");
        return true;
    }
    
    @Override
    public String getDescription() {
        return "切换服务器核心版本";
    }
    
    @Override
    public CommandAnnotation getAnnotation() {
        return getClass().getAnnotation(CommandAnnotation.class);
    }
}