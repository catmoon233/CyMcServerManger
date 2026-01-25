package exmo.cy.model;

import java.util.ArrayList;
import java.util.List;

/**
 * 服务器群组模型
 * 用于管理一组服务器的启动和控制
 */
public class ServerGroup {
    private String name;
    private List<String> serverNames; // 服务器名称列表
    private boolean orderedStartup; // 是否按顺序启动
    private String triggerKeyword; // 触发关键词，用于有序启动时判断上一个服务器是否准备好
    private int startupDelay; // 启动延迟（毫秒），在有序启动时使用
    private int launchMode; // 启动模式，默认为CORE模式
    private String presetJvmArgs; // 预设JVM参数
    private String presetServerArgs; // 预设服务器参数
    private String minMemory; // 最小内存参数，例如 "512M" 或 "2G"
    private String maxMemory; // 最大内存参数，例如 "2G" 或 "4G"
    
    public ServerGroup() {
        this.serverNames = new ArrayList<>();
        this.orderedStartup = false;
        this.triggerKeyword = "Done"; // 默认触发词为 "Done"，通常表示服务器启动完成
        this.startupDelay = 5000; // 默认延迟5秒
        this.launchMode = 1; // 默认为CORE模式
        this.presetJvmArgs = null;
        this.presetServerArgs = null;
    }
    
    public ServerGroup(String name) {
        this();
        this.name = name;
    }
    
    public ServerGroup(String name, int launchMode) {
        this(name);
        this.launchMode = launchMode;
    }
    
    public ServerGroup(String name, int launchMode, String presetJvmArgs, String presetServerArgs) {
        this(name, launchMode);
        this.presetJvmArgs = presetJvmArgs;
        this.presetServerArgs = presetServerArgs;
    }
    
    public ServerGroup(String name, int launchMode, String presetJvmArgs, String presetServerArgs, String minMemory, String maxMemory) {
        this(name, launchMode);
        this.presetJvmArgs = presetJvmArgs;
        this.presetServerArgs = presetServerArgs;
        this.minMemory = minMemory;
        this.maxMemory = maxMemory;
    }
    
    // Getters and Setters
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public List<String> getServerNames() {
        return serverNames;
    }
    
    public void setServerNames(List<String> serverNames) {
        this.serverNames = serverNames != null ? serverNames : new ArrayList<>();
    }
    
    public boolean isOrderedStartup() {
        return orderedStartup;
    }
    
    public void setOrderedStartup(boolean orderedStartup) {
        this.orderedStartup = orderedStartup;
    }
    
    public String getTriggerKeyword() {
        return triggerKeyword;
    }
    
    public void setTriggerKeyword(String triggerKeyword) {
        this.triggerKeyword = triggerKeyword;
    }
    
    public int getStartupDelay() {
        return startupDelay;
    }
    
    public void setStartupDelay(int startupDelay) {
        this.startupDelay = startupDelay;
    }
    
    public int getLaunchMode() {
        return launchMode;
    }
    
    public void setLaunchMode(int launchMode) {
        this.launchMode = launchMode;
    }
    
    public String getPresetJvmArgs() {
        return presetJvmArgs;
    }
    
    public void setPresetJvmArgs(String presetJvmArgs) {
        this.presetJvmArgs = presetJvmArgs;
    }
    
    public String getPresetServerArgs() {
        return presetServerArgs;
    }
    
    public void setPresetServerArgs(String presetServerArgs) {
        this.presetServerArgs = presetServerArgs;
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
     * 添加服务器到群组
     */
    public void addServer(String serverName) {
        if (!serverNames.contains(serverName)) {
            serverNames.add(serverName);
        }
    }
    
    /**
     * 从群组中移除服务器
     */
    public boolean removeServer(String serverName) {
        return serverNames.remove(serverName);
    }
    
    /**
     * 检查群组是否包含指定服务器
     */
    public boolean containsServer(String serverName) {
        return serverNames.contains(serverName);
    }
    
    /**
     * 获取服务器数量
     */
    public int getServerCount() {
        return serverNames.size();
    }
    
    @Override
    public String toString() {
        return "ServerGroup{" +
                "name='" + name + '\'' +
                ", serverNames=" + serverNames +
                ", orderedStartup=" + orderedStartup +
                ", triggerKeyword='" + triggerKeyword + '\'' +
                ", startupDelay=" + startupDelay +
                ", launchMode=" + launchMode +
                ", presetJvmArgs='" + presetJvmArgs + '\'' +
                ", presetServerArgs='" + presetServerArgs + '\'' +
                '}';
    }
}