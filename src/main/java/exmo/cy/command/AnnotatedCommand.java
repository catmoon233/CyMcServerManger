package exmo.cy.command;

/**
 * 带注解的命令抽象类
 */
public abstract class AnnotatedCommand implements CommandInterface {
    /**
     * 获取命令注解
     * @return 命令注解
     */
    public abstract CommandAnnotation getAnnotation();
}