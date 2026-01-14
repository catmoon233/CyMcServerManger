/**
 * 控制台模块
 * 管理 WebSocket 连接、消息显示、命令发送等功能
 */

// 全局 WebSocket 变量
let currentServer = null;
let ws = null;
let autoScroll = true;
let reconnectAttempts = 0;
let maxReconnectAttempts = 5;
let reconnectDelay = 3000; // 3秒
let reconnectTimer = null;
let isManualDisconnect = false;

/**
 * 切换控制台连接
 */
function switchConsole() {
    const serverName = document.getElementById('serverSelect').value;
    
    // 断开当前连接
    disconnectConsole();
    
    // 等待一小段时间确保连接完全关闭
    setTimeout(() => {
        if (serverName) {
            // 连接新服务器控制台
            connectConsole(serverName);
        } else {
            // 清空控制台
            const consoleOutput = document.getElementById('consoleOutput');
            if (consoleOutput) {
                consoleOutput.innerHTML = '<div class="console-line">请选择一个运行中的服务器查看控制台</div>';
            }
            
            // 禁用控制台相关元素
            const consoleInput = document.getElementById('consoleInput');
            const sendCommandBtn = document.getElementById('sendCommandBtn');
            const clearConsoleBtn = document.getElementById('clearConsoleBtn');
            
            if (consoleInput) consoleInput.disabled = true;
            if (sendCommandBtn) sendCommandBtn.disabled = true;
            if (clearConsoleBtn) clearConsoleBtn.disabled = true;
        }
    }, 100);
}

/**
 * 连接到服务器控制台
 * @param {string} serverName - 服务器名称
 */
function connectConsole(serverName) {
    // 如果已有连接，先断开
    if (ws && ws.readyState !== WebSocket.CLOSED) {
        disconnectConsole();
    }
    
    currentServer = serverName;
    isManualDisconnect = false;
    reconnectAttempts = 0;
    
    // 清除之前的重连定时器
    if (reconnectTimer) {
        clearTimeout(reconnectTimer);
        reconnectTimer = null;
    }
    
    // 启用控制台相关元素
    document.getElementById('consoleInput').disabled = false;
    document.getElementById('sendCommandBtn').disabled = false;
    document.getElementById('clearConsoleBtn').disabled = false;
    
    // 清空当前控制台内容
    document.getElementById('consoleOutput').innerHTML = `<div class="console-line">正在连接到服务器 "${serverName}" 控制台...</div>`;
    
    // 更新连接状态
    updateWebSocketStatus('connecting', '连接中...');
    
    // 连接 WebSocket
    try {
        const wsUrl = buildWebSocketUrl(serverName);
        console.log('正在连接WebSocket:', wsUrl.replace(/token=[^&]*/, 'token=***'));
        
        // 创建 WebSocket 连接
        ws = new WebSocket(wsUrl);
        
        ws.onopen = function(event) {
            console.log('WebSocket连接已建立');
            reconnectAttempts = 0; // 重置重连计数
            updateWebSocketStatus('connected', '已连接');
            addConsoleLine(`[INFO] 已连接到服务器 "${serverName}" 控制台`, 'log-level-info');
            addConsoleLine('[INFO] 现在可以通过下方输入框向服务器发送命令', 'log-level-info');
        };
        
        ws.onmessage = function(event) {
            // 解析消息，支持JSON和纯文本
            let message = event.data;
            try {
                const data = JSON.parse(message);
                if (data.message) {
                    message = data.message;
                } else if (data.text) {
                    message = data.text;
                }
            } catch (e) {
                // 不是JSON，使用原始消息
            }
            addConsoleLine(message, 'log-level-info');
        };
        
        ws.onclose = function(event) {
            console.log('WebSocket连接已关闭:', event.code, event.reason);
            
            // 如果不是手动断开，且还有重连次数，则尝试重连
            if (!isManualDisconnect && reconnectAttempts < maxReconnectAttempts) {
                reconnectAttempts++;
                const delay = reconnectDelay * reconnectAttempts; // 递增延迟
                updateWebSocketStatus('connecting', `重连中 (${reconnectAttempts}/${maxReconnectAttempts})`);
                addConsoleLine(`[WARN] 连接已断开，${delay/1000}秒后尝试重连 (${reconnectAttempts}/${maxReconnectAttempts})...`, 'log-level-warn');
                
                reconnectTimer = setTimeout(() => {
                    if (currentServer && !isManualDisconnect) {
                        addConsoleLine(`[INFO] 正在尝试重新连接...`, 'log-level-info');
                        connectConsole(currentServer);
                    }
                }, delay);
            } else if (isManualDisconnect) {
                updateWebSocketStatus('disconnected', '已断开');
                addConsoleLine(`[INFO] 已手动断开与服务器 "${serverName}" 的连接`, 'log-level-info');
            } else {
                updateWebSocketStatus('disconnected', '连接失败');
                addConsoleLine(`[ERROR] 与服务器 "${serverName}" 的连接已断开，重连次数已达上限`, 'log-level-error');
                currentServer = null;
                // 禁用控制台相关元素
                document.getElementById('consoleInput').disabled = true;
                document.getElementById('sendCommandBtn').disabled = true;
                document.getElementById('clearConsoleBtn').disabled = true;
            }
        };
        
        ws.onerror = function(error) {
            console.error('WebSocket错误:', error);
            updateWebSocketStatus('disconnected', '连接错误');
            // WebSocket的error事件不提供详细信息，错误信息通常在onclose中
            addConsoleLine(`[ERROR] WebSocket连接发生错误`, 'log-level-error');
        };
    } catch (error) {
        console.error('创建WebSocket连接失败:', error);
        updateWebSocketStatus('disconnected', '连接失败');
        addConsoleLine(`[ERROR] 连接控制台失败: ${error.message || error}`, 'log-level-error');
        
        // 如果是因为认证问题，提示用户重新登录
        if (error.message && error.message.includes('认证')) {
            setTimeout(() => {
                showNotification('认证已过期，请重新登录', 'error');
                logout();
            }, 2000);
        }
    }
}

/**
 * 更新WebSocket连接状态显示
 */
function updateWebSocketStatus(status, text) {
    const indicator = document.getElementById('wsStatusIndicator');
    const statusText = document.getElementById('wsStatusText');
    
    if (!indicator || !statusText) return;
    
    // 移除所有状态类
    indicator.className = 'ws-status-indicator';
    
    // 添加新状态类
    if (status === 'connected') {
        indicator.classList.add('connected');
    } else if (status === 'connecting') {
        indicator.classList.add('connecting');
    } else {
        indicator.classList.add('disconnected');
    }
    
    statusText.textContent = text || getWebSocketStatus();
}

/**
 * 断开控制台连接
 */
function disconnectConsole() {
    isManualDisconnect = true;
    
    // 清除重连定时器
    if (reconnectTimer) {
        clearTimeout(reconnectTimer);
        reconnectTimer = null;
    }
    
    if (ws) {
        try {
            // 如果连接还在打开状态，正常关闭
            if (ws.readyState === WebSocket.OPEN || ws.readyState === WebSocket.CONNECTING) {
                ws.close(1000, '用户主动断开');
            }
        } catch (error) {
            console.error('关闭WebSocket连接时出错:', error);
        }
        ws = null;
    }
    
    currentServer = null;
    reconnectAttempts = 0;
    
    // 更新连接状态
    updateWebSocketStatus('disconnected', '未连接');
    
    // 禁用控制台相关元素
    const consoleInput = document.getElementById('consoleInput');
    const sendCommandBtn = document.getElementById('sendCommandBtn');
    const clearConsoleBtn = document.getElementById('clearConsoleBtn');
    
    if (consoleInput) consoleInput.disabled = true;
    if (sendCommandBtn) sendCommandBtn.disabled = true;
    if (clearConsoleBtn) clearConsoleBtn.disabled = true;
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
    
    if (!command) {
        return;
    }
    
    if (!currentServer) {
        addConsoleLine('[ERROR] 未选择服务器，无法发送命令', 'log-level-error');
        return;
    }

    // 检查WebSocket连接状态
    if (!ws) {
        addConsoleLine('[ERROR] WebSocket未初始化，正在尝试重新连接...', 'log-level-error');
        connectConsole(currentServer);
        return;
    }
    
    if (ws.readyState === WebSocket.CONNECTING) {
        addConsoleLine('[WARN] WebSocket正在连接中，请稍候...', 'log-level-warn');
        return;
    }
    
    if (ws.readyState !== WebSocket.OPEN) {
        addConsoleLine('[ERROR] WebSocket未连接，正在尝试重新连接...', 'log-level-error');
        connectConsole(currentServer);
        return;
    }

    // 显示用户输入的命令
    addConsoleLine(`[COMMAND] > ${command}`, 'log-command');
    
    try {
        // 发送带有认证信息的命令
        ws.send(JSON.stringify({ 
            command: command
        }));
        console.log('命令已发送:', command);
    } catch (error) {
        console.error('发送命令失败:', error);
        addConsoleLine(`[ERROR] 发送命令失败: ${error.message}`, 'log-level-error');
        
        // 如果发送失败，尝试重连
        if (ws.readyState !== WebSocket.OPEN) {
            connectConsole(currentServer);
        }
    }
    
    // 清空输入框并聚焦
    commandInput.value = '';
    commandInput.focus();
}

/**
 * 清空控制台
 */
function clearConsole() {
    if (confirm('确定要清空控制台吗？')) {
        document.getElementById('consoleOutput').innerHTML = '<div class="console-line">控制台已清空</div>';
        showNotification('控制台已清空', 'info');
    }
}

/**
 * 导出控制台日志
 */
function exportConsoleLogs() {
    const consoleOutput = document.getElementById('consoleOutput');
    const lines = Array.from(consoleOutput.querySelectorAll('.console-line'));
    
    if (lines.length === 0) {
        showNotification('控制台为空，没有可导出的日志', 'info');
        return;
    }
    
    let logContent = `CyMc服务器管理器 - 控制台日志导出\n`;
    logContent += `导出时间: ${new Date().toLocaleString()}\n`;
    logContent += `服务器: ${currentServer || '未选择'}\n`;
    logContent += `========================================\n\n`;
    
    lines.forEach(line => {
        const text = line.textContent || line.innerText;
        logContent += text + '\n';
    });
    
    // 创建下载链接
    const blob = new Blob([logContent], { type: 'text/plain;charset=utf-8' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = `console-${currentServer || 'logs'}-${new Date().toISOString().replace(/[:.]/g, '-')}.txt`;
    document.body.appendChild(a);
    a.click();
    document.body.removeChild(a);
    URL.revokeObjectURL(url);
    
    showNotification('日志导出成功', 'success');
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
        isManualDisconnect = true;
        if (reconnectTimer) {
            clearTimeout(reconnectTimer);
        }
        disconnectConsole();
    });
    
    // 页面可见性变化时处理连接
    document.addEventListener('visibilitychange', function() {
        if (document.hidden) {
            // 页面隐藏时，可以选择保持连接或断开
            // 这里保持连接，以便用户切换回来时继续接收日志
        } else {
            // 页面重新可见时，检查连接状态
            if (currentServer && ws && ws.readyState !== WebSocket.OPEN) {
                console.log('页面重新可见，检查WebSocket连接状态');
                if (ws.readyState === WebSocket.CLOSED) {
                    // 如果连接已关闭，尝试重连
                    connectConsole(currentServer);
                }
            }
        }
    });
}

/**
 * 获取WebSocket连接状态
 * @returns {string} 连接状态描述
 */
function getWebSocketStatus() {
    if (!ws) {
        return '未连接';
    }
    
    switch (ws.readyState) {
        case WebSocket.CONNECTING:
            return '连接中...';
        case WebSocket.OPEN:
            return '已连接';
        case WebSocket.CLOSING:
            return '正在关闭...';
        case WebSocket.CLOSED:
            return '已断开';
        default:
            return '未知状态';
    }
}
