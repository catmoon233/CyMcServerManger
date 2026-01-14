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
    const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
    return `${protocol}//${window.location.host}/ws/logs/${serverName}?token=${encodeURIComponent(authToken)}`;
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
    return response.json();
}

/**
 * 加载运行中的服务器
 * @returns {Promise<object>}
 */
async function apiLoadRunningServers() {
    const response = await authenticatedFetch('/api/servers/running');
    if (!response) return {};
    return response.json();
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
    return response.text();
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
    return response.text();
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
    return response.text();
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
    return response.text();
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
    return response.text();
}
