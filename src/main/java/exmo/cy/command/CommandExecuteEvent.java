package exmo.cy.command;

/**
 * 命令执行事件
 */
public class CommandExecuteEvent extends Event {
    private final String command;
    private final String[] args;
    private final String sender;
    
    public CommandExecuteEvent(String command, String[] args, String sender) {
        this.command = command;
        this.args = args.clone();
        this.sender = sender;
    }
    
    public String getCommand() {
        return command;
    }
    
    public String[] getArgs() {
        return args.clone();
    }
    
    public String getSender() {
        return sender;
    }
}