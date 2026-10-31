-- Club crests are downloaded once and served by the application itself, so that no visitor's
-- browser contacts the crest hosts (Wikimedia, imgur, dfb.de, …). One row per team: the source
-- URL it was taken from, and either the processed image or, after a failed attempt, no data.
create table team_crest (
    team_id      bigint primary key references team (id),
    source_url   text        not null,
    content_type varchar(40),
    data         bytea,
    fetched_at   timestamptz not null
);

-- the file name pages link to (content hash plus extension); null until a crest is stored
alter table team add column crest_file varchar(40);
