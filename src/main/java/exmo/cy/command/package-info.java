/**
 * 命令系统包，包含模块化和事件驱动的命令处理系统
 * 
 * <p>主要组件：</p>
 * <ul>
 *   <li>{@link exmo.cy.command.CommandManager} - 命令管理系统的核心</li>
 *   <li>{@link exmo.cy.command.CommandInterface} - 命令接口定义</li>
 *   <li>{@link exmo.cy.command.EventManager} - 事件管理系统</li>
 *   <li>{@link exmo.cy.command.CommandAnnotation} - 命令注解</li>
 *   <li>{@link exmo.cy.command.EventHandler} - 事件处理注解</li>
 * </ul>
 * 
 * <p>该系统实现了：</p>
 * <ul>
 *   <li>模块化的命令设计</li>
 *   <li>基于注解的命令注册</li>
 *   <li>事件驱动的架构</li>
 *   <li>灵活的扩展机制</li>
 * </ul>
 */
package exmo.cy.command;