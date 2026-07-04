document.addEventListener('DOMContentLoaded', function () {
    const header = document.querySelector('.main-header');
    const cartButtons = document.querySelectorAll('.add-cart, .product-add-cart');
    const cartStorageKey = 'nirton-cart';

    function readCart() {
        try {
            const cart = JSON.parse(localStorage.getItem(cartStorageKey) || '[]');
            return Array.isArray(cart) ? cart : [];
        } catch (error) {
            return [];
        }
    }

    function saveCart(cart) {
        localStorage.setItem(cartStorageKey, JSON.stringify(cart));
        updateCartHeader(cart);
    }

    function currency() {
        return document.body.dataset.currency || 'лв.';
    }

    function formatPrice(value) {
        return Number(value).toFixed(2) + ' ' + currency();
    }

    function updateCartHeader(cart) {
        const count = cart.reduce(function (sum, item) { return sum + item.quantity; }, 0);
        const total = cart.reduce(function (sum, item) { return sum + item.price * item.quantity; }, 0);
        document.querySelectorAll('.cart-count').forEach(function (element) {
            element.textContent = count;
            element.hidden = count === 0;
        });
        document.querySelectorAll('.cart-total').forEach(function (element) {
            element.textContent = formatPrice(total);
        });
    }

    function productFromButton(button) {
        const card = button.closest('[data-product-id], .product-card');
        if (!card) return null;

        const nameElement = card.querySelector('.product-name-link, h1, h3');
        const priceElement = card.querySelector('.price, .product-details-price');
        const name = card.dataset.productName || (nameElement ? nameElement.textContent.trim() : 'Продукт');
        const rawPrice = card.dataset.productPrice || (priceElement ? priceElement.textContent : '0');
        const price = Number(String(rawPrice).replace(',', '.').match(/[0-9]+(?:\.[0-9]+)?/)?.[0] || 0);
        const imageElement = card.querySelector('.product-image img, .product-gallery-main img');
        const quantityInput = card.querySelector('.quantity-control input');

        return {
            id: card.dataset.productId || name.toLowerCase().replace(/[^a-zа-я0-9]+/gi, '-'),
            name: name,
            price: price,
            image: card.dataset.productImage || (imageElement ? imageElement.src : ''),
            url: card.dataset.productUrl || '#',
            quantity: Math.max(1, Number(quantityInput ? quantityInput.value : 1) || 1)
        };
    }

    function addProduct(product) {
        const cart = readCart();
        const existing = cart.find(function (item) { return item.id === product.id; });
        if (existing) existing.quantity += product.quantity;
        else cart.push(product);
        saveCart(cart);
    }

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
            const product = productFromButton(button);
            if (!product) return;
            addProduct(product);

            const originalHtml = button.innerHTML;
            button.textContent = document.body.dataset.cartAdded || 'Added ✓';
            button.classList.add('is-added');
            button.disabled = true;

            setTimeout(function () {
                button.innerHTML = originalHtml;
                button.classList.remove('is-added');
                button.disabled = false;
            }, 1200);
        });
    });

    function escapeHtml(value) {
        return String(value || '')
            .replaceAll('&', '&amp;')
            .replaceAll('<', '&lt;')
            .replaceAll('>', '&gt;')
            .replaceAll('"', '&quot;')
            .replaceAll("'", '&#039;');
    }

    function renderCart() {
        const container = document.getElementById('cartItems');
        if (!container) return;

        const cart = readCart();
        const empty = document.getElementById('cartEmpty');
        const summary = document.getElementById('cartSummary');
        container.innerHTML = cart.map(function (item) {
            const image = item.image
                ? '<img src="' + escapeHtml(item.image) + '" alt="' + escapeHtml(item.name) + '">'
                : '<i class="bi bi-box-seam"></i>';
            return '<article class="cart-item" data-cart-id="' + escapeHtml(item.id) + '">' +
                '<a class="cart-item-image" href="' + escapeHtml(item.url) + '">' + image + '</a>' +
                '<div class="cart-item-info"><a href="' + escapeHtml(item.url) + '"><h2>' + escapeHtml(item.name) + '</h2></a>' +
                '<strong>' + formatPrice(item.price) + '</strong></div>' +
                '<label class="cart-item-quantity"><span>' + escapeHtml(document.body.dataset.cartQuantity || 'Количество') + '</span><input type="number" min="1" value="' + item.quantity + '"></label>' +
                '<strong class="cart-item-total">' + formatPrice(item.price * item.quantity) + '</strong>' +
                '<button class="cart-remove" type="button" aria-label="' + escapeHtml(document.body.dataset.cartRemove || 'Премахни') + '"><i class="bi bi-trash3"></i></button></article>';
        }).join('');

        empty.hidden = cart.length > 0;
        summary.hidden = cart.length === 0;
        const count = cart.reduce(function (sum, item) { return sum + item.quantity; }, 0);
        const total = cart.reduce(function (sum, item) { return sum + item.price * item.quantity; }, 0);
        document.getElementById('cartSummaryCount').textContent = count;
        document.getElementById('cartSummaryTotal').textContent = formatPrice(total);

        container.querySelectorAll('.cart-item').forEach(function (row) {
            const id = row.dataset.cartId;
            row.querySelector('input').addEventListener('change', function (event) {
                const updated = readCart();
                const item = updated.find(function (entry) { return entry.id === id; });
                if (item) item.quantity = Math.max(1, Number(event.target.value) || 1);
                saveCart(updated);
                renderCart();
            });
            row.querySelector('.cart-remove').addEventListener('click', function () {
                saveCart(readCart().filter(function (item) { return item.id !== id; }));
                renderCart();
            });
        });
    }

    updateCartHeader(readCart());
    renderCart();

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
