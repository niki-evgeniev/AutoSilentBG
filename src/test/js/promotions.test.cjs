const {test} = require('node:test');
const assert = require('node:assert/strict');
const vm = require('node:vm');
const fs = require('node:fs');
const path = require('node:path');

function cartPage(initial, readyState = 'loading') {
    const ready = [], events = {}, nodes = new Map(), data = new Map([['AutoSilent-cart', JSON.stringify(initial)]]);
    function node() { return {dataset: {}, classList: {add() {}, remove() {}}, listeners: {},
        addEventListener(key, fn) { this.listeners[key] = fn; }, querySelectorAll: () => []}; }
    for (const id of ['cartItems', 'cartEmpty', 'cartSummary', 'cartSummaryCount', 'cartSummaryTotal',
        'checkoutDiscountAmount', 'checkoutDiscountPercent', 'promotionDiscountNotice', 'checkoutPromoCode', 'checkoutPromoApply']) nodes.set(id, node());
    const button = node();
    button.closest = () => ({dataset: {productId: '10', promotionId: '99', productName: 'Pack of 25', productPrice: '175', productUrl: '/promocii/99'},
        querySelector: selector => selector === '.quantity-control input' ? {value: '2'} : null});
    const document = {readyState, body: {dataset: {discountPercent: '20'}},
        addEventListener: (key, fn) => ready.push(fn), getElementById: id => nodes.get(id) || null,
        querySelector: () => null, querySelectorAll: selector => selector === '.add-cart, .product-add-cart' ? [button] : []};
    const window = {location: {hash: '', pathname: '/cart'}, matchMedia: () => ({matches: false}), addEventListener: (key, fn) => { events[key] = fn; }};
    vm.runInNewContext(fs.readFileSync(path.join(__dirname, '../../main/resources/static/js/shop.js'), 'utf8'),
        {document, window, localStorage: {getItem: key => data.get(key) ?? null, setItem: (key, value) => data.set(key, value)}, setTimeout: fn => fn()});
    if (readyState === 'loading') ready.forEach(fn => fn());
    button.listeners.click(); events['autosilent:cart-refresh']();
    return {cart: JSON.parse(data.get('AutoSilent-cart')), nodes};
}

test('promotion packs and the same ordinary product stay separate and only ordinary items receive discounts', () => {
    const {cart, nodes} = cartPage([{id: '10', name: 'Single sheet', price: 10, quantity: 1}]);
    assert.equal(cart.length, 2);
    assert.equal(cart[1].id, 'promotion-99');
    assert.equal(cart[1].productId, '10');
    assert.equal(cart[1].promotionId, '99');
    assert.equal(cart[1].quantity, 2);
    assert.equal(nodes.get('cartSummaryTotal').textContent, '358.00 €');
    assert.equal(nodes.get('checkoutDiscountAmount').textContent, '-2.00 €');
});

test('a cart containing only promotions has no discount and disables promo-code input', () => {
    const {nodes} = cartPage([]);
    assert.equal(nodes.get('cartSummaryTotal').textContent, '350.00 €');
    assert.equal(nodes.get('checkoutDiscountAmount').textContent, '-0.00 €');
    assert.equal(nodes.get('checkoutDiscountPercent').textContent, '0%');
    assert.equal(nodes.get('checkoutPromoCode').disabled, true);
    assert.equal(nodes.get('promotionDiscountNotice').hidden, false);
});

test('add-to-cart works when the script loads after DOMContentLoaded', () => {
    const {cart} = cartPage([], 'complete');
    assert.equal(cart.length, 1);
    assert.equal(cart[0].id, 'promotion-99');
    assert.equal(cart[0].quantity, 2);
});
