document.addEventListener('DOMContentLoaded', function () {
    const header = document.querySelector('.main-header');
    const cartButtons = document.querySelectorAll('.add-cart');

    if (typeof Swiper !== 'undefined') {
        new Swiper('.hero-swiper', {
            loop: true,
            speed: 750,
            effect: 'slide',
            autoplay: {
                delay: 3200,
                disableOnInteraction: false
            },
            pagination: {
                el: '.hero-swiper .swiper-pagination',
                clickable: true
            }
        });
    }

    window.addEventListener('scroll', function () {
        if (!header) {
            return;
        }

        if (window.scrollY > 20) {
            header.classList.add('header-scrolled');
        } else {
            header.classList.remove('header-scrolled');
        }
    });

    cartButtons.forEach(function (button) {
        button.addEventListener('click', function () {
            const originalText = button.textContent;
            button.textContent = document.body.dataset.cartAdded || 'Added ✓';
            button.disabled = true;

            setTimeout(function () {
                button.textContent = originalText;
                button.disabled = false;
            }, 1200);
        });
    });

    const canTilt = window.matchMedia('(hover: hover) and (pointer: fine)').matches &&
        !window.matchMedia('(prefers-reduced-motion: reduce)').matches;

    if (canTilt) {
        document.querySelectorAll('.tilt-card, .header-3d .cart-btn').forEach(function (card) {
            card.addEventListener('mousemove', function (event) {
                const rect = card.getBoundingClientRect();
                const x = (event.clientX - rect.left) / rect.width - 0.5;
                const y = (event.clientY - rect.top) / rect.height - 0.5;
                const lift = card.classList.contains('hero-slide-card') ? 18 : 8;
                const rotateX = y * -8;
                const rotateY = x * 10;

                card.classList.add('is-tilting');
                card.style.transform = 'translateY(-' + lift + 'px) rotateX(' + rotateX + 'deg) rotateY(' + rotateY + 'deg)';
            });

            card.addEventListener('mouseleave', function () {
                card.classList.remove('is-tilting');
                card.style.transform = '';
            });
        });
    }
});

document.addEventListener('DOMContentLoaded', function () {
    const priceRanges = document.querySelectorAll('.price-range');

    priceRanges.forEach(function (range) {
        const outputId = range.dataset.priceOutput;
        const output = document.getElementById(outputId);

        function updatePrice() {
            if (output) {
                output.textContent = range.value + ' ' + (document.body.dataset.currency || 'BGN');
            }
        }

        updatePrice();
        range.addEventListener('input', updatePrice);
    });
});
