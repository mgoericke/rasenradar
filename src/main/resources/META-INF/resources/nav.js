// Phones: the competitions sit in one swipeable row — scroll the active one into view, so
// "Champions League" isn't hidden past the right edge. Runs again after every hx-boost swap,
// which replaces the body and with it the navigation.
(function () {
  function revealActive() {
    var list = document.querySelector('.nav-links');
    var active = list && list.querySelector('.nav-link.is-active');
    if (!active || list.scrollWidth <= list.clientWidth) return;
    var item = active.parentElement;
    list.scrollLeft = item.offsetLeft - list.offsetLeft - (list.clientWidth - item.offsetWidth) / 2;
  }
  document.addEventListener('DOMContentLoaded', revealActive);
  document.addEventListener('htmx:afterSettle', revealActive);
})();

// "Darstellung": follow the system, or pin light or dark in this browser (localStorage, no account).
// The head script applies the stored choice before the first paint; this wires up the menu.
(function () {
  function stored() { try { return localStorage.getItem('rr-theme') || 'system'; } catch (e) { return 'system'; } }
  function apply(choice) {
    if (choice === 'light' || choice === 'dark') document.documentElement.dataset.theme = choice;
    else delete document.documentElement.dataset.theme;
  }
  function setUp() {
    var menu = document.querySelector('.theme-menu');
    if (!menu || menu.dataset.ready) return;
    menu.dataset.ready = '1';
    var buttons = menu.querySelectorAll('[data-theme-choice]');
    function mark() {
      var current = stored();
      buttons.forEach(function (b) { b.setAttribute('aria-pressed', String(b.dataset.themeChoice === current)); });
    }
    buttons.forEach(function (b) {
      b.addEventListener('click', function () {
        var choice = b.dataset.themeChoice;
        try { if (choice === 'system') localStorage.removeItem('rr-theme'); else localStorage.setItem('rr-theme', choice); } catch (e) {}
        apply(choice);
        mark();
        menu.open = false;
        menu.querySelector('summary').focus();
      });
    });
    menu.addEventListener('keydown', function (e) { if (e.key === 'Escape' && menu.open) { menu.open = false; menu.querySelector('summary').focus(); } });
    mark();
  }
  document.addEventListener('DOMContentLoaded', setUp);
  document.addEventListener('htmx:afterSettle', setUp);
})();

// Story r057: while a match on the page runs, the tab title leads with it — "1:0 Bremen – Hoffenheim
// · läuft – Rasen-Radar" — so a background tab still shows the score. It follows the live poll, which
// swaps the score cells; once nothing runs the page's own title comes back. No alerts, no permissions.
(function () {
  var ours = null, base = document.title;
  function update() {
    if (document.title !== ours) base = document.title; // hx-boost brought a new page title
    var cell = document.querySelector('.score.is-live[data-teams]');
    if (!cell) {
      if (ours) { document.title = base; ours = null; }
      return;
    }
    var score = cell.classList.contains('has-score') ? cell.textContent.trim() + ' ' : '';
    ours = score + cell.dataset.teams + ' · läuft – Rasen-Radar';
    if (document.title !== ours) document.title = ours;
  }
  window.rrLiveTitle = update; // match.js calls it when its countdown turns into "Läuft"
  document.addEventListener('DOMContentLoaded', update);
  document.addEventListener('htmx:afterSettle', update);
  document.addEventListener('htmx:oobAfterSwap', update); // the live poll swaps out of band only
})();

// Story r028: "Bogen drucken" opens the browser's print dialog; the print stylesheet in app.css
// turns the matchday page into a plain A4 sheet (Ctrl+P gives the same sheet).
document.addEventListener('click', function (e) {
  if (e.target.closest && e.target.closest('[data-print]')) window.print();
});
