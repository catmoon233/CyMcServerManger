/**
 * 动画和交互效果模块
 */

/**
 * 初始化粒子效果
 */
function initParticles() {
    const container = document.getElementById('particlesContainer');
    if (!container) return;
    
    // 清除现有粒子
    container.innerHTML = '';
    
    // 根据屏幕大小创建粒子
    const particleCount = Math.min(Math.floor(window.innerWidth / 50), 20);
    
    for (let i = 0; i < particleCount; i++) {
        const particle = document.createElement('div');
        particle.className = 'particle';
        particle.style.left = Math.random() * 100 + '%';
        particle.style.animationDelay = Math.random() * 15 + 's';
        particle.style.animationDuration = (15 + Math.random() * 10) + 's';
        container.appendChild(particle);
    }
}

/**
 * 添加元素进入动画
 */
function animateOnScroll() {
    const elements = document.querySelectorAll('.animate-on-scroll');
    
    const observer = new IntersectionObserver((entries) => {
        entries.forEach(entry => {
            if (entry.isIntersecting) {
                entry.target.style.opacity = '1';
                entry.target.style.transform = 'translateY(0)';
                observer.unobserve(entry.target);
            }
        });
    }, {
        threshold: 0.1,
        rootMargin: '0px 0px -50px 0px'
    });
    
    elements.forEach(el => observer.observe(el));
}

/**
 * 按钮点击波纹效果
 */
function addRippleEffect(button) {
    button.addEventListener('click', function(e) {
        const ripple = document.createElement('span');
        const rect = this.getBoundingClientRect();
        const size = Math.max(rect.width, rect.height);
        const x = e.clientX - rect.left - size / 2;
        const y = e.clientY - rect.top - size / 2;
        
        ripple.style.width = ripple.style.height = size + 'px';
        ripple.style.left = x + 'px';
        ripple.style.top = y + 'px';
        ripple.classList.add('ripple');
        
        this.appendChild(ripple);
        
        setTimeout(() => {
            ripple.remove();
        }, 600);
    });
}

/**
 * 初始化所有按钮的波纹效果
 */
function initRippleEffects() {
    const buttons = document.querySelectorAll('.btn');
    buttons.forEach(btn => addRippleEffect(btn));
}

/**
 * 数字计数动画
 */
function animateCounter(element, from, to, duration = 1000) {
    const start = performance.now();
    const difference = to - from;
    
    function update(currentTime) {
        const elapsed = currentTime - start;
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
 * 页面加载动画
 */
function initPageAnimations() {
    // 为统计项添加动画
    const statItems = document.querySelectorAll('.stat-item');
    statItems.forEach((item, index) => {
        item.style.opacity = '0';
        item.style.transform = 'scale(0.8)';
        setTimeout(() => {
            item.style.transition = 'all 0.5s cubic-bezier(0.4, 0, 0.2, 1)';
            item.style.opacity = '1';
            item.style.transform = 'scale(1)';
        }, index * 100);
    });
    
    // 为面板添加动画
    const panels = document.querySelectorAll('.panel');
    panels.forEach((panel, index) => {
        panel.style.opacity = '0';
        panel.style.transform = 'translateY(30px)';
        setTimeout(() => {
            panel.style.transition = 'all 0.6s cubic-bezier(0.4, 0, 0.2, 1)';
            panel.style.opacity = '1';
            panel.style.transform = 'translateY(0)';
        }, index * 150);
    });
}

/**
 * 初始化所有动画
 */
function initAllAnimations() {
    // 等待DOM加载完成
    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', () => {
            initParticles();
            initRippleEffects();
            animateOnScroll();
            initPageAnimations();
        });
    } else {
        initParticles();
        initRippleEffects();
        animateOnScroll();
        initPageAnimations();
    }
    
    // 窗口大小改变时重新初始化粒子
    let resizeTimer;
    window.addEventListener('resize', () => {
        clearTimeout(resizeTimer);
        resizeTimer = setTimeout(() => {
            initParticles();
        }, 250);
    });
}

// 自动初始化
initAllAnimations();
