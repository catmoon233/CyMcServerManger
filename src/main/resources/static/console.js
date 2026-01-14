/**
 * 控制台模块
 * 管理 WebSocket 连接、消息显示、命令发送等功能
 */

// 全局 WebSocket 变量
let currentServer = null;
let ws = null;
let autoScroll = true;

/**
 * 切换控制台连接
 */
function switchConsole() {
    const serverName = document.getElementById('serverSelect').value;
    
    // 断开当前连接
    disconnectConsole();
    
    if (serverName) {
        // 连接新服务器控制台
        connectConsole(serverName);
    } else {
        // 清空控制台
        document.getElementById('consoleOutput').innerHTML = '<div class="console-line">请选择一个运行中的服务器查看控制台</div>';
    }
}

/**
 * 连接到服务器控制台
 * @param {string} serverName - 服务器名称
 */
function connectConsole(serverName) {
    currentServer = serverName;
    
    // 启用控制台相关元素
    document.getElementById('consoleInput').disabled = false;
    document.getElementById('sendCommandBtn').disabled = false;
    document.getElementById('clearConsoleBtn').disabled = false;
    
    // 清空当前控制台内容
    document.getElementById('consoleOutput').innerHTML = `<div class="console-line">正在连接到服务器 "${serverName}" 控制台...</div>`;
    
    // 连接 WebSocket
    const wsUrl = buildWebSocketUrl(serverName);
    
    try {
        // 创建 WebSocket 连接
        ws = new WebSocket(wsUrl);
        
        ws.onopen = function(event) {
            addConsoleLine(`[INFO] 已连接到服务器 "${serverName}" 控制台`, 'log-level-info');
            addConsoleLine('[INFO] 现在可以通过下方输入框向服务器发送命令', 'log-level-info');
        };
        
        ws.onmessage = function(event) {
            addConsoleLine(`[CONSOLE] ${event.data}`, 'log-level-info');
        };
        
        ws.onclose = function(event) {
            addConsoleLine(`[INFO] 与服务器 "${serverName}" 的连接已断开`, 'log-level-warn');
            currentServer = null;
        };
        
        ws.onerror = function(error) {
            addConsoleLine(`[ERROR] WebSocket连接错误: ${error.message || error}`, 'log-level-error');
        };
    } catch (error) {
        addConsoleLine(`[ERROR] 连接控制台失败: ${error.message || error}`, 'log-level-error');
    }
}

/**
 * 断开控制台连接
 */
function disconnectConsole() {
    if (ws) {
        ws.close();
        ws = null;
    }
    currentServer = null;
    
    // 禁用控制台相关元素
    document.getElementById('consoleInput').disabled = true;
    document.getElementById('sendCommandBtn').disabled = true;
    document.getElementById('clearConsoleBtn').disabled = true;
}

/**
 * 添加控制台行
 * @param {string} message - 消息内容
 * @param {string} level - 日志级别 (log-level-info, log-level-warn, log-level-error, log-command)
 */
function addConsoleLine(message, level = 'log-level-info') {
    const consoleOutput = document.getElementById('consoleOutput');
    const timestamp = new Date().toLocaleTimeString();
    
    const lineDiv = document.createElement('div');
    lineDiv.className = `console-line log-entry`;
    lineDiv.innerHTML = `<span class="log-timestamp">[${timestamp}]</span><span class="${level}"> ${message}</span>`;
    
    consoleOutput.appendChild(lineDiv);
    
    // 自动滚动到底部
    if (autoScroll) {
        consoleOutput.scrollTop = consoleOutput.scrollHeight;
    }
}

/**
 * 发送命令到服务器
 */
function sendCommand() {
    const commandInput = document.getElementById('consoleInput');
    const command = commandInput.value.trim();
    
    if (!command || !currentServer) {
        return;
    }

    // 显示用户输入的命令
    addConsoleLine(`[COMMAND] > ${command}`, 'log-command');
    
    // 发送命令到服务器
    if (ws && ws.readyState === WebSocket.OPEN) {
        // 发送带有认证信息的命令
        ws.send(JSON.stringify({ 
            command: command
        }));
    } else {
        addConsoleLine('[ERROR] WebSocket未连接，无法发送命令', 'log-level-error');
    }
    
    // 清空输入框并聚焦
    commandInput.value = '';
    commandInput.focus();
}

/**
 * 清空控制台
 */
function clearConsole() {
    document.getElementById('consoleOutput').innerHTML = '<div class="console-line">控制台已清空</div>';
}

/**
 * 切换自动滚动
 */
function toggleAutoScroll() {
    autoScroll = !autoScroll;
    const btn = document.getElementById('autoScrollBtn');
    btn.innerHTML = autoScroll ? 
        '<i class="fas fa-arrow-down"></i> 自动滚动' : 
        '<i class="fas fa-pause"></i> 手动滚动';
        
    if (autoScroll) {
        const consoleOutput = document.getElementById('consoleOutput');
        consoleOutput.scrollTop = consoleOutput.scrollHeight;
    }
}

/**
 * 初始化控制台事件处理
 */
function initializeConsoleHandlers() {
    // 服务器选择下拉菜单改变事件
    document.getElementById('serverSelect').addEventListener('change', switchConsole);
    
    // 发送按钮点击事件
    document.getElementById('sendCommandBtn').addEventListener('click', sendCommand);
    
    // 清空按钮点击事件
    document.getElementById('clearConsoleBtn').addEventListener('click', clearConsole);
    
    // 自动滚动按钮点击事件
    document.getElementById('autoScrollBtn').addEventListener('click', toggleAutoScroll);
    
    // 控制台输入框键盘事件
    document.getElementById('consoleInput').addEventListener('keypress', function(e) {
        if (e.key === 'Enter') {
            sendCommand();
        }
    });
}

/**
 * 页面卸载时断开连接
 */
function setupUnloadHandler() {
    window.addEventListener('beforeunload', function() {
        disconnectConsole();
    });
}
