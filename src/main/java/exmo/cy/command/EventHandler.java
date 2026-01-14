package exmo.cy.command;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 事件监听器注解
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface EventHandler {
    /**
     * 事件优先级
     */
    EventPriority priority() default EventPriority.NORMAL;
    
    /**
     * 是否忽略取消的事件
     */
    boolean ignoreCancelled() default false;
}
