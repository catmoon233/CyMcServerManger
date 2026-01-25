package exmo.cy.socket;

import exmo.cy.model.ServerInstance;
import exmo.cy.service.ServerService;
import exmo.cy.util.Logger;
import java.io.*;
import java.net.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * TCP Socket服务器
 * 提供远程API接口，支持查询服务器资源、启动服务器等操作
 */
public class TcpSocketServer {
    private static final int DEFAULT_PORT = 5245; // 使用5245端口，避免与Web端口冲突
    
    private final int port;
    private final ServerService serverService;
    private ServerSocket serverSocket;
    private volatile boolean running = false;
    private final ExecutorService executorService;
    
    public TcpSocketServer(ServerService serverService) {
        this(DEFAULT_PORT, serverService);
    }
    
    public TcpSocketServer(int port, ServerService serverService) {
        this.port = port;
        this.serverService = serverService;
        this.executorService = Executors.newCachedThreadPool();
    }
    
    /**
     * 启动TCP Socket服务器
     */
    public void start() throws IOException {
        if (running) {
            throw new IllegalStateException("TCP Socket服务器已经在运行");
        }
        
        serverSocket = new ServerSocket(port);
        running = true;
        
        Logger.info("TCP Socket服务器启动，监听端口: " + port);
        
        // 接受客户端连接的循环
        while (running) {
            try {
                Socket clientSocket = serverSocket.accept();
                Logger.debug("新的TCP客户端连接: " + clientSocket.getRemoteSocketAddress());
                
                // 为每个客户端连接创建一个处理器
                executorService.submit(new ClientHandler(clientSocket, serverService));
            } catch (IOException e) {
                if (running) {
                    Logger.error("接受客户端连接时出错", e);
                }
            }
        }
    }
    
    /**
     * 停止TCP Socket服务器
     */
    public void stop() throws IOException {
        running = false;
        
        if (serverSocket != null && !serverSocket.isClosed()) {
            serverSocket.close();
        }
        
        executorService.shutdown();
        Logger.info("TCP Socket服务器已停止");
    }
    
    /**
     * 检查服务器是否正在运行
     */
    public boolean isRunning() {
        return running && serverSocket != null && !serverSocket.isClosed();
    }
    
    /**
     * 客户端处理器
     */
    private static class ClientHandler implements Runnable {
        private final Socket clientSocket;
        private final ServerService serverService;
        
        public ClientHandler(Socket clientSocket, ServerService serverService) {
            this.clientSocket = clientSocket;
            this.serverService = serverService;
        }
        
        @Override
        public void run() {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                 PrintWriter writer = new PrintWriter(clientSocket.getOutputStream(), true)) {
                
                // 发送欢迎消息
                writer.println("{\"status\":\"ok\",\"message\":\"TCP Socket服务器已连接\",\"port\":" + clientSocket.getLocalPort() + "}");
                
                String inputLine;
                while ((inputLine = reader.readLine()) != null) {
                    if ("quit".equalsIgnoreCase(inputLine.trim())) {
                        break;
                    }
                    
                    // 处理客户端命令
                    String response = processCommand(inputLine.trim());
                    writer.println(response);
                }
            } catch (IOException e) {
                Logger.error("处理客户端连接时出错", e);
            } finally {
                try {
                    clientSocket.close();
                } catch (IOException e) {
                    Logger.error("关闭客户端连接时出错", e);
                }
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
            sb.append("{\"command\":\"health-check\",\"description\":\"健康检查\"},");
            sb.append("{\"command\":\"help\",\"description\":\"显示帮助信息\"},");
            sb.append("{\"command\":\"quit\",\"description\":\"退出连接\"}");
            sb.append("]}");
            return sb.toString();
        }
        
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
    }
}