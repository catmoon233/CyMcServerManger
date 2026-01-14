package exmo.cy.web;

import exmo.cy.security.JwtUtil;
import exmo.cy.security.UserDetailsServiceImpl;
import exmo.cy.service.ServerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;

/**
 * WebSocket配置类
 * 配置WebSocket端点和处理器
 */
@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private UserDetailsServiceImpl userDetailsService;
    
    @Autowired
    private ServerService serverService;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        // 注册日志处理器，支持动态服务器名称路由
        registry.addHandler(logWebSocketHandler(), "/ws/logs/{serverName}")
                .addInterceptors(webSocketHandshakeInterceptor())
                .setAllowedOrigins("*");
                
        // 添加sockjs支持作为备用
        registry.addHandler(logWebSocketHandler(), "/ws/logs/{serverName}/info")
                .addInterceptors(webSocketHandshakeInterceptor())
                .setAllowedOrigins("*");
    }

    @Bean
    public LogWebSocketHandler logWebSocketHandler() {
        LogWebSocketHandler handler = new LogWebSocketHandler();
        handler.jwtUtil = jwtUtil;
        handler.userDetailsService = userDetailsService;
        handler.serverService = serverService; // 注入ServerService
        return handler;
    }

    @Bean
    public WebSocketHandshakeInterceptor webSocketHandshakeInterceptor() {
        return new WebSocketHandshakeInterceptor(jwtUtil);
    }
    
    /**
     * WebSocket握手拦截器，用于验证认证状态
     */
    public static class WebSocketHandshakeInterceptor implements HandshakeInterceptor {

        private JwtUtil jwtUtil;

        public WebSocketHandshakeInterceptor(JwtUtil jwtUtil) {
            this.jwtUtil = jwtUtil;
        }

        @Override
        public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response, 
                                     WebSocketHandler wsHandler, Map<String, Object> attributes) throws Exception {
            // 从查询参数中获取token
            String token = null;
            if (request.getURI().getQuery() != null) {
                String query = request.getURI().getQuery();
                String[] params = query.split("&");
                for (String param : params) {
                    if (param.startsWith("token=")) {
                        token = param.substring(6); // "token=".length()
                        break;
                    }
                }
            }

            // 验证JWT令牌
            if (token == null || token.isEmpty()) {
                System.err.println("WebSocket握手失败: 缺少认证令牌");
                response.setStatusCode(HttpStatus.UNAUTHORIZED);
                return false;
            }

            // 验证token
            try {
                String username = jwtUtil.extractUsername(token);
                if (username == null || !jwtUtil.validateToken(token, username)) {
                    System.err.println("WebSocket握手失败: 无效的认证令牌");
                    response.setStatusCode(HttpStatus.UNAUTHORIZED);
                    return false;
                }
            } catch (Exception e) {
                System.err.println("WebSocket握手失败: 令牌验证异常 - " + e.getMessage());
                response.setStatusCode(HttpStatus.UNAUTHORIZED);
                return false;
            }

            System.out.println("WebSocket握手成功: 用户 " + jwtUtil.extractUsername(token));
            return true;
        }

        @Override
        public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response, 
                                 WebSocketHandler wsHandler, Exception exception) {
            // 握手后处理
            if (exception != null) {
                System.err.println("WebSocket握手过程中发生异常: " + exception.getMessage());
            } else {
                System.out.println("WebSocket握手完成");
            }
        }
    }
}