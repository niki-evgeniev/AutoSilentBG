document.addEventListener('DOMContentLoaded', async function () {
    const form = document.getElementById('deliveryForm');
    if (!form) return;
    let draft;
    try { draft = JSON.parse(sessionStorage.getItem('AutoSilent-checkout')); } catch (_) { /* Missing draft. */ }
    if (!draft || draft.identity !== (document.body.dataset.customerEmail || '') || Date.now() - draft.savedAt >= 86400000
        || !draft.firstName || !draft.lastName || !draft.phone || !draft.email) {
        window.location.replace('/orders/checkout'); return;
    }
    const city = document.getElementById('deliveryCity');
    const office = document.getElementById('deliveryOffice');
    const address = document.getElementById('deliveryAddress');
    const confirm = document.getElementById('deliveryConfirm');
    const loadError = document.getElementById('shippingLoadError');
    const retry = document.getElementById('shippingRetry');
    const error = document.getElementById('checkoutError');
    const saved = draft.delivery || {};
    let cities = [], offices = [], selectedCity = null, loadingOffices = false, requestVersion = 0, promoReady = !draft.promoCode, submitting = false;

    document.getElementById('deliveryRecipient').textContent = [draft.firstName + ' ' + draft.lastName, draft.phone, draft.email, draft.customerNote].filter(Boolean).join('\n');
    function headers() {
        return {'Content-Type': 'application/json', [document.querySelector('meta[name=_csrf_header]').content]: document.querySelector('meta[name=_csrf]').content};
    }
    function type() { return form.elements.deliveryType.value; }
    function label(item) { return item.name + (item.region ? ', ' + item.region : '') + (item.postcode ? ' (' + item.postcode + ')' : ''); }
    function save() {
        draft.delivery = {cityId: selectedCity?.id || null, type: type(), officeId: office.value ? Number(office.value) : null, address: address.value};
        draft.savedAt = Date.now();
        sessionStorage.setItem('AutoSilent-checkout', JSON.stringify(draft));
    }
    function refreshValidity() {
        confirm.disabled = submitting || !promoReady || !selectedCity || (type() === 'OFFICE' && (loadingOffices || !office.value));
    }
    function showLoadError() {
        loadError.textContent = document.body.dataset.shippingError; loadError.hidden = false; retry.hidden = false;
    }
    function showOfficeAddress() {
        document.getElementById('officeAddress').textContent = offices.find(item => String(item.id) === office.value)?.address || '';
    }
    function setType() {
        const isOffice = type() === 'OFFICE';
        document.getElementById('officeFields').hidden = !isOffice;
        document.getElementById('addressFields').hidden = isOffice;
        office.required = isOffice; office.disabled = !isOffice || !selectedCity || loadingOffices;
        address.required = !isOffice; address.disabled = isOffice;
        refreshValidity(); save();
    }
    async function loadOffices(restoreId) {
        const version = ++requestVersion;
        loadingOffices = true; offices = []; office.replaceChildren(); office.disabled = true;
        showOfficeAddress(); refreshValidity();
        if (!selectedCity) { loadingOffices = false; refreshValidity(); return; }
        try {
            const response = await fetch('/delivery/econt/offices?cityId=' + selectedCity.id);
            if (!response.ok) throw new Error();
            const items = await response.json();
            if (version !== requestVersion) return;
            offices = items;
            office.add(new Option(items.length ? document.body.dataset.selectOffice : document.body.dataset.noOffices, ''));
            items.forEach(item => office.add(new Option(item.name + ' — ' + item.address, item.id)));
            if (restoreId && items.some(item => String(item.id) === String(restoreId))) office.value = String(restoreId);
            loadError.hidden = true; retry.hidden = true;
        } catch (_) { if (version === requestVersion) showLoadError(); }
        finally {
            if (version === requestVersion) {
                loadingOffices = false; office.disabled = type() !== 'OFFICE';
                showOfficeAddress(); refreshValidity(); save();
            }
        }
    }
    async function loadCities() {
        confirm.disabled = true; city.disabled = true;
        try {
            const response = await fetch('/delivery/econt/cities');
            if (!response.ok) throw new Error();
            cities = await response.json();
            if (!cities.length) throw new Error();
            const list = document.getElementById('deliveryCities'); list.replaceChildren();
            cities.forEach(item => { const option = document.createElement('option'); option.value = label(item); list.appendChild(option); });
            selectedCity = cities.find(item => item.id === saved.cityId) || null;
            city.value = selectedCity ? label(selectedCity) : '';
            city.disabled = false; loadError.hidden = true; retry.hidden = true;
            await loadOffices(saved.officeId);
        } catch (_) { showLoadError(); }
        refreshValidity();
    }
    city.addEventListener('input', function () {
        city.setCustomValidity('');
        const next = cities.find(item => label(item) === city.value) || null;
        if (next?.id === selectedCity?.id) return;
        selectedCity = next; loadOffices(); save();
    });
    city.addEventListener('change', function () {
        city.setCustomValidity(selectedCity ? '' : document.body.dataset.shippingInvalid);
        city.reportValidity();
    });
    office.addEventListener('change', () => { showOfficeAddress(); refreshValidity(); save(); });
    address.addEventListener('input', save);
    form.querySelectorAll('[name=deliveryType]').forEach(input => input.addEventListener('change', setType));
    retry.addEventListener('click', () => cities.length ? loadOffices() : loadCities());
    if (saved.type === 'ADDRESS') form.elements.deliveryType.value = 'ADDRESS';
    address.value = saved.address || '';
    // Restore selection before saving the initial form state.
    document.getElementById('officeFields').hidden = type() !== 'OFFICE';
    document.getElementById('addressFields').hidden = type() === 'OFFICE';
    address.disabled = type() === 'OFFICE'; address.required = type() === 'ADDRESS'; office.required = type() === 'OFFICE';

    form.addEventListener('submit', async function (event) {
        event.preventDefault();
        if (submitting || !form.reportValidity() || !selectedCity || !promoReady || (type() === 'OFFICE' && (!office.value || loadingOffices))) return;
        let cart;
        try { cart = JSON.parse(localStorage.getItem('AutoSilent-cart') || '[]'); } catch (_) { cart = []; }
        if (!Array.isArray(cart) || !cart.length) { window.location.href = '/cart'; return; }
        submitting = true; refreshValidity(); error.hidden = true;
        try {
            save();
            const response = await fetch('/orders/cart', {method: 'POST', headers: headers(), body: JSON.stringify({
                firstName: draft.firstName, lastName: draft.lastName, email: draft.email, phone: draft.phone,
                customerNote: draft.customerNote, promoCode: draft.promoCode,
                items: cart.map(item => ({productId: Number(item.id), quantity: item.quantity})), delivery: draft.delivery
            })});
            const result = await response.json();
            if (!response.ok) throw new Error(result.error || document.body.dataset.orderFailed);
            localStorage.removeItem('AutoSilent-cart'); sessionStorage.removeItem('AutoSilent-checkout');
            window.location.href = result.redirectUrl;
        } catch (failure) { error.textContent = failure.message || document.body.dataset.orderFailed; error.hidden = false; submitting = false; refreshValidity(); }
    });
    // Revalidate discounts before the customer can confirm the order.
    if (draft.promoCode) {
        try {
            const response = await fetch('/orders/promo-code', {method: 'POST', headers: headers(), body: JSON.stringify({promoCode: draft.promoCode})});
            const result = await response.json();
            if (!response.ok) throw new Error(result.error || document.body.dataset.orderFailed);
            document.body.dataset.discountPercent = String(result.totalDiscountPercent || 0);
            // Re-render the shared cart totals after loading the validated discount.
            window.dispatchEvent(new Event('autosilent:cart-refresh'));
            promoReady = true;
        } catch (failure) { error.textContent = failure.message; error.hidden = false; }
    }
    await loadCities();
});
