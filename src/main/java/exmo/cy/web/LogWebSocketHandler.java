package exmo.cy.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import exmo.cy.security.JwtUtil;
import exmo.cy.security.UserDetailsServiceImpl;
import exmo.cy.service.ServerService;
import java.util.concurrent.CopyOnWriteArrayList;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * WebSocket日志处理器
 * 用于实时传输服务器控制台输出和处理客户端命令
 */
public class LogWebSocketHandler extends TextWebSocketHandler {

    // 存储所有WebSocket会话，按服务器名称分类，value 使用线程安全的列表
    private static final Map<String, List<WebSocketSession>> serverSessions = new ConcurrentHashMap<>();
    private static final ObjectMapper objectMapper = new ObjectMapper();

    // 用于依赖注入的字段
    @org.springframework.beans.factory.annotation.Autowired
    public JwtUtil jwtUtil;
    @org.springframework.beans.factory.annotation.Autowired
    public UserDetailsServiceImpl userDetailsService;
    @org.springframework.beans.factory.annotation.Autowired
    public ServerService serverService; // 添加ServerService依赖
    
    // 静态引用以便在静态方法中访问
    private static LogWebSocketHandler instance;
    
    // 静态引用的ServerService，用于日志发送时的屏蔽检查
    private static ServerService staticServerService;
    
    public LogWebSocketHandler() {
        instance = this;
    }
    
    // Setter方法用于注入ServerService到静态上下文
    public void setStaticServerService(ServerService serverService) {
        staticServerService = serverService;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        try {
            // 从URI获取服务器名称
            String uri = session.getUri().toString();
            System.out.println("WebSocket连接请求 URI: " + uri);
            
            String serverName = extractServerNameFromUri(uri);
            System.out.println("提取的服务器名称: " + serverName);
            
            // 验证JWT令牌
            String token = extractTokenFromUri(uri);
            if (token == null || token.isEmpty()) {
                System.err.println("WebSocket连接认证失败: 令牌为空");
                session.close(CloseStatus.NOT_ACCEPTABLE.withReason("认证失败: 缺少令牌"));
                return;
            }
            
            System.out.println("提取的令牌: " + (token.length() > 20 ? token.substring(0, 20) + "..." : token));
            
            if (validateToken(token)) {
                System.out.println("新的WebSocket连接到服务器 " + serverName + ": " + session.getId());
                serverSessions.computeIfAbsent(serverName, k -> new CopyOnWriteArrayList<>()).add(session);
                
                // 发送连接成功的确认消息
                sendMessageToSession(session, "[INFO] WebSocket连接已建立，正在连接到 " + serverName + " 控制台...");
            } else {
                System.err.println("WebSocket连接认证失败: 令牌验证失败");
                session.close(CloseStatus.NOT_ACCEPTABLE.withReason("认证失败: 令牌无效"));
                return;
            }
        } catch (Exception e) {
            System.err.println("WebSocket连接建立时发生异常: " + e.getMessage());
            e.printStackTrace();
            try {
                session.close(CloseStatus.SERVER_ERROR.withReason("服务器错误: " + e.getMessage()));
            } catch (Exception closeException) {
                System.err.println("关闭WebSocket连接时出错: " + closeException.getMessage());
            }
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, org.springframework.web.socket.TextMessage message) throws Exception {
        String payload = message.getPayload();
        String uri = session.getUri().toString();
        String serverName = extractServerNameFromUri(uri);
        
        // 解析客户端发送的命令
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> commandData = objectMapper.readValue(payload, Map.class);
            String command = commandData.get("command") != null ? commandData.get("command").toString() : null;

            if (command != null && !command.trim().isEmpty()) {
                // 将命令发送到对应的服务器实例
                if (serverService != null) {
                    try {
                        serverService.sendCommand(serverName, command);
                        System.out.println("已将命令发送到服务器 " + serverName + ": " + command);
                        // 服务器服务类内部会把命令回显到WebSocket
                    } catch (Exception e) {
                        System.err.println("发送命令到服务器失败: " + e.getMessage());
                        sendMessageToSession(session, "[ERROR] 发送命令失败: " + e.getMessage());
                    }
                } else {
                    System.err.println("ServerService未注入，无法发送命令到服务器");
                    sendMessageToSession(session, "[ERROR] 服务器服务不可用");
                }
            } else {
                // 如果没有command字段，直接发送原始消息作为命令
                if (serverService != null) {
                    try {
                        serverService.sendCommand(serverName, payload);
                        System.out.println("已将命令发送到服务器 " + serverName + ": " + payload);
                    } catch (Exception e) {
                        System.err.println("发送命令到服务器失败: " + e.getMessage());
                        sendMessageToSession(session, "[ERROR] 发送命令失败: " + e.getMessage());
                    }
                } else {
                    System.err.println("ServerService未注入，无法发送命令到服务器");
                    sendMessageToSession(session, "[ERROR] 服务器服务不可用");
                }
            }
        } catch (Exception e) {
            System.err.println("处理WebSocket消息失败: " + e.getMessage());
            sendMessageToSession(session, "错误: 无法解析命令 - " + e.getMessage());
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        // 从会话列表中移除断开的连接
        for (Map.Entry<String, List<WebSocketSession>> entry : serverSessions.entrySet()) {
            List<WebSocketSession> sessions = entry.getValue();
            if (sessions != null && sessions.remove(session)) {
                System.out.println("从服务器 '" + entry.getKey() + "' 的会话列表中移除连接: " + session.getId());
                
                // 如果该服务器的会话列表为空，可以选择保留或移除
                if (sessions.isEmpty()) {
                    System.out.println("服务器 '" + entry.getKey() + "' 的所有WebSocket连接已断开");
                }
            }
        }
        System.out.println("WebSocket连接关闭: " + session.getId() + ", 状态码: " + status.getCode() + ", 原因: " + status.getReason());
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        System.err.println("WebSocket传输错误: " + exception.getMessage());
        exception.printStackTrace();
        
        // 尝试发送错误消息到客户端
        try {
            if (session.isOpen()) {
                sendMessageToSession(session, "[ERROR] WebSocket传输错误: " + exception.getMessage());
            }
        } catch (Exception e) {
            System.err.println("发送错误消息失败: " + e.getMessage());
        }
        
        super.handleTransportError(session, exception);
    }

    /**
     * 从URI中提取JWT令牌
     */
    private String extractTokenFromUri(String uri) {
        // 兼容完整URI与路径风格，优先使用URI解析query
        try {
            URI u = new URI(uri);
            String query = u.getQuery();
            if (query != null) {
                String[] params = query.split("&");
                for (String p : params) {
                    if (p.startsWith("token=")) {
                        String value = p.substring(6);
                        return URLDecoder.decode(value, StandardCharsets.UTF_8.name());
                    }
                }
            }
        } catch (Exception e) {
            // 如果URI解析失败，回退到简单字符串查找
            if (uri.contains("?token=")) {
                int tokenIndex = uri.indexOf("?token=");
                String token = uri.substring(tokenIndex + 7);
                return token.replace("%3D", "=");
            }
        }
        return null;
    }

    /**
     * 验证JWT令牌
     */
    private boolean validateToken(String token) {
        try {
            if (token == null || token.isEmpty()) {
                System.err.println("令牌验证失败: 令牌为空");
                return false;
            }

            // 去掉可能的Bearer前缀
            if (token.startsWith("Bearer ")) {
                token = token.substring(7);
            }

            // URL解码token（如果被编码了）
            try {
                token = URLDecoder.decode(token, StandardCharsets.UTF_8.name());
            } catch (Exception e) {
                // 如果解码失败，使用原始token
            }

            String username = jwtUtil.extractUsername(token);
            if (username == null || username.isEmpty()) {
                System.err.println("令牌验证失败: 无法提取用户名");
                return false;
            }

            // 验证令牌
            boolean isValid = jwtUtil.validateToken(token, username);
            if (!isValid) {
                System.err.println("令牌验证失败: 令牌无效或已过期");
                return false;
            }

            // 如果SecurityContext中没有认证信息，设置它
            if (SecurityContextHolder.getContext().getAuthentication() == null) {
                try {
                    // 加载用户详细信息
                    org.springframework.security.core.userdetails.UserDetails userDetails = 
                        userDetailsService.loadUserByUsername(username);

                    // 设置认证信息
                    UsernamePasswordAuthenticationToken authentication = 
                        new UsernamePasswordAuthenticationToken(
                            userDetails, null, userDetails.getAuthorities());
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                } catch (Exception e) {
                    System.err.println("设置认证信息时出错: " + e.getMessage());
                    // 即使设置认证信息失败，如果token有效，仍然允许连接
                }
            }

            System.out.println("令牌验证成功: 用户 " + username);
            return true;
        } catch (Exception e) {
            System.err.println("令牌验证异常: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 提取URI中的服务器名称
     */
    private String extractServerNameFromUri(String uri) {
        try {
            URI u = new URI(uri);
            String path = u.getPath(); // e.g. /ws/logs/estt or /ws/logs/estt/info
            if (path != null && !path.isEmpty()) {
                String[] segs = path.split("/");
                // 找到"logs"段，下一段应为serverName
                for (int i = 0; i < segs.length; i++) {
                    if ("logs".equals(segs[i]) && i + 1 < segs.length) {
                        String candidate = segs[i + 1];
                        if (candidate != null && !candidate.isEmpty()) {
                            // 检查是否是info路径，如果是则再往前一个段
                            if ("info".equals(candidate)) {
                                if (i > 0) {
                                    candidate = segs[i - 1];
                                }
                            } else {
                                // 如果候选者是info，说明前面的才是服务器名称
                                if ("info".equals(candidate) && i > 0) {
                                    candidate = segs[i - 1];
                                }
                            }
                            
                            // 移除可能的查询参数片段
                            if (candidate.contains("?")) {
                                candidate = candidate.split("\\?")[0];
                            }
                            
                            // URL解码服务器名称
                            try {
                                return URLDecoder.decode(candidate, StandardCharsets.UTF_8.name());
                            } catch (Exception e) {
                                return candidate;
                            }
                        }
                    }
                }
                // 回退：取path中最后一个非空段（排除info）
                for (int i = segs.length - 1; i >= 0; i--) {
                    String s = segs[i];
                    if (s != null && !s.isEmpty() && !"info".equals(s)) {
                        // 移除可能的查询参数片段
                        if (s.contains("?")) {
                            s = s.split("\\?")[0];
                        }
                        try {
                            return URLDecoder.decode(s, StandardCharsets.UTF_8.name());
                        } catch (Exception e) {
                            return s;
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("解析URI时出错: " + e.getMessage());
            // 回退到旧解析方式
            String[] parts = uri.split("/");
            if (parts.length >= 4) {
                String serverNamePart = parts[3];
                if (serverNamePart.contains("?")) {
                    serverNamePart = serverNamePart.split("\\?")[0];
                }
                try {
                    return URLDecoder.decode(serverNamePart, StandardCharsets.UTF_8.name());
                } catch (Exception decodeException) {
                    return serverNamePart;
                }
            }
        }
        System.err.println("无法从URI提取服务器名称: " + uri);
        return "unknown";
    }

    /**
     * 发送消息到特定会话
     */
    private void sendMessageToSession(WebSocketSession session, String message) {
        if (session.isOpen()) {
            try {
                session.sendMessage(new org.springframework.web.socket.TextMessage(message));
            } catch (IOException e) {
                System.err.println("发送WebSocket消息失败: " + e.getMessage());
            }
        }
    }

    /**
     * 发送日志消息给指定服务器的所有连接客户端
     */
    public static void sendLogMessage(String serverName, String message) {
        if (serverName == null || serverName.isEmpty()) {
            System.err.println("[警告] sendLogMessage: serverName为null或为空，日志: " + message);
            return;
        }
        
        // 检查服务器是否被屏蔽
        if (staticServerService != null && staticServerService.isServerBlocked(serverName)) {
            // 服务器被屏蔽，不发送消息
            return;
        }
        
        List<WebSocketSession> sessions = serverSessions.get(serverName);
        if (sessions == null || sessions.isEmpty()) {
            // 服务器没有WebSocket连接，不输出调试信息
            return;
        }
        
        for (WebSocketSession session : sessions) {
            if (session.isOpen()) {
                try {
                    session.sendMessage(new org.springframework.web.socket.TextMessage(message));
                } catch (IOException e) {
                    System.err.println("[错误] 发送WebSocket消息失败: " + e.getMessage());
                    // 如果发送失败，从列表中移除该会话
                    sessions.remove(session);
                }
            }
        }
    }
    
    /**
     * 发送命令响应到指定服务器的所有连接客户端
     */
    public static void sendCommandResponse(String serverName, String command, String response) {
        List<WebSocketSession> sessions = serverSessions.get(serverName);
        if (sessions != null) {
            for (WebSocketSession session : sessions) {
                if (session.isOpen()) {
                    try {
                        String message = "[" + command + "] " + response;
                        session.sendMessage(new org.springframework.web.socket.TextMessage(message));
                    } catch (IOException e) {
                        System.err.println("发送命令响应失败: " + e.getMessage());
                        sessions.remove(session);
                    }
                }
            }
        }
    }
    
    /**
     * 移除特定服务器的所有连接
     */
    public static void removeServerConnections(String serverName) {
        serverSessions.remove(serverName);
    }
    
    /**
     * 发送日志消息给指定服务器的所有连接客户端（带屏蔽检查）
     * @param serverName 服务器名称
     * @param message 消息内容
     * @param serverService 服务器服务实例
     */
    public static void sendLogMessageWithBlockCheck(String serverName, String message, ServerService serverService) {
        if (serverName == null || serverName.isEmpty()) {
            System.err.println("[警告] sendLogMessageWithBlockCheck: serverName为null或为空，日志: " + message);
            return;
        }
        
        // 检查服务器是否被屏蔽 - 优先使用传入的serverService，如果为null则使用静态引用
        ServerService serviceToUse = serverService != null ? serverService : staticServerService;
        if (serviceToUse != null && serviceToUse.isServerBlocked(serverName)) {
            // 服务器被屏蔽，不发送消息
            return;
        }
        
        sendLogMessage(serverName, message);
    }
    
    /**
     * 发送日志消息给指定服务器的所有连接客户端（带屏蔽检查，使用静态引用）
     * @param serverName 服务器名称
     * @param message 消息内容
     */
    public static void sendLogMessageWithBlockCheck(String serverName, String message) {
        if (serverName == null || serverName.isEmpty()) {
            System.err.println("[警告] sendLogMessageWithBlockCheck: serverName为null或为空，日志: " + message);
            return;
        }
        
        // 检查服务器是否被屏蔽
        if (staticServerService != null && staticServerService.isServerBlocked(serverName)) {
            // 服务器被屏蔽，不发送消息
            return;
        }
        
        sendLogMessage(serverName, message);
    }
    
//    /**
//     * 发送日志消息给指定服务器的所有连接客户端（带屏蔽检查，使用静态引用）
//     * @param serverName 服务器名称
//     * @param message 消息内容
//     */
//    public static void sendLogMessageWithBlockCheck(String serverName, String message) {
//        sendLogMessageWithBlockCheck(serverName, message, staticServerService);
//    }
}