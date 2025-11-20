-- ============================================
-- Mosia Nexus Database Schema
-- PostgreSQL 17+
-- ============================================

-- 扩展
CREATE EXTENSION IF NOT EXISTS "pg_trgm";  -- 用于文本搜索

-- ============================================
-- 1. 用户表
-- ============================================

CREATE TABLE IF NOT EXISTS users (
  id UUID PRIMARY KEY,
  email VARCHAR(255) UNIQUE NOT NULL,
  name VARCHAR(255) NOT NULL,
  avatar VARCHAR(255),
  password_hash VARCHAR(255) NOT NULL,
  organization VARCHAR(255),
  role VARCHAR(20) NOT NULL DEFAULT 'developer',

  -- 状态
  is_active BOOLEAN NOT NULL DEFAULT TRUE,
  email_verified BOOLEAN NOT NULL DEFAULT FALSE,

  -- 时间戳
  created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
  last_login_at TIMESTAMPTZ,

  CONSTRAINT users_role_check CHECK (role IN ('admin', 'developer', 'viewer'))
);

-- 索引
CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_organization ON users(organization);
CREATE INDEX idx_users_created_at ON users(created_at DESC);

-- 更新时间触发器
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
  NEW.updated_at = CURRENT_TIMESTAMP;
  RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER users_updated_at
  BEFORE UPDATE ON users
  FOR EACH ROW
  EXECUTE FUNCTION update_updated_at_column();

-- ============================================
-- 2. 用户配额表
-- ============================================

CREATE TABLE IF NOT EXISTS user_quotas (
  user_id UUID PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,

  -- 配额限制
  max_concurrent_sessions INT NOT NULL DEFAULT 5,
  max_gpu_hours_per_month DOUBLE PRECISION NOT NULL DEFAULT 100.0,
  max_storage_gb DOUBLE PRECISION NOT NULL DEFAULT 50.0,

  -- 当前使用量
  current_active_sessions INT NOT NULL DEFAULT 0,
  current_gpu_hours_this_month DOUBLE PRECISION NOT NULL DEFAULT 0.0,
  current_storage_gb DOUBLE PRECISION NOT NULL DEFAULT 0.0,

  -- 统计周期
  quota_reset_at TIMESTAMPTZ NOT NULL DEFAULT DATE_TRUNC('month', CURRENT_TIMESTAMP + INTERVAL '1 month'),

  updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

  CONSTRAINT check_positive_limits CHECK (
    max_concurrent_sessions > 0 AND
    max_gpu_hours_per_month > 0 AND
    max_storage_gb > 0
  )
);

-- ============================================
-- 3. 项目表
-- ============================================

CREATE TABLE IF NOT EXISTS projects (
  id UUID PRIMARY KEY,
  name VARCHAR(255) NOT NULL,
  description TEXT,
  owner_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,

  -- 设置 (JSON)
  settings JSONB NOT NULL DEFAULT '{
    "default_environment": "warehouse",
    "auto_save_results": true,
    "retention_days": 30
  }'::JSONB,

  -- 元数据
  tags TEXT[] DEFAULT '{}',
  is_archived BOOLEAN NOT NULL DEFAULT FALSE,

  -- 时间戳
  created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

  -- 约束
  CONSTRAINT projects_name_not_empty CHECK (LENGTH(TRIM(name)) > 0)
);

-- 索引
CREATE INDEX idx_projects_owner_id ON projects(owner_id);
CREATE INDEX idx_projects_created_at ON projects(created_at DESC);
CREATE INDEX idx_projects_tags ON projects USING GIN(tags);
CREATE INDEX idx_projects_is_archived ON projects(is_archived) WHERE is_archived = FALSE;

-- 全文搜索索引
CREATE INDEX idx_projects_name_trgm ON projects USING GIN(name gin_trgm_ops);
CREATE INDEX idx_projects_description_trgm ON projects USING GIN(description gin_trgm_ops);

-- 更新时间触发器
CREATE TRIGGER projects_updated_at
  BEFORE UPDATE ON projects
  FOR EACH ROW
  EXECUTE FUNCTION update_updated_at_column();

-- ============================================
-- 4. 项目协作者表
-- ============================================

CREATE TABLE IF NOT EXISTS project_collaborators (
  project_id UUID NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
  user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  permission VARCHAR(20) NOT NULL DEFAULT 'viewer',

  added_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
  added_by UUID REFERENCES users(id),

  PRIMARY KEY (project_id, user_id),
  CONSTRAINT collab_permission_check CHECK (permission IN ('admin', 'editor', 'viewer'))
);

-- 索引
CREATE INDEX idx_collaborators_user_id ON project_collaborators(user_id);

-- ============================================
-- 5. 仿真表
-- ============================================

CREATE TABLE IF NOT EXISTS simulations (
  id UUID PRIMARY KEY,
  project_id UUID NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
  name VARCHAR(255) NOT NULL,
  description TEXT,

  -- 版本控制
  version_major INT NOT NULL DEFAULT 1,
  version_minor INT NOT NULL DEFAULT 0,

  -- SceneConfig 核心字段（结构化）
  scene_name TEXT NOT NULL,
  robot_type TEXT NOT NULL,                    -- e.g., 'quadruped', 'humanoid'
  robot_urdf TEXT NOT NULL,                    -- URDF 内容或路径
  environment JSONB NOT NULL DEFAULT '{}',     -- 复杂但稳定，可用 JSONB

  -- 起始位置 (Position3D)
  start_pos_x DOUBLE PRECISION NOT NULL,
  start_pos_y DOUBLE PRECISION NOT NULL,
  start_pos_z DOUBLE PRECISION NOT NULL,

  -- 目标位置 (Option[Position3D])
  goal_pos_x DOUBLE PRECISION,
  goal_pos_y DOUBLE PRECISION,
  goal_pos_z DOUBLE PRECISION,

  -- 仿真参数 (JSON)
  simulation_params JSONB NOT NULL DEFAULT '{
    "physics_engine": "physx",
    "time_step": 0.016,
    "max_duration": 300,
    "real_time": true,
    "record_video": false,
    "record_trajectory": false
  }'::JSONB,

  -- 训练配置 (可选)
  training_config JSONB,

--  -- 状态字段
--  status TEXT NOT NULL CHECK (status IN ('pending','running','completed','failed','stopped','cancelling')),
--  started_at TIMESTAMPTZ,
--  ended_at TIMESTAMPTZ,
--  failure_reason TEXT,

  -- 统计信息
  total_runs INT NOT NULL DEFAULT 0,
  successful_runs INT NOT NULL DEFAULT 0,
  failed_runs INT NOT NULL DEFAULT 0,
  avg_completion_time DOUBLE PRECISION NOT NULL DEFAULT 0.0,
  avg_collisions DOUBLE PRECISION NOT NULL DEFAULT 0.0,
  best_session_id UUID,
  last_run_at TIMESTAMPTZ,

  -- 元数据
  tags TEXT[] DEFAULT '{}',
  created_by UUID NOT NULL REFERENCES users(id),
  is_archived BOOLEAN NOT NULL DEFAULT FALSE,

  -- 时间戳
  created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

  -- 约束
  CONSTRAINT simulations_name_not_empty CHECK (LENGTH(TRIM(name)) > 0),
  CONSTRAINT simulations_stats_positive CHECK (
    total_runs >= 0 AND
    successful_runs >= 0 AND
    failed_runs >= 0 AND
    successful_runs + failed_runs <= total_runs
  )
);

-- 索引
CREATE INDEX idx_simulations_project_id ON simulations(project_id);
CREATE INDEX idx_simulations_created_by ON simulations(created_by);
CREATE INDEX idx_simulations_created_at ON simulations(created_at DESC);
CREATE INDEX idx_simulations_tags ON simulations USING GIN(tags);
CREATE INDEX idx_simulations_is_archived ON simulations(is_archived) WHERE is_archived = FALSE;
CREATE INDEX idx_simulations_last_run_at ON simulations(last_run_at DESC NULLS LAST);

-- 全文搜索
CREATE INDEX idx_simulations_name_trgm ON simulations USING GIN(name gin_trgm_ops);

-- 更新时间触发器
CREATE TRIGGER simulations_updated_at
  BEFORE UPDATE ON simulations
  FOR EACH ROW
  EXECUTE FUNCTION update_updated_at_column();

CREATE TABLE simulation_obstacles (
  simulation_id UUID NOT NULL REFERENCES simulations(id) ON DELETE CASCADE,
  obstacle_id UUID NOT NULL DEFAULT gen_random_uuid(),  -- 对应 ObstacleId

  -- Obstacle 字段
  obstacle_type TEXT NOT NULL,          -- e.g., 'box', 'cylinder', 'mesh'
  position_x DOUBLE PRECISION NOT NULL,
  position_y DOUBLE PRECISION NOT NULL,
  position_z DOUBLE PRECISION NOT NULL,
  rotation_qx DOUBLE PRECISION NOT NULL, -- Quaternion (x, y, z, w)
  rotation_qy DOUBLE PRECISION NOT NULL,
  rotation_qz DOUBLE PRECISION NOT NULL,
  rotation_qw DOUBLE PRECISION NOT NULL,
  dimensions_x DOUBLE PRECISION NOT NULL, -- Dimensions3D
  dimensions_y DOUBLE PRECISION NOT NULL,
  dimensions_z DOUBLE PRECISION NOT NULL,
  material JSONB,                        -- Option[Material] → JSONB
  dynamic BOOLEAN NOT NULL DEFAULT false,

  PRIMARY KEY (simulation_id, obstacle_id),
  -- 可选：业务唯一键
  UNIQUE (simulation_id, obstacle_type, position_x, position_y, position_z)
);

CREATE INDEX idx_obstacles_position ON simulation_obstacles(position_x, position_y, position_z);

CREATE TABLE simulation_sensors (
  simulation_id UUID NOT NULL REFERENCES simulations(id) ON DELETE CASCADE,
  sensor_id UUID NOT NULL DEFAULT gen_random_uuid(),  -- 对应 SensorId

  -- Sensor 字段
  sensor_type TEXT NOT NULL,             -- e.g., 'lidar', 'camera', 'imu'
  position_x DOUBLE PRECISION NOT NULL,
  position_y DOUBLE PRECISION NOT NULL,
  position_z DOUBLE PRECISION NOT NULL,
  orientation_qx DOUBLE PRECISION NOT NULL, -- Quaternion
  orientation_qy DOUBLE PRECISION NOT NULL,
  orientation_qz DOUBLE PRECISION NOT NULL,
  orientation_qw DOUBLE PRECISION NOT NULL,
  config JSONB NOT NULL,                 -- SensorConfig → 复杂配置用 JSONB

  PRIMARY KEY (simulation_id, sensor_id),
  UNIQUE (simulation_id, sensor_type)    -- 一个仿真中同类型传感器通常唯一
);

CREATE INDEX idx_sensors_type ON simulation_sensors(sensor_type);

-- ============================================
-- 6. 会话表
-- ============================================

CREATE TABLE IF NOT EXISTS sessions (
  id UUID PRIMARY KEY,
  simulation_id UUID NOT NULL REFERENCES simulations(id) ON DELETE CASCADE,
  project_id UUID NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
  user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,

  -- 配置快照 (引用或嵌入)
  config_snapshot JSONB NOT NULL,
  config_snapshot_version VARCHAR(10) NOT NULL,

  -- 状态
  status VARCHAR(20) NOT NULL DEFAULT 'pending',

  -- 资源分配 (运行时填充)
  isaac_sim_instance_id UUID,
  nucleus_path TEXT,
  stream_host VARCHAR(255),
  stream_port INT,
  stream_protocol VARCHAR(20),
  control_ws_url TEXT,

  -- 最新指标快照（可选，用于快速概览）
  latest_fps DOUBLE PRECISION,
  latest_simulation_time DOUBLE PRECISION,
  latest_robot_position JSONB,  -- {x,y,z}
  latest_gpu_utilization DOUBLE PRECISION,
  metrics_last_updated_at TIMESTAMPTZ,

  -- 错误信息
  error_message TEXT,
  error_code VARCHAR(50),

  -- 时间戳
  created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
  started_at TIMESTAMPTZ,
  completed_at TIMESTAMPTZ,

  -- 约束
  CONSTRAINT sessions_status_check CHECK (
    status IN ('pending', 'initializing', 'running', 'paused', 'completed', 'failed')
  ),
  CONSTRAINT sessions_time_order CHECK (
    (started_at IS NULL OR started_at >= created_at) AND
    (completed_at IS NULL OR completed_at >= created_at)
  )
);

-- 索引
CREATE INDEX idx_sessions_simulation_id ON sessions(simulation_id);
CREATE INDEX idx_sessions_project_id ON sessions(project_id);
CREATE INDEX idx_sessions_user_id ON sessions(user_id);
CREATE INDEX idx_sessions_status ON sessions(status);
CREATE INDEX idx_sessions_created_at ON sessions(created_at DESC);
CREATE INDEX idx_sessions_completed_at ON sessions(completed_at DESC NULLS LAST);

-- 复合索引 (常见查询优化)
CREATE INDEX idx_sessions_sim_status ON sessions(simulation_id, status);
CREATE INDEX idx_sessions_user_status ON sessions(user_id, status);

-- ============================================
-- 7. 会话结果表
-- ============================================

CREATE TABLE IF NOT EXISTS session_results (
  session_id UUID PRIMARY KEY REFERENCES sessions(id) ON DELETE CASCADE,

  -- 基本结果
  success BOOLEAN NOT NULL,
  completion_time DOUBLE PRECISION NOT NULL DEFAULT 0,

  -- 任务指标
  goal_reached BOOLEAN NOT NULL DEFAULT FALSE,
  path_length DOUBLE PRECISION NOT NULL DEFAULT 0,
  collisions INT NOT NULL DEFAULT 0,
  energy_consumption DOUBLE PRECISION NOT NULL DEFAULT 0,

  -- 扩展指标 (JSON，灵活扩展)
  custom_metrics JSONB DEFAULT '{}'::JSONB,

  -- 数据存储 URL
  trajectory_url TEXT,
  video_url TEXT,

  -- 训练结果 (如果有)
  model_path TEXT,
  checkpoint_path TEXT,
  final_reward DOUBLE PRECISION,
  final_metrics JSONB,

  created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

  CONSTRAINT session_results_positive_metrics CHECK (
    path_length >= 0 AND
    collisions >= 0 AND
    energy_consumption >= 0
  )
);

-- 索引
CREATE INDEX idx_session_results_success ON session_results(success);
CREATE INDEX idx_session_results_completion_time ON session_results(completion_time);
CREATE INDEX idx_session_results_created_at ON session_results(created_at DESC);

-- 创建新表存储用户标注
CREATE TABLE session_annotations (
  id UUID PRIMARY KEY,
  session_id UUID NOT NULL REFERENCES sessions(id) ON DELETE CASCADE,
  user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,

  annotation_type VARCHAR(50) NOT NULL, -- 'best', 'favorite', 'important' 等
  notes TEXT,

  created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

  CONSTRAINT unique_user_session_annotation
    UNIQUE(session_id, user_id, annotation_type)
);

-- ============================================
-- 8. 训练任务表
-- ============================================

CREATE TABLE IF NOT EXISTS training_jobs (
  id UUID PRIMARY KEY,
  session_id UUID NOT NULL REFERENCES sessions(id) ON DELETE CASCADE,
  user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,

  -- 配置
  algorithm VARCHAR(20) NOT NULL,
  config JSONB NOT NULL,

  -- 状态
  status VARCHAR(20) NOT NULL DEFAULT 'queued',

  -- 进度
  current_epoch INT NOT NULL DEFAULT 0,
  total_epochs INT NOT NULL,
  progress_percentage DOUBLE PRECISION NOT NULL DEFAULT 0,
  current_reward DOUBLE PRECISION DEFAULT 0,
  average_reward DOUBLE PRECISION DEFAULT 0,
  loss DOUBLE PRECISION DEFAULT 0,

  -- 资源分配
  training_instance_id VARCHAR(50),
  gpu_ids INT[],

  -- 结果
  model_path TEXT,
  checkpoint_path TEXT,
  final_reward DOUBLE PRECISION,
  final_metrics JSONB,

  error_message TEXT,

  -- 时间戳
  created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
  started_at TIMESTAMPTZ,
  completed_at TIMESTAMPTZ,

  CONSTRAINT training_jobs_algorithm_check CHECK (
    algorithm IN ('PPO', 'SAC', 'TD3', 'DQN')
  ),
  CONSTRAINT training_jobs_status_check CHECK (
    status IN ('queued', 'running', 'paused', 'completed', 'failed', 'cancelled')
  )
);

-- 索引
CREATE INDEX idx_training_jobs_session_id ON training_jobs(session_id);
CREATE INDEX idx_training_jobs_user_id ON training_jobs(user_id);
CREATE INDEX idx_training_jobs_status ON training_jobs(status);
CREATE INDEX idx_training_jobs_created_at ON training_jobs(created_at DESC);

-- ============================================
-- 9. 审计日志表
-- ============================================

CREATE TABLE IF NOT EXISTS audit_logs (
  id UUID PRIMARY KEY,
  user_id UUID REFERENCES users(id) ON DELETE SET NULL,   -- 允许 NULL（匿名操作）
  action VARCHAR(50) NOT NULL,                            -- 操作类型
  resource_type VARCHAR(50),                              -- 资源类型
  resource_id VARCHAR(255),                               -- 资源 ID
  details JSONB,                                          -- 详细信息（JSON）
  ip_address INET,                                        -- IP 地址
  user_agent TEXT,                                        -- User-Agent
  platform VARCHAR(20),                                   -- ios, android, web
  success BOOLEAN NOT NULL DEFAULT true,                  -- 操作是否成功
  error_message TEXT,                                     -- 错误信息
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- 索引（用于快速查询）
CREATE INDEX idx_audit_logs_user_id ON audit_logs(user_id);
CREATE INDEX idx_audit_logs_action ON audit_logs(action);
CREATE INDEX idx_audit_logs_created_at ON audit_logs(created_at DESC);
CREATE INDEX idx_audit_logs_resource ON audit_logs(resource_type, resource_id);
CREATE INDEX idx_audit_logs_success ON audit_logs(success);
CREATE INDEX idx_audit_logs_platform ON audit_logs(platform);

-- 复合索引（常见查询组合）
CREATE INDEX idx_audit_logs_user_created ON audit_logs(user_id, created_at DESC);
CREATE INDEX idx_audit_logs_action_created ON audit_logs(action, created_at DESC);

-- 分区表（可选 - 如果日志量很大）
-- CREATE TABLE audit_logs_2025_01 PARTITION OF audit_logs
-- FOR VALUES FROM ('2025-01-01') TO ('2025-02-01');t DESC);

-- ============================================
-- 10. 视图 (便捷查询)
-- ============================================

-- 项目概览视图
CREATE OR REPLACE VIEW v_project_overview AS
SELECT
  p.id,
  p.name,
  p.description,
  p.owner_id,
  u.name AS owner_name,
  COUNT(DISTINCT s.id) AS simulation_count,
  COUNT(DISTINCT sess.id) AS total_sessions,
  SUM(CASE WHEN sess.status = 'running' THEN 1 ELSE 0 END) AS active_sessions,
  p.created_at,
  p.updated_at
FROM projects p
LEFT JOIN users u ON p.owner_id = u.id
LEFT JOIN simulations s ON p.id = s.project_id
LEFT JOIN sessions sess ON p.id = sess.project_id
WHERE p.is_archived = FALSE
GROUP BY p.id, p.name, p.description, p.owner_id, u.name, p.created_at, p.updated_at;

-- 仿真统计视图
CREATE OR REPLACE VIEW v_simulation_statistics AS
SELECT
  s.id,
  s.name,
  s.project_id,
  s.total_runs,
  s.successful_runs,
  s.failed_runs,
  CASE
    WHEN s.total_runs > 0 THEN ROUND((s.successful_runs::NUMERIC / s.total_runs * 100)::NUMERIC, 2)
    ELSE 0
  END AS success_rate_percentage,
  s.avg_completion_time,
  s.last_run_at,
  COUNT(sess.id) AS pending_sessions
FROM simulations s
LEFT JOIN sessions sess ON s.id = sess.simulation_id AND sess.status IN ('pending', 'running')
GROUP BY s.id, s.name, s.project_id, s.total_runs, s.successful_runs, s.failed_runs, s.avg_completion_time, s.last_run_at;

-- 用户活跃度视图
CREATE OR REPLACE VIEW v_user_activity AS
SELECT
  u.id,
  u.name,
  u.email,
  COUNT(DISTINCT p.id) AS project_count,
  COUNT(DISTINCT s.id) AS simulation_count,
  COUNT(DISTINCT sess.id) AS total_sessions,
  SUM(CASE WHEN sess.status = 'running' THEN 1 ELSE 0 END) AS active_sessions,
  q.current_gpu_hours_this_month,
  q.max_gpu_hours_per_month,
  u.last_login_at
FROM users u
LEFT JOIN projects p ON u.id = p.owner_id
LEFT JOIN simulations s ON u.id = s.created_by
LEFT JOIN sessions sess ON u.id = sess.user_id
LEFT JOIN user_quotas q ON u.id = q.user_id
GROUP BY u.id, u.name, u.email, q.current_gpu_hours_this_month, q.max_gpu_hours_per_month, u.last_login_at;

-- ============================================
-- 11. 触发器 - 自动更新统计
-- ============================================

-- 当 session 完成时，自动更新 simulation 统计
CREATE OR REPLACE FUNCTION update_simulation_statistics()
RETURNS TRIGGER AS $$
BEGIN
  IF NEW.status = 'completed' AND OLD.status != 'completed' THEN
    UPDATE simulations
    SET
      total_runs = total_runs + 1,
      successful_runs = successful_runs + CASE
        WHEN EXISTS (
          SELECT 1 FROM session_results
          WHERE session_id = NEW.id AND success = TRUE
        ) THEN 1 ELSE 0 END,
      failed_runs = failed_runs + CASE
        WHEN EXISTS (
          SELECT 1 FROM session_results
          WHERE session_id = NEW.id AND success = FALSE
        ) THEN 1 ELSE 0 END,
      last_run_at = NEW.completed_at
    WHERE id = NEW.simulation_id;
  END IF;

  RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trigger_update_simulation_stats
  AFTER UPDATE OF status ON sessions
  FOR EACH ROW
  EXECUTE FUNCTION update_simulation_statistics();

-- ============================================
-- 12. 初始数据
-- ============================================

-- 插入默认管理员用户
INSERT INTO users (id, email, name, password_hash, role, email_verified)
VALUES
  ('550e8400-e29b-41d4-a716-446655440000', 'admin@mosia.app', 'Admin User', '$2a$12$dummy_hash', 'admin', TRUE)
ON CONFLICT (email) DO NOTHING;

-- 插入默认配额
INSERT INTO user_quotas (user_id)
SELECT id FROM users
ON CONFLICT (user_id) DO NOTHING;

-- ============================================
-- 13. 注释
-- ============================================

COMMENT ON TABLE projects IS '项目表 - 组织和管理仿真配置的容器';
COMMENT ON TABLE simulations IS '仿真配置表 - 可重复运行的场景模板';
COMMENT ON TABLE sessions IS '会话表 - 仿真的实际运行实例';
COMMENT ON TABLE session_results IS '会话结果表 - 存储仿真运行的最终结果';
COMMENT ON TABLE training_jobs IS '训练任务表 - RL 训练任务管理';

COMMENT ON COLUMN sessions.config_snapshot IS '配置快照 - 防止 simulation 被修改影响历史数据';
COMMENT ON COLUMN sessions.status IS '状态: pending, initializing, running, paused, completed, failed';
COMMENT ON COLUMN session_results.custom_metrics IS '自定义指标 - 灵活扩展的 JSON 字段';