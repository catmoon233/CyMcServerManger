package exmo.cy.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class InitializeDefaultUser implements CommandLineRunner {
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private PasswordEncoder passwordEncoder;
    
    @Override
    public void run(String... args) throws Exception {
        // 检查是否存在管理员用户，如果没有则创建默认用户
        if (!userRepository.existsByUsername("admin")) {
            User adminUser = new User("admin", passwordEncoder.encode("admin123"), "ADMIN");
            userRepository.save(adminUser);
            System.out.println("已创建默认管理员用户: admin / admin123");
        }
    }
}