-- src/main/resources/db/migration/timescale/V1__create_session_metrics.sql

-- ========================================
-- 1. 实时快照表 (Latest Snapshot) - 普通表
-- ========================================
CREATE TABLE IF NOT EXISTS session_metrics_snapshot (
  session_id UUID PRIMARY KEY,  -- 每个 session 只有一条最新记录
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

-- 快照表索引
CREATE INDEX idx_snapshot_simulation ON session_metrics_snapshot(simulation_id);
CREATE INDEX idx_snapshot_updated_at ON session_metrics_snapshot(updated_at DESC);

-- ========================================
-- 2. 时序历史表 - TimescaleDB Hypertable
-- ========================================
CREATE TABLE IF NOT EXISTS session_metrics_history (
  time TIMESTAMPTZ NOT NULL,
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

  tags JSONB,

  -- 时序数据的复合主键
  PRIMARY KEY (session_id, time)
);

-- 转为 Hypertable (7 天一个 chunk)
SELECT create_hypertable(
  'session_metrics_history',
  'time',
  chunk_time_interval => INTERVAL '7 days',
  if_not_exists => TRUE
);

-- 历史表索引
CREATE INDEX idx_history_session_time ON session_metrics_history(session_id, time DESC);
CREATE INDEX idx_history_simulation_time ON session_metrics_history(simulation_id, time DESC);
CREATE INDEX idx_history_time ON session_metrics_history(time DESC);

-- ========================================
-- 3. 数据保留和压缩策略
-- ========================================

-- 自动删除 90 天前的历史数据
SELECT add_retention_policy(
  'session_metrics_history',
  INTERVAL '90 days',
  if_not_exists => TRUE
);

-- 启用压缩
ALTER TABLE session_metrics_history SET (
  timescaledb.compress,
  timescaledb.compress_segmentby = 'session_id',
  timescaledb.compress_orderby = 'time DESC'
);

-- 7 天后自动压缩
SELECT add_compression_policy(
  'session_metrics_history',
  INTERVAL '7 days',
  if_not_exists => TRUE
);