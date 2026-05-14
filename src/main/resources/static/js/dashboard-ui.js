document.addEventListener("DOMContentLoaded", function () {
    const root = document.documentElement;
    const key = "alumni-dashboard-theme";
    const savedTheme = localStorage.getItem(key) || "dark";
    root.setAttribute("data-dashboard-theme", savedTheme);

    function setToggleIcon(btn) {
        const icon = btn.querySelector("i");
        if (!icon) return;
        const dark = root.getAttribute("data-dashboard-theme") === "dark";
        icon.className = dark ? "fa-regular fa-sun" : "fa-regular fa-moon";
    }

    document.querySelectorAll(".js-theme-toggle").forEach(function (btn) {
        setToggleIcon(btn);
        btn.addEventListener("click", function () {
            const current = root.getAttribute("data-dashboard-theme") || "dark";
            const next = current === "dark" ? "light" : "dark";
            root.setAttribute("data-dashboard-theme", next);
            localStorage.setItem(key, next);
            document.querySelectorAll(".js-theme-toggle").forEach(setToggleIcon);
        });
    });

    document.querySelectorAll(".kpi-value").forEach(function (el) {
        const rawValue = el.getAttribute("data-kpi-target") || el.textContent || "";
        const raw = rawValue.replace(/[^0-9.]/g, "");
        const target = Number(raw);
        if (!Number.isFinite(target) || target <= 0) return;
        let current = 0;
        const step = Math.max(1, Math.ceil(target / 42));
        const tick = function () {
            current += step;
            if (current >= target) {
                el.textContent = target.toLocaleString();
                return;
            }
            el.textContent = current.toLocaleString();
            requestAnimationFrame(tick);
        };
        tick();
    });
});
