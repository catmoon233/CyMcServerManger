# 迁移指南：从v1.0到v2.0

本文档帮助您从旧版本的CyMc服务器管理器迁移到全新重构的v2.0版本。

## 重大变更

### 1. 主类变更

**旧版本：**
```java
exmo.cy.ServerManager
```

**新版本：**
```java
exmo.cy.ServerManagerApp
```

### 2. 项目结构变更

v2.0采用了全新的模块化架构：

```
旧版本：所有代码在一个文件中
新版本：分层模块化架构
├── command/     # 命令处理
├── config/      # 配置管理
├── exception/   # 异常处理
├── model/       # 数据模型
├── service/     # 业务逻辑
└── util/        # 工具类
```

## 配置文件兼容性

### serverList.json

**完全兼容**，无需修改。新版本可以直接读取旧版本的配置文件。

### lastLaunch.json

**完全兼容**，但新版本增加了更多字段：
- `javaPath` - Java路径（新增）
- `jvmArgs` - JVM参数（自定义模式）
- `serverArgs` - 服务器参数（自定义模式）

## 功能变更

### 新增功能

1. **改进的异常处理**
   - 更详细的错误信息
   - 更好的错误恢复机制
   - 完善的日志记录

2. **增强的进程管理**
   - 更可靠的进程启动
   - 异步输出处理
   - 优雅的进程关闭

3. **完善的资源管理**
   - 自动资源清理
   - 防止内存泄漏
   - 线程安全保证

### 改进的功能

1. **服务器启动**
   - 更智能的Java路径检测
   - 改进的参数验证
   - 更好的错误提示

2. **命令处理**
   - 更清晰的命令结构
   - 更好的输入验证
   - 改进的用户反馈

3. **文件操作**
   - 更安全的文件处理
   - 更好的错误恢复
   - 防止路径遍历攻击

## 迁移步骤

### 步骤1：备份数据

在迁移前，请备份以下内容：
```bash
备份目录：
- serverList.json
- lastLaunch.json
- servers/
- maps/
- backups/
```

### 步骤2：更新代码

1. 拉取最新代码
2. 重新编译项目：
   ```bash
   gradlew.bat clean build
   ```

### 步骤3：验证配置

启动新版本，验证配置文件是否正确加载：
```bash
gradlew.bat run
```

输入 `list` 命令，检查所有服务器是否正确显示。

### 步骤4：测试功能

1. 测试服务器启动
2. 测试服务器控制台连接
3. 测试服务器停止

## 代码更新指南

如果您基于旧版本进行了自定义开发，请参考以下指南：

### 创建服务器

**旧版本：**
```java
// 所有逻辑在一个方法中
createServer(scanner);
```

**新版本：**
```java
// 使用服务层
ServerService serverService = new ServerService();
Server server = serverService.createServer(coreName, serverName, description);
```

### 启动服务器

**旧版本：**
```java
// 直接创建ProcessBuilder
ProcessBuilder pb = new ProcessBuilder();
Process process = pb.start();
```

**新版本：**
```java
// 使用服务层和配置管理
ServerService serverService = new ServerService();
ServerInstance instance = serverService.startServer(
    server, launchMode, javaPath, jvmArgs, serverArgs
);
```

### 配置管理

**旧版本：**
```java
// 直接操作文件
Gson gson = new Gson();
BufferedReader reader = Files.newBufferedReader(path);
Server[] servers = gson.fromJson(reader, Server[].class);
```

**新版本：**
```java
// 使用配置管理器
ConfigurationManager configManager = new ConfigurationManager();
List<Server> servers = configManager.loadServers();
Optional<Server> server = configManager.findServerByName(name);
```

## API变更

### 模型类

**Server类：**
- 新增 `isValid()` 方法
- 新增 `equals()` 和 `hashCode()` 方法
- 改进的字段封装

**ServerInstance类：**
- 新增 `isRunning()` 方法
- 新增 `getUptime()` 方法
- 更好的生命周期管理

### 异常处理

**旧版本：**
```java
try {
    // 操作
} catch (Exception e) {
    System.out.println("错误: " + e.getMessage());
}
```

**新版本：**
```java
try {
    // 操作
} catch (ServerOperationException e) {
    Logger.error("操作失败", e);
} catch (ConfigurationException e) {
    Logger.error("配置错误", e);
}
```

## 性能改进

### 1. 并发性能

- 使用 `ConcurrentHashMap` 管理活动服务器
- 线程安全的操作
- 更好的并发控制

### 2. I/O性能

- 异步输出处理
- 缓冲I/O操作
- 优化的文件操作

### 3. 内存使用

- 及时释放资源
- 防止内存泄漏
- 优化的对象创建

## 故障排除

### 问题1：找不到主类

**原因：** 构建配置未更新

**解决方案：**
```bash
gradlew.bat clean build
```

### 问题2：配置文件加载失败

**原因：** JSON格式错误

**解决方案：**
1. 验证JSON格式
2. 检查字段名称
3. 查看日志详细信息

### 问题3：服务器启动失败

**原因：** 多种可能

**解决方案：**
1. 检查Java路径
2. 验证核心文件存在
3. 查看详细日志
4. 检查端口占用

## 回滚指南

如果需要回退到旧版本：

1. 恢复旧版本代码
2. 恢复配置文件备份
3. 重新编译项目

**注意：** 新版本的配置文件可能包含旧版本不支持的字段，但这不会影响旧版本的运行。

## 获取帮助

如果在迁移过程中遇到问题：

1. 查看 README.md 文档
2. 检查日志文件
3. 提交 Issue 到 GitHub
4. 联系开发团队

## 总结

v2.0是一次全面的重构，带来了：
- ✅ 更好的代码结构
- ✅ 更强的错误处理
- ✅ 更高的性能
- ✅ 更好的可维护性
- ✅ 完全向后兼容的配置

建议尽快迁移到新版本以获得最佳体验！