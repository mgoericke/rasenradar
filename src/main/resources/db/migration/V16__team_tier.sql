-- Spec 06: which division a club played in during a season — the basis for "Ueberraschung"
-- in the cup. Derived from the team lists of the first three divisions; the third division
-- is a reference list only, not a competition of its own.
create sequence team_tier_seq start with 1 increment by 50;

create table team_tier (
    id      bigint      not null primary key,
    team_id bigint      not null references team (id),
    season  integer     not null,
    tier    varchar(10) not null,
    constraint uk_team_tier unique (team_id, season)
);
