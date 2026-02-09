package exmo.cy.command.impl;

import exmo.cy.command.AnnotatedCommand;
import exmo.cy.command.CommandAnnotation;
import exmo.cy.service.ServerService;
import exmo.cy.util.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

@CommandAnnotation(
    name = "block",
    aliases = {"bl"},
    description = "屏蔽/取消屏蔽指定服务器的控制台输出"
)
public class BlockCommand extends AnnotatedCommand {
    private final ServerService serverService;
    private final Scanner scanner = new Scanner(System.in);
    
    public BlockCommand(ServerService serverService) {
        this.serverService = serverService;
    }
    
    @Override
    public boolean execute(String[] args) {
        if (args.length == 0) {
            // 显示使用说明和当前屏蔽列表
            showUsage();
            return true;
        }
        
        String action = args[0].toLowerCase();
        
        switch (action) {
            case "add":
            case "on":
                return addBlock(args);
            case "remove":
            case "del":
            case "off":
                return removeBlock(args);
            case "list":
            case "ls":
                return listBlocks();
            case "toggle":
            case "switch":
                return toggleBlock(args);
            default:
                Logger.println("❌ 未知的操作: " + action);
                showUsage();
                return true;
        }
    }
    
    private void showUsage() {
        Logger.println("屏蔽服务器控制台输出命令用法:");
        Logger.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        Logger.println("  block add <服务器>        - 屏蔽服务器的控制台输出");
        Logger.println("  block remove <服务器>     - 取消屏蔽服务器的控制台输出");
        Logger.println("  block toggle <服务器>     - 切换服务器的屏蔽状态");
        Logger.println("  block list               - 显示所有被屏蔽的服务器");
        Logger.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        Logger.println("说明:");
        Logger.println("  - <服务器> 可以是服务器序号(1,2,3) 或服务器名称");
        Logger.println("  - 屏蔽只影响Web控制台显示，服务器日志仍会保存到日志文件");
        Logger.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        Logger.println("");
        
        // 显示当前被屏蔽的服务器
        listBlocks();
    }

    private boolean addBlock(String[] args) {
        if (args.length < 2) {
            Logger.println("错误: 请指定要屏蔽的服务器（可以是序号或名称）");
            Logger.println("用法: block add <服务器序号或名称>");

            // 显示所有服务器供参考
            showAvailableServers();
            return true;
        }

        String serverIdentifier = args[1];

        // 尝试解析为序号
        try {
            int serverIndex = Integer.parseInt(serverIdentifier) - 1;
            List<String> allServers = getAllServerNames();

            if (serverIndex < 0 || serverIndex >= allServers.size()) {
                Logger.println("错误: 服务器序号超出范围，有效范围是 1 到 " + allServers.size());
                showAvailableServers();
                return true;
            }

            String serverName = allServers.get(serverIndex);
            if (serverService.blockServerOutput(serverName)) {
                Logger.println("服务器 " + serverName + " (#" + (serverIndex + 1) + ") 的控制台输出已被屏蔽");
            } else {
                Logger.println("服务器 " + serverName + " (#" + (serverIndex + 1) + ") 已经在屏蔽列表中");
            }
        } catch (NumberFormatException e) {
            // 如果不是数字，按名称处理
            String serverName = serverIdentifier;

            if (serverService.blockServerOutput(serverName)) {
                Logger.println("服务器 " + serverName + " 的控制台输出已被屏蔽");
            } else {
                Logger.println("服务器 " + serverName + " 已经在屏蔽列表中");
            }
        }

        return true;
    }

    private boolean removeBlock(String[] args) {
        if (args.length < 2) {
            Logger.println("错误: 请指定要取消屏蔽的服务器（可以是序号或名称）");
            Logger.println("用法: block remove <服务器序号或名称>");

            // 显示被屏蔽的服务器供参考
            listBlocks();
            return true;
        }

        String serverIdentifier = args[1];

        // 尝试解析为序号
        try {
            int serverIndex = Integer.parseInt(serverIdentifier) - 1;
            List<String> blockedServers = serverService.getBlockedServers();

            if (serverIndex < 0 || serverIndex >= blockedServers.size()) {
                Logger.println("错误: 服务器序号超出范围，有效范围是 1 到 " + blockedServers.size());
                listBlocks();
                return true;
            }

            String serverName = blockedServers.get(serverIndex);
            if (serverService.unblockServerOutput(serverName)) {
                Logger.println("服务器 " + serverName + " (#" + (serverIndex + 1) + ") 的控制台输出屏蔽已取消");
            } else {
                Logger.println("服务器 " + serverName + " (#" + (serverIndex + 1) + ") 不在屏蔽列表中");
            }
        } catch (NumberFormatException e) {
            // 如果不是数字，按名称处理
            String serverName = serverIdentifier;

            if (serverService.unblockServerOutput(serverName)) {
                Logger.println("服务器 " + serverName + " 的控制台输出屏蔽已取消");
            } else {
                Logger.println("服务器 " + serverName + " 不在屏蔽列表中");
            }
        }

        return true;
    }
    private boolean toggleBlock(String[] args) {
        if (args.length < 2) {
            Logger.println("❌ 错误: 请指定要切换屏蔽状态的服务器");
            Logger.println("用法: block toggle <服务器序号或名称>");
            return true;
        }
        
        String serverIdentifier = args[1];
        String serverName = serverIdentifier;
        
        // 尝试解析为序号
        try {
            int serverIndex = Integer.parseInt(serverIdentifier) - 1;
            List<String> allServers = getAllServerNames();
            
            if (serverIndex < 0 || serverIndex >= allServers.size()) {
                Logger.println("❌ 错误: 服务器序号超出范围，有效范围是 1 到 " + allServers.size());
                return true;
            }
            
            serverName = allServers.get(serverIndex);
        } catch (NumberFormatException e) {
            // 如果不是数字，使用原始输入作为名称
        }
        
        boolean isCurrentlyBlocked = serverService.isServerBlocked(serverName);
        
        if (isCurrentlyBlocked) {
            serverService.unblockServerOutput(serverName);
            Logger.println("✓ 已取消屏蔽服务器 " + serverName);
        } else {
            serverService.blockServerOutput(serverName);
            Logger.println("✓ 已屏蔽服务器 " + serverName);
        }
        
        return true;
    }
    

    
    private void showAvailableServers() {
        List<String> allServers = getAllServerNames();
        if (allServers.isEmpty()) {
            Logger.println("当前没有可用的服务器");
        } else {
            Logger.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            Logger.println("可用的服务器列表:");
            Logger.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            for (int i = 0; i < allServers.size(); i++) {
                Logger.println("  [" + (i + 1) + "] " + allServers.get(i));
            }
            Logger.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        }
    }
    
    private List<String> getAllServerNames() {
        try {
            List<String> serverNames = new ArrayList<>();
            List<exmo.cy.model.Server> servers = serverService.getConfigManager().loadServers();
            for (exmo.cy.model.Server server : servers) {
                serverNames.add(server.getName());
            }
            return serverNames;
        } catch (Exception e) {
            Logger.error("获取服务器列表失败: " + e.getMessage());
            return new ArrayList<>();
        }
    }
    
    @Override
    public String getDescription() {
        return "屏蔽/取消屏蔽指定服务器的控制台输出";
    }
    
    @Override
    public CommandAnnotation getAnnotation() {
        return getClass().getAnnotation(CommandAnnotation.class);
    }

    

    


    private boolean listBlocks() {
        List<String> blockedServers = serverService.getBlockedServers();

        Logger.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        Logger.println("已屏蔽的服务器列表:");
        Logger.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");

        if (blockedServers.isEmpty()) {
            Logger.println("当前没有被屏蔽的服务器");
        } else {
            for (int i = 0; i < blockedServers.size(); i++) {
                Logger.println("  [" + (i + 1) + "] " + blockedServers.get(i) + " 🔕");
            }
        }

        Logger.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");

        return true;
    }
    

}