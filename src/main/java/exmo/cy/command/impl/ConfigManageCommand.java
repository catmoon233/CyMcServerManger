package exmo.cy.command.impl;

import exmo.cy.command.AnnotatedCommand;
import exmo.cy.command.CommandAnnotation;
import exmo.cy.model.Server;
import exmo.cy.service.ServerService;
import exmo.cy.util.Logger;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Scanner;

@CommandAnnotation(
    name = "configmanage",
    aliases = {"cfg", "conf"},
    description = "管理服务器配置"
)
public class ConfigManageCommand extends AnnotatedCommand {
    private final ServerService serverService;
    private final Scanner scanner = new Scanner(System.in);
    
    public ConfigManageCommand(ServerService serverService) {
        this.serverService = serverService;
    }
    
    @Override
    public boolean execute(String[] args) {
        if (args.length == 0) {
            showUsage();
            return true;
        }
        
        String operation = args[0].toLowerCase();
        
        switch (operation) {
            case "view":
            case "show":
                return viewConfig(args);
            case "set":
                return setConfig(args);
            case "reset":
                return resetConfig(args);
            case "list":
                return listConfigs();
            default:
                Logger.println("未知的配置操作: " + operation);
                showUsage();
                return true;
        }
    }
    
    private void showUsage() {
        Logger.println("服务器配置管理命令用法:");
        Logger.println("  configmanage view <服务器名>                    - 查看服务器配置");
        Logger.println("  configmanage set <服务器名> <参数> <值>           - 设置服务器参数");
        Logger.println("  configmanage reset <服务器名> <参数>              - 重置服务器参数");
        Logger.println("  configmanage list                              - 列出所有服务器配置");
        Logger.println("可用参数:");
        Logger.println("  jvmargs - JVM启动参数");
        Logger.println("  serverargs - 服务器启动参数");
        Logger.println("  version - 服务器版本");
        Logger.println("  description - 服务器描述");
        Logger.println("  示例:");
        Logger.println("    configmanage view myserver");
        Logger.println("    configmanage set myserver jvmargs \"-Xms1G -Xmx4G\"");
        Logger.println("    configmanage set myserver version \"1.19.2\"");
        Logger.println("    configmanage reset myserver jvmargs");
    }
    
    private boolean viewConfig(String[] args) {

        try {


        if (args.length < 2) {
            Logger.println("错误: 请指定服务器名称");
            return true;
        }
        
        String serverName = args[1];
        Optional<Server> serverOpt = serverService.getConfigManager().findServerByName(serverName);
        
        if (!serverOpt.isPresent()) {
            Logger.println("错误: 服务器 '" + serverName + "' 不存在");
            return true;
        }
        
        Server server = serverOpt.get();
        
        Logger.println("服务器 '" + serverName + "' 配置:");
        Logger.println("  名称: " + server.getName());
        Logger.println("  版本: " + server.getVersion());
        Logger.println("  描述: " + server.getDescription());
        Logger.println("  核心路径: " + server.getCorePath());
        Logger.println("  默认JVM参数: " + (server.getDefaultJvmArgs() != null ? server.getDefaultJvmArgs() : "无"));
        Logger.println("  默认服务器参数: " + (server.getDefaultServerArgs() != null ? server.getDefaultServerArgs() : "无"));
        Logger.println("  所属群组: " + (server.getGroup() != null ? server.getGroup() : "无"));
        Logger.println("  地图: " + server.getMap());
        Logger.println("  是否模组包: " + server.isModpack());
        
        return true;
        }catch (Exception e){
            Logger.error("错误: ", e);
            return true;
        }
    }
    
    private boolean setConfig(String[] args) {
        if (args.length < 4) {
            Logger.println("错误: 请指定服务器名称、参数和值");
            return true;
        }

        
        String serverName = args[1];
        String param = args[2].toLowerCase();
        String value = String.join(" ", Arrays.copyOfRange(args, 3, args.length));

        try {


            Optional<Server> serverOpt = serverService.getConfigManager().findServerByName(serverName);

        if (!serverOpt.isPresent()) {
            Logger.println("错误: 服务器 '" + serverName + "' 不存在");
            return true;
        }
        
        Server server = serverOpt.get();
        
        try {
            switch (param) {
                case "jvmargs":
                    server.setDefaultJvmArgs(value);
                    Logger.println("已设置服务器 '" + serverName + "' 的JVM参数为: " + value);
                    break;
                case "serverargs":
                    server.setDefaultServerArgs(value);
                    Logger.println("已设置服务器 '" + serverName + "' 的服务器参数为: " + value);
                    break;
                case "version":
                    server.setVersion(value);
                    Logger.println("已设置服务器 '" + serverName + "' 的版本为: " + value);
                    break;
                case "description":
                    server.setDescription(value);
                    Logger.println("已设置服务器 '" + serverName + "' 的描述为: " + value);
                    break;
                default:
                    Logger.println("错误: 不支持的参数 '" + param + "'");
                    Logger.println("支持的参数: jvmargs, serverargs, version, description");
                    return true;
            }
            
            // 保存更改
            serverService.getConfigManager().saveServer(server);
            Logger.println("服务器配置已保存");
        } catch (Exception e) {
            Logger.println("设置配置失败: " + e.getMessage());
        }
        
        return true;
        }catch (Exception e){
            Logger.error("错误: ", e);
            return true;
        }
    }
    
    private boolean resetConfig(String[] args) {
        if (args.length < 3) {
            Logger.println("错误: 请指定服务器名称和参数");
            return true;
        }
        
        String serverName = args[1];
        String param = args[2].toLowerCase();
        try {

        Optional<Server> serverOpt = serverService.getConfigManager().findServerByName(serverName);
        
        if (!serverOpt.isPresent()) {
            Logger.println("错误: 服务器 '" + serverName + "' 不存在");
            return true;
        }
        
        Server server = serverOpt.get();
        

            switch (param) {
                case "jvmargs":
                    server.setDefaultJvmArgs(null);
                    Logger.println("已重置服务器 '" + serverName + "' 的JVM参数");
                    break;
                case "serverargs":
                    server.setDefaultServerArgs(null);
                    Logger.println("已重置服务器 '" + serverName + "' 的服务器参数");
                    break;
                default:
                    Logger.println("错误: 不支持的参数 '" + param + "'");
                    Logger.println("支持的参数: jvmargs, serverargs");
                    return true;
            }
            
            // 保存更改
            serverService.getConfigManager().saveServer(server);
            Logger.println("服务器配置已保存");
        } catch (Exception e) {
            Logger.println("重置配置失败: " + e.getMessage());
        }
        
        return true;
    }
    
    private boolean listConfigs() {
        try {
            var servers = serverService.getConfigManager().loadServers();
            
            if (servers.isEmpty()) {
                Logger.println("没有配置任何服务器");
                return true;
            }
            
            Logger.println("服务器配置列表:");
            for (var server : servers) {
                String jvmArgs = server.getDefaultJvmArgs() != null ? server.getDefaultJvmArgs() : "无";
                String serverArgs = server.getDefaultServerArgs() != null ? server.getDefaultServerArgs() : "无";
                Logger.println("  - " + server.getName() + " (版本: " + server.getVersion() + 
                             ", JVM参数: " + jvmArgs + ", 服务器参数: " + serverArgs + ")");
            }
        } catch (Exception e) {
            Logger.println("获取服务器配置列表失败: " + e.getMessage());
        }
        
        return true;
    }
    
    @Override
    public String getDescription() {
        return "管理服务器配置";
    }
    
    @Override
    public CommandAnnotation getAnnotation() {
        return getClass().getAnnotation(CommandAnnotation.class);
    }
}