package exmo.cy.util;

import exmo.cy.config.ThreadConfig;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 线程监控工具类
 * 定期监控系统线程使用情况，及时发现和预警线程资源问题
 */
public class ThreadMonitor {
    private static final AtomicBoolean isMonitoring = new AtomicBoolean(false);
    private static Timer monitoringTimer;
    private static final long MONITOR_INTERVAL = 30000; // 30秒检查一次
    
    // 警告阈值
    private static final double THREAD_WARNING_THRESHOLD = 0.7; // 70%使用率警告
    private static final double THREAD_CRITICAL_THRESHOLD = 0.85; // 85%使用率严重警告
    private static final int MIN_THREAD_COUNT_FOR_MONITORING = 50; // 至少50个线程才开始监控
    
    private ThreadMonitor() {
        // 私有构造函数，防止实例化
    }
    
    /**
     * 启动线程监控
     */
    public static void startMonitoring() {
        if (isMonitoring.compareAndSet(false, true)) {
            monitoringTimer = new Timer("ThreadMonitor", true);
            monitoringTimer.scheduleAtFixedRate(new ThreadMonitorTask(), 0, MONITOR_INTERVAL);
            Logger.info("线程监控已启动，检查间隔: " + (MONITOR_INTERVAL/1000) + "秒");
        }
    }
    
    /**
     * 停止线程监控
     */
    public static void stopMonitoring() {
        if (isMonitoring.compareAndSet(true, false)) {
            if (monitoringTimer != null) {
                monitoringTimer.cancel();
                monitoringTimer = null;
            }
            Logger.info("线程监控已停止");
        }
    }
    
    /**
     * 检查当前线程使用情况
     * @return 线程使用情况报告
     */
    public static ThreadUsageReport getCurrentUsage() {
        ThreadMXBean threadBean = ManagementFactory.getThreadMXBean();
        int currentThreads = threadBean.getThreadCount();
        int peakThreads = threadBean.getPeakThreadCount();
        int daemonThreads = threadBean.getDaemonThreadCount();
        long totalStarted = threadBean.getTotalStartedThreadCount();
        
        double usageRatio = peakThreads > 0 ? (double) currentThreads / peakThreads : 0;
        
        return new ThreadUsageReport(currentThreads, peakThreads, daemonThreads, 
                                   totalStarted, usageRatio);
    }
    
    /**
     * 执行即时线程健康检查
     * @return 健康检查结果
     */
    public static HealthCheckResult performHealthCheck() {
        ThreadUsageReport usage = getCurrentUsage();
        ThreadMXBean threadBean = ManagementFactory.getThreadMXBean();
        
        HealthCheckResult result = new HealthCheckResult();
        result.usageReport = usage;
        
        // 检查线程数量是否过多
        if (usage.currentThreads > MIN_THREAD_COUNT_FOR_MONITORING) {
            if (usage.usageRatio >= THREAD_CRITICAL_THRESHOLD) {
                result.status = HealthStatus.CRITICAL;
                result.message = "线程使用率达到临界水平: " + 
                               String.format("%.1f%%", usage.usageRatio * 100);
            } else if (usage.usageRatio >= THREAD_WARNING_THRESHOLD) {
                result.status = HealthStatus.WARNING;
                result.message = "线程使用率较高: " + 
                               String.format("%.1f%%", usage.usageRatio * 100);
            } else {
                result.status = HealthStatus.HEALTHY;
                result.message = "线程使用率正常: " + 
                               String.format("%.1f%%", usage.usageRatio * 100);
            }
        } else {
            result.status = HealthStatus.HEALTHY;
            result.message = "线程数量较少，无需担心";
        }
        
        // 检查线程泄漏风险
        if (usage.totalStarted > usage.currentThreads * 10 && usage.currentThreads > 100) {
            result.leakRisk = true;
            result.message += " (检测到潜在线程泄漏风险)";
        }
        
        return result;
    }
    
    /**
     * 线程监控任务
     */
    private static class ThreadMonitorTask extends TimerTask {
        @Override
        public void run() {
            try {
                HealthCheckResult health = performHealthCheck();
                
                switch (health.status) {
                    case CRITICAL:
                        Logger.error("[线程监控] " + health.message);
                        Logger.error("[线程监控] 当前线程数: " + health.usageReport.currentThreads + 
                                   ", 峰值: " + health.usageReport.peakThreads);
                        ThreadConfig.printDetailedThreadInfo();
                        // 可以在这里添加自动清理或其他缓解措施
                        break;
                        
                    case WARNING:
                        Logger.warn("[线程监控] " + health.message);
                        if (health.leakRisk) {
                            Logger.warn("[线程监控] 建议检查应用程序是否存在线程泄漏");
                        }
                        break;
                        
                    case HEALTHY:
                        Logger.debug("[线程监控] " + health.message);
                        break;
                }
                
            } catch (Exception e) {
                Logger.error("线程监控任务执行出错: " + e.getMessage(), e);
            }
        }
    }
    
    /**
     * 线程使用情况报告
     */
    public static class ThreadUsageReport {
        public final int currentThreads;
        public final int peakThreads;
        public final int daemonThreads;
        public final long totalStarted;
        public final double usageRatio;
        
        public ThreadUsageReport(int currentThreads, int peakThreads, int daemonThreads, 
                               long totalStarted, double usageRatio) {
            this.currentThreads = currentThreads;
            this.peakThreads = peakThreads;
            this.daemonThreads = daemonThreads;
            this.totalStarted = totalStarted;
            this.usageRatio = usageRatio;
        }
        
        @Override
        public String toString() {
            return String.format("当前线程:%d, 峰值:%d, 守护线程:%d, 总启动:%d, 使用率:%.1f%%", 
                               currentThreads, peakThreads, daemonThreads, 
                               totalStarted, usageRatio * 100);
        }
    }
    
    /**
     * 健康检查结果
     */
    public static class HealthCheckResult {
        public HealthStatus status;
        public String message;
        public ThreadUsageReport usageReport;
        public boolean leakRisk = false;
    }
    
    /**
     * 健康状态枚举
     */
    public enum HealthStatus {
        HEALTHY,    // 健康
        WARNING,    // 警告
        CRITICAL    // 严重
    }
}