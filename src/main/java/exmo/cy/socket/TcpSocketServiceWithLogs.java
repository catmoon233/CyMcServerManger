package exmo.cy.socket;

import exmo.cy.config.TcpServerConfig;
import exmo.cy.service.ServerService;
import exmo.cy.util.Logger;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;

/**
 * 增强版TCP Socket服务
 * 管理TCP Socket服务器的生命周期，支持日志推送和IP认证
 */
@Service
public class TcpSocketServiceWithLogs {
    
    @Autowired
    private ServerService serverService;
    
    private TcpSocketServerWithLogs tcpSocketServer;
    private TcpServerConfig config;
    
    @PostConstruct
    public void init() {
        Logger.info("初始化增强版TCP Socket服务");
        
        // 加载配置
        config = TcpServerConfig.loadConfig();
        
        if (!config.isEnabled()) {
            Logger.info("TCP Socket服务已禁用，跳过启动");
            return;
        }
        
        tcpSocketServer = new TcpSocketServerWithLogs(config, serverService);
        
        // 在单独的线程中启动TCP服务器
        Thread tcpThread = new Thread(() -> {
            try {
                tcpSocketServer.start();
            } catch (IOException e) {
                Logger.error("启动TCP Socket服务器失败", e);
            }
        }, "TcpSocketServerWithLogs-Thread");
        
        tcpThread.setDaemon(false); // 确保主线程不会在TCP服务器启动后立即退出
        tcpThread.start();
    }
    
    @PreDestroy
    public void destroy() {
        Logger.info("销毁增强版TCP Socket服务");
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
    public TcpSocketServerWithLogs getTcpSocketServer() {
        return tcpSocketServer;
    }
    
    /**
     * 检查TCP Socket服务器是否正在运行
     */
    public boolean isRunning() {
        return tcpSocketServer != null && tcpSocketServer.isRunning();
    }
    
    /**
     * 向所有客户端广播日志消息
     */
    public void broadcastLogMessage(String message) {
        if (tcpSocketServer != null) {
            tcpSocketServer.broadcastLogMessage(message);
        }
    }
    
    /**
     * 获取当前配置
     */
    public TcpServerConfig getConfig() {
        return config;
    }
}