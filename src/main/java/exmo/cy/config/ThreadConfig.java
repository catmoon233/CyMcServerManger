package exmo.cy.config;

import exmo.cy.util.Logger;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 线程配置和管理类
 * 提供统一的线程池配置和系统线程监控功能
 */
public class ThreadConfig {
    
    // 防止实例化
    private ThreadConfig() {
        throw new UnsupportedOperationException("这是一个工具类，不能被实例化");
    }
    
    /**
     * 获取系统推荐的线程池大小
     * @param multiplier 倍数因子
     * @return 推荐的线程池大小
     */
    public static int getRecommendedThreadPoolSize(double multiplier) {
        int processors = Runtime.getRuntime().availableProcessors();
        return Math.max(2, (int) (processors * multiplier));
    }
    
    /**
     * 获取系统线程信息
     * @return 线程统计信息
     */
    public static String getSystemThreadInfo() {
        ThreadMXBean threadBean = ManagementFactory.getThreadMXBean();
        int threadCount = threadBean.getThreadCount();
        int peakThreadCount = threadBean.getPeakThreadCount();
        long totalStartedThreadCount = threadBean.getTotalStartedThreadCount();
        int daemonThreadCount = threadBean.getDaemonThreadCount();
        
        return String.format(
            "当前线程数:%d, 峰值线程数:%d, 总启动线程数:%d, 守护线程数:%d",
            threadCount, peakThreadCount, totalStartedThreadCount, daemonThreadCount
        );
    }
    
    /**
     * 检查线程资源是否充足
     * @return 如果线程资源充足返回true
     */
    public static boolean isThreadResourceSufficient() {
        ThreadMXBean threadBean = ManagementFactory.getThreadMXBean();
        int currentThreads = threadBean.getThreadCount();
        int peakThreads = threadBean.getPeakThreadCount();
        
        // 如果当前线程数超过峰值的80%，认为资源紧张
        if (currentThreads > peakThreads * 0.8) {
            Logger.warn("线程资源紧张 - 当前线程数:" + currentThreads + ", 峰值线程数:" + peakThreads);
            return false;
        }
        
        return true;
    }
    
    /**
     * 创建命名线程工厂
     * @param namePrefix 线程名称前缀
     * @param daemon 是否为守护线程
     * @param priority 线程优先级
     * @return 线程工厂
     */
    public static ThreadFactory createNamedThreadFactory(String namePrefix, boolean daemon, int priority) {
        return new ThreadFactory() {
            private final AtomicInteger threadNumber = new AtomicInteger(1);
            
            @Override
            public Thread newThread(Runnable r) {
                Thread t = new Thread(r, namePrefix + "-" + threadNumber.getAndIncrement());
                t.setDaemon(daemon);
                t.setPriority(priority);
                return t;
            }
        };
    }
    
    /**
     * 创建标准的服务线程工厂
     * @param serviceName 服务名称
     * @return 线程工厂
     */
    public static ThreadFactory createServiceThreadFactory(String serviceName) {
        return createNamedThreadFactory(serviceName, true, Thread.NORM_PRIORITY);
    }
    
    /**
     * 创建低优先级的后台线程工厂
     * @param name 用途名称
     * @return 线程工厂
     */
    public static ThreadFactory createBackgroundThreadFactory(String name) {
        return createNamedThreadFactory(name, true, Thread.MIN_PRIORITY);
    }
    
    /**
     * 打印详细的线程信息
     */
    public static void printDetailedThreadInfo() {
        ThreadMXBean threadBean = ManagementFactory.getThreadMXBean();
        
        Logger.info("=== 系统线程详细信息 ===");
        Logger.info("当前线程数: " + threadBean.getThreadCount());
        Logger.info("峰值线程数: " + threadBean.getPeakThreadCount());
        Logger.info("总启动线程数: " + threadBean.getTotalStartedThreadCount());
        Logger.info("守护线程数: " + threadBean.getDaemonThreadCount());
        Logger.info("CPU核心数: " + Runtime.getRuntime().availableProcessors());
        
        // 打印前几个线程的信息
        long[] threadIds = threadBean.getAllThreadIds();
        int maxDisplay = Math.min(10, threadIds.length);
        
        Logger.info("前" + maxDisplay + "个线程详情:");
        for (int i = 0; i < maxDisplay; i++) {
            long threadId = threadIds[i];
            String threadName = threadBean.getThreadInfo(threadId).getThreadName();
            Thread.State threadState = threadBean.getThreadInfo(threadId).getThreadState();
            Logger.info("  线程" + (i+1) + ": " + threadName + " (ID: " + threadId + ", 状态: " + threadState + ")");
        }
        
        Logger.info("========================");
    }
    
    /**
     * 检查是否存在潜在的线程泄漏
     * @return 如果可能存在线程泄漏返回true
     */
    public static boolean checkPotentialThreadLeak() {
        ThreadMXBean threadBean = ManagementFactory.getThreadMXBean();
        long totalStarted = threadBean.getTotalStartedThreadCount();
        int current = threadBean.getThreadCount();
        int peak = threadBean.getPeakThreadCount();
        
        // 如果总启动线程数远大于当前线程数，可能存在线程泄漏
        if (totalStarted > current * 10 && current > 100) {
            Logger.warn("检测到潜在的线程泄漏风险 - 总启动线程数:" + totalStarted + 
                       ", 当前线程数:" + current + ", 峰值线程数:" + peak);
            return true;
        }
        
        return false;
    }
}