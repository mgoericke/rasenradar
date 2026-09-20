-- Spec 04: season outlook. A snapshot per team, replaced wholesale after each completed
-- matchday of its league — no history, see spec 04's rules.

create sequence season_outlook_seq start with 1 increment by 50;

create table season_outlook (
    id          bigint      not null primary key,
    league      varchar(20) not null,
    season      integer     not null,
    team_id     bigint      not null,
    computed_at timestamptz not null
);
create unique index season_outlook_team_idx on season_outlook (league, season, team_id);

create table season_outlook_probability (
    season_outlook_id bigint           not null references season_outlook(id),
    placement_goal     varchar(50)      not null,
    probability         double precision not null,
    primary key (season_outlook_id, placement_goal)
);
