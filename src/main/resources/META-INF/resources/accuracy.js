// Hit rate per matchday (accuracy page): oracle and baseline side by side, the count in the tooltip.
(function () {
  document.querySelectorAll('canvas[id^="accuracy-"]').forEach(function (el) {
    var dataEl = document.getElementById('accuracy-data-' + el.id.substring('accuracy-'.length));
    if (!dataEl) return;
    var data = JSON.parse(dataEl.textContent);
    new Chart(el, {
      type: 'bar',
      data: { labels: data.labels, datasets: [
        { label: 'KI-Vorschau', data: data.rates, backgroundColor: '#1E9E5A', borderRadius: 4 },
        { label: 'Basisprognose', data: data.baselineRates, backgroundColor: '#C9D3CD', borderRadius: 4 }
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
