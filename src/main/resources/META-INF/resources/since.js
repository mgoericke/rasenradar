// Story r044: "Seit dem letzten Besuch" on the start page. This browser remembers the time of the
// last visit and the table of final results at that time (rr-visit); a visit more than two hours
// later shows the matches played since and how the club of "Mein Verein" moved — or, without one,
// the biggest move in the table. The base of the comparison stays for this tab (rr-since) until the
// card is closed, so browsing away and back keeps it. Runs right after the card, before the first
// paint, so nothing below the card jumps. Nothing of it is ever sent to the server.
// The card is off until switched on in "Seite anpassen" (rr-shown); the visit is remembered anyway,
// so switching it on shows what has been played since right away.
(function () {
  var VISIT = 'rr-visit', SINCE = 'rr-since', GAP = 2 * 60 * 60 * 1000, SHOWN = 6;
  var card = document.querySelector('[data-since]');
  if (!card) return;

  function get(store, key) {
    try { var v = JSON.parse(store.getItem(key) || 'null'); return v && typeof v.t === 'number' ? v : null; } catch (e) { return null; }
  }
  function set(store, key, v) {
    try { if (v) store.setItem(key, JSON.stringify(v)); else store.removeItem(key); } catch (e) {}
  }
  function table() {
    var places = {};
    card.dataset.places.split(';').forEach(function (entry) {
      var p = entry.split(':');
      if (p.length === 3) places[p[0]] = { place: +p[1], name: p[2] };
    });
    return places;
  }
  function club() {
    try { var c = JSON.parse(localStorage.getItem('rr-club') || 'null'); return c && /^\d+$/.test(String(c.id)) ? String(c.id) : null; } catch (e) { return null; }
  }
  function move(then, now) {
    var id = club();
    if (id) {
      // the club's own move, or nothing when it stayed put or plays elsewhere
      if (!now[id] || !then[id] || now[id].place === then[id].place) return '';
      return now[id].name + ': von Platz ' + then[id].place + ' auf Platz ' + now[id].place;
    }
    var best = null, gap = 0;
    Object.keys(now).forEach(function (k) {
      if (!then[k]) return;
      var d = Math.abs(then[k].place - now[k].place);
      if (d > gap) { gap = d; best = k; }
    });
    return best ? 'Größter Sprung: ' + now[best].name + ' von Platz ' + then[best].place + ' auf Platz ' + now[best].place : '';
  }

  var now = Date.now(), places = table();
  var base = get(sessionStorage, SINCE);
  if (!base) {
    var last = get(localStorage, VISIT);
    if (last && now - last.t > GAP) { base = last; set(sessionStorage, SINCE, base); }
  }
  set(localStorage, VISIT, { t: now, places: places });
  window.rrSince = function () { show(card, base, places); };
  if (!window.rrSinceBound) {
    window.rrSinceBound = true;
    document.addEventListener('click', function (e) {
      var close = e.target.closest && e.target.closest('[data-since-close]');
      if (!close) return;
      set(sessionStorage, SINCE, null);
      var c = document.querySelector('[data-since]');
      if (c) c.hidden = true;
    });
    document.addEventListener('rr:sections', function () { if (document.querySelector('[data-since]')) window.rrSince(); });
  }
  window.rrSince();

  function show(card, base, places) {
    var on = false;
    try { on = (JSON.parse(localStorage.getItem('rr-shown') || '[]') || []).indexOf('start-seit-besuch') >= 0; } catch (e) {}
    card.hidden = true;
    if (!on || !base) return;

    var rows = card.querySelectorAll('li[data-end]'), shown = 0, more = 0;
    rows.forEach(function (li) {
      var since = +li.dataset.end * 1000 > base.t;
      if (since && shown < SHOWN) { li.hidden = false; shown++; } else if (since) more++;
    });
    var text = move(base.places || {}, places);
    if (!shown && !text) return;
    var line = card.querySelector('.since-move');
    line.textContent = text;
    line.hidden = !text;
    var rest = card.querySelector('.since-more');
    rest.textContent = more ? 'und ' + more + (more === 1 ? ' weiteres Spiel' : ' weitere Spiele') : '';
    rest.hidden = !more;
    card.querySelector('.since-results').hidden = !shown;
    card.hidden = false;
  }
})();
