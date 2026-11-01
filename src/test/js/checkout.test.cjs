const {test} = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const vm = require('node:vm');
const path = require('node:path');

const resources = path.join(__dirname, '../../main/resources');
const checkout = fs.readFileSync(path.join(resources, 'templates/checkout.html'), 'utf8');
const phonePattern = checkout.match(/pattern="([^"]+)"/)[1];

test('phone patterns compile with HTML Unicode sets and retain phone validation', () => {
    for (const template of ['checkout.html', 'product-details.html']) {
        const html = fs.readFileSync(path.join(resources, 'templates', template), 'utf8');
        for (const match of html.matchAll(/\bpattern="([^"]+)"/g)) {
            const pattern = new RegExp('^(?:' + match[1] + ')$', 'v');
            for (const valid of ['0888123456', '+359 888 123 456', '+359 (888) 123-456']) {
                assert.ok(pattern.test(valid), `${template}: ${valid}`);
            }
            for (const invalid of ['123', 'abcdefghij', '() -----', '0888/123456']) {
                assert.equal(pattern.test(invalid), false, `${template}: ${invalid}`);
            }
        }
    }
});

function storage(initial = {}) {
    const items = new Map(Object.entries(initial));
    return {getItem: key => items.get(key) ?? null, setItem: (key, value) => items.set(key, String(value)), removeItem: key => items.delete(key)};
}

function checkoutPage(phone = '+359 (888) 123-456') {
    const ready = [];
    const elements = new Map();
    const network = [];
    function element(value = '') {
        return {value, dataset: {}, listeners: {}, classList: {add() {}, remove() {}, toggle() {}},
            addEventListener(event, handler) { this.listeners[event] = handler; },
            setCustomValidity(message) { this.validationMessage = message; },
            checkValidity() { return !this.validationMessage; }, reportValidity() { return this.checkValidity(); }, focus() {}};
    }
    for (const [id, value] of Object.entries({cartCheckoutButton: '', checkoutFirstName: 'Ivan', checkoutLastName: 'Ivanov',
        checkoutEmail: 'ivan@example.com', checkoutPhone: phone, customerNote: '', checkoutError: ''})) {
        elements.set(id, element(value));
    }
    const phoneInput = elements.get('checkoutPhone');
    phoneInput.checkValidity = () => !phoneInput.validationMessage && new RegExp('^(?:' + phonePattern + ')$', 'v').test(phoneInput.value);
    const sessionStorage = storage();
    const window = {location: {hash: '', pathname: '/orders/checkout', href: ''},
        matchMedia: () => ({matches: false}), addEventListener() {}};
    const document = {body: {dataset: {phoneInvalid: 'Invalid phone'}},
        querySelector: () => null, querySelectorAll: () => [], getElementById: id => elements.get(id) || null,
        addEventListener: (event, handler) => { if (event === 'DOMContentLoaded') ready.push(handler); }};
    vm.runInNewContext(fs.readFileSync(path.join(resources, 'static/js/shop.js'), 'utf8'), {
        document, window, sessionStorage,
        localStorage: storage({'AutoSilent-cart': JSON.stringify([{id: '1', quantity: 2, price: 10}])}),
        fetch: async url => { network.push(url); throw new Error('First checkout step must not send an order'); }
    });
    ready.forEach(handler => handler());
    return {window, sessionStorage, network, button: elements.get('cartCheckoutButton')};
}

test('first checkout step saves the draft and navigates to delivery without creating an order', async () => {
    const page = checkoutPage();
    await page.button.listeners.click();
    assert.equal(page.window.location.href, '/orders/checkout/delivery');
    assert.equal(JSON.parse(page.sessionStorage.getItem('AutoSilent-checkout')).phone, '+359 (888) 123-456');
    assert.deepEqual(page.network, []);
});

test('invalid phone keeps the customer on the details step', async () => {
    const page = checkoutPage('abc');
    await page.button.listeners.click();
    assert.equal(page.window.location.href, '');
    assert.equal(page.sessionStorage.getItem('AutoSilent-checkout'), null);
    assert.deepEqual(page.network, []);
});
