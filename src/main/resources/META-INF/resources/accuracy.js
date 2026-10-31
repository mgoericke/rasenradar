// Chart colours come from the CSS tokens (docs/styleguide.md), so the charts follow light and dark.
function rrToken(name) { return getComputedStyle(document.documentElement).getPropertyValue(name).trim(); }
function rrChartDefaults() {
  Chart.defaults.color = rrToken('--ink-soft');
  Chart.defaults.borderColor = rrToken('--line');
  Chart.defaults.font.family = rrToken('--text');
}

// Hit rate per matchday (accuracy page): oracle and baseline side by side, the count in the tooltip.
(function () {
  document.querySelectorAll('canvas[id^="accuracy-"]').forEach(function (el) {
    var dataEl = document.getElementById('accuracy-data-' + el.id.substring('accuracy-'.length));
    if (!dataEl) return;
    var data = JSON.parse(dataEl.textContent);
    rrChartDefaults();
    new Chart(el, {
      type: 'bar',
      data: { labels: data.labels, datasets: [
        { label: 'KI-Vorschau', data: data.rates, backgroundColor: rrToken('--ki'), borderRadius: 4 },
        { label: 'Basisprognose', data: data.baselineRates, backgroundColor: rrToken('--draw'), borderRadius: 4 }
      ] },
      options: {
        animation: false,
        maintainAspectRatio: false,
        scales: {
          y: { min: 0, max: 100, ticks: { callback: function (v) { return v + ' %'; } } },
          x: { grid: { display: false } }
        },
        plugins: {
          legend: { position: 'bottom' },
          tooltip: { callbacks: { label: function (c) { return c.dataset.label + ': ' + c.parsed.y + ' % von ' + data.counts[c.dataIndex] + ' Vorschauen'; } } }
        }
      }
    });
  });
})();
