/**
 * 管理员面板模块
 * 管理用户、权限、系统设置等管理员功能
 */

let currentUserRole = null;
let isAdmin = false;

/**
 * 初始化管理员面板
 */
async function initializeAdminPanel() {
    // 检查当前用户是否为管理员
    await checkAdminStatus();
    
    // 如果是管理员，添加管理员菜单项
    if (isAdmin) {
        addAdminMenuItems();
    }
}

/**
 * 检查管理员状态
 */
async function checkAdminStatus() {
    try {
        const response = await fetch('/api/admin/check', {
            method: 'GET',
            headers: {
                'Authorization': `Bearer ${getAuthToken()}`
            }
        });
        
        if (response.ok) {
            const data = await response.json();
            isAdmin = data.isAdmin === true;
            currentUserRole = data.role || localStorage.getItem('userRole') || 'USER';
        } else {
            isAdmin = false;
            // 从localStorage获取角色
            currentUserRole = localStorage.getItem('userRole') || 'USER';
            isAdmin = currentUserRole === 'ADMIN';
        }
    } catch (error) {
        console.error('检查管理员状态失败:', error);
        isAdmin = false;
    }
}

/**
 * 添加管理员菜单项
 */
function addAdminMenuItems() {
    const userDropdown = document.getElementById('userDropdown');
    if (!userDropdown) return;
    
    // 检查是否已添加管理员菜单
    if (userDropdown.querySelector('.admin-menu-item')) return;
    
    // 添加分隔线
    const divider = document.createElement('div');
    divider.className = 'dropdown-divider';
    userDropdown.appendChild(divider);
    
    // 添加管理员面板菜单项
    const adminMenuItem = document.createElement('a');
    adminMenuItem.href = '#';
    adminMenuItem.className = 'dropdown-item admin-menu-item';
    adminMenuItem.innerHTML = '<i class="fas fa-shield-alt"></i> 管理员面板';
    adminMenuItem.onclick = function(e) {
        e.preventDefault();
        showAdminPanel();
    };
    userDropdown.appendChild(adminMenuItem);
}

/**
 * 显示管理员面板
 */
function showAdminPanel() {
    // 创建管理员面板模态框
    const modal = document.createElement('div');
    modal.className = 'modal show';
    modal.id = 'adminPanelModal';
    modal.setAttribute('role', 'dialog');
    modal.setAttribute('aria-labelledby', 'adminPanelTitle');
    modal.setAttribute('aria-modal', 'true');
    
    modal.innerHTML = `
        <div class="modal-content" style="max-width: 1200px; width: 95%;">
            <div class="modal-header">
                <div class="modal-title" id="adminPanelTitle">
                    <i class="fas fa-shield-alt" aria-hidden="true"></i> 管理员面板
                </div>
                <button type="button" class="close" onclick="closeAdminPanel()" aria-label="关闭对话框">&times;</button>
            </div>
            <div class="modal-body">
                <div class="admin-tabs">
                    <button class="admin-tab active" onclick="switchAdminTab('users', event)">
                        <i class="fas fa-users"></i> 用户管理
                    </button>
                    <button class="admin-tab" onclick="switchAdminTab('stats', event)">
                        <i class="fas fa-chart-bar"></i> 系统统计
                    </button>
                    <button class="admin-tab" onclick="switchAdminTab('settings', event)">
                        <i class="fas fa-cog"></i> 系统设置
                    </button>
                </div>
                
                <div class="admin-tab-content" id="adminTabContent">
                    <!-- 内容将通过JavaScript动态加载 -->
                </div>
            </div>
        </div>
    `;
    
    document.body.appendChild(modal);
    
    // 点击背景关闭
    modal.addEventListener('click', function(e) {
        if (e.target === modal) {
            closeAdminPanel();
        }
    });
    
    // ESC键关闭
    document.addEventListener('keydown', function escHandler(e) {
        if (e.key === 'Escape') {
            closeAdminPanel();
            document.removeEventListener('keydown', escHandler);
        }
    });
    
    // 加载默认标签页
    switchAdminTab('users');
}

/**
 * 关闭管理员面板
 */
function closeAdminPanel() {
    const modal = document.getElementById('adminPanelModal');
    if (modal) {
        modal.remove();
    }
}

/**
 * 切换管理员标签页
 */
function switchAdminTab(tabName, event) {
    // 更新标签页状态
    document.querySelectorAll('.admin-tab').forEach(tab => {
        tab.classList.remove('active');
    });
    if (event && event.target) {
        event.target.classList.add('active');
    } else {
        // 如果没有事件对象，通过tabName查找对应的标签
        document.querySelectorAll('.admin-tab').forEach(tab => {
            if (tab.textContent.includes(tabName === 'users' ? '用户管理' : 
                                         tabName === 'stats' ? '系统统计' : '系统设置')) {
                tab.classList.add('active');
            }
        });
    }
    
    const content = document.getElementById('adminTabContent');
    if (!content) return;
    
    switch(tabName) {
        case 'users':
            loadUsersTab(content);
            break;
        case 'stats':
            loadStatsTab(content);
            break;
        case 'settings':
            loadSettingsTab(content);
            break;
    }
}

/**
 * 加载用户管理标签页
 */
async function loadUsersTab(container) {
    container.innerHTML = `
        <div class="admin-section">
            <div class="admin-section-header">
                <h3><i class="fas fa-users"></i> 用户管理</h3>
                <button class="btn btn-success" onclick="openCreateUserModal()">
                    <i class="fas fa-plus"></i> 创建用户
                </button>
            </div>
            <div class="admin-table-container">
                <table class="admin-table">
                    <thead>
                        <tr>
                            <th>ID</th>
                            <th>用户名</th>
                            <th>角色</th>
                            <th>状态</th>
                            <th>创建时间</th>
                            <th>最后登录</th>
                            <th>操作</th>
                        </tr>
                    </thead>
                    <tbody id="usersTableBody">
                        <tr>
                            <td colspan="7" style="text-align: center; padding: 20px;">
                                <i class="fas fa-spinner fa-spin"></i> 加载中...
                            </td>
                        </tr>
                    </tbody>
                </table>
            </div>
        </div>
    `;
    
    await loadUsersList();
}

/**
 * 加载用户列表
 */
async function loadUsersList() {
    try {
        const response = await fetch('/api/admin/users', {
            method: 'GET',
            headers: {
                'Authorization': `Bearer ${getAuthToken()}`
            }
        });
        
        if (!response.ok) {
            const error = await response.json();
            showNotification(error.error || '加载用户列表失败', 'error');
            return;
        }
        
        const data = await response.json();
        const users = data.data || [];
        
        const tbody = document.getElementById('usersTableBody');
        if (!tbody) return;
        
        if (users.length === 0) {
            tbody.innerHTML = `
                <tr>
                    <td colspan="7" style="text-align: center; padding: 20px; color: var(--text-secondary);">
                        暂无用户
                    </td>
                </tr>
            `;
            return;
        }
        
        tbody.innerHTML = users.map(user => `
            <tr>
                <td>${user.id}</td>
                <td><strong>${escapeHtml(user.username)}</strong></td>
                <td>
                    <span class="role-badge ${user.role === 'ADMIN' ? 'admin' : 'user'}">
                        ${user.role === 'ADMIN' ? '<i class="fas fa-shield-alt"></i> 管理员' : '<i class="fas fa-user"></i> 用户'}
                    </span>
                </td>
                <td>
                    <span class="status-badge ${user.enabled ? 'enabled' : 'disabled'}">
                        ${user.enabled ? '启用' : '禁用'}
                    </span>
                </td>
                <td>${user.createdAt ? formatDateTime(user.createdAt) : '--'}</td>
                <td>${user.lastLoginAt ? formatDateTime(user.lastLoginAt) : '--'}</td>
                <td>
                    <div class="admin-actions">
                        <button class="btn btn-sm btn-outline" onclick="editUser(${user.id})" title="编辑">
                            <i class="fas fa-edit"></i>
                        </button>
                        <button class="btn btn-sm btn-outline" onclick="toggleUserStatus(${user.id}, ${user.enabled})" title="${user.enabled ? '禁用' : '启用'}">
                            <i class="fas fa-${user.enabled ? 'ban' : 'check'}"></i>
                        </button>
                        <button class="btn btn-sm btn-danger" onclick="deleteUser(${user.id}, '${escapeHtml(user.username)}')" title="删除">
                            <i class="fas fa-trash"></i>
                        </button>
                    </div>
                </td>
            </tr>
        `).join('');
        
    } catch (error) {
        console.error('加载用户列表失败:', error);
        showNotification('加载用户列表失败: ' + error.message, 'error');
    }
}

/**
 * 加载统计标签页
 */
async function loadStatsTab(container) {
    container.innerHTML = `
        <div class="admin-section">
            <div class="admin-section-header">
                <h3><i class="fas fa-chart-bar"></i> 系统统计</h3>
            </div>
            <div class="admin-stats-grid" id="adminStatsGrid">
                <div class="admin-stat-card">
                    <div class="admin-stat-icon">
                        <i class="fas fa-users"></i>
                    </div>
                    <div class="admin-stat-content">
                        <div class="admin-stat-value" id="statTotalUsers">--</div>
                        <div class="admin-stat-label">总用户数</div>
                    </div>
                </div>
                <div class="admin-stat-card">
                    <div class="admin-stat-icon admin">
                        <i class="fas fa-shield-alt"></i>
                    </div>
                    <div class="admin-stat-content">
                        <div class="admin-stat-value" id="statAdminUsers">--</div>
                        <div class="admin-stat-label">管理员</div>
                    </div>
                </div>
                <div class="admin-stat-card">
                    <div class="admin-stat-icon user">
                        <i class="fas fa-user"></i>
                    </div>
                    <div class="admin-stat-content">
                        <div class="admin-stat-value" id="statUserUsers">--</div>
                        <div class="admin-stat-label">普通用户</div>
                    </div>
                </div>
                <div class="admin-stat-card">
                    <div class="admin-stat-icon enabled">
                        <i class="fas fa-check-circle"></i>
                    </div>
                    <div class="admin-stat-content">
                        <div class="admin-stat-value" id="statEnabledUsers">--</div>
                        <div class="admin-stat-label">已启用</div>
                    </div>
                </div>
                <div class="admin-stat-card">
                    <div class="admin-stat-icon disabled">
                        <i class="fas fa-ban"></i>
                    </div>
                    <div class="admin-stat-content">
                        <div class="admin-stat-value" id="statDisabledUsers">--</div>
                        <div class="admin-stat-label">已禁用</div>
                    </div>
                </div>
            </div>
        </div>
    `;
    
    await loadSystemStats();
}

/**
 * 加载系统统计
 */
async function loadSystemStats() {
    try {
        const response = await fetch('/api/admin/stats', {
            method: 'GET',
            headers: {
                'Authorization': `Bearer ${getAuthToken()}`
            }
        });
        
        if (!response.ok) {
            const error = await response.json();
            showNotification(error.error || '加载统计信息失败', 'error');
            return;
        }
        
        const data = await response.json();
        const stats = data.data || {};
        
        document.getElementById('statTotalUsers').textContent = stats.totalUsers || 0;
        document.getElementById('statAdminUsers').textContent = stats.adminUsers || 0;
        document.getElementById('statUserUsers').textContent = stats.userUsers || 0;
        document.getElementById('statEnabledUsers').textContent = stats.enabledUsers || 0;
        document.getElementById('statDisabledUsers').textContent = stats.disabledUsers || 0;
        
    } catch (error) {
        console.error('加载统计信息失败:', error);
        showNotification('加载统计信息失败: ' + error.message, 'error');
    }
}

/**
 * 加载设置标签页
 */
function loadSettingsTab(container) {
    container.innerHTML = `
        <div class="admin-section">
            <div class="admin-section-header">
                <h3><i class="fas fa-cog"></i> 系统设置</h3>
            </div>
            <div class="admin-settings">
                <div class="setting-group">
                    <h4><i class="fas fa-server"></i> 服务器设置</h4>
                    <div class="setting-item">
                        <label>默认端口</label>
                        <input type="number" class="form-control" value="8080" disabled>
                        <small>当前Web服务端口</small>
                    </div>
                </div>
                <div class="setting-group">
                    <h4><i class="fas fa-database"></i> 数据库设置</h4>
                    <div class="setting-item">
                        <label>数据库类型</label>
                        <input type="text" class="form-control" value="H2 文件数据库" disabled>
                        <small>当前使用的数据库类型</small>
                    </div>
                </div>
                <div class="setting-group">
                    <h4><i class="fas fa-shield-alt"></i> 安全设置</h4>
                    <div class="setting-item">
                        <label>JWT令牌有效期</label>
                        <input type="text" class="form-control" value="24小时" disabled>
                        <small>JWT令牌的有效期</small>
                    </div>
                </div>
            </div>
        </div>
    `;
}

/**
 * 打开创建用户模态框
 */
function openCreateUserModal() {
    const modal = document.createElement('div');
    modal.className = 'modal show';
    modal.id = 'createUserModal';
    modal.setAttribute('role', 'dialog');
    modal.setAttribute('aria-labelledby', 'createUserModalTitle');
    modal.setAttribute('aria-modal', 'true');
    
    modal.innerHTML = `
        <div class="modal-content" style="max-width: 500px;">
            <div class="modal-header">
                <div class="modal-title" id="createUserModalTitle">
                    <i class="fas fa-user-plus" aria-hidden="true"></i> 创建用户
                </div>
                <button type="button" class="close" onclick="closeCreateUserModal()" aria-label="关闭对话框">&times;</button>
            </div>
            <div class="modal-body">
                <form id="createUserForm">
                    <div class="form-group">
                        <label for="newUsername"><i class="fas fa-user"></i> 用户名</label>
                        <input type="text" class="form-control" id="newUsername" required>
                    </div>
                    <div class="form-group">
                        <label for="newPassword"><i class="fas fa-lock"></i> 密码</label>
                        <div class="password-input-wrapper">
                            <input type="password" class="form-control" id="newPassword" required>
                            <button type="button" class="password-toggle-btn" onclick="togglePasswordVisibility(document.getElementById('newPassword'), this)">
                                <i class="fas fa-eye"></i>
                            </button>
                        </div>
                    </div>
                    <div class="form-group">
                        <label for="newRole"><i class="fas fa-shield-alt"></i> 角色</label>
                        <select class="form-control" id="newRole">
                            <option value="USER">普通用户</option>
                            <option value="ADMIN">管理员</option>
                        </select>
                    </div>
                </form>
            </div>
            <div class="modal-footer">
                <button class="btn btn-outline" onclick="closeCreateUserModal()">取消</button>
                <button class="btn btn-primary" onclick="submitCreateUser()">创建</button>
            </div>
        </div>
    `;
    
    document.body.appendChild(modal);
    
    // 点击背景关闭
    modal.addEventListener('click', function(e) {
        if (e.target === modal) {
            closeCreateUserModal();
        }
    });
    
    // ESC键关闭
    document.addEventListener('keydown', function escHandler(e) {
        if (e.key === 'Escape') {
            closeCreateUserModal();
            document.removeEventListener('keydown', escHandler);
        }
    });
    
    // 聚焦到用户名输入框
    setTimeout(() => {
        document.getElementById('newUsername')?.focus();
    }, 100);
}

/**
 * 关闭创建用户模态框
 */
function closeCreateUserModal() {
    const modal = document.getElementById('createUserModal');
    if (modal) {
        modal.remove();
    }
}

/**
 * 提交创建用户
 */
async function submitCreateUser() {
    const username = document.getElementById('newUsername')?.value.trim();
    const password = document.getElementById('newPassword')?.value;
    const role = document.getElementById('newRole')?.value;
    
    if (!username || !password) {
        showNotification('请填写所有必填字段', 'error');
        return;
    }
    
    try {
        const response = await fetch('/api/admin/users', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'Authorization': `Bearer ${getAuthToken()}`
            },
            body: JSON.stringify({
                username: username,
                password: password,
                role: role || 'USER'
            })
        });
        
        const data = await response.json();
        
        if (data.success) {
            showNotification('用户创建成功', 'success');
            closeCreateUserModal();
            loadUsersList();
        } else {
            showNotification(data.error || '创建用户失败', 'error');
        }
    } catch (error) {
        console.error('创建用户失败:', error);
        showNotification('创建用户失败: ' + error.message, 'error');
    }
}

/**
 * 编辑用户
 */
async function editUser(userId) {
    try {
        const response = await fetch(`/api/admin/users/${userId}`, {
            method: 'GET',
            headers: {
                'Authorization': `Bearer ${getAuthToken()}`
            }
        });
        
        if (!response.ok) {
            const error = await response.json();
            showNotification(error.error || '获取用户信息失败', 'error');
            return;
        }
        
        const data = await response.json();
        const user = data.data;
        
        // 创建编辑用户模态框
        const modal = document.createElement('div');
        modal.className = 'modal show';
        modal.id = 'editUserModal';
        modal.setAttribute('role', 'dialog');
        modal.setAttribute('aria-labelledby', 'editUserModalTitle');
        modal.setAttribute('aria-modal', 'true');
        
        modal.innerHTML = `
            <div class="modal-content" style="max-width: 500px;">
                <div class="modal-header">
                    <div class="modal-title" id="editUserModalTitle">
                        <i class="fas fa-edit" aria-hidden="true"></i> 编辑用户: ${escapeHtml(user.username)}
                    </div>
                    <button type="button" class="close" onclick="closeEditUserModal()" aria-label="关闭对话框">&times;</button>
                </div>
                <div class="modal-body">
                    <form id="editUserForm">
                        <div class="form-group">
                            <label><i class="fas fa-user"></i> 用户名</label>
                            <input type="text" class="form-control" value="${escapeHtml(user.username)}" disabled>
                            <small>用户名不可修改</small>
                        </div>
                        <div class="form-group">
                            <label for="editRole"><i class="fas fa-shield-alt"></i> 角色</label>
                            <select class="form-control" id="editRole">
                                <option value="USER" ${user.role === 'USER' ? 'selected' : ''}>普通用户</option>
                                <option value="ADMIN" ${user.role === 'ADMIN' ? 'selected' : ''}>管理员</option>
                            </select>
                        </div>
                        <div class="form-group">
                            <label for="editEnabled"><i class="fas fa-toggle-on"></i> 状态</label>
                            <select class="form-control" id="editEnabled">
                                <option value="true" ${user.enabled ? 'selected' : ''}>启用</option>
                                <option value="false" ${!user.enabled ? 'selected' : ''}>禁用</option>
                            </select>
                        </div>
                        <div class="form-group">
                            <label for="editPassword"><i class="fas fa-lock"></i> 新密码（留空不修改）</label>
                            <div class="password-input-wrapper">
                                <input type="password" class="form-control" id="editPassword" placeholder="留空则不修改密码">
                                <button type="button" class="password-toggle-btn" onclick="togglePasswordVisibility(document.getElementById('editPassword'), this)">
                                    <i class="fas fa-eye"></i>
                                </button>
                            </div>
                        </div>
                    </form>
                </div>
                <div class="modal-footer">
                    <button class="btn btn-outline" onclick="closeEditUserModal()">取消</button>
                    <button class="btn btn-primary" onclick="submitEditUser(${userId})">保存</button>
                </div>
            </div>
        `;
        
        document.body.appendChild(modal);
        
        // 点击背景关闭
        modal.addEventListener('click', function(e) {
            if (e.target === modal) {
                closeEditUserModal();
            }
        });
        
        // ESC键关闭
        document.addEventListener('keydown', function escHandler(e) {
            if (e.key === 'Escape') {
                closeEditUserModal();
                document.removeEventListener('keydown', escHandler);
            }
        });
        
    } catch (error) {
        console.error('获取用户信息失败:', error);
        showNotification('获取用户信息失败: ' + error.message, 'error');
    }
}

/**
 * 关闭编辑用户模态框
 */
function closeEditUserModal() {
    const modal = document.getElementById('editUserModal');
    if (modal) {
        modal.remove();
    }
}

/**
 * 提交编辑用户
 */
async function submitEditUser(userId) {
    const role = document.getElementById('editRole')?.value;
    const enabled = document.getElementById('editEnabled')?.value === 'true';
    const password = document.getElementById('editPassword')?.value;
    
    const updateData = {
        role: role,
        enabled: enabled
    };
    
    if (password && password.trim()) {
        updateData.password = password;
    }
    
    try {
        const response = await fetch(`/api/admin/users/${userId}`, {
            method: 'PUT',
            headers: {
                'Content-Type': 'application/json',
                'Authorization': `Bearer ${getAuthToken()}`
            },
            body: JSON.stringify(updateData)
        });
        
        const data = await response.json();
        
        if (data.success) {
            showNotification('用户更新成功', 'success');
            closeEditUserModal();
            loadUsersList();
        } else {
            showNotification(data.error || '更新用户失败', 'error');
        }
    } catch (error) {
        console.error('更新用户失败:', error);
        showNotification('更新用户失败: ' + error.message, 'error');
    }
}

/**
 * 切换用户状态
 */
async function toggleUserStatus(userId, currentStatus) {
    const newStatus = !currentStatus;
    const action = newStatus ? '启用' : '禁用';
    
    if (!confirm(`确定要${action}该用户吗？`)) {
        return;
    }
    
    try {
        const response = await fetch(`/api/admin/users/${userId}`, {
            method: 'PUT',
            headers: {
                'Content-Type': 'application/json',
                'Authorization': `Bearer ${getAuthToken()}`
            },
            body: JSON.stringify({
                enabled: newStatus
            })
        });
        
        const data = await response.json();
        
        if (data.success) {
            showNotification(`用户已${action}`, 'success');
            loadUsersList();
        } else {
            showNotification(data.error || `${action}用户失败`, 'error');
        }
    } catch (error) {
        console.error(`${action}用户失败:`, error);
        showNotification(`${action}用户失败: ` + error.message, 'error');
    }
}

/**
 * 删除用户
 */
async function deleteUser(userId, username) {
    if (!confirm(`确定要删除用户 "${username}" 吗？\n此操作不可恢复！`)) {
        return;
    }
    
    try {
        const response = await fetch(`/api/admin/users/${userId}`, {
            method: 'DELETE',
            headers: {
                'Authorization': `Bearer ${getAuthToken()}`
            }
        });
        
        const data = await response.json();
        
        if (data.success) {
            showNotification('用户删除成功', 'success');
            loadUsersList();
            loadSystemStats(); // 刷新统计
        } else {
            showNotification(data.error || '删除用户失败', 'error');
        }
    } catch (error) {
        console.error('删除用户失败:', error);
        showNotification('删除用户失败: ' + error.message, 'error');
    }
}

/**
 * 格式化日期时间
 */
function formatDateTime(dateTimeString) {
    if (!dateTimeString) return '--';
    try {
        const date = new Date(dateTimeString);
        return date.toLocaleString('zh-CN', {
            year: 'numeric',
            month: '2-digit',
            day: '2-digit',
            hour: '2-digit',
            minute: '2-digit'
        });
    } catch (e) {
        return dateTimeString;
    }
}

/**
 * HTML转义
 */
function escapeHtml(text) {
    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
}
