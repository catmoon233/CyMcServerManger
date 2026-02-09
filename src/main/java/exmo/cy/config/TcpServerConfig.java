package exmo.cy.config;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import exmo.cy.util.Logger;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * TCP服务器配置类
 */
public class TcpServerConfig {
    private static final String CONFIG_FILE = "tcp-server-config.json";
    private static final int DEFAULT_PORT = 5245;
    
    @JsonProperty("enabled")
    private boolean enabled = true;
    
    @JsonProperty("port")
    private int port = DEFAULT_PORT;
    
    @JsonProperty("allowed_ips")
    private List<String> allowedIps = new ArrayList<>();
    
    @JsonProperty("max_connections")
    private int maxConnections = 10;
    
    @JsonProperty("connection_timeout")
    private int connectionTimeout = 30000; // 30秒
    
    public TcpServerConfig() {
        // 默认允许本地连接
        allowedIps.add("127.0.0.1");
        allowedIps.add("localhost");
        allowedIps.add("0:0:0:0:0:0:0:1"); // IPv6 localhost
        allowedIps.add("::1"); // IPv6 localhost short form
    }
    
    /**
     * 加载配置文件
     */
    public static TcpServerConfig loadConfig() {
        Path configPath = Paths.get(CONFIG_FILE);
        ObjectMapper mapper = new ObjectMapper();
        
        try {
            if (Files.exists(configPath)) {
                Logger.info("加载TCP服务器配置: " + configPath.toAbsolutePath());
                return mapper.readValue(configPath.toFile(), TcpServerConfig.class);
            } else {
                Logger.info("TCP服务器配置文件不存在，使用默认配置");
                TcpServerConfig config = new TcpServerConfig();
                config.saveConfig();
                return config;
            }
        } catch (IOException e) {
            Logger.error("加载TCP服务器配置失败，使用默认配置: " + e.getMessage());
            return new TcpServerConfig();
        }
    }
    
    /**
     * 保存配置文件
     */
    public void saveConfig() {
        ObjectMapper mapper = new ObjectMapper();
        try {
            mapper.writerWithDefaultPrettyPrinter().writeValue(new File(CONFIG_FILE), this);
            Logger.info("TCP服务器配置已保存: " + new File(CONFIG_FILE).getAbsolutePath());
        } catch (IOException e) {
            Logger.error("保存TCP服务器配置失败: " + e.getMessage());
        }
    }
    
    // Getters and Setters
    public boolean isEnabled() {
        return enabled;
    }
    
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
    
    public int getPort() {
        return port;
    }
    
    public void setPort(int port) {
        this.port = port;
    }
    
    public List<String> getAllowedIps() {
        return allowedIps;
    }
    
    public void setAllowedIps(List<String> allowedIps) {
        this.allowedIps = allowedIps;
    }
    
    public int getMaxConnections() {
        return maxConnections;
    }
    
    public void setMaxConnections(int maxConnections) {
        this.maxConnections = maxConnections;
    }
    
    public int getConnectionTimeout() {
        return connectionTimeout;
    }
    
    public void setConnectionTimeout(int connectionTimeout) {
        this.connectionTimeout = connectionTimeout;
    }
    
    /**
     * 检查IP是否被允许
     */
    public boolean isIpAllowed(String ip) {
        if (ip == null || ip.isEmpty()) {
            return false;
        }
        
        // 如果允许列表为空，允许所有IP（不推荐）
        if (allowedIps.isEmpty()) {
            return true;
        }
        
        // 检查精确匹配
        for (String allowedIp : allowedIps) {
            if (allowedIp.equals(ip)) {
                return true;
            }
        }
        
        // 检查通配符匹配（如 192.168.*）
        for (String allowedIp : allowedIps) {
            if (allowedIp.endsWith("*")) {
                String prefix = allowedIp.substring(0, allowedIp.length() - 1);
                if (ip.startsWith(prefix)) {
                    return true;
                }
            }
        }
        
        return false;
    }
}