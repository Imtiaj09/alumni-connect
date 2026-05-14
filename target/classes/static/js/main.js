// AOS ইনিশিয়ালাইজ
AOS.init({
    duration: 800,
    easing: "ease-in-out",
    once: true
});

// কাউন্টার অ্যানিমেশন
document.addEventListener("DOMContentLoaded", () => {
    const counters = document.querySelectorAll(".counter");
    const speed = 200; // অ্যানিমেশন স্পিড (ছোট = দ্রুত)

    const animate = () => {
        counters.forEach(counter => {
            const target = +counter.getAttribute("data-target");
            const count = +counter.innerText;
            const increment = target / speed;

            if (count < target) {
                counter.innerText = Math.ceil(count + increment);
                setTimeout(animate, 1);
            } else {
                counter.innerText = target.toLocaleString(); // কমা সহ ফরম্যাট (30,000)
            }
        });
    };

    // Intersection Observer দিয়ে ট্রিগার (স্ক্রলে দেখা গেলেই শুরু)
    const observer = new IntersectionObserver(entries => {
        entries.forEach(entry => {
            if (entry.isIntersecting) {
                animate();
                observer.unobserve(entry.target);
            }
        });
    }, { threshold: 0.5 });

    const statsSection = document.getElementById("stats-counter");
    if (statsSection) {
        observer.observe(statsSection);
    }
});
