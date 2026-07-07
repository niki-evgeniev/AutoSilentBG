document.addEventListener('DOMContentLoaded', function () {
    const header = document.querySelector('.main-header');
    const cartButtons = document.querySelectorAll('.add-cart, .product-add-cart');
    const cartStorageKey = 'nirton-cart';

    function updateActiveNavigation() {
        const navLinks = document.querySelectorAll('.navbar-nav .nav-link[data-nav]');
        const hashNavigation = {
            '#promo': 'promo',
            '#products': 'new-products',
            '#footer': 'contacts'
        };
        const activeNavigation = hashNavigation[window.location.hash] ||
            (window.location.pathname.startsWith('/products') ? 'products' :
                (window.location.pathname === '/' ? 'home' : null));

        navLinks.forEach(function (link) {
            const isActive = link.dataset.nav === activeNavigation;
            link.classList.toggle('active', isActive);
            if (isActive) link.setAttribute('aria-current', 'page');
            else link.removeAttribute('aria-current');
        });
    }

    updateActiveNavigation();
    window.addEventListener('hashchange', updateActiveNavigation);

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
        return document.body.dataset.currency || '€';
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
        const name = card.dataset.productName || (nameElement ? nameElement.textContent.trim() :
            (document.body.dataset.productDefaultName || 'Product'));
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
                '<div class="cart-item-quantity"><span>' + escapeHtml(document.body.dataset.cartQuantity || 'Quantity') + '</span>' +
                '<div class="quantity-stepper"><input class="cart-quantity-input" type="number" min="1" value="' + item.quantity + '">' +
                '<div class="quantity-step-arrows"><button type="button" data-quantity-action="plus" aria-label="' + escapeHtml(document.body.dataset.cartIncrease || 'Increase') + '"><i class="bi bi-chevron-up"></i></button>' +
                '<button type="button" data-quantity-action="minus" aria-label="' + escapeHtml(document.body.dataset.cartDecrease || 'Decrease') + '"><i class="bi bi-chevron-down"></i></button></div></div></div>' +
                '<strong class="cart-item-total">' + formatPrice(item.price * item.quantity) + '</strong>' +
                '<button class="cart-remove" type="button" aria-label="' + escapeHtml(document.body.dataset.cartRemove || 'Премахни') + '"><i class="bi bi-trash3"></i></button></article>';
        }).join('');

        empty.hidden = cart.length > 0;
        summary.hidden = cart.length === 0;
        const count = cart.reduce(function (sum, item) { return sum + item.quantity; }, 0);
        const subtotal = cart.reduce(function (sum, item) { return sum + item.price * item.quantity; }, 0);
        const discountPercent = Math.min(100, Math.max(0, Number(document.body.dataset.discountPercent) || 0));
        const discountAmount = subtotal * discountPercent / 100;
        const total = subtotal - discountAmount;
        document.getElementById('cartSummaryCount').textContent = count;
        document.getElementById('cartSummaryTotal').textContent = formatPrice(total);
        const discountPercentElement = document.getElementById('checkoutDiscountPercent');
        const discountAmountElement = document.getElementById('checkoutDiscountAmount');
        if (discountPercentElement) discountPercentElement.textContent = discountPercent + '%';
        if (discountAmountElement) discountAmountElement.textContent = '-' + formatPrice(discountAmount);

        container.querySelectorAll('.cart-item').forEach(function (row) {
            const id = row.dataset.cartId;
            const quantityInput = row.querySelector('.cart-quantity-input');

            function changeQuantity(quantity) {
                const updated = readCart();
                const item = updated.find(function (entry) { return entry.id === id; });
                if (item) item.quantity = Math.max(1, Number(quantity) || 1);
                saveCart(updated);
                renderCart();
            }

            quantityInput.addEventListener('change', function (event) {
                changeQuantity(event.target.value);
            });
            row.querySelectorAll('[data-quantity-action]').forEach(function (button) {
                button.addEventListener('click', function () {
                    const adjustment = button.dataset.quantityAction === 'plus' ? 1 : -1;
                    changeQuantity(Number(quantityInput.value) + adjustment);
                });
            });
            row.querySelector('.cart-remove').addEventListener('click', function () {
                saveCart(readCart().filter(function (item) { return item.id !== id; }));
                renderCart();
            });
        });
    }

    updateCartHeader(readCart());
    renderCart();

    const quickOrderForm = document.querySelector('.quick-order-form');
    if (quickOrderForm) {
        quickOrderForm.addEventListener('submit', function () {
            const productQuantity = document.getElementById('productQuantity');
            const quickOrderQuantity = document.getElementById('quickOrderQuantity');
            if (productQuantity && quickOrderQuantity) {
                quickOrderQuantity.value = productQuantity.value;
            }
        });
    }

    const checkoutButton = document.getElementById('cartCheckoutButton');
    if (checkoutButton) {
        checkoutButton.addEventListener('click', async function () {
            const cart = readCart();
            const firstName = document.getElementById('checkoutFirstName');
            const lastName = document.getElementById('checkoutLastName');
            const email = document.getElementById('checkoutEmail');
            const phone = document.getElementById('checkoutPhone');
            const customerNote = document.getElementById('customerNote');
            const errorBox = document.getElementById('checkoutError');
            const items = cart.map(function (item) {
                return { productId: Number(item.id), quantity: item.quantity };
            });

            errorBox.hidden = true;
            [firstName, lastName, email, phone].forEach(function (field) {
                if (field) {
                    field.setCustomValidity('');
                    field.classList.remove('is-invalid');
                }
            });
            if (!firstName.value.trim()) {
                firstName.setCustomValidity(document.body.dataset.firstNameRequired);
            }
            if (!lastName.value.trim()) {
                lastName.setCustomValidity(document.body.dataset.lastNameRequired);
            }
            if (email && !email.value.trim()) {
                email.setCustomValidity(document.body.dataset.emailRequired);
            } else if (email && !email.checkValidity()) {
                email.setCustomValidity(document.body.dataset.emailInvalid);
            }
            if (!phone.value.trim()) {
                phone.setCustomValidity(document.body.dataset.phoneRequired);
            } else if (!phone.checkValidity()) {
                phone.setCustomValidity(document.body.dataset.phoneInvalid);
            }
            const invalidField = [firstName, lastName, email, phone].find(function (field) {
                return field && !field.checkValidity();
            });
            if (invalidField) {
                invalidField.classList.add('is-invalid');
                invalidField.reportValidity();
                invalidField.focus();
                return;
            }
            if (items.some(function (item) { return !Number.isInteger(item.productId); })) {
                errorBox.textContent = document.body.dataset.invalidCart || 'The cart contains an invalid product.';
                errorBox.hidden = false;
                return;
            }

            checkoutButton.disabled = true;
            try {
                const csrfToken = document.querySelector('meta[name="_csrf"]')?.content;
                const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.content;
                const headers = { 'Content-Type': 'application/json' };
                if (csrfToken && csrfHeader) headers[csrfHeader] = csrfToken;

                const response = await fetch('/orders/cart', {
                    method: 'POST',
                    headers: headers,
                    body: JSON.stringify({
                        items: items,
                        firstName: firstName.value.trim(),
                        lastName: lastName.value.trim(),
                        email: email ? email.value.trim() : null,
                        phone: phone.value.trim(),
                        customerNote: customerNote ? customerNote.value.trim() : ''
                    })
                });
                const result = await response.json();
                if (!response.ok) throw new Error(result.error || document.body.dataset.orderFailed || 'Order creation failed.');

                localStorage.removeItem(cartStorageKey);
                window.location.href = result.redirectUrl;
            } catch (error) {
                errorBox.textContent = error.message;
                errorBox.hidden = false;
                checkoutButton.disabled = false;
            }
        });

        document.querySelectorAll('.checkout-required').forEach(function (field) {
            field.addEventListener('input', function () {
                field.setCustomValidity('');
                field.classList.remove('is-invalid');
            });
        });
    }

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
                output.textContent = range.value + ' ' + (document.body.dataset.currency || '€');
            }
        }

        updatePrice();
        range.addEventListener('input', updatePrice);
    });
});
