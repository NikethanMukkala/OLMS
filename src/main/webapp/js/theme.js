(function() {
    'use strict';
    
    const savedTheme = localStorage.getItem("theme");
    if (savedTheme) {
        document.documentElement.setAttribute("data-theme", savedTheme);
    } else if (window.matchMedia && window.matchMedia('(prefers-color-scheme: dark)').matches) {
        document.documentElement.setAttribute("data-theme", "dark");
    } else {
        document.documentElement.setAttribute("data-theme", "light");
    }
})();

document.addEventListener("DOMContentLoaded", () => {
    // Theme toggle
    let toggleBtn = document.getElementById("theme-toggle");
    if (!toggleBtn) {
        toggleBtn = document.createElement("button");
        toggleBtn.id = "theme-toggle";
        toggleBtn.title = "Toggle Theme";
        toggleBtn.innerHTML = "🌓";
        toggleBtn.className = "theme-toggle-btn";
        document.body.appendChild(toggleBtn);
    }

    toggleBtn.style.cssText = `
        position: fixed; bottom: 24px; right: 24px; 
        z-index: 10001; width: 56px; height: 56px;
        background: var(--surface-color); border-radius: 50%;
        border: 2px solid var(--border-color); cursor: pointer;
        font-size: 1.4rem; display: flex; align-items: center; justify-content: center;
        box-shadow: var(--shadow-lg); transition: all 0.3s cubic-bezier(0.23,1,0.32,1.2);
        backdrop-filter: blur(16px);
    `;

    toggleBtn.addEventListener("click", () => {
        const current = document.documentElement.getAttribute("data-theme");
        const next = current === "dark" ? "light" : "dark";
        document.documentElement.setAttribute("data-theme", next);
        localStorage.setItem("theme", next);
        
        // Animate toggle
        toggleBtn.style.transform = "scale(0.95) rotate(180deg)";
        setTimeout(() => toggleBtn.style.transform = "scale(1) rotate(0deg)", 150);
    });

    // IntersectionObserver for animation replay (tab switching, scroll-triggered)
    if ('IntersectionObserver' in window && document.querySelector('.books-bg-canvas')) {
        const observer = new IntersectionObserver((entries) => {
            entries.forEach(entry => {
                if (entry.isIntersecting) {
                    // Trigger animation replay
                    entry.target.classList.add('animate-in');
                }
            });
        }, { threshold: 0.1 });

        document.querySelectorAll('.books-bg-canvas, .login-form-container, .quotes-section').forEach(el => {
            observer.observe(el);
        });
    }
});
