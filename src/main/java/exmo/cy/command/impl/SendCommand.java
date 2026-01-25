package exmo.cy.command.impl;

import exmo.cy.command.AnnotatedCommand;
import exmo.cy.command.CommandAnnotation;
import exmo.cy.model.ServerInstance;
import exmo.cy.service.ServerService;
import exmo.cy.util.ConsoleColor;
import exmo.cy.util.Logger;

import java.util.Map;

@CommandAnnotation(
    name = "send",
    aliases = {"cmd", "command", "send-command"},
    description = "向指定服务器或全部服务器发送MC指令"
)
public class SendCommand extends AnnotatedCommand {
    private final ServerService serverService;
    
    public SendCommand(ServerService serverService) {
        this.serverService = serverService;
    }
    
    @Override
    public boolean execute(String[] args) {
        if (args.length == 0) {
            showUsage();
            return true;
        }
        
        String target = args[0]; // 服务器名称或"all"
        
        if (args.length < 2 && !"all".equalsIgnoreCase(target)) {
            showUsage();
            return true;
        }
        
        String command;
        if ("all".equalsIgnoreCase(target)) {
            if (args.length < 2) {
                showUsage();
                return true;
            }
            // 将剩余参数组合成完整命令
            StringBuilder cmdBuilder = new StringBuilder();
            for (int i = 1; i < args.length; i++) {
                if (i > 1) cmdBuilder.append(" ");
                cmdBuilder.append(args[i]);
            }
            command = cmdBuilder.toString();
            return sendToAllServers(command);
        } else {
            // 将剩余参数组合成完整命令
            StringBuilder cmdBuilder = new StringBuilder();
            for (int i = 1; i < args.length; i++) {
                if (i > 1) cmdBuilder.append(" ");
                cmdBuilder.append(args[i]);
            }
            command = cmdBuilder.toString();
            return sendToSpecificServer(target, command);
        }
    }
    
    private boolean sendToAllServers(String command) {
        Map<String, ServerInstance> activeServers = serverService.getActiveServers();
        
        if (activeServers.isEmpty()) {
            System.out.println(ConsoleColor.colorize(ConsoleColor.YELLOW, "没有运行中的服务器"));
            return true;
        }
        
        System.out.println(ConsoleColor.colorize(ConsoleColor.BRIGHT_GREEN, 
            "正在向所有运行中的服务器发送命令: " + command));
        
        int successCount = 0;
        for (Map.Entry<String, ServerInstance> entry : activeServers.entrySet()) {
            String serverName = entry.getKey();
            try {
                serverService.sendCommand(serverName, command);
                System.out.println(ConsoleColor.colorize(ConsoleColor.GREEN, 
                    "✓ 命令已发送到服务器: " + serverName));
                successCount++;
            } catch (Exception e) {
                System.out.println(ConsoleColor.colorize(ConsoleColor.RED, 
                    "✗ 发送到服务器 " + serverName + " 失败: " + e.getMessage()));
            }
        }
        
        System.out.println(ConsoleColor.colorize(ConsoleColor.BRIGHT_GREEN, 
            "成功发送到 " + successCount + "/" + activeServers.size() + " 个服务器"));
        
        return true;
    }
    
    private boolean sendToSpecificServer(String serverName, String command) {
        // 检查服务器是否正在运行
        Map<String, ServerInstance> activeServers = serverService.getActiveServers();
        
        if (!activeServers.containsKey(serverName)) {
            System.out.println(ConsoleColor.colorize(ConsoleColor.RED, 
                "服务器 " + serverName + " 当前未运行"));
            
            // 显示可用的运行中服务器列表
            if (!activeServers.isEmpty()) {
                System.out.println(ConsoleColor.colorize(ConsoleColor.YELLOW, "运行中的服务器:"));
                for (String name : activeServers.keySet()) {
                    System.out.println("  - " + name);
                }
            } else {
                System.out.println(ConsoleColor.colorize(ConsoleColor.YELLOW, "当前没有运行中的服务器"));
            }
            return true;
        }
        
        try {
            serverService.sendCommand(serverName, command);
            System.out.println(ConsoleColor.colorize(ConsoleColor.GREEN, 
                "命令已发送到服务器 " + serverName + ": " + command));
        } catch (Exception e) {
            System.out.println(ConsoleColor.colorize(ConsoleColor.RED, 
                "发送命令失败: " + e.getMessage()));
        }
        
        return true;
    }
    
    private void showUsage() {
        System.out.println(ConsoleColor.colorize(ConsoleColor.BRIGHT_YELLOW, "发送命令到服务器用法:"));
        System.out.println(ConsoleColor.colorize(ConsoleColor.CYAN, "  send <服务器名称> <命令>     - 向指定服务器发送命令"));
        System.out.println(ConsoleColor.colorize(ConsoleColor.CYAN, "  send all <命令>              - 向所有运行中的服务器发送命令"));
        System.out.println(ConsoleColor.colorize(ConsoleColor.BRIGHT_WHITE, "示例:"));
        System.out.println(ConsoleColor.colorize(ConsoleColor.WHITE, "  send server1 op player1"));
        System.out.println(ConsoleColor.colorize(ConsoleColor.WHITE, "  send myserver say Hello World"));
        System.out.println(ConsoleColor.colorize(ConsoleColor.WHITE, "  send all broadcast Server maintenance in 5 minutes"));
        System.out.println(ConsoleColor.colorize(ConsoleColor.WHITE, "  send all save-all"));
    }
    
    @Override
    public String getDescription() {
        return "向指定服务器或全部服务器发送MC指令";
    }
    
    @Override
    public CommandAnnotation getAnnotation() {
        return getClass().getAnnotation(CommandAnnotation.class);
    }
}