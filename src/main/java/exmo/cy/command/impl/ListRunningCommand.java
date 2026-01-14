package exmo.cy.command.impl;

import exmo.cy.command.AnnotatedCommand;
import exmo.cy.command.CommandAnnotation;
import exmo.cy.model.ServerInstance;
import exmo.cy.service.ServerService;
import exmo.cy.util.Logger;

import java.util.Map;

@CommandAnnotation(
    name = "list-running",
    aliases = {"lr"},
    description = "列出所有运行中的服务器"
)
public class ListRunningCommand extends AnnotatedCommand {
    private final ServerService serverService;
    
    public ListRunningCommand(ServerService serverService) {
        this.serverService = serverService;
    }
    
    @Override
    public boolean execute(String[] args) {
        Map<String, ServerInstance> activeServers = serverService.getActiveServers();
        if (activeServers.isEmpty()) {
            Logger.println("没有运行中的服务器");
        } else {
            Logger.println("运行中的服务器：");
            for (ServerInstance instance : activeServers.values()) {
                Logger.println("- " + instance.getServer().getName() + 
                             " (版本: " + instance.getServer().getVersion() + 
                             ", 运行时长: " + (instance.getUptime() / 1000) + "秒)");
            }
        }
        return true;
    }
    
    @Override
    public String getDescription() {
        return "列出所有运行中的服务器";
    }
    
    @Override
    public CommandAnnotation getAnnotation() {
        return getClass().getAnnotation(CommandAnnotation.class);
    }
}