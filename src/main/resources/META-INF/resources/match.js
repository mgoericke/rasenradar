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
