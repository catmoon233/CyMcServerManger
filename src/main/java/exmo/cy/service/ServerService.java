package exmo.cy.service;

import exmo.cy.config.Constants;
import exmo.cy.exception.ConfigurationException;
import exmo.cy.exception.ServerOperationException;
import exmo.cy.model.LaunchConfig;
import exmo.cy.model.Server;
import exmo.cy.model.ServerInstance;
import exmo.cy.util.FileUtils;
import exmo.cy.util.JavaPathFinder;
import exmo.cy.util.Logger;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Service;

/**
 * 服务器服务类
 * 负责服务器的创建、启动、停止等核心操作
 */
@Service
public class ServerService {
    
    private final ConfigurationManager configManager;
    private final ProcessManager processManager;
    private final Map<String, ServerInstance> activeServers;
    
    /**
     * 构造函数
     */
    public ServerService() {
        this.configManager = new ConfigurationManager();
        this.processManager = new ProcessManager();
        this.activeServers = new ConcurrentHashMap<>();
    }
    
    /**
     * 创建新服务器
     * @param coreName 核心文件名
     * @param serverName 服务器名称
     * @param description 描述
     * @return 创建的服务器配置
     * @throws ServerOperationException 如果创建失败
     */
    public Server createServer(String coreName, String serverName, String description) 
            throws ServerOperationException, ConfigurationException {
        
        // 检查服务器名称是否已存在
        if (configManager.serverExists(serverName)) {
            throw new ServerOperationException("服务器名称已存在: " + serverName);
        }
        
        // 创建服务器目录
        Path serverDir = Paths.get(Constants.SERVERS_DIR, serverName);
        FileUtils.ensureDirectoryExists(serverDir);
        
        // 复制核心文件
        Path sourceCorejar = Paths.get(Constants.CORES_DIR, coreName);
        Path targetCorePath = serverDir.resolve(Constants.CORE_JAR);
        FileUtils.copyFile(sourceCorejar, targetCorePath);
        
        // 提取版本号
        String version = extractVersion(coreName);
        
        // 创建服务器配置
        Server server = new Server();
        server.setName(serverName);
        server.setCorePath(targetCorePath.toString());
        server.setVersion(version);
        server.setDescription(description);
        server.setModpack(false);
        
        // 保存配置
        configManager.saveServer(server);
        Logger.info("成功创建服务器: " + serverName);
        
        return server;
    }
    
    /**
     * 添加现有服务器目录
     * @param serverPath 服务器目录路径
     * @param serverName 服务器名称
     * @param version 版本
     * @param description 描述
     * @return 服务器配置
     * @throws ServerOperationException 如果添加失败
     */
    public Server addExistingServer(String serverPath, String serverName, String version, String description) 
            throws ServerOperationException, ConfigurationException {
        
        Path serverDir = Paths.get(serverPath);
        if (!Files.exists(serverDir)) {
            throw new ServerOperationException("服务器目录不存在: " + serverPath);
        }
        
        Path corePath = serverDir.resolve(Constants.CORE_JAR);
        if (!FileUtils.isFileReadable(corePath)) {
            Logger.warn("服务器目录中没有Core.jar文件");
        }
        
        Server server = new Server();
        server.setName(serverName);
        server.setCorePath(corePath.toString());
        server.setVersion(version);
        server.setDescription(description);
        server.setModpack(false);
        
        configManager.saveServer(server);
        Logger.info("成功添加服务器: " + serverName);
        
        return server;
    }
    
    /**
     * 启动服务器
     * @param server 服务器配置
     * @param launchMode 启动模式
     * @param javaPath Java路径
     * @param jvmArgs JVM参数（可选）
     * @param serverArgs 服务器参数（可选）
     * @return 服务器实例
     * @throws ServerOperationException 如果启动失败
     */
    public ServerInstance startServer(Server server, int launchMode, String javaPath, 
                                      String jvmArgs, String serverArgs) 
            throws ServerOperationException, ConfigurationException {
        
        if (server == null) {
            throw new ServerOperationException("服务器配置不能为null");
        }
        
        if (activeServers.containsKey(server.getName())) {
            throw new ServerOperationException("服务器已在运行: " + server.getName());
        }
        
        // 构建进程命令
        ProcessBuilder pb = buildProcessCommand(server, launchMode, javaPath, jvmArgs, serverArgs);
        
        // 启动进程
        ServerInstance instance = processManager.startProcess(pb);
        instance.setServer(server);
        
        // 保存启动配置
        LaunchConfig config = new LaunchConfig(server.getName(), launchMode, javaPath, jvmArgs, serverArgs);
        configManager.saveLastLaunchConfig(config);
        
        // 添加到活动服务器列表
        activeServers.put(server.getName(), instance);
        
        // 监控进程结束
        startProcessMonitor(instance);
        
        Logger.info("服务器已启动: " + server.getName());
        return instance;
    }
    
    /**
     * 构建进程启动命令
     */
    private ProcessBuilder buildProcessCommand(Server server, int launchMode, String javaPath,
                                               String jvmArgs, String serverArgs) 
            throws ServerOperationException {
        
        File serverDir = new File(server.getCorePath()).getParentFile();
        ProcessBuilder pb = new ProcessBuilder();
        pb.directory(serverDir);
        
        // 验证Java路径
        String validJavaPath = validateJavaPath(javaPath);
        
        List<String> command = new ArrayList<>();
        command.add(validJavaPath);
        
        switch (launchMode) {
            case Constants.LAUNCH_MODE_CORE:
                command.addAll(Arrays.asList(
                    "-Xms" + Constants.DEFAULT_MIN_MEMORY,
                    "-Xmx" + Constants.DEFAULT_MAX_MEMORY,
                    "-Dterminal.jline=false",
                    "-Dterminal.ansi=true",
                    "-Dfile.encoding=" + Constants.FILE_ENCODING,
                    "-Dlog4j2.formatMsgNoLookups=true",
                    "-Duser.timezone=" + Constants.TIMEZONE,
                    "-Dfunction.permission.level=" + Constants.MAX_FUNCTION_PERMISSION_LEVEL,
                    "-Dop.permission.level=" + Constants.MAX_OP_PERMISSION_LEVEL,
                    "-jar", Constants.CORE_JAR, "-nogui"
                ));
                break;
                
            case Constants.LAUNCH_MODE_MODPACK:
                command.addAll(Arrays.asList(
                    "-Xms" + Constants.DEFAULT_MIN_MEMORY,
                    "-Xmx" + Constants.DEFAULT_MAX_MEMORY,
                    "-Dterminal.jline=false",
                    "-Dterminal.ansi=true",
                    "-Dfile.encoding=" + Constants.FILE_ENCODING,
                    "-Dlog4j2.formatMsgNoLookups=true",
                    "-Duser.timezone=" + Constants.TIMEZONE,
                    "-Dfunction.permission.level=" + Constants.MAX_FUNCTION_PERMISSION_LEVEL,
                    "-Dop.permission.level=" + Constants.MAX_OP_PERMISSION_LEVEL,
                    "@cnmforge.txt", "-nogui"
                ));
                break;
                
            case Constants.LAUNCH_MODE_BASIC:
                command.addAll(Arrays.asList(
                    "-Dfunction.permission.level=" + Constants.MAX_FUNCTION_PERMISSION_LEVEL,
                    "-Dop.permission.level=" + Constants.MAX_OP_PERMISSION_LEVEL,
                    "-jar", Constants.CORE_JAR
                ));
                break;
                
            case Constants.LAUNCH_MODE_BASIC_FIX:
                command.addAll(Arrays.asList(
                    "-Dfunction.permission.level=" + Constants.MAX_FUNCTION_PERMISSION_LEVEL,
                    "-Dop.permission.level=" + Constants.MAX_OP_PERMISSION_LEVEL,
                    "-Dpaper.disableChannelLimit=true",
                    "-jar", Constants.CORE_JAR
                ));
                break;
                
            case Constants.LAUNCH_MODE_CUSTOM:
                if (jvmArgs != null && !jvmArgs.trim().isEmpty()) {
                    command.addAll(Arrays.asList(jvmArgs.trim().split("\\s+")));
                }
                command.add("-jar");
                command.add(Constants.CORE_JAR);
                if (serverArgs != null && !serverArgs.trim().isEmpty()) {
                    command.addAll(Arrays.asList(serverArgs.trim().split("\\s+")));
                }
                break;
                
            default:
                throw new ServerOperationException("无效的启动模式: " + launchMode);
        }
        
        pb.command(command);
        return pb;
    }
    
    /**
     * 验证Java路径
     */
    private String validateJavaPath(String javaPath) {
        if (javaPath != null && JavaPathFinder.isValidJavaPath(javaPath)) {
            return javaPath;
        }
        return JavaPathFinder.getDefaultJavaPath();
    }
    
    /**
     * 启动进程监控线程
     */
    private void startProcessMonitor(ServerInstance instance) {
        new Thread(() -> {
            try {
                int exitCode = processManager.waitForProcess(instance);
                Logger.info("服务器 " + instance.getServer().getName() + " 已关闭，退出代码: " + exitCode);
            } catch (ServerOperationException e) {
                Logger.error("监控进程时出错", e);
            } finally {
                activeServers.remove(instance.getServer().getName());
            }
        }).start();
    }
    
    /**
     * 停止服务器
     * @param serverName 服务器名称
     * @throws ServerOperationException 如果停止失败
     */
    public void stopServer(String serverName) throws ServerOperationException {
        ServerInstance instance = activeServers.get(serverName);
        if (instance == null) {
            throw new ServerOperationException("服务器未运行: " + serverName);
        }
        processManager.stopServer(instance);
    }
    
    /**
     * 强制停止服务器
     * @param serverName 服务器名称
     * @throws ServerOperationException 如果停止失败
     */
    public void forceStopServer(String serverName) throws ServerOperationException {
        ServerInstance instance = activeServers.get(serverName);
        if (instance == null) {
            throw new ServerOperationException("服务器未运行: " + serverName);
        }
        processManager.forceStopServer(instance);
        activeServers.remove(serverName);
    }
    
    /**
     * 向服务器发送命令
     * @param serverName 服务器名称
     * @param command 命令
     * @throws ServerOperationException 如果发送失败
     */
    public void sendCommand(String serverName, String command) throws ServerOperationException {
        ServerInstance instance = activeServers.get(serverName);
        if (instance == null) {
            throw new ServerOperationException("服务器未运行: " + serverName);
        }
        processManager.sendCommand(instance, command);
    }
    
    /**
     * 获取所有活动服务器
     * @return 活动服务器映射
     */
    public Map<String, ServerInstance> getActiveServers() {
        return new HashMap<>(activeServers);
    }
    
    /**
     * 获取活动服务器实例
     * @param serverName 服务器名称
     * @return 服务器实例，如果不存在返回Optional.empty()
     */
    public Optional<ServerInstance> getActiveServer(String serverName) {
        return Optional.ofNullable(activeServers.get(serverName));
    }
    
    /**
     * 删除服务器
     * @param serverName 服务器名称
     * @param deleteFiles 是否删除本地文件
     * @throws ServerOperationException 如果删除失败
     */
    public void deleteServer(String serverName, boolean deleteFiles) 
            throws ServerOperationException, ConfigurationException {
        
        // 检查服务器是否正在运行
        if (activeServers.containsKey(serverName)) {
            throw new ServerOperationException("无法删除正在运行的服务器: " + serverName);
        }
        
        Optional<Server> serverOpt = configManager.findServerByName(serverName);
        if (!serverOpt.isPresent()) {
            throw new ServerOperationException("服务器不存在: " + serverName);
        }
        
        Server server = serverOpt.get();
        
        // 删除配置
        configManager.deleteServer(serverName);
        
        // 删除本地文件
        if (deleteFiles) {
            Path serverDir = Paths.get(server.getCorePath()).getParent();
            if (Files.exists(serverDir)) {
                FileUtils.deleteDirectory(serverDir);
                Logger.info("已删除服务器目录: " + serverDir);
            }
        }
        
        Logger.info("已删除服务器: " + serverName);
    }
    
    /**
     * 切换服务器核心版本
     * @param serverName 服务器名称
     * @param newCoreName 新核心文件名
     * @throws ServerOperationException 如果切换失败
     */
    public void switchCoreVersion(String serverName, String newCoreName) 
            throws ServerOperationException, ConfigurationException {
        
        Optional<Server> serverOpt = configManager.findServerByName(serverName);
        if (!serverOpt.isPresent()) {
            throw new ServerOperationException("服务器不存在: " + serverName);
        }
        
        Server server = serverOpt.get();
        String newVersion = extractVersion(newCoreName);
        server.setVersion(newVersion);
        
        configManager.saveServer(server);
        Logger.info("已更新服务器 " + serverName + " 的核心版本到: " + newVersion);
    }
    
    /**
     * 从文件名提取版本号
     */
    private String extractVersion(String fileName) {
        return fileName.replaceAll("[^0-9.]+", "");
    }
    
    /**
     * 创建备份
     * @param serverName 服务器名称
     * @return 备份目录路径
     * @throws ServerOperationException 如果备份失败
     */
    public Path createBackup(String serverName) throws ServerOperationException, ConfigurationException {
        Optional<Server> serverOpt = configManager.findServerByName(serverName);
        if (!serverOpt.isPresent()) {
            throw new ServerOperationException("服务器不存在: " + serverName);
        }
        
        Server server = serverOpt.get();
        Path serverDir = Paths.get(server.getCorePath()).getParent();
        
        // 创建备份目录
        String timestamp = new SimpleDateFormat(Constants.TIMESTAMP_FORMAT).format(new Date());
        Path backupDir = Paths.get(Constants.BACKUPS_DIR, timestamp, serverName);
        FileUtils.ensureDirectoryExists(backupDir);
        
        // 复制服务器目录
        FileUtils.copyDirectory(serverDir, backupDir);
        
        Logger.info("创建备份: " + backupDir);
        return backupDir;
    }
    
    public ConfigurationManager getConfigManager() {
        return configManager;
    }
}