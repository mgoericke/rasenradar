-- Story r039: the median final points total at every table position of the latest season outlook
-- run, replaced together with the outlook rows — "Was braucht {Verein}?" reads it, never recomputes.
create sequence season_threshold_seq start with 1 increment by 50;

create table season_threshold (
    id       bigint      not null primary key,
    league   varchar(20) not null,
    season   integer     not null,
    position integer     not null,
    points   integer     not null
);
create unique index season_threshold_position_idx on season_threshold (league, season, position);

-- the run's inputs per team, so the page can say what is still missing without asking the table again
alter table season_outlook add column points integer;
alter table season_outlook add column remaining integer;
