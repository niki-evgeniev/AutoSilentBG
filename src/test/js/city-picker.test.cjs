const {test} = require('node:test');
const assert = require('node:assert/strict');
const vm = require('node:vm');
const fs = require('node:fs');
const path = require('node:path');

function setup() {
    function element() {
        return {children: [], attributes: {}, listeners: {}, style: {}, dataset: {}, value: '', hidden: true,
            classList: {toggle() {}},
            addEventListener(name, fn) { this.listeners[name] = fn; },
            setAttribute(name, value) { this.attributes[name] = value; },
            removeAttribute(name) { delete this.attributes[name]; },
            replaceChildren() { this.children = []; }, appendChild(item) { this.children.push(item); },
            setCustomValidity(value) { this.validationMessage = value; },
            blur() { this.blurred = true; }, scrollIntoView() {},
            getBoundingClientRect() { return {top: 100, bottom: 148}; }};
    }
    const input = element(), panel = element(), list = element(), status = element();
    status.dataset = {empty: 'No results', refine: 'Keep typing'};
    input.parentElement = {addEventListener() {}, contains: () => true};
    const document = {createElement: element, addEventListener() {}};
    const window = {innerHeight: 800, addEventListener() {}};
    vm.runInNewContext(fs.readFileSync(path.join(__dirname, '../../main/resources/static/js/city-picker.js'), 'utf8'),
        {window, document, requestAnimationFrame: fn => fn()});
    let selected;
    const picker = window.createCityPicker({input, panel, list, status,
        label: item => item.name + ', ' + item.region, onSelect: item => { selected = item; }});
    const cities = [{id: 1, name: 'Пловдив', region: 'Пловдив'}, {id: 2, name: 'София', region: 'София-град'}];
    picker.setItems(cities);
    return {input, panel, list, status, picker, selected: () => selected};
}

test('typing filters cities inside the page and touch selection closes the keyboard', () => {
    const page = setup();
    page.input.value = 'пЛо'; page.input.listeners.input();
    assert.equal(page.panel.hidden, false);
    assert.equal(page.list.children.length, 1);
    assert.equal(page.list.children[0].textContent, 'Пловдив, Пловдив');
    page.list.children[0].listeners.click();
    assert.equal(page.selected().id, 1);
    assert.equal(page.input.value, 'Пловдив, Пловдив');
    assert.equal(page.panel.hidden, true);
    assert.equal(page.input.blurred, true);
});

test('keyboard navigation selects a city and Escape dismisses suggestions', () => {
    const page = setup();
    page.input.listeners.focus();
    page.input.listeners.keydown({key: 'ArrowDown', preventDefault() {}});
    assert.equal(page.input.attributes['aria-activedescendant'], 'delivery-city-option-0');
    page.input.listeners.keydown({key: 'Enter', preventDefault() {}});
    assert.equal(page.selected().id, 1);
    page.input.listeners.focus();
    page.input.listeners.keydown({key: 'Escape', preventDefault() {}});
    assert.equal(page.panel.hidden, true);
    assert.equal(page.input.attributes['aria-expanded'], 'false');
});

test('no-match search shows feedback without selecting a city', () => {
    const page = setup();
    page.input.value = 'missing'; page.input.listeners.input();
    assert.equal(page.list.children.length, 0);
    assert.equal(page.status.textContent, 'No results');
    assert.equal(page.status.hidden, false);
    assert.equal(page.selected(), undefined);
});
