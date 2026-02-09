package exmo.cy.command.impl;

import exmo.cy.command.AnnotatedCommand;
import exmo.cy.command.CommandAnnotation;
import exmo.cy.exception.ConfigurationException;
import exmo.cy.exception.ServerOperationException;
import exmo.cy.model.Server;
import exmo.cy.service.ServerService;
import exmo.cy.util.Logger;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Pattern;

@CommandAnnotation(
    name = "editconfig",
    aliases = {"ecfg", "setprop"},
    description = "修改服务器配置文件中的属性值"
)
public class EditConfigCommand extends AnnotatedCommand {
    private final ServerService serverService;
    private final Scanner scanner = new Scanner(System.in);
    
    // 常见的配置文件映射
    private static final Map<String, String> COMMON_CONFIG_FILES = new HashMap<>();
    static {
        COMMON_CONFIG_FILES.put("server-port", "server.properties");
        COMMON_CONFIG_FILES.put("server-ip", "server.properties");
        COMMON_CONFIG_FILES.put("max-players", "server.properties");
        COMMON_CONFIG_FILES.put("level-name", "server.properties");
        COMMON_CONFIG_FILES.put("online-mode", "server.properties");
        COMMON_CONFIG_FILES.put("enable-rcon", "server.properties");
        COMMON_CONFIG_FILES.put("rcon.port", "server.properties");
        COMMON_CONFIG_FILES.put("rcon.password", "server.properties");
        COMMON_CONFIG_FILES.put("query.port", "server.properties");
        COMMON_CONFIG_FILES.put("enable-query", "server.properties");
        COMMON_CONFIG_FILES.put("gamemode", "server.properties");
        COMMON_CONFIG_FILES.put("difficulty", "server.properties");
        COMMON_CONFIG_FILES.put("motd", "server.properties");
    }
    
    public EditConfigCommand(ServerService serverService) {
        this.serverService = serverService;
    }
    
    @Override
    public boolean execute(String[] args) {
        if (args.length == 0) {
            showUsage();
            return true;
        }
        
        String action = args[0].toLowerCase();
        
        switch (action) {
            case "set":
                return setProperty(args);
            case "get":
                return getProperty(args);
            case "list-files":
                return listConfigFiles(args);
            case "quick":
                return quickEdit(args);
            default:
                Logger.println("未知的操作: " + action);
                showUsage();
                return true;
        }
    }
    
    private void showUsage() {
        Logger.println("配置文件编辑命令用法:");
        Logger.println("  editconfig set <服务器名称> <文件名> <属性名> <新值>     - 修改指定配置文件中的属性");
        Logger.println("  editconfig set <服务器名称> <属性名> <新值>           - 自动检测配置文件（仅支持常见属性）");
        Logger.println("  editconfig get <服务器名称> <文件名> <属性名>         - 查看指定配置文件中的属性值");
        Logger.println("  editconfig get <服务器名称> <属性名>                 - 查看常见属性值");
        Logger.println("  editconfig list-files <服务器名称>                   - 列出服务器目录中的配置文件");
        Logger.println("  editconfig quick <服务器名称>                        - 快速编辑常用配置");
        Logger.println("");
        Logger.println("常用属性快速参考:");
        Logger.println("  server-port, server-ip, max-players, level-name, online-mode");
        Logger.println("  enable-rcon, rcon.port, rcon.password, gamemode, difficulty, motd");
        Logger.println("");
        Logger.println("示例:");
        Logger.println("  editconfig set myserver server.properties server-port 25566");
        Logger.println("  editconfig set myserver server-port 25566");
        Logger.println("  editconfig get myserver server-port");
        Logger.println("  editconfig quick myserver");
    }
    
    private boolean setProperty(String[] args) {
        if (args.length < 4) {
            Logger.println("错误: 参数不足");
            showUsage();
            return true;
        }
        
        String serverName = args[1];
        String key;
        String value;
        String fileName;
        
        // 检查是否提供了文件名
        if (args.length >= 5 && !COMMON_CONFIG_FILES.containsKey(args[2])) {
            // 格式: set <server> <file> <key> <value>
            fileName = args[2];
            key = args[3];
            value = args[4];
        } else {
            // 格式: set <server> <key> <value> 或 set <server> <file> <key> <value>（如果file是常见属性）
            fileName = COMMON_CONFIG_FILES.getOrDefault(args[2], "server.properties");
            key = args[2];
            value = args[3];
        }
        
        try {
            // 验证服务器存在
            Optional<Server> serverOpt = serverService.getConfigManager().findServerByName(serverName);
            if (!serverOpt.isPresent()) {
                Logger.println("错误: 服务器 " + serverName + " 不存在");
                return true;
            }
            
            Server server = serverOpt.get();
            Path serverDir = Paths.get(server.getCorePath()).getParent();
            Path configFile = serverDir.resolve(fileName);
            
            // 检查配置文件是否存在
            if (!Files.exists(configFile)) {
                Logger.println("警告: 配置文件 " + fileName + " 不存在，将创建新文件");
                Files.createDirectories(configFile.getParent());
                Files.createFile(configFile);
            }
            
            // 读取并修改配置文件
            List<String> lines = Files.readAllLines(configFile);
            boolean found = false;
            List<String> newLines = new ArrayList<>();
            
            for (String line : lines) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) {
                    newLines.add(line);
                    continue;
                }
                
                // 使用正则表达式匹配键值对
                String[] parts = line.split("=", 2);
                if (parts.length == 2) {
                    String currentKey = parts[0].trim();
                    if (currentKey.equals(key)) {
                        newLines.add(key + "=" + value);
                        found = true;
                    } else {
                        newLines.add(line);
                    }
                } else {
                    newLines.add(line);
                }
            }
            
            // 如果没有找到键，添加新键值对
            if (!found) {
                newLines.add(key + "=" + value);
            }
            
            // 写回文件
            Files.write(configFile, newLines);
            Logger.println("✓ 已成功修改 " + serverName + " 的 " + fileName + " 中的 " + key + " = " + value);
            
        } catch (Exception e) {
            Logger.error("修改配置文件失败: " + e.getMessage(), e);
            return true;
        }
        
        return true;
    }
    
    private boolean getProperty(String[] args) {
        if (args.length < 3) {
            Logger.println("错误: 参数不足");
            showUsage();
            return true;
        }
        
        String serverName = args[1];
        String key;
        String fileName;
        
        // 检查是否提供了文件名
        if (args.length >= 4 && !COMMON_CONFIG_FILES.containsKey(args[2])) {
            fileName = args[2];
            key = args[3];
        } else {
            fileName = COMMON_CONFIG_FILES.getOrDefault(args[2], "server.properties");
            key = args[2];
        }
        
        try {
            // 验证服务器存在
            Optional<Server> serverOpt = serverService.getConfigManager().findServerByName(serverName);
            if (!serverOpt.isPresent()) {
                Logger.println("错误: 服务器 " + serverName + " 不存在");
                return true;
            }
            
            Server server = serverOpt.get();
            Path serverDir = Paths.get(server.getCorePath()).getParent();
            Path configFile = serverDir.resolve(fileName);
            
            if (!Files.exists(configFile)) {
                Logger.println("错误: 配置文件 " + fileName + " 不存在");
                return true;
            }
            
            // 读取配置文件
            List<String> lines = Files.readAllLines(configFile);
            for (String line : lines) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }
                
                String[] parts = line.split("=", 2);
                if (parts.length == 2) {
                    String currentKey = parts[0].trim();
                    if (currentKey.equals(key)) {
                        Logger.println(key + " = " + parts[1].trim());
                        return true;
                    }
                }
            }
            
            Logger.println("未找到属性: " + key);
            
        } catch (Exception e) {
            Logger.error("读取配置文件失败: " + e.getMessage(), e);
            return true;
        }
        
        return true;
    }
    
    private boolean listConfigFiles(String[] args) {
        if (args.length < 2) {
            Logger.println("错误: 请指定服务器名称");
            return true;
        }
        
        String serverName = args[1];
        
        try {
            Optional<Server> serverOpt = serverService.getConfigManager().findServerByName(serverName);
            if (!serverOpt.isPresent()) {
                Logger.println("错误: 服务器 " + serverName + " 不存在");
                return true;
            }
            
            Server server = serverOpt.get();
            Path serverDir = Paths.get(server.getCorePath()).getParent();
            
            if (!Files.exists(serverDir)) {
                Logger.println("错误: 服务器目录不存在");
                return true;
            }
            
            Logger.println("服务器 " + serverName + " 的配置文件:");
            Files.list(serverDir)
                .filter(path -> path.toString().endsWith(".properties") || path.toString().endsWith(".txt") || path.toString().endsWith(".cfg"))
                .forEach(path -> Logger.println("  - " + serverDir.relativize(path)));
                
        } catch (Exception e) {
            Logger.error("列出配置文件失败: " + e.getMessage(), e);
            return true;
        }
        
        return true;
    }
    
    private boolean quickEdit(String[] args) {
        if (args.length < 2) {
            Logger.println("错误: 请指定服务器名称");
            return true;
        }
        
        String serverName = args[1];
        
        try {
            Optional<Server> serverOpt = serverService.getConfigManager().findServerByName(serverName);
            if (!serverOpt.isPresent()) {
                Logger.println("错误: 服务器 " + serverName + " 不存在");
                return true;
            }
            
            Server server = serverOpt.get();
            Path serverDir = Paths.get(server.getCorePath()).getParent();
            Path serverProperties = serverDir.resolve("server.properties");
            
            if (!Files.exists(serverProperties)) {
                Logger.println("警告: server.properties 不存在，将创建默认配置");
                Files.createDirectories(serverDir);
                createDefaultServerProperties(serverProperties);
            }
            
            // 读取当前配置
            Map<String, String> currentProps = readPropertiesFile(serverProperties);
            
            Logger.println("快速编辑 " + serverName + " 的常用配置:");
            Logger.println("当前配置:");
            
            // 显示常用配置的当前值
            String[] commonKeys = {"server-port", "server-ip", "max-players", "level-name", "online-mode", "gamemode", "difficulty", "motd"};
            for (String key : commonKeys) {
                String currentValue = currentProps.getOrDefault(key, "未设置");
                Logger.println("  " + key + " = " + currentValue);
            }
            
            Logger.println("");
            Logger.println("请输入要修改的属性名 (输入 'done' 完成):");
            
            while (true) {
                Logger.print("属性名: ");
                String input = scanner.nextLine().trim();
                
                if ("done".equalsIgnoreCase(input)) {
                    break;
                }
                
                if (input.isEmpty()) {
                    continue;
                }
                
                // 检查属性是否有效
                if (!Arrays.asList(commonKeys).contains(input)) {
                    Logger.println("警告: " + input + " 不是常用属性，但仍可修改");
                }
                
                Logger.print("新值: ");
                String newValue = scanner.nextLine().trim();
                
                if (newValue.isEmpty()) {
                    Logger.println("跳过空值");
                    continue;
                }
                
                // 修改配置文件
                List<String> lines = Files.readAllLines(serverProperties);
                boolean found = false;
                List<String> newLines = new ArrayList<>();
                
                for (String line : lines) {
                    line = line.trim();
                    if (line.isEmpty() || line.startsWith("#")) {
                        newLines.add(line);
                        continue;
                    }
                    
                    String[] parts = line.split("=", 2);
                    if (parts.length == 2) {
                        String currentKey = parts[0].trim();
                        if (currentKey.equals(input)) {
                            newLines.add(input + "=" + newValue);
                            found = true;
                        } else {
                            newLines.add(line);
                        }
                    } else {
                        newLines.add(line);
                    }
                }
                
                if (!found) {
                    newLines.add(input + "=" + newValue);
                }
                
                Files.write(serverProperties, newLines);
                Logger.println("✓ 已修改 " + input + " = " + newValue);
            }
            
            Logger.println("快速编辑完成!");
            
        } catch (Exception e) {
            Logger.error("快速编辑失败: " + e.getMessage(), e);
            return true;
        }
        
        return true;
    }
    
    private Map<String, String> readPropertiesFile(Path file) throws IOException {
        Map<String, String> props = new HashMap<>();
        if (!Files.exists(file)) {
            return props;
        }
        
        List<String> lines = Files.readAllLines(file);
        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }
            
            String[] parts = line.split("=", 2);
            if (parts.length == 2) {
                props.put(parts[0].trim(), parts[1].trim());
            }
        }
        
        return props;
    }
    
    private void createDefaultServerProperties(Path file) throws IOException {
        List<String> defaultProps = Arrays.asList(
            "#Minecraft server properties",
            "#Sun Feb 09 23:37:03 CST 2026",
            "server-port=25565",
            "server-ip=",
            "max-players=20",
            "level-name=world",
            "online-mode=true",
            "enable-rcon=false",
            "rcon.port=25575",
            "rcon.password=",
            "query.port=25565",
            "enable-query=false",
            "gamemode=survival",
            "difficulty=easy",
            "motd=A Minecraft Server"
        );
        
        Files.write(file, defaultProps);
    }
    
    @Override
    public String getDescription() {
        return "修改服务器配置文件中的属性值";
    }
    
    @Override
    public CommandAnnotation getAnnotation() {
        return getClass().getAnnotation(CommandAnnotation.class);
    }
}