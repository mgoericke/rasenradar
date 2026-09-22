-- Which section of the external source a matchday came from — the unit the source
-- synchronises and reports changes for. In a league and in a cup that is the matchday
-- (or round) itself; in a group competition one section is a whole group holding six
-- matchdays, and without this column the change check cannot find them before loading.
alter table matchday add column section_number integer;
update matchday set section_number = number where section_number is null;
alter table matchday alter column section_number set not null;
