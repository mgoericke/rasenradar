// Chart colours come from the CSS tokens (docs/styleguide.md), so the charts follow light and dark.
function rrToken(name) { return getComputedStyle(document.documentElement).getPropertyValue(name).trim(); }
function rrChartDefaults() {
  Chart.defaults.color = rrToken('--ink-soft');
  Chart.defaults.borderColor = rrToken('--line');
  Chart.defaults.font.family = rrToken('--text');
}

// Table position over the season so far: both teams on the match page, one team on the club page.
(function () {
  var el = document.getElementById('positions');
  var dataEl = document.getElementById('chart-data');
  if (!el || !dataEl) return;
  var data = JSON.parse(dataEl.textContent);
  if (!data.labels.length) {
    var p = document.createElement('p');
    p.className = 'muted';
    p.textContent = 'Noch kein Verlauf – die Saison hat gerade begonnen.';
    el.replaceWith(p);
    return;
  }
  rrChartDefaults();
  // two teams in ink and soft ink, told apart by the point shape too; green and red stay results only
  var colors = [rrToken('--ink'), rrToken('--ink-soft')];
  var shapes = ['circle', 'rect'];
  var mutedColor = rrToken('--line'); // a previous season is a faded reference line, not text
  new Chart(el, {
    type: 'line',
    data: {
      labels: data.labels,
      datasets: data.teams.map(function (t, i) {
        var color = t.dashed ? mutedColor : colors[i];
        return { label: t.name, data: t.positions, borderColor: color, backgroundColor: color,
                 borderWidth: 2, pointRadius: t.dashed ? 0 : 3, pointStyle: shapes[i] || 'circle', tension: 0.2, spanGaps: true,
                 borderDash: t.dashed ? [6, 4] : undefined };
      })
    },
    options: {
      animation: false,
      maintainAspectRatio: false,
      scales: {
        y: { reverse: true, min: 1, max: data.teamCount, ticks: { stepSize: 1, precision: 0 }, title: { display: true, text: 'Platz' } },
        x: { title: { display: true, text: 'nach Spieltag' }, grid: { display: false } }
      },
      plugins: {
        legend: { position: 'bottom' },
        tooltip: { callbacks: { label: function (c) { return c.dataset.label + ': Platz ' + c.parsed.y; } } }
      }
    }
  });
})();

// Goals scored/conceded per 15-minute window — club page only.
(function () {
  var el = document.getElementById('goal-timing');
  var dataEl = document.getElementById('goal-timing-data');
  if (!el || !dataEl) return;
  var data = JSON.parse(dataEl.textContent);
  rrChartDefaults();
  new Chart(el, {
    type: 'bar',
    data: {
      labels: data.labels,
      datasets: [
        { label: 'Erzielt', data: data.scored, backgroundColor: rrToken('--win') },
        { label: 'Kassiert', data: data.conceded, backgroundColor: rrToken('--loss') }
      ]
    },
    options: {
      animation: false,
      maintainAspectRatio: false,
      scales: {
        y: { beginAtZero: true, ticks: { stepSize: 1, precision: 0 }, title: { display: true, text: 'Tore' } },
        x: { grid: { display: false } }
      },
      plugins: { legend: { position: 'bottom' } }
    }
  });
})();

// Story r004: the match head counts down to kickoff, on the server's clock rather than a skewed
// device clock. At kickoff it says "Läuft" and starts the live poll, in case the page was opened
// too early to get one. No live region: a screen reader is not told every minute.
(function () {
  function label(ms) {
    var min = Math.max(1, Math.ceil(ms / 60000)), h = Math.floor(min / 60), m = min % 60;
    return 'Anstoß in ' + (h ? h + ' Std.' + (m ? ' ' + m + ' Min.' : '') : m + ' Min.');
  }
  function kickoff(el) {
    el.remove();
    var score = document.querySelector('.scoreboard .score');
    if (!score || score.classList.contains('is-played') || score.classList.contains('is-live')) return;
    score.classList.add('is-live');
    score.textContent = 'Läuft';
    if (window.rrLiveTitle) rrLiveTitle();
    var poller = document.getElementById('live-poller');
    if (poller && poller.getAttribute('hx-get')) return;
    var div = document.createElement('div');
    div.id = 'live-poller';
    div.setAttribute('hx-get', '/live/spielstaende?match=' + score.dataset.match + '&details=true');
    div.setAttribute('hx-trigger', "every 60s, visibilitychange[document.visibilityState === 'visible'] from:document");
    div.setAttribute('hx-swap', 'none');
    if (poller) poller.replaceWith(div); else document.body.appendChild(div);
    if (window.htmx) htmx.process(div);
  }
  function tick() {
    var el = document.querySelector('.scoreboard .countdown');
    if (!el) { clearInterval(window.rrCountdown); window.rrCountdown = null; return; }
    if (!el.dataset.offset) el.dataset.offset = el.dataset.now * 1000 - Date.now();
    var left = el.dataset.kickoff * 1000 - (Date.now() + Number(el.dataset.offset));
    if (left > 0) el.textContent = label(left); else kickoff(el);
  }
  function start() {
    if (window.rrCountdown || !document.querySelector('.scoreboard .countdown')) return;
    window.rrCountdown = setInterval(tick, 15000);
    tick();
  }
  if (!window.rrCountdownSeen) {
    window.rrCountdownSeen = true;
    document.addEventListener('visibilitychange', function () { if (window.rrCountdown && !document.hidden) tick(); });
    // a poll before the server's kickoff brings the countdown back (a device clock running ahead)
    document.addEventListener('htmx:oobAfterSwap', start);
  }
  start();
})();
