package exmo.cy.web;

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
    public ResponseEntity<Map<String, Object>> getAllServers() {
        try {
            List<Server> servers = serverService.getConfigManager().loadServers();
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", servers);
            response.put("count", servers.size());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("error", "加载服务器列表失败: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * 获取所有运行中的服务器（包含详细信息）
     */
    @GetMapping("/running")
    public ResponseEntity<Map<String, Object>> getRunningServers() {
        try {
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
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", result);
            response.put("count", result.size());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("error", "获取运行中服务器失败: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * 启动服务器
     */
    @PostMapping("/{name}/start")
    public ResponseEntity<Map<String, Object>> startServer(
            @PathVariable String name,
            @RequestBody(required = false) Map<String, Object> request) {
        try {
            Optional<Server> serverOpt = serverService.getConfigManager().findServerByName(name);
            if (!serverOpt.isPresent()) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("error", "服务器不存在");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
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
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "服务器启动成功");
            response.put("serverName", name);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("error", "启动失败: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * 停止服务器
     */
    @PostMapping("/{name}/stop")
    public ResponseEntity<Map<String, Object>> stopServer(@PathVariable String name) {
        try {
            serverService.stopServer(name);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "服务器停止成功");
            response.put("serverName", name);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("error", "停止失败: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * 强制停止服务器
     */
    @PostMapping("/{name}/forceStop")
    public ResponseEntity<Map<String, Object>> forceStopServer(@PathVariable String name) {
        try {
            serverService.forceStopServer(name);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "服务器强制停止成功");
            response.put("serverName", name);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("error", "强制停止失败: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * 向服务器发送命令
     */
    @PostMapping("/{name}/command")
    public ResponseEntity<Map<String, Object>> sendCommand(
            @PathVariable String name,
            @RequestBody Map<String, String> request) {
        try {
            String command = request.get("command");
            if (command == null || command.trim().isEmpty()) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("error", "命令不能为空");
                return ResponseEntity.badRequest().body(response);
            }
            
            serverService.sendCommand(name, command);
            
            // 将命令结果发送到WebSocket
            LogWebSocketHandler.sendCommandResponse(name, command, "命令已发送");
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "命令发送成功");
            response.put("serverName", name);
            response.put("command", command);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            String errorMsg = "发送命令失败: " + e.getMessage();
            LogWebSocketHandler.sendLogMessage(name, "[ERROR] " + errorMsg);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("error", errorMsg);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * 创建服务器
     */
    @PostMapping("/create")
    public ResponseEntity<Map<String, Object>> createServer(
            @RequestBody Map<String, Object> request) {
        try {
            String coreName = (String) request.get("coreName");
            String serverName = (String) request.get("serverName");
            String description = (String) request.get("description");
            String version = (String) request.getOrDefault("version", "1.0.0");
            String path = (String) request.getOrDefault("path", "");

            if (coreName == null || serverName == null) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("error", "核心文件名和服务器名称不能为空");
                return ResponseEntity.badRequest().body(response);
            }

            serverService.createServer(coreName, serverName, description, version, path);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "服务器创建成功");
            response.put("serverName", serverName);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("error", "创建失败: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * 删除服务器
     */
    @DeleteMapping("/{name}")
    public ResponseEntity<Map<String, Object>> deleteServer(
            @PathVariable String name,
            @RequestBody(required = false) Map<String, Boolean> request) {
        try {
            boolean deleteFiles = request != null && request.getOrDefault("deleteFiles", false);
            serverService.deleteServer(name, deleteFiles);
            
            // 通知WebSocket移除相关连接
            LogWebSocketHandler.removeServerConnections(name);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "服务器删除成功");
            response.put("serverName", name);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("error", "删除失败: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * 屏蔽服务器的控制台输出
     */
    @PostMapping("/{name}/block")
    public ResponseEntity<Map<String, Object>> blockServerOutput(@PathVariable String name) {
        try {
            boolean wasBlocked = serverService.isServerBlocked(name);
            serverService.blockServerOutput(name);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", wasBlocked ? "服务器已在屏蔽列表中" : "服务器控制台输出已屏蔽");
            response.put("serverName", name);
            response.put("isBlocked", true);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("error", "屏蔽服务器失败: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * 取消屏蔽服务器的控制台输出
     */
    @PostMapping("/{name}/unblock")
    public ResponseEntity<Map<String, Object>> unblockServerOutput(@PathVariable String name) {
        try {
            boolean wasBlocked = serverService.isServerBlocked(name);
            serverService.unblockServerOutput(name);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", !wasBlocked ? "服务器不在屏蔽列表中" : "服务器控制台屏蔽已取消");
            response.put("serverName", name);
            response.put("isBlocked", false);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("error", "取消屏蔽服务器失败: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * 获取被屏蔽的服务器列表
     */
    @GetMapping("/blocked")
    public ResponseEntity<Map<String, Object>> getBlockedServers() {
        try {
            List<String> blockedServers = serverService.getBlockedServers();
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", blockedServers);
            response.put("count", blockedServers.size());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("error", "获取被屏蔽服务器列表失败: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * 检查服务器是否被屏蔽
     */
    @GetMapping("/{name}/blocked")
    public ResponseEntity<Map<String, Object>> isServerBlocked(@PathVariable String name) {
        try {
            boolean isBlocked = serverService.isServerBlocked(name);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("isBlocked", isBlocked);
            response.put("serverName", name);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("error", "检查屏蔽状态失败: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * 从模板服务器复制文件夹到目标服务器
     */
    @PostMapping("/{targetName}/copy-from-template")
    public ResponseEntity<Map<String, Object>> copyFromTemplate(
            @PathVariable String targetName,
            @RequestBody Map<String, Object> request) {
        try {
            String templateName = (String) request.get("templateName");
            if (templateName == null || templateName.trim().isEmpty()) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("error", "模板服务器名称不能为空");
                return ResponseEntity.badRequest().body(response);
            }

            @SuppressWarnings("unchecked")
            List<String> foldersToCopy = (List<String>) request.getOrDefault("folders", 
                java.util.Arrays.asList("mods", "config", "world", "plugins"));
            boolean overwrite = (boolean) request.getOrDefault("overwrite", false);

            serverService.copyFromTemplateServer(targetName, templateName, foldersToCopy, overwrite);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "服务器数据复制成功");
            response.put("targetServer", targetName);
            response.put("templateServer", templateName);
            response.put("copiedFolders", foldersToCopy);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("error", "复制失败: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
}