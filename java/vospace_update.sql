

CREATE INDEX nod_own_id_idx on nodes (owner(25), identifier(250));
CREATE INDEX nod_own_dep_id_idx on nodes (owner(25), depth, identifier(250));

-- Make location and indentifier indices more compact.
-- Originally we make the indices of 764 bytes long but
-- I can't find a location or identifier longer than
-- 225, so setting it to 250 saves quite a bit of space
-- and should traverse memory pages slighly faster.
-- DROP INDEX nod_id_idx on nodes;
-- CREATE INDEX nod_id_idx on nodes (identifier(250));
-- DROP INDEX nod_loc_idx on nodes;
-- CREATE INDEX nod_loc_idx on nodes (location(250));

-- Possibly drop the following indices
-- DROP INDEX nod_own_idx on nodes;
-- DROP INDEX nod_dep_idx on nodes;
-- DROP INDEX nod_typ_idx on nodes;

