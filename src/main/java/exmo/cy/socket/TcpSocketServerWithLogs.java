package exmo.cy.socket;

import exmo.cy.config.ThreadConfig;
import exmo.cy.config.TcpServerConfig;
import exmo.cy.model.ServerInstance;
import exmo.cy.service.ServerService;
import exmo.cy.util.Logger;
import java.io.*;
import java.net.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;

/**
 * 增强版TCP Socket服务器
 * 支持日志推送、IP认证和配置文件
 */
public class TcpSocketServerWithLogs {
    private final TcpServerConfig config;
    private final ServerService serverService;
    private ServerSocket serverSocket;
    private volatile boolean running = false;
    private final ExecutorService executorService;
    private final Object shutdownLock = new Object();
    
    // 存储所有活跃的客户端连接
    private final Map<String, ClientConnection> activeConnections = new ConcurrentHashMap<>();
    private final AtomicInteger connectionCounter = new AtomicInteger(0);
    
    public TcpSocketServerWithLogs(TcpServerConfig config, ServerService serverService) {
        this.config = config;
        this.serverService = serverService;
        this.executorService = createSocketThreadPool();
    }
    
    /**
     * 创建Socket专用线程池
     */
    private ExecutorService createSocketThreadPool() {
        // 使用线程配置类获取推荐大小
        int poolSize = ThreadConfig.getRecommendedThreadPoolSize(2.0);
        poolSize = Math.min(config.getMaxConnections(), poolSize); // 限制最大线程数
        
        ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(
            poolSize, 
            ThreadConfig.createServiceThreadFactory("TcpSocketWithLogs")
        );
        
        // 设置合理的拒绝策略
        executor.setRejectedExecutionHandler(new RejectedExecutionHandler() {
            @Override
            public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
                Logger.warn("TCP Socket线程池已满，拒绝新连接请求");
                // 检查线程资源状况
                if (!ThreadConfig.isThreadResourceSufficient()) {
                    Logger.error("系统线程资源严重不足，建议减少服务器启动数量");
                    ThreadConfig.printDetailedThreadInfo();
                }
            }
        });
        
        return executor;
    }
    
    /**
     * 启动TCP Socket服务器
     */
    public void start() throws IOException {
        if (running) {
            throw new IllegalStateException("TCP Socket服务器已经在运行");
        }
        
        if (!config.isEnabled()) {
            Logger.info("TCP Socket服务器已禁用，跳过启动");
            return;
        }
        
        serverSocket = new ServerSocket(config.getPort());
        serverSocket.setSoTimeout(config.getConnectionTimeout());
        running = true;
        
        Logger.info("TCP Socket服务器启动，监听端口: " + config.getPort() + 
                   ", 允许IP: " + config.getAllowedIps() + 
                   ", 线程池配置: " + getSocketThreadPoolInfo());
        
        // 接受客户端连接的循环
        while (running) {
            try {
                Socket clientSocket = serverSocket.accept();
                
                // IP认证检查
                String clientIp = clientSocket.getInetAddress().getHostAddress();
                if (!config.isIpAllowed(clientIp)) {
                    Logger.warn("拒绝来自未授权IP的连接: " + clientIp);
                    clientSocket.close();
                    continue;
                }
                
                // 连接数限制检查
                if (activeConnections.size() >= config.getMaxConnections()) {
                    Logger.warn("达到最大连接数限制，拒绝新连接: " + clientIp);
                    PrintWriter writer = new PrintWriter(clientSocket.getOutputStream(), true);
                    writer.println("{\"status\":\"error\",\"message\":\"服务器连接数已满\"}");
                    clientSocket.close();
                    continue;
                }
                
                Logger.debug("新的TCP客户端连接: " + clientSocket.getRemoteSocketAddress());
                
                // 检查线程池状态
                if (executorService instanceof ThreadPoolExecutor) {
                    ThreadPoolExecutor tpe = (ThreadPoolExecutor) executorService;
                    if (tpe.getActiveCount() >= tpe.getMaximumPoolSize() * 0.8) {
                        Logger.warn("TCP Socket线程池使用率过高: " + 
                            String.format("%.1f%%", (double)tpe.getActiveCount()/tpe.getMaximumPoolSize()*100));
                    }
                }
                
                // 为每个客户端连接提交任务
                String connectionId = "conn-" + connectionCounter.incrementAndGet();
                ClientConnection clientConnection = new ClientConnection(connectionId, clientSocket, serverService);
                activeConnections.put(connectionId, clientConnection);
                executorService.submit(clientConnection);
            } catch (SocketTimeoutException e) {
                // 正常的超时，继续循环
            } catch (IOException e) {
                if (running) {
                    Logger.error("接受客户端连接时出错", e);
                }
            }
        }
    }
    
    /**
     * 获取Socket线程池信息
     */
    private String getSocketThreadPoolInfo() {
        if (executorService instanceof ThreadPoolExecutor) {
            ThreadPoolExecutor tpe = (ThreadPoolExecutor) executorService;
            return String.format("核心线程数:%d, 最大线程数:%d, 活跃线程数:%d, 队列大小:%d", 
                tpe.getCorePoolSize(), tpe.getMaximumPoolSize(), 
                tpe.getActiveCount(), tpe.getQueue().size());
        }
        return "未知线程池类型";
    }
    
    /**
     * 停止TCP Socket服务器
     */
    public void stop() throws IOException {
        synchronized (shutdownLock) {
            if (!running) {
                return;
            }
            
            Logger.info("正在停止TCP Socket服务器...");
            running = false;
            
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
            
            // 关闭所有客户端连接
            for (ClientConnection connection : activeConnections.values()) {
                connection.close();
            }
            activeConnections.clear();
            
            // 优雅关闭线程池
            executorService.shutdown();
            try {
                if (!executorService.awaitTermination(10, java.util.concurrent.TimeUnit.SECONDS)) {
                    Logger.warn("TCP Socket线程池关闭超时，强制关闭");
                    executorService.shutdownNow();
                    if (!executorService.awaitTermination(5, java.util.concurrent.TimeUnit.SECONDS)) {
                        Logger.error("TCP Socket线程池无法正常关闭");
                    }
                }
            } catch (InterruptedException e) {
                Logger.warn("TCP Socket服务器关闭过程中被中断");
                executorService.shutdownNow();
                Thread.currentThread().interrupt();
            }
            
            Logger.info("TCP Socket服务器已停止，线程池统计: " + getSocketThreadPoolInfo());
        }
    }
    
    /**
     * 检查服务器是否正在运行
     */
    public boolean isRunning() {
        return running && serverSocket != null && !serverSocket.isClosed();
    }
    
    /**
     * 向所有客户端发送日志消息
     */
    public void broadcastLogMessage(String message) {
        if (!running) {
            return;
        }
        
        List<String> failedConnections = new ArrayList<>();
        for (Map.Entry<String, ClientConnection> entry : activeConnections.entrySet()) {
            try {
                entry.getValue().sendLogMessage(message);
            } catch (Exception e) {
                Logger.warn("向客户端 " + entry.getKey() + " 发送日志失败: " + e.getMessage());
                failedConnections.add(entry.getKey());
            }
        }
        
        // 清理失败的连接
        for (String connectionId : failedConnections) {
            ClientConnection connection = activeConnections.remove(connectionId);
            if (connection != null) {
                connection.close();
            }
        }
    }
    
    /**
     * 向指定客户端发送日志消息
     */
    public void sendLogMessageToClient(String connectionId, String message) {
        ClientConnection connection = activeConnections.get(connectionId);
        if (connection != null) {
            try {
                connection.sendLogMessage(message);
            } catch (Exception e) {
                Logger.warn("向客户端 " + connectionId + " 发送日志失败: " + e.getMessage());
                activeConnections.remove(connectionId);
                connection.close();
            }
        }
    }
    
    /**
     * 客户端连接处理器
     */
    private class ClientConnection implements Runnable {
        private final String connectionId;
        private final Socket clientSocket;
        private final ServerService serverService;
        private volatile boolean connected = true;
        private PrintWriter writer;
        private BufferedReader reader;
        
        public ClientConnection(String connectionId, Socket clientSocket, ServerService serverService) {
            this.connectionId = connectionId;
            this.clientSocket = clientSocket;
            this.serverService = serverService;
        }
        
        @Override
        public void run() {
            try {
                reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                writer = new PrintWriter(clientSocket.getOutputStream(), true);
                
                // 发送欢迎消息
                sendResponse("{\"status\":\"ok\",\"message\":\"TCP Socket服务器已连接\",\"connection_id\":\"" + 
                           connectionId + "\",\"port\":" + clientSocket.getLocalPort() + "}");
                
                String inputLine;
                while (connected && (inputLine = reader.readLine()) != null) {
                    if ("quit".equalsIgnoreCase(inputLine.trim())) {
                        break;
                    }
                    
                    // 处理客户端命令
                    String response = processCommand(inputLine.trim());
                    sendResponse(response);
                }
            } catch (IOException e) {
                if (connected) {
                    Logger.error("处理客户端连接时出错: " + connectionId, e);
                }
            } finally {
                cleanup();
            }
        }
        
        /**
         * 处理客户端命令
         */
        private String processCommand(String command) {
            try {
                // 解析命令格式: command:arg1,arg2,arg3
                String[] parts = command.split(":", 2);
                if (parts.length < 1) {
                    return "{\"status\":\"error\",\"message\":\"无效的命令格式\"}";
                }
                
                String cmd = parts[0].toLowerCase().trim();
                String argsStr = parts.length > 1 ? parts[1] : "";
                
                switch (cmd) {
                    case "list-servers":
                        return handleListServers();
                    case "start-server":
                        return handleStartServer(argsStr);
                    case "stop-server":
                        return handleStopServer(argsStr);
                    case "server-status":
                        return handleServerStatus(argsStr);
                    case "create-server":
                        return handleCreateServer(argsStr);
                    case "delete-server":
                        return handleDeleteServer(argsStr);
                    case "health-check":
                        return handleHealthCheck();
                    case "list-running":
                        return handleListRunningServers();
                    case "send-command":
                        return handleSendCommand(argsStr);
                    case "server-info":
                        return handleServerInfo(argsStr);
                    case "subscribe-logs":
                        return handleSubscribeLogs(argsStr);
                    case "unsubscribe-logs":
                        return handleUnsubscribeLogs(argsStr);
                    case "help":
                        return handleHelp();
                    default:
                        return "{\"status\":\"error\",\"message\":\"未知命令: " + cmd + "\"}";
                }
            } catch (Exception e) {
                Logger.error("处理命令时出错: " + command, e);
                return "{\"status\":\"error\",\"message\":\"命令处理失败: " + e.getMessage() + "\"}";
            }
        }
        
        /**
         * 发送响应
         */
        private void sendResponse(String response) {
            if (writer != null && !writer.checkError()) {
                writer.println(response);
            }
        }
        
        /**
         * 发送日志消息
         */
        public void sendLogMessage(String message) throws IOException {
            if (writer != null && !writer.checkError()) {
                writer.println("{\"type\":\"log\",\"message\":" + jsonEscape(message) + "}");
            }
        }
        
        /**
         * JSON转义
         */
        private String jsonEscape(String str) {
            if (str == null) return "null";
            return "\"" + str.replace("\\", "\\\\")
                           .replace("\"", "\\\"")
                           .replace("\n", "\\n")
                           .replace("\r", "\\r")
                           .replace("\t", "\\t") + "\"";
        }
        
        /**
         * 处理帮助命令
         */
        private String handleHelp() {
            StringBuilder sb = new StringBuilder();
            sb.append("{\"status\":\"ok\",\"message\":\"TCP Socket服务器命令列表\",\"commands\":[");
            sb.append("{\"command\":\"list-servers\",\"description\":\"列出所有服务器\"},");
            sb.append("{\"command\":\"start-server:<serverName>\",\"description\":\"启动指定服务器\"},");
            sb.append("{\"command\":\"stop-server:<serverName>\",\"description\":\"停止指定服务器\"},");
            sb.append("{\"command\":\"server-status:<serverName>\",\"description\":\"查询服务器状态\"},");
            sb.append("{\"command\":\"list-running\",\"description\":\"列出运行中的服务器\"},");
            sb.append("{\"command\":\"send-command:<serverName>,<command>\",\"description\":\"向服务器发送命令\"},");
            sb.append("{\"command\":\"server-info:<serverName>\",\"description\":\"获取服务器详细信息\"},");
            sb.append("{\"command\":\"subscribe-logs\",\"description\":\"订阅日志推送\"},");
            sb.append("{\"command\":\"unsubscribe-logs\",\"description\":\"取消日志订阅\"},");
            sb.append("{\"command\":\"health-check\",\"description\":\"健康检查\"},");
            sb.append("{\"command\":\"help\",\"description\":\"显示帮助信息\"},");
            sb.append("{\"command\":\"quit\",\"description\":\"退出连接\"}");
            sb.append("]}");
            return sb.toString();
        }
        
        /**
         * 处理日志订阅命令
         */
        private String handleSubscribeLogs(String argsStr) {
            // 在这个实现中，所有连接默认都会收到日志
            // 可以扩展为按服务器或日志类型订阅
            return "{\"status\":\"ok\",\"message\":\"日志订阅成功\",\"connection_id\":\"" + connectionId + "\"}";
        }
        
        /**
         * 处理日志取消订阅命令
         */
        private String handleUnsubscribeLogs(String argsStr) {
            return "{\"status\":\"ok\",\"message\":\"日志取消订阅成功\",\"connection_id\":\"" + connectionId + "\"}";
        }
        
        // ... 其他handle方法保持与原TcpSocketServer相同 ...
        
        /**
         * 处理列出服务器命令
         */
        private String handleListServers() {
            try {
                var servers = serverService.getConfigManager().loadServers();
                StringBuilder response = new StringBuilder("{\"status\":\"ok\",\"servers\":[");
                
                for (int i = 0; i < servers.size(); i++) {
                    var server = servers.get(i);
                    response.append("{");
                    response.append("\"name\":\"").append(server.getName()).append("\",");
                    response.append("\"version\":\"").append(server.getVersion()).append("\",");
                    response.append("\"description\":\"").append(server.getDescription()).append("\",");
                    response.append("\"default_jvm_args\":\"").append(server.getDefaultJvmArgs() != null ? server.getDefaultJvmArgs() : "").append("\",");
                    response.append("\"default_server_args\":\"").append(server.getDefaultServerArgs() != null ? server.getDefaultServerArgs() : "").append("\"");
                    response.append("}");
                    
                    if (i < servers.size() - 1) {
                        response.append(",");
                    }
                }
                
                response.append("],\"total_count\":").append(servers.size());
                response.append(",\"active_servers\":").append(serverService.getActiveServers().size());
                response.append("}");
                
                return response.toString();
            } catch (Exception e) {
                return "{\"status\":\"error\",\"message\":\"列出服务器失败: " + e.getMessage() + "\"}";
            }
        }
        
        /**
         * 处理启动服务器命令
         */
        private String handleStartServer(String argsStr) {
            try {
                // 参数格式: serverName,launchMode,jvmArgs,serverArgs
                String[] args = argsStr.split(",", 4);
                if (args.length < 1) {
                    return "{\"status\":\"error\",\"message\":\"缺少服务器名称参数\"}";
                }
                
                String serverName = args[0].trim();
                
                var serverOpt = serverService.getConfigManager().findServerByName(serverName);
                if (!serverOpt.isPresent()) {
                    return "{\"status\":\"error\",\"message\":\"服务器不存在: " + serverName + "\"}";
                }
                
                var server = serverOpt.get();
                
                // 如果提供了启动模式，使用提供的模式，否则使用默认模式
                int launchMode = 1; // 默认模式
                if (args.length > 1 && !args[1].trim().isEmpty()) {
                    try {
                        launchMode = Integer.parseInt(args[1].trim());
                    } catch (NumberFormatException e) {
                        return "{\"status\":\"error\",\"message\":\"启动模式必须是数字\"}";
                    }
                }
                
                // 如果提供了JVM参数，使用提供的参数，否则使用默认参数
                String jvmArgs = null;
                if (args.length > 2 && !args[2].trim().isEmpty()) {
                    jvmArgs = args[2].trim();
                }
                
                // 如果提供了服务器参数，使用提供的参数，否则使用默认参数
                String serverArgs = null;
                if (args.length > 3 && !args[3].trim().isEmpty()) {
                    serverArgs = args[3].trim();
                }
                
                // 根据是否提供自定义参数决定使用哪个启动方法
                ServerInstance instance;
                if (jvmArgs != null || serverArgs != null) {
                    // 使用自定义参数启动服务器
                    instance = serverService.startServer(server, launchMode, null, jvmArgs, serverArgs);
                } else {
                    // 使用默认参数启动服务器
                    instance = serverService.startServerWithDefaults(server, launchMode, null);
                }
                
                return "{\"status\":\"ok\",\"message\":\"服务器 " + serverName + " 启动成功\",\"process_id\":\"" + 
                       instance.getProcess().pid() + "\"}";
            } catch (Exception e) {
                return "{\"status\":\"error\",\"message\":\"启动服务器失败: " + e.getMessage() + "\"}";
            }
        }
        
        /**
         * 处理停止服务器命令
         */
        private String handleStopServer(String argsStr) {
            try {
                // 参数格式: serverName
                String[] args = argsStr.split(",");
                if (args.length < 1) {
                    return "{\"status\":\"error\",\"message\":\"缺少服务器名称参数\"}";
                }
                
                String serverName = args[0].trim();
                
                serverService.stopServer(serverName);
                
                return "{\"status\":\"ok\",\"message\":\"服务器 " + serverName + " 停止命令已发送\"}";
            } catch (Exception e) {
                return "{\"status\":\"error\",\"message\":\"停止服务器失败: " + e.getMessage() + "\"}";
            }
        }
        
        /**
         * 处理服务器状态命令
         */
        private String handleServerStatus(String argsStr) {
            try {
                // 参数格式: serverName
                String[] args = argsStr.split(",");
                if (args.length < 1) {
                    return "{\"status\":\"error\",\"message\":\"缺少服务器名称参数\"}";
                }
                
                String serverName = args[0].trim();
                
                var activeServers = serverService.getActiveServers();
                boolean isRunning = activeServers.containsKey(serverName);
                
                return "{\"status\":\"ok\",\"server\":\"" + serverName + "\",\"running\":" + isRunning + "}";
            } catch (Exception e) {
                return "{\"status\":\"error\",\"message\":\"查询服务器状态失败: " + e.getMessage() + "\"}";
            }
        }
        
        /**
         * 处理健康检查命令
         */
        private String handleHealthCheck() {
            return "{\"status\":\"ok\",\"message\":\"TCP Socket服务器运行正常\",\"timestamp\":" + System.currentTimeMillis() + "}";
        }
        
        /**
         * 处理创建服务器命令
         */
        private String handleCreateServer(String argsStr) {
            // 创建服务器需要更复杂的参数，这里简化处理
            return "{\"status\":\"error\",\"message\":\"创建服务器命令需要更多参数，建议使用控制台命令\"}";
        }
        
        /**
         * 处理删除服务器命令
         */
        private String handleDeleteServer(String argsStr) {
            // 删除服务器需要更复杂的参数，这里简化处理
            return "{\"status\":\"error\",\"message\":\"删除服务器命令需要更多参数，建议使用控制台命令\"}";
        }
        
        /**
         * 处理列出运行中服务器命令
         */
        private String handleListRunningServers() {
            try {
                var activeServers = serverService.getActiveServers();
                StringBuilder response = new StringBuilder("{\"status\":\"ok\",\"running_servers\":[");
                
                var entries = activeServers.entrySet().iterator();
                while (entries.hasNext()) {
                    var entry = entries.next();
                    var server = entry.getValue().getServer();
                    response.append("{");
                    response.append("\"name\":\"").append(entry.getKey()).append("\",");
                    response.append("\"version\":\"").append(server.getVersion()).append("\",");
                    response.append("\"pid\":\"").append(entry.getValue().getProcess().pid()).append("\"");
                    response.append("}");
                    
                    if (entries.hasNext()) {
                        response.append(",");
                    }
                }
                
                response.append("],\"count\":").append(activeServers.size());
                response.append("}");
                
                return response.toString();
            } catch (Exception e) {
                return "{\"status\":\"error\",\"message\":\"列出运行中服务器失败: " + e.getMessage() + "\"}";
            }
        }
        
        /**
         * 处理发送命令到服务器命令
         */
        private String handleSendCommand(String argsStr) {
            try {
                // 参数格式: serverName,command
                String[] args = argsStr.split(",", 2);
                if (args.length < 2) {
                    return "{\"status\":\"error\",\"message\":\"需要服务器名称和命令参数\"}";
                }
                
                String serverName = args[0].trim();
                String command = args[1].trim();
                
                serverService.sendCommand(serverName, command);
                
                return "{\"status\":\"ok\",\"message\":\"命令已发送到服务器 " + serverName + "\"}";
            } catch (Exception e) {
                return "{\"status\":\"error\",\"message\":\"发送命令失败: " + e.getMessage() + "\"}";
            }
        }
        
        /**
         * 处理获取服务器信息命令
         */
        private String handleServerInfo(String argsStr) {
            try {
                // 参数格式: serverName
                String[] args = argsStr.split(",");
                if (args.length < 1) {
                    return "{\"status\":\"error\",\"message\":\"缺少服务器名称参数\"}";
                }
                
                String serverName = args[0].trim();
                
                var serverOpt = serverService.getConfigManager().findServerByName(serverName);
                if (!serverOpt.isPresent()) {
                    return "{\"status\":\"error\",\"message\":\"服务器不存在: " + serverName + "\"}";
                }
                
                var server = serverOpt.get();
                var activeServers = serverService.getActiveServers();
                boolean isRunning = activeServers.containsKey(serverName);
                
                StringBuilder response = new StringBuilder();
                response.append("{\"status\":\"ok\",\"server\":{");
                response.append("\"name\":\"").append(server.getName()).append("\",");
                response.append("\"version\":\"").append(server.getVersion()).append("\",");
                response.append("\"description\":\"").append(server.getDescription()).append("\",");
                response.append("\"core_path\":\"").append(server.getCorePath()).append("\",");
                response.append("\"default_jvm_args\":\"").append(server.getDefaultJvmArgs() != null ? server.getDefaultJvmArgs() : "").append("\",");
                response.append("\"default_server_args\":\"").append(server.getDefaultServerArgs() != null ? server.getDefaultServerArgs() : "").append("\",");
                response.append("\"is_running\":").append(isRunning);
                response.append("}}");
                
                return response.toString();
            } catch (Exception e) {
                return "{\"status\":\"error\",\"message\":\"获取服务器信息失败: " + e.getMessage() + "\"}";
            }
        }
        
        /**
         * 清理资源
         */
        private void cleanup() {
            connected = false;
            activeConnections.remove(connectionId);
            
            try {
                if (reader != null) {
                    reader.close();
                }
            } catch (IOException e) {
                Logger.error("关闭读取器时出错", e);
            }
            
            try {
                if (writer != null) {
                    writer.close();
                }
            } catch (Exception e) {
                Logger.error("关闭写入器时出错", e);
            }
            
            try {
                if (clientSocket != null && !clientSocket.isClosed()) {
                    clientSocket.close();
                }
            } catch (IOException e) {
                Logger.error("关闭客户端连接时出错", e);
            }
            
            Logger.debug("客户端连接已清理: " + connectionId);
        }
        
        /**
         * 关闭连接
         */
        public void close() {
            cleanup();
        }
    }
}