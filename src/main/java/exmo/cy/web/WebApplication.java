package exmo.cy.web;

import exmo.cy.security.InitializeDefaultUser;
import exmo.cy.security.SecurityConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * Web应用程序主类
 * 启动Spring Boot Web服务器
 */
@SpringBootApplication
@ComponentScan(basePackages = "exmo.cy")
@EntityScan(basePackages = "exmo.cy.security") // 扫描实体类
@EnableJpaRepositories(basePackages = "exmo.cy.security") // 启用JPA仓库扫描
@Import({SecurityConfig.class, InitializeDefaultUser.class, WebMvcConfig.class}) // 导入安全配置和默认用户初始化
public class WebApplication {

    public static void main(String[] args) {
        // 设置web应用类型为servlet
        System.setProperty("spring.main.web-application-type", "servlet");
        SpringApplication.run(WebApplication.class, args);
    }
}