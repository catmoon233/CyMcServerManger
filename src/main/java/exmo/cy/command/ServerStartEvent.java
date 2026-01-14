package exmo.cy.command;

/**
 * 服务器启动事件
 */
public class ServerStartEvent extends Event {
    private final String serverName;
    
    public ServerStartEvent(String serverName) {
        this.serverName = serverName;
    }
    
    public String getServerName() {
        return serverName;
    }
}