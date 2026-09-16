-- Spec 01: matchday data (leagues, seasons, matchdays, matches, goals).
-- Hibernate expects one sequence per entity (<entity>_seq, increment 50)
-- because PanacheEntity uses @GeneratedValue(AUTO).

create sequence team_seq     start with 1 increment by 50;
create sequence matchday_seq start with 1 increment by 50;
create sequence match_seq    start with 1 increment by 50;
create sequence goal_seq     start with 1 increment by 50;

-- A team keeps its identity across seasons, promotion and relegation via the
-- source's stable external id. Name and short name may change.
create table team (
    id          bigint       not null primary key,
    external_id integer      not null unique,
    name        varchar(100) not null,
    short_name  varchar(30)
);

-- Each league has its own matchday counter: (league, season, number) is unique,
-- there is no counter shared across leagues.
create table matchday (
    id                     bigint      not null primary key,
    league                 varchar(20) not null,
    season                 integer     not null,
    number                 integer     not null,
    source_last_changed_at timestamptz,
    unique (league, season, number)
);

create table match (
    id                     bigint      not null primary key,
    external_id            integer     not null unique,
    matchday_id            bigint      not null references matchday,
    home_team_id           bigint      not null references team,
    away_team_id           bigint      not null references team,
    kickoff                timestamptz not null,
    half_time_home         integer,
    half_time_away         integer,
    full_time_home         integer,
    full_time_away         integer,
    -- PROVISIONAL until 24h after kickoff without further change, then FINAL
    result_status          varchar(20) not null,
    source_last_changed_at timestamptz,
    check (home_team_id <> away_team_id)
);

create index match_matchday_idx  on match (matchday_id);
create index match_home_team_idx on match (home_team_id);
create index match_away_team_idx on match (away_team_id);

create table goal (
    id               bigint  not null primary key,
    match_id         bigint  not null references match on delete cascade,
    -- order within the match; goals are replaced as a whole on every sync
    position         integer not null,
    minute           integer,
    scorer_name      varchar(100),
    score_after_home integer not null,
    score_after_away integer not null,
    penalty          boolean not null default false,
    own_goal         boolean not null default false,
    unique (match_id, position)
);
