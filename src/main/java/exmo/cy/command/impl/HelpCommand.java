package exmo.cy.command.impl;

import exmo.cy.command.AnnotatedCommand;
import exmo.cy.command.CommandAnnotation;
import exmo.cy.command.CommandInterface;
import exmo.cy.command.CommandManager;
import exmo.cy.util.ConsoleColor;
import exmo.cy.util.Logger;

import java.util.Map;

@CommandAnnotation(
    name = "help",
    aliases = {"?"}, // 添加问号作为别名
    description = "显示帮助信息"
)
public class HelpCommand extends AnnotatedCommand {
    private CommandManager commandManager;
    
    // 用于注入CommandManager的构造函数
    public HelpCommand(CommandManager commandManager) {
        this.commandManager = commandManager;
    }
    
    @Override
    public boolean execute(String[] args) {
        if (commandManager == null) {
            Logger.println(ConsoleColor.colorize(ConsoleColor.RED, "错误: CommandManager未初始化"));
            return true;
        }
        
        Map<String, CommandInterface> commands = commandManager.getCommands();
        
        // 彩色输出标题
        System.out.println(ConsoleColor.colorize(ConsoleColor.BRIGHT_GREEN, 
            "\n=== CyMc服务器管理器帮助信息 ==="));
        System.out.println(ConsoleColor.colorize(ConsoleColor.BRIGHT_CYAN, 
            "可用指令说明："));
        
        for (Map.Entry<String, CommandInterface> entry : commands.entrySet()) {
            CommandInterface command = entry.getValue();
            
            // 检查是否为AnnotatedCommand以获取注解信息
            if (command instanceof AnnotatedCommand) {
                AnnotatedCommand annotatedCommand = (AnnotatedCommand) command;
                CommandAnnotation annotation = annotatedCommand.getAnnotation();
                
                if (annotation != null) {
                    String name = annotation.name();
                    String[] aliases = annotation.aliases();
                    String description = annotation.description();
                    
                    StringBuilder commandInfo = new StringBuilder();
                    commandInfo.append(ConsoleColor.colorize(ConsoleColor.BRIGHT_YELLOW, name));
                    
                    if (aliases.length > 0) {
                        commandInfo.append(ConsoleColor.colorize(ConsoleColor.BRIGHT_BLACK, " ("));
                        for (int i = 0; i < aliases.length; i++) {
                            if (i > 0) {
                                commandInfo.append(", ");
                            }
                            commandInfo.append(ConsoleColor.colorize(ConsoleColor.BRIGHT_BLACK, aliases[i]));
                        }
                        commandInfo.append(ConsoleColor.colorize(ConsoleColor.BRIGHT_BLACK, ")"));
                    }
                    
                    commandInfo.append(": ").append(ConsoleColor.colorize(ConsoleColor.BRIGHT_WHITE, description));
                    
                    System.out.println("  " + commandInfo.toString());
                }
            } else {
                // 对于非注解命令，尝试使用通用方法获取描述
                String commandName = command.getClass().getSimpleName().replace("Command", "").toLowerCase();
                String description = command.getDescription();
                System.out.println("  " + ConsoleColor.colorize(ConsoleColor.BRIGHT_YELLOW, commandName) + ": " + 
                                 ConsoleColor.colorize(ConsoleColor.BRIGHT_WHITE, description));
            }
        }
        
        System.out.println(ConsoleColor.colorize(ConsoleColor.BRIGHT_BLACK, 
            "\n提示: 输入 '命令名 ?' 可查看特定命令的详细帮助信息"));
        
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