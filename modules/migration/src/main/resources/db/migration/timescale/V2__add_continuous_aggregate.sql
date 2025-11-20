-- 每分钟的平均指标
CREATE MATERIALIZED VIEW session_metrics_1min
WITH (timescaledb.continuous) AS
SELECT
  time_bucket('1 minute', time) AS bucket,
  session_id,
  simulation_id,
  AVG(current_fps) AS avg_fps,
  MAX(current_fps) AS max_fps,
  MIN(current_fps) AS min_fps,
  AVG(gpu_utilization) AS avg_gpu_util,
  MAX(gpu_memory_mb) AS max_gpu_memory
FROM session_metrics_history
GROUP BY bucket, session_id, simulation_id;

-- 自动刷新策略
SELECT add_continuous_aggregate_policy(
  'session_metrics_1min',
  start_offset => INTERVAL '1 hour',
  end_offset => INTERVAL '1 minute',
  schedule_interval => INTERVAL '1 minute',
  if_not_exists => TRUE
);

-- 每小时的聚合
CREATE MATERIALIZED VIEW session_metrics_1hour
WITH (timescaledb.continuous) AS
SELECT
  time_bucket('1 hour', time) AS bucket,
  session_id,
  simulation_id,
  AVG(current_fps) AS avg_fps,
  PERCENTILE_CONT(0.5) WITHIN GROUP (ORDER BY current_fps) AS p50_fps,
  PERCENTILE_CONT(0.99) WITHIN GROUP (ORDER BY current_fps) AS p99_fps,
  AVG(gpu_utilization) AS avg_gpu_util
FROM session_metrics_history
GROUP BY bucket, session_id, simulation_id;

SELECT add_continuous_aggregate_policy(
  'session_metrics_1hour',
  start_offset => INTERVAL '3 hours',
  end_offset => INTERVAL '1 hour',
  schedule_interval => INTERVAL '1 hour',
  if_not_exists => TRUE
);