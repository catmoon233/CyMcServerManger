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
    
    // 防止实例化
    private Logger() {
        throw new UnsupportedOperationException("这是一个工具类，不能被实例化");
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
            System.out.println(logMessage);
            
            // 通过WebSocket发送到前端
            try {
                LogWebSocketHandler.sendLogMessage(logMessage);
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
        System.out.println(message);
        System.out.println();
        
        // 通过WebSocket发送到前端
        try {
            LogWebSocketHandler.sendLogMessage(message);
        } catch (Exception e) {
            // 忽略WebSocket发送错误
        }
    }
    
    /**
     * 输出不带换行的信息
     * @param message 消息
     */
    public static void print(String message) {
        System.out.print(message);
        
        // 通过WebSocket发送到前端
        try {
            LogWebSocketHandler.sendLogMessage(message);
        } catch (Exception e) {
            // 忽略WebSocket发送错误
        }
    }
}