document.addEventListener('DOMContentLoaded', function () {
    const container = document.getElementById('kitComponents');
    const add = document.getElementById('addKitComponent');
    if (!container || !add) return;
    const form = container.closest('form');

    function updateThumbnail(row) {
        const select = row.querySelector('.kit-component-product');
        const image = row.querySelector('.kit-component-image');
        const placeholder = row.querySelector('.kit-component-placeholder');
        const imageUrl = select.selectedOptions[0]?.dataset.image;
        image.hidden = !imageUrl;
        placeholder.hidden = !!imageUrl;
        if (imageUrl) image.src = imageUrl;
        else image.removeAttribute('src');
    }

    container.querySelectorAll('.kit-component-row').forEach(updateThumbnail);

    function renumber() {
        container.querySelectorAll('.kit-component-row').forEach(function (row, index) {
            row.querySelector('.kit-component-product').name = 'components[' + index + '].productId';
            row.querySelector('.kit-component-quantity').name = 'components[' + index + '].quantity';
        });
    }

    add.addEventListener('click', function () {
        const source = container.querySelector('.kit-component-row');
        if (!source) return;
        const row = source.cloneNode(true);
        row.querySelector('.kit-component-product').value = '';
        row.querySelector('.kit-component-quantity').value = '1';
        container.appendChild(row);
        renumber();
        updateThumbnail(row);
    });

    container.addEventListener('click', function (event) {
        const button = event.target.closest('.kit-component-remove');
        if (!button) return;
        if (container.querySelectorAll('.kit-component-row').length === 1) return;
        button.closest('.kit-component-row').remove();
        renumber();
    });

    container.addEventListener('change', function (event) {
        if (event.target.matches('.kit-component-product')) {
            event.target.setCustomValidity('');
            updateThumbnail(event.target.closest('.kit-component-row'));
        }
    });

    form.addEventListener('submit', function (event) {
        const selected = new Set();
        for (const select of container.querySelectorAll('.kit-component-product')) {
            select.setCustomValidity('');
            if (select.value && selected.has(select.value)) {
                select.setCustomValidity('Този продукт вече е добавен в кита.');
                select.reportValidity();
                event.preventDefault();
                return;
            }
            if (select.value) selected.add(select.value);
        }
    });
});
