-- Spec 05, "Mut-Bilanz": carry the forecast's "Gegen den Strom" flag into review's own
-- records, the same way `backtest` already is, so the hit rate can report it separately.
alter table recorded_forecast add column contrarian boolean;
alter table forecast_evaluation add column contrarian boolean;
