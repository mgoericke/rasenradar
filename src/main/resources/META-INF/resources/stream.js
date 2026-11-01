// Spec 01, "Spieltags-Strom der Startseite": filter the stream by status (live, finished, upcoming)
// and by league. Empty kickoff blocks, days and stream parts ("Spieltag", "Nächster Spieltag")
// disappear with their matches. Design r102: one league or both ("Alle / 1. Liga / 2. Liga"), the
// status inside "Mehr zum Spieltag". Without this script switch and status stay hidden and every
// match shows — nothing depends on it. The stream opens on the league of "Mein Verein", without a
// club on the 1. Bundesliga: "Alle" mixes in finished cup ties and other leagues' matches.
// The landing page arrives by hx-boost as often as by a full load, so this runs on every swap
// but binds its click handler only once.
(function () {
  if (window.rasenRadarStream) {
    window.rasenRadarStream();
    return;
  }

  var state = { status: 'alle', league: 'alle' };

  function visible(row) {
    if (state.league !== 'alle' && !row.classList.contains('lg-' + state.league)) return false;
    if (state.status === 'live') return row.classList.contains('is-live');
    if (state.status === 'beendet') return row.classList.contains('is-played');
    if (state.status === 'anstehend') return row.classList.contains('is-open');
    return true;
  }

  function apply(root) {
    var any = false;
    root.querySelectorAll('.stream-row').forEach(function (row) { row.hidden = !visible(row); });
    root.querySelectorAll('.stream-slot').forEach(function (slot) { slot.hidden = !slot.querySelector('.stream-row:not([hidden])'); });
    root.querySelectorAll('.stream-day').forEach(function (day) {
      day.hidden = !day.querySelector('.stream-slot:not([hidden])');
      any = any || !day.hidden;
    });
    // story r096: the block "Beobachtet" holds rows moved out of their day; it follows the filter too
    var watched = root.querySelector('[data-watch-block]');
    if (watched && watched.querySelector('.stream-row')) {
      watched.hidden = !watched.querySelector('.stream-row:not([hidden])');
      any = any || !watched.hidden;
    }
    var empty = root.querySelector('.stream-empty[hidden], .stream-empty.is-filtered');
    if (empty) {
      empty.hidden = any;
      empty.classList.toggle('is-filtered', !any);
    }
    // a part without a match left goes too — unless it carries the "nothing found" line
    root.querySelectorAll('.stream-part:not([data-watch-block])').forEach(function (part) {
      part.hidden = !part.querySelector('.stream-day:not([hidden])') && (any || !part.querySelector('.stream-empty'));
    });
    root.querySelectorAll('[data-status]').forEach(function (b) { b.setAttribute('aria-pressed', String(b.dataset.status === state.status)); });
    root.querySelectorAll('[data-league]').forEach(function (b) { b.setAttribute('aria-pressed', String(b.dataset.league === state.league)); });
  }

  // the league of "Mein Verein" from the tables beside, else the first league of the page
  function home(root) {
    var club = null;
    try { var c = JSON.parse(localStorage.getItem('rr-club') || 'null'); if (c && /^\d+$/.test(String(c.id))) club = String(c.id); } catch (e) {}
    var row = club && root.querySelector('.rail-panel tr[data-clubs~="' + club + '"]');
    var panel = row && row.closest('.rail-panel');
    var mine = panel && Array.prototype.slice.call(panel.classList).filter(function (k) { return k.indexOf('table-') === 0; })[0];
    var first = root.querySelector('[data-league]:not([data-league="alle"])');
    return mine ? mine.slice(6) : first ? first.dataset.league : 'alle';
  }

  function reset(root) {
    state = { status: 'alle', league: home(root) };
    var table = root.querySelector('#table-' + state.league);
    if (table) table.checked = true;
  }

  function init() {
    var root = document.querySelector('.landing');
    if (!root) return;
    root.querySelectorAll('.stream-leagues, .md-menu-status').forEach(function (el) { el.hidden = false; });
    reset(root);
    apply(root);
  }

  document.addEventListener('click', function (e) {
    var root = document.querySelector('.landing');
    var button = e.target.closest && e.target.closest('[data-status], [data-league], [data-reset]');
    if (!root || !button || !root.contains(button)) return;
    if (button.dataset.status) {
      state.status = button.dataset.status;
    } else if (button.dataset.league) {
      state.league = button.dataset.league;
      // the table beside follows the league picked
      var table = root.querySelector('#table-' + state.league);
      if (table) table.checked = true;
    } else {
      reset(root);
    }
    apply(root);
  });

  // "Mehr zum Spieltag" closes on a click outside and on Escape
  document.addEventListener('click', function (e) {
    document.querySelectorAll('.md-menu[open]').forEach(function (m) { if (!m.contains(e.target)) m.open = false; });
  });
  document.addEventListener('keydown', function (e) {
    if (e.key !== 'Escape') return;
    document.querySelectorAll('.md-menu[open]').forEach(function (m) { m.open = false; m.querySelector('summary').focus(); });
  });

  document.addEventListener('rr:watch', function () {
    var root = document.querySelector('.landing');
    if (root) apply(root);
  });

  window.rasenRadarStream = init;
  init();
})();
