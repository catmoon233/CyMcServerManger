package exmo.cy.model;

import java.io.OutputStream;
import java.util.Objects;

/**
 * 服务器实例数据模型
 * 表示一个正在运行的服务器实例
 */
public class ServerInstance {
    
    private Process process;
    private Server server;
    private OutputStream processInput;
    private long startTime;
    
    /**
     * 默认构造函数
     */
    public ServerInstance() {
        this.startTime = System.currentTimeMillis();
    }
    
    /**
     * 构造函数
     * @param process 服务器进程
     * @param server 服务器配置
     * @param processInput 进程输入流
     */
    public ServerInstance(Process process, Server server, OutputStream processInput) {
        this.process = process;
        this.server = server;
        this.processInput = processInput;
        this.startTime = System.currentTimeMillis();
    }
    
    // Getter和Setter方法
    
    public Process getProcess() {
        return process;
    }
    
    public void setProcess(Process process) {
        this.process = process;
    }
    
    public Server getServer() {
        return server;
    }
    
    public void setServer(Server server) {
        this.server = server;
    }
    
    public OutputStream getProcessInput() {
        return processInput;
    }
    
    public void setProcessInput(OutputStream processInput) {
        this.processInput = processInput;
    }
    
    public long getStartTime() {
        return startTime;
    }
    
    /**
     * 检查服务器实例是否正在运行
     * @return 如果正在运行返回true
     */
    public boolean isRunning() {
        return process != null && process.isAlive();
    }
    
    /**
     * 获取服务器运行时长（毫秒）
     * @return 运行时长
     */
    public long getUptime() {
        return System.currentTimeMillis() - startTime;
    }
    
    /**
     * 获取服务器名称（如果服务器配置存在）
     * @return 服务器名称，如果不存在则返回null
     */
    public String getServerName() {
        return server != null ? server.getName() : null;
    }
    
    /**
     * 获取服务器版本（如果服务器配置存在）
     * @return 服务器版本，如果不存在则返回null
     */
    public String getVersion() {
        return server != null ? server.getVersion() : null;
    }
    
    /**
     * 获取服务器描述（如果服务器配置存在）
     * @return 服务器描述，如果不存在则返回null
     */
    public String getDescription() {
        return server != null ? server.getDescription() : null;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ServerInstance that = (ServerInstance) o;
        return Objects.equals(server, that.server);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(server);
    }
    
    @Override
    public String toString() {
        return "ServerInstance{" +
                "server=" + (server != null ? server.getName() : "null") +
                ", running=" + isRunning() +
                ", uptime=" + getUptime() + "ms" +
                '}';
    }
}