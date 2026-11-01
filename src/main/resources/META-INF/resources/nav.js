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

// Story r086: "Liste" or "Kompakt" on the matchday page, kept in this browser only (rr-view). The head
// script sets the class before the first paint; without storage the list simply stays the default.
(function () {
  function mark() {
    var compact = document.documentElement.classList.contains('rr-compact');
    document.querySelectorAll('[data-view-choice]').forEach(function (b) {
      b.setAttribute('aria-pressed', String((b.dataset.viewChoice === 'kompakt') === compact));
    });
  }
  document.addEventListener('click', function (e) {
    var b = e.target.closest && e.target.closest('[data-view-choice]');
    if (!b) return;
    var compact = b.dataset.viewChoice === 'kompakt';
    document.documentElement.classList.toggle('rr-compact', compact);
    try { if (compact) localStorage.setItem('rr-view', 'kompakt'); else localStorage.removeItem('rr-view'); } catch (err) {}
    mark();
  });
  document.addEventListener('DOMContentLoaded', mark);
  document.addEventListener('htmx:afterSettle', mark);
})();

// Story r028: "Spieltag drucken" opens the browser's print dialog; the print stylesheet in app.css
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
    document.querySelectorAll('[data-my-club-href]').forEach(function (a) { a.href = c ? '/mein-verein/' + c.id : '/mein-verein'; });
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
  function refresh() {
    if (document.visibilityState !== 'visible') return;
    var c = read();
    loadNextMatch(c);
    if (c) loadStage(c, true);
  }
  setInterval(refresh, 60000);
  document.addEventListener('visibilitychange', refresh);
  document.addEventListener('DOMContentLoaded', function () { loadNextMatch(read()); });
  // the start page's stage: the club's own running or next match instead of the top match. The
  // server's top match is kept and comes back without a club, or when the club has no match.
  function loadStage(c, force) {
    var slot = document.getElementById('top-stage');
    if (!slot || !window.htmx) return;
    if (slot.rrTop == null) slot.rrTop = slot.innerHTML;
    var id = c ? String(c.id) : '';
    if (!force && slot.dataset.club === id) return;
    slot.dataset.club = id;
    if (!c) { slot.innerHTML = slot.rrTop; return; }
    htmx.ajax('GET', '/mein-verein/' + c.id + '/topspiel', { target: slot, swap: 'innerHTML' })
      .then(function () { if (!slot.children.length) slot.innerHTML = slot.rrTop; });
  }
  document.addEventListener('DOMContentLoaded', function () { loadStage(read()); });
  document.addEventListener('htmx:afterSettle', function (e) {
    if (e.target.closest && e.target.closest('#top-stage')) return;
    loadStage(read());
  });
  document.addEventListener('click', function (e) {
    if (!e.target.closest) return;
    var choose = e.target.closest('[data-club-choose]');
    var clear = e.target.closest('[data-club-clear]');
    if (!choose && !clear) return;
    var c = choose ? { id: choose.dataset.clubId, name: choose.dataset.clubName, short: choose.dataset.clubShort } : null;
    write(c); highlight(c); render(); loadNextMatch(c); loadStage(c, true);
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
    // the start page filter (stream.js) re-runs on this, so the block follows the filter
    document.dispatchEvent(new CustomEvent('rr:watch'));
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

// Story r078: on the current matchday, an open match of a table neighbour of "Mein Verein" (up to
// three places away) says which result helps the club — from the table alone, never from a
// forecast. One neighbour: that neighbour should lose; two neighbours: a draw. The club stays in
// the browser; the page only carries the places ("teamId:place,…").
(function () {
  function club() {
    try {
      var c = JSON.parse(localStorage.getItem('rr-club') || 'null');
      return c && /^\d+$/.test(String(c.id)) ? c : null;
    } catch (e) { return null; }
  }
  function apply() {
    document.querySelectorAll('.good-for').forEach(function (p) { p.hidden = true; p.textContent = ''; });
    var root = document.querySelector('[data-places]');
    var c = club();
    if (!root || !c) return;
    var places = {};
    root.dataset.places.split(',').forEach(function (pair) { var kv = pair.split(':'); places[kv[0]] = Number(kv[1]); });
    var id = String(c.id), mine = places[id];
    if (!mine) return;
    function neighbour(team) { return team !== id && places[team] > 0 && Math.abs(places[team] - mine) <= 3; }
    document.querySelectorAll('li[data-good-for]').forEach(function (li) {
      var teams = li.dataset.goodFor.split(' ');
      if (teams.indexOf(id) >= 0) return;
      var home = neighbour(teams[0]), away = neighbour(teams[1]);
      var good = home && away ? 'Remis' : home ? 'Sieg ' + li.dataset.awayName : away ? 'Sieg ' + li.dataset.homeName : null;
      var p = li.querySelector('.good-for');
      if (!good || !p) return;
      p.textContent = 'Gut für ' + c.short + ': ' + good;
      p.hidden = false;
    });
  }
  document.addEventListener('DOMContentLoaded', apply);
  document.addEventListener('htmx:afterSettle', apply);
})();

// Story r099: glossary terms on the KI pages. A term is a link to its explanation on the explainer
// page; here it becomes a button that opens the same sentence in a box right under the word. Tap
// again, tap elsewhere or press Escape to close — focus goes back to the term.
(function () {
  var open = null, count = 0;
  function close(focus) {
    if (!open) return;
    open.term.setAttribute('aria-expanded', 'false');
    open.box.remove();
    if (focus) open.term.focus();
    open = null;
  }
  function show(term) {
    var box = document.createElement('span');
    box.className = 'term-box';
    box.id = 'term-box-' + (++count);
    box.setAttribute('role', 'note');
    box.textContent = term.dataset.explain;
    document.body.appendChild(box);
    var r = term.getBoundingClientRect();
    var width = Math.min(320, document.documentElement.clientWidth - 32);
    box.style.width = width + 'px';
    box.style.left = Math.max(16, Math.min(r.left, document.documentElement.clientWidth - width - 16)) + window.scrollX + 'px';
    box.style.top = r.bottom + window.scrollY + 6 + 'px';
    term.setAttribute('aria-expanded', 'true');
    term.setAttribute('aria-controls', box.id);
    open = { term: term, box: box };
  }
  function init() {
    document.querySelectorAll('a.term:not([role])').forEach(function (t) {
      t.setAttribute('role', 'button');
      t.setAttribute('aria-expanded', 'false');
    });
  }
  document.addEventListener('click', function (e) {
    var term = e.target.closest && e.target.closest('a.term[role="button"]');
    if (open && open.box.contains(e.target)) return;
    if (!term) { close(false); return; }
    e.preventDefault();
    var same = open && open.term === term;
    close(false);
    if (!same) show(term);
  });
  document.addEventListener('keydown', function (e) {
    var term = e.target.closest && e.target.closest('a.term[role="button"]');
    if (term && e.key === ' ') { e.preventDefault(); term.click(); }
    if (e.key === 'Escape') close(true);
  });
  window.addEventListener('resize', function () { close(false); });
  document.addEventListener('htmx:beforeSwap', function () { close(false); });
  document.addEventListener('DOMContentLoaded', init);
  document.addEventListener('htmx:afterSettle', init);
})();
