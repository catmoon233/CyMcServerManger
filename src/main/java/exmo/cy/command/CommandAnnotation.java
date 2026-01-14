package exmo.cy.command;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 命令注解，用于标记命令类
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface CommandAnnotation {
    /**
     * 命令名称
     */
    String name();
    
    /**
     * 命令别名
     */
    String[] aliases() default {};
    
    /**
     * 命令描述
     */
    String description() default "";
}