package exmo.cy.web;

import exmo.cy.command.CommandManager;
import exmo.cy.model.Server;
import exmo.cy.model.ServerInstance;
import exmo.cy.service.ServerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 服务器管理REST控制器
 * 提供服务器操作的API端点
 */
@RestController
@RequestMapping("/api/servers")
public class ServerController {

    @Autowired
    private ServerService serverService;

    /**
     * 获取所有服务器列表
     */
    @GetMapping("/")
    public ResponseEntity<List<Server>> getAllServers() {
        try {
            List<Server> servers = serverService.getConfigManager().loadServers();
            return ResponseEntity.ok(servers);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * 获取所有运行中的服务器（包含详细信息）
     */
    @GetMapping("/running")
    public ResponseEntity<Map<String, Map<String, Object>>> getRunningServers() {
        Map<String, ServerInstance> activeServers = serverService.getActiveServers();
        Map<String, Map<String, Object>> result = new HashMap<>();
        
        for (Map.Entry<String, ServerInstance> entry : activeServers.entrySet()) {
            ServerInstance instance = entry.getValue();
            Map<String, Object> serverInfo = new HashMap<>();
            
            serverInfo.put("name", instance.getServerName());
            serverInfo.put("version", instance.getVersion());
            serverInfo.put("description", instance.getDescription());
            serverInfo.put("uptime", instance.getUptime());
            serverInfo.put("startTime", instance.getStartTime());
            serverInfo.put("running", instance.isRunning());
            
            // TODO: 可以添加更多服务器状态信息，如内存使用、在线玩家等
            serverInfo.put("playerCount", 0); // 暂时设为0，后续可以实现玩家计数
            serverInfo.put("memoryUsage", "N/A"); // 暂时设为N/A
            
            result.put(entry.getKey(), serverInfo);
        }
        
        return ResponseEntity.ok(result);
    }

    /**
     * 启动服务器
     */
    @PostMapping("/{name}/start")
    public ResponseEntity<String> startServer(
            @PathVariable String name,
            @RequestBody(required = false) Map<String, Object> request) {
        try {
            Optional<Server> serverOpt = serverService.getConfigManager().findServerByName(name);
            if (!serverOpt.isPresent()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("服务器不存在");
            }
            
            Server server = serverOpt.get();
            
            // 从请求体获取参数
            int launchMode = request != null && request.containsKey("launchMode") ? 
                ((Number)request.get("launchMode")).intValue() : 1;
            String javaPath = request != null && request.containsKey("javaPath") ? 
                (String)request.get("javaPath") : "java";
            String jvmArgs = request != null && request.containsKey("jvmArgs") ? 
                (String)request.get("jvmArgs") : null;
            String serverArgs = request != null && request.containsKey("serverArgs") ? 
                (String)request.get("serverArgs") : null;

            serverService.startServer(server, launchMode, javaPath, jvmArgs, serverArgs);
            return ResponseEntity.ok("服务器启动成功");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("启动失败: " + e.getMessage());
        }
    }

    /**
     * 停止服务器
     */
    @PostMapping("/{name}/stop")
    public ResponseEntity<String> stopServer(@PathVariable String name) {
        try {
            serverService.stopServer(name);
            return ResponseEntity.ok("服务器停止成功");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("停止失败: " + e.getMessage());
        }
    }

    /**
     * 强制停止服务器
     */
    @PostMapping("/{name}/forceStop")
    public ResponseEntity<String> forceStopServer(@PathVariable String name) {
        try {
            serverService.forceStopServer(name);
            return ResponseEntity.ok("服务器强制停止成功");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("强制停止失败: " + e.getMessage());
        }
    }

    /**
     * 向服务器发送命令
     */
    @PostMapping("/{name}/command")
    public ResponseEntity<String> sendCommand(
            @PathVariable String name,
            @RequestBody Map<String, String> request) {
        try {
            String command = request.get("command");
            if (command == null || command.trim().isEmpty()) {
                return ResponseEntity.badRequest().body("命令不能为空");
            }
            
            serverService.sendCommand(name, command);
            
            // 将命令结果发送到WebSocket
            LogWebSocketHandler.sendCommandResponse(name, command, "命令已发送");
            
            return ResponseEntity.ok("命令发送成功");
        } catch (Exception e) {
            String errorMsg = "发送命令失败: " + e.getMessage();
            LogWebSocketHandler.sendLogMessage(name, "[ERROR] " + errorMsg);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorMsg);
        }
    }

    /**
     * 创建服务器
     */
    @PostMapping("/create")
    public ResponseEntity<String> createServer(
            @RequestBody Map<String, Object> request) {
        try {
            String coreName = (String) request.get("coreName");
            String serverName = (String) request.get("serverName");
            String description = (String) request.get("description");
            String version = (String) request.getOrDefault("version", "1.0.0");
            String path = (String) request.getOrDefault("path", "");

            if (coreName == null || serverName == null) {
                return ResponseEntity.badRequest().body("核心文件名和服务器名称不能为空");
            }

            serverService.createServer(coreName, serverName, description, version, path);
            return ResponseEntity.ok("服务器创建成功");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("创建失败: " + e.getMessage());
        }
    }

    /**
     * 删除服务器
     */
    @DeleteMapping("/{name}")
    public ResponseEntity<String> deleteServer(
            @PathVariable String name,
            @RequestBody(required = false) Map<String, Boolean> request) {
        try {
            boolean deleteFiles = request != null && request.getOrDefault("deleteFiles", false);
            serverService.deleteServer(name, deleteFiles);
            
            // 通知WebSocket移除相关连接
            LogWebSocketHandler.removeServerConnections(name);
            
            return ResponseEntity.ok("服务器删除成功");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("删除失败: " + e.getMessage());
        }
    }
}