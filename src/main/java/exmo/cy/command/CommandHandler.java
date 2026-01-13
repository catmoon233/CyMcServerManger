package exmo.cy.command;

import exmo.cy.exception.ConfigurationException;
import exmo.cy.exception.ServerOperationException;
import exmo.cy.model.LaunchConfig;
import exmo.cy.model.Server;
import exmo.cy.model.ServerInstance;
import exmo.cy.service.ServerService;
import exmo.cy.util.JavaPathFinder;
import exmo.cy.util.Logger;

import java.util.*;

/**
 * 命令处理器
 * 处理用户输入的各种命令
 */
public class CommandHandler {
    
    private final ServerService serverService;
    private final Scanner scanner;
    private ServerInstance attachedServer;
    
    /**
     * 构造函数
     * @param scanner 输入扫描器
     */
    public CommandHandler(Scanner scanner) {
        this.serverService = new ServerService();
        this.scanner = scanner;
        this.attachedServer = null;
    }
    
    /**
     * 处理命令
     * @param command 命令字符串
     * @return 是否继续运行
     */
    public boolean handleCommand(String command) {
        try {
            switch (command.toLowerCase().trim()) {
                case "create":
                    handleCreate();
                    break;
                case "add":
                    handleAdd();
                    break;
                case "switch":
                    handleSwitch();
                    break;
                case "start":
                    handleStart();
                    break;
                case "last":
                    handleLast();
                    break;
                case "stop":
                    return false;
                case "help":
                    handleHelp();
                    break;
                case "list":
                    handleList();
                    break;
                case "map":
                    handleMap();
                    break;
                case "delete":
                    handleDelete();
                    break;
                case "attach":
                case "at":
                    handleAttach();
                    break;
                case "list-running":
                case "lr":
                    handleListRunning();
                    break;
                case "stop-server":
                case "ss":
                    handleStopServer();
                    break;
                default:
                    Logger.println("未知指令。输入 'help' 查看可用指令");
            }
        } catch (Exception e) {
            Logger.error("执行命令时出错: " + e.getMessage(), e);
        }
        return true;
    }
    
    /**
     * 处理服务器控制台命令
     * @param input 输入
     * @return 是否继续运行
     */
    public boolean handleConsoleCommand(String input) {
        if ("detach".equalsIgnoreCase(input)) {
            attachedServer = null;
            Logger.println("已返回主控制台");
            return true;
        } else if ("force-stop".equalsIgnoreCase(input)) {
            handleForceStop();
            return true;
        }
        
        // 转发命令到服务器
        try {
            serverService.sendCommand(attachedServer.getServer().getName(), input);
        } catch (ServerOperationException e) {
            Logger.error("转发输入失败，服务器可能已关闭: " + e.getMessage());
            attachedServer = null;
        }
        return true;
    }
    
    /**
     * 处理创建服务器命令
     */
    private void handleCreate() throws Exception {
        // 实现创建服务器逻辑
        Logger.println("=== 创建新服务器 ===");
        
        // TODO: 列出可用核心文件并让用户选择
        // TODO: 获取服务器名称和描述
        // TODO: 调用serverService.createServer()
        
        Logger.println("此功能正在实现中...");
    }
    
    /**
     * 处理添加现有服务器命令
     */
    private void handleAdd() throws Exception {
        Logger.println("=== 添加现有服务器 ===");
        
        Logger.print("输入服务器目录路径: ");
        String path = scanner.nextLine().trim();
        
        Logger.print("输入服务器名称: ");
        String name = scanner.nextLine().trim();
        
        Logger.print("输入服务器描述: ");
        String description = scanner.nextLine().trim();
        
        Logger.print("输入服务器版本: ");
        String version = scanner.nextLine().trim();
        
        serverService.addExistingServer(path, name, version, description);
        Logger.println("服务器 " + name + " 添加成功！");
    }
    
    /**
     * 处理切换核心版本命令
     */
    private void handleSwitch() throws Exception {
        Logger.println("=== 切换服务器核心版本 ===");
        
        // TODO: 实现切换核心版本逻辑
        
        Logger.println("此功能正在实现中...");
    }
    
    /**
     * 处理启动服务器命令
     */
    private void handleStart() throws Exception {
        List<Server> servers = serverService.getConfigManager().loadServers();
        if (servers.isEmpty()) {
            Logger.println("错误: 没有可用服务器");
            return;
        }
        
        // 显示服务器列表
        Logger.println("可用服务器：");
        for (int i = 0; i < servers.size(); i++) {
            Server server = servers.get(i);
            Logger.println((i + 1) + ". " + server.getName());
            Logger.println("   版本: " + server.getVersion());
            Logger.println("   描述: " + server.getDescription());
            Logger.println("   当前地图: " + (server.getMap() != null ? server.getMap() : "未设置"));
        }
        
        Logger.print("选择服务器编号: ");
        int choice = Integer.parseInt(scanner.nextLine()) - 1;
        if (choice < 0 || choice >= servers.size()) {
            Logger.println("错误: 无效的选择");
            return;
        }
        
        Server selectedServer = servers.get(choice);
        
        // 选择Java路径
        String selectedJavaPath = selectJavaPath();
        
        // 选择启动模式
        int mode = selectLaunchMode();
        
        // 获取自定义参数（如果是自定义模式）
        String jvmArgs = null;
        String serverArgs = null;
        if (mode == 5) {
            Logger.print("输入JVM参数(以空格分隔): ");
            jvmArgs = scanner.nextLine().trim();
            Logger.print("输入服务器参数(以空格分隔): ");
            serverArgs = scanner.nextLine().trim();
        }
        
        // 启动服务器
        serverService.startServer(selectedServer, mode, selectedJavaPath, jvmArgs, serverArgs);
        Logger.println("服务器 " + selectedServer.getName() + " 已启动");
    }
    
    /**
     * 选择Java路径
     */
    private String selectJavaPath() {
        List<String> javaPaths = JavaPathFinder.findAvailableJavaPaths();
        
        if (javaPaths.isEmpty()) {
            Logger.println("未找到可用的Java路径，将使用默认Java");
            return "java";
        }
        
        Logger.println("可用的Java路径：");
        Logger.println("0. 默认 (java)");
        for (int i = 0; i < javaPaths.size(); i++) {
            Logger.println((i + 1) + ". " + javaPaths.get(i));
        }
        
        Logger.print("请选择Java路径编号: ");
        int choice = Integer.parseInt(scanner.nextLine());
        
        if (choice < 0 || choice > javaPaths.size()) {
            Logger.println("无效的选择，使用默认Java");
            return "java";
        }
        
        return choice == 0 ? "java" : javaPaths.get(choice - 1);
    }
    
    /**
     * 选择启动模式
     */
    private int selectLaunchMode() {
        Logger.println("选择启动模式：");
        Logger.println("1. 核心版本启动");
        Logger.println("2. 整合包启动");
        Logger.println("3. 基础核心版本");
        Logger.println("4. 基础核心版本(修复)");
        Logger.println("5. 自定义启动模式");
        
        Logger.print("输入选项: ");
        return Integer.parseInt(scanner.nextLine());
    }
    
    /**
     * 处理使用上次启动配置命令
     */
    private void handleLast() throws Exception {
        Optional<LaunchConfig> configOpt = serverService.getConfigManager().loadLastLaunchConfig();
        
        if (!configOpt.isPresent()) {
            Logger.println("错误: 未找到上次启动配置");
            return;
        }
        
        LaunchConfig config = configOpt.get();
        Optional<Server> serverOpt = serverService.getConfigManager().findServerByName(config.getServerName());
        
        if (!serverOpt.isPresent()) {
            Logger.println("错误: 服务器 " + config.getServerName() + " 不存在");
            return;
        }
        
        Server server = serverOpt.get();
        
        // 显示启动信息
        Logger.println("即将使用上次参数启动：");
        Logger.println("服务器: " + server.getName());
        Logger.println("模式: " + getLaunchModeName(config.getLaunchMode()));
        Logger.print("确定启动? (y/n): ");
        
        if (!"y".equalsIgnoreCase(scanner.nextLine().trim())) {
            Logger.println("启动已取消");
            return;
        }
        
        // 启动服务器
        serverService.startServer(server, config.getLaunchMode(), config.getJavaPath(), 
                                 config.getJvmArgs(), config.getServerArgs());
        Logger.println("服务器 " + server.getName() + " 已启动");
    }
    
    /**
     * 获取启动模式名称
     */
    private String getLaunchModeName(int mode) {
        switch (mode) {
            case 1: return "核心版本";
            case 2: return "整合包";
            case 3: return "基础核心版本";
            case 4: return "基础核心版本(修复)";
            case 5: return "自定义";
            default: return "未知";
        }
    }
    
    /**
     * 处理帮助命令
     */
    private void handleHelp() {
        Logger.println("可用指令说明：");
        Logger.println("create - 创建新服务器");
        Logger.println("add - 添加现有服务器目录");
        Logger.println("switch - 切换服务器核心版本");
        Logger.println("start - 启动服务器");
        Logger.println("last - 调用上次的参数启动服务器");
        Logger.println("stop - 退出程序");
        Logger.println("help - 显示此帮助信息");
        Logger.println("list - 列出所有服务器");
        Logger.println("map - 切换服务器地图");
        Logger.println("delete - 删除服务器配置和本地文件");
        Logger.println("attach/at - 连接到运行中的服务器控制台");
        Logger.println("detach - 从服务器控制台返回主控制台");
        Logger.println("list-running/lr - 列出所有运行中的服务器");
        Logger.println("stop-server/ss - 正常停止指定服务器");
        Logger.println("force-stop - 强制终止当前连接的服务器");
    }
    
    /**
     * 处理列出服务器命令
     */
    private void handleList() throws ConfigurationException {
        List<Server> servers = serverService.getConfigManager().loadServers();
        if (servers.isEmpty()) {
            Logger.println("没有可用服务器");
            return;
        }
        
        Logger.println("服务器列表：");
        for (Server server : servers) {
            Logger.println("名称: " + server.getName());
            Logger.println("  版本: " + server.getVersion());
            Logger.println("  描述: " + server.getDescription());
            Logger.println("  当前地图: " + (server.getMap() != null ? server.getMap() : "未设置"));
            Logger.println("  路径: " + server.getCorePath());
            // Logger.println();
        }
    }
    
    /**
     * 处理切换地图命令
     */
    private void handleMap() {
        Logger.println("地图切换功能正在实现中...");
    }
    
    /**
     * 处理删除服务器命令
     */
    private void handleDelete() throws Exception {
        List<Server> servers = serverService.getConfigManager().loadServers();
        if (servers.isEmpty()) {
            Logger.println("错误: 没有可用服务器");
            return;
        }
        
        Logger.println("可用服务器：");
        for (int i = 0; i < servers.size(); i++) {
            Logger.println((i + 1) + ". " + servers.get(i).getName());
        }
        
        Logger.print("选择要删除的服务器编号: ");
        int choice = Integer.parseInt(scanner.nextLine()) - 1;
        if (choice < 0 || choice >= servers.size()) {
            Logger.println("错误: 无效的选择");
            return;
        }
        
        Server server = servers.get(choice);
        Logger.print("确定要删除服务器配置和本地文件? (y/n): ");
        
        if ("y".equalsIgnoreCase(scanner.nextLine().trim())) {
            serverService.deleteServer(server.getName(), true);
            Logger.println("服务器 " + server.getName() + " 已删除");
        } else {
            Logger.println("操作已取消");
        }
    }
    
    /**
     * 处理连接服务器命令
     */
    private void handleAttach() {
        Map<String, ServerInstance> activeServers = serverService.getActiveServers();
        if (activeServers.isEmpty()) {
            Logger.println("没有运行中的服务器");
            return;
        }
        
        List<ServerInstance> serverList = new ArrayList<>(activeServers.values());
        Logger.println("运行中的服务器：");
        for (int i = 0; i < serverList.size(); i++) {
            Logger.println((i + 1) + ". " + serverList.get(i).getServer().getName());
        }
        
        Logger.print("输入要连接的服务器序号: ");
        try {
            int choice = Integer.parseInt(scanner.nextLine()) - 1;
            if (choice < 0 || choice >= serverList.size()) {
                Logger.println("错误: 无效的选择");
                return;
            }
            
            attachedServer = serverList.get(choice);
            Logger.println("已连接到服务器 " + attachedServer.getServer().getName());
            Logger.println("输入 'detach' 返回主控制台，'force-stop' 强制停止服务器");
        } catch (NumberFormatException e) {
            Logger.println("错误: 请输入有效的数字");
        }
    }
    
    /**
     * 处理列出运行中服务器命令
     */
    private void handleListRunning() {
        Map<String, ServerInstance> activeServers = serverService.getActiveServers();
        if (activeServers.isEmpty()) {
            Logger.println("没有运行中的服务器");
        } else {
            Logger.println("运行中的服务器：");
            for (ServerInstance instance : activeServers.values()) {
                Logger.println("- " + instance.getServer().getName() + 
                             " (版本: " + instance.getServer().getVersion() + 
                             ", 运行时长: " + (instance.getUptime() / 1000) + "秒)");
            }
        }
    }
    
    /**
     * 处理停止服务器命令
     */
    private void handleStopServer() {
        Map<String, ServerInstance> activeServers = serverService.getActiveServers();
        if (activeServers.isEmpty()) {
            Logger.println("没有运行中的服务器");
            return;
        }
        
        List<ServerInstance> serverList = new ArrayList<>(activeServers.values());
        Logger.println("运行中的服务器：");
        for (int i = 0; i < serverList.size(); i++) {
            Logger.println((i + 1) + ". " + serverList.get(i).getServer().getName());
        }
        
        Logger.print("输入要停止的服务器序号: ");
        try {
            int choice = Integer.parseInt(scanner.nextLine()) - 1;
            if (choice < 0 || choice >= serverList.size()) {
                Logger.println("错误: 无效的选择");
                return;
            }
            
            ServerInstance instance = serverList.get(choice);
            serverService.stopServer(instance.getServer().getName());
            Logger.println("已发送停止指令到服务器 " + instance.getServer().getName());
        } catch (Exception e) {
            Logger.error("停止服务器时出错: " + e.getMessage());
        }
    }
    
    /**
     * 处理强制停止命令
     */
    private void handleForceStop() {
        if (attachedServer == null) {
            Logger.println("错误：当前未连接到任何服务器");
            return;
        }
        
        try {
            String serverName = attachedServer.getServer().getName();
            Logger.println("正在强制终止服务器 " + serverName + "...");
            serverService.forceStopServer(serverName);
            Logger.println("服务器 " + serverName + " 已强制关闭");
            attachedServer = null;
        } catch (ServerOperationException e) {
            Logger.error("强制关闭服务器时出错: " + e.getMessage());
        }
    }
    
    /**
     * 检查是否连接到服务器
     * @return 如果已连接返回true
     */
    public boolean isAttached() {
        return attachedServer != null;
    }
    
    /**
     * 获取当前连接的服务器名称
     * @return 服务器名称
     */
    public String getAttachedServerName() {
        return attachedServer != null ? attachedServer.getServer().getName() : null;
    }
}