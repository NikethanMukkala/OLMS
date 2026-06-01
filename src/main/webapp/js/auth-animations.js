(function() {
    'use strict';

    // ═══════════════════════════════════════════════════════════════════════════════════════
    // SHARED AUTH PAGE ANIMATIONS - Books, Sparkles, Quotes, Form Staggers
    // ═══════════════════════════════════════════════════════════════════════════════════════

    /**
     * Initialize floating book particles on canvas
     * @param {string} selector - Canvas element selector
     * @param {number} count - Number of book particles
     */
    function initBookParticles(canvasSel, count = 16) {
        const canvas = document.querySelector(canvasSel);
        if (!canvas) return;

        const bookEmojis = ['📚','📖','📕','📗','📘','📙','🔖','📜','📝','✏️','🖊️','📐','🎓','📓','📔'];
        const sparkleColors = ['#4361ee','#818cf8','#10b981','#6c63ff','#f59e0b','#f87171'];

        // Books
        for (let i = 0; i < count; i++) {
            const el = document.createElement('span');
            el.className = 'book-particle';
            const dur = 13 + Math.random() * 10;
            const delay = -Math.random() * dur;
            const x = Math.random() * 96;
            const mop = 0.13 + Math.random() * 0.16;
            const r0 = (Math.random() * 30 - 15) + 'deg';
            const r1 = (Math.random() * 30 - 15) + 'deg';

            el.style.cssText = `
                left: ${x}%;
                --dur: ${dur}s;
                --delay: ${delay}s;
                --mop: ${mop};
                --r0: ${r0};
                --r1: ${r1};
            `;
            el.textContent = bookEmojis[Math.floor(Math.random() * bookEmojis.length)];
            canvas.appendChild(el);
        }

        // Sparkles
        for (let i = 0; i < 28; i++) {
            const sp = document.createElement('span');
            sp.className = 'sparkle';
            sp.style.cssText = `
                left: ${Math.random()*98}%;
                top: ${Math.random()*98}%;
                --dur: ${2.5 + Math.random()*3.5}s;
                --delay: ${-Math.random()*5}s;
                background: ${sparkleColors[Math.floor(Math.random()*sparkleColors.length)]};
                width: ${3+Math.random()*4}px;
                height: ${3+Math.random()*4}px;
            `;
            canvas.appendChild(sp);
        }
    }

    /**
     * Rotating quote carousel
     * @param {string} textSel - Quote text selector  
     * @param {string} authorSel - Author selector
     * @param {array} quotes - Array of {text, author}
     * @param {number} intervalMs - Rotation interval
     */
    function initQuoteCarousel(textSel, authorSel, quotes, intervalMs = 5000) {
        const qText = document.querySelector(textSel);
        const qAuth = document.querySelector(authorSel);
        if (!qText || !qAuth) return;

        let qIndex = 0;
        
        function rotate() {
            qText.style.opacity = '0';
            qAuth.style.opacity = '0';
            setTimeout(() => {
                qIndex = (qIndex + 1) % quotes.length;
                qText.textContent = '"' + quotes[qIndex].text + '"';
                qAuth.textContent = quotes[qIndex].author;
                qText.style.opacity = '1';
                qAuth.style.opacity = '1';
            }, 500);
        }

        // Initial quote
        qText.textContent = '"' + quotes[0].text + '"';
        qAuth.textContent = quotes[0].author;
        
        setInterval(rotate, intervalMs);
    }

    /**
     * Staggered form field entrance animation
     * @param {string} selector - Form group selector
     */
    function animateFormFields(groupSel) {
        document.querySelectorAll(groupSel).forEach((group, i) => {
            group.style.animation = `fadeUpIn 0.45s ${0.05 + i * 0.08}s cubic-bezier(0.34,1.56,0.64,1) both`;
        });
    }

    /**
     * Tab switching with animation replay
     * @param {string} tabContentSel - Tab content selector
     * @param {string} tabBtnSel - Tab button selector
     */
    function initTabs(tabContentSel, tabBtnSel) {
        const contents = document.querySelectorAll(tabContentSel);
        const buttons = document.querySelectorAll(tabBtnSel);

        buttons.forEach(btn => {
            btn.addEventListener('click', () => {
                const targetTab = btn.dataset.tab;
                if (!targetTab) return;
                
                // Deactivate all
                buttons.forEach(b => b.classList.remove('active'));
                contents.forEach(c => c.classList.remove('active'));
                
                // Activate target
                btn.classList.add('active');
                const targetContent = document.getElementById(targetTab + '-form');
                if (targetContent) {
                    targetContent.classList.add('active');
                }
                
                // Replay form animations
                setTimeout(() => animateFormFields('.tab-content.active .form-group'), 150);
            });
        });
    }

    /**
     * Mouse parallax for books canvas (subtle)
     * @param {string} canvasSel - Canvas selector
     */
    function initParallax(canvasSel) {
        const canvas = document.querySelector(canvasSel);
        if (!canvas) return;

        let mouseX = 0, mouseY = 0;
        document.addEventListener('mousemove', (e) => {
            mouseX = (e.clientX / window.innerWidth) * 10;
            mouseY = (e.clientY / window.innerHeight) * 10;
            canvas.style.transform = `translate(${mouseX}px, ${mouseY}px)`;
        });
    }

    // ═══════════════════════════════════════════════════════════════════════════════════════
    // AUTO-INITIALIZE ON DOM READY
    // ═══════════════════════════════════════════════════════════════════════════════════════
    
    const commonQuotes = [
        { text: "A room without books is like a body without a soul.", author: "Cicero" },
        { text: "So many books, so little time.", author: "Frank Zappa" },
        { text: "There is no friend as loyal as a book.", author: "Ernest Hemingway" },
        { text: "Books are a uniquely portable magic.", author: "Stephen King" },
        { text: "That is part of the beauty of all literature.", author: "F. Scott Fitzgerald" },
        { text: "Knowledge is power.", author: "Francis Bacon" },
        { text: "I have always imagined that Paradise will be a kind of library.", author: "Jorge Luis Borges" },
        { text: "We read to know we're not alone.", author: "William Nicholson" },
        { text: "A reader lives a thousand lives before he dies.", author: "George R.R. Martin" },
        { text: "The only thing that you absolutely have to know, is the location of the library.", author: "Albert Einstein" }
    ];

    document.addEventListener('DOMContentLoaded', () => {
        // Initialize books if canvas exists
        if (document.querySelector('.books-bg-canvas')) {
            initBookParticles('#books-bg', document.querySelector('.books-bg-canvas')?.dataset?.bookCount || 16);
            initParallax('.books-bg-canvas');
        }

        // Initialize quotes if present
        const quoteText = document.querySelector('#quote-text');
        const quoteAuth = document.querySelector('#quote-author');
        if (quoteText && quoteAuth) {
            initQuoteCarousel('#quote-text', '#quote-author', commonQuotes);
        }

        // Animate forms
        animateFormFields('.form-group');

    // Init tabs if present (login page)
        if (document.querySelector('.tabs')) {
            initTabs('.tab-content', '.tab-btn');
        }
    });

    // Mobile validation for signup
    window.validateMobile = function() {
    const mobile = document.getElementById("mobile")?.value;
    if (!mobile || !/^\d{10}$/.test(mobile)) {
        const errorEl = document.getElementById("mobile-error");
        if (errorEl) errorEl.style.display = "block";
        return false;
    }
    const errorEl = document.getElementById("mobile-error");
    if (errorEl) errorEl.style.display = "none";
    return true;
};

// Export for manual calls if needed
window.AuthAnimations = {
    initBookParticles,
    initQuoteCarousel,
    animateFormFields,
    initTabs,
    initParallax,
    validateMobile
};

})();

