-- 1. 删除旧的物化视图
DROP MATERIALIZED VIEW IF EXISTS session_metrics_1min;

-- 2. 重新创建(去掉WHERE子句)
CREATE MATERIALIZED VIEW session_metrics_1min AS
SELECT
  date_trunc('minute', time) AS bucket,
  session_id,
  simulation_id,
  AVG(current_fps) AS avg_fps,
  MAX(current_fps) AS max_fps,
  MIN(current_fps) AS min_fps,
  AVG(gpu_utilization) AS avg_gpu_util,
  MAX(gpu_memory_mb) AS max_gpu_memory
FROM session_metrics_history
-- ❌ 去掉 WHERE 子句,改用增量刷新策略
GROUP BY bucket, session_id, simulation_id;

-- 3. 创建唯一索引(必须,用于CONCURRENTLY刷新)
-- bucket + session_id + simulation_id 组合唯一标识一行
CREATE UNIQUE INDEX session_metrics_1min_unique_idx 
ON session_metrics_1min (bucket, session_id, simulation_id);

-- 4. 创建其他查询索引
CREATE INDEX idx_1min_bucket ON session_metrics_1min(bucket DESC);
CREATE INDEX idx_1min_session ON session_metrics_1min(session_id, bucket DESC);

-- 5. 权限设置
REVOKE ALL PRIVILEGES ON session_metrics_1min FROM anon, authenticated;
GRANT SELECT ON session_metrics_1min TO service_role;

COMMENT ON MATERIALIZED VIEW session_metrics_1min IS '每分钟会话指标聚合（并发刷新,保留全量数据）';