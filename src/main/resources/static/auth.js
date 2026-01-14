/**
 * 认证模块
 * 管理登录、注册、退出登录等认证相关功能
 */

/**
 * 初始化认证页面事件监听
 */
function initializeAuthHandlers() {
    // 登录表单提交
    document.getElementById('loginForm').addEventListener('submit', handleLogin);
    
    // 注册表单提交
    document.getElementById('registerForm').addEventListener('submit', handleRegister);
    
    // 切换到注册页面
    document.getElementById('showRegisterLink').addEventListener('click', function(e) {
        e.preventDefault();
        showRegisterPage();
    });
    
    // 切换到登录页面
    document.getElementById('showLoginLink').addEventListener('click', function(e) {
        e.preventDefault();
        showLoginPage();
    });
    
    // 初始化密码显示/隐藏功能
    initializePasswordToggle();
}

/**
 * 初始化密码显示/隐藏切换功能
 */
function initializePasswordToggle() {
    // 登录页面的密码切换
    const loginPasswordInput = document.getElementById('password');
    const loginPasswordToggle = document.getElementById('passwordToggle');
    
    if (loginPasswordInput && loginPasswordToggle) {
        loginPasswordToggle.addEventListener('click', function() {
            togglePasswordVisibility(loginPasswordInput, loginPasswordToggle);
        });
    }
    
    // 注册页面的密码切换
    const regPasswordInput = document.getElementById('regPassword');
    const regPasswordToggle = document.getElementById('regPasswordToggle');
    
    if (regPasswordInput && regPasswordToggle) {
        regPasswordToggle.addEventListener('click', function() {
            togglePasswordVisibility(regPasswordInput, regPasswordToggle);
        });
    }
    
    // 注册页面的确认密码切换
    const regConfirmPasswordInput = document.getElementById('regConfirmPassword');
    const regConfirmPasswordToggle = document.getElementById('regConfirmPasswordToggle');
    
    if (regConfirmPasswordInput && regConfirmPasswordToggle) {
        regConfirmPasswordToggle.addEventListener('click', function() {
            togglePasswordVisibility(regConfirmPasswordInput, regConfirmPasswordToggle);
        });
    }
}

/**
 * 切换密码显示/隐藏
 * @param {HTMLInputElement} passwordInput - 密码输入框元素
 * @param {HTMLButtonElement} toggleBtn - 切换按钮元素
 */
function togglePasswordVisibility(passwordInput, toggleBtn) {
    if (!passwordInput || !toggleBtn) return;
    
    const icon = toggleBtn.querySelector('i');
    
    if (passwordInput.type === 'password') {
        // 显示密码
        passwordInput.type = 'text';
        icon.classList.remove('fa-eye');
        icon.classList.add('fa-eye-slash');
        toggleBtn.classList.add('active');
        toggleBtn.setAttribute('aria-label', '隐藏密码');
    } else {
        // 隐藏密码
        passwordInput.type = 'password';
        icon.classList.remove('fa-eye-slash');
        icon.classList.add('fa-eye');
        toggleBtn.classList.remove('active');
        toggleBtn.setAttribute('aria-label', '显示密码');
    }
    
    // 保持焦点在输入框
    passwordInput.focus();
}

/**
 * 处理登录请求
 * @param {Event} e - 表单提交事件
 */
async function handleLogin(e) {
    e.preventDefault();
    
    const username = document.getElementById('username').value;
    const password = document.getElementById('password').value;
    
    try {
        const data = await apiLogin(username, password);
        
        if (data && data.token) {
            // 保存认证信息
            setAuthInfo(data.token, data.username);
            
            // 保存用户角色
            if (data.role) {
                localStorage.setItem('userRole', data.role);
            }
            
            showNotification('登录成功！', 'success');
            await showMainApp();
        } else {
            showNotification(data?.error || '登录失败', 'error');
        }
    } catch (error) {
        showNotification('网络错误: ' + error.message, 'error');
    }
}

/**
 * 处理注册请求
 * @param {Event} e - 表单提交事件
 */
async function handleRegister(e) {
    e.preventDefault();
    
    const username = document.getElementById('regUsername').value;
    const email = document.getElementById('regEmail').value;
    const password = document.getElementById('regPassword').value;
    const confirmPassword = document.getElementById('regConfirmPassword').value;
    
    if (password !== confirmPassword) {
        showNotification('两次输入的密码不一致', 'error');
        return;
    }
    
    try {
        const data = await apiRegister(username, email, password);
        
        if (data && !data.error) {
            showNotification('注册成功，请登录', 'success');
            showLoginPage();
        } else {
            showNotification(data?.error || '注册失败', 'error');
        }
    } catch (error) {
        showNotification('网络错误: ' + error.message, 'error');
    }
}

/**
 * 退出登录
 */
function logout() {
    // 清除认证信息
    clearAuthInfo();
    
    // 断开WebSocket连接
    disconnectConsole();
    
    // 显示登录页面
    showLoginPage();
    showNotification('已退出登录', 'info');
}

/**
 * 初始化认证状态
 * 在页面加载时检查用户是否已登录
 */
async function initializeAuthState() {
    const token = getAuthToken();
    if (token) {
        // 用户已认证，显示主应用
        await showMainApp();
    } else {
        // 用户未认证，显示登录页
        showLoginPage();
    }
}
