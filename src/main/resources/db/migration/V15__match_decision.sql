-- Spec 06: a knockout match can go to extra time or a penalty shootout. The shootout
-- aggregate is kept separately — it decides the winner but is not a score of the match.
alter table match add column decision varchar(20) not null default 'REGULAR';
alter table match add column penalty_home integer;
alter table match add column penalty_away integer;
