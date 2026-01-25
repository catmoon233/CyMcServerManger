package exmo.cy.service;

import exmo.cy.exception.ServerOperationException;
import exmo.cy.model.ServerInstance;
import exmo.cy.service.ServerService;
import exmo.cy.util.Logger;
import exmo.cy.web.LogWebSocketHandler;

import java.io.*;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * 进程管理器
 * 负责管理服务器进程的输入输出流
 */
public class ProcessManager {
    
    // 存储服务器名称到实例的映射，用于WebSocket通信
    private final Map<ServerInstance, String> serverNames = new ConcurrentHashMap<>();
    private ServerService serverService;
    
    /**
     * 启动进程并设置输出监听
     * @param processBuilder 进程构建器
     * @return 服务器实例
     * @throws ServerOperationException 如果启动败
     */
    public ServerInstance startProcess(ProcessBuilder processBuilder) throws ServerOperationException {
        return startProcess(processBuilder, null);
    }
    
    /**
     * 启动进程并设置输出监听
     * @param processBuilder 进程构建器
     * @param serverService 服务器服务实例（用于检查屏蔽）
     * @return 服务器实例
     * @throws ServerOperationException 如果启动失败
     */
    public ServerInstance startProcess(ProcessBuilder processBuilder, ServerService serverService) throws ServerOperationException {
        try {
            this.serverService = serverService;
            Logger.info("启动进程: " + String.join(" ", processBuilder.command()));
            Process process = processBuilder.start();
            
            ServerInstance instance = new ServerInstance();
            instance.setProcess(process);
            instance.setProcessInput(process.getOutputStream());
            
            // 从目录获取服务器名称
            String serverName = extractServerNameFromDirectory(processBuilder.directory().getPath());
            setServerName(instance, serverName);
            
            Logger.info("服务器名称: " + serverName);
            
            // 启动输出监听线程 - 传递服务器名称用于日志记录
            startOutputGobbler(process.getInputStream(), serverName, output -> {
                LogWebSocketHandler.sendLogMessageWithBlockCheck(serverName, output, serverService);
                // 同时输出到控制台
                System.out.println("[SERVER " + serverName + "] " + output);
            });
            startOutputGobbler(process.getErrorStream(), serverName, error -> {
                LogWebSocketHandler.sendLogMessageWithBlockCheck(serverName, "[ERROR] " + error, serverService);
                // 同时输出到控制台
                System.err.println("[SERVER " + serverName + " ERROR] " + error);
            });
            
            return instance;
        } catch (IOException e) {
            throw new ServerOperationException("启动进程失败", e);
        }
    }
    
    /**
     * 从目录路径提取服务器名称
     */
    private String extractServerNameFromDirectory(String directoryPath) {
        // 从路径中提取服务器名称（通常是目录的最后一部分）
        String[] parts = directoryPath.split("[\\\\/]");
        if (parts.length > 0) {
            return parts[parts.length - 1];
        }
        return "unknown";
    }
    
    /**
     * 设置服务器名称与实例的关联
     */
    public void setServerName(ServerInstance instance, String serverName) {
        serverNames.put(instance, serverName);
    }
    
    /**
     * 获取服务器名称
     */
    public String getServerName(ServerInstance instance) {
        return serverNames.get(instance);
    }
    
    /**
     * 启动输出流读取线程
     * @param inputStream 输入流
     * @param serverName 服务器名称
     * @param consumer 输出消费者
     */
    private void startOutputGobbler(InputStream inputStream, String serverName, Consumer<String> consumer) {
        Thread thread = new Thread(new StreamGobbler(inputStream, serverName, consumer));
        thread.setDaemon(true);
        thread.start();
    }
    
    /**
     * 向进程发送命令
     * @param instance 服务器实例
     * @param command 命令
     * @throws ServerOperationException 如果发送失败
     */
    public void sendCommand(ServerInstance instance, String command) throws ServerOperationException {
        if (instance == null || !instance.isRunning()) {
            throw new ServerOperationException("服务器实例无效或未运行");
        }
        
        try {
            OutputStream input = instance.getProcessInput();
            if (input == null) {
                throw new ServerOperationException("无法获取服务器输入流");
            }
            input.write((command + "\n").getBytes("UTF-8"));  // 确保使用UTF-8编码并添加换行符
            input.flush();
            Logger.debug("发送命令到服务器: " + command);
            
            // 发送命令到WebSocket
            String serverName = getServerName(instance);
            if (serverName == null) {
                serverName = instance.getServerName() != null ? instance.getServerName() : "unknown";
            }
            LogWebSocketHandler.sendLogMessageWithBlockCheck(serverName, "[COMMAND SENT] " + command, serverService);
        } catch (IOException e) {
            Logger.error("发送命令失败", e);
            throw new ServerOperationException("发送命令失败", e);
        }
    }
    
    /**
     * 正常停止服务器
     * @param instance 服务器实例
     * @throws ServerOperationException 如果停止失败
     */
    public void stopServer(ServerInstance instance) throws ServerOperationException {
        if (instance == null || !instance.isRunning()) {
            throw new ServerOperationException("服务器实例无效或未运行");
        }
        
        try {
            // 发送stop命令
            sendCommand(instance, "stop");
            Logger.info("已发送停止命令到服务器");
        } catch (ServerOperationException e) {
            // 如果发送stop命令失败，尝试强制停止
            Logger.warn("发送停止命令失败，尝试强制停止");
            forceStopServer(instance);
        }
    }
    
    /**
     * 强制停止服务器
     * @param instance 服务器实例
     * @throws ServerOperationException 如果停止失败
     */
    public void forceStopServer(ServerInstance instance) throws ServerOperationException {
        if (instance == null || instance.getProcess() == null) {
            throw new ServerOperationException("服务器实例无效");
        }
        
        try {
            String serverName = getServerName(instance);
            if (serverName == null) {
                serverName = instance.getServerName() != null ? instance.getServerName() : "unknown";
            }
            LogWebSocketHandler.sendLogMessageWithBlockCheck(serverName, "[INFO] 强制终止服务器进程", serverService);
            
            Logger.info("强制终止服务器进程");
            instance.getProcess().destroyForcibly();
            int exitCode = instance.getProcess().waitFor();
            Logger.info("服务器进程已终止，退出代码: " + exitCode);
            
            LogWebSocketHandler.sendLogMessageWithBlockCheck(serverName, "[INFO] 服务器进程已终止，退出代码: " + exitCode, serverService);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ServerOperationException("等待进程终止时被中断", e);
        }
    }
    
    /**
     * 等待进程结束
     * @param instance 服务器实例
     * @return 退出代码
     * @throws ServerOperationException 如果等待失败
     */
    public int waitForProcess(ServerInstance instance) throws ServerOperationException {
        if (instance == null || instance.getProcess() == null) {
            throw new ServerOperationException("服务器实例无效");
        }
        
        try {
            return instance.getProcess().waitFor();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ServerOperationException("等待进程结束时被中断", e);
        }
    }
    
    /**
     * 流读取器内部类
     * 用于读取进程的输出流
     */
    private static class StreamGobbler implements Runnable {
        private final InputStream inputStream;
        private final Consumer<String> consumer;
        private final String serverName;
        
        public StreamGobbler(InputStream inputStream, String serverName, Consumer<String> consumer) {
            this.inputStream = inputStream;
            this.consumer = consumer;
            this.serverName = serverName;
        }
        
        @Override
        public void run() {
            // 设置当前线程的服务器名称上下文
            if (serverName != null && !serverName.isEmpty()) {
                Logger.setServerNameContext(serverName);
            }
            
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    consumer.accept(line);
                }
            } catch (IOException e) {
                Logger.debug("读取进程输出时出错: " + e.getMessage());
            } finally {
                // 清除线程本地存储
                Logger.clearServerNameContext();
            }
        }
    }
}