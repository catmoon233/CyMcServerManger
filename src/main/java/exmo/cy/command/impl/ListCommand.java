package exmo.cy.command.impl;

import exmo.cy.command.AnnotatedCommand;
import exmo.cy.command.CommandAnnotation;
import exmo.cy.exception.ConfigurationException;
import exmo.cy.model.Server;
import exmo.cy.service.ServerService;
import exmo.cy.util.Logger;

import java.util.List;

@CommandAnnotation(
    name = "list",
    aliases = {},
    description = "列出所有服务器"
)
public class ListCommand extends AnnotatedCommand {
    private final ServerService serverService;
    
    public ListCommand(ServerService serverService) {
        this.serverService = serverService;
    }
    
    @Override
    public boolean execute(String[] args) {
        try {
            List<Server> servers = serverService.getConfigManager().loadServers();
            if (servers.isEmpty()) {
                Logger.println("没有可用服务器");
                return true;
            }
            
            Logger.println("服务器列表：");
            for (Server server : servers) {
                Logger.println("名称: " + server.getName());
                Logger.println("  版本: " + server.getVersion());
                Logger.println("  描述: " + server.getDescription());
                Logger.println("  当前地图: " + (server.getMap() != null ? server.getMap() : "未设置"));
                Logger.println("  路径: " + server.getCorePath());
                // Logger.println();
            }
        } catch (ConfigurationException e) {
            Logger.error("加载服务器配置时出错: " + e.getMessage(), e);
        }
        
        return true;
    }
    
    @Override
    public String getDescription() {
        return "列出所有服务器";
    }
    
    @Override
    public CommandAnnotation getAnnotation() {
        return getClass().getAnnotation(CommandAnnotation.class);
    }
}