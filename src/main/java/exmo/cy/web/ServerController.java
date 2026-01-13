package exmo.cy.web;

import exmo.cy.model.Server;
import exmo.cy.model.ServerInstance;
import exmo.cy.service.ServerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    /**
     * 获取所有运行中的服务器
     */
    @GetMapping("/running")
    public ResponseEntity<Map<String, ServerInstance>> getRunningServers() {
        Map<String, ServerInstance> activeServers = serverService.getActiveServers();
        return ResponseEntity.ok(activeServers);
    }

    /**
     * 启动服务器
     */
    @PostMapping("/{name}/start")
    public ResponseEntity<String> startServer(
            @PathVariable String name,
            @RequestParam(defaultValue = "1") int launchMode,
            @RequestParam(defaultValue = "java") String javaPath,
            @RequestParam(required = false) String jvmArgs,
            @RequestParam(required = false) String serverArgs) {
        try {
            Optional<Server> serverOpt = serverService.getConfigManager().findServerByName(name);
            if (!serverOpt.isPresent()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("服务器不存在");
            }
            Server server = serverOpt.get();
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
            return ResponseEntity.ok("命令发送成功");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("发送命令失败: " + e.getMessage());
        }
    }

    /**
     * 创建服务器
     */
    @PostMapping("/create")
    public ResponseEntity<String> createServer(
            @RequestBody Map<String, String> request) {
        try {
            String coreName = request.get("coreName");
            String serverName = request.get("serverName");
            String description = request.get("description");

            if (coreName == null || serverName == null) {
                return ResponseEntity.badRequest().body("核心文件名和服务器名称不能为空");
            }

            serverService.createServer(coreName, serverName, description);
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
            @RequestParam(defaultValue = "false") boolean deleteFiles) {
        try {
            serverService.deleteServer(name, deleteFiles);
            return ResponseEntity.ok("服务器删除成功");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("删除失败: " + e.getMessage());
        }
    }
}