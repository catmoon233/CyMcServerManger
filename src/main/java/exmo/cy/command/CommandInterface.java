package exmo.cy.command;

/**
 * 命令接口，定义命令的基本结构
 */
public interface CommandInterface {
    /**
     * 执行命令
     * @param args 命令参数
     * @return 是否继续运行
     */
    boolean execute(String[] args);
    
    /**
     * 获取命令描述
     * @return 命令描述
     */
    String getDescription();
}