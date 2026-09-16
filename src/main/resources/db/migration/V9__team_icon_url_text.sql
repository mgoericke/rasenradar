-- OpenLigaDB delivers at least one club crest as an inline data: URI (base64 PNG) instead of a
-- regular external URL — several kilobytes, far past the 500 chars the column allowed for.
alter table team alter column icon_url type text;
