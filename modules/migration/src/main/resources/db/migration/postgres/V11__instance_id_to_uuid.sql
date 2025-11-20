ALTER TABLE training_jobs 
  DROP COLUMN training_instance_id;

ALTER TABLE training_jobs 
  ADD COLUMN training_instance_id UUID;