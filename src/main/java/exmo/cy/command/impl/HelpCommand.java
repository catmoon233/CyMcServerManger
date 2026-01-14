package exmo.cy.command.impl;

import exmo.cy.command.AnnotatedCommand;
import exmo.cy.command.CommandAnnotation;
import exmo.cy.command.CommandManager;
import exmo.cy.util.Logger;

@CommandAnnotation(
    name = "help",
    aliases = {},
    description = "显示帮助信息"
)
public class HelpCommand extends AnnotatedCommand {
    
    @Override
    public boolean execute(String[] args) {
        Logger.println("可用指令说明：");
        Logger.println("create - 创建新服务器");
        Logger.println("add - 添加现有服务器目录");
        Logger.println("switch - 切换服务器核心版本");
        Logger.println("start - 启动服务器");
        Logger.println("last - 调用上次的参数启动服务器");
        Logger.println("stop - 退出程序（在主控制台）");
        Logger.println("help - 显示此帮助信息");
        Logger.println("list - 列出所有服务器");
        Logger.println("map - 切换服务器地图");
        Logger.println("delete - 删除服务器配置和本地文件");
        Logger.println("attach/at - 连接到运行中的服务器控制台");
        Logger.println("detach - 从服务器控制台返回主控制台");
        Logger.println("list-running/lr - 列出所有运行中的服务器");
        Logger.println("stop-server/ss - 正常停止指定服务器");
        Logger.println("force-stop - 强制终止当前连接的服务器（在服务器控制台）");
        return true;
    }
    
    @Override
    public String getDescription() {
        return "显示帮助信息";
    }
    
    @Override
    public CommandAnnotation getAnnotation() {
        return getClass().getAnnotation(CommandAnnotation.class);
    }
}