// Spec 01, "Spieltags-Strom der Startseite": filter the stream by status (live, finished, upcoming)
// and by league. Empty kickoff blocks and days disappear with their matches. Without this script
// the filter bar stays hidden and every match shows — nothing depends on it.
// The landing page arrives by hx-boost as often as by a full load, so this runs on every swap
// but binds its click handler only once.
(function () {
  if (window.rasenRadarStream) {
    window.rasenRadarStream();
    return;
  }

  var state = { status: 'alle', hidden: {} };

  function visible(row) {
    for (var league in state.hidden) {
      if (state.hidden[league] && row.classList.contains('lg-' + league)) return false;
    }
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
    root.querySelectorAll('[data-status]').forEach(function (b) { b.setAttribute('aria-pressed', String(b.dataset.status === state.status)); });
    root.querySelectorAll('[data-league]').forEach(function (b) { b.setAttribute('aria-pressed', String(!state.hidden[b.dataset.league])); });
  }

  function init() {
    var root = document.querySelector('.landing');
    if (!root) return;
    var bar = root.querySelector('.stream-filters');
    if (bar) bar.hidden = false;
    state = { status: 'alle', hidden: {} };
    apply(root);
  }

  document.addEventListener('click', function (e) {
    var root = document.querySelector('.landing');
    var button = e.target.closest && e.target.closest('[data-status], [data-league], [data-reset]');
    if (!root || !button || !root.contains(button)) return;
    if (button.dataset.status) {
      state.status = button.dataset.status;
    } else if (button.dataset.league) {
      var league = button.dataset.league;
      var shown = root.querySelectorAll('[data-league]').length - Object.keys(state.hidden).filter(function (k) { return state.hidden[k]; }).length;
      if (!state.hidden[league] && shown <= 1) return; // never filter out every league
      state.hidden[league] = !state.hidden[league];
    } else {
      state = { status: 'alle', hidden: {} };
    }
    apply(root);
  });

  document.addEventListener('rr:watch', function () {
    var root = document.querySelector('.landing');
    if (root) apply(root);
  });

  window.rasenRadarStream = init;
  init();
})();
