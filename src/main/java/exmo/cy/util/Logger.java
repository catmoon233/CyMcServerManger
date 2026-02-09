package exmo.cy.util;

import exmo.cy.web.LogWebSocketHandler;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 简单的日志工具类
 * 提供统一的日志输出格式和级别控制
 */
public final class Logger {

    // 禁用Java util logging输出
    private static final java.util.logging.Logger LOGGER = java.util.logging.Logger.getLogger(Logger.class.getName());
    static {
        java.util.logging.Logger rootLogger = java.util.logging.Logger.getLogger("");
        rootLogger.setLevel(java.util.logging.Level.OFF);
        LOGGER.setLevel(java.util.logging.Level.OFF);
    }
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter FILE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");
    private static LogLevel currentLevel = LogLevel.INFO;
    
    // 线程本地存储，用于追踪当前线程的服务器名称上下文
    private static final ThreadLocal<String> serverNameContext = ThreadLocal.withInitial(() -> "CONSOLE");
    
    // 行为日志相关
    private static final String BEHAVIOR_LOG_DIR = "behavior_logs";
    private static final String BEHAVIOR_LOG_FILE = "behavior_" + LocalDateTime.now().format(FILE_TIME_FORMATTER) + ".log";
    private static final Path BEHAVIOR_LOG_PATH = Paths.get(BEHAVIOR_LOG_DIR, BEHAVIOR_LOG_FILE);
    private static PrintWriter behaviorLogWriter = null;
    private static final Object logLock = new Object();
    
    // 静态初始化行为日志
    static {
        initializeBehaviorLogging();
    }
    
    // 防止实例化
    private Logger() {
        throw new UnsupportedOperationException("这是一个工具类，不能被实例化");
    }
    
    /**
     * 初始化行为日志系统
     */
    private static void initializeBehaviorLogging() {
        try {
            // 创建日志目录
            Path logDir = Paths.get(BEHAVIOR_LOG_DIR);
            if (!Files.exists(logDir)) {
                Files.createDirectories(logDir);
            }
            
            // 创建日志文件写入器
            FileWriter fileWriter = new FileWriter(BEHAVIOR_LOG_PATH.toFile(), true);
            behaviorLogWriter = new PrintWriter(fileWriter);
            behaviorLogWriter.println("\n=== 行为日志开始 - " + LocalDateTime.now().format(TIME_FORMATTER) + " ===");

            behaviorLogWriter.flush();
        } catch (IOException e) {
            System.err.println("无法初始化行为日志系统: " + e.getMessage());
        }
    }
    
    /**
     * 记录行为日志（仅记录主控制台行为，不记录子服日志）
     * @param message 行为描述
     */
    public static void logBehavior(String message) {
        synchronized (logLock) {
            if (behaviorLogWriter != null) {
                String timestamp = LocalDateTime.now().format(TIME_FORMATTER);
                String logMessage = String.format("[%s] [BEHAVIOR] %s", timestamp, message);
                LOGGER.log(java.util.logging.Level.INFO, logMessage);
                behaviorLogWriter.flush(); // 确保立即写入
            }
        }
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
            
            // 通过WebSocket发送到前端 - 使用当前线程的服务器名称上下文
            try {
                String contextServerName = getServerNameContext();
                LogWebSocketHandler.sendLogMessage(contextServerName, logMessage);
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
        // 直接输出消息，不添加时间戳和标签
        System.out.println(message);
        
        // 只为主控制台（非子服）记录行为日志
        if ("CONSOLE".equals(getServerNameContext())) {
            logBehavior("输出信息: " + message);
        }
        
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
        LOGGER.log(java.util.logging.Level.INFO, coloredMessage);

        // 只为主控制台（非子服）记录行为日志
        if ("CONSOLE".equals(getServerNameContext())) {
            logBehavior("输出信息: " + message);
        }
        // 通过WebSocket发送到前端 - 使用当前线程的服务器名称上下文
        try {
            String contextServerName = getServerNameContext();
            LogWebSocketHandler.sendLogMessage(contextServerName, message);
        } catch (Exception e) {
            // 忽略WebSocket发送错误
        }
    }
    
    /**
     * 关闭行为日志系统
     */
    public static void closeBehaviorLog() {
        synchronized (logLock) {
            if (behaviorLogWriter != null) {
                behaviorLogWriter.println("=== 行为日志结束 - " + LocalDateTime.now().format(TIME_FORMATTER) + " ===");
                behaviorLogWriter.close();
                behaviorLogWriter = null;
            }
        }
    }
}