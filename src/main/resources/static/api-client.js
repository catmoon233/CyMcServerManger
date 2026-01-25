/**
 * API 客户端模块
 * 统一管理所有 API 请求和认证相关的功能
 */

let authToken = localStorage.getItem('authToken');
let currentUser = localStorage.getItem('currentUser');

/**
 * 通用请求函数，自动添加认证头
 * @param {string} url - 请求 URL
 * @param {object} options - 请求选项
 * @returns {Promise<Response>}
 */
async function authenticatedFetch(url, options = {}) {
    if (!authToken) {
        showNotification('请先登录', 'error');
        showLoginPage();
        return null;
    }
    
    const defaultOptions = {
        headers: {
            'Authorization': `Bearer ${authToken}`,
            'Content-Type': 'application/json'
        }
    };
    
    const mergedOptions = {
        ...defaultOptions,
        ...options,
        headers: {
            ...defaultOptions.headers,
            ...options.headers
        }
    };
    
    try {
        const response = await fetch(url, mergedOptions);
        
        // 检查是否是认证错误
        if (response.status === 401) {
            showNotification('认证过期，请重新登录', 'error');
            logout();
            return null;
        }
        
        return response;
    } catch (error) {
        showNotification('网络错误: ' + error.message, 'error');
        throw error;
    }
}

/**
 * 更新认证信息
 * @param {string} token - JWT token
 * @param {string} username - 用户名
 */
function setAuthInfo(token, username) {
    authToken = token;
    currentUser = username;
    localStorage.setItem('authToken', token);
    localStorage.setItem('currentUser', username);
}

/**
 * 清除认证信息
 */
function clearAuthInfo() {
    authToken = null;
    currentUser = null;
    localStorage.removeItem('authToken');
    localStorage.removeItem('currentUser');
}

/**
 * 获取认证 Token
 * @returns {string|null}
 */
function getAuthToken() {
    return authToken;
}

/**
 * 获取当前用户名
 * @returns {string|null}
 */
function getCurrentUser() {
    return currentUser;
}

/**
 * 构建 WebSocket URL
 * @param {string} serverName - 服务器名称
 * @returns {string}
 */
function buildWebSocketUrl(serverName) {
    if (!authToken) {
        console.error('无法构建WebSocket URL: 认证令牌不存在');
        throw new Error('认证令牌不存在，请先登录');
    }
    
    if (!serverName || serverName.trim() === '') {
        console.error('无法构建WebSocket URL: 服务器名称为空');
        throw new Error('服务器名称不能为空');
    }
    
    const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
    const host = window.location.host;
    const encodedServerName = encodeURIComponent(serverName);
    const encodedToken = encodeURIComponent(authToken);
    
    // 使用新的WebSocket路径格式，兼容info路径
    const url = `${protocol}//${host}/ws/logs/${encodedServerName}?token=${encodedToken}`;
    console.log('构建WebSocket URL:', url.replace(encodedToken, '***'));
    return url;
}

/**
 * 登录请求
 * @param {string} username - 用户名
 * @param {string} password - 密码
 * @returns {Promise<object>}
 */
async function apiLogin(username, password) {
    const response = await fetch('/api/auth/login', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json'
        },
        body: JSON.stringify({ username, password })
    });
    return response.json();
}

/**
 * 注册请求
 * @param {string} username - 用户名
 * @param {string} email - 邮箱
 * @param {string} password - 密码
 * @returns {Promise<object>}
 */
async function apiRegister(username, email, password) {
    const response = await fetch('/api/auth/register', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json'
        },
        body: JSON.stringify({ username, email, password })
    });
    return response.json();
}

/**
 * 加载服务器列表
 * @returns {Promise<Array>}
 */
async function apiLoadServers() {
    const response = await authenticatedFetch('/api/servers/');
    if (!response) return [];
    const data = await response.json();
    // 支持新的统一响应格式
    if (data.success !== undefined) {
        return data.data || [];
    }
    // 兼容旧格式
    return Array.isArray(data) ? data : [];
}

/**
 * 加载运行中的服务器
 * @returns {Promise<object>}
 */
async function apiLoadRunningServers() {
    const response = await authenticatedFetch('/api/servers/running');
    if (!response) return {};
    const data = await response.json();
    // 支持新的统一响应格式
    if (data.success !== undefined) {
        return data.data || {};
    }
    // 兼容旧格式
    return data || {};
}

/**
 * 启动服务器
 * @param {string} serverName - 服务器名称
 * @param {number} launchMode - 启动模式
 * @returns {Promise<object>}
 */
async function apiStartServer(serverName, launchMode = 1) {
    const response = await authenticatedFetch(`/api/servers/${serverName}/start`, {
        method: 'POST',
        body: JSON.stringify({
            launchMode: launchMode,
            javaPath: 'java'
        })
    });
    if (!response) return null;
    const data = await response.json();
    // 支持新的统一响应格式
    if (data.success !== undefined) {
        return data.success ? data.message : data.error;
    }
    // 兼容旧格式（纯文本）
    return data;
}

/**
 * 停止服务器
 * @param {string} serverName - 服务器名称
 * @returns {Promise<string>}
 */
async function apiStopServer(serverName) {
    const response = await authenticatedFetch(`/api/servers/${serverName}/stop`, {
        method: 'POST'
    });
    if (!response) return null;
    const data = await response.json();
    // 支持新的统一响应格式
    if (data.success !== undefined) {
        return data.success ? data.message : data.error;
    }
    // 兼容旧格式
    return data;
}

/**
 * 强制停止服务器
 * @param {string} serverName - 服务器名称
 * @returns {Promise<string>}
 */
async function apiForceStopServer(serverName) {
    const response = await authenticatedFetch(`/api/servers/${serverName}/forceStop`, {
        method: 'POST'
    });
    if (!response) return null;
    const data = await response.json();
    // 支持新的统一响应格式
    if (data.success !== undefined) {
        return data.success ? data.message : data.error;
    }
    // 兼容旧格式
    return data;
}

/**
 * 删除服务器
 * @param {string} serverName - 服务器名称
 * @param {boolean} deleteFiles - 是否删除文件
 * @returns {Promise<string>}
 */
async function apiDeleteServer(serverName, deleteFiles = true) {
    const response = await authenticatedFetch(`/api/servers/${serverName}`, {
        method: 'DELETE',
        body: JSON.stringify({ deleteFiles: deleteFiles })
    });
    if (!response) return null;
    const data = await response.json();
    // 支持新的统一响应格式
    if (data.success !== undefined) {
        return data.success ? data.message : data.error;
    }
    // 兼容旧格式
    return data;
}

/**
 * 创建服务器
 * @param {object} serverData - 服务器数据
 * @returns {Promise<string>}
 */
async function apiCreateServer(serverData) {
    const response = await authenticatedFetch('/api/servers/create', {
        method: 'POST',
        body: JSON.stringify(serverData)
    });
    if (!response) return null;
    const data = await response.json();
    // 支持新的统一响应格式
    if (data.success !== undefined) {
        return data.success ? data.message : data.error;
    }
    // 兼容旧格式
    return data;
}
