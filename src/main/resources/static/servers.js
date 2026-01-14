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

/**
 * 更新统计信息
 * @param {Array} servers - 服务器数组
 * @param {object} runningServers - 运行中的服务器对象
 */
function updateStats(servers, runningServers) {
    document.getElementById('totalServers').textContent = servers.length;
    document.getElementById('runningServers').textContent = Object.keys(runningServers).length;
    
    // 计算在线玩家数量
    let totalPlayers = 0;
    for (const serverName in runningServers) {
        totalPlayers += runningServers[serverName].playerCount || 0;
    }
    document.getElementById('totalPlayers').textContent = totalPlayers;
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
        
        const result = await apiStartServer(serverName);
        
        if (result) {
            showNotification(`服务器 "${serverName}" 启动成功！`, 'success');
        } else {
            showNotification(`启动失败: ${result}`, 'error');
        }
        
        loadServers(); // 刷新服务器列表
    } catch (error) {
        console.error('启动服务器失败:', error);
        showNotification(`启动服务器失败: ${error.message}`, 'error');
    }
}

/**
 * 停止服务器
 * @param {string} serverName - 服务器名称
 */
async function stopServer(serverName) {
    if (!confirm(`确定要停止服务器 "${serverName}" 吗？`)) return;

    try {
        showNotification(`正在停止服务器 "${serverName}"...`, 'info');
        
        const result = await apiStopServer(serverName);
        
        if (result) {
            showNotification(`服务器 "${serverName}" 停止成功！`, 'success');
        } else {
            showNotification(`停止失败: ${result}`, 'error');
        }
        
        loadServers(); // 刷新服务器列表
    } catch (error) {
        console.error('停止服务器失败:', error);
        showNotification(`停止服务器失败: ${error.message}`, 'error');
    }
}

/**
 * 强制停止服务器
 * @param {string} serverName - 服务器名称
 */
async function forceStopServer(serverName) {
    if (!confirm(`确定要强制停止服务器 "${serverName}" 吗？此操作可能导致数据丢失！`)) return;

    try {
        showNotification(`正在强制停止服务器 "${serverName}"...`, 'info');
        
        const result = await apiForceStopServer(serverName);
        
        if (result) {
            showNotification(`服务器 "${serverName}" 强制停止成功！`, 'success');
        } else {
            showNotification(`强制停止失败: ${result}`, 'error');
        }
        
        loadServers(); // 刷新服务器列表
    } catch (error) {
        console.error('强制停止服务器失败:', error);
        showNotification(`强制停止服务器失败: ${error.message}`, 'error');
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
            showNotification(`删除失败: ${result}`, 'error');
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
function viewDetails(serverName) {
    const server = Array.from(document.querySelectorAll('#serverList .server-card'))
        .find(card => card.querySelector('.server-name').textContent === serverName);
    if (!server) return;
    
    const serverData = { 
        name: server.querySelector('.server-name').textContent,
        version: server.querySelector('.detail-item:nth-child(1) span').textContent.replace('版本: ', ''),
        description: server.querySelector('.detail-item:nth-child(2) span').textContent.replace('描述: ', ''),
        path: server.querySelector('.detail-item:nth-child(3) span').textContent.replace('路径: ', ''),
        map: server.querySelector('.detail-item:nth-child(4) span').textContent.replace('地图: ', '')
    };
    
    alert(`服务器详情:
名称: ${serverData.name}
版本: ${serverData.version}
描述: ${serverData.description}
路径: ${serverData.path}
地图: ${serverData.map}`);
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
                showNotification(`创建失败: ${result}`, 'error');
            }
        } catch (error) {
            console.error('创建服务器失败:', error);
            showNotification(`创建服务器失败: ${error.message}`, 'error');
        }
    });
}
