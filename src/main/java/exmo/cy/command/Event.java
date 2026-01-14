package exmo.cy.command;

/**
 * 事件基类
 */
public abstract class Event {
    private boolean cancelled = false;
    
    /**
     * 检查事件是否被取消
     * @return 如果事件被取消则返回true
     */
    public boolean isCancelled() {
        return cancelled;
    }
    
    /**
     * 设置事件取消状态
     * @param cancel 取消状态
     */
    public void setCancelled(boolean cancel) {
        this.cancelled = cancel;
    }
    
    /**
     * 获取事件类型
     * @return 事件类型
     */
    public String getEventName() {
        return this.getClass().getSimpleName();
    }
}