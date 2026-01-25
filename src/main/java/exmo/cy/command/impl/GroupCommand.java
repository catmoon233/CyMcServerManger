package exmo.cy.command.impl;

import exmo.cy.command.AnnotatedCommand;
import exmo.cy.command.CommandAnnotation;
import exmo.cy.model.ServerGroup;
import exmo.cy.service.ServerGroupService;
import exmo.cy.service.ServerService;
import exmo.cy.util.Logger;

import java.util.List;
import java.util.Optional;
import java.util.Scanner;
import java.util.Set;

@CommandAnnotation(
    name = "group",
    aliases = {"grp"},
    description = "管理服务器群组"
)
public class GroupCommand extends AnnotatedCommand {
    private final ServerGroupService serverGroupService;
    private final ServerService serverService;
    private final Scanner scanner = new Scanner(System.in);
    
    public GroupCommand(ServerGroupService serverGroupService, ServerService serverService) {
        this.serverGroupService = serverGroupService;
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
            case "create":
                return createGroup(args);
            case "delete":
                return deleteGroup(args);
            case "add":
                return addServerToGroup(args);
            case "remove":
                return removeServerFromGroup(args);
            case "list":
            case "ls":
                return listGroups();
            case "info":
                return showGroupInfo(args);
            case "start":
                return startGroup(args);
            case "stop":
                return stopGroup(args);
            case "next":
                return startNextServer(args);
            default:
                Logger.println("未知的操作: " + action);
                showUsage();
                return true;
        }
    }
    
    private void showUsage() {
        Logger.println("服务器群组管理命令用法:");
        Logger.println("  group create <群组名>                        - 创建服务器群组");
        Logger.println("  group create <群组名> <启动模式> [JVM参数] [服务器参数]  - 创建带预设配置的服务器群组");
        Logger.println("  group delete <群组名>                        - 删除服务器群组");
        Logger.println("  group add <群组名> <服务器名>                 - 添加服务器到群组");
        Logger.println("  group remove <群组名> <服务器名>              - 从群组中移除服务器");
        Logger.println("  group list                                  - 列出所有群组");
        Logger.println("  group info <群组名>                          - 查看群组信息");
        Logger.println("  group start concurrent <群组名>              - 同时启动群组中的所有服务器");
        Logger.println("  group start ordered <群组名>                 - 按顺序启动群组中的服务器");
        Logger.println("  group stop <群组名>                          - 停止群组中的所有服务器");
        Logger.println("  group next <群组名>                          - 手动启动队列中的下一个服务器");
        Logger.println("示例:");
        Logger.println("  group create mygroup");
        Logger.println("  group create mygroup 1 \"-Xms1G -Xmx4G\" \"nogui\"");
        Logger.println("  group add mygroup server1");
        Logger.println("  group add mygroup server2");
        Logger.println("  group start concurrent mygroup");
    }
    
    private boolean createGroup(String[] args) {
        if (args.length < 2) {
            Logger.println("错误: 请指定群组名称");
            showUsage();
            return true;
        }
        
        String groupName = args[1];
        
        // 检查是否提供了启动模式和预设参数
        int launchMode = 1; // 默认CORE模式
        String presetJvmArgs = null;
        String presetServerArgs = null;
        String minMemory = null;
        String maxMemory = null;
        
        if (args.length > 2) {
            try {
                launchMode = Integer.parseInt(args[2]);
                if (launchMode < 1 || launchMode > 5) {
                    Logger.println("错误: 启动模式必须在1-5之间");
                    Logger.println("1: CORE模式, 2: MODPACK模式, 3: BASIC模式, 4: BASIC_FIX模式, 5: CUSTOM模式");
                    return true;
                }
                
                if (args.length > 3) {
                    presetJvmArgs = args[3];
                }
                
                if (args.length > 4) {
                    presetServerArgs = args[4];
                }
                
                if (args.length > 5) {
                    minMemory = args[5];
                }
                
                if (args.length > 6) {
                    maxMemory = args[6];
                }
                
                if (serverGroupService.createGroup(groupName, launchMode, presetJvmArgs, presetServerArgs, minMemory, maxMemory)) {
                    Logger.println("群组 " + groupName + " 创建成功 (启动模式: " + launchMode + ")");
                } else {
                    Logger.println("群组 " + groupName + " 创建失败或已存在");
                }
            } catch (NumberFormatException e) {
                Logger.println("错误: 启动模式必须是一个数字");
                return true;
            }
        } else {
            if (serverGroupService.createGroup(groupName)) {
                Logger.println("群组 " + groupName + " 创建成功");
            } else {
                Logger.println("群组 " + groupName + " 创建失败或已存在");
            }
        }
        
        return true;
    }
    
    private boolean deleteGroup(String[] args) {
        if (args.length < 2) {
            Logger.println("错误: 请指定群组名称");
            showUsage();
            return true;
        }
        
        String groupName = args[1];
        if (serverGroupService.deleteGroup(groupName)) {
            Logger.println("群组 " + groupName + " 删除成功");
        } else {
            Logger.println("群组 " + groupName + " 删除失败或不存在");
        }
        
        return true;
    }
    
    private boolean addServerToGroup(String[] args) {
        if (args.length < 3) {
            Logger.println("错误: 请指定群组名称和服务器名称");
            showUsage();
            return true;
        }
        
        String groupName = args[1];
        String serverName = args[2];
        
        if (serverGroupService.addServerToGroup(groupName, serverName)) {
            Logger.println("服务器 " + serverName + " 添加到群组 " + groupName + " 成功");
        } else {
            Logger.println("服务器 " + serverName + " 添加到群组 " + groupName + " 失败");
        }
        
        return true;
    }
    
    private boolean removeServerFromGroup(String[] args) {
        if (args.length < 3) {
            Logger.println("错误: 请指定群组名称和服务器名称");
            showUsage();
            return true;
        }
        
        String groupName = args[1];
        String serverName = args[2];
        
        if (serverGroupService.removeServerFromGroup(groupName, serverName)) {
            Logger.println("服务器 " + serverName + " 从群组 " + groupName + " 中移除成功");
        } else {
            Logger.println("服务器 " + serverName + " 从群组 " + groupName + " 中移除失败");
        }
        
        return true;
    }
    
    private boolean listGroups() {
        Set<String> groupNames = serverGroupService.getGroupNames();
        
        if (groupNames.isEmpty()) {
            Logger.println("当前没有服务器群组");
        } else {
            Logger.println("服务器群组列表:");
            for (String groupName : groupNames) {
                ServerGroup group = serverGroupService.getGroup(groupName);
                if (group != null) {
                    Logger.println("  - " + groupName + " (服务器数量: " + group.getServerCount() + 
                                 ", 顺序启动: " + (group.isOrderedStartup() ? "是" : "否") + ")");
                }
            }
        }
        
        return true;
    }
    
    private boolean showGroupInfo(String[] args) {
        if (args.length < 2) {
            Logger.println("错误: 请指定群组名称");
            showUsage();
            return true;
        }
        
        String groupName = args[1];
        ServerGroup group = serverGroupService.getGroup(groupName);
        
        if (group == null) {
            Logger.println("错误: 群组 " + groupName + " 不存在");
            return true;
        }
        
        Logger.println("群组信息 - " + groupName + ":");
        Logger.println("  服务器数量: " + group.getServerCount());
        Logger.println("  顺序启动: " + (group.isOrderedStartup() ? "是" : "否"));
        Logger.println("  触发关键词: " + group.getTriggerKeyword());
        Logger.println("  启动延迟: " + group.getStartupDelay() + "ms");
        Logger.println("  启动模式: " + group.getLaunchMode());
        Logger.println("  预设JVM参数: " + (group.getPresetJvmArgs() != null ? group.getPresetJvmArgs() : "无"));
        Logger.println("  预设服务器参数: " + (group.getPresetServerArgs() != null ? group.getPresetServerArgs() : "无"));
        Logger.println("  服务器列表:");
        for (String serverName : group.getServerNames()) {
            Logger.println("    - " + serverName);
        }
        
        return true;
    }
    
    private boolean startGroup(String[] args) {
        if (args.length < 3) {
            Logger.println("错误: 请指定启动模式和群组名称");
            showUsage();
            return true;
        }
        
        String mode = args[1].toLowerCase();
        String groupName = args[2];
        
        if ("concurrent".equals(mode)) {
            serverGroupService.startGroupConcurrently(groupName);
            Logger.println("正在同时启动群组 " + groupName + " 中的所有服务器...");
        } else if ("ordered".equals(mode)) {
            serverGroupService.startGroupOrdered(groupName);
            Logger.println("正在按顺序启动群组 " + groupName + " 中的服务器...");
        } else {
            Logger.println("错误: 无效的启动模式。请使用 'concurrent' 或 'ordered'");
            return true;
        }
        
        return true;
    }
    
    private boolean stopGroup(String[] args) {
        if (args.length < 2) {
            Logger.println("错误: 请指定群组名称");
            showUsage();
            return true;
        }
        
        String groupName = args[1];
        serverGroupService.stopGroup(groupName);
        Logger.println("正在停止群组 " + groupName + " 中的所有服务器...");
        
        return true;
    }
    
    private boolean startNextServer(String[] args) {
        if (args.length < 2) {
            Logger.println("错误: 请指定群组名称");
            showUsage();
            return true;
        }
        
        String groupName = args[1];
        serverGroupService.manualStartNextServer(groupName);
        Logger.println("正在启动群组 " + groupName + " 队列中的下一个服务器...");
        
        return true;
    }
    
    @Override
    public String getDescription() {
        return "管理服务器群组";
    }
    
    @Override
    public CommandAnnotation getAnnotation() {
        return getClass().getAnnotation(CommandAnnotation.class);
    }
}