/**
 * UI 工具模块
 * 管理通知、主题、页面导航等 UI 相关功能
 */

/**
 * 显示通知
 * @param {string} message - 通知消息
 * @param {string} type - 通知类型 (success, error, info)
 * @param {number} duration - 显示时长（毫秒），默认3000
 */
function showNotification(message, type = 'info', duration = 3000) {
    const notification = document.getElementById('notification');
    if (!notification) return;
    
    // 如果消息太长，截断并添加省略号
    const maxLength = 100;
    const displayMessage = message.length > maxLength ? message.substring(0, maxLength) + '...' : message;
    
    notification.textContent = displayMessage;
    notification.className = `notification ${type} show`;
    notification.title = message; // 完整消息作为提示
    
    // 根据类型调整显示时长
    if (type === 'error') {
        duration = 5000; // 错误消息显示更久
    } else if (type === 'success') {
        duration = 3000;
    } else {
        duration = 3000;
    }
    
    setTimeout(() => {
        notification.classList.remove('show');
    }, duration);
}

/**
 * 显示登录页面
 */
function showLoginPage() {
    document.getElementById('loginPage').style.display = 'flex';
    document.getElementById('registerPage').style.display = 'none';
    document.getElementById('mainApp').style.display = 'none';
    
    // 重置密码输入框状态
    resetPasswordInputs();
}

/**
 * 显示注册页面
 */
function showRegisterPage() {
    document.getElementById('loginPage').style.display = 'none';
    document.getElementById('registerPage').style.display = 'flex';
    document.getElementById('mainApp').style.display = 'none';
    
    // 重置密码输入框状态
    resetPasswordInputs();
}

/**
 * 重置密码输入框状态（隐藏密码并重置切换按钮）
 */
function resetPasswordInputs() {
    // 重置登录页面的密码输入框
    const loginPasswordInput = document.getElementById('password');
    const loginPasswordToggle = document.getElementById('passwordToggle');
    if (loginPasswordInput && loginPasswordToggle) {
        loginPasswordInput.type = 'password';
        const loginIcon = loginPasswordToggle.querySelector('i');
        if (loginIcon) {
            loginIcon.classList.remove('fa-eye-slash');
            loginIcon.classList.add('fa-eye');
        }
        loginPasswordToggle.classList.remove('active');
        loginPasswordToggle.setAttribute('aria-label', '显示密码');
    }
    
    // 重置注册页面的密码输入框
    const regPasswordInput = document.getElementById('regPassword');
    const regPasswordToggle = document.getElementById('regPasswordToggle');
    if (regPasswordInput && regPasswordToggle) {
        regPasswordInput.type = 'password';
        const regIcon = regPasswordToggle.querySelector('i');
        if (regIcon) {
            regIcon.classList.remove('fa-eye-slash');
            regIcon.classList.add('fa-eye');
        }
        regPasswordToggle.classList.remove('active');
        regPasswordToggle.setAttribute('aria-label', '显示密码');
    }
    
    // 重置确认密码输入框
    const regConfirmPasswordInput = document.getElementById('regConfirmPassword');
    const regConfirmPasswordToggle = document.getElementById('regConfirmPasswordToggle');
    if (regConfirmPasswordInput && regConfirmPasswordToggle) {
        regConfirmPasswordInput.type = 'password';
        const confirmIcon = regConfirmPasswordToggle.querySelector('i');
        if (confirmIcon) {
            confirmIcon.classList.remove('fa-eye-slash');
            confirmIcon.classList.add('fa-eye');
        }
        regConfirmPasswordToggle.classList.remove('active');
        regConfirmPasswordToggle.setAttribute('aria-label', '显示密码');
    }
}

/**
 * 显示主应用
 */
let serverRefreshInterval = null;
async function showMainApp() {
    document.getElementById('loginPage').style.display = 'none';
    document.getElementById('registerPage').style.display = 'none';
    document.getElementById('mainApp').style.display = 'block';
    
    // 设置用户信息
    const currentUser = getCurrentUser();
    if (currentUser) {
        document.getElementById('usernameDisplay').textContent = currentUser;
        document.getElementById('userAvatar').textContent = currentUser.charAt(0).toUpperCase();
    }
    
    // 初始化管理员面板
    if (typeof initializeAdminPanel === 'function') {
        await initializeAdminPanel();
    }
    
    // 开始加载服务器数据（仅加载一次）
    loadServers();
    
    // 清除之前的刷新计时器
    if (serverRefreshInterval) {
        clearInterval(serverRefreshInterval);
    }
    
    // 改为较少频率的刷新（30秒一次），避免UI频繁闪烁和JWT验证过于频繁
    serverRefreshInterval = setInterval(loadServers, 30000);
}

/**
 * 切换用户菜单
 */
function toggleUserMenu() {
    const dropdown = document.getElementById('userDropdown');
    dropdown.classList.toggle('show');
}

/**
 * 切换主题（浅色/深色）
 */
function toggleTheme() {
    const current = document.body.getAttribute('data-theme');
    const next = current === 'dark' ? 'light' : 'dark';
    document.body.setAttribute('data-theme', next);
    localStorage.setItem('cy_theme', next);
    const btnIcon = document.querySelector('#themeToggle i');
    if (btnIcon) {
        btnIcon.className = next === 'dark' ? 'fas fa-sun' : 'fas fa-moon';
    }
}

/**
 * 初始化主题
 */
function initializeTheme() {
    const saved = localStorage.getItem('cy_theme') || 'light';
    document.body.setAttribute('data-theme', saved);
    const btnIcon = document.querySelector('#themeToggle i');
    if (btnIcon) {
        btnIcon.className = saved === 'dark' ? 'fas fa-sun' : 'fas fa-moon';
    }
}

/**
 * 打开创建服务器模态框
 */
function openCreateModal() {
    const modal = document.getElementById('createModal');
    modal.style.display = 'flex';
    modal.classList.add('show');
    // 聚焦第一个输入框
    setTimeout(() => {
        const firstInput = modal.querySelector('input[type="text"]');
        if (firstInput) firstInput.focus();
    }, 100);
}

/**
 * 关闭创建服务器模态框
 */
function closeCreateModal() {
    const modal = document.getElementById('createModal');
    modal.style.display = 'none';
    modal.classList.remove('show');
    document.getElementById('createForm').reset();
}

/**
 * 显示个人资料
 */
function showProfile() {
    const currentUser = getCurrentUser();
    alert(`用户信息:\n用户名: ${currentUser}`);
    toggleUserMenu();
}

/**
 * 修改密码
 */
function changePassword() {
    alert('修改密码功能将在后续版本中实现');
    toggleUserMenu();
}

/**
 * 页面全局点击事件处理
 * 用于关闭下拉菜单和模态框
 */
function setupGlobalClickHandlers() {
    window.onclick = function(event) {
        // 关闭用户菜单
        if (!event.target.matches('#usernameDisplay') && !event.target.matches('.fa-chevron-down')) {
            const dropdowns = document.getElementsByClassName('dropdown');
            for (let i = 0; i < dropdowns.length; i++) {
                const openDropdown = dropdowns[i];
                if (openDropdown.classList.contains('show')) {
                    openDropdown.classList.remove('show');
                }
            }
        }
        
        // 关闭模态框
        const modal = document.getElementById('createModal');
        if (event.target === modal) {
            closeCreateModal();
        }
    };
}

/**
 * 刷新服务器列表
 */
function refreshServers() {
    loadServers();
    showNotification('服务器列表已刷新', 'info');
}
