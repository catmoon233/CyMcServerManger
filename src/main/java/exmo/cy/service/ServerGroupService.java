package exmo.cy.service;

import exmo.cy.config.ThreadConfig;
import exmo.cy.model.Server;
import exmo.cy.model.ServerGroup;
import exmo.cy.util.Logger;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;


import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.RejectedExecutionHandler;

/**
 * 服务器群组服务
 * 管理服务器群组的创建、配置和启动
 */
@Service
public class ServerGroupService {
    
    @Autowired
    private ServerService serverService;
    
    private final Map<String, ServerGroup> groups = new ConcurrentHashMap<>();
    private final Map<String, Queue<String>> orderedStartupQueues = new ConcurrentHashMap<>(); // 群组启动队列
    private final Map<String, Boolean> groupStartupStatus = new ConcurrentHashMap<>(); // 群组启动状态
    private final ExecutorService executorService = createManagedThreadPool();
    
    private static final String GROUPS_CONFIG_FILE = "server_groups.json";
    
    @PostConstruct
    public void init() {
        loadGroups();
        Logger.info("服务器群组服务初始化完成，线程池配置: " + getThreadPoolInfo());
    }
    
    /**
     * 创建受管的线程池
     */
    private ExecutorService createManagedThreadPool() {
        // 使用线程配置类获取推荐大小
        int poolSize = ThreadConfig.getRecommendedThreadPoolSize(1.0);
        
        ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(
            poolSize,
            ThreadConfig.createServiceThreadFactory("ServerGroup")
        );
        
        // 设置合理的拒绝策略
        executor.setRejectedExecutionHandler(new RejectedExecutionHandler() {
            @Override
            public void rejectedExecution(Runnable r, ThreadPoolExecutor exec) {
                Logger.warn("服务器群组线程池任务被拒绝，当前活跃线程数: " + 
                           exec.getActiveCount() + "/" + exec.getMaximumPoolSize() + 
                           ", 队列大小: " + exec.getQueue().size());
                // 检查线程资源状况
                if (!ThreadConfig.isThreadResourceSufficient()) {
                    Logger.error("系统线程资源不足，可能导致线程创建失败");
                    ThreadConfig.printDetailedThreadInfo();
                }
                // 在当前线程中执行被拒绝的任务
                try {
                    r.run();
                } catch (Exception e) {
                    Logger.error("执行被拒绝的任务时出错: " + e.getMessage(), e);
                }
            }
        });
        
        return executor;
    }
    
    /**
     * 获取线程池信息
     */
    private String getThreadPoolInfo() {
        if (executorService instanceof ThreadPoolExecutor) {
            ThreadPoolExecutor tpe = (ThreadPoolExecutor) executorService;
            return String.format("核心线程数:%d, 最大线程数:%d, 活跃线程数:%d, 队列大小:%d", 
                tpe.getCorePoolSize(), tpe.getMaximumPoolSize(), 
                tpe.getActiveCount(), tpe.getQueue().size());
        }
        return "未知线程池类型";
    }
    
    /**
     * 设置ServerService（用于非Spring环境）
     */
    public void setServerService(ServerService serverService) {
        this.serverService = serverService;
    }
    
    /**
     * 创建服务器群组
     */
    public boolean createGroup(String groupName) {
        return createGroup(groupName, 1, null, null); // 默认使用CORE模式，无预设参数
    }
    
    /**
     * 创建服务器群组
     */
    public boolean createGroup(String groupName, int launchMode, String presetJvmArgs, String presetServerArgs) {
        return createGroup(groupName, launchMode, presetJvmArgs, presetServerArgs, null, null);
    }
    
    /**
     * 创建服务器群组
     */
    public boolean createGroup(String groupName, int launchMode, String presetJvmArgs, String presetServerArgs, String minMemory, String maxMemory) {
        if (groups.containsKey(groupName)) {
            Logger.warn("群组已存在: " + groupName);
            return false;
        }
        
        ServerGroup group = new ServerGroup(groupName, launchMode, presetJvmArgs, presetServerArgs, minMemory, maxMemory);
        groups.put(groupName, group);
        saveGroups();
        Logger.info("创建服务器群组: " + groupName + " (启动模式: " + launchMode + ")");
        return true;
    }
    
    /**
     * 删除服务器群组
     */
    public boolean deleteGroup(String groupName) {
        if (!groups.containsKey(groupName)) {
            Logger.warn("群组不存在: " + groupName);
            return false;
        }
        
        groups.remove(groupName);
        orderedStartupQueues.remove(groupName); // 移除队列
        groupStartupStatus.remove(groupName); // 移除状态
        saveGroups();
        Logger.info("删除服务器群组: " + groupName);
        return true;
    }
    
    /**
     * 添加服务器到群组
     */
    public boolean addServerToGroup(String groupName, String serverName) {
        ServerGroup group = groups.get(groupName);
        if (group == null) {
            Logger.warn("群组不存在: " + groupName);
            return false;
        }
        
        // 检查服务器是否存在
        try {
            Optional<Server> serverOpt = serverService.getConfigManager().findServerByName(serverName);
            if (serverOpt.isEmpty()) {
                Logger.warn("服务器不存在: " + serverName);
                return false;
            }
            
            // 更新服务器的群组信息
            Server server = serverOpt.get();
            server.setGroup(groupName);
            serverService.getConfigManager().saveServer(server);
            
            group.addServer(serverName);
            saveGroups();
            Logger.info("服务器 " + serverName + " 添加到群组 " + groupName);
            return true;
        } catch (Exception e) {
            Logger.error("添加服务器到群组失败: " + e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * 从群组中移除服务器
     */
    public boolean removeServerFromGroup(String groupName, String serverName) {
        ServerGroup group = groups.get(groupName);
        if (group == null) {
            Logger.warn("群组不存在: " + groupName);
            return false;
        }
        
        // 更新服务器的群组信息
        try {
            Optional<Server> serverOpt = serverService.getConfigManager().findServerByName(serverName);
            if (serverOpt.isPresent()) {
                Server server = serverOpt.get();
                server.setGroup(null);
                serverService.getConfigManager().saveServer(server);
            }
        } catch (Exception e) {
            Logger.error("更新服务器群组信息失败: " + e.getMessage(), e);
        }
        
        boolean removed = group.removeServer(serverName);
        if (removed) {
            saveGroups();
            Logger.info("服务器 " + serverName + " 从群组 " + groupName + " 中移除");
        }
        return removed;
    }
    
    /**
     * 启动群组中的所有服务器（同时启动）
     */
    public void startGroupConcurrently(String groupName) {
        ServerGroup group = groups.get(groupName);
        if (group == null) {
            Logger.warn("群组不存在: " + groupName);
            return;
        }
        
        Logger.info("开始同时启动群组 " + groupName + " 中的所有服务器");
        groupStartupStatus.put(groupName, true);
        
        List<CompletableFuture<Void>> futures = new ArrayList<>();
        
        for (String serverName : group.getServerNames()) {
            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                try {
                    Optional<Server> serverOpt = serverService.getConfigManager().findServerByName(serverName);
                    if (serverOpt.isPresent()) {
                        Server server = serverOpt.get();
                        // 使用群组配置的启动模式和预设参数，包括内存参数
                        String jvmArgs = buildJvmArgs(group, server);
                        serverService.startServer(server, group.getLaunchMode(), null,
                            jvmArgs,
                            group.getPresetServerArgs() != null ? group.getPresetServerArgs() : server.getDefaultServerArgs());
                        Logger.info("服务器 " + serverName + " 启动成功");
                    } else {
                        Logger.warn("服务器不存在: " + serverName);
                    }
                } catch (Exception e) {
                    Logger.error("启动服务器失败 " + serverName + ": " + e.getMessage(), e);
                }
            }, executorService);
            
            futures.add(future);
        }
        
        // 等待所有服务器启动完成
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        Logger.info("群组 " + groupName + " 中的所有服务器启动完成");
    }
    
    /**
     * 启动群组中的服务器（按顺序启动）
     */
    public void startGroupOrdered(String groupName) {
        ServerGroup group = groups.get(groupName);
        if (group == null) {
            Logger.warn("群组不存在: " + groupName);
            return;
        }
        
        Logger.info("开始按顺序启动群组 " + groupName + " 中的服务器");
        groupStartupStatus.put(groupName, true);
        
        // 创建启动队列
        Queue<String> startupQueue = new LinkedList<>(group.getServerNames());
        orderedStartupQueues.put(groupName, startupQueue);
        
        // 启动第一个服务器
        startNextServerInQueue(groupName);
    }
    
    /**
     * 启动队列中的下一个服务器
     */
    public void startNextServerInQueue(String groupName) {
        ServerGroup group = groups.get(groupName); // 重新获取group对象
        if (group == null) {
            Logger.warn("群组不存在: " + groupName);
            orderedStartupQueues.remove(groupName);
            return;
        }
        
        Queue<String> queue = orderedStartupQueues.get(groupName);
        if (queue == null || queue.isEmpty()) {
            Logger.info("群组 " + groupName + " 的启动队列已完成");
            orderedStartupQueues.remove(groupName);
            groupStartupStatus.put(groupName, false);
            return;
        }
        
        String serverName = queue.poll();
        if (serverName != null) {
            try {
                Optional<Server> serverOpt = serverService.getConfigManager().findServerByName(serverName);
                if (serverOpt.isPresent()) {
                    Server server = serverOpt.get();
                    // 使用群组配置的启动模式和预设参数，包括内存参数
                    String jvmArgs = buildJvmArgs(group, server);
                    serverService.startServer(server, group.getLaunchMode(), null,
                        jvmArgs,
                        group.getPresetServerArgs() != null ? group.getPresetServerArgs() : server.getDefaultServerArgs());
                    Logger.info("服务器 " + serverName + " 已启动，等待启动完成信号...");
                    
                    // 在单独的线程中监控启动完成信号
                    monitorStartupCompletion(groupName, serverName, group.getTriggerKeyword());
                } else {
                    Logger.warn("服务器不存在: " + serverName + "，跳过启动");
                    // 继续启动下一个服务器
                    startNextServerInQueue(groupName);
                }
            } catch (Exception e) {
                Logger.error("启动服务器失败 " + serverName + ": " + e.getMessage(), e);
                // 继续启动下一个服务器
                startNextServerInQueue(groupName);
            }
        }
    }
    
    /**
     * 监控服务器启动完成情况
     */
    private void monitorStartupCompletion(String groupName, String serverName, String triggerKeyword) {
        // 这里我们模拟监控，实际上需要监听服务器控制台输出
        // 在真实情况下，我们需要监听服务器输出以检测启动完成信号
        executorService.submit(() -> {
            try {
                // 模拟等待一段时间，实际应用中应监听服务器输出
                Thread.sleep(10000); // 等待10秒
                
                Logger.info("假定服务器 " + serverName + " 启动完成，启动下一个服务器");
                
                // 启动队列中的下一个服务器
                startNextServerInQueue(groupName);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                Logger.error("监控启动完成被中断", e);
            }
        });
    }
    
    /**
     * 手动启动队列中的下一个服务器
     */
    public void manualStartNextServer(String groupName) {
        startNextServerInQueue(groupName);
    }
    
    /**
     * 获取群组信息
     */
    public ServerGroup getGroup(String groupName) {
        // 如果群组不存在且当前群组集合为空，尝试加载配置
        if (!groups.containsKey(groupName) && groups.isEmpty()) {
            loadGroups();
        }
        return groups.get(groupName);
    }
    
    /**
     * 获取所有群组名称
     */
    public Set<String> getGroupNames() {
        // 在返回群组名称之前，确保群组已经加载
        if (groups.isEmpty()) {
            loadGroups();
        }
        return new HashSet<>(groups.keySet());
    }
    
    /**
     * 检查群组是否存在
     */
    public boolean groupExists(String groupName) {
        return groups.containsKey(groupName);
    }
    
    /**
     * 保存群组配置到文件
     */
    public void saveGroups() {
        try (PrintWriter writer = new PrintWriter(GROUPS_CONFIG_FILE)) {
            writer.println("[");
            boolean first = true;
            for (ServerGroup group : groups.values()) {
                if (!first) {
                    writer.println(",");
                }
                writer.print("  {\n");
                writer.print("    \"name\":\"" + group.getName() + "\",\n");
                writer.print("    \"orderedStartup\":" + group.isOrderedStartup() + ",\n");
                writer.print("    \"triggerKeyword\":\"" + group.getTriggerKeyword() + "\",\n");
                writer.print("    \"startupDelay\":" + group.getStartupDelay() + ",\n");
                writer.print("    \"launchMode\":" + group.getLaunchMode() + ",\n");
                writer.print("    \"presetJvmArgs\":\"" + (group.getPresetJvmArgs() != null ? group.getPresetJvmArgs() : "") + "\",\n");
                writer.print("    \"presetServerArgs\":\"" + (group.getPresetServerArgs() != null ? group.getPresetServerArgs() : "") + "\",\n");
                writer.print("    \"minMemory\":\"" + (group.getMinMemory() != null ? group.getMinMemory() : "") + "\",\n");
                writer.print("    \"maxMemory\":\"" + (group.getMaxMemory() != null ? group.getMaxMemory() : "") + "\",\n");
                writer.print("    \"serverNames\":[");
                boolean serverFirst = true;
                for (String serverName : group.getServerNames()) {
                    if (!serverFirst) {
                        writer.print(",");
                    }
                    writer.print("\"" + serverName + "\"");
                    serverFirst = false;
                }
                writer.print("]");
                writer.print("}");
                first = false;
            }
            writer.println();
            writer.println("]");
        } catch (FileNotFoundException e) {
            Logger.error("保存群组配置失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 从文件加载群组配置
     */
    public void loadGroups() {
        Path path = Paths.get(GROUPS_CONFIG_FILE);
        if (!Files.exists(path)) {
            Logger.info("群组配置文件不存在，使用默认配置");
            return;
        }
        
        try {
            String content = Files.readString(path);
            
            // 解析JSON内容
            content = content.trim();
            if (content.startsWith("[") && content.endsWith("]")) {
                content = content.substring(1, content.length() - 1); // 移除开头和结尾的方括号
            } else {
                Logger.warn("群组配置文件格式错误，不是有效的JSON数组");
                return;
            }
            
            // 重置groups map以避免重复加载
            groups.clear();
            
            // 按对象边界分割JSON数组
            List<String> groupObjects = splitJsonArray(content);
            
            for (String groupStr : groupObjects) {
                parseGroupFromJson(groupStr.trim());
            }
            
            Logger.info("成功加载 " + groups.size() + " 个群组配置");
        } catch (Exception e) {
            Logger.error("加载群组配置失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 将JSON数组字符串分割为独立的对象字符串
     */
    private List<String> splitJsonArray(String jsonArray) {
        List<String> result = new ArrayList<>();
        int braceCount = 0;
        int start = 0;
        boolean inString = false;
        char quoteChar = '\0';
        
        // 处理可能的前置逗号或空格
        while (start < jsonArray.length() && Character.isWhitespace(jsonArray.charAt(start))) {
            start++;
        }
        
        if (jsonArray.startsWith(",")) {
            start = 1;
            while (start < jsonArray.length() && Character.isWhitespace(jsonArray.charAt(start))) {
                start++;
            }
        }
        
        for (int i = start; i < jsonArray.length(); i++) {
            char c = jsonArray.charAt(i);
            
            // 检查字符串边界
            if (c == '"' || c == '\'') {
                if (!inString) {
                    inString = true;
                    quoteChar = c;
                } else if (quoteChar == c) {
                    // 检查是否是转义的引号
                    if (i == 0 || jsonArray.charAt(i - 1) != '\\') {
                        inString = false;
                        quoteChar = '\0';
                    }
                }
            }
            
            if (!inString) {
                if (c == '{') {
                    if (braceCount == 0) {
                        start = i; // 记录新对象的开始位置
                    }
                    braceCount++;
                } else if (c == '}') {
                    braceCount--;
                    
                    // 当braceCount为0时，说明找到一个完整的对象
                    if (braceCount == 0) {
                        String obj = jsonArray.substring(start, i + 1);
                        result.add(obj);
                        
                        // 跳过逗号和空格
                        i++; // 移动到右括号后一位
                        while (i < jsonArray.length() && (jsonArray.charAt(i) == ',' || Character.isWhitespace(jsonArray.charAt(i)))) {
                            i++;
                        }
                        start = i;
                        i--; // 因为for循环会递增i
                    }
                }
            }
        }
        
        return result;
    }
    
    /**
     * 从JSON字符串解析群组对象
     */
    private void parseGroupFromJson(String groupJson) {
        try {
            // 提取name
            String name = extractJsonValue(groupJson, "name");
            if (name == null || name.isEmpty()) {
                Logger.warn("群组JSON中缺少name字段或name为空: " + groupJson);
                return;
            }
            
            // 创建群组对象并设置属性
            ServerGroup group = new ServerGroup();
            group.setName(name);
            
            // 提取其他字段
            group.setOrderedStartup(extractBooleanValue(groupJson, "orderedStartup"));
            group.setTriggerKeyword(extractJsonValue(groupJson, "triggerKeyword"));
            group.setStartupDelay(extractIntValue(groupJson, "startupDelay", 5000));
            group.setLaunchMode(extractIntValue(groupJson, "launchMode", 1));
            
            String presetJvmArgs = extractJsonValue(groupJson, "presetJvmArgs");
            if (presetJvmArgs != null && !presetJvmArgs.isEmpty() && !presetJvmArgs.equals("")) {
                group.setPresetJvmArgs(presetJvmArgs);
            }
            
            String presetServerArgs = extractJsonValue(groupJson, "presetServerArgs");
            if (presetServerArgs != null && !presetServerArgs.isEmpty() && !presetServerArgs.equals("")) {
                group.setPresetServerArgs(presetServerArgs);
            }
            
            String minMemory = extractJsonValue(groupJson, "minMemory");
            if (minMemory != null && !minMemory.isEmpty() && !minMemory.equals("")) {
                group.setMinMemory(minMemory);
            }
            
            String maxMemory = extractJsonValue(groupJson, "maxMemory");
            if (maxMemory != null && !maxMemory.isEmpty() && !maxMemory.equals("")) {
                group.setMaxMemory(maxMemory);
            }
            
            // 提取serverNames数组
            String serverNamesStr = extractJsonArray(groupJson, "serverNames");
            if (serverNamesStr != null) {
                List<String> serverNames = parseStringArray(serverNamesStr);
                group.setServerNames(serverNames);
            }
            
            // 将群组添加到映射中
            groups.put(name, group);
            
        } catch (Exception e) {
            Logger.error("解析群组JSON失败: " + e.getMessage() + ", JSON: " + groupJson, e);
        }
    }
    
    /**
     * 从JSON中提取字符串值
     */
    private String extractJsonValue(String json, String key) {
        String regex = "\"" + key + "\"\\s*:\\s*\"([^\"]*)\"";
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(regex);
        java.util.regex.Matcher matcher = pattern.matcher(json);
        
        if (matcher.find()) {
            return matcher.group(1);
        }
        
        // 尝试匹配布尔值或数字
        regex = "\"" + key + "\"\\s*:\\s*(true|false|[0-9]+)";
        pattern = java.util.regex.Pattern.compile(regex);
        matcher = pattern.matcher(json);
        
        if (matcher.find()) {
            String value = matcher.group(1);
            // 如果是布尔值或数字，返回字符串形式
            if (value.equals("true") || value.equals("false")) {
                return value.equals("true") ? "true" : "false";
            }
            return value;
        }
        
        return null;
    }
    
    /**
     * 从JSON中提取布尔值
     */
    private boolean extractBooleanValue(String json, String key) {
        String value = extractJsonValue(json, key);
        return value != null && value.equals("true");
    }
    
    /**
     * 从JSON中提取整数值
     */
    private int extractIntValue(String json, String key, int defaultValue) {
        String value = extractJsonValue(json, key);
        if (value != null) {
            try {
                return Integer.parseInt(value);
            } catch (NumberFormatException e) {
                return defaultValue;
            }
        }
        return defaultValue;
    }
    
    /**
     * 从JSON中提取数组部分
     */
    private String extractJsonArray(String json, String key) {
        String regex = "\"" + key + "\"\\s*:\\s*\\[(.*?)\\]";
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(regex);
        java.util.regex.Matcher matcher = pattern.matcher(json);
        
        if (matcher.find()) {
            return matcher.group(1);
        }
        
        return null;
    }
    
    /**
     * 解析字符串数组
     */
    private List<String> parseStringArray(String arrayStr) {
        List<String> result = new ArrayList<>();
        
        if (arrayStr == null || arrayStr.trim().isEmpty()) {
            return result;
        }
        
        // 处理数组中的每个字符串
        int start = 0;
        int quoteCount = 0;
        boolean inQuotes = false;
        char quoteChar = '\0';
        
        for (int i = 0; i < arrayStr.length(); i++) {
            char c = arrayStr.charAt(i);
            
            if (c == '"' || c == '\'') {
                if (!inQuotes) {
                    inQuotes = true;
                    quoteChar = c;
                } else if (quoteChar == c) {
                    // 检查是否是转义的引号
                    if (i == 0 || arrayStr.charAt(i - 1) != '\\') {
                        inQuotes = false;
                    }
                }
            } else if (c == ',' && !inQuotes) {
                // 找到数组元素分隔符
                String element = arrayStr.substring(start, i).trim();
                if (element.startsWith("\"") && element.endsWith("\"")) {
                    element = element.substring(1, element.length() - 1);
                } else if (element.startsWith("'") && element.endsWith("'")) {
                    element = element.substring(1, element.length() - 1);
                }
                if (!element.isEmpty()) {
                    result.add(element);
                }
                start = i + 1;
            }
        }
        
        // 添加最后一个元素
        String element = arrayStr.substring(start).trim();
        if (element.startsWith("\"") && element.endsWith("\"")) {
            element = element.substring(1, element.length() - 1);
        } else if (element.startsWith("'") && element.endsWith("'")) {
            element = element.substring(1, element.length() - 1);
        }
        if (!element.isEmpty()) {
            result.add(element);
        }
        
        return result;
    }
    
    /**
     * 构建JVM参数，整合内存参数和其他JVM参数
     */
    private String buildJvmArgs(ServerGroup group, Server server) {
        StringBuilder jvmArgsBuilder = new StringBuilder();
        
        // 添加最小内存参数
        if (group.getMinMemory() != null) {
            jvmArgsBuilder.append("-Xms").append(group.getMinMemory()).append(" ");
        }
        
        // 添加最大内存参数
        if (group.getMaxMemory() != null) {
            jvmArgsBuilder.append("-Xmx").append(group.getMaxMemory()).append(" ");
        }
        
        // 如果群组有预设的JVM参数，将其追加到内存参数后面
        if (group.getPresetJvmArgs() != null) {
            jvmArgsBuilder.append(group.getPresetJvmArgs()).append(" ");
        }
        
        // 如果服务器有自己的默认JVM参数且群组没有预设参数，则使用服务器的参数
        if (group.getPresetJvmArgs() == null && server.getDefaultJvmArgs() != null) {
            jvmArgsBuilder.append(server.getDefaultJvmArgs()).append(" ");
        }
        
        String jvmArgs = jvmArgsBuilder.toString().trim();
        
        // 如果结果为空，返回null
        return jvmArgs.isEmpty() ? null : jvmArgs;
    }
    
    /**
     * 停止群组中的所有服务器
     */
    public void stopGroup(String groupName) {
        ServerGroup group = groups.get(groupName);
        if (group == null) {
            Logger.warn("群组不存在: " + groupName);
            return;
        }
        
        Logger.info("停止群组 " + groupName + " 中的所有服务器");
        
        for (String serverName : group.getServerNames()) {
            try {
                serverService.stopServer(serverName);
                Logger.info("服务器 " + serverName + " 停止命令已发送");
            } catch (Exception e) {
                Logger.error("停止服务器失败 " + serverName + ": " + e.getMessage(), e);
            }
        }
    }
}