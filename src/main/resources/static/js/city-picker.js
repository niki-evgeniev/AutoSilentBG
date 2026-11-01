// Keep city suggestions inside the page, including on mobile keyboards.
window.createCityPicker = function ({input, panel, list, status, label, onSelect}) {
    const root = input.parentElement;
    let items = [], matches = [], active = -1;
    const normalize = value => value.trim().toLocaleLowerCase().replace(/\s+/g, ' ');

    function close() {
        panel.hidden = true;
        input.setAttribute('aria-expanded', 'false');
        input.removeAttribute('aria-activedescendant');
        active = -1;
    }
    function resize() {
        if (panel.hidden) return;
        const viewport = window.visualViewport;
        const bottom = viewport ? viewport.offsetTop + viewport.height : window.innerHeight;
        const bounds = input.getBoundingClientRect();
        const below = bottom - bounds.bottom - 12;
        const above = bounds.top - (viewport?.offsetTop || 0) - 12;
        const openAbove = below < 140 && above > below;
        panel.classList.toggle('opens-above', openAbove);
        const space = openAbove ? above : below;
        panel.style.maxHeight = Math.max(44, Math.min(300, space)) + 'px';
    }
    function choose(item) {
        input.value = label(item);
        input.setCustomValidity('');
        close();
        onSelect(item);
        input.blur();
    }
    function highlight(index) {
        active = index;
        Array.from(list.children).forEach((option, i) => {
            option.setAttribute('aria-selected', String(i === active));
        });
        const option = list.children[active];
        if (option) {
            input.setAttribute('aria-activedescendant', option.id);
            option.scrollIntoView({block: 'nearest'});
        }
    }
    function render() {
        if (input.disabled) return;
        const query = normalize(input.value);
        const filtered = items.filter(item => normalize(label(item)).includes(query));
        // Prioritize matching city names over matching regions or postcodes.
        filtered.sort((a, b) => Number(normalize(b.name).startsWith(query)) - Number(normalize(a.name).startsWith(query)));
        matches = filtered.slice(0, 50);
        active = -1;
        input.removeAttribute('aria-activedescendant');
        list.replaceChildren();
        matches.forEach((item, index) => {
            const option = document.createElement('button');
            option.type = 'button'; option.className = 'delivery-city-option'; option.tabIndex = -1;
            option.id = 'delivery-city-option-' + index;
            option.setAttribute('role', 'option'); option.setAttribute('aria-selected', 'false');
            option.textContent = label(item);
            option.addEventListener('pointerdown', event => {
                // Preserve focus for mouse selection; allow touch scrolling normally.
                if (event.pointerType === 'mouse') event.preventDefault();
            });
            option.addEventListener('click', () => choose(item));
            list.appendChild(option);
        });
        status.textContent = !filtered.length ? status.dataset.empty : filtered.length > 50 ? status.dataset.refine : '';
        status.hidden = !status.textContent;
        panel.hidden = false;
        input.setAttribute('aria-expanded', 'true');
        resize();
    }
    input.addEventListener('focus', render);
    input.addEventListener('click', render);
    input.addEventListener('input', render);
    input.addEventListener('keydown', event => {
        if (event.key === 'Escape') { event.preventDefault(); close(); return; }
        if (event.key === 'Tab') { close(); return; }
        if (event.key === 'ArrowDown' || event.key === 'ArrowUp') {
            event.preventDefault();
            if (panel.hidden) render();
            if (matches.length) highlight(event.key === 'ArrowDown' ? Math.min(active + 1, matches.length - 1) : Math.max(active - 1, 0));
        } else if (event.key === 'Enter' && !panel.hidden) {
            event.preventDefault();
            if (active >= 0) choose(matches[active]);
            else if (matches.length === 1) choose(matches[0]);
        }
    });
    root.addEventListener('focusout', () => {
        requestAnimationFrame(() => { if (!root.contains(document.activeElement)) close(); });
    });
    document.addEventListener('pointerdown', event => { if (!root.contains(event.target)) close(); });
    window.addEventListener('resize', resize);
    window.addEventListener('scroll', resize, true);
    window.visualViewport?.addEventListener('resize', resize);
    window.visualViewport?.addEventListener('scroll', resize);
    return {setItems(value) { items = value; if (document.activeElement === input) render(); }, close};
};
