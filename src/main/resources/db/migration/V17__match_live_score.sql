-- Spec 01: the running score of a match in progress ("Zwischenstand"). Display only — it is
-- never a result, so it gets its own columns instead of borrowing full_time_*, which tables,
-- form and the review read.
alter table match add column live_home integer;
alter table match add column live_away integer;
