-- Spec 03: the baseline forecast (plain statistics before kickoff) is kept with the
-- situation, so every evaluated forecast can be measured against it. Null until derived.
alter table situation
    add column baseline_home_win double precision,
    add column baseline_draw     double precision,
    add column baseline_away_win double precision;
