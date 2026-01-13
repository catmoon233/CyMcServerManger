package exmo.cy.exception;

/**
 * 配置相关异常类
 * 用于处理配置文件读取、解析和验证错误
 */
public class ConfigurationException extends ServerManagerException {
    
    /**
     * 构造函数
     * @param message 异常消息
     */
    public ConfigurationException(String message) {
        super(message);
    }
    
    /**
     * 构造函数
     * @param message 异常消息
     * @param cause 原因异常
     */
    public ConfigurationException(String message, Throwable cause) {
        super(message, cause);
    }
}