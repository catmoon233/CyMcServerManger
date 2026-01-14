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

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        // 从URI获取服务器名称
        String uri = session.getUri().toString();
        String serverName = extractServerNameFromUri(uri);
        
        // 验证JWT令牌
        String token = extractTokenFromUri(uri);
        if (token != null && validateToken(token)) {
            System.out.println("新的WebSocket连接到服务器 " + serverName + ": " + session.getId());
            serverSessions.computeIfAbsent(serverName, k -> new CopyOnWriteArrayList<>()).add(session);
            
            // 发送连接成功的确认消息
            sendMessageToSession(session, "WebSocket连接已建立，正在连接到 " + serverName + " 控制台...");
        } else {
            System.err.println("WebSocket连接认证失败，关闭连接");
            session.close(CloseStatus.NOT_ACCEPTABLE.withReason("认证失败"));
            return;
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, org.springframework.web.socket.TextMessage message) throws Exception {
        String payload = message.getPayload();
        String uri = session.getUri().toString();
        String serverName = extractServerNameFromUri(uri);
        
        // 解析客户端发送的命令
        try {
            Map<String, String> commandData = objectMapper.readValue(payload, Map.class);
            String command = commandData.get("command");

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
        for (List<WebSocketSession> sessions : serverSessions.values()) {
            sessions.remove(session);
        }
        System.out.println("WebSocket连接关闭: " + session.getId() + ", 状态: " + status);
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        System.err.println("WebSocket传输错误: " + exception.getMessage());
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
                return false;
            }

            // 去掉可能的Bearer前缀
            if (token.startsWith("Bearer ")) {
                token = token.substring(7);
            }

            String username = jwtUtil.extractUsername(token);
            if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                // 加载用户详细信息
                org.springframework.security.core.userdetails.User userDetails = 
                    (org.springframework.security.core.userdetails.User) userDetailsService.loadUserByUsername(username);

                // 验证令牌
                if (jwtUtil.validateToken(token, username)) {
                    // 设置认证信息
                    UsernamePasswordAuthenticationToken authentication = 
                        new UsernamePasswordAuthenticationToken(
                            userDetails, null, userDetails.getAuthorities());
                    // 不设置details，因为在WebSocket上下文中可能没有HTTP请求
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                    return true;
                }
            }
            return false;
        } catch (Exception e) {
            System.err.println("令牌验证失败: " + e.getMessage());
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
                            return candidate;
                        }
                    }
                }
                // 回退：取path中最后一个非空段（排除info）
                for (int i = segs.length - 1; i >= 0; i--) {
                    String s = segs[i];
                    if (s != null && !s.isEmpty() && !"info".equals(s)) {
                        return s;
                    }
                }
            }
        } catch (Exception e) {
            // 回退到旧解析方式
            String[] parts = uri.split("/");
            if (parts.length >= 4) {
                String serverNamePart = parts[3];
                if (serverNamePart.contains("?")) {
                    serverNamePart = serverNamePart.split("\\?")[0];
                }
                return serverNamePart;
            }
        }
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
        
        List<WebSocketSession> sessions = serverSessions.get(serverName);
        if (sessions == null || sessions.isEmpty()) {
            System.out.println("[以信] 服务器 '" + serverName + "' 消息队列为null或为空，待客户端连接，消息: " + message);
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
}