package exmo.cy.command;

/**
 * 事件优先级枚举
 */
public enum EventPriority {
    LOWEST,    // 最低优先级
    LOW,       // 低优先级
    NORMAL,    // 正常优先级
    HIGH,      // 高优先级
    HIGHEST,   // 最高优先级
    MONITOR    // 监视器优先级（最后执行）
}