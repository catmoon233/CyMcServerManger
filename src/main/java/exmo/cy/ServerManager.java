package exmo.cy;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

import com.google.gson.*;
import com.google.gson.stream.*;



public class ServerManager {
    static class LastLaunchConfig {
        String serverName;
        int launchMode;
        String javaPath; // 添加java路径字段
        String jvmArgs;  // 添加JVM参数字段
        String serverArgs; // 添加服务器参数字段
    }
   static class Server {
        String name;
        String corePath;
        String version;
        String description;
        boolean isModpack;
        String map;  // 新增地图字段
    }

    // 新增服务器实例管理类
    static class ServerInstance {
        Process process;
        Server server;
        OutputStream processInput;
    }

    private static final Map<String, ServerInstance> activeServers = new ConcurrentHashMap<>();
    private static volatile ServerInstance attachedServer = null;
    
    // 添加缺失的常量定义
    private static final String LAST_LAUNCH_CONFIG = "lastLaunch.json";
    private static final String MAPS_DIR = "maps";      // 地图存储目录
    private static final String BACKUPS_DIR = "backups"; // 备份目录
    private static final String CORES_DIR = "cores";
    private static final String SERVERS_DIR = "servers";
    private static final String CONFIG_FILE = "serverList.json";
    
    public static void main(String[] args) {
        // 1. 指令循环系统
        Scanner scanner = new Scanner(System.in);
        printf("CyMcServer> 输入help以查看指令\n");
        while (true) {
            if (attachedServer != null) {
                printf(attachedServer.server.name + "> ");
                String input = scanner.nextLine().trim();
                if ("detach".equalsIgnoreCase(input)) {
                    attachedServer = null;
                    printinf("已返回主控制台");
                    continue;
                } else if ("force-stop".equalsIgnoreCase(input)) {
                    forceStopServer();
                    continue;
                }
                try {
                    attachedServer.processInput.write((input + "\n").getBytes());
                    attachedServer.processInput.flush();
                } catch (IOException e) {
                    printinf("转发输入失败，服务器可能已关闭");
                    attachedServer = null;
                }
            } else {
                printf("CyMcServer> ");
                String input = scanner.nextLine().trim().toLowerCase();
                
                switch (input) {
                    case "create":
                        createServer(scanner);
                        break;
                    case "add":
                        addServer(scanner);
                        break;
                    case "switch":
                        switchCoreVersion(scanner);
                        break;
                    case "start":
                        startServer(scanner);
                        break;
                    case "last":
                        useLastLaunch(scanner);
                        break;
                    case "stop":
                        printinf("退出服务器管理器");
                        return;
                    case "help":
                        showHelp();
                        break;
                    case "list":
                        listServers();
                        break;
                    case "map":
                        changeMap(scanner);
                        break;
                    case "delete":
                        deleteServer(scanner);
                        break;
                    case "attach":
                        attachServer(scanner);
                        break;
                    case "list-running":
                        listRunningServers();
                        break;
                    case "lr":
                        listRunningServers();
                        break;
                    case "stop-server":
                        stopServer(scanner);
                        break;
                    case "ss":
                        stopServer(scanner);
                        break;
                    case "at":
                        attachServer(scanner);
                        break;
                    default:
                        printinf("未知指令。可用指令: create, add, switch, start, stop, help, list, map, delete, last, attach, list-running, stop-server");
                }
            }
        }
    }

    // 新增方法：强制停止当前连接的服务器
    private static void forceStopServer() {
        if (attachedServer == null) {
            printinf("错误：当前未连接到任何服务器");
            return;
        }

        ServerInstance instance = attachedServer;
        printinf("正在强制终止服务器 " + instance.server.name + "...");

        try {
            // 强制终止进程
            instance.process.destroyForcibly();
            // 等待进程结束
            int exitCode = instance.process.waitFor();
            printinf("服务器 " + instance.server.name + " 已强制关闭 (退出代码: " + exitCode + ")");
        } catch (Exception e) {
            printinf("强制关闭服务器时出错: " + e.getMessage());
        } finally {
            // 清理资源
            activeServers.remove(instance.server.name);
            attachedServer = null;
        }
    }

    // 新增方法：正常停止指定服务器
    private static void stopServer(Scanner scanner) {
        if (activeServers.isEmpty()) {
            printinf("没有运行中的服务器");
            return;
        }

        // 将服务器实例存入列表以保持顺序
        List<ServerInstance> serverList = new ArrayList<>(activeServers.values());
        
        printinf("运行中的服务器：");
        for (int i = 0; i < serverList.size(); i++) {
            ServerInstance instance = serverList.get(i);
            printinf((i+1) + ". " + instance.server.name);
        }

        printf("输入要停止的服务器序号: ");
        try {
            int choice = Integer.parseInt(scanner.nextLine().trim()) - 1;
            if (choice < 0 || choice >= serverList.size()) {
                printinf("错误: 无效的选择");
                return;
            }
            
            ServerInstance instance = serverList.get(choice);
            String name = instance.server.name;

            printinf("正在向服务器 " + name + " 发送停止指令...");
            try {
                // 发送stop命令请求正常关闭
                instance.processInput.write("stop\n".getBytes());
                instance.processInput.flush();
                printinf("已发送停止指令，请等待服务器正常关闭");
            } catch (IOException e) {
                printinf("发送停止指令失败: " + e.getMessage());
                // 如果发送失败，尝试强制关闭
                printinf("尝试强制关闭...");
                try {
                    instance.process.destroyForcibly();
                    int exitCode = instance.process.waitFor();
                    printinf("服务器 " + name + " 已强制关闭 (退出代码: " + exitCode + ")");
                } catch (Exception ex) {
                    printinf("强制关闭也失败: " + ex.getMessage());
                } finally {
                    activeServers.remove(name);
                    if (attachedServer != null && attachedServer.server.name.equals(name)) {
                        attachedServer = null;
                    }
                }
            }
        } catch (NumberFormatException e) {
            printinf("错误: 请输入有效的数字");
        }
    }

    // 新增方法：连接到运行中的服务器
    private static void attachServer(Scanner scanner) {
        if (activeServers.isEmpty()) {
            printinf("没有运行中的服务器");
            return;
        }
        
        // 将服务器实例存入列表以保持顺序
        List<ServerInstance> serverList = new ArrayList<>(activeServers.values());
        
        printinf("运行中的服务器：");
        for (int i = 0; i < serverList.size(); i++) {
            ServerInstance instance = serverList.get(i);
            printinf((i+1) + ". " + instance.server.name);
        }
        
        printf("输入要连接的服务器序号: ");
        try {
            int choice = Integer.parseInt(scanner.nextLine().trim()) - 1;
            if (choice < 0 || choice >= serverList.size()) {
                printinf("错误: 无效的选择");
                return;
            }
            
            ServerInstance instance = serverList.get(choice);
            attachedServer = instance;
            printinf("已连接到服务器 " + instance.server.name + "，输入 'detach' 返回主控制台");
        } catch (NumberFormatException e) {
            printinf("错误: 请输入有效的数字");
        }
    }

    // 新增方法：列出运行中的服务器
    private static void listRunningServers() {
        if (activeServers.isEmpty()) {
            printinf("没有运行中的服务器");
        } else {
            printinf("运行中的服务器：");
            for (ServerInstance instance : activeServers.values()) {
                printinf("- " + instance.server.name + " (版本: " + instance.server.version + ")");
            }
        }
    }

    private static boolean startServer(Scanner scanner) {
        List<Server> servers = loadServerList();
        if (servers.isEmpty()) {
            printinf("错误: 没有可用服务器");
            return false;
        }
        
        printinf("可用服务器：");
        for (int i=0; i<servers.size(); i++) {
            Server server = servers.get(i);
            printinf((i+1) + ". " + server.name);
            printinf("   版本: " + server.version);
            printinf("   描述: " + server.description);
            printinf("   当前地图: " + (server.map != null ? server.map : "未设置"));
        }
        
        printf("选择服务器编号: ");
        int choice = Integer.parseInt(scanner.nextLine()) - 1;
        if (choice < 0 || choice >= servers.size()) {
            printinf("错误: 无效的选择");
            return false;
        }
        
        Server selectedServer = servers.get(choice);
        
        // 确认启动
        printinf("即将启动服务器: " + selectedServer.name);
        printinf("核心版本: " + selectedServer.version);
        printinf("当前地图: " + (selectedServer.map != null ? selectedServer.map : "未设置"));
        String selectedJavaPath = "java";
        // 选择Java版本
        List<String> javaPaths = findAvailableJavaPaths();
        if (!javaPaths.isEmpty()) {
            printinf("可用的Java路径：");
            printinf("0. 默认 (java)");
            for (int i = 0; i < javaPaths.size(); i++) {
                printinf((i + 1) + ". " + javaPaths.get(i));
            }
            printf("请选择Java路径编号: ");
            int javaChoice = Integer.parseInt(scanner.nextLine());
            
            if (javaChoice < 0 || javaChoice > javaPaths.size()) {
                printinf("错误: 无效的选择");
                return false;
            }
            
            selectedJavaPath = javaChoice == 0 ? "java" : javaPaths.get(javaChoice - 1);
        } else {
            printinf("未找到可用的Java路径，将使用默认Java");
        }
        
        // 选择启动模式
        printinf("选择启动模式：");
        printinf("1. 核心版本启动");
        printinf("2. 整合包启动");
        printinf("3. 基础核心版本");
        printinf("4. 基础核心版本(修复)");
        printinf("5. 自定义启动模式");
        printf("输入选项: ");
        int mode = Integer.parseInt(scanner.nextLine());
        
        // 如果选择了自定义模式，获取用户输入的参数
        String customJvmArgs = "";
        String customServerArgs = "";
        if (mode == 5) {
            printf("输入JVM参数(以空格分隔，例如: -Xms512M -Xmx2G): ");
            customJvmArgs = scanner.nextLine().trim();
            printf("输入服务器参数(以空格分隔，例如: --nogui): ");
            customServerArgs = scanner.nextLine().trim();
            
            if (customJvmArgs.isEmpty() && customServerArgs.isEmpty()) {
                printinf("错误: 自定义模式需要至少提供一个参数");
                return false;
            }
        }
        
        try {
            ProcessBuilder pb = new ProcessBuilder();
            File parentFile = new File(selectedServer.corePath).getParentFile();
            pb.directory(parentFile);
            
            if (mode == 1) {
                // 核心版本启动 - 添加最高权限等级参数
                pb.command(selectedJavaPath, "-Xms128M", "-Xmx23347M", 
                    "-Dterminal.jline=false", "-Dterminal.ansi=true", 
                    "-Dfile.encoding=UTF-8", "-Dlog4j2.formatMsgNoLookups=true", 
                    "-Duser.timezone=Asia/Shanghai", 
                    "-Dfunction.permission.level=4", // 设置函数权限等级为最高
                    "-Dop.permission.level=4",       // 设置OP权限等级为最高
                    "-jar", "Core.jar", "-nogui");
            } else if (mode == 2) {
                // 整合包启动 - 添加最高权限等级参数
                pb.command(selectedJavaPath, "-Xms128M", "-Xmx23347M", 
                    "-Dterminal.jline=false", "-Dterminal.ansi=true", 
                    "-Dfile.encoding=UTF-8", "-Dlog4j2.formatMsgNoLookups=true", 
                    "-Duser.timezone=Asia/Shanghai", 
                    "-Dfunction.permission.level=4", // 设置函数权限等级为最高
                    "-Dop.permission.level=4",       // 设置OP权限等级为最高
                    "@cnmforge.txt", "-nogui");
            } else if (mode == 3) {
                // 基础核心版本 - 添加最高权限等级参数
                pb.command(selectedJavaPath, 
                    "-Dfunction.permission.level=4", // 设置函数权限等级为最高
                    "-Dop.permission.level=4",       // 设置OP权限等级为最高
                    "-jar", "Core.jar");
            } else if (mode == 4) {
                // 基础核心版本 - 添加最高权限等级参数
                pb.command(selectedJavaPath,
                    "-Dfunction.permission.level=4", // 设置函数权限等级为最高
                    "-Dop.permission.level=4","-Dpaper.disableChannelLimit=true",       // 设置OP权限等级为最高
                    "-jar", "Core.jar");
            } else if (mode == 5) {
                // 自定义启动模式
                List<String> command = new ArrayList<>();
                command.add(selectedJavaPath);
                
                // 添加JVM参数
                if (!customJvmArgs.isEmpty()) {
                    String[] jvmArgsArray = customJvmArgs.split("\\s+");
                    command.addAll(Arrays.asList(jvmArgsArray));
                }
                
                // 添加必须的参数
                command.add("-jar");
                command.add("Core.jar");
                
                // 添加服务器参数
                if (!customServerArgs.isEmpty()) {
                    String[] serverArgsArray = customServerArgs.split("\\s+");
                    command.addAll(Arrays.asList(serverArgsArray));
                }
                
                pb.command(command);
            } else {
                printinf("错误: 无效的启动模式");
                return false;
            }
            LastLaunchConfig config = new LastLaunchConfig();
            config.serverName = selectedServer.name;
            config.launchMode = mode;
            config.javaPath = selectedJavaPath; // 保存选择的Java路径
            // 如果是自定义模式，保存参数
            if (mode == 5) {
                config.jvmArgs = customJvmArgs;
                config.serverArgs = customServerArgs;
            }

            try {
                Gson gson = new GsonBuilder().setPrettyPrinting().create();
                BufferedWriter writer = Files.newBufferedWriter(Paths.get(LAST_LAUNCH_CONFIG));
                writer.write(gson.toJson(config));
                writer.close();
            } catch (Exception e) {
                printinf("保存最后启动配置时发生错误: " + e.getMessage());
            }

            printinf("正在启动服务器 " + selectedServer.name + "...");
            Process process = pb.start();

            // 读取进程输出
            StreamGobbler outputGobbler = new StreamGobbler(process.getInputStream(), System.out::println);
            StreamGobbler errorGobbler = new StreamGobbler(process.getErrorStream(), System.err::println);

            Thread outputThread = new Thread(outputGobbler);
            outputThread.start();
            Thread errorThread = new Thread(errorGobbler);
            errorThread.start();

            // 创建服务器实例
            ServerInstance instance = new ServerInstance();
            instance.process = process;
            instance.server = selectedServer;
            instance.processInput = process.getOutputStream();

            activeServers.put(selectedServer.name, instance);

            // 监控进程结束
            new Thread(() -> {
                try {
                    int exitCode = process.waitFor();
                    printinf("服务器 " + selectedServer.name + " 已关闭 (退出代码: " + exitCode + ")");
                    activeServers.remove(selectedServer.name);
                    // 如果当前attached到这个服务器，需要detach
                    if (attachedServer != null && attachedServer.server.name.equals(selectedServer.name)) {
                        attachedServer = null;
                        printinf("已自动返回主控制台（服务器已关闭）");
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }).start();

            printinf("服务器 " + selectedServer.name + " 已启动，ID: " + selectedServer.name);
            return false;

        } catch (Exception e) {
            printinf("启动服务器时发生错误: " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }
    
    // 新增方法：显示帮助信息
    private static void showHelp() {
        printinf("可用指令说明：");
        printinf("create - 创建新服务器");
        printinf("add - 添加现有服务器目录");
        printinf("switch - 切换服务器核心版本");
        printinf("start - 启动服务器");
        printinf("stop - 退出程序");
        printinf("help - 显示此帮助信息");
        printinf("list - 列出所有服务器");
        printinf("map - 切换服务器地图");
        printinf("last - 调用上次的参数启动服务器");
        printinf("delete - 删除服务器配置和本地文件");
        printinf("attach - 连接到运行中的服务器控制台");
        printinf("detach - 从服务器控制台返回主控制台（在服务器控制台模式下使用）");
        printinf("list-running - 列出所有运行中的服务器");
        printinf("stop-server - 正常停止指定服务器（发送stop命令）");
        printinf("force-stop - 强制终止当前连接的服务器（在服务器控制台中使用）");
    }
    
    private static void presetOnCreate(String path){
        Path presetDir = Paths.get("preset");
        if (!Files.exists(presetDir)) {
            printinf("预设目录不存在，跳过预设应用");
            return;
        }

        try {
            // 查找所有包含preset.txt的子文件夹
            Map<String, String> presets = new HashMap<>();
            Files.walk(presetDir, 1)
                .filter(Files::isDirectory)
                .forEach(dir -> {
                    try {
                        Path presetFile = dir.resolve("preset.txt");
                        if (Files.exists(presetFile)) {
                            String description = Files.readString(presetFile).trim();
                            presets.put(dir.getFileName().toString(), description);
                        }
                    } catch (IOException e) {
                        printinf("读取预设文件失败: " + e.getMessage());
                    }
                });

            if (presets.isEmpty()) {
                printinf("没有找到可用预设");
                return;
            }

            // 显示预设列表
            printinf("可用预设：");
            int index = 1;
            Map<Integer, String> indexToPreset = new HashMap<>();
            for (Map.Entry<String, String> entry : presets.entrySet()) {
                printinf(index + ". " + entry.getKey() + " - " + entry.getValue());
                indexToPreset.put(index, entry.getKey());
                index++;
            }

            // 用户选择预设
            Scanner scanner = new Scanner(System.in);
            printf("选择预设编号 (0跳过): ");
            int choice = Integer.parseInt(scanner.nextLine());
            
            if (choice == 0) {
                printinf("跳过预设应用");
                return;
            }
            
            if (!indexToPreset.containsKey(choice)) {
                printinf("无效选择，跳过预设应用");
                return;
            }

            String selectedPreset = indexToPreset.get(choice);
            Path sourcePresetDir = presetDir.resolve(selectedPreset);
            Path targetDir = Paths.get(path);

            // 复制预设文件到目标目录
            Files.walk(sourcePresetDir)
                .filter(source -> !source.equals(sourcePresetDir.resolve("preset.txt"))) // 跳过preset.txt
                .forEach(source -> {
                    try {
                        Path target = targetDir.resolve(sourcePresetDir.relativize(source));
                        if (Files.isDirectory(source)) {
                            Files.createDirectories(target);
                        } else {
                            Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
                        }
                    } catch (IOException e) {
                        printinf("复制预设文件时出错: " + e.getMessage());
                    }
                });

            printinf("预设 '" + selectedPreset + "' 已应用到服务器目录");

        } catch (IOException e) {
            printinf("处理预设时发生错误: " + e.getMessage());
        }
    }

    private static void createServer(Scanner scanner) {
        // 实现创建服务器功能
        try {
            // 检查核心文件夹
            Path coresPath = Paths.get(CORES_DIR);
            if (!Files.exists(coresPath)) {
                printinf("错误: 未找到核心文件夹 (" + CORES_DIR + ")");
                return;
            }
            
            // 获取核心列表
            List<String> cores = new ArrayList<>();
            Files.list(coresPath).forEach(path -> {
                if (path.toString().endsWith(".jar")) {
                    cores.add(path.getFileName().toString());
                }
            });
            
            if (cores.isEmpty()) {
                printinf("错误: 核心文件夹中没有jar文件");
                return;
            }
            
            // 显示核心列表
            printinf("可选核心文件：");
            for (int i=0; i<cores.size(); i++) {
                printinf((i+1) + ". " + cores.get(i));
            }
            
            // 获取用户选择
            printf("选择核心编号: ");
            int choice = Integer.parseInt(scanner.nextLine()) - 1;
            if (choice < 0 || choice >= cores.size()) {
                printinf("错误: 无效的选择");
                return;
            }
            
            String selectedCore = cores.get(choice);
            String version = selectedCore.replaceAll("[^0-9.]+", ""); // 从文件名提取版本号
            
            // 获取服务器信息
            printf("输入服务器名称: ");
            String name = scanner.nextLine();
            printf("输入服务器描述(可选): ");
            String description = scanner.nextLine();
            
            // 创建服务器目录
            Path serverDir = Paths.get(SERVERS_DIR, name);
            if (Files.exists(serverDir)) {
                printinf("错误: 服务器目录已存在");
                return;
            }
            Files.createDirectories(serverDir);
            
            // 复制核心文件
            Files.copy(
                Paths.get(CORES_DIR, selectedCore),
                Paths.get(serverDir.toString(), "Core.jar")
            );
            presetOnCreate(String.valueOf(serverDir));
            // 保存配置
            Server server = new Server();
            server.name = name;
            server.corePath = Paths.get(serverDir.toString(), "Core.jar").toString();
            server.version = version;
            server.description = description;
            server.isModpack = false;
            
            saveServerConfig(server);
            printinf("服务器 " + name + " 创建成功！");
            
        } catch (Exception e) {
            printinf("创建服务器时发生错误: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private static void addServer(Scanner scanner) {
        // 实现添加现有目录作为服务器
        printf("输入服务器目录路径: ");
        String path = scanner.nextLine();
        Path serverPath = Paths.get(path);
        
        if (!Files.exists(serverPath)) {
            printinf("错误: 目录不存在");
            return;
        }
        
        // 验证核心文件
        Path corePath = Paths.get(path, "Core.jar");
        if (!Files.exists(corePath)) {
            printinf("警告: 目录中没有Core.jar文件");
        }
        
        // 获取服务器信息
        printf("输入服务器名称: ");
        String name = scanner.nextLine();
        printf("输入服务器描述: ");
        String description = scanner.nextLine();
        printf("输入服务器版本: ");
        String version = scanner.nextLine();

        
        Server server = new Server();
        server.name = name;
        server.corePath = corePath.toString();
        server.version = version;
        server.description = description;
        server.isModpack = false;
        
        saveServerConfig(server);
        printinf("服务器 " + name + " 添加成功！");
    }
    
    private static void switchCoreVersion(Scanner scanner) {
        // 实现切换核心版本功能
        List<Server> servers = loadServerList();
        if (servers.isEmpty()) {
            printinf("错误: 没有可用服务器");
            return;
        }
        
        // 显示服务器列表
        printinf("可用服务器：");
        for (int i=0; i<servers.size(); i++) {
            printinf((i+1) + ". " + servers.get(i).name);
        }
        
        printf("选择服务器编号: ");
        int serverChoice = Integer.parseInt(scanner.nextLine()) - 1;
        if (serverChoice < 0 || serverChoice >= servers.size()) {
            printinf("错误: 无效的选择");
            return;
        }
        
        Server selectedServer = servers.get(serverChoice);
        
        // 获取新核心版本
        Path coresPath = Paths.get(CORES_DIR);
        List<String> cores = new ArrayList<>();
        try {
            Files.list(coresPath).forEach(path -> {
                if (path.toString().endsWith(".jar")) {
                    cores.add(path.getFileName().toString());
                }
            });
        } catch (IOException e) {
            printinf("错误: 无法读取核心文件夹");
            return;
        }
        
        if (cores.isEmpty()) {
            printinf("错误: 核心文件夹中没有jar文件");
            return;
        }
        
        printinf("可选核心版本：");
        for (int i=0; i<cores.size(); i++) {
            printinf((i+1) + ". " + cores.get(i));
        }
        
        printf("选择新核心编号: ");
        int coreChoice = Integer.parseInt(scanner.nextLine()) - 1;
        if (coreChoice < 0 || coreChoice >= cores.size()) {
            printinf("错误: 无效的选择");
            return;
        }
        
        String newCore = cores.get(coreChoice);
        //selectedServer.corePath = Paths.get(CORES_DIR, newCore).toString();
        selectedServer.version = newCore.replaceAll("[^0-9.]+", "");
        
        // 更新配置
        saveServerConfig(selectedServer);
        printinf("服务器 " + selectedServer.name + " 的核心已更新到 " + newCore);
    }
    
    private static void saveServerConfig(Server server) {
        // 保存服务器配置到JSON文件
        try {
            List<Server> servers = new ArrayList<>();
            Path configPath = Paths.get(CONFIG_FILE);
            
            if (Files.exists(configPath)) {
                Gson gson = new Gson();
                BufferedReader reader = Files.newBufferedReader(configPath);
                Server[] serverArray = gson.fromJson(reader, Server[].class);
                if (serverArray != null) {
                    servers.addAll(Arrays.asList(serverArray));
                }
            }
            
            // 替换现有服务器配置而不是直接添加
            boolean found = false;
            for (int i = 0; i < servers.size(); i++) {
                if (servers.get(i).name.equals(server.name)) {
                    servers.set(i, server);  // 替换现有配置
                    found = true;
                    break;
                }
            }
            if (!found) {
                servers.add(server);  // 新增服务器时才添加
            }
            
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            BufferedWriter writer = Files.newBufferedWriter(configPath);
            writer.write(gson.toJson(servers.toArray()));
            writer.close();
            
        } catch (Exception e) {
            printinf("保存配置文件时发生错误: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    // 新增方法：保存服务器配置列表
    private static void saveServerConfigList(List<Server> servers) {
        // 保存服务器配置列表到JSON文件
        try {
            Path configPath = Paths.get(CONFIG_FILE);
            
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            BufferedWriter writer = Files.newBufferedWriter(configPath);
            writer.write(gson.toJson(servers.toArray()));
            writer.close();
            
        } catch (Exception e) {
            printinf("保存配置文件时发生错误: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private static List<Server> loadServerList() {
        // 从JSON文件加载服务器列表
        Path configPath = Paths.get(CONFIG_FILE);
        if (!Files.exists(configPath)) {
            return new ArrayList<>();
        }
        
        try {
            Gson gson = new Gson();
            BufferedReader reader = Files.newBufferedReader(configPath);
            Server[] servers = gson.fromJson(reader, Server[].class);
            return servers != null ? Arrays.asList(servers) : new ArrayList<>();
        } catch (Exception e) {
            printinf("加载配置文件时发生错误: " + e.getMessage());
            return new ArrayList<>();
        }
    }
    
    // 辅助类用于读取进程输出
    static class StreamGobbler implements Runnable {
        private InputStream inputStream;
        private Consumer<String> consumer;
        
        public StreamGobbler(InputStream inputStream, Consumer<String> consumer) {
            this.inputStream = inputStream;
            this.consumer = consumer;
        }
        
        @Override
        public void run() {
            new BufferedReader(new InputStreamReader(inputStream)).lines().forEach(consumer);
        }
    }
    private static boolean useLastLaunch(Scanner scanner) {
    Path configPath = Paths.get(LAST_LAUNCH_CONFIG);
    if (!Files.exists(configPath)) {
        printinf("错误: 未找到上次启动配置");
        return false;
    }

    try {
        Gson gson = new Gson();
        BufferedReader reader = Files.newBufferedReader(configPath);
        LastLaunchConfig config = gson.fromJson(reader, LastLaunchConfig.class);

        // 查找对应服务器
        List<Server> servers = loadServerList();
        Server targetServer = servers.stream()
            .filter(s -> s.name.equals(config.serverName))
            .findFirst()
            .orElse(null);

        if (targetServer == null) {
            printinf("错误: 服务器 " + config.serverName + " 不存在");
            return false;
        }

        // 确认启动参数
        printinf("即将使用上次参数启动：");
        printinf("服务器: " + targetServer.name);
        printinf("模式: " + (config.launchMode == 1 ? "核心版本" : 
                          config.launchMode == 2 ? "整合包" : 
                          config.launchMode == 3 ? "基础核心版本" : 
                          config.launchMode == 4 ? "基础核心版本(修复)" : "自定义"));
        printf("确定启动? (y/n): ");

        if (!"y".equalsIgnoreCase(scanner.nextLine().trim())) {
            printinf("启动已取消");
            return false;
        }

        // 调用启动逻辑
        if (config.launchMode == 5) {
            // 自定义模式需要传递额外参数
            return startServerWithParams(targetServer, config.launchMode, config.javaPath, config.jvmArgs, config.serverArgs);
        } else {
            return startServerWithParams(targetServer, config.launchMode, config.javaPath);
        }

    } catch (Exception e) {
        printinf("读取启动配置时发生错误: " + e.getMessage());
    }
    return false;
}

private static boolean startServerWithParams(Server server, int mode, String savedJavaPath) {
    return startServerWithParams(server, mode, savedJavaPath, null, null);
}

private static boolean startServerWithParams(Server server, int mode, String savedJavaPath, String jvmArgs, String serverArgs) {
    try {
        ProcessBuilder pb = new ProcessBuilder();
        File parentFile = new File(server.corePath).getParentFile();
        pb.directory(parentFile);

        String selectedJavaPath = "java"; // 默认Java路径
        
        // 如果配置中保存了Java路径且该路径仍然存在，则使用保存的路径
        if (savedJavaPath != null && !savedJavaPath.isEmpty()) {
            File javaFile = new File(savedJavaPath);
            if (javaFile.exists() && javaFile.canExecute()) {
                selectedJavaPath = savedJavaPath;
            }
        }
        
        // 如果没有有效的保存路径，则查找可用的Java路径
        if ("java".equals(selectedJavaPath)) {
            List<String> javaPaths = findAvailableJavaPaths();
            if (!javaPaths.isEmpty()) {
                selectedJavaPath = javaPaths.get(0); // 使用第一个找到的Java路径
            }
        }

        if (mode == 1) {
            pb.command(selectedJavaPath, "-Xms128M", "-Xmx23347M",
                "-Dterminal.jline=false", "-Dterminal.ansi=true",
                "-Dfile.encoding=UTF-8", "-Dlog4j2.formatMsgNoLookups=true",
                "-Duser.timezone=Asia/Shanghai", 
                "-Dfunction.permission.level=4", // 设置函数权限等级为最高
                "-Dop.permission.level=4",       // 设置OP权限等级为最高
                "-jar", "Core.jar", "-nogui");
        } else if (mode == 2) {
            pb.command(selectedJavaPath, "-Xms128M", "-Xmx23347M",
                "-Dterminal.jline=false", "-Dterminal.ansi=true",
                "-Dfile.encoding=UTF-8", "-Dlog4j2.formatMsgNoLookups=true",
                "-Duser.timezone=Asia/Shanghai", 
                "-Dfunction.permission.level=4", // 设置函数权限等级为最高
                "-Dop.permission.level=4",       // 设置OP权限等级为最高
                "@cnmforge.txt", "-nogui");
        } else if (mode == 3) {
            pb.command(selectedJavaPath,
                    "-Dfunction.permission.level=4", // 设置函数权限等级为最高
                    "-Dop.permission.level=4",       // 设置OP权限等级为最高
                    "-jar", "Core.jar");
        } else if (mode == 4) {
            pb.command(selectedJavaPath,
                    "-Dfunction.permission.level=4", // 设置函数权限等级为最高
                    "-Dop.permission.level=4","-Dpaper.disableChannelLimit=true",       // 设置OP权限等级为最高
                    "-jar", "Core.jar");
        } else if (mode == 5) {
            // 自定义启动模式
            List<String> command = new ArrayList<>();
            command.add(selectedJavaPath);
            
            // 添加JVM参数
            if (jvmArgs != null && !jvmArgs.isEmpty()) {
                String[] jvmArgsArray = jvmArgs.split("\\s+");
                command.addAll(Arrays.asList(jvmArgsArray));
            }
            
            // 添加必须的参数
            command.add("-jar");
            command.add("Core.jar");
            
            // 添加服务器参数
            if (serverArgs != null && !serverArgs.isEmpty()) {
                String[] serverArgsArray = serverArgs.split("\\s+");
                command.addAll(Arrays.asList(serverArgsArray));
            }
            
            pb.command(command);
        } else {
            printinf("错误: 无效的启动模式");
            return false;
        }

        printinf("正在启动服务器 " + server.name + "...");
        Process process = pb.start();


        // 读取进程输出
        StreamGobbler outputGobbler = new StreamGobbler(process.getInputStream(), System.out::println);
        StreamGobbler errorGobbler = new StreamGobbler(process.getErrorStream(), System.err::println);

        Thread thread1 = new Thread(outputGobbler);
        thread1.start();
        Thread thread2 = new Thread(errorGobbler);
        thread2.start();

        // 新增：将控制台输入转发到服务器进程
        Thread thread = new Thread(() -> {
            try {
                int c;
                OutputStream processInput = process.getOutputStream();
                while ((c = System.in.read()) != -1) {
                    processInput.write(c);
                    processInput.flush();
                }
            } catch (IOException e) {
                printinf("转发输入时出错: " + e.getMessage());
            }
        });
        thread.start();

        int exitCode = process.waitFor();
        printinf("服务器已关闭 (退出代码: " + exitCode + ")");
        thread.interrupt();
        thread1.interrupt();
        thread2.interrupt();
        return true;


    } catch (Exception e) {
        printinf("启动服务器时发生错误: " + e.getMessage());
    }
    return false;
}

    // 新增方法：删除服务器
    private static void deleteServer(Scanner scanner) {
        List<Server> servers = new ArrayList<>(loadServerList()); // 转换为可变列表
        if (servers.isEmpty()) {
            printinf("错误: 没有可用服务器");
            return;
        }
        
        // 显示服务器列表
        printinf("可用服务器：");
        for (int i=0; i<servers.size(); i++) {
            printinf((i+1) + ". " + servers.get(i).name);
        }
        
        printf("选择要删除的服务器编号: ");
        int choice = Integer.parseInt(scanner.nextLine()) - 1;
        if (choice < 0 || choice >= servers.size()) {
            printinf("错误: 无效的选择");
            return;
        }
        
        Server selectedServer = servers.get(choice);
        Path serverDir = Paths.get(selectedServer.corePath).getParent();
        
        // 确认删除操作
        printf("确定要删除服务器配置" + (Files.exists(serverDir) ? "和本地文件" : "") + "? (y/n): ");
        boolean confirm = scanner.nextLine().trim().toLowerCase().equals("y");
        
        if (confirm) {
            // 从配置中移除
            servers.remove(choice);
            saveServerConfigList(servers); // 保存更新后的列表
            
            // 删除本地文件
            if (Files.exists(serverDir)) {
                try {
                    Files.walk(serverDir)
                        .sorted(Comparator.reverseOrder())
                        .forEach(path -> {
                            try {
                                Files.delete(path);
                            } catch (IOException e) {
                                printinf("删除文件失败: " + path + " - " + e.getMessage());
                            }
                        });
                    printinf("服务器目录已删除: " + serverDir);
                } catch (IOException e) {
                    printinf("删除服务器目录时出错: " + e.getMessage());
                }
            } else {
                printinf("服务器目录不存在，仅删除了配置");
            }
            
            printinf("服务器 " + selectedServer.name + " 已删除");
        } else {
            printinf("操作已取消");
        }
    }
    
    // 新增方法：列出所有服务器
    private static void listServers() {
        List<Server> servers = loadServerList();
        if (servers.isEmpty()) {
            printinf("没有可用服务器");
            return;
        }
        
        printinf("服务器列表：");
        for (Server server : servers) {
            printinf("名称: " + server.name);
            printinf("  版本: " + server.version);
            printinf("  描述: " + server.description);
            printinf("  当前地图: " + (server.map != null ? server.map : "未设置"));
            printinf("  路径: " + server.corePath);
        }
    }
    
    // 新增方法：切换地图
    private static void changeMap(Scanner scanner) {
        List<Server> servers = loadServerList();
        if (servers.isEmpty()) {
            printinf("错误: 没有可用服务器");
            return;
        }
        
        // 选择服务器
        printinf("可用服务器：");
        for (int i=0; i<servers.size(); i++) {
            printinf((i+1) + ". " + servers.get(i).name);
        }
        
        printf("选择服务器编号: ");
        int serverChoice = Integer.parseInt(scanner.nextLine()) - 1;
        if (serverChoice < 0 || serverChoice >= servers.size()) {
            printinf("错误: 无效的选择");
            return;
        }
        
        Server selectedServer = servers.get(serverChoice);
        Path serverDir = Paths.get(selectedServer.corePath).getParent();
        
        // 选择备份
        printf("是否备份当前状态? (y/n): ");
        boolean backup = scanner.nextLine().trim().toLowerCase().equals("y");
        
        if (backup) {
            // 创建备份目录
            String timestamp = new java.text.SimpleDateFormat("yyyyMMdd_HHmmss").format(new java.util.Date());
            Path backupDir = Paths.get(BACKUPS_DIR, timestamp, selectedServer.name);
            try {
                Files.createDirectories(backupDir);
                // 复制服务器目录内容到备份
                Files.walk(serverDir).forEach(source -> {
                    try {
                        Path target = backupDir.resolve(serverDir.relativize(source));
                        if (Files.isDirectory(source)) {
                            Files.createDirectories(target);
                        } else {
                            Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
                        }
                    } catch (IOException e) {
                        printinf("备份时出错: " + e.getMessage());
                    }
                });
                printinf("备份已创建于: " + backupDir);
            } catch (IOException e) {
                printinf("创建备份目录时出错: " + e.getMessage());
                return;
            }
        }

// 选择地图文件
        try {
            Path mapsPath = Paths.get(MAPS_DIR);
            if (!Files.exists(mapsPath)) {
                Files.createDirectories(mapsPath);
            }

            List<String> maps = new ArrayList<>();
            List<String> mapVersions = new ArrayList<>(); // 存储版本信息

            Files.list(mapsPath).forEach(path -> {
                if (path.toString().endsWith(".zip") || Files.isDirectory(path)) {
                    String mapName = path.getFileName().toString();
                    maps.add(mapName);

                    // 检查是否存在version.txt并读取内容
                    String versionInfo = "";
                    try {
                        Path versionFile;
                        if (path.toString().endsWith(".zip")) {
                            // 如果是zip文件，先检查zip内是否有version.txt
                            versionInfo = getVersionFromZip(path);
                        } else {
                            // 如果是目录，直接检查目录下的version.txt
                            versionFile = path.resolve("version.txt");
                            if (Files.exists(versionFile)) {
                                versionInfo = Files.readString(versionFile).trim();
                            }
                        }
                    } catch (IOException e) {
                        printinf("读取版本信息失败: " + e.getMessage());
                    }

                    mapVersions.add(versionInfo);
                }
            });

            if (maps.isEmpty()) {
                printinf("错误: 没有可用地图文件");
                return;
            }

            printinf("可用地图：");
            for (int i = 0; i < maps.size(); i++) {
                String versionDisplay = mapVersions.get(i).isEmpty() ? "" : " (" + mapVersions.get(i) + ")";
                printinf((i + 1) + ". " + maps.get(i) + versionDisplay);
            }

            printf("选择地图编号: ");
            int mapChoice = Integer.parseInt(scanner.nextLine()) - 1;
            if (mapChoice < 0 || mapChoice >= maps.size()) {
                printinf("错误: 无效的选择");
                return;
            }

            String selectedMap = maps.get(mapChoice);
            Path mapSource = Paths.get(MAPS_DIR, selectedMap);

            // 处理zip文件
            if (selectedMap.endsWith(".zip")) {
                // 解压到服务器目录
                extractZip(mapSource, serverDir);
                selectedServer.map = selectedMap.substring(0, selectedMap.length() - 4); // 去除.zip扩展名
            } else {
                // 直接复制目录
                Path targetDir = serverDir.resolve("world");
                if (Files.exists(targetDir)) {
                    // 删除现有world目录
                    Files.walk(targetDir).sorted(Comparator.reverseOrder())
                        .forEach(path -> {
                            try { Files.delete(path); }
                            catch (IOException e) {}
                        });
                }
                Files.createDirectories(targetDir);

                Files.walk(mapSource).forEach(source -> {
                    try {
                        Path target = targetDir.resolve(mapSource.relativize(source));
                        if (Files.isDirectory(source)) {
                            Files.createDirectories(target);
                        } else {
                            Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
                        }
                    } catch (IOException e) {
                        printinf("复制地图时出错: " + e.getMessage());
                    }
                });
                selectedServer.map = selectedMap;
            }

            saveServerConfig(selectedServer);
            printinf("地图已更新为: " + selectedServer.map);

        } catch (IOException e) {
            printinf("切换地图时出错: " + e.getMessage());
        }
    }

    private static void printf(String s) {
        System.out.print(s);
        System.out.println();
    }
    private static void printinf(String s) {
        System.out.println(s);
        System.out.println();
    }

    private static String getVersionFromZip(Path zipPath) throws IOException {
        try (ZipFile zipFile = new ZipFile(zipPath.toFile())) {
            // 在zip文件中查找version.txt
            ZipEntry versionEntry = zipFile.getEntry("version.txt");
            if (versionEntry != null && !versionEntry.isDirectory()) {
                try (InputStream is = zipFile.getInputStream(versionEntry);
                     BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
                    return reader.readLine().trim(); // 读取第一行作为版本信息
                }
            }

            // 如果根目录没有，检查world目录下的version.txt（常见于地图zip）
            ZipEntry worldVersionEntry = zipFile.getEntry("world/version.txt");
            if (worldVersionEntry != null && !worldVersionEntry.isDirectory()) {
                try (InputStream is = zipFile.getInputStream(worldVersionEntry);
                     BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
                    return reader.readLine().trim();
                }
            }
        }
        return "";
    }
    // 新增方法：解压zip文件
    private static void extractZip(Path zipPath, Path targetDir) throws IOException {
        try (ZipInputStream zis = new ZipInputStream(Files.newInputStream(zipPath))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                Path entryPath = targetDir.resolve(entry.getName());
                if (entry.isDirectory()) {
                    Files.createDirectories(entryPath);
                } else {
                    Files.createDirectories(entryPath.getParent());
                    Files.copy(zis, entryPath, StandardCopyOption.REPLACE_EXISTING);
                }
                zis.closeEntry();
            }
        }
    }

    // 查找可用的Java路径
    private static List<String> findAvailableJavaPaths() {
        List<String> javaPaths = new ArrayList<>();
        
        // Windows系统查找Java路径
        if (System.getProperty("os.name").toLowerCase().contains("win")) {
            // 查找常见的Java安装路径
            String[] commonPaths = {
                "C:\\Program Files\\Java",
                "C:\\Program Files (x86)\\Java",
                System.getProperty("java.home")
            };
            
            for (String basePath : commonPaths) {
                try {
                    Path javaBasePath = Paths.get(basePath);
                    if (Files.exists(javaBasePath)) {
                        Files.list(javaBasePath)
                            .filter(Files::isDirectory)
                            .forEach(dir -> {
                                Path javaExe = dir.resolve("bin").resolve("java.exe");
                                if (Files.exists(javaExe)) {
                                    javaPaths.add(javaExe.toAbsolutePath().toString());
                                }
                            });
                    }
                } catch (IOException e) {
                    // 忽略错误，继续查找其他路径
                }
            }
        } else {
            // Unix-like系统查找Java路径
            String[] commonPaths = {
                "/usr/lib/jvm",
                "/usr/java",
                System.getProperty("java.home")
            };
            
            for (String basePath : commonPaths) {
                try {
                    Path javaBasePath = Paths.get(basePath);
                    if (Files.exists(javaBasePath)) {
                        Files.list(javaBasePath)
                            .filter(Files::isDirectory)
                            .forEach(dir -> {
                                Path javaBin = dir.resolve("bin").resolve("java");
                                if (Files.exists(javaBin)) {
                                    javaPaths.add(javaBin.toAbsolutePath().toString());
                                }
                            });
                    }
                } catch (IOException e) {
                    // 忽略错误，继续查找其他路径
                }
            }
            
            // 同时检查当前目录下的java目录
            try {
                Path currentJavaDir = Paths.get("java");
                if (Files.exists(currentJavaDir) && Files.isDirectory(currentJavaDir)) {
                    Files.list(currentJavaDir)
                        .filter(Files::isDirectory)
                        .forEach(dir -> {
                            Path javaBin = dir.resolve("bin").resolve("java");
                            if (Files.exists(javaBin)) {
                                javaPaths.add(javaBin.toAbsolutePath().toString());
                            }
                        });
                }
            } catch (IOException e) {
                // 忽略错误
            }
        }
        
        return javaPaths;
    }
    
    // 辅助方法：打印带参数的消息
    private static void printf(String format, Object... args) {
        System.out.println();
        System.out.print(String.format(format, args));
    }
}











































