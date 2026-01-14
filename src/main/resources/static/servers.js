/**
 * 服务器管理模块
 * 管理服务器的加载、启动、停止、删除等操作
 */

/**
 * 加载服务器列表
 */
async function loadServers() {
    try {
        const servers = await apiLoadServers();
        const runningServersData = await apiLoadRunningServers();
        
        if (!servers || !runningServersData) return;
        
        updateStats(servers, runningServersData);
        renderServerList(servers, runningServersData);
        updateServerSelect(servers, runningServersData);
        
    } catch (error) {
        console.error('加载服务器列表失败:', error);
        showNotification('加载服务器列表失败: ' + error.message, 'error');
    }
}

// 存储上一次的统计数据用于趋势计算
let previousStats = {
    totalServers: 0,
    runningServers: 0,
    totalPlayers: 0,
    avgUptime: 0
};

/**
 * 更新统计信息
 * @param {Array} servers - 服务器数组
 * @param {object} runningServers - 运行中的服务器对象
 */
function updateStats(servers, runningServers) {
    const totalServers = servers.length;
    const runningServersCount = Object.keys(runningServers).length;
    
    // 计算在线玩家数量
    let totalPlayers = 0;
    let totalUptime = 0;
    let runningCount = 0;
    
    for (const serverName in runningServers) {
        const server = runningServers[serverName];
        totalPlayers += server.playerCount || 0;
        if (server.uptime) {
            totalUptime += server.uptime;
            runningCount++;
        }
    }
    
    const avgUptime = runningCount > 0 ? Math.floor(totalUptime / runningCount / 1000 / 60 / 60) : 0;
    
    // 更新数字显示
    animateNumber('totalServers', previousStats.totalServers, totalServers);
    animateNumber('runningServers', previousStats.runningServers, runningServersCount);
    animateNumber('totalPlayers', previousStats.totalPlayers, totalPlayers);
    updateUptimeDisplay(avgUptime);
    
    // 更新进度条
    const maxServers = Math.max(totalServers, 10); // 至少10个作为最大值
    updateCircularProgress('totalServersProgress', (totalServers / maxServers) * 100);
    updateCircularProgress('runningServersProgress', totalServers > 0 ? (runningServersCount / totalServers) * 100 : 0);
    updateCircularProgress('totalPlayersProgress', Math.min((totalPlayers / 100) * 100, 100)); // 假设100为最大值
    
    // 更新趋势
    updateTrend('totalServersTrend', previousStats.totalServers, totalServers);
    updateTrend('runningServersTrend', previousStats.runningServers, runningServersCount);
    updateTrend('totalPlayersTrend', previousStats.totalPlayers, totalPlayers);
    
    // 更新平均运行时间进度条
    updateLinearProgress('uptimeProgressBar', Math.min((avgUptime / 24) * 100, 100)); // 24小时为最大值
    
    // 保存当前统计数据
    previousStats = {
        totalServers,
        runningServers: runningServersCount,
        totalPlayers,
        avgUptime
    };
}

/**
 * 数字动画
 */
function animateNumber(elementId, from, to) {
    const element = document.getElementById(elementId);
    if (!element) return;
    
    const duration = 500;
    const startTime = performance.now();
    const difference = to - from;
    
    function update(currentTime) {
        const elapsed = currentTime - startTime;
        const progress = Math.min(elapsed / duration, 1);
        const easeOutQuart = 1 - Math.pow(1 - progress, 4);
        const current = Math.floor(from + difference * easeOutQuart);
        
        element.textContent = current;
        
        if (progress < 1) {
            requestAnimationFrame(update);
        } else {
            element.textContent = to;
        }
    }
    
    requestAnimationFrame(update);
}

/**
 * 更新圆形进度条
 */
function updateCircularProgress(elementId, percentage) {
    const element = document.getElementById(elementId);
    if (!element) return;
    
    const circle = element.querySelector('.progress-ring-circle');
    const text = element.querySelector('.progress-text');
    if (!circle || !text) return;
    
    const circumference = 2 * Math.PI * 26; // r = 26
    const offset = circumference - (percentage / 100) * circumference;
    
    circle.style.strokeDashoffset = offset;
    circle.classList.add('progress');
    text.textContent = Math.round(percentage) + '%';
    
    // 添加渐变定义
    if (!element.querySelector('defs')) {
        const svg = element.querySelector('svg');
        const defs = document.createElementNS('http://www.w3.org/2000/svg', 'defs');
        const gradient = document.createElementNS('http://www.w3.org/2000/svg', 'linearGradient');
        gradient.setAttribute('id', 'progressGradient');
        gradient.setAttribute('x1', '0%');
        gradient.setAttribute('y1', '0%');
        gradient.setAttribute('x2', '100%');
        gradient.setAttribute('y2', '100%');
        
        const stop1 = document.createElementNS('http://www.w3.org/2000/svg', 'stop');
        stop1.setAttribute('offset', '0%');
        stop1.setAttribute('stop-color', '#3b82f6');
        
        const stop2 = document.createElementNS('http://www.w3.org/2000/svg', 'stop');
        stop2.setAttribute('offset', '100%');
        stop2.setAttribute('stop-color', '#8b5cf6');
        
        gradient.appendChild(stop1);
        gradient.appendChild(stop2);
        defs.appendChild(gradient);
        svg.insertBefore(defs, svg.firstChild);
    }
}

/**
 * 更新线性进度条
 */
function updateLinearProgress(elementId, percentage) {
    const element = document.getElementById(elementId);
    if (!element) return;
    
    element.style.width = Math.min(percentage, 100) + '%';
}

/**
 * 更新趋势显示
 */
function updateTrend(elementId, previous, current) {
    const element = document.getElementById(elementId);
    if (!element) return;
    
    const trendElement = element.closest('.stat-trend');
    if (!trendElement) return;
    
    const difference = current - previous;
    const icon = trendElement.querySelector('i');
    
    if (difference > 0) {
        trendElement.className = 'stat-trend';
        icon.className = 'fas fa-arrow-up';
        element.textContent = '+' + difference;
    } else if (difference < 0) {
        trendElement.className = 'stat-trend down';
        icon.className = 'fas fa-arrow-down';
        element.textContent = difference;
    } else {
        trendElement.className = 'stat-trend neutral';
        icon.className = 'fas fa-minus';
        element.textContent = '0';
    }
}

/**
 * 更新运行时间显示
 */
function updateUptimeDisplay(hours) {
    const element = document.getElementById('avgUptime');
    if (!element) return;
    
    if (hours < 1) {
        element.textContent = '<1h';
    } else if (hours < 24) {
        element.textContent = hours + 'h';
    } else {
        const days = Math.floor(hours / 24);
        const remainingHours = hours % 24;
        element.textContent = days + 'd ' + remainingHours + 'h';
    }
}

/**
 * 更新服务器选择下拉菜单
 * @param {Array} servers - 服务器数组
 * @param {object} runningServers - 运行中的服务器对象
 */
function updateServerSelect(servers, runningServers) {
    const select = document.getElementById('serverSelect');
    const currentSelected = select.value; // 保存当前选中的值
    const options = [];
    
    // 收集所有运行中的服务器
    for (const serverName in runningServers) {
        options.push(serverName);
    }
    
    // 仅当选项真的发生变化时才更新DOM（避免闪烁）
    const currentOptions = Array.from(select.options).slice(1).map(o => o.value);
    const hasChanged = currentOptions.length !== options.length || 
                     !currentOptions.every(o => options.includes(o));
    
    if (hasChanged) {
        // 清空现有选项，保留第一个空白选项
        while (select.options.length > 1) {
            select.remove(1);
        }
        
        // 添加运行中的服务器
        options.forEach(serverName => {
            const option = document.createElement('option');
            option.value = serverName;
            option.textContent = `${serverName} (运行中)`;
            select.appendChild(option);
        });
    }
    
    // 尝试恢复之前选中的值
    if (currentSelected && runningServers[currentSelected]) {
        select.value = currentSelected;
    } else if (select.value && !runningServers[select.value]) {
        // 如果当前选中的服务器不在运行中，重置选择
        select.value = '';
        disconnectConsole();
    }
    
    // 启用下拉菜单
    select.disabled = false;
}

/**
 * 渲染服务器列表
 * @param {Array} servers - 服务器数组
 * @param {object} runningServers - 运行中的服务器对象
 */
function renderServerList(servers, runningServers) {
    const serverList = document.getElementById('serverList');
    serverList.innerHTML = '';

    if (servers.length === 0) {
        serverList.innerHTML = `
            <div style="text-align: center; padding: 40px; color: #6c757d;">
                <i class="fas fa-server" style="font-size: 48px; margin-bottom: 15px;"></i>
                <h3>暂无服务器</h3>
                <p>点击右上角"创建服务器"按钮开始创建您的第一个服务器</p>
            </div>
        `;
        return;
    }

    servers.forEach(server => {
        const isRunning = runningServers[server.name] !== undefined;
        const runningInfo = runningServers[server.name];
        const serverCard = createServerCard(server, isRunning, runningInfo);
        serverList.appendChild(serverCard);
    });
}

/**
 * 创建服务器卡片
 * @param {object} server - 服务器数据
 * @param {boolean} isRunning - 是否运行中
 * @param {object} runningInfo - 运行信息
 * @returns {HTMLElement}
 */
function createServerCard(server, isRunning, runningInfo) {
    const div = document.createElement('div');
    div.className = `server-card ${isRunning ? 'running' : 'stopped'}`;

    // 获取服务器状态详细信息
    let uptime = 'N/A';
    let playerCount = 'N/A';
    let memoryUsage = 'N/A';
    
    if (runningInfo) {
        uptime = runningInfo.uptime ? Math.floor(runningInfo.uptime / 1000) + 's' : 'N/A';
        playerCount = runningInfo.playerCount || '0';
        memoryUsage = runningInfo.memoryUsage || 'N/A';
    }

    div.innerHTML = `
        <div class="server-header">
            <div class="server-name">${server.name}</div>
            <div class="server-status status-${isRunning ? 'running' : 'stopped'}">
                <i class="fas fa-${isRunning ? 'play' : 'stop'}"></i>
                ${isRunning ? '运行中' : '已停止'}
            </div>
        </div>
        
        <div class="server-details">
            <div class="detail-item">
                <i class="fas fa-tag"></i>
                <span>版本: ${server.version || '未知'}</span>
            </div>
            <div class="detail-item">
                <i class="fas fa-align-left"></i>
                <span>描述: ${server.description || '无描述'}</span>
            </div>
            <div class="detail-item">
                <i class="fas fa-folder"></i>
                <span>路径: ${server.corePath || '未设置'}</span>
            </div>
            <div class="detail-item">
                <i class="fas fa-map-marker-alt"></i>
                <span>地图: ${server.map || '未设置'}</span>
            </div>
        </div>
        
        <div class="server-stats">
            <div class="stat-badge">
                <i class="fas fa-users"></i>
                <span class="player-count">玩家: ${playerCount}</span>
            </div>
            <div class="stat-badge">
                <i class="fas fa-microchip"></i>
                <span class="memory-usage">内存: ${memoryUsage}</span>
            </div>
            <div class="stat-badge">
                <i class="fas fa-clock"></i>
                <span class="uptime">运行时间: ${uptime}</span>
            </div>
        </div>
        
        <div class="server-actions">
            ${isRunning ? `
                <button class="btn btn-danger" onclick="stopServer('${server.name}')">
                    <i class="fas fa-stop"></i> 停止
                </button>
                <button class="btn btn-warning" onclick="forceStopServer('${server.name}')">
                    <i class="fas fa-exclamation-triangle"></i> 强制停止
                </button>
                <button class="btn btn-primary" onclick="connectToConsole('${server.name}')">
                    <i class="fas fa-terminal"></i> 控制台
                </button>
            ` : `
                <button class="btn btn-success" onclick="startServer('${server.name}')">
                    <i class="fas fa-play"></i> 启动
                </button>
            `}
            <button class="btn btn-outline" onclick="viewDetails('${server.name}')">
                <i class="fas fa-info-circle"></i> 详情
            </button>
            <button class="btn btn-danger" onclick="deleteServer('${server.name}')">
                <i class="fas fa-trash"></i> 删除
            </button>
        </div>
    `;

    return div;
}

/**
 * 启动服务器
 * @param {string} serverName - 服务器名称
 */
async function startServer(serverName) {
    if (!confirm(`确定要启动服务器 "${serverName}" 吗？`)) return;

    try {
        showNotification(`正在启动服务器 "${serverName}"...`, 'info');
        
        // 显示加载状态
        const startBtn = event?.target?.closest('.btn-success');
        if (startBtn) {
            startBtn.classList.add('loading');
            startBtn.disabled = true;
        }
        
        const result = await apiStartServer(serverName);
        
        // 移除加载状态
        if (startBtn) {
            startBtn.classList.remove('loading');
            startBtn.disabled = false;
        }
        
        if (result && result !== 'null' && result !== '') {
            showNotification(`服务器 "${serverName}" 启动成功！`, 'success');
        } else {
            const errorMsg = typeof result === 'string' && result !== 'null' ? result : '启动失败，请检查服务器配置和日志';
            showNotification(errorMsg, 'error');
        }
        
        // 延迟刷新，给服务器启动时间
        setTimeout(() => {
            loadServers();
        }, 2000);
    } catch (error) {
        console.error('启动服务器失败:', error);
        showNotification(`启动服务器失败: ${error.message || '网络错误'}`, 'error');
        
        // 移除加载状态
        const startBtn = event?.target?.closest('.btn-success');
        if (startBtn) {
            startBtn.classList.remove('loading');
            startBtn.disabled = false;
        }
    }
}

/**
 * 停止服务器
 * @param {string} serverName - 服务器名称
 */
async function stopServer(serverName) {
    if (!confirm(`确定要停止服务器 "${serverName}" 吗？\n服务器将正常关闭，玩家数据会保存。`)) return;

    try {
        showNotification(`正在停止服务器 "${serverName}"...`, 'info');
        
        const result = await apiStopServer(serverName);
        
        if (result && result !== 'null' && result !== '') {
            showNotification(`服务器 "${serverName}" 停止成功！`, 'success');
        } else {
            const errorMsg = typeof result === 'string' && result !== 'null' ? result : '停止失败，服务器可能已经停止';
            showNotification(errorMsg, 'error');
        }
        
        // 延迟刷新
        setTimeout(() => {
            loadServers();
        }, 1500);
    } catch (error) {
        console.error('停止服务器失败:', error);
        showNotification(`停止服务器失败: ${error.message || '网络错误'}`, 'error');
    }
}

/**
 * 强制停止服务器
 * @param {string} serverName - 服务器名称
 */
async function forceStopServer(serverName) {
    const confirmed = confirm(
        `⚠️ 警告：强制停止服务器\n\n` +
        `服务器: ${serverName}\n` +
        `此操作将立即终止服务器进程，可能导致：\n` +
        `- 玩家数据未保存\n` +
        `- 世界数据损坏\n` +
        `- 插件数据丢失\n\n` +
        `确定要继续吗？`
    );
    
    if (!confirmed) return;

    try {
        showNotification(`正在强制停止服务器 "${serverName}"...`, 'warning');
        
        const result = await apiForceStopServer(serverName);
        
        if (result && result !== 'null' && result !== '') {
            showNotification(`服务器 "${serverName}" 强制停止成功！`, 'success');
        } else {
            const errorMsg = typeof result === 'string' && result !== 'null' ? result : '强制停止失败，请检查服务器状态';
            showNotification(errorMsg, 'error');
        }
        
        // 延迟刷新
        setTimeout(() => {
            loadServers();
        }, 1000);
    } catch (error) {
        console.error('强制停止服务器失败:', error);
        showNotification(`强制停止服务器失败: ${error.message || '网络错误'}`, 'error');
    }
}

/**
 * 删除服务器
 * @param {string} serverName - 服务器名称
 */
async function deleteServer(serverName) {
    if (!confirm(`确定要删除服务器 "${serverName}" 吗？此操作不可恢复！`)) return;

    try {
        showNotification(`正在删除服务器 "${serverName}"...`, 'info');
        
        const result = await apiDeleteServer(serverName);
        
        if (result) {
            showNotification(`服务器 "${serverName}" 删除成功！`, 'success');
        } else {
            const errorMsg = typeof result === 'string' ? result : '删除失败，请检查服务器是否正在运行';
            showNotification(errorMsg, 'error');
        }
        
        loadServers(); // 刷新服务器列表
    } catch (error) {
        console.error('删除服务器失败:', error);
        showNotification(`删除服务器失败: ${error.message}`, 'error');
    }
}

/**
 * 连接到控制台
 * @param {string} serverName - 服务器名称
 */
function connectToConsole(serverName) {
    // 更新下拉菜单选择
    document.getElementById('serverSelect').value = serverName;
    switchConsole();
}

/**
 * 查看服务器详情
 * @param {string} serverName - 服务器名称
 */
async function viewDetails(serverName) {
    try {
        // 获取服务器列表
        const servers = await apiLoadServers();
        const runningServers = await apiLoadRunningServers();
        
        const server = servers.find(s => s.name === serverName);
        if (!server) {
            showNotification('服务器不存在', 'error');
            return;
        }
        
        const runningInfo = runningServers[serverName];
        const isRunning = runningInfo !== undefined;
        
        // 构建详情HTML
        let detailsHtml = `
            <div class="server-details-modal">
                <div class="detail-section">
                    <h3><i class="fas fa-info-circle"></i> 基本信息</h3>
                    <div class="detail-grid">
                        <div class="detail-row">
                            <span class="detail-label">服务器名称:</span>
                            <span class="detail-value">${server.name}</span>
                        </div>
                        <div class="detail-row">
                            <span class="detail-label">版本:</span>
                            <span class="detail-value">${server.version || '未知'}</span>
                        </div>
                        <div class="detail-row">
                            <span class="detail-label">描述:</span>
                            <span class="detail-value">${server.description || '无描述'}</span>
                        </div>
                        <div class="detail-row">
                            <span class="detail-label">核心路径:</span>
                            <span class="detail-value">${server.corePath || '未设置'}</span>
                        </div>
                        <div class="detail-row">
                            <span class="detail-label">地图:</span>
                            <span class="detail-value">${server.map || '未设置'}</span>
                        </div>
                        <div class="detail-row">
                            <span class="detail-label">状态:</span>
                            <span class="detail-value">
                                <span class="status-badge ${isRunning ? 'running' : 'stopped'}">
                                    ${isRunning ? '运行中' : '已停止'}
                                </span>
                            </span>
                        </div>
                    </div>
                </div>
        `;
        
        // 如果服务器正在运行，显示运行信息
        if (isRunning && runningInfo) {
            const uptime = runningInfo.uptime ? formatUptime(runningInfo.uptime) : 'N/A';
            const startTime = runningInfo.startTime ? new Date(runningInfo.startTime).toLocaleString() : 'N/A';
            
            detailsHtml += `
                <div class="detail-section">
                    <h3><i class="fas fa-chart-line"></i> 运行状态</h3>
                    <div class="detail-grid">
                        <div class="detail-row">
                            <span class="detail-label">运行时间:</span>
                            <span class="detail-value">${uptime}</span>
                        </div>
                        <div class="detail-row">
                            <span class="detail-label">启动时间:</span>
                            <span class="detail-value">${startTime}</span>
                        </div>
                        <div class="detail-row">
                            <span class="detail-label">在线玩家:</span>
                            <span class="detail-value">${runningInfo.playerCount || 0}</span>
                        </div>
                        <div class="detail-row">
                            <span class="detail-label">内存使用:</span>
                            <span class="detail-value">${runningInfo.memoryUsage || 'N/A'}</span>
                        </div>
                    </div>
                </div>
            `;
        }
        
        detailsHtml += '</div>';
        
        // 显示模态框
        showDetailsModal(serverName, detailsHtml);
    } catch (error) {
        console.error('获取服务器详情失败:', error);
        showNotification('获取服务器详情失败: ' + error.message, 'error');
    }
}

/**
 * 格式化运行时间
 */
function formatUptime(ms) {
    const seconds = Math.floor(ms / 1000);
    const minutes = Math.floor(seconds / 60);
    const hours = Math.floor(minutes / 60);
    const days = Math.floor(hours / 24);
    
    if (days > 0) {
        return `${days}天 ${hours % 24}小时 ${minutes % 60}分钟`;
    } else if (hours > 0) {
        return `${hours}小时 ${minutes % 60}分钟`;
    } else if (minutes > 0) {
        return `${minutes}分钟 ${seconds % 60}秒`;
    } else {
        return `${seconds}秒`;
    }
}

/**
 * 显示详情模态框
 */
function showDetailsModal(title, content) {
    // 创建模态框
    const modal = document.createElement('div');
    modal.className = 'modal show';
    modal.id = 'detailsModal';
    modal.setAttribute('role', 'dialog');
    modal.setAttribute('aria-labelledby', 'detailsModalTitle');
    modal.setAttribute('aria-modal', 'true');
    
    modal.innerHTML = `
        <div class="modal-content" style="max-width: 700px;">
            <div class="modal-header">
                <div class="modal-title" id="detailsModalTitle">
                    <i class="fas fa-info-circle" aria-hidden="true"></i> ${title} - 服务器详情
                </div>
                <button type="button" class="close" onclick="closeDetailsModal()" aria-label="关闭对话框">&times;</button>
            </div>
            <div class="modal-body">
                ${content}
            </div>
            <div class="modal-footer">
                <button class="btn btn-primary" onclick="closeDetailsModal()">
                    <i class="fas fa-times"></i> 关闭
                </button>
            </div>
        </div>
    `;
    
    document.body.appendChild(modal);
    
    // 点击背景关闭
    modal.addEventListener('click', function(e) {
        if (e.target === modal) {
            closeDetailsModal();
        }
    });
    
    // ESC键关闭
    document.addEventListener('keydown', function escHandler(e) {
        if (e.key === 'Escape') {
            closeDetailsModal();
            document.removeEventListener('keydown', escHandler);
        }
    });
}

/**
 * 关闭详情模态框
 */
function closeDetailsModal() {
    const modal = document.getElementById('detailsModal');
    if (modal) {
        modal.remove();
    }
}

/**
 * 初始化创建服务器表单
 */
function initializeCreateServerForm() {
    document.getElementById('createForm').addEventListener('submit', async function(e) {
        e.preventDefault();

        const formData = {
            coreName: document.getElementById('coreName').value,
            serverName: document.getElementById('serverName').value,
            description: document.getElementById('description').value,
            version: document.getElementById('version').value || '1.0.0',
            path: document.getElementById('path').value || '',
            launchMode: parseInt(document.getElementById('launchMode').value) || 1
        };

        if (!formData.coreName || !formData.serverName) {
            showNotification('核心文件名和服务器名称不能为空', 'error');
            return;
        }

        try {
            showNotification('正在创建服务器...', 'info');
            
            const result = await apiCreateServer(formData);
            
            if (result) {
                showNotification('服务器创建成功！', 'success');
                closeCreateModal();
                loadServers();
            } else {
                const errorMsg = typeof result === 'string' ? result : '创建失败，请检查输入信息是否正确';
                showNotification(errorMsg, 'error');
            }
        } catch (error) {
            console.error('创建服务器失败:', error);
            showNotification(`创建服务器失败: ${error.message}`, 'error');
        }
    });
}
