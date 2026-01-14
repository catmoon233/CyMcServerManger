package exmo.cy.command;

import java.lang.reflect.Method;

/**
 * 注册的监听器
 */
public class RegisteredListener implements Comparable<RegisteredListener> {
    private final Object listener;
    private final Method method;
    private final EventPriority priority;
    
    public RegisteredListener(Object listener, Method method, EventPriority priority) {
        this.listener = listener;
        this.method = method;
        this.priority = priority;
    }
    
    public Object getListener() {
        return listener;
    }
    
    public Method getMethod() {
        return method;
    }
    
    public EventPriority getPriority() {
        return priority;
    }
    
    @Override
    public int compareTo(RegisteredListener other) {
        // 按优先级排序，HIGH优先级的在前面
        return Integer.compare(other.getPriority().ordinal(), this.getPriority().ordinal());
    }
}