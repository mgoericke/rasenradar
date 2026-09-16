-- When the matchday was last checked against the source (successful or not changed),
-- shown to the viewer as the age of the data when the source is down.
alter table matchday add column last_checked_at timestamptz;
