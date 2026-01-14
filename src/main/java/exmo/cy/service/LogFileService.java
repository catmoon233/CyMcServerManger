package exmo.cy.service;

import exmo.cy.exception.ConfigurationException;
import exmo.cy.util.Logger;

import java.io.*;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 日志文件管理服务
 * 负责日志文件的创建、读取、管理和导出
 */
public class LogFileService {
    
    private static final String LOG_DIR = "logs";
    private static final String SYSTEM_LOG_FILE = "system.log";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    
    private final Path logDirectory;
    private final Path systemLogFile;
    
    /**
     * 构造函数
     */
    public LogFileService() {
        this.logDirectory = Paths.get(LOG_DIR);
        this.systemLogFile = logDirectory.resolve(SYSTEM_LOG_FILE);
        initializeLogDirectory();
    }
    
    /**
     * 初始化日志目录
     */
    private void initializeLogDirectory() {
        try {
            if (!Files.exists(logDirectory)) {
                Files.createDirectories(logDirectory);
                Logger.info("创建日志目录: " + logDirectory.toAbsolutePath());
            }
        } catch (IOException e) {
            Logger.error("创建日志目录失败", e);
        }
    }
    
    /**
     * 写入系统日志
     * @param level 日志级别
     * @param message 日志消息
     */
    public void writeSystemLog(String level, String message) {
        try {
            String timestamp = LocalDateTime.now().format(TIME_FORMATTER);
            String logEntry = String.format("[%s] [%s] %s%n", timestamp, level, message);
            
            // 追加写入日志文件
            Files.write(systemLogFile, logEntry.getBytes(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            // 静默失败，避免日志记录导致的问题
            System.err.println("写入日志文件失败: " + e.getMessage());
        }
    }
    
    /**
     * 写入服务器日志
     * @param serverName 服务器名称
     * @param level 日志级别
     * @param message 日志消息
     */
    public void writeServerLog(String serverName, String level, String message) {
        if (serverName == null || serverName.isEmpty()) {
            writeSystemLog(level, message);
            return;
        }
        
        try {
            Path serverLogFile = logDirectory.resolve(serverName + ".log");
            String timestamp = LocalDateTime.now().format(TIME_FORMATTER);
            String logEntry = String.format("[%s] [%s] %s%n", timestamp, level, message);
            
            // 追加写入日志文件
            Files.write(serverLogFile, logEntry.getBytes(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            // 静默失败
            System.err.println("写入服务器日志文件失败: " + e.getMessage());
        }
    }
    
    /**
     * 读取系统日志
     * @param lines 读取的行数（从末尾开始），0表示读取全部
     * @return 日志行列表
     */
    public List<String> readSystemLog(int lines) {
        return readLogFile(systemLogFile, lines);
    }
    
    /**
     * 读取服务器日志
     * @param serverName 服务器名称
     * @param lines 读取的行数（从末尾开始），0表示读取全部
     * @return 日志行列表
     */
    public List<String> readServerLog(String serverName, int lines) {
        if (serverName == null || serverName.isEmpty()) {
            return new ArrayList<>();
        }
        
        Path serverLogFile = logDirectory.resolve(serverName + ".log");
        return readLogFile(serverLogFile, lines);
    }
    
    /**
     * 读取日志文件
     * @param logFile 日志文件路径
     * @param lines 读取的行数（从末尾开始），0表示读取全部
     * @return 日志行列表
     */
    private List<String> readLogFile(Path logFile, int lines) {
        if (!Files.exists(logFile)) {
            return new ArrayList<>();
        }
        
        try {
            List<String> allLines = Files.readAllLines(logFile);
            
            if (lines <= 0 || lines >= allLines.size()) {
                return allLines;
            }
            
            // 返回最后N行
            return allLines.subList(allLines.size() - lines, allLines.size());
        } catch (IOException e) {
            Logger.error("读取日志文件失败: " + logFile, e);
            return new ArrayList<>();
        }
    }
    
    /**
     * 获取系统日志文件大小
     * @return 文件大小（字节）
     */
    public long getSystemLogSize() {
        try {
            return Files.exists(systemLogFile) ? Files.size(systemLogFile) : 0;
        } catch (IOException e) {
            return 0;
        }
    }
    
    /**
     * 获取服务器日志文件大小
     * @param serverName 服务器名称
     * @return 文件大小（字节）
     */
    public long getServerLogSize(String serverName) {
        if (serverName == null || serverName.isEmpty()) {
            return 0;
        }
        
        try {
            Path serverLogFile = logDirectory.resolve(serverName + ".log");
            return Files.exists(serverLogFile) ? Files.size(serverLogFile) : 0;
        } catch (IOException e) {
            return 0;
        }
    }
    
    /**
     * 清空系统日志
     */
    public void clearSystemLog() {
        try {
            if (Files.exists(systemLogFile)) {
                Files.delete(systemLogFile);
            }
            Logger.info("系统日志已清空");
        } catch (IOException e) {
            Logger.error("清空系统日志失败", e);
        }
    }
    
    /**
     * 清空服务器日志
     * @param serverName 服务器名称
     */
    public void clearServerLog(String serverName) {
        if (serverName == null || serverName.isEmpty()) {
            return;
        }
        
        try {
            Path serverLogFile = logDirectory.resolve(serverName + ".log");
            if (Files.exists(serverLogFile)) {
                Files.delete(serverLogFile);
            }
            Logger.info("服务器日志已清空: " + serverName);
        } catch (IOException e) {
            Logger.error("清空服务器日志失败: " + serverName, e);
        }
    }
    
    /**
     * 导出系统日志
     * @param outputPath 输出路径
     * @return 导出是否成功
     */
    public boolean exportSystemLog(Path outputPath) {
        return exportLogFile(systemLogFile, outputPath);
    }
    
    /**
     * 导出服务器日志
     * @param serverName 服务器名称
     * @param outputPath 输出路径
     * @return 导出是否成功
     */
    public boolean exportServerLog(String serverName, Path outputPath) {
        if (serverName == null || serverName.isEmpty()) {
            return false;
        }
        
        Path serverLogFile = logDirectory.resolve(serverName + ".log");
        return exportLogFile(serverLogFile, outputPath);
    }
    
    /**
     * 导出日志文件
     * @param sourceFile 源文件
     * @param outputPath 输出路径
     * @return 导出是否成功
     */
    private boolean exportLogFile(Path sourceFile, Path outputPath) {
        if (!Files.exists(sourceFile)) {
            return false;
        }
        
        try {
            Files.copy(sourceFile, outputPath, StandardCopyOption.REPLACE_EXISTING);
            return true;
        } catch (IOException e) {
            Logger.error("导出日志文件失败", e);
            return false;
        }
    }
    
    /**
     * 获取所有日志文件列表
     * @return 日志文件信息列表
     */
    public List<Map<String, Object>> listLogFiles() {
        List<Map<String, Object>> logFiles = new ArrayList<>();
        
        try {
            if (!Files.exists(logDirectory)) {
                return logFiles;
            }
            
            try (Stream<Path> paths = Files.list(logDirectory)) {
                paths.filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".log"))
                    .forEach(path -> {
                        try {
                            Map<String, Object> fileInfo = new HashMap<>();
                            fileInfo.put("name", path.getFileName().toString());
                            fileInfo.put("size", Files.size(path));
                            fileInfo.put("modified", Files.getLastModifiedTime(path).toMillis());
                            logFiles.add(fileInfo);
                        } catch (IOException e) {
                            // 忽略单个文件的错误
                        }
                    });
            }
        } catch (IOException e) {
            Logger.error("列出日志文件失败", e);
        }
        
        // 按修改时间排序（最新的在前）
        logFiles.sort((a, b) -> Long.compare(
            (Long) b.get("modified"),
            (Long) a.get("modified")
        ));
        
        return logFiles;
    }
    
    /**
     * 清理旧日志文件
     * @param daysToKeep 保留天数
     * @return 清理的文件数量
     */
    public int cleanOldLogs(int daysToKeep) {
        int cleanedCount = 0;
        long cutoffTime = System.currentTimeMillis() - (daysToKeep * 24L * 60 * 60 * 1000);
        
        try {
            if (!Files.exists(logDirectory)) {
                return 0;
            }
            
            try (Stream<Path> paths = Files.list(logDirectory)) {
                List<Path> filesToDelete = paths
                    .filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".log"))
                    .filter(path -> {
                        try {
                            return Files.getLastModifiedTime(path).toMillis() < cutoffTime;
                        } catch (IOException e) {
                            return false;
                        }
                    })
                    .collect(Collectors.toList());
                
                for (Path file : filesToDelete) {
                    try {
                        Files.delete(file);
                        cleanedCount++;
                    } catch (IOException e) {
                        Logger.warn("删除旧日志文件失败: " + file);
                    }
                }
            }
        } catch (IOException e) {
            Logger.error("清理旧日志文件失败", e);
        }
        
        if (cleanedCount > 0) {
            Logger.info("清理了 " + cleanedCount + " 个旧日志文件");
        }
        
        return cleanedCount;
    }
}
