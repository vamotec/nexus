ALTER TABLE sessions
  DROP COLUMN IF EXISTS latest_fps;

ALTER TABLE sessions
  DROP COLUMN IF EXISTS latest_simulation_time;

ALTER TABLE sessions
  DROP COLUMN IF EXISTS latest_robot_position;

ALTER TABLE sessions
  DROP COLUMN IF EXISTS latest_gpu_utilization;

ALTER TABLE sessions
  DROP COLUMN IF EXISTS metrics_last_updated_at;