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
