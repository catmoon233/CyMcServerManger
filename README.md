# CyMc服务器管理器 v2.0

高性能，现代化的Minecraft服务器管理核心

## 项目概述

CyMc服务器管理器是一个用Java开发的服务器管理工具，提供命令行和Web界面两种操作方式，用于管理多个Minecraft服务器实例。该项目经过全面重构，采用现代化的设计模式和最佳实践。

## 主要特性

- 🚀 **多服务器管理** - 同时管理多个Minecraft服务器实例
- 🔄 **核心版本切换** - 轻松切换服务器核心版本
- 🗺️ **地图管理** - 支持地图备份和切换
- ☕ **Java版本选择** - 自动检测并选择Java版本
- 💾 **配置持久化** - 保存服务器配置和启动参数
- 🎮 **服务器控制台** - 连接到运行中的服务器进行交互
- 📦 **预设支持** - 支持服务器预设配置
- 🔒 **异常处理** - 完善的错误处理和恢复机制
- 🌐 **Web界面** - 提供现代化的Web管理界面
- ⚡ **实时监控** - WebSocket实时传输服务器日志
- 📱 **响应式设计** - 支持移动端访问

## 项目结构

```
CyMcServerManger/
├── src/main/java/exmo/cy/
│   ├── ServerManagerApp.java          # 主程序入口
│   ├── command/                       # 命令处理模块
│   │   └── CommandHandler.java        # 命令处理器
│   ├── web/                          # Web界面模块
│   │   ├── WebApplication.java       # Spring Boot应用入口
│   │   ├── ServerController.java     # REST API控制器
│   │   ├── WebSocketConfig.java      # WebSocket配置
│   │   └── LogWebSocketHandler.java  # 日志WebSocket处理器
│   ├── config/                        # 配置模块
│   │   └── Constants.java             # 常量定义
│   ├── exception/                     # 异常处理模块
│   │   ├── ServerManagerException.java
│   │   ├── ConfigurationException.java
│   │   └── ServerOperationException.java
│   ├── model/                         # 数据模型
│   │   ├── Server.java                # 服务器配置模型
│   │   ├── LaunchConfig.java          # 启动配置模型
│   │   └── ServerInstance.java        # 服务器实例模型
│   ├── service/                       # 业务逻辑层
│   │   ├── ConfigurationManager.java  # 配置管理器
│   │   ├── ProcessManager.java        # 进程管理器
│   │   └── ServerService.java         # 服务器服务
│   └── util/                          # 工具类
│       ├── Logger.java                # 日志工具
│       ├── FileUtils.java             # 文件工具
│       └── JavaPathFinder.java        # Java路径查找工具
├── cores/                             # 服务器核心文件目录
├── servers/                           # 服务器实例目录
├── maps/                              # 地图文件目录
├── backups/                           # 备份目录
├── preset/                            # 预设配置目录
└── build.gradle                       # Gradle构建配置
```

## 快速开始

### 环境要求

- Java 8 或更高版本
- Gradle 7.0 或更高版本

### 构建项目

```bash
# Windows
gradlew.bat build

# Linux/Mac
./gradlew build
```

### 运行程序

#### 命令行模式

```bash
# 使用Gradle运行
gradlew.bat run

# 或直接运行JAR文件
java -jar build/libs/CyMcServerManger-1.0-SNAPSHOT-all.jar
```

#### Web模式

```bash
# 使用Gradle运行Web模式
gradlew.bat run --args="-web"

# 或直接运行JAR文件
java -jar build/libs/CyMcServerManger-1.0-SNAPSHOT-all.jar -web
```

Web界面访问地址：http://localhost:8080

## 使用指南

### Web界面

Web界面提供了直观的图形化操作，支持以下功能：

1. **服务器列表** - 显示所有服务器的状态和基本信息
2. **启动/停止** - 一键启动或停止服务器
3. **控制台** - 实时查看服务器控制台输出和发送命令
4. **创建服务器** - 快速创建新的服务器实例
5. **删除服务器** - 删除服务器配置和文件
6. **实时监控** - 服务器状态自动刷新

### 基本命令

- `help` - 显示帮助信息
- `create` - 创建新服务器
- `add` - 添加现有服务器目录
- `list` - 列出所有服务器
- `start` - 启动服务器
- `last` - 使用上次的参数启动服务器
- `stop` - 退出程序

### 服务器管理

- `switch` - 切换服务器核心版本
- `map` - 切换服务器地图
- `delete` - 删除服务器

### 运行时管理

- `list-running` 或 `lr` - 列出运行中的服务器
- `attach` 或 `at` - 连接到服务器控制台
- `detach` - 从服务器控制台返回主控制台
- `stop-server` 或 `ss` - 正常停止服务器
- `force-stop` - 强制终止服务器（在服务器控制台中使用）

## 启动模式

1. **核心版本启动** - 使用优化的JVM参数启动
2. **整合包启动** - 使用整合包配置文件启动
3. **基础核心版本** - 基础启动模式
4. **基础核心版本(修复)** - 包含Paper修复的基础模式
5. **自定义启动模式** - 自定义JVM和服务器参数

## 架构设计

### 分层架构

- **表示层** - 命令处理和用户交互
- **业务逻辑层** - 服务器管理、配置管理、进程管理
- **数据访问层** - 配置文件读写、文件操作
- **工具层** - 通用工具类和辅助功能

### 设计模式

- **单例模式** - 配置管理器
- **工厂模式** - 服务器实例创建
- **策略模式** - 不同的启动模式
- **观察者模式** - 进程状态监控

## 技术亮点

### 1. 现代化代码实现

- 使用Java 8+ 特性（Lambda、Stream API、Optional等）
- 异步进程管理
- 类型安全的配置处理

### 2. 完善的异常处理

- 自定义异常体系
- 优雅的错误恢复机制
- 详细的日志记录

### 3. 资源管理

- Try-with-resources 自动资源管理
- 线程安全的并发控制
- 防止资源泄漏

### 4. 代码质量

- 清晰的代码结构
- 完整的注释文档
- 遵循Java编码规范

## 配置文件

### serverList.json

存储所有服务器的配置信息：

```json
[
  {
    "name": "server1",
    "corePath": "servers/server1/Core.jar",
    "version": "1.20.1",
    "description": "我的服务器",
    "isModpack": false,
    "map": "world1"
  }
]
```

### lastLaunch.json

保存最后一次启动的配置：

```json
{
  "serverName": "server1",
  "launchMode": 1,
  "javaPath": "java",
  "jvmArgs": "-Xms2G -Xmx4G",
  "serverArgs": "-nogui"
}
```

## 开发指南

### 添加新命令

1. 在 `CommandHandler.java` 中添加命令处理方法
2. 在 `handleCommand()` 方法的 switch 语句中注册命令
3. 更新 `handleHelp()` 方法添加命令说明

### 扩展功能

项目采用模块化设计，可以轻松扩展：

- 添加新的服务类到 `service` 包
- 创建新的工具类到 `util` 包
- 定义新的数据模型到 `model` 包

## 故障排除

### 常见问题

1. **找不到Java** - 确保Java已正确安装并配置在PATH中
2. **服务器启动失败** - 检查核心文件是否存在，端口是否被占用
3. **配置文件损坏** - 删除配置文件重新创建服务器

## 性能优化

- 使用 ConcurrentHashMap 管理活动服务器
- 异步I/O处理进程输出
- 优化的文件操作
- 高效的资源管理

## 安全性

- 输入验证和清理
- ZIP路径遍历攻击防护
- 安全的进程管理
- 配置文件完整性检查

## 贡献指南

欢迎提交问题和拉取请求！

## 许可证

本项目采用 MIT 许可证。

## 更新日志

### v2.0 (2026-01-13)

- 🎉 完全重写项目架构
- ✨ 采用现代化设计模式
- 🐛 修复所有已知bug
- 📚 添加完整文档
- 🔧 改进错误处理
- ⚡ 性能优化
- 🎨 代码规范化
- 🌐 添加Web管理界面
- ⚡ 实现WebSocket实时日志传输
- 📱 响应式设计支持移动端

### v1.0

- 初始版本

## 联系方式

- 项目地址：https://github.com/your-username/CyMcServerManger
- 问题反馈：https://github.com/your-username/CyMcServerManger/issues

---

**注意：** 本项目仍在持续开发中，欢迎反馈和建议！