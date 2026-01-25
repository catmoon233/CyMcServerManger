package exmo.cy.socket;

import exmo.cy.service.ServerService;
import exmo.cy.util.Logger;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;


import java.io.IOException;

/**
 * TCP Socket服务
 * 管理TCP Socket服务器的生命周期
 */
@Service
public class TcpSocketService {
    
    @Autowired
    private ServerService serverService;
    
    private TcpSocketServer tcpSocketServer;
    
    @PostConstruct
    public void init() {
        Logger.info("初始化TCP Socket服务");
        tcpSocketServer = new TcpSocketServer(serverService);
        
        // 在单独的线程中启动TCP服务器
        Thread tcpThread = new Thread(() -> {
            try {
                tcpSocketServer.start();
            } catch (IOException e) {
                Logger.error("启动TCP Socket服务器失败", e);
            }
        }, "TcpSocketServer-Thread");
        
        tcpThread.setDaemon(false); // 确保主线程不会在TCP服务器启动后立即退出
        tcpThread.start();
    }
    
    @PreDestroy
    public void destroy() {
        Logger.info("销毁TCP Socket服务");
        if (tcpSocketServer != null && tcpSocketServer.isRunning()) {
            try {
                tcpSocketServer.stop();
            } catch (IOException e) {
                Logger.error("停止TCP Socket服务器时出错", e);
            }
        }
    }
    
    /**
     * 获取TCP Socket服务器实例
     */
    public TcpSocketServer getTcpSocketServer() {
        return tcpSocketServer;
    }
    
    /**
     * 检查TCP Socket服务器是否正在运行
     */
    public boolean isRunning() {
        return tcpSocketServer != null && tcpSocketServer.isRunning();
    }
}