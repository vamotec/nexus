-- ============================================
-- Mosia Nexus Complete Database Schema
-- PostgreSQL 17+
--
-- 包含所有表、索引、约束、RLS策略和视图
-- 支持本地开发和Supabase生产环境
-- ============================================

-- ============================================
-- 0. 初始化——在supabase确认打开了pg_cron和pg——trgm
-- ============================================

-- ============================================
-- 1. 用户表
-- ============================================

CREATE TABLE users (
  id UUID PRIMARY KEY,
  email VARCHAR(255) UNIQUE NOT NULL,
  name VARCHAR(255) NOT NULL,
  avatar VARCHAR(255),
  password_hash VARCHAR(255) NOT NULL,
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

-- 启用 RLS
ALTER TABLE users ENABLE ROW LEVEL SECURITY;

-- 索引
CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_created_at ON users(created_at DESC);

-- RLS 策略
CREATE POLICY "Service role has full access to users"
  ON users FOR ALL TO service_role
  USING (true) WITH CHECK (true);

COMMENT ON TABLE users IS 'RLS enabled: 用户主表';

-- ============================================
-- 2. 用户配额表
-- ============================================

CREATE TABLE user_quotas (
  user_id UUID PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,

  -- 会话配额
  max_concurrent_sessions INT NOT NULL DEFAULT 5,
  max_gpu_hours_per_month DOUBLE PRECISION NOT NULL DEFAULT 100.0,
  max_storage_gb DOUBLE PRECISION NOT NULL DEFAULT 5.0,

  -- 组织配额
  max_owned_organizations INT NOT NULL DEFAULT 2,
  max_joined_organizations INT NOT NULL DEFAULT 2147483647,
  current_owned_organizations INT NOT NULL DEFAULT 0,

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
  ),
  CONSTRAINT check_owned_organizations_limit CHECK (
    current_owned_organizations <= max_owned_organizations
  )
);

-- 启用 RLS
ALTER TABLE user_quotas ENABLE ROW LEVEL SECURITY;

-- RLS 策略
CREATE POLICY "Service role has full access to user_quotas"
  ON user_quotas FOR ALL TO service_role
  USING (true) WITH CHECK (true);

COMMENT ON TABLE user_quotas IS 'RLS enabled: 用户配额表';
COMMENT ON COLUMN user_quotas.max_owned_organizations IS '最大拥有的组织数 (免费用户: 2)';
COMMENT ON COLUMN user_quotas.max_joined_organizations IS '最大加入的组织数 (免费用户: 无限制)';
COMMENT ON COLUMN user_quotas.current_owned_organizations IS '当前拥有的组织数';

-- ============================================
-- 3. 刷新令牌表
-- ============================================

CREATE TABLE refresh_tokens (
  id BIGSERIAL PRIMARY KEY,
  token VARCHAR(255) UNIQUE NOT NULL,
  user_id UUID NOT NULL REFERENCES users(id),
  expires_at TIMESTAMP NOT NULL,
  device_info VARCHAR(500),
  is_revoked BOOLEAN DEFAULT FALSE,
  created_at TIMESTAMPTZ DEFAULT NOW()
);

-- 启用 RLS
ALTER TABLE refresh_tokens ENABLE ROW LEVEL SECURITY;

-- 索引
CREATE INDEX idx_refresh_tokens_user_id ON refresh_tokens(user_id);
CREATE INDEX idx_refresh_tokens_user_active ON refresh_tokens(user_id, expires_at)
  WHERE is_revoked = FALSE;

-- RLS 策略
CREATE POLICY "Service role has full access to refresh_tokens"
  ON refresh_tokens FOR ALL TO service_role
  USING (true) WITH CHECK (true);

COMMENT ON TABLE refresh_tokens IS 'RLS enabled: 刷新令牌表';

-- ============================================
-- 4. 设备表
-- ============================================

CREATE TABLE devices (
  id UUID PRIMARY KEY,
  user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  device_id VARCHAR(255) NOT NULL,
  device_name VARCHAR(255) NOT NULL,
  platform VARCHAR(20) NOT NULL,
  os_version VARCHAR(50) NOT NULL,
  app_version VARCHAR(50) NOT NULL,
  push_token TEXT,
  last_active_at TIMESTAMPTZ NOT NULL,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

  UNIQUE(user_id, device_id)
);

-- 启用 RLS
ALTER TABLE devices ENABLE ROW LEVEL SECURITY;

-- 索引
CREATE INDEX idx_devices_user_id ON devices(user_id);
CREATE INDEX idx_devices_device_id ON devices(device_id);
CREATE INDEX idx_devices_last_active ON devices(last_active_at DESC);
CREATE INDEX idx_devices_platform ON devices(platform);

-- RLS 策略
CREATE POLICY "Service role has full access to devices"
  ON devices FOR ALL TO service_role
  USING (true) WITH CHECK (true);

COMMENT ON TABLE devices IS 'RLS enabled: 用户设备表';

-- ============================================
-- 5. WebAuthn 认证器表
-- ============================================

CREATE TABLE authenticators (
  id UUID PRIMARY KEY,
  user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  device_id TEXT NOT NULL,
  key_id TEXT NOT NULL,
  public_key BYTEA NOT NULL,
  sign_count BIGINT DEFAULT 0,
  created_at TIMESTAMPTZ DEFAULT NOW(),
  last_used_at TIMESTAMPTZ
);

-- 启用 RLS
ALTER TABLE authenticators ENABLE ROW LEVEL SECURITY;

-- 索引
CREATE INDEX idx_authenticators_user_id ON authenticators(user_id);
CREATE INDEX idx_authenticators_key_id ON authenticators(key_id);

-- RLS 策略
CREATE POLICY "Service role has full access to authenticators"
  ON authenticators FOR ALL TO service_role
  USING (true) WITH CHECK (true);

COMMENT ON TABLE authenticators IS 'RLS enabled: Web Auth 认证器表';

-- ============================================
-- 6. 认证挑战表
-- ============================================

CREATE TABLE auth_challenges (
  id UUID PRIMARY KEY,
  user_id UUID,
  device_id VARCHAR(255),
  challenge TEXT NOT NULL,
  purpose TEXT NOT NULL,
  expires_at TIMESTAMPTZ NOT NULL,
  consumed BOOLEAN DEFAULT FALSE,
  created_at TIMESTAMPTZ DEFAULT NOW()
);

-- 启用 RLS
ALTER TABLE auth_challenges ENABLE ROW LEVEL SECURITY;

-- 索引
CREATE INDEX idx_auth_challenges_user_id ON auth_challenges(user_id);
CREATE INDEX idx_auth_challenges_expires_at ON auth_challenges(expires_at);
CREATE INDEX idx_auth_challenges_consumed ON auth_challenges(consumed, expires_at);

-- RLS 策略
CREATE POLICY "Service role has full access to auth_challenges"
  ON auth_challenges FOR ALL TO service_role
  USING (true) WITH CHECK (true);

COMMENT ON TABLE auth_challenges IS 'RLS enabled: 认证挑战表';

-- ============================================
-- 7. 组织表
-- ============================================

CREATE TABLE organizations (
  id UUID PRIMARY KEY,
  name VARCHAR(255) NOT NULL,
  description TEXT,
  avatar VARCHAR(255),

  -- 计划与配额
  plan_type VARCHAR(50) NOT NULL DEFAULT 'free',
  max_users INT NOT NULL DEFAULT 5,
  max_storage_gb DOUBLE PRECISION NOT NULL DEFAULT 100.0,
  max_gpu_hours_per_month DOUBLE PRECISION NOT NULL DEFAULT 500.0,

  -- 状态
  is_active BOOLEAN NOT NULL DEFAULT TRUE,
  is_deleted BOOLEAN NOT NULL DEFAULT FALSE,

  -- 时间戳
  created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
  deleted_at TIMESTAMPTZ
);

-- 启用 RLS
ALTER TABLE organizations ENABLE ROW LEVEL SECURITY;

-- 索引
CREATE INDEX idx_organizations_name ON organizations(name);
CREATE INDEX idx_organizations_plan_type ON organizations(plan_type);
CREATE INDEX idx_organizations_is_active ON organizations(is_active) WHERE is_active = TRUE;
CREATE INDEX idx_organizations_created_at ON organizations(created_at DESC);

-- RLS 策略
CREATE POLICY "Service role has full access to organizations"
  ON organizations FOR ALL TO service_role
  USING (true) WITH CHECK (true);

COMMENT ON TABLE organizations IS 'RLS enabled: 组织表';

-- ============================================
-- 8. 组织成员表
-- ============================================

CREATE TABLE organization_members (
  organization_id UUID NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
  user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  role VARCHAR(50) NOT NULL DEFAULT 'member',

  -- 状态与权限
  is_active BOOLEAN NOT NULL DEFAULT TRUE,
  is_invited BOOLEAN NOT NULL DEFAULT FALSE,
  joined_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
  left_at TIMESTAMPTZ,

  -- 邀请者
  invited_by UUID REFERENCES users(id),
  invited_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,

  PRIMARY KEY (organization_id, user_id),
  CONSTRAINT org_member_role_check CHECK (role IN ('owner', 'admin', 'member')),
  CONSTRAINT chk_active_left_at CHECK (
    (is_active = TRUE AND left_at IS NULL) OR
    (is_active = FALSE AND left_at IS NOT NULL)
  )
);

-- 启用 RLS
ALTER TABLE organization_members ENABLE ROW LEVEL SECURITY;

-- 索引
CREATE INDEX idx_org_members_user_id ON organization_members(user_id);
CREATE INDEX idx_org_members_role ON organization_members(role);
CREATE INDEX idx_org_members_is_active ON organization_members(is_active) WHERE is_active = TRUE;
CREATE INDEX idx_org_members_is_invited ON organization_members(is_invited) WHERE is_invited = TRUE;

-- RLS 策略
CREATE POLICY "Service role has full access to organization_members"
  ON organization_members FOR ALL TO service_role
  USING (true) WITH CHECK (true);

COMMENT ON TABLE organization_members IS 'RLS enabled: 组织成员表';

-- ============================================
-- 9. 项目表
-- ============================================

CREATE TABLE projects (
  id UUID PRIMARY KEY,
  name VARCHAR(255) NOT NULL,
  description TEXT,
  owner_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,

  -- 设置
  settings JSONB NOT NULL DEFAULT '{
    "default_environment": "warehouse",
    "auto_save_results": true,
    "retention_days": 30
  }'::JSONB,

  -- 元数据
  tags TEXT[] NOT NULL DEFAULT '{}',
  is_archived BOOLEAN NOT NULL DEFAULT FALSE,

  -- 时间戳
  created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

  -- 约束
  CONSTRAINT projects_name_not_empty CHECK (LENGTH(TRIM(name)) > 0),
  CONSTRAINT projects_owner_name_unique UNIQUE (owner_id, name),
  CONSTRAINT projects_name_valid CHECK (
    name ~ '^[a-zA-Z0-9_-]+$' AND
    LENGTH(name) > 0 AND
    LENGTH(name) <= 100
  )
);

-- 启用 RLS
ALTER TABLE projects ENABLE ROW LEVEL SECURITY;

-- 索引
CREATE INDEX idx_projects_owner_id ON projects(owner_id);
CREATE INDEX idx_projects_created_at ON projects(created_at DESC);
CREATE INDEX idx_projects_tags ON projects USING GIN(tags);
CREATE INDEX idx_projects_is_archived ON projects(is_archived) WHERE is_archived = FALSE;
CREATE INDEX idx_projects_name_trgm ON projects USING GIN(name gin_trgm_ops);
CREATE INDEX idx_projects_description_trgm ON projects USING GIN(description gin_trgm_ops);

-- RLS 策略
CREATE POLICY "Service role has full access to projects"
  ON projects FOR ALL TO service_role
  USING (true) WITH CHECK (true);

COMMENT ON TABLE projects IS 'RLS enabled: 项目表';

-- ============================================
-- 10. 项目协作者表
-- ============================================

CREATE TABLE project_collaborators (
  project_id UUID NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
  user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  permission VARCHAR(20) NOT NULL DEFAULT 'viewer',
  added_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
  added_by UUID REFERENCES users(id),

  PRIMARY KEY (project_id, user_id),
  CONSTRAINT collab_permission_check CHECK (permission IN ('admin', 'editor', 'viewer'))
);

-- 启用 RLS
ALTER TABLE project_collaborators ENABLE ROW LEVEL SECURITY;

-- 索引
CREATE INDEX idx_collaborators_user_id ON project_collaborators(user_id);

-- RLS 策略
CREATE POLICY "Service role has full access to project_collaborators"
  ON project_collaborators FOR ALL TO service_role
  USING (true) WITH CHECK (true);

COMMENT ON TABLE project_collaborators IS 'RLS enabled: 项目协作者表';

-- ============================================
-- 11. 仿真表
-- ============================================

CREATE TABLE simulations (
  id UUID PRIMARY KEY,
  project_id UUID NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
  name VARCHAR(255) NOT NULL,
  description TEXT,

  -- 版本控制
  version_major INT NOT NULL DEFAULT 1,
  version_minor INT NOT NULL DEFAULT 0,

  -- SceneConfig 核心字段
  scene_name TEXT NOT NULL,
  robot_type TEXT NOT NULL,
  robot_urdf TEXT NOT NULL,
  environment JSONB NOT NULL DEFAULT '{}',

  -- 起始位置
  start_pos_x DOUBLE PRECISION NOT NULL,
  start_pos_y DOUBLE PRECISION NOT NULL,
  start_pos_z DOUBLE PRECISION NOT NULL,

  -- 目标位置
  goal_pos_x DOUBLE PRECISION,
  goal_pos_y DOUBLE PRECISION,
  goal_pos_z DOUBLE PRECISION,

  -- 仿真参数
  simulation_params JSONB NOT NULL DEFAULT '{
    "physics_engine": "physx",
    "time_step": 0.016,
    "max_duration": 300,
    "real_time": true,
    "record_video": false,
    "record_trajectory": false
  }'::JSONB,

  -- 训练配置
  training_config JSONB,

  -- 统计信息
  total_runs INT NOT NULL DEFAULT 0,
  successful_runs INT NOT NULL DEFAULT 0,
  failed_runs INT NOT NULL DEFAULT 0,
  avg_completion_time DOUBLE PRECISION NOT NULL DEFAULT 0.0,
  avg_collisions DOUBLE PRECISION NOT NULL DEFAULT 0.0,
  best_session_id UUID,
  last_run_at TIMESTAMPTZ,

  -- 元数据
  tags TEXT[] NOT NULL DEFAULT '{}',
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

-- 启用 RLS
ALTER TABLE simulations ENABLE ROW LEVEL SECURITY;

-- 索引
CREATE INDEX idx_simulations_project_id ON simulations(project_id);
CREATE INDEX idx_simulations_created_by ON simulations(created_by);
CREATE INDEX idx_simulations_created_at ON simulations(created_at DESC);
CREATE INDEX idx_simulations_tags ON simulations USING GIN(tags);
CREATE INDEX idx_simulations_is_archived ON simulations(is_archived) WHERE is_archived = FALSE;
CREATE INDEX idx_simulations_last_run_at ON simulations(last_run_at DESC NULLS LAST);
CREATE INDEX idx_simulations_name_trgm ON simulations USING GIN(name gin_trgm_ops);

-- RLS 策略
CREATE POLICY "Service role has full access to simulations"
  ON simulations FOR ALL TO service_role
  USING (true) WITH CHECK (true);

COMMENT ON TABLE simulations IS 'RLS enabled: 仿真配置表';

-- ============================================
-- 12. 仿真障碍物表
-- ============================================

CREATE TABLE simulation_obstacles (
  simulation_id UUID NOT NULL REFERENCES simulations(id) ON DELETE CASCADE,
  obstacle_id UUID NOT NULL DEFAULT gen_random_uuid(),
  obstacle_type TEXT NOT NULL,
  position_x DOUBLE PRECISION NOT NULL,
  position_y DOUBLE PRECISION NOT NULL,
  position_z DOUBLE PRECISION NOT NULL,
  rotation_qx DOUBLE PRECISION NOT NULL,
  rotation_qy DOUBLE PRECISION NOT NULL,
  rotation_qz DOUBLE PRECISION NOT NULL,
  rotation_qw DOUBLE PRECISION NOT NULL,
  dimensions_x DOUBLE PRECISION NOT NULL,
  dimensions_y DOUBLE PRECISION NOT NULL,
  dimensions_z DOUBLE PRECISION NOT NULL,
  material JSONB,
  dynamic BOOLEAN NOT NULL DEFAULT false,

  PRIMARY KEY (simulation_id, obstacle_id),
  UNIQUE (simulation_id, obstacle_type, position_x, position_y, position_z)
);

-- 启用 RLS
ALTER TABLE simulation_obstacles ENABLE ROW LEVEL SECURITY;

-- 索引
CREATE INDEX idx_obstacles_position ON simulation_obstacles(position_x, position_y, position_z);

-- RLS 策略
CREATE POLICY "Service role has full access to simulation_obstacles"
  ON simulation_obstacles FOR ALL TO service_role
  USING (true) WITH CHECK (true);

COMMENT ON TABLE simulation_obstacles IS 'RLS enabled: 仿真障碍物表';

-- ============================================
-- 13. 仿真传感器表
-- ============================================

CREATE TABLE simulation_sensors (
  simulation_id UUID NOT NULL REFERENCES simulations(id) ON DELETE CASCADE,
  sensor_id UUID NOT NULL DEFAULT gen_random_uuid(),
  sensor_type TEXT NOT NULL,
  position_x DOUBLE PRECISION NOT NULL,
  position_y DOUBLE PRECISION NOT NULL,
  position_z DOUBLE PRECISION NOT NULL,
  orientation_qx DOUBLE PRECISION NOT NULL,
  orientation_qy DOUBLE PRECISION NOT NULL,
  orientation_qz DOUBLE PRECISION NOT NULL,
  orientation_qw DOUBLE PRECISION NOT NULL,
  config JSONB NOT NULL,

  PRIMARY KEY (simulation_id, sensor_id),
  UNIQUE (simulation_id, sensor_type)
);

-- 启用 RLS
ALTER TABLE simulation_sensors ENABLE ROW LEVEL SECURITY;

-- 索引
CREATE INDEX idx_sensors_type ON simulation_sensors(sensor_type);

-- RLS 策略
CREATE POLICY "Service role has full access to simulation_sensors"
  ON simulation_sensors FOR ALL TO service_role
  USING (true) WITH CHECK (true);

COMMENT ON TABLE simulation_sensors IS 'RLS enabled: 仿真传感器表';

-- ============================================
-- 14. 会话表
-- ============================================

CREATE TABLE sessions (
  id UUID PRIMARY KEY,
  simulation_id UUID NOT NULL REFERENCES simulations(id) ON DELETE CASCADE,
  project_id UUID NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
  user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,

  -- 配置快照
  config_snapshot JSONB NOT NULL,
  config_snapshot_version VARCHAR(10) NOT NULL,

  -- 会话模式
  mode VARCHAR(20) NOT NULL DEFAULT 'manual',

  -- 状态
  status VARCHAR(20) NOT NULL DEFAULT 'pending',

  -- 集群ID
  cluster_id VARCHAR(36) NOT NULL,

  -- 资源分配
  isaac_sim_instance_id UUID,
  nucleus_path TEXT,
  stream_host VARCHAR(255),
  stream_port INT,
  stream_protocol VARCHAR(20),
  control_ws_url TEXT,

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
  CONSTRAINT sessions_mode_check CHECK (mode IN ('manual', 'training', 'hybrid')),
  CONSTRAINT sessions_time_order CHECK (
    (started_at IS NULL OR started_at >= created_at) AND
    (completed_at IS NULL OR completed_at >= created_at)
  )
);

-- 启用 RLS
ALTER TABLE sessions ENABLE ROW LEVEL SECURITY;

-- 索引
CREATE INDEX idx_sessions_simulation_id ON sessions(simulation_id);
CREATE INDEX idx_sessions_project_id ON sessions(project_id);
CREATE INDEX idx_sessions_user_id ON sessions(user_id);
CREATE INDEX idx_sessions_status ON sessions(status);
CREATE INDEX idx_sessions_mode ON sessions(mode);
CREATE INDEX idx_sessions_created_at ON sessions(created_at DESC);
CREATE INDEX idx_sessions_completed_at ON sessions(completed_at DESC NULLS LAST);
CREATE INDEX idx_sessions_sim_status ON sessions(simulation_id, status);
CREATE INDEX idx_sessions_user_status ON sessions(user_id, status);
CREATE INDEX idx_sessions_project_mode ON sessions(project_id, mode);
CREATE INDEX idx_sessions_user_mode_status ON sessions(user_id, mode, status);

-- RLS 策略
CREATE POLICY "Service role has full access to sessions"
  ON sessions FOR ALL TO service_role
  USING (true) WITH CHECK (true);

COMMENT ON TABLE sessions IS 'RLS enabled: 会话表';
COMMENT ON COLUMN sessions.config_snapshot IS '配置快照 - 防止 simulation 被修改影响历史数据';
COMMENT ON COLUMN sessions.status IS '状态: pending, initializing, running, paused, completed, failed';
COMMENT ON COLUMN sessions.mode IS '会话模式: manual（手动控制）, training（AI训练）, hybrid（混合模式）';

-- ============================================
-- 15. 会话结果表
-- ============================================

CREATE TABLE session_results (
  session_id UUID PRIMARY KEY REFERENCES sessions(id) ON DELETE CASCADE,

  -- 基本结果
  success BOOLEAN NOT NULL,
  completion_time DOUBLE PRECISION NOT NULL DEFAULT 0,

  -- 任务指标
  goal_reached BOOLEAN NOT NULL DEFAULT FALSE,
  path_length DOUBLE PRECISION NOT NULL DEFAULT 0,
  collisions INT NOT NULL DEFAULT 0,
  energy_consumption DOUBLE PRECISION NOT NULL DEFAULT 0,

  -- 扩展指标
  custom_metrics JSONB DEFAULT '{}'::JSONB,

  -- 数据存储
  trajectory_url TEXT,
  video_url TEXT,

  -- 训练结果
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

-- 启用 RLS
ALTER TABLE session_results ENABLE ROW LEVEL SECURITY;

-- 索引
CREATE INDEX idx_session_results_success ON session_results(success);
CREATE INDEX idx_session_results_completion_time ON session_results(completion_time);
CREATE INDEX idx_session_results_created_at ON session_results(created_at DESC);

-- RLS 策略
CREATE POLICY "Service role has full access to session_results"
  ON session_results FOR ALL TO service_role
  USING (true) WITH CHECK (true);

COMMENT ON TABLE session_results IS 'RLS enabled: 会话结果表';
COMMENT ON COLUMN session_results.custom_metrics IS '自定义指标 - 灵活扩展的 JSON 字段';

-- ============================================
-- 16. 会话标注表
-- ============================================

CREATE TABLE session_annotations (
  id UUID PRIMARY KEY,
  session_id UUID NOT NULL REFERENCES sessions(id) ON DELETE CASCADE,
  user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  annotation_type VARCHAR(50) NOT NULL,
  notes TEXT,
  created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

  CONSTRAINT unique_user_session_annotation UNIQUE(session_id, user_id, annotation_type)
);

-- 启用 RLS
ALTER TABLE session_annotations ENABLE ROW LEVEL SECURITY;

-- RLS 策略
CREATE POLICY "Service role has full access to session_annotations"
  ON session_annotations FOR ALL TO service_role
  USING (true) WITH CHECK (true);

COMMENT ON TABLE session_annotations IS 'RLS enabled: 会话标注表';

-- ============================================
-- 17. 训练任务表
-- ============================================

CREATE TABLE training_jobs (
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
  training_instance_id UUID,
  gpu_ids INT[] NOT NULL DEFAULT '{}',

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

-- 启用 RLS
ALTER TABLE training_jobs ENABLE ROW LEVEL SECURITY;

-- 索引
CREATE INDEX idx_training_jobs_session_id ON training_jobs(session_id);
CREATE INDEX idx_training_jobs_user_id ON training_jobs(user_id);
CREATE INDEX idx_training_jobs_status ON training_jobs(status);
CREATE INDEX idx_training_jobs_created_at ON training_jobs(created_at DESC);

-- RLS 策略
CREATE POLICY "Service role has full access to training_jobs"
  ON training_jobs FOR ALL TO service_role
  USING (true) WITH CHECK (true);

COMMENT ON TABLE training_jobs IS 'RLS enabled: 训练任务表';

-- ============================================
-- 18. 资源表
-- ============================================

CREATE TYPE resource_visibility AS ENUM ('private', 'organization', 'public');

CREATE TABLE resources (
  id UUID PRIMARY KEY,
  name VARCHAR(255) NOT NULL,
  description TEXT,
  type VARCHAR(50) NOT NULL,
  visibility resource_visibility NOT NULL DEFAULT 'private',
  organization_id UUID NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
  project_id UUID REFERENCES projects(id) ON DELETE CASCADE,
  created_by UUID NOT NULL REFERENCES users(id),

  -- 元数据
  metadata JSONB NOT NULL DEFAULT '{}',
  tags TEXT[] DEFAULT '{}',

  -- 公共资源相关
  is_published BOOLEAN NOT NULL DEFAULT FALSE,
  license_type VARCHAR(50) DEFAULT 'proprietary',
  download_count INT NOT NULL DEFAULT 0,
  rating_avg FLOAT DEFAULT 0.0,
  is_featured BOOLEAN NOT NULL DEFAULT FALSE,

  -- 状态
  is_active BOOLEAN NOT NULL DEFAULT TRUE,
  is_deleted BOOLEAN NOT NULL DEFAULT FALSE,

  -- 时间戳
  created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
  published_at TIMESTAMPTZ,
  deleted_at TIMESTAMPTZ,

  -- 约束
  CONSTRAINT chk_visibility_project CHECK (
    (visibility = 'private' AND project_id IS NOT NULL) OR
    (visibility IN ('organization', 'public') AND project_id IS NULL)
  ),
  CONSTRAINT resources_name_not_empty CHECK (LENGTH(TRIM(name)) > 0)
);

-- 启用 RLS
ALTER TABLE resources ENABLE ROW LEVEL SECURITY;

-- 索引
CREATE INDEX idx_resources_organization_id ON resources(organization_id);
CREATE INDEX idx_resources_project_id ON resources(project_id);
CREATE INDEX idx_resources_type ON resources(type);
CREATE INDEX idx_resources_visibility ON resources(visibility);
CREATE INDEX idx_resources_is_published ON resources(is_published) WHERE is_published = TRUE;
CREATE INDEX idx_resources_tags ON resources USING GIN(tags);
CREATE INDEX idx_resources_created_at ON resources(created_at DESC);
CREATE INDEX idx_resources_is_active ON resources(is_active) WHERE is_active = TRUE;
CREATE INDEX idx_resources_name_trgm ON resources USING GIN(name gin_trgm_ops);
CREATE INDEX idx_resources_description_trgm ON resources USING GIN(description gin_trgm_ops);

-- RLS 策略
CREATE POLICY "Service role has full access to resources"
  ON resources FOR ALL TO service_role
  USING (true) WITH CHECK (true);

COMMENT ON TABLE resources IS 'RLS enabled: 资源表';

-- ============================================
-- 19. 项目资源引用表
-- ============================================

CREATE TABLE project_resource_references (
  project_id UUID NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
  resource_id UUID NOT NULL REFERENCES resources(id) ON DELETE CASCADE,
  reference_type VARCHAR(20) NOT NULL DEFAULT 'imported',
  created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
  created_by UUID NOT NULL REFERENCES users(id),

  PRIMARY KEY (project_id, resource_id)
);

-- 启用 RLS
ALTER TABLE project_resource_references ENABLE ROW LEVEL SECURITY;

-- 索引
CREATE INDEX idx_proj_resource_refs_resource_id ON project_resource_references(resource_id);

-- RLS 策略
CREATE POLICY "Service role has full access to project_resource_references"
  ON project_resource_references FOR ALL TO service_role
  USING (true) WITH CHECK (true);

COMMENT ON TABLE project_resource_references IS 'RLS enabled: 项目资源引用表';

-- ============================================
-- 20. 审计日志表
-- ============================================

CREATE TABLE audit_logs (
  id UUID PRIMARY KEY,
  user_id UUID REFERENCES users(id) ON DELETE SET NULL,
  action VARCHAR(50) NOT NULL,
  resource_type VARCHAR(50),
  resource_id VARCHAR(255),
  details JSONB,
  ip_address INET,
  user_agent TEXT,
  platform VARCHAR(20),
  success BOOLEAN NOT NULL DEFAULT true,
  error_message TEXT,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- 启用 RLS
ALTER TABLE audit_logs ENABLE ROW LEVEL SECURITY;

-- 索引
CREATE INDEX idx_audit_logs_user_id ON audit_logs(user_id);
CREATE INDEX idx_audit_logs_action ON audit_logs(action);
CREATE INDEX idx_audit_logs_created_at ON audit_logs(created_at DESC);
CREATE INDEX idx_audit_logs_resource ON audit_logs(resource_type, resource_id);
CREATE INDEX idx_audit_logs_success ON audit_logs(success);
CREATE INDEX idx_audit_logs_platform ON audit_logs(platform);
CREATE INDEX idx_audit_logs_user_created ON audit_logs(user_id, created_at DESC);
CREATE INDEX idx_audit_logs_action_created ON audit_logs(action, created_at DESC);

-- RLS 策略
CREATE POLICY "Service role has full access to audit_logs"
  ON audit_logs FOR ALL TO service_role
  USING (true) WITH CHECK (true);

COMMENT ON TABLE audit_logs IS 'RLS enabled: 审计日志表（包含分区表）';

-- ============================================
-- 21. 触发器函数
-- ============================================

-- 更新时间触发器函数
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
  NEW.updated_at = CURRENT_TIMESTAMP;
  RETURN NEW;
END;
$$ LANGUAGE plpgsql
SET search_path = pg_catalog, public;
-- 设备更新时间触发器函数
CREATE OR REPLACE FUNCTION update_devices_updated_at()
RETURNS TRIGGER AS $$
BEGIN
  NEW.updated_at = NOW();
  RETURN NEW;
END;
$$ LANGUAGE plpgsql
SET search_path = pg_catalog, public;
-- 会话统计更新函数
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
$$ LANGUAGE plpgsql
SET search_path = pg_catalog, public;
-- ============================================
-- 22. 触发器
-- ============================================

-- 用户表更新时间触发器
CREATE TRIGGER users_updated_at
  BEFORE UPDATE ON users
  FOR EACH ROW
  EXECUTE FUNCTION update_updated_at_column();

-- 项目表更新时间触发器
CREATE TRIGGER projects_updated_at
  BEFORE UPDATE ON projects
  FOR EACH ROW
  EXECUTE FUNCTION update_updated_at_column();

-- 仿真表更新时间触发器
CREATE TRIGGER simulations_updated_at
  BEFORE UPDATE ON simulations
  FOR EACH ROW
  EXECUTE FUNCTION update_updated_at_column();

-- 组织表更新时间触发器
CREATE TRIGGER organizations_updated_at
  BEFORE UPDATE ON organizations
  FOR EACH ROW
  EXECUTE FUNCTION update_updated_at_column();

-- 资源表更新时间触发器
CREATE TRIGGER resources_updated_at
  BEFORE UPDATE ON resources
  FOR EACH ROW
  EXECUTE FUNCTION update_updated_at_column();

-- 设备表更新时间触发器
CREATE TRIGGER trigger_update_devices_updated_at
  BEFORE UPDATE ON devices
  FOR EACH ROW
  EXECUTE FUNCTION update_devices_updated_at();

-- 会话统计更新触发器
CREATE TRIGGER trigger_update_simulation_stats
  AFTER UPDATE OF status ON sessions
  FOR EACH ROW
  EXECUTE FUNCTION update_simulation_statistics();

-- ============================================
-- 23. 视图（使用 security_invoker）
-- ============================================

-- 项目概览视图
CREATE OR REPLACE VIEW v_project_overview
WITH (security_invoker=true) AS
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
CREATE OR REPLACE VIEW v_simulation_statistics
WITH (security_invoker=true) AS
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
CREATE OR REPLACE VIEW v_user_activity
WITH (security_invoker=true) AS
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

-- 用户项目权限上下文视图
CREATE VIEW user_project_context
WITH (security_invoker=true) AS
SELECT
  u.id AS user_id,
  o.id AS organization_id,
  om.role AS org_role,
  p.id AS project_id,
  p.name AS project_name,
  pc.permission AS project_permission,
  pc.added_at
FROM users u
JOIN organization_members om ON u.id = om.user_id AND om.is_active = TRUE
JOIN organizations o ON om.organization_id = o.id
JOIN projects p ON p.owner_id = u.id OR p.id IN (
  SELECT project_id FROM project_collaborators WHERE user_id = u.id
)
LEFT JOIN project_collaborators pc ON u.id = pc.user_id AND pc.project_id = p.id
WHERE p.is_archived = FALSE OR p.is_archived IS NULL;

COMMENT ON VIEW user_project_context IS 'Security invoker view: 用户项目权限上下文，使用查询者权限';

-- 用户资源权限上下文视图
CREATE VIEW user_resource_context
WITH (security_invoker=true) AS
SELECT
  u.id AS user_id,
  r.id AS resource_id,
  r.organization_id,
  r.project_id,
  r.visibility,
  om.role AS org_role,
  pc.permission AS project_permission
FROM users u
JOIN resources r ON (
  (r.visibility = 'public' AND r.is_published = TRUE) OR
  (r.visibility = 'organization' AND u.id IN (
    SELECT user_id FROM organization_members WHERE organization_id = r.organization_id
  )) OR
  (r.visibility = 'private' AND u.id IN (
    SELECT user_id FROM project_collaborators WHERE project_id = r.project_id
  ))
)
LEFT JOIN organization_members om ON u.id = om.user_id AND om.organization_id = r.organization_id
LEFT JOIN project_collaborators pc ON u.id = pc.user_id AND pc.project_id = r.project_id
WHERE r.is_active = TRUE AND r.is_deleted = FALSE;

COMMENT ON VIEW user_resource_context IS 'Security invoker view: 用户资源权限上下文，使用查询者权限';

-- 用户组织上下文视图
CREATE VIEW user_organization_context
WITH (security_invoker=true) AS
SELECT
  u.id AS user_id,
  o.id AS organization_id,
  o.name AS organization_name,
  om.role AS organization_role,
  om.joined_at,
  om.is_active
FROM users u
JOIN organization_members om ON u.id = om.user_id
JOIN organizations o ON om.organization_id = o.id
WHERE om.is_active = TRUE;

COMMENT ON VIEW user_organization_context IS 'Security invoker view: 用户组织上下文，使用查询者权限';

-- ============================================
-- 24. 初始数据
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

-- 确保公共角色无法访问 Flyway 元数据表
REVOKE ALL PRIVILEGES ON flyway_schema_history FROM anon, authenticated;