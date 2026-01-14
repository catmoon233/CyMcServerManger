package exmo.cy.command;

/**
 * 服务器停止事件
 */
public class ServerStopEvent extends Event {
    private final String serverName;
    
    public ServerStopEvent(String serverName) {
        this.serverName = serverName;
    }
    
    public String getServerName() {
        return serverName;
    }
}