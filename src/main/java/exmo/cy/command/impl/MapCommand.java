package exmo.cy.command.impl;

import exmo.cy.command.AnnotatedCommand;
import exmo.cy.command.CommandAnnotation;
import exmo.cy.service.ServerService;
import exmo.cy.util.Logger;

@CommandAnnotation(
    name = "map",
    aliases = {},
    description = "切换服务器地图"
)
public class MapCommand extends AnnotatedCommand {
    private final ServerService serverService;
    
    public MapCommand(ServerService serverService) {
        this.serverService = serverService;
    }
    
    @Override
    public boolean execute(String[] args) {
        Logger.println("地图切换功能正在实现中...");
        return true;
    }
    
    @Override
    public String getDescription() {
        return "切换服务器地图";
    }
    
    @Override
    public CommandAnnotation getAnnotation() {
        return getClass().getAnnotation(CommandAnnotation.class);
    }
}