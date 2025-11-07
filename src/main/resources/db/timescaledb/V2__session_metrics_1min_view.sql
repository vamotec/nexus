-- 1分钟平均指标
CREATE MATERIALIZED VIEW session_metrics_1min
WITH (timescaledb.continuous) AS
SELECT
  session_id,
  time_bucket('1 minute', time) AS bucket,
  AVG(current_fps) AS avg_fps,
  MAX(gpu_utilization) AS max_gpu,
  LAST(robot_position_x, time) AS final_x
FROM session_metrics_history
GROUP BY session_id, bucket;

-- 自动刷新
SELECT add_continuous_aggregate_policy('session_metrics_1min',
  start_offset => INTERVAL '3 months',
  end_offset => INTERVAL '1 minute',
  schedule_interval => INTERVAL '1 minute');