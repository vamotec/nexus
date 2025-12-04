-- ============================================
-- Mosia Nexus Metrics Schema (Standard PostgreSQL)
-- PostgreSQL 17+ (No TimescaleDB)
-- 适用于 Supabase Free Tier + 本地开发
-- ============================================

-- ============================================
-- 1. 会话指标实时快照表
-- ============================================

CREATE TABLE session_metrics_snapshot (
  session_id UUID PRIMARY KEY,
  simulation_id UUID NOT NULL,

  -- 最新指标
  current_fps DOUBLE PRECISION NOT NULL,
  frame_count BIGINT NOT NULL,
  simulation_time DOUBLE PRECISION NOT NULL,
  wall_time DOUBLE PRECISION NOT NULL,

  -- 机器人位置
  robot_position_x DOUBLE PRECISION NOT NULL,
  robot_position_y DOUBLE PRECISION NOT NULL,
  robot_position_z DOUBLE PRECISION NOT NULL,

  -- GPU 指标
  gpu_utilization DOUBLE PRECISION NOT NULL,
  gpu_memory_mb BIGINT NOT NULL,

  -- 元数据
  tags JSONB,
  updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- 索引
CREATE INDEX idx_snapshot_simulation ON session_metrics_snapshot(simulation_id);
CREATE INDEX idx_snapshot_updated_at ON session_metrics_snapshot(updated_at DESC);

COMMENT ON TABLE session_metrics_snapshot IS '会话指标实时快照表 - 存储每个会话的最新指标';

-- ============================================
-- 2. 会话指标时序历史表（标准表，非 Hypertable）
-- ============================================

CREATE TABLE session_metrics_history (
  id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  time TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  session_id UUID NOT NULL,
  simulation_id UUID NOT NULL,

  -- 性能指标
  current_fps DOUBLE PRECISION NOT NULL,
  frame_count BIGINT NOT NULL,
  simulation_time DOUBLE PRECISION NOT NULL,
  wall_time DOUBLE PRECISION NOT NULL,

  -- 机器人位置
  robot_position_x DOUBLE PRECISION NOT NULL,
  robot_position_y DOUBLE PRECISION NOT NULL,
  robot_position_z DOUBLE PRECISION NOT NULL,

  -- GPU 指标
  gpu_utilization DOUBLE PRECISION NOT NULL,
  gpu_memory_mb BIGINT NOT NULL,

  tags JSONB
);

-- 关键索引（优化时间范围 + 会话查询）
CREATE INDEX idx_history_session_time ON session_metrics_history(session_id, time DESC);
CREATE INDEX idx_history_simulation_time ON session_metrics_history(simulation_id, time DESC);
CREATE INDEX idx_history_time ON session_metrics_history(time DESC);

COMMENT ON TABLE session_metrics_history IS '会话指标时序历史表（标准 PostgreSQL 表）';

-- ============================================
-- 3. 聚合视图：每分钟指标（替代 Continuous Aggregate）
-- ============================================

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
WHERE time >= NOW() - INTERVAL '30 days'  -- 限制范围提升刷新速度
GROUP BY bucket, session_id, simulation_id;

-- 为聚合视图建索引
CREATE INDEX idx_1min_bucket ON session_metrics_1min(bucket DESC);
CREATE INDEX idx_1min_session ON session_metrics_1min(session_id, bucket DESC);
-- 🛡️ 关键：撤销公共角色的访问权限
REVOKE ALL PRIVILEGES ON session_metrics_1min FROM anon, authenticated;
-- 只允许 service_role（后台服务）访问
GRANT SELECT ON session_metrics_1min TO service_role;

COMMENT ON MATERIALIZED VIEW session_metrics_1min IS '每分钟会话指标聚合（手动或定时刷新）';

-- ============================================
-- 4. 聚合视图：每小时指标
-- ============================================

CREATE MATERIALIZED VIEW session_metrics_1hour AS
SELECT
  date_trunc('hour', time) AS bucket,
  session_id,
  simulation_id,
  AVG(current_fps) AS avg_fps,
  PERCENTILE_CONT(0.5) WITHIN GROUP (ORDER BY current_fps) AS p50_fps,
  PERCENTILE_CONT(0.99) WITHIN GROUP (ORDER BY current_fps) AS p99_fps,
  AVG(gpu_utilization) AS avg_gpu_util
FROM session_metrics_history
WHERE time >= NOW() - INTERVAL '90 days'
GROUP BY bucket, session_id, simulation_id;

CREATE INDEX idx_1hour_bucket ON session_metrics_1hour(bucket DESC);
CREATE INDEX idx_1hour_session ON session_metrics_1hour(session_id, bucket DESC);

-- 🛡️ 关键：撤销公共角色的访问权限
REVOKE ALL PRIVILEGES ON session_metrics_1hour FROM anon, authenticated;
-- 只允许 service_role（后台服务）访问
GRANT SELECT ON session_metrics_1hour TO service_role;

COMMENT ON MATERIALIZED VIEW session_metrics_1hour IS '每小时会话指标聚合';

-- ============================================
-- 5. 自动维护任务（使用 pg_cron）
-- ============================================

-- 5.1 每 5 分钟刷新 1 分钟聚合视图
SELECT cron.schedule('refresh-metrics-1min', '*/5 * * * *', $$
  REFRESH MATERIALIZED VIEW CONCURRENTLY session_metrics_1min
$$);

-- 5.2 每小时刷新 1 小时聚合视图
SELECT cron.schedule('refresh-metrics-1hour', '0 * * * *', $$
  REFRESH MATERIALIZED VIEW CONCURRENTLY session_metrics_1hour
$$);

-- 5.3 每天凌晨删除 90 天前的原始历史数据
SELECT cron.schedule('cleanup-old-metrics', '0 0 * * *', $$
  DELETE FROM session_metrics_history WHERE time < NOW() - INTERVAL '90 days'
$$);

-- 5.4 （可选）每 30 天清理聚合视图中的旧数据（保持视图轻量）
SELECT cron.schedule('cleanup-old-aggregates', '0 0 1 */1 *', $$
  -- 1min 聚合保留 60 天
  DELETE FROM session_metrics_1min WHERE bucket < NOW() - INTERVAL '60 days';
  -- 1hour 聚合保留 365 天
  DELETE FROM session_metrics_1hour WHERE bucket < NOW() - INTERVAL '365 days';
$$);

-- ============================================
-- 6. metrics禁用 RLS
-- ============================================
REVOKE ALL ON session_metrics_snapshot FROM anon, authenticated;
GRANT SELECT, INSERT, UPDATE, DELETE ON session_metrics_snapshot TO service_role;

REVOKE ALL ON session_metrics_history FROM anon, authenticated;
GRANT SELECT, INSERT, UPDATE, DELETE ON session_metrics_history TO service_role;