-- Spec 03: review and learning. Situations are stored for every completed match,
-- recorded forecasts when a forecast is committed, evaluations once the result is
-- final. Nothing here is ever updated or deleted.

create sequence situation_seq           start with 1 increment by 50;
create sequence recorded_forecast_seq   start with 1 increment by 50;
create sequence forecast_evaluation_seq start with 1 increment by 50;

-- The constellation before a completed match, reduced to comparable features (spec 03, step 3).
create table situation (
    id              bigint       not null primary key,
    match_id        bigint       not null unique,
    league          varchar(20)  not null,
    season          integer      not null,
    matchday        integer      not null,
    kickoff         timestamptz  not null,
    home_team       varchar(100) not null,
    away_team       varchar(100) not null,
    -- features: table position gap (home minus away), form points of the last matches,
    -- points per game at home / away, promoted flags
    position_gap    integer,
    home_form_points integer     not null,
    away_form_points integer     not null,
    form_matches    integer      not null,
    home_home_ppg   double precision not null,
    away_away_ppg   double precision not null,
    home_promoted   boolean      not null,
    away_promoted   boolean      not null,
    -- the actual outcome
    home_goals      integer      not null,
    away_goals      integer      not null
);
create index situation_league_idx on situation (league, season);

-- A forecast as handed over by the forecast feature when it was committed.
create table recorded_forecast (
    id           bigint      not null primary key,
    forecast_id  bigint      not null unique,
    match_id     bigint      not null,
    league       varchar(20) not null,
    season       integer     not null,
    matchday     integer     not null,
    created_at   timestamptz not null,
    home_win     double precision not null,
    draw         double precision not null,
    away_win     double precision not null,
    confidence   double precision not null
);
create index recorded_forecast_match_idx on recorded_forecast (match_id);

-- The comparison of a recorded forecast with the final result (spec 03, steps 1-2).
create table forecast_evaluation (
    id                     bigint      not null primary key,
    forecast_id            bigint      not null unique,
    match_id               bigint      not null,
    league                 varchar(20) not null,
    season                 integer     not null,
    matchday               integer     not null,
    evaluated_at           timestamptz not null,
    predicted_outcome      varchar(20) not null,
    actual_outcome         varchar(20) not null,
    tendency_hit           boolean     not null,
    probability_of_actual  double precision not null,
    brier_score            double precision not null,
    confidence             double precision not null,
    confidence_verdict     varchar(20) not null
);
create index forecast_evaluation_league_idx on forecast_evaluation (league, season, matchday);

-- The retrospective the forecaster saw, kept with the forecast (spec 03, step 5).
alter table forecast add column retrospective text;
