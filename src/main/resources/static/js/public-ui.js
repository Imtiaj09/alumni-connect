(function () {
    const doc = document;
    const body = doc.body;

    body.classList.add("page-enter");
    window.addEventListener("load", function () {
        body.classList.add("page-ready");
        body.classList.remove("page-enter");
    });

    if (window.AOS) {
        window.AOS.init({
            duration: 700,
            easing: "ease-out-cubic",
            once: true,
            offset: 30
        });
    }

    function initTheme() {
        const html = doc.documentElement;
        const saved = localStorage.getItem("alumni-theme");
        const preferred = saved || "dark";
        html.setAttribute("data-theme", preferred);

        const toggle = doc.getElementById("themeToggle");
        if (!toggle) return;
        const icon = toggle.querySelector("i");
        const applyIcon = function () {
            const isLight = html.getAttribute("data-theme") === "light";
            if (icon) {
                icon.className = isLight ? "fa-solid fa-sun" : "fa-solid fa-moon";
            }
        };
        applyIcon();
        toggle.addEventListener("click", function () {
            const next = html.getAttribute("data-theme") === "light" ? "dark" : "light";
            html.setAttribute("data-theme", next);
            localStorage.setItem("alumni-theme", next);
            applyIcon();
        });
    }

    function initCursor() {
        if ("ontouchstart" in window) return;
        const dot = doc.getElementById("cursorDot");
        const ring = doc.getElementById("cursorRing");
        if (!dot || !ring) return;
        body.classList.add("cursor-active");
        let rx = 0;
        let ry = 0;
        let mx = 0;
        let my = 0;
        window.addEventListener("mousemove", function (e) {
            mx = e.clientX;
            my = e.clientY;
            dot.style.transform = "translate(" + mx + "px," + my + "px)";
        });
        function tick() {
            rx += (mx - rx) * 0.18;
            ry += (my - ry) * 0.18;
            ring.style.transform = "translate(" + rx + "px," + ry + "px) translate(-50%,-50%)";
            requestAnimationFrame(tick);
        }
        tick();
    }

    function initRouteTransition() {
        doc.querySelectorAll("a.route-link").forEach(function (a) {
            a.addEventListener("click", function (e) {
                const href = a.getAttribute("href");
                if (!href || href.startsWith("#") || a.target === "_blank" || e.metaKey || e.ctrlKey) return;
                if (href.startsWith("http") && !href.includes(location.host)) return;
                e.preventDefault();
                body.classList.add("page-leave");
                setTimeout(function () {
                    window.location.href = href;
                }, 190);
            });
        });
    }

    function initHeroCounters() {
        const counters = doc.querySelectorAll(".counter[data-target]");
        if (!counters.length) return;
        const animateCounter = function (el) {
            const target = Number(el.dataset.target || "0");
            let current = 0;
            const step = Math.max(1, Math.ceil(target / 100));
            const runner = function () {
                current += step;
                if (current >= target) {
                    el.textContent = target.toLocaleString();
                    return;
                }
                el.textContent = current.toLocaleString();
                requestAnimationFrame(runner);
            };
            runner();
        };
        const observer = new IntersectionObserver(function (entries) {
            entries.forEach(function (entry) {
                if (entry.isIntersecting) {
                    animateCounter(entry.target);
                    observer.unobserve(entry.target);
                }
            });
        }, { threshold: 0.6 });
        counters.forEach(function (c) { observer.observe(c); });
    }

    function initParticles() {
        const canvas = doc.getElementById("heroParticles");
        if (!canvas) return;
        const ctx = canvas.getContext("2d");
        if (!ctx) return;
        const points = [];
        let w = 0;
        let h = 0;
        const density = 60;

        const resize = function () {
            w = canvas.width = canvas.offsetWidth;
            h = canvas.height = canvas.offsetHeight;
            points.length = 0;
            const count = Math.max(32, Math.floor((w * h) / (density * density)));
            for (let i = 0; i < count; i++) {
                points.push({
                    x: Math.random() * w,
                    y: Math.random() * h,
                    vx: (Math.random() - 0.5) * 0.45,
                    vy: (Math.random() - 0.5) * 0.45
                });
            }
        };

        const draw = function () {
            ctx.clearRect(0, 0, w, h);
            for (let i = 0; i < points.length; i++) {
                const p = points[i];
                p.x += p.vx;
                p.y += p.vy;
                if (p.x < 0 || p.x > w) p.vx *= -1;
                if (p.y < 0 || p.y > h) p.vy *= -1;

                ctx.beginPath();
                ctx.arc(p.x, p.y, 1.8, 0, Math.PI * 2);
                ctx.fillStyle = "rgba(235,240,255,.72)";
                ctx.fill();

                for (let j = i + 1; j < points.length; j++) {
                    const q = points[j];
                    const dx = p.x - q.x;
                    const dy = p.y - q.y;
                    const dist = Math.sqrt(dx * dx + dy * dy);
                    if (dist < 120) {
                        ctx.beginPath();
                        ctx.moveTo(p.x, p.y);
                        ctx.lineTo(q.x, q.y);
                        ctx.strokeStyle = "rgba(180,192,255," + ((120 - dist) / 420) + ")";
                        ctx.stroke();
                    }
                }
            }
            requestAnimationFrame(draw);
        };

        resize();
        window.addEventListener("resize", resize);
        draw();
    }

    function initPricingToggle() {
        const wrap = doc.querySelector("[data-pricing-toggle]");
        if (!wrap) return;
        const monthlyBtn = wrap.querySelector("[data-cycle='monthly']");
        const yearlyBtn = wrap.querySelector("[data-cycle='yearly']");
        const priceEls = doc.querySelectorAll("[data-monthly][data-yearly]");

        const setMode = function (mode) {
            priceEls.forEach(function (el) {
                const value = mode === "yearly" ? el.dataset.yearly : el.dataset.monthly;
                el.textContent = value || "";
            });
            monthlyBtn.classList.toggle("active", mode === "monthly");
            yearlyBtn.classList.toggle("active", mode === "yearly");
        };

        monthlyBtn.addEventListener("click", function () { setMode("monthly"); });
        yearlyBtn.addEventListener("click", function () { setMode("yearly"); });
    }

    function initTestimonials() {
        doc.querySelectorAll("[data-rotator]").forEach(function (root) {
            const items = root.querySelectorAll(".testimonial-item");
            if (items.length < 2) return;
            let i = 0;
            setInterval(function () {
                items[i].classList.remove("active");
                i = (i + 1) % items.length;
                items[i].classList.add("active");
            }, 3500);
        });
    }

    function initContactFormFlip() {
        const box = doc.getElementById("contactFlipBox");
        const form = doc.getElementById("contactForm");
        if (!box || !form) return;
        form.addEventListener("submit", function (e) {
            e.preventDefault();
            box.classList.add("submitted");
        });
    }

    function initGalleryPage() {
        const shell = doc.getElementById("galleryPage");
        if (!shell) return;

        const searchInput = doc.getElementById("gallerySearch");
        const yearSelect = doc.getElementById("batchYearFilter");
        const typeSelect = doc.getElementById("mediaTypeFilter");
        const chips = shell.querySelectorAll("[data-filter-chip]");
        const items = Array.from(shell.querySelectorAll(".gallery-item"));
        const masonry = doc.getElementById("galleryMasonry");
        const viewToggle = doc.getElementById("viewToggle");
        const sentinel = doc.getElementById("gallerySentinel");
        const bg = doc.getElementById("galleryParallaxBg");
        let currentFilter = "all";
        let shownCount = 9;

        const applyFilter = function () {
            const term = (searchInput ? searchInput.value : "").toLowerCase().trim();
            const year = yearSelect ? yearSelect.value : "";
            const type = typeSelect ? typeSelect.value : "all";
            let visible = 0;

            items.forEach(function (item, index) {
                const category = (item.dataset.category || "").toLowerCase();
                const itemType = (item.dataset.type || "").toLowerCase();
                const itemYear = item.dataset.year || "";
                const title = (item.dataset.title || "").toLowerCase();

                const okCategory = currentFilter === "all" || currentFilter === category;
                const okType = type === "all" || type === itemType;
                const okYear = !year || year === itemYear;
                const okTerm = !term || title.includes(term);
                const allowed = okCategory && okType && okYear && okTerm;

                item.classList.toggle("hidden", !allowed || index >= shownCount);
                if (allowed) visible++;
            });

            if (sentinel) {
                sentinel.style.display = visible > shownCount ? "block" : "none";
            }
        };

        chips.forEach(function (chip) {
            chip.addEventListener("click", function () {
                chips.forEach(function (c) { c.classList.remove("active"); });
                chip.classList.add("active");
                currentFilter = chip.dataset.filterChip || "all";
                shownCount = 9;
                applyFilter();
            });
        });

        [searchInput, yearSelect, typeSelect].forEach(function (el) {
            if (!el) return;
            el.addEventListener("input", function () {
                shownCount = 9;
                applyFilter();
            });
            el.addEventListener("change", function () {
                shownCount = 9;
                applyFilter();
            });
        });

        if (viewToggle && masonry) {
            viewToggle.addEventListener("click", function () {
                masonry.classList.toggle("masonry");
                if (masonry.classList.contains("masonry")) {
                    viewToggle.textContent = "Grid View";
                } else {
                    viewToggle.textContent = "Masonry View";
                }
            });
        }

        if (bg) {
            window.addEventListener("scroll", function () {
                const y = Math.min(180, window.scrollY * 0.25);
                bg.style.transform = "translateY(" + y + "px)";
            });
        }

        if (sentinel && "IntersectionObserver" in window) {
            const ob = new IntersectionObserver(function (entries) {
                entries.forEach(function (entry) {
                    if (entry.isIntersecting) {
                        shownCount += 6;
                        applyFilter();
                    }
                });
            }, { threshold: 0.5 });
            ob.observe(sentinel);
        }

        initGalleryLightbox(shell);
        applyFilter();
    }

    function initGalleryLightbox(shell) {
        const modal = doc.getElementById("galleryLightbox");
        if (!modal) return;
        const image = modal.querySelector("#lightboxImage");
        const video = modal.querySelector("#lightboxVideo");
        const title = modal.querySelector("#lightboxTitle");
        const meta = modal.querySelector("#lightboxMeta");
        const close = modal.querySelector("#lightboxClose");
        const prev = modal.querySelector("#lightboxPrev");
        const next = modal.querySelector("#lightboxNext");
        const tiles = Array.from(shell.querySelectorAll(".gallery-item"));
        let activeIndex = -1;

        const visibleTiles = function () {
            return tiles.filter(function (t) { return !t.classList.contains("hidden"); });
        };

        const render = function (tile) {
            if (!tile) return;
            const src = tile.dataset.src;
            const type = tile.dataset.type;
            const label = tile.dataset.title || "Gallery Item";
            const extra = (tile.dataset.year || "") + " • " + (tile.dataset.category || "");
            title.textContent = label;
            meta.textContent = extra;
            if (type === "video") {
                image.style.display = "none";
                video.style.display = "block";
                video.src = src;
            } else {
                video.pause();
                video.removeAttribute("src");
                video.style.display = "none";
                image.style.display = "block";
                image.src = src;
            }
            modal.classList.add("open");
            body.style.overflow = "hidden";
        };

        const move = function (dir) {
            const currentList = visibleTiles();
            if (!currentList.length) return;
            activeIndex = (activeIndex + dir + currentList.length) % currentList.length;
            render(currentList[activeIndex]);
        };

        shell.querySelectorAll(".gallery-item").forEach(function (tile) {
            tile.addEventListener("click", function () {
                const currentList = visibleTiles();
                activeIndex = currentList.indexOf(tile);
                render(tile);
            });
        });

        close.addEventListener("click", function () {
            modal.classList.remove("open");
            video.pause();
            body.style.overflow = "";
        });
        prev.addEventListener("click", function () { move(-1); });
        next.addEventListener("click", function () { move(1); });
        window.addEventListener("keydown", function (e) {
            if (!modal.classList.contains("open")) return;
            if (e.key === "Escape") close.click();
            if (e.key === "ArrowLeft") move(-1);
            if (e.key === "ArrowRight") move(1);
        });
    }

    function initLoginPage() {
        const loginForm = doc.getElementById("loginForm");
        const showPwd = doc.getElementById("toggleLoginPassword");
        const pwd = doc.getElementById("loginPassword");
        const forgot = doc.getElementById("forgotLink");
        const back = doc.getElementById("backToLogin");
        const flip = doc.getElementById("authFlip");
        if (showPwd && pwd) {
            showPwd.addEventListener("click", function () {
                const text = pwd.type === "password";
                pwd.type = text ? "text" : "password";
                showPwd.innerHTML = text ? "<i class='fa-regular fa-eye-slash'></i>" : "<i class='fa-regular fa-eye'></i>";
            });
        }
        if (forgot && back && flip) {
            forgot.addEventListener("click", function (e) {
                e.preventDefault();
                flip.classList.add("flipped");
            });
            back.addEventListener("click", function (e) {
                e.preventDefault();
                flip.classList.remove("flipped");
            });
        }
        if (loginForm) {
            loginForm.addEventListener("submit", function () {
                const btn = loginForm.querySelector("button[type='submit']");
                if (!btn) return;
                btn.disabled = true;
                btn.innerHTML = "<span class='spinner-border spinner-border-sm me-2'></span>Signing In";
            });
        }
    }

    function initRegisterWizard() {
        const wizard = doc.getElementById("registerWizard");
        if (!wizard) return;
        const steps = Array.from(wizard.querySelectorAll(".wizard-pane"));
        const indicators = Array.from(wizard.querySelectorAll(".wizard-step"));
        const nextBtns = wizard.querySelectorAll("[data-next]");
        const prevBtns = wizard.querySelectorAll("[data-prev]");
        const roleCards = wizard.querySelectorAll(".role-card");
        const roleInput = doc.getElementById("accountType");
        const pass = doc.getElementById("regPassword");
        const meterFill = doc.getElementById("pwdMeterFill");
        const meterText = doc.getElementById("pwdMeterText");
        const review = doc.getElementById("reviewSummary");
        const form = doc.getElementById("registerForm");
        const celebrate = doc.getElementById("celebrateOverlay");
        const schoolSelect = doc.getElementById("schoolSelect");
        const batchSelect = doc.getElementById("batchSelect");
        let current = 0;

        const update = function () {
            steps.forEach(function (step, i) {
                step.classList.toggle("active", i === current);
            });
            indicators.forEach(function (step, i) {
                step.classList.toggle("active", i === current);
                step.classList.toggle("done", i < current);
            });
        };

        const validateStep = function () {
            const currentStep = steps[current];
            if (!currentStep) return true;
            const required = currentStep.querySelectorAll("input,select,textarea");
            for (const field of required) {
                if (field.hasAttribute("required") && !field.checkValidity()) {
                    field.reportValidity();
                    return false;
                }
            }
            return true;
        };

        const buildReview = function () {
            if (!review) return;
            const pairs = [
                ["Account Type", roleInput ? roleInput.value : ""],
                ["Full Name", doc.getElementById("regFullName").value],
                ["Email", doc.getElementById("regEmail").value],
                ["Phone", doc.getElementById("regPhone").value],
                ["School", schoolSelect ? schoolSelect.options[schoolSelect.selectedIndex]?.text : ""],
                ["Batch", batchSelect ? batchSelect.options[batchSelect.selectedIndex]?.text : ""],
                ["Class/Section", doc.getElementById("regClassSection").value],
                ["Roll Number", doc.getElementById("regRollNumber").value],
                ["Bio", doc.getElementById("regBio").value]
            ];
            review.innerHTML = pairs.map(function (pair) {
                return "<div class='d-flex justify-content-between border-bottom py-2'><strong>" + pair[0] + "</strong><span>" + (pair[1] || "N/A") + "</span></div>";
            }).join("");
        };

        nextBtns.forEach(function (btn) {
            btn.addEventListener("click", function () {
                if (!validateStep()) return;
                if (current === steps.length - 2) buildReview();
                current = Math.min(steps.length - 1, current + 1);
                update();
            });
        });

        prevBtns.forEach(function (btn) {
            btn.addEventListener("click", function () {
                current = Math.max(0, current - 1);
                update();
            });
        });

        roleCards.forEach(function (card) {
            card.addEventListener("click", function () {
                roleCards.forEach(function (c) { c.classList.remove("active"); });
                card.classList.add("active");
                if (roleInput) roleInput.value = card.dataset.role || "";
            });
        });

        if (pass && meterFill && meterText) {
            pass.addEventListener("input", function () {
                const v = pass.value;
                let score = 0;
                if (v.length >= 8) score++;
                if (/[A-Z]/.test(v)) score++;
                if (/[0-9]/.test(v)) score++;
                if (/[^A-Za-z0-9]/.test(v)) score++;
                const width = (score / 4) * 100;
                meterFill.style.width = width + "%";
                const labels = ["Weak", "Fair", "Strong", "Excellent"];
                const colors = ["#e02424", "#f59e0b", "#2d8cff", "#19b36b"];
                meterFill.style.background = colors[Math.max(0, score - 1)] || "#e02424";
                meterText.textContent = labels[Math.max(0, score - 1)] || "Weak";
            });
        }

        if (schoolSelect && batchSelect) {
            schoolSelect.addEventListener("change", function () {
                const schoolId = schoolSelect.value;
                Array.from(batchSelect.options).forEach(function (opt, idx) {
                    if (idx === 0) return;
                    const allow = !schoolId || opt.dataset.school === schoolId;
                    opt.hidden = !allow;
                });
                batchSelect.value = "";
            });
        }

        if (form) {
            form.addEventListener("submit", function (e) {
                const terms = doc.getElementById("termsCheck");
                if (terms && !terms.checked) {
                    terms.reportValidity();
                    e.preventDefault();
                    return;
                }
                if (celebrate) {
                    celebrate.classList.add("show");
                    if (window.confetti) {
                        window.confetti({ particleCount: 160, spread: 90, origin: { y: 0.62 } });
                    }
                }
            });
        }

        update();
    }

    initTheme();
    initCursor();
    initRouteTransition();
    initHeroCounters();
    initParticles();
    initPricingToggle();
    initTestimonials();
    initContactFormFlip();
    initGalleryPage();
    initLoginPage();
    initRegisterWizard();
})();
