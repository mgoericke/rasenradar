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

// Story r003: "Mein Verein" — one club of the two Bundesligas, stored in this browser only
// (no account, nothing on the server). The head script in base.html highlights its rows before
// the first paint; this wires up the choice buttons and the link in the navigation.
(function () {
  var KEY = 'rr-club';
  function read() {
    try {
      var c = JSON.parse(localStorage.getItem(KEY) || 'null');
      return c && /^\d+$/.test(String(c.id)) ? c : null;
    } catch (e) { return null; }
  }
  function write(c) {
    try { if (c) localStorage.setItem(KEY, JSON.stringify(c)); else localStorage.removeItem(KEY); } catch (e) {}
  }
  function highlight(c) {
    var style = document.getElementById('rr-club-style');
    if (!c) { if (style) style.remove(); return; }
    if (!style) { style = document.createElement('style'); style.id = 'rr-club-style'; document.head.appendChild(style); }
    style.textContent = window.rrClubCss(c.id);
  }
  function render() {
    var c = read();
    // the server sends a club that plays in neither Bundesliga any more back to the choice
    var gone = new URLSearchParams(location.search).get('nicht-gefunden');
    if (gone && c && String(c.id) === gone) {
      write(null); highlight(null); c = null;
      var note = document.querySelector('[data-my-club-gone]');
      if (note) note.hidden = false;
    }
    var link = document.querySelector('[data-my-club-link]');
    if (link) {
      link.href = c ? '/mein-verein/' + c.id : '/mein-verein';
      link.textContent = c ? c.short : 'Mein Verein';
      link.title = c ? 'Mein Verein: ' + c.name : 'Mein Verein wählen';
    }
    document.querySelectorAll('[data-club-choose]').forEach(function (b) {
      var mine = !!c && String(c.id) === b.dataset.clubId;
      if (b.classList.contains('my-club-option')) b.setAttribute('aria-pressed', mine ? 'true' : 'false');
      else b.hidden = mine;
    });
    document.querySelectorAll('[data-club-clear]').forEach(function (b) {
      b.hidden = !c || (!!b.dataset.clubId && String(c.id) !== b.dataset.clubId);
    });
    var current = document.querySelector('[data-my-club-current]');
    if (current) {
      current.hidden = !c;
      var a = current.querySelector('[data-my-club-current-link]');
      if (c && a) { a.textContent = c.name; a.href = '/mein-verein/' + c.id; }
    }
  }
  // story r092: the bar with the club's next match, refreshed every minute while the tab is visible
  function loadNextMatch(c) {
    var slot = document.getElementById('next-match');
    if (!slot) return;
    function mark() { document.documentElement.classList.toggle('has-next-match', slot.children.length > 0); }
    if (!c || !window.htmx) { slot.innerHTML = ''; mark(); return; }
    htmx.ajax('GET', '/mein-verein/' + c.id + '/naechstes-spiel', { target: slot, swap: 'innerHTML' }).then(mark, mark);
  }
  setInterval(function () { if (document.visibilityState === 'visible') loadNextMatch(read()); }, 60000);
  document.addEventListener('visibilitychange', function () { if (document.visibilityState === 'visible') loadNextMatch(read()); });
  document.addEventListener('DOMContentLoaded', function () { loadNextMatch(read()); });
  document.addEventListener('click', function (e) {
    if (!e.target.closest) return;
    var choose = e.target.closest('[data-club-choose]');
    var clear = e.target.closest('[data-club-clear]');
    if (!choose && !clear) return;
    var c = choose ? { id: choose.dataset.clubId, name: choose.dataset.clubName, short: choose.dataset.clubShort } : null;
    write(c); highlight(c); render(); loadNextMatch(c);
  });
  document.addEventListener('DOMContentLoaded', render);
  document.addEventListener('htmx:afterSettle', render);
})();

// Story r096: star single open matches on a matchday page; starred ones move up into the block
// "Beobachtet" until the matchday is over. The ids live in this browser only (at most 30, the
// oldest dropped first). The star only pins — no notifications, no permission prompts.
(function () {
  var KEY = 'rr-stars', MAX = 30;
  function read() {
    try {
      var a = JSON.parse(localStorage.getItem(KEY) || '[]');
      return Array.isArray(a) ? a.map(String).filter(function (x) { return /^\d+$/.test(x); }) : [];
    } catch (e) { return []; }
  }
  function write(a) { try { localStorage.setItem(KEY, JSON.stringify(a.slice(-MAX))); } catch (e) {} }
  function apply() {
    var root = document.querySelector('[data-watch-root]');
    var block = document.querySelector('[data-watch-block]');
    if (!root || !block) return;
    var list = block.querySelector('ol');
    var stars = read();
    var over = root.dataset.watchOver === 'true';
    document.querySelectorAll('li[data-watch]').forEach(function (li) {
      var id = li.dataset.watch;
      var on = !over && stars.indexOf(id) >= 0;
      var star = li.querySelector('[data-star]');
      if (star) {
        star.hidden = false;
        star.setAttribute('aria-pressed', on ? 'true' : 'false');
        star.title = on ? 'Nicht mehr beobachten' : 'Beobachten';
      }
      var pinned = list.contains(li);
      if (on && !pinned) {
        // a hidden placeholder keeps the place in the day group for the way back
        var slot = document.createElement('li');
        slot.hidden = true;
        slot.dataset.watchSlot = id;
        li.before(slot);
        list.append(li);
      } else if (!on && pinned) {
        var back = root.querySelector('[data-watch-slot="' + id + '"]');
        if (back) back.replaceWith(li);
      }
    });
    // pinned matches keep the order of the day groups, i.e. by kickoff
    Array.prototype.slice.call(list.children).sort(function (a, b) {
      var sa = root.querySelector('[data-watch-slot="' + a.dataset.watch + '"]');
      var sb = root.querySelector('[data-watch-slot="' + b.dataset.watch + '"]');
      return sa && sb && (sa.compareDocumentPosition(sb) & Node.DOCUMENT_POSITION_FOLLOWING) ? -1 : 1;
    }).forEach(function (li) { list.append(li); });
    block.hidden = list.children.length === 0;
  }
  document.addEventListener('click', function (e) {
    var star = e.target.closest && e.target.closest('[data-star]');
    if (!star) return;
    var id = star.dataset.star;
    var stars = read().filter(function (x) { return x !== id; });
    if (star.getAttribute('aria-pressed') !== 'true') stars.push(id);
    write(stars);
    apply();
    star.focus();
  });
  document.addEventListener('DOMContentLoaded', apply);
  document.addEventListener('htmx:afterSettle', apply);
})();
