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
  var colors = ['#14201B', '#1E9E5A'];
  new Chart(el, {
    type: 'line',
    data: {
      labels: data.labels,
      datasets: data.teams.map(function (t, i) {
        return { label: t.name, data: t.positions, borderColor: colors[i], backgroundColor: colors[i],
                 borderWidth: 2, pointRadius: 3, tension: 0.2, spanGaps: true };
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
  new Chart(el, {
    type: 'bar',
    data: {
      labels: data.labels,
      datasets: [
        { label: 'Erzielt', data: data.scored, backgroundColor: '#1A8A50' },
        { label: 'Kassiert', data: data.conceded, backgroundColor: '#C63C3C' }
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
