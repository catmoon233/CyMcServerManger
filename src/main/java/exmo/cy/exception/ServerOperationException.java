package exmo.cy.exception;

/**
 * 服务器操作异常类
 * 用于处理服务器启动、停止、管理等操作中的错误
 */
public class ServerOperationException extends ServerManagerException {
    
    /**
     * 构造函数
     * @param message 异常消息
     */
    public ServerOperationException(String message) {
        super(message);
    }
    
    /**
     * 构造函数
     * @param message 异常消息
     * @param cause 原因异常
     */
    public ServerOperationException(String message, Throwable cause) {
        super(message, cause);
    }
}