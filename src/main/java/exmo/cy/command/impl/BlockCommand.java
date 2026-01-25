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
    description = "屏蔽指定服务器的控制台输出"
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
                return addBlock(args);
            case "remove":
            case "del":
                return removeBlock(args);
            case "list":
            case "ls":
                return listBlocks();
            default:
                Logger.println("未知的操作: " + action);
                showUsage();
                return true;
        }
    }
    
    private void showUsage() {
        Logger.println("屏蔽服务器控制台命令用法:");
        Logger.println("  block add <服务器名称>     - 屏蔽指定服务器的控制台输出");
        Logger.println("  block remove <服务器名称>  - 取消屏蔽指定服务器的控制台输出");
        Logger.println("  block list               - 列出所有被屏蔽的服务器");
        Logger.println("  block ls                 - 列出所有被屏蔽的服务器");
        
        // 显示当前被屏蔽的服务器
        listBlocks();
    }
    
    private boolean addBlock(String[] args) {
        if (args.length < 2) {
            Logger.println("错误: 请指定要屏蔽的服务器名称");
            Logger.println("用法: block add <服务器名称>");
            return true;
        }
        
        String serverName = args[1];
        
        if (serverService.blockServerOutput(serverName)) {
            Logger.println("服务器 " + serverName + " 的控制台输出已被屏蔽");
        } else {
            Logger.println("服务器 " + serverName + " 已经在屏蔽列表中");
        }
        
        return true;
    }
    
    private boolean removeBlock(String[] args) {
        if (args.length < 2) {
            Logger.println("错误: 请指定要取消屏蔽的服务器名称");
            Logger.println("用法: block remove <服务器名称>");
            return true;
        }
        
        String serverName = args[1];
        
        if (serverService.unblockServerOutput(serverName)) {
            Logger.println("服务器 " + serverName + " 的控制台输出屏蔽已取消");
        } else {
            Logger.println("服务器 " + serverName + " 不在屏蔽列表中");
        }
        
        return true;
    }
    
    private boolean listBlocks() {
        List<String> blockedServers = serverService.getBlockedServers();
        
        if (blockedServers.isEmpty()) {
            Logger.println("当前没有被屏蔽的服务器");
        } else {
            Logger.println("被屏蔽的服务器列表:");
            for (String serverName : blockedServers) {
                Logger.println("  - " + serverName);
            }
        }
        
        return true;
    }
    
    @Override
    public String getDescription() {
        return "屏蔽指定服务器的控制台输出";
    }
    
    @Override
    public CommandAnnotation getAnnotation() {
        return getClass().getAnnotation(CommandAnnotation.class);
    }
}