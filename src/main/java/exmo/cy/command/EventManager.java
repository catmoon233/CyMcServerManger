package exmo.cy.command;

import java.lang.reflect.Method;
import java.util.*;

/**
 * 事件处理器 - 事件驱动机制
 */
public class EventManager {
    private final Map<Class<?>, List<RegisteredListener>> listeners;
    
    public EventManager() {
        this.listeners = new HashMap<>();
    }
    
    /**
     * 注册监听器
     * @param listener 监听器对象
     */
    public void registerEvents(Object listener) {
        Class<?> clazz = listener.getClass();
        Method[] methods = clazz.getDeclaredMethods();
        
        for (Method method : methods) {
            if (method.isAnnotationPresent(EventHandler.class)) {
                EventHandler eventHandler = method.getAnnotation(EventHandler.class);
                
                // 确保方法只有一个参数且是Event的子类
                Class<?>[] paramTypes = method.getParameterTypes();
                if (paramTypes.length != 1 || !Event.class.isAssignableFrom(paramTypes[0])) {
                    throw new IllegalArgumentException("事件处理方法必须接收一个Event类型的参数: " + method.getName());
                }
                
                Class<?> eventType = paramTypes[0];
                
                // 获取或创建监听器列表
                List<RegisteredListener> eventListeners = listeners.computeIfAbsent(
                    eventType.asSubclass(Event.class), k -> new ArrayList<>());
                
                // 添加新的监听器
                RegisteredListener registeredListener = new RegisteredListener(listener, method, eventHandler.priority());
                eventListeners.add(registeredListener);
                
                // 按优先级排序
                Collections.sort(eventListeners);
            }
        }
    }
    
    /**
     * 调用事件
     * @param event 事件对象
     */
    public void callEvent(Event event) {
        Class<?> eventType = event.getClass();
        
        // 获取所有父类事件类型
        List<Class<?>> eventTypes = new ArrayList<>();
        Class<?> currentClass = eventType;
        while (currentClass != null && Event.class.isAssignableFrom(currentClass)) {
            eventTypes.add(currentClass);
            currentClass = currentClass.getSuperclass();
        }
        
        // 为每个事件类型调用监听器
        for (Class<?> type : eventTypes) {
            List<RegisteredListener> eventListeners = listeners.get(type);
            if (eventListeners != null) {
                for (RegisteredListener listener : eventListeners) {
                    try {
                        listener.getMethod().invoke(listener.getListener(), event);
                    } catch (Exception e) {
                        System.err.println("调用事件监听器时出错: " + e.getMessage());
                        e.printStackTrace();
                    }
                }
            }
        }
    }
    
    /**
     * 取消注册监听器
     * @param listener 监听器对象
     */
    public void unregisterEvents(Object listener) {
        Iterator<List<RegisteredListener>> iterator = listeners.values().iterator();
        while (iterator.hasNext()) {
            List<RegisteredListener> eventListeners = iterator.next();
            eventListeners.removeIf(registeredListener -> 
                registeredListener.getListener().equals(listener));
            
            if (eventListeners.isEmpty()) {
                iterator.remove();
            }
        }
    }
}