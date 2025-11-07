-- ========================================
-- 1. 实时快照表（Latest Metrics） - 普通表
-- ========================================
CREATE TABLE IF NOT EXISTS session_metrics (
  time TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
  simulation_id UUID NOT NULL,
  session_id UUID NOT NULL,

  current_fps DOUBLE PRECISION,
  frame_count BIGINT,
  simulation_time DOUBLE PRECISION,
  wall_time DOUBLE PRECISION,
  robot_position_x DOUBLE PRECISION,
  robot_position_y DOUBLE PRECISION,
  robot_position_z DOUBLE PRECISION,
  gpu_utilization DOUBLE PRECISION,
  gpu_memory_mb BIGINT,

  tags JSONB,

  PRIMARY KEY (session_id),  -- 每个 session 只有一条最新记录
  CONSTRAINT fk_session FOREIGN KEY (session_id) REFERENCES sessions(id) ON DELETE CASCADE
);

-- 索引：快速按 simulation_id 查找所有运行中的 session
CREATE INDEX idx_session_metrics_simulation_id ON session_metrics(simulation_id);
CREATE INDEX idx_session_metrics_time ON session_metrics(time DESC);


-- ========================================
-- 2. 时序历史表 - TimescaleDB Hypertable
-- ========================================
CREATE TABLE IF NOT EXISTS session_metrics_history (
  time TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
  simulation_id UUID NOT NULL,
  session_id UUID NOT NULL,

  current_fps DOUBLE PRECISION NOT NULL,
  frame_count BIGINT NOT NULL,
  simulation_time DOUBLE PRECISION NOT NULL,
  wall_time DOUBLE PRECISION NOT NULL,
  robot_position_x DOUBLE PRECISION NOT NULL,
  robot_position_y DOUBLE PRECISION NOT NULL,
  robot_position_z DOUBLE PRECISION NOT NULL,
  gpu_utilization DOUBLE PRECISION NOT NULL,
  gpu_memory_mb BIGINT NOT NULL,

  tags JSONB
);

-- 转为 hypertable
SELECT create_hypertable('session_metrics_history', 'time');

-- 索引（必须！）
CREATE INDEX idx_metrics_history_session_id ON session_metrics_history(session_id);
CREATE INDEX idx_metrics_history_simulation_id ON session_metrics_history(simulation_id);
CREATE INDEX idx_metrics_history_time ON session_metrics_history(time DESC);

-- 可选：触发器校验 session_id 存在（一致性）
CREATE OR REPLACE FUNCTION check_session_exists()
RETURNS TRIGGER AS $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM sessions WHERE id = NEW.session_id) THEN
    RAISE EXCEPTION 'session_id % not found', NEW.session_id;
  END IF;
  RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_check_session_metrics_history
  BEFORE INSERT ON session_metrics_history
  FOR EACH ROW EXECUTE FUNCTION check_session_exists();