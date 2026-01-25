package exmo.cy.initializer;

import exmo.cy.scheduler.SchedulerManager;
import exmo.cy.service.ServerService;
import exmo.cy.util.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * 调度管理器初始化器
 * 在Spring应用启动时初始化调度管理器
 */
@Component
public class SchedulerInitializer implements CommandLineRunner {
    
    @Autowired
    private ServerService serverService;
    
    @Override
    public void run(String... args) throws Exception {
        // 初始化调度管理器
        SchedulerManager.getInstance().initialize(serverService);
        Logger.info("调度管理器初始化完成");
    }
}