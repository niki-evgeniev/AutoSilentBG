document.addEventListener('DOMContentLoaded', function () {
    const form = document.getElementById('stockUpdateForm');
    const button = document.getElementById('stockUpdateButton');
    if (!form || !button) return;
    const inputs = Array.from(form.querySelectorAll('.product-stock-input'));
    const errorBox = document.getElementById('stockUpdateError');
    const count = document.getElementById('stockChangedCount');
    let saving = false;

    function changed(input) {
        return input.value === '' || Number(input.value) !== Number(input.dataset.originalStock);
    }

    function refresh() {
        const edited = inputs.filter(changed);
        inputs.forEach(input => input.classList.toggle('stock-edited', changed(input)));
        count.textContent = form.dataset.changedLabel + ' ' + edited.length;
        button.disabled = saving || edited.length === 0;
    }
    inputs.forEach(input => input.addEventListener('input', refresh));
    refresh();

    form.addEventListener('submit', async function (event) {
        event.preventDefault();
        if (saving || !form.reportValidity()) return;
        const edited = inputs.filter(changed);
        if (!edited.length) return;
        if (edited.some(input => !Number.isInteger(Number(input.value)) || Number(input.value) < 0 || Number(input.value) > 2147483647)) {
            errorBox.textContent = form.dataset.invalid; errorBox.hidden = false; return;
        }
        const changes = edited.map(input => ({productId: Number(input.dataset.productId),
            originalStock: Number(input.dataset.originalStock), stock: Number(input.value)}));
        saving = true; errorBox.hidden = true;
        inputs.forEach(input => { input.readOnly = true; });
        refresh();
        try {
            const headers = {'Content-Type': 'application/json'};
            headers[document.querySelector('meta[name=_csrf_header]').content] = document.querySelector('meta[name=_csrf]').content;
            const response = await fetch(form.action, {method: 'POST', headers, body: JSON.stringify({changes})});
            const result = await response.json();
            if (!response.ok) throw new Error(result.error || form.dataset.error);
            window.location.href = form.dataset.returnUrl + '?updated=' + encodeURIComponent(result.updated);
        } catch (error) {
            errorBox.textContent = error.message || form.dataset.error;
            errorBox.hidden = false;
            saving = false;
            inputs.forEach(input => { input.readOnly = false; });
            refresh();
        }
    });
});
