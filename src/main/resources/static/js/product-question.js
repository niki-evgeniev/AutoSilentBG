document.addEventListener('DOMContentLoaded', function () {
    const form = document.getElementById('productQuestionForm');
    if (!form) return;
    // Fetch only after opening the form; leave any already edited field alone.
    let loaded = false;
    document.getElementById('productQuestionPanel').addEventListener('show.bs.collapse', async function () {
        if (loaded) return;
        loaded = true;
        try {
            const response = await fetch('/orders/checkout/customer');
            if (!response.ok) return;
            const customer = await response.json();
            const values = {name: [customer.firstName, customer.lastName].filter(Boolean).join(' ').slice(0, 100), phone: customer.phone, email: customer.email};
            Object.entries(values).forEach(([key, value]) => {
                if (!form.elements[key].value && value) form.elements[key].value = value;
            });
        } catch (_) { /* The customer can fill in the form manually. */ }
    });
    form.addEventListener('submit', async function (event) {
        event.preventDefault();
        if (!form.reportValidity()) return;
        const button = form.querySelector('[type=submit]');
        if (button.disabled) return;
        const result = document.getElementById('questionResult');
        button.disabled = true;
        try {
            const headers = {'Content-Type': 'application/json'};
            headers[document.querySelector('meta[name=_csrf_header]').content] = document.querySelector('meta[name=_csrf]').content;
            const response = await fetch(form.dataset.url, {method: 'POST', headers,
                body: JSON.stringify(Object.fromEntries(new FormData(form)))});
            if (!response.ok) throw new Error(response.status === 429 ? form.dataset.throttled : form.dataset.error);
            result.textContent = form.dataset.success;
            result.className = 'alert alert-success';
            form.elements.question.value = '';
        } catch (error) {
            result.textContent = error.message || form.dataset.error;
            result.className = 'alert alert-danger';
        } finally { result.hidden = false; button.disabled = false; }
    });
});
