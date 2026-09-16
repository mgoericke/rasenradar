-- Spec 02: forecasts. A forecast is written once and never updated or deleted;
-- a new forecast for the same match is a new row.

create sequence forecast_seq   start with 1 increment by 50;
create sequence assessment_seq start with 1 increment by 50;

-- The assessment scales (spec 02, rules): editable at runtime, one row.
create table forecast_parameters (
    id                  bigint  not null primary key,
    home_advantage      double precision not null,
    form_matches        integer not null,
    promoted_team_malus double precision not null
);
insert into forecast_parameters (id, home_advantage, form_matches, promoted_team_malus) values (1, 0.10, 5, 0.05);

create table forecast (
    id                    bigint      not null primary key,
    match_id              bigint      not null references match,
    created_at            timestamptz not null,
    -- snapshot of the match as forecast (the match itself may be corrected later)
    league                varchar(20) not null,
    season                integer     not null,
    matchday              integer     not null,
    home_team             varchar(100) not null,
    away_team             varchar(100) not null,
    kickoff               timestamptz not null,
    -- the forecast itself; the three probabilities sum to 1
    home_win              double precision not null,
    draw                  double precision not null,
    away_win              double precision not null,
    expected_home_goals   integer     not null,
    expected_away_goals   integer     not null,
    confidence            double precision not null,
    reasoning             text        not null,
    -- outcome of the review (spec 02, steps 5-6)
    verdict               varchar(20) not null,
    verdict_reason        text,
    revised               boolean     not null,
    objection_remains     boolean     not null,
    -- the scales in effect when this forecast was made
    home_advantage        double precision not null,
    form_matches          integer     not null,
    promoted_team_malus   double precision not null,
    model_name            varchar(100),
    check (abs(home_win + draw + away_win - 1) < 0.001)
);
create index forecast_match_idx on forecast (match_id);

create table assessment (
    id          bigint      not null primary key,
    forecast_id bigint      not null references forecast on delete cascade,
    kind        varchar(20) not null,
    lean        varchar(20),
    confidence  double precision,
    summary     text        not null,
    failed      boolean     not null default false,
    unique (forecast_id, kind)
);
