package exmo.cy.web;

import exmo.cy.security.User;
import exmo.cy.security.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 管理员控制器
 * 提供用户管理、权限管理等管理员功能
 */
@RestController
@RequestMapping("/api/admin")
@CrossOrigin(origins = "*")
public class AdminController {
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private PasswordEncoder passwordEncoder;
    
    /**
     * 检查当前用户是否为管理员
     */
    @GetMapping("/check")
    public ResponseEntity<?> checkAdmin(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("error", "未登录");
            return ResponseEntity.status(401).body(response);
        }
        
        String username = authentication.getName();
        User user = userRepository.findByUsername(username).orElse(null);
        
        Map<String, Object> response = new HashMap<>();
        if (user != null && "ADMIN".equals(user.getRole())) {
            response.put("success", true);
            response.put("isAdmin", true);
            response.put("username", username);
        } else {
            response.put("success", true);
            response.put("isAdmin", false);
        }
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * 获取所有用户列表（仅管理员）
     */
    @GetMapping("/users")
    public ResponseEntity<?> getAllUsers(Authentication authentication) {
        if (!isAdmin(authentication)) {
            return unauthorizedResponse();
        }
        
        try {
            List<User> users = userRepository.findAll();
            List<Map<String, Object>> userList = users.stream().map(user -> {
                Map<String, Object> userMap = new HashMap<>();
                userMap.put("id", user.getId());
                userMap.put("username", user.getUsername());
                userMap.put("role", user.getRole());
                userMap.put("enabled", user.isEnabled());
                userMap.put("createdAt", user.getCreatedAt() != null ? user.getCreatedAt().toString() : null);
                userMap.put("lastLoginAt", user.getLastLoginAt() != null ? user.getLastLoginAt().toString() : null);
                return userMap;
            }).collect(Collectors.toList());
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", userList);
            response.put("count", userList.size());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("error", "获取用户列表失败: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }
    
    /**
     * 获取用户详情（仅管理员）
     */
    @GetMapping("/users/{id}")
    public ResponseEntity<?> getUserById(@PathVariable Long id, Authentication authentication) {
        if (!isAdmin(authentication)) {
            return unauthorizedResponse();
        }
        
        try {
            Optional<User> userOpt = userRepository.findById(id);
            if (!userOpt.isPresent()) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("error", "用户不存在");
                return ResponseEntity.status(404).body(response);
            }
            
            User user = userOpt.get();
            Map<String, Object> userMap = new HashMap<>();
            userMap.put("id", user.getId());
            userMap.put("username", user.getUsername());
            userMap.put("role", user.getRole());
            userMap.put("enabled", user.isEnabled());
            userMap.put("createdAt", user.getCreatedAt() != null ? user.getCreatedAt().toString() : null);
            userMap.put("lastLoginAt", user.getLastLoginAt() != null ? user.getLastLoginAt().toString() : null);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", userMap);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("error", "获取用户详情失败: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }
    
    /**
     * 创建用户（仅管理员）
     */
    @PostMapping("/users")
    public ResponseEntity<?> createUser(@RequestBody Map<String, String> request, Authentication authentication) {
        if (!isAdmin(authentication)) {
            return unauthorizedResponse();
        }
        
        try {
            String username = request.get("username");
            String password = request.get("password");
            String role = request.getOrDefault("role", "USER");
            
            if (username == null || username.trim().isEmpty()) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("error", "用户名不能为空");
                return ResponseEntity.badRequest().body(response);
            }
            
            if (password == null || password.trim().isEmpty()) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("error", "密码不能为空");
                return ResponseEntity.badRequest().body(response);
            }
            
            if (!role.equals("ADMIN") && !role.equals("USER")) {
                role = "USER";
            }
            
            if (userRepository.existsByUsername(username)) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("error", "用户名已存在");
                return ResponseEntity.badRequest().body(response);
            }
            
            User user = new User(username, passwordEncoder.encode(password), role);
            userRepository.save(user);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "用户创建成功");
            response.put("userId", user.getId());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("error", "创建用户失败: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }
    
    /**
     * 更新用户信息（仅管理员）
     */
    @PutMapping("/users/{id}")
    public ResponseEntity<?> updateUser(@PathVariable Long id, @RequestBody Map<String, Object> request, Authentication authentication) {
        if (!isAdmin(authentication)) {
            return unauthorizedResponse();
        }
        
        try {
            Optional<User> userOpt = userRepository.findById(id);
            if (!userOpt.isPresent()) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("error", "用户不存在");
                return ResponseEntity.status(404).body(response);
            }
            
            User user = userOpt.get();
            
            // 更新角色
            if (request.containsKey("role")) {
                String role = (String) request.get("role");
                if (role.equals("ADMIN") || role.equals("USER")) {
                    user.setRole(role);
                }
            }
            
            // 更新启用状态
            if (request.containsKey("enabled")) {
                Boolean enabled = (Boolean) request.get("enabled");
                user.setEnabled(enabled != null ? enabled : true);
            }
            
            // 更新密码
            if (request.containsKey("password")) {
                String password = (String) request.get("password");
                if (password != null && !password.trim().isEmpty()) {
                    user.setPassword(passwordEncoder.encode(password));
                }
            }
            
            userRepository.save(user);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "用户更新成功");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("error", "更新用户失败: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }
    
    /**
     * 删除用户（仅管理员）
     */
    @DeleteMapping("/users/{id}")
    public ResponseEntity<?> deleteUser(@PathVariable Long id, Authentication authentication) {
        if (!isAdmin(authentication)) {
            return unauthorizedResponse();
        }
        
        try {
            Optional<User> userOpt = userRepository.findById(id);
            if (!userOpt.isPresent()) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("error", "用户不存在");
                return ResponseEntity.status(404).body(response);
            }
            
            User user = userOpt.get();
            String currentUsername = authentication.getName();
            
            // 防止删除自己
            if (user.getUsername().equals(currentUsername)) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("error", "不能删除自己的账户");
                return ResponseEntity.badRequest().body(response);
            }
            
            userRepository.delete(user);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "用户删除成功");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("error", "删除用户失败: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }
    
    /**
     * 获取系统统计信息（仅管理员）
     */
    @GetMapping("/stats")
    public ResponseEntity<?> getSystemStats(Authentication authentication) {
        if (!isAdmin(authentication)) {
            return unauthorizedResponse();
        }
        
        try {
            List<User> users = userRepository.findAll();
            long totalUsers = users.size();
            long adminUsers = users.stream().filter(u -> "ADMIN".equals(u.getRole())).count();
            long enabledUsers = users.stream().filter(User::isEnabled).count();
            
            Map<String, Object> stats = new HashMap<>();
            stats.put("totalUsers", totalUsers);
            stats.put("adminUsers", adminUsers);
            stats.put("userUsers", totalUsers - adminUsers);
            stats.put("enabledUsers", enabledUsers);
            stats.put("disabledUsers", totalUsers - enabledUsers);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", stats);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("error", "获取统计信息失败: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }
    
    /**
     * 检查是否为管理员
     */
    private boolean isAdmin(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }
        
        String username = authentication.getName();
        User user = userRepository.findByUsername(username).orElse(null);
        return user != null && "ADMIN".equals(user.getRole());
    }
    
    /**
     * 返回未授权响应
     */
    private ResponseEntity<?> unauthorizedResponse() {
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("error", "需要管理员权限");
        return ResponseEntity.status(403).body(response);
    }
}
