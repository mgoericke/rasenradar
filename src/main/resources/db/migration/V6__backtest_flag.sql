-- Backtest forecasts: made for matches of the current season that are already played,
-- with the facts as they were before kickoff. Flagged everywhere so the hit rate can
-- report live and backtest forecasts separately.
alter table forecast            add column backtest boolean not null default false;
alter table recorded_forecast   add column backtest boolean not null default false;
alter table forecast_evaluation add column backtest boolean not null default false;
