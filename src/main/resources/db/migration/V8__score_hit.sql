-- Spec 03: distinguish "tendency right" from "exact score right" (a Volltreffer) —
-- the review keeps the forecast's own expected score to compare against the final result.
alter table recorded_forecast
    add column expected_home_goals integer,
    add column expected_away_goals integer;
alter table forecast_evaluation
    add column score_hit boolean not null default false;

update recorded_forecast rf
set expected_home_goals = f.expected_home_goals,
    expected_away_goals = f.expected_away_goals
from forecast f
where f.id = rf.forecast_id;

alter table recorded_forecast
    alter column expected_home_goals set not null,
    alter column expected_away_goals set not null;

update forecast_evaluation fe
set score_hit = true
from recorded_forecast rf, situation s
where rf.forecast_id = fe.forecast_id
  and s.match_id = fe.match_id
  and rf.expected_home_goals = s.home_goals
  and rf.expected_away_goals = s.away_goals;
