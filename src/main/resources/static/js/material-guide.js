(() => {
  'use strict';
  const search = document.getElementById('material-search');
  const cards = [...document.querySelectorAll('.material')];
  const status = document.getElementById('filter-status');
  const empty = document.getElementById('no-results');
  const normalize = value => value.toLocaleLowerCase('bg').trim();
  const applyFilter = () => {
    const query = normalize(search.value);
    let count = 0;
    cards.forEach(card => {
      const matches = normalize(card.textContent).includes(query);
      card.hidden = !matches;
      if (matches) count++;
    });
    status.textContent = `${count} от ${cards.length} материала`;
    empty.hidden = count !== 0;
  };
  document.getElementById('filter-bar').hidden = false;
  search.addEventListener('input', applyFilter);
  applyFilter();
  // Keep deep links usable even after a search has hidden their target card.
  const syncHash = () => {
    const id = location.hash.slice(1);
    const target = document.getElementById(id);
    if (target?.classList.contains('material') && target.hidden) {
      search.value = '';
      applyFilter();
      target.scrollIntoView();
    }
    document.querySelectorAll('[data-zone]').forEach(link => {
      if (link.dataset.zone === id) link.setAttribute('aria-current', 'location');
      else link.removeAttribute('aria-current');
    });
    document.getElementById('selection-status').textContent =
      target?.classList.contains('zone-card') ? `Избрана зона: ${target.querySelector('.zone-number').textContent.split(' / ')[1]}.` : '';
  };
  window.addEventListener('hashchange', syncHash);
  document.querySelectorAll('a[href^="#"]').forEach(link => {
    link.addEventListener('click', () => {
      const target = document.getElementById(link.hash.slice(1));
      if (target?.classList.contains('material') && target.hidden) {
        search.value = '';
        applyFilter();
      }
    });
  });
  syncHash();
})();
