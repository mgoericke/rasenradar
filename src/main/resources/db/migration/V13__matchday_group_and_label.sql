-- Spec 06: a matchday can belong to a parallel group (Nations League) and can carry the
-- name of a knockout round (DFB-Pokal). The group is part of the matchday's identity:
-- in a group competition every group counts its own matchdays, so (league, season, number)
-- alone is no longer unique.
alter table matchday add column group_name varchar(40);
alter table matchday add column label varchar(40);

-- Postgres treats nulls in a unique constraint as distinct, which would let the same
-- league/season/number exist many times over for the competitions without groups.
-- coalesce in a unique index keeps the old guarantee for them.
alter table matchday drop constraint matchday_league_season_number_key;
create unique index uk_matchday_section on matchday (league, season, coalesce(group_name, ''), number);
