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
        // 如果存在但密码可能不正确，也重新设置密码
        User existingUser = userRepository.findByUsername("admin").orElse(null);
        
        if (existingUser == null) {
            // 创建新用户
            User adminUser = new User("admin", passwordEncoder.encode("admin123"), "ADMIN");
            userRepository.save(adminUser);
            System.out.println("已创建默认管理员用户: admin / admin123");
        } else {
            // 检查密码是否正确加密（BCrypt密码以$2a$、$2b$或$2y$开头）
            String currentPassword = existingUser.getPassword();
            if (currentPassword == null || !currentPassword.startsWith("$2")) {
                // 密码未加密或格式不正确，重新加密
                existingUser.setPassword(passwordEncoder.encode("admin123"));
                userRepository.save(existingUser);
                System.out.println("已更新默认管理员用户密码: admin / admin123");
            } else {
                // 验证密码是否正确
                if (!passwordEncoder.matches("admin123", currentPassword)) {
                    // 密码不匹配，重新设置
                    existingUser.setPassword(passwordEncoder.encode("admin123"));
                    userRepository.save(existingUser);
                    System.out.println("已重置默认管理员用户密码: admin / admin123");
                } else {
                    System.out.println("默认管理员用户已存在: admin / admin123");
                }
            }
        }
    }
}