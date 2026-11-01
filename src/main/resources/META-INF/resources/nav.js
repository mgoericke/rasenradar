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

// Story r085: the section row of the season page. It sticks below the top navigation, whose height
// changes with "Mein Verein" and on phones, so that height is measured into --topnav-h. The section
// whose head has passed under the row is marked as current, and the row scrolls sideways to keep its
// link in view. The links themselves are plain anchors and work without this.
(function () {
  var pending = false;
  function measure() {
    var nav = document.querySelector('.topnav');
    document.documentElement.style.setProperty('--topnav-h', (nav ? nav.offsetHeight : 0) + 'px');
  }
  function update() {
    pending = false;
    var toc = document.querySelector('.season-toc');
    if (!toc) return;
    var links = toc.querySelectorAll('a[href^="#"]');
    // a jump leaves the heading a little below the row (scroll-margin-top), so allow some room
    var edge = toc.getBoundingClientRect().bottom + 48;
    var atEnd = window.innerHeight + window.scrollY >= document.documentElement.scrollHeight - 2;
    var current = null;
    links.forEach(function (a) {
      var section = document.getElementById(a.hash.slice(1));
      if (section && (atEnd || section.getBoundingClientRect().top <= edge)) current = a;
    });
    links.forEach(function (a) {
      if (a === current) a.setAttribute('aria-current', 'location'); else a.removeAttribute('aria-current');
    });
    if (!current || current.dataset.shown) return;
    links.forEach(function (a) { delete a.dataset.shown; });
    current.dataset.shown = '1';
    var list = current.closest('ol');
    var item = current.parentElement;
    list.scrollLeft = item.offsetLeft - list.offsetLeft - (list.clientWidth - item.offsetWidth) / 2;
  }
  function schedule() {
    if (pending) return;
    pending = true;
    requestAnimationFrame(update);
  }
  function setUp() { measure(); schedule(); }
  document.addEventListener('DOMContentLoaded', setUp);
  document.addEventListener('htmx:afterSettle', setUp);
  window.addEventListener('resize', setUp);
  window.addEventListener('scroll', schedule, { passive: true });
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

// Story r073: "Nach Tag" or "Spannendste zuerst" on the matchday page, kept in this browser only
// (rr-sort). The head script sets the class before the first paint; the order itself is CSS.
(function () {
  function mark() {
    var byTension = document.documentElement.classList.contains('rr-by-tension');
    document.querySelectorAll('.sort-choice').forEach(function (p) { p.hidden = false; });
    document.querySelectorAll('[data-sort-choice]').forEach(function (b) {
      b.setAttribute('aria-pressed', String((b.dataset.sortChoice === 'spannung') === byTension));
    });
  }
  document.addEventListener('click', function (e) {
    var b = e.target.closest && e.target.closest('[data-sort-choice]');
    if (!b) return;
    var byTension = b.dataset.sortChoice === 'spannung';
    document.documentElement.classList.toggle('rr-by-tension', byTension);
    try { if (byTension) localStorage.setItem('rr-sort', 'spannung'); else localStorage.removeItem('rr-sort'); } catch (err) {}
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
    // the kept top match goes back as plain markup; htmx.process wires its KI line and arrows again
    function restore() { slot.innerHTML = slot.rrTop; htmx.process(slot); }
    if (!c) { restore(); return; }
    htmx.ajax('GET', '/mein-verein/' + c.id + '/topspiel', { target: slot, swap: 'innerHTML' })
      .then(function () { if (!slot.children.length) restore(); });
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

// design r102: the search in the navigation. "/" jumps into the field from anywhere but another
// field; the arrow keys walk from the field through the hits and back, Escape empties and leaves it.
(function () {
  function typing(el) { return el && (/^(INPUT|TEXTAREA|SELECT)$/.test(el.tagName) || el.isContentEditable); }
  document.addEventListener('keydown', function (e) {
    var field = document.getElementById('site-search-q');
    if (!field) return;
    if (e.key === '/' && !e.metaKey && !e.ctrlKey && !e.altKey && !typing(document.activeElement)) {
      e.preventDefault(); field.focus(); field.select(); return;
    }
    var form = field.form;
    if (!form.contains(document.activeElement)) return;
    var links = Array.prototype.slice.call(form.querySelectorAll('.search-hits a'));
    var at = links.indexOf(document.activeElement);
    if (e.key === 'ArrowDown' && links.length) {
      e.preventDefault(); links[Math.min(at + 1, links.length - 1)].focus();
    } else if (e.key === 'ArrowUp' && at >= 0) {
      e.preventDefault(); if (at === 0) field.focus(); else links[at - 1].focus();
    } else if (e.key === 'Escape') {
      field.value = ''; document.getElementById('site-search-hits').innerHTML = ''; field.blur();
    }
  });
})();

// Story r019: the Saisonrennen. Hovering, tapping or focusing a club draws a copy of its line on a
// layer above all others, so it is never hidden behind another club; the lines themselves keep
// their order, so the tab order stays the table's. "Mein Verein" is the line shown when none is.
(function () {
  function myClub() {
    try { var c = JSON.parse(localStorage.getItem('rr-club') || 'null'); return c ? String(c.id) : null; } catch (e) { return null; }
  }
  function init() {
    document.querySelectorAll('[data-race]:not([data-race-ready])').forEach(function (svg) {
      svg.setAttribute('data-race-ready', '');
      var top = document.createElementNS('http://www.w3.org/2000/svg', 'g');
      top.setAttribute('class', 'race-top');
      top.setAttribute('aria-hidden', 'true');
      svg.appendChild(top);
      var mine = svg.querySelector('.race-line[data-team="' + myClub() + '"]');
      function show(line) {
        top.textContent = '';
        line = line || mine;
        if (line) top.appendChild(line.cloneNode(true)).removeAttribute('tabindex');
      }
      svg.querySelectorAll('.race-line').forEach(function (line) {
        line.addEventListener('mouseenter', function () { show(line); });
        line.addEventListener('focus', function () { show(line); });
        line.addEventListener('click', function () { show(line); });
        line.addEventListener('mouseleave', function () { if (document.activeElement !== line) show(null); });
        line.addEventListener('blur', function () { show(null); });
      });
      show(null);
      // on a narrow screen the box scrolls: open it at today's end, where the names stand
      svg.parentNode.scrollLeft = svg.parentNode.scrollWidth;
    });
  }
  document.addEventListener('DOMContentLoaded', init);
  document.addEventListener('htmx:afterSettle', init);
})();

// Stories r038 "Sofa-Modus" and r052 "Konferenz nachspielen": one board, the dark stage in full
// size. The Sofa-Modus reads the matchday list of this page — the live poll keeps its scores fresh,
// the board copies them after every poll and marks a changed tile with a static "Neu". The
// Konferenz plays the last complete matchday back from /{league}/konferenz: 0' to 90'+ in about a
// minute, every goal at its minute, the real results at the end. Reduced motion: no running clock,
// one goal per "Nächstes Tor". No sound, nothing flashes.
(function () {
  var MINUTE_MS = 620;
  var HALF_TIME_MS = 1800;
  var board, opener, mode, lock, replay;

  function $(sel) { return board.querySelector(sel); }
  function plural(n, one, many) { return n + ' ' + (n === 1 ? one : many); }
  function text(el) { return el ? el.textContent.trim() : ''; }
  function goalsOf(score) { var m = /(\d+):(\d+)/.exec(score || ''); return m ? +m[1] + +m[2] : 0; }

  function tile(t) {
    var li = document.createElement('li');
    li.className = 'board-tile';
    li.innerHTML = '<a><span class="bt-status"></span><span class="bt-team bt-home"></span>' +
        '<span class="bt-score"></span><span class="bt-team bt-away"></span><span class="bt-line"></span>' +
        '<span class="bt-new" hidden>Neu</span></a>';
    var a = li.querySelector('a');
    a.href = t.link;
    [['.bt-home', t.homeIcon, t.homeShort], ['.bt-away', t.awayIcon, t.awayShort]].forEach(function (side) {
      var box = li.querySelector(side[0]);
      if (side[1]) { var img = document.createElement('img'); img.src = side[1]; img.alt = ''; img.className = 'crest'; box.appendChild(img); }
      var name = document.createElement('span'); name.className = 'bt-name'; name.textContent = side[2]; box.appendChild(name);
    });
    return li;
  }
  function set(li, status, score, line, state) {
    li.querySelector('.bt-status').textContent = status;
    li.querySelector('.bt-score').textContent = score;
    var l = li.querySelector('.bt-line');
    l.textContent = line ? line.text : '';
    l.className = 'bt-line' + (line && line.ki ? ' bt-ki' : '');
    li.classList.remove('is-live', 'is-played', 'is-open');
    li.classList.add(state);
  }
  function mark(li) {
    board.querySelectorAll('.board-tile.is-changed').forEach(function (o) { o.classList.remove('is-changed'); o.querySelector('.bt-new').hidden = true; });
    li.classList.add('is-changed');
    li.querySelector('.bt-new').hidden = false;
  }

  // --- Sofa-Modus: the page's own list is the source ---
  function sofaRows() {
    var seen = {};
    return Array.prototype.slice.call(document.querySelectorAll('li[data-watch]')).filter(function (li) {
      var id = li.getAttribute('data-watch');
      if (seen[id] || !li.querySelector('a.fixture')) return false;
      return (seen[id] = true);
    });
  }
  function sofaBuild() {
    var list = $('[data-board-tiles]');
    list.textContent = '';
    sofaRows().forEach(function (row) {
      var a = row.querySelector('a.fixture');
      var crest = function (side) { var img = a.querySelector(side + ' img'); return img ? img.getAttribute('src') : null; };
      var li = tile({ link: a.getAttribute('href'), homeShort: text(a.querySelector('.home .team-name.short')) || text(a.querySelector('.home .team-name')),
        awayShort: text(a.querySelector('.away .team-name.short')) || text(a.querySelector('.away .team-name')),
        homeIcon: crest('.home'), awayIcon: crest('.away') });
      li.setAttribute('data-board-match', row.getAttribute('data-watch'));
      li.setAttribute('data-time', text(a.querySelector('.time')));
      list.appendChild(li);
    });
    sofaSync(false);
  }
  function sofaSync(markChanges) {
    var running = 0, goals = 0;
    var rank = { 'is-live': 0, 'is-played': 1, 'is-open': 2 };
    board.querySelectorAll('[data-board-match]').forEach(function (li) {
      var id = li.getAttribute('data-board-match');
      var score = document.querySelector('.score[data-match="' + id + '"]');
      if (!score) return;
      var live = score.classList.contains('is-live'), played = score.classList.contains('is-played');
      var shown = played || (live && score.classList.contains('has-score')) ? text(score) : live ? '0:0' : '–:–';
      var state = live ? 'is-live' : played ? 'is-played' : 'is-open';
      var status = live ? 'Läuft' : played ? (score.classList.contains('is-provisional') ? 'Ende · vorläufig' : 'Ende') : li.getAttribute('data-time');
      var marker = document.getElementById('forecast-marker-' + id);
      // "KI-Richtwert 2:1: Tendenz getroffen" (settled) or "KI-Richtwert 2:1: Tendenz zum …" (not yet)
      var f = played && marker ? /KI-Richtwert (\d+:\d+)(?:: Tendenz (getroffen|daneben))?/.exec(marker.getAttribute('title') || '') : null;
      var ki = f ? { text: 'KI-Vorschau ' + f[1] + (f[2] ? ' · Tendenz ' + f[2] : ''), ki: true } : null;
      var before = li.querySelector('.bt-score').textContent;
      set(li, status, shown, ki, state);
      if (markChanges && before && before !== shown) mark(li);
      li.style.order = rank[state];
      if (live) running++;
      if (live || played) goals += goalsOf(shown);
    });
    $('[data-board-summary]').textContent = (running ? plural(running, 'Spiel läuft', 'Spiele laufen') : 'Keine Spiele laufen') +
        ', ' + plural(goals, 'Tor', 'Tore') + ' bisher';
  }

  // --- Konferenz: a fetched matchday played back ---
  function replayStart(data) {
    var list = $('[data-board-tiles]');
    list.textContent = '';
    var events = [];
    data.matches.forEach(function (m, i) {
      var li = tile(m);
      li.setAttribute('data-replay', i);
      list.appendChild(li);
      m.goals.forEach(function (g) { events.push({ match: i, goal: g }); });
    });
    events.sort(function (a, b) { return a.goal.minute - b.goal.minute; });
    var end = Math.max(90, events.length ? events[events.length - 1].goal.minute : 90);
    replay = { data: data, events: events, next: 0, minute: 0, end: end, timer: null, paused: false,
               stepwise: window.matchMedia && window.matchMedia('(prefers-reduced-motion: reduce)').matches };
    data.matches.forEach(function (m, i) { set(tileAt(i), "0'", '0:0', null, 'is-live'); });
    $('[data-board-ticker]').hidden = false;
    $('[data-board-ticker]').textContent = replay.stepwise ? 'Anpfiff. Weiter mit „Nächstes Tor“.' : 'Anpfiff.';
    $('[data-board-controls]').hidden = false;
    $('[data-replay-step]').hidden = !replay.stepwise;
    $('[data-replay-pause]').hidden = replay.stepwise;
    $('[data-replay-pause]').textContent = 'Pause';
    summary();
    if (!replay.stepwise) tick();
  }
  function tileAt(i) { return board.querySelector('[data-replay="' + i + '"]'); }
  function summary() {
    var goals = replay.events.slice(0, replay.next).length;
    $('[data-board-summary]').textContent = (replay.minute >= replay.end ? 'Abpfiff' : replay.minute + '. Minute') + ', ' + plural(goals, 'Tor', 'Tore');
  }
  function score(e) {
    var m = replay.data.matches[e.match], g = e.goal, li = tileAt(e.match);
    var who = g.scorer ? g.scorer : 'Tor';
    var line = g.minute + '. ' + who + (g.note ? ' (' + g.note + ')' : '');
    set(li, g.minute + "'", g.score, { text: line }, 'is-live');
    mark(li);
    $('[data-board-ticker]').textContent = 'Tor für ' + (g.forHome ? m.homeShort : m.awayShort) + '! ' + m.homeShort + ' ' + g.score +
        ' ' + m.awayShort + (g.scorer ? ' – ' + g.scorer + ' (' + g.minute + '.)' : '');
  }
  function advanceTo(minute) {
    replay.minute = minute;
    while (replay.next < replay.events.length && replay.events[replay.next].goal.minute <= minute) score(replay.events[replay.next++]);
    replay.data.matches.forEach(function (m, i) { tileAt(i).querySelector('.bt-status').textContent = minute + "'"; });
    summary();
    if (minute >= replay.end) finish();
  }
  function finish() {
    clearTimeout(replay.timer);
    replay.minute = replay.end;
    replay.data.matches.forEach(function (m, i) {
      var li = tileAt(i);
      set(li, 'Ende', m.result, { text: li.querySelector('.bt-line').textContent }, 'is-played');
    });
    $('[data-board-ticker]').textContent = 'Abpfiff. ' + plural(replay.data.goals, 'Tor', 'Tore') + ' an diesem Spieltag.';
    $('[data-board-summary]').textContent = 'Abpfiff, ' + plural(replay.data.goals, 'Tor', 'Tore');
    $('[data-replay-pause]').hidden = true;
    $('[data-replay-step]').hidden = true;
  }
  function tick() {
    if (replay.paused || replay.minute >= replay.end) return;
    replay.timer = setTimeout(function () {
      advanceTo(replay.minute + 1);
      if (replay.minute === 45) {
        $('[data-board-summary]').textContent = 'Halbzeit, ' + plural(replay.next, 'Tor', 'Tore');
        replay.timer = setTimeout(tick, HALF_TIME_MS);
      } else {
        tick();
      }
    }, MINUTE_MS);
  }
  function stop() { if (replay) { clearTimeout(replay.timer); replay = null; } }

  // --- the board itself ---
  function wake() {
    if (!('wakeLock' in navigator)) {
      var note = $('[data-board-note]');
      note.textContent = 'Dieses Gerät lässt den Bildschirm nicht wach halten – er kann sich wie gewohnt abschalten.';
      note.hidden = false;
      return;
    }
    navigator.wakeLock.request('screen').then(function (l) { lock = l; }).catch(function () {});
  }
  function open(kind, button) {
    board = document.querySelector('[data-board]');
    if (!board) return;
    opener = button; mode = kind;
    $('[data-board-note]').hidden = true;
    $('[data-board-controls]').hidden = true;
    $('[data-board-ticker]').hidden = true;
    board.hidden = false;
    document.documentElement.classList.add('board-open');
    if (board.requestFullscreen) board.requestFullscreen().catch(function () {});
    wake();
    $('[data-board-close]').focus();
    if (kind === 'sofa') {
      $('[data-board-title]').textContent = text(document.querySelector('.matchday-title')) + ' · ' + text(document.querySelector('.matchday-season'));
      sofaBuild();
    } else {
      $('[data-board-title]').textContent = 'Konferenz';
      $('[data-board-summary]').textContent = 'Wird geladen …';
      $('[data-board-tiles]').textContent = '';
      fetch(button.getAttribute('data-replay-open'), { headers: { Accept: 'application/json' } })
          .then(function (r) { if (!r.ok) throw r; return r.json(); })
          .then(function (data) { if (mode !== 'replay' || board.hidden) return; $('[data-board-title]').textContent = 'Konferenz · ' + data.title; replayStart(data); })
          .catch(function () { $('[data-board-summary]').textContent = 'Noch kein Spieltag komplett beendet.'; });
    }
  }
  function close() {
    if (!board || board.hidden) return;
    stop();
    board.hidden = true;
    document.documentElement.classList.remove('board-open');
    if (document.fullscreenElement && document.exitFullscreen) document.exitFullscreen().catch(function () {});
    if (lock) { lock.release().catch(function () {}); lock = null; }
    if (opener) opener.focus();
    mode = null;
  }

  document.addEventListener('click', function (e) {
    var sofa = e.target.closest('[data-sofa-open]');
    if (sofa) { open('sofa', sofa); return; }
    var conf = e.target.closest('[data-replay-open]');
    if (conf) { open('replay', conf); return; }
    if (!board || board.hidden) return;
    if (e.target.closest('[data-board-close]')) { close(); return; }
    if (e.target.closest('[data-replay-again]') && replay) { var d = replay.data; stop(); replayStart(d); return; }
    if (e.target.closest('[data-replay-step]') && replay) {
      if (replay.next < replay.events.length) advanceTo(replay.events[replay.next].goal.minute); else advanceTo(replay.end);
      return;
    }
    var pause = e.target.closest('[data-replay-pause]');
    if (pause && replay) {
      replay.paused = !replay.paused;
      pause.textContent = replay.paused ? 'Weiter' : 'Pause';
      if (replay.paused) clearTimeout(replay.timer); else tick();
      return;
    }
    // a tile leads to its match page: leave the board first, so the page underneath is as it was
    if (e.target.closest('.board-tile a')) close();
  });
  document.addEventListener('keydown', function (e) {
    if (!board || board.hidden) return;
    if (e.key === 'Escape') { e.preventDefault(); close(); return; }
    if (e.key !== 'Tab') return;
    var items = Array.prototype.slice.call(board.querySelectorAll('button:not([hidden]), a[href]'))
        .filter(function (el) { return !el.closest('[hidden]'); });
    if (!items.length) return;
    var first = items[0], last = items[items.length - 1];
    if (e.shiftKey && document.activeElement === first) { e.preventDefault(); last.focus(); }
    else if (!e.shiftKey && document.activeElement === last) { e.preventDefault(); first.focus(); }
  });
  document.addEventListener('fullscreenchange', function () {
    if (!document.fullscreenElement && board && !board.hidden) close();
  });
  document.addEventListener('visibilitychange', function () {
    // the browser drops a wake lock while the tab is hidden; take it again on return
    if (board && !board.hidden && document.visibilityState === 'visible') wake();
  });
  // the live poll swaps the page's score cells; the Sofa-Modus copies what changed
  document.addEventListener('htmx:afterSettle', function () { if (board && !board.hidden && mode === 'sofa') sofaSync(true); });
  document.addEventListener('htmx:oobAfterSwap', function () { if (board && !board.hidden && mode === 'sofa') sofaSync(true); });
  function init() { document.querySelectorAll('[data-sofa-open], [data-replay-open]').forEach(function (b) { b.hidden = false; }); }
  document.addEventListener('DOMContentLoaded', init);
  document.addEventListener('htmx:afterSettle', init);
  // an hx-boost navigation away swaps the page: the board goes with it
  document.addEventListener('htmx:beforeSwap', function (e) { if (e.detail && e.detail.target === document.body) close(); });
})();
