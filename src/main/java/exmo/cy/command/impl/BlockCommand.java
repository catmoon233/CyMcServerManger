package exmo.cy.command.impl;

import exmo.cy.command.AnnotatedCommand;
import exmo.cy.command.CommandAnnotation;
import exmo.cy.service.ServerService;
import exmo.cy.util.Logger;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;

@CommandAnnotation(
    name = "block",
    aliases = {"bl"},
    description = "屏蔽/取消屏蔽指定服务器的控制台输出"
)
public class BlockCommand extends AnnotatedCommand {
    private final ServerService serverService;
    private final Scanner scanner = new Scanner(System.in);
    
    // 用于存储临时屏蔽前的原始屏蔽状态
    private static Set<String> originalBlockedServers = null;
    private static boolean isTemporarilyBlockingAll = false;
    
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
            case "all":
            case "temp-all":
                return temporaryBlockAll(args);
            case "restore":
            case "unblock-all":
                return restoreOriginalBlocks(args);
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
        Logger.println("  block all                - 临时屏蔽所有服务器（保留原屏蔽状态）");
        Logger.println("  block restore            - 恢复原始屏蔽状态");
        Logger.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        Logger.println("说明:");
        Logger.println("  - <服务器> 可以是服务器序号(1,2,3) 或服务器名称");
        Logger.println("  - 屏蔽只影响Web控制台显示，服务器日志仍会保存到日志文件");
        Logger.println("  - 'block all' 会临时屏蔽所有服务器，'block restore' 恢复原状");
        Logger.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        Logger.println("");
        
        // 显示当前被屏蔽的服务器
        listBlocks();
        
        // 显示临时屏蔽状态
        if (isTemporarilyBlockingAll) {
            Logger.println("⚠️  注意: 当前处于临时全屏蔽模式");
            Logger.println("   使用 'block restore' 恢复原始屏蔽状态");
        }
    }

    private boolean addBlock(String[] args) {
        if (args.length < 2) {
            Logger.println("❌ 错误: 请指定要屏蔽的服务器（可以是序号或名称）");
            Logger.println("用法: block add <服务器序号或名称>");
            Logger.println("");
            
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
                Logger.println("❌ 错误: 服务器序号超出范围，有效范围是 1 到 " + allServers.size());
                showAvailableServers();
                return true;
            }
            
            String serverName = allServers.get(serverIndex);
            if (serverService.blockServerOutput(serverName)) {
                Logger.println("✓ 已屏蔽服务器 " + serverName + " (#" + (serverIndex + 1) + ") 的控制台输出");
                Logger.println("  服务器日志仍会保存到日志文件中");
            } else {
                Logger.println("ℹ 服务器 " + serverName + " (#" + (serverIndex + 1) + ") 已在屏蔽列表中");
            }
        } catch (NumberFormatException e) {
            // 如果不是数字，按名称处理
            String serverName = serverIdentifier;
            
            if (serverService.blockServerOutput(serverName)) {
                Logger.println("✓ 已屏蔽服务器 " + serverName + " 的控制台输出");
                Logger.println("  服务器日志仍会保存到日志文件中");
            } else {
                Logger.println("ℹ 服务器 " + serverName + " 已在屏蔽列表中");
            }
        }
        
        return true;
    }

    private boolean removeBlock(String[] args) {
        if (args.length < 2) {
            Logger.println("❌ 错误: 请指定要取消屏蔽的服务器（可以是序号或名称）");
            Logger.println("用法: block remove <服务器序号或名称>");
            Logger.println("");
            
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
                Logger.println("❌ 错误: 服务器序号超出范围，有效范围是 1 到 " + blockedServers.size());
                listBlocks();
                return true;
            }
            
            String serverName = blockedServers.get(serverIndex);
            if (serverService.unblockServerOutput(serverName)) {
                Logger.println("✓ 已取消屏蔽服务器 " + serverName + " (#" + (serverIndex + 1) + ")");
                Logger.println("  服务器控制台输出现在会显示在Web界面中");
            } else {
                Logger.println("ℹ 服务器 " + serverName + " (#" + (serverIndex + 1) + ") 不在屏蔽列表中");
            }
        } catch (NumberFormatException e) {
            // 如果不是数字，按名称处理
            String serverName = serverIdentifier;
            
            if (serverService.unblockServerOutput(serverName)) {
                Logger.println("✓ 已取消屏蔽服务器 " + serverName);
                Logger.println("  服务器控制台输出现在会显示在Web界面中");
            } else {
                Logger.println("ℹ 服务器 " + serverName + " 不在屏蔽列表中");
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
    
    /**
     * 临时屏蔽所有服务器，保存原始屏蔽状态
     */
    private boolean temporaryBlockAll(String[] args) {
        if (isTemporarilyBlockingAll) {
            Logger.println("⚠️  警告: 已经处于临时全屏蔽模式");
            Logger.println("   使用 'block restore' 恢复原始状态");
            return true;
        }
        
        try {
            // 获取所有可用的服务器
            List<String> allServers = getAllServerNames();
            if (allServers.isEmpty()) {
                Logger.println("❌ 错误: 没有可用的服务器");
                return true;
            }
            
            // 保存当前的屏蔽状态
            originalBlockedServers = new HashSet<>(serverService.getBlockedServers());
            isTemporarilyBlockingAll = true;
            
            // 屏蔽所有服务器
            int blockedCount = 0;
            for (String serverName : allServers) {
                if (serverService.blockServerOutput(serverName)) {
                    blockedCount++;
                }
            }
            
            Logger.println("✅ 已临时屏蔽所有 " + allServers.size() + " 个服务器");
            Logger.println("   原始屏蔽状态已保存 (" + originalBlockedServers.size() + " 个服务器)");
            Logger.println("   使用 'block restore' 恢复原始屏蔽状态");
            
        } catch (Exception e) {
            Logger.error("临时屏蔽所有服务器失败: " + e.getMessage(), e);
            return true;
        }
        
        return true;
    }
    
    /**
     * 恢复原始屏蔽状态
     */
    private boolean restoreOriginalBlocks(String[] args) {
        if (!isTemporarilyBlockingAll) {
            Logger.println("❌ 错误: 当前没有处于临时全屏蔽模式");
            Logger.println("   使用 'block all' 先启用临时全屏蔽");
            return true;
        }
        
        try {
            // 获取所有可用的服务器
            List<String> allServers = getAllServerNames();
            
            // 首先取消屏蔽所有服务器
            for (String serverName : allServers) {
                serverService.unblockServerOutput(serverName);
            }
            
            // 然后恢复原始的屏蔽状态
            if (originalBlockedServers != null) {
                for (String serverName : originalBlockedServers) {
                    serverService.blockServerOutput(serverName);
                }
            }
            
            // 重置临时状态
            originalBlockedServers = null;
            isTemporarilyBlockingAll = false;
            
            Logger.println("✅ 已恢复原始屏蔽状态");
            if (originalBlockedServers != null) {
                Logger.println("   原始屏蔽的 " + originalBlockedServers.size() + " 个服务器已重新屏蔽");
            }
            
        } catch (Exception e) {
            Logger.error("恢复原始屏蔽状态失败: " + e.getMessage(), e);
            // 即使失败也要重置状态，避免死锁
            originalBlockedServers = null;
            isTemporarilyBlockingAll = false;
            return true;
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