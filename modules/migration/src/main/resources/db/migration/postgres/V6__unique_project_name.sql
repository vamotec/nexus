ALTER TABLE projects
ADD CONSTRAINT projects_owner_name_unique
UNIQUE (owner_id, name);

ALTER TABLE projects
ADD CONSTRAINT projects_name_valid
CHECK (
  name ~ '^[a-zA-Z0-9_-]+$' AND
  LENGTH(name) > 0 AND
  LENGTH(name) <= 100
);