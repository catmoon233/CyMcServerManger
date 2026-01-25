package exmo.cy.model;

import java.util.Objects;

/**
 * 服务器配置数据模型
 * 封装服务器的所有配置信息
 */
public class Server {
    
    private String name;
    private String corePath;
    private String version;
    private String description;
    private boolean isModpack;
    private String map;
    private String defaultJvmArgs;
    private String defaultServerArgs;
    private String group;
    private String minMemory; // 最小内存设置
    private String maxMemory; // 最大内存设置
    
    /**
     * 默认构造函数
     */
    public Server() {
    }
    
    /**
     * 完整构造函数
     */
    public Server(String name, String corePath, String version, String description, boolean isModpack, String map) {
        this.name = name;
        this.corePath = corePath;
        this.version = version;
        this.description = description;
        this.isModpack = isModpack;
        this.map = map;
        this.defaultJvmArgs = null;
        this.defaultServerArgs = null;
    }
    
    public Server(String name, String corePath, String version, String description, boolean isModpack, String map, String defaultJvmArgs, String defaultServerArgs) {
        this.name = name;
        this.corePath = corePath;
        this.version = version;
        this.description = description;
        this.isModpack = isModpack;
        this.map = map;
        this.defaultJvmArgs = defaultJvmArgs;
        this.defaultServerArgs = defaultServerArgs;
        this.group = null;
        this.minMemory = null;
        this.maxMemory = null;
    }
    
    public Server(String name, String corePath, String version, String description, boolean isModpack, String map, String defaultJvmArgs, String defaultServerArgs, String group) {
        this.name = name;
        this.corePath = corePath;
        this.version = version;
        this.description = description;
        this.isModpack = isModpack;
        this.map = map;
        this.defaultJvmArgs = defaultJvmArgs;
        this.defaultServerArgs = defaultServerArgs;
        this.group = group;
        this.minMemory = null;
        this.maxMemory = null;
    }
    
    // Getter和Setter方法
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getCorePath() {
        return corePath;
    }
    
    public void setCorePath(String corePath) {
        this.corePath = corePath;
    }
    
    public String getVersion() {
        return version;
    }
    
    public void setVersion(String version) {
        this.version = version;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public boolean isModpack() {
        return isModpack;
    }
    
    public void setModpack(boolean modpack) {
        isModpack = modpack;
    }
    
    public String getMap() {
        return map;
    }
    
    public void setMap(String map) {
        this.map = map;
    }
    
    public String getDefaultJvmArgs() {
        return defaultJvmArgs;
    }
    
    public void setDefaultJvmArgs(String defaultJvmArgs) {
        this.defaultJvmArgs = defaultJvmArgs;
    }
    
    public String getDefaultServerArgs() {
        return defaultServerArgs;
    }
    
    public void setDefaultServerArgs(String defaultServerArgs) {
        this.defaultServerArgs = defaultServerArgs;
    }
    
    public String getGroup() {
        return group;
    }
    
    public void setGroup(String group) {
        this.group = group;
    }
    
    public String getMinMemory() {
        return minMemory;
    }
    
    public void setMinMemory(String minMemory) {
        this.minMemory = minMemory;
    }
    
    public String getMaxMemory() {
        return maxMemory;
    }
    
    public void setMaxMemory(String maxMemory) {
        this.maxMemory = maxMemory;
    }
    
    /**
     * 验证服务器配置的有效性
     * @return 如果配置有效返回true
     */
    public boolean isValid() {
        return name != null && !name.trim().isEmpty() 
            && corePath != null && !corePath.trim().isEmpty()
            && version != null && !version.trim().isEmpty();
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Server server = (Server) o;
        return Objects.equals(name, server.name);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(name);
    }
    
    @Override
    public String toString() {
        return "Server{" +
                "name='" + name + '\'' +
                ", version='" + version + '\'' +
                ", description='" + description + '\'' +
                ", map='" + map + '\'' +
                ", isModpack=" + isModpack +
                ", defaultJvmArgs='" + defaultJvmArgs + '\'' +
                ", defaultServerArgs='" + defaultServerArgs + '\'' +
                ", group='" + group + '\'' +
                ", minMemory='" + minMemory + '\'' +
                ", maxMemory='" + maxMemory + '\'' +
                '}';
    }
}