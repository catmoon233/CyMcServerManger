package exmo.cy.command.impl;

import exmo.cy.command.AnnotatedCommand;
import exmo.cy.command.CommandAnnotation;
import exmo.cy.model.ServerInstance;
import exmo.cy.service.ServerService;
import exmo.cy.util.ConsoleColor;
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
            System.out.println(ConsoleColor.colorize(ConsoleColor.YELLOW, "没有运行中的服务器"));
        } else {
            System.out.println(ConsoleColor.colorize(ConsoleColor.BRIGHT_GREEN, "运行中的服务器："));
            for (ServerInstance instance : activeServers.values()) {
                String serverInfo = "- " + ConsoleColor.colorize(ConsoleColor.BRIGHT_CYAN, instance.getServer().getName()) + 
                             " (版本: " + ConsoleColor.colorize(ConsoleColor.BRIGHT_WHITE, instance.getServer().getVersion()) + 
                             ", 运行时长: " + ConsoleColor.colorize(ConsoleColor.BRIGHT_YELLOW, String.valueOf(instance.getUptime() / 1000)) + "秒)";
                System.out.println(serverInfo);
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