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
        if (window.scrollY > 20) {
            header.classList.add('header-scrolled');
        } else {
            header.classList.remove('header-scrolled');
        }
    });

    cartButtons.forEach(function (button) {
        button.addEventListener('click', function () {
            const originalText = button.textContent;
            button.textContent = 'Добавено ✓';
            button.disabled = true;

            setTimeout(function () {
                button.textContent = originalText;
                button.disabled = false;
            }, 1200);
        });
    });
});

document.addEventListener('DOMContentLoaded', function () {
    const priceRanges = document.querySelectorAll('.price-range');

    priceRanges.forEach(function (range) {
        const outputId = range.dataset.priceOutput;
        const output = document.getElementById(outputId);

        function updatePrice() {
            if (output) {
                output.textContent = range.value + ' лв.';
            }
        }

        updatePrice();
        range.addEventListener('input', updatePrice);
    });
});
