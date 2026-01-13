package exmo.cy.model;

import java.util.Objects;

/**
 * 启动配置数据模型
 * 保存上次启动服务器时使用的配置参数
 */
public class LaunchConfig {
    
    private String serverName;
    private int launchMode;
    private String javaPath;
    private String jvmArgs;
    private String serverArgs;
    
    /**
     * 默认构造函数
     */
    public LaunchConfig() {
    }
    
    /**
     * 完整构造函数
     */
    public LaunchConfig(String serverName, int launchMode, String javaPath, String jvmArgs, String serverArgs) {
        this.serverName = serverName;
        this.launchMode = launchMode;
        this.javaPath = javaPath;
        this.jvmArgs = jvmArgs;
        this.serverArgs = serverArgs;
    }
    
    // Getter和Setter方法
    
    public String getServerName() {
        return serverName;
    }
    
    public void setServerName(String serverName) {
        this.serverName = serverName;
    }
    
    public int getLaunchMode() {
        return launchMode;
    }
    
    public void setLaunchMode(int launchMode) {
        this.launchMode = launchMode;
    }
    
    public String getJavaPath() {
        return javaPath;
    }
    
    public void setJavaPath(String javaPath) {
        this.javaPath = javaPath;
    }
    
    public String getJvmArgs() {
        return jvmArgs;
    }
    
    public void setJvmArgs(String jvmArgs) {
        this.jvmArgs = jvmArgs;
    }
    
    public String getServerArgs() {
        return serverArgs;
    }
    
    public void setServerArgs(String serverArgs) {
        this.serverArgs = serverArgs;
    }
    
    /**
     * 验证启动配置的有效性
     * @return 如果配置有效返回true
     */
    public boolean isValid() {
        return serverName != null && !serverName.trim().isEmpty()
            && launchMode >= 1 && launchMode <= 5;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LaunchConfig that = (LaunchConfig) o;
        return launchMode == that.launchMode &&
                Objects.equals(serverName, that.serverName) &&
                Objects.equals(javaPath, that.javaPath);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(serverName, launchMode, javaPath);
    }
    
    @Override
    public String toString() {
        return "LaunchConfig{" +
                "serverName='" + serverName + '\'' +
                ", launchMode=" + launchMode +
                ", javaPath='" + javaPath + '\'' +
                '}';
    }
}