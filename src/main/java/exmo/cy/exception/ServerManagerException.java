package exmo.cy.exception;

/**
 * 服务器管理器基础异常类
 * 所有自定义异常的父类
 */
public class ServerManagerException extends Exception {
    
    /**
     * 构造函数
     * @param message 异常消息
     */
    public ServerManagerException(String message) {
        super(message);
    }
    
    /**
     * 构造函数
     * @param message 异常消息
     * @param cause 原因异常
     */
    public ServerManagerException(String message, Throwable cause) {
        super(message, cause);
    }
    
    /**
     * 构造函数
     * @param cause 原因异常
     */
    public ServerManagerException(Throwable cause) {
        super(cause);
    }
}