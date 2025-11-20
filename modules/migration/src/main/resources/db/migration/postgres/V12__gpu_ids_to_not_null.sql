ALTER TABLE training_jobs DROP COLUMN IF EXISTS gpu_ids;

ALTER TABLE training_jobs ADD COLUMN gpu_ids INT[] NOT NULL DEFAULT '{}';