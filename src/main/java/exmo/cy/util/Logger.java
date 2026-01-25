package exmo.cy.util;

import exmo.cy.web.LogWebSocketHandler;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 简单的日志工具类
 * 提供统一的日志输出格式和级别控制
 */
public final class Logger {
    
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static LogLevel currentLevel = LogLevel.INFO;
    
    // 线程本地存储，用于追踪当前线程的服务器名称上下文
    private static final ThreadLocal<String> serverNameContext = ThreadLocal.withInitial(() -> "CONSOLE");
    
    // 防止实例化
    private Logger() {
        throw new UnsupportedOperationException("这是一个工具类，不能被实例化");
    }
    
    /**
     * 设置当前线程的服务器名称上下文
     * @param serverName 服务器名称
     */
    public static void setServerNameContext(String serverName) {
        if (serverName != null && !serverName.isEmpty()) {
            serverNameContext.set(serverName);
        } else {
            serverNameContext.set("CONSOLE");
        }
    }
    
    /**
     * 获取当前线程的服务器名称上下文
     * @return 服务器名称
     */
    public static String getServerNameContext() {
        return serverNameContext.get();
    }
    
    /**
     * 清除当前线程的服务器名称上下文
     */
    public static void clearServerNameContext() {
        serverNameContext.remove();
    }
    
    /**
     * 日志级别枚举
     */
    public enum LogLevel {
        DEBUG(0),
        INFO(1),
        WARN(2),
        ERROR(3);
        
        private final int level;
        
        LogLevel(int level) {
            this.level = level;
        }
        
        public int getLevel() {
            return level;
        }
    }
    
    /**
     * 设置日志级别
     * @param level 日志级别
     */
    public static void setLevel(LogLevel level) {
        currentLevel = level;
    }
    
    /**
     * 输出调试信息
     * @param message 消息
     */
    public static void debug(String message) {
        log(LogLevel.DEBUG, message);
    }
    
    /**
     * 输出信息
     * @param message 消息
     */
    public static void info(String message) {
        log(LogLevel.INFO, message);
    }
    
    /**
     * 输出警告信息
     * @param message 消息
     */
    public static void warn(String message) {
        log(LogLevel.WARN, message);
    }
    
    /**
     * 输出错误信息
     * @param message 消息
     */
    public static void error(String message) {
        log(LogLevel.ERROR, message);
    }
    
    /**
     * 输出错误信息和异常堆栈
     * @param message 消息
     * @param throwable 异常
     */
    public static void error(String message, Throwable throwable) {
        log(LogLevel.ERROR, message);
        if (throwable != null) {
            throwable.printStackTrace();
        }
    }
    
    /**
     * 输出日志
     * @param level 日志级别
     * @param message 消息
     */
    private static void log(LogLevel level, String message) {
        if (level.getLevel() >= currentLevel.getLevel()) {
            String timestamp = LocalDateTime.now().format(TIME_FORMATTER);
            String logMessage = String.format("[%s] [%s] %s", timestamp, level.name(), message);
            
            // 使用颜色输出日志
            String coloredLogMessage = ConsoleColor.colorizeLogLevel(level, logMessage);
            System.out.println(coloredLogMessage);
            
            // 通过WebSocket发送到前端 - 使用当前线程的服务器名称上下文
            try {
                String contextServerName = getServerNameContext();
                LogWebSocketHandler.sendLogMessage(contextServerName, logMessage); // 发送原始消息，前端处理颜色
            } catch (Exception e) {
                // 忽略WebSocket发送错误
            }
        }
    }
    
    /**
     * 输出带换行的信息
     * @param message 消息
     */
    public static void println(String message) {
        // 为普通输出也添加颜色支持
        String coloredMessage = ConsoleColor.colorize(ConsoleColor.BRIGHT_WHITE, message);
        System.out.println(coloredMessage);
        System.out.println();
        
        // 通过WebSocket发送到前端 - 使用当前线程的服务器名称上下文
        try {
            String contextServerName = getServerNameContext();
            LogWebSocketHandler.sendLogMessage(contextServerName, message);
        } catch (Exception e) {
            // 忽略WebSocket发送错误
        }
    }
    
    /**
     * 输出不带换行的信息
     * @param message 消息
     */
    public static void print(String message) {
        // 为普通输出也添加颜色支持
        String coloredMessage = ConsoleColor.colorize(ConsoleColor.WHITE, message);
        System.out.print(coloredMessage);
        
        // 通过WebSocket发送到前端 - 使用当前线程的服务器名称上下文
        try {
            String contextServerName = getServerNameContext();
            LogWebSocketHandler.sendLogMessage(contextServerName, message);
        } catch (Exception e) {
            // 忽略WebSocket发送错误
        }
    }
}