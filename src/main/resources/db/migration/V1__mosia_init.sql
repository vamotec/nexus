CREATE TABLE users (
    id UUID PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    name VARCHAR(255) NOT NULL,
    organization VARCHAR(255),
    role VARCHAR(20) NOT NULL DEFAULT 'USER',

    -- 认证信息
    hashed_password VARCHAR(255) NOT NULL,

    -- 时间戳
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,

    -- 索引
    INDEX idx_users_email (email),
    INDEX idx_users_organization (organization)
);

-- 会话表
CREATE TABLE sessions (
  id UUID PRIMARY KEY,
  user_id UUID NOT NULL,
  project_id UUID NOT NULL,
  scene_name VARCHAR(255) NOT NULL,
  robot_type VARCHAR(50) NOT NULL,
  environment VARCHAR(50) NOT NULL,
  status VARCHAR(20) NOT NULL,

  -- 资源分配
  isaac_sim_instance_id VARCHAR(50),
  nucleus_path TEXT,
  stream_host VARCHAR(255),
  stream_port INT,
  control_ws_url TEXT,

  -- 性能指标
  fps DOUBLE PRECISION DEFAULT 0,
  frame_count BIGINT DEFAULT 0,
  gpu_utilization DOUBLE PRECISION DEFAULT 0,
  gpu_memory_mb BIGINT DEFAULT 0,

  -- 时间戳
  created_at TIMESTAMP WITH TIME ZONE NOT NULL,
  updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
  started_at TIMESTAMP WITH TIME ZONE,
  completed_at TIMESTAMP WITH TIME ZONE,

  -- 外键约束
  FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,

  -- 索引
  INDEX idx_sessions_user_id (user_id),
  INDEX idx_sessions_status (status),
  INDEX idx_sessions_created_at (created_at DESC)
);

-- 场景配置表
CREATE TABLE scene_configs (
  session_id UUID PRIMARY KEY,
  config JSONB NOT NULL,
  FOREIGN KEY (session_id) REFERENCES sessions(id) ON DELETE CASCADE
);

-- 训练任务表
CREATE TABLE training_jobs (
  id UUID PRIMARY KEY,
  session_id UUID NOT NULL,
  user_id UUID NOT NULL,
  algorithm VARCHAR(20) NOT NULL,
  config JSONB NOT NULL,
  status VARCHAR(20) NOT NULL,

  -- 进度
  current_epoch INT DEFAULT 0,
  total_epochs INT NOT NULL,
  current_reward DOUBLE PRECISION DEFAULT 0,

  -- 结果
  model_path TEXT,
  final_reward DOUBLE PRECISION,
  final_metrics JSONB,

  created_at TIMESTAMP WITH TIME ZONE NOT NULL,
  updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
  started_at TIMESTAMP WITH TIME ZONE,
  completed_at TIMESTAMP WITH TIME ZONE,

  -- 外键约束
  FOREIGN KEY (session_id) REFERENCES sessions(id) ON DELETE CASCADE,
  FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,

  -- 索引
  INDEX idx_training_jobs_session_id (session_id),
  INDEX idx_training_jobs_user_id (user_id),
  INDEX idx_training_jobs_status (status)
);

-- 用户配额表
CREATE TABLE user_quotas (
  user_id UUID PRIMARY KEY,
  max_concurrent_sessions INT NOT NULL DEFAULT 3,
  max_gpu_hours_per_month DOUBLE PRECISION NOT NULL DEFAULT 100.0,
  max_storage_gb DOUBLE PRECISION NOT NULL DEFAULT 50.0,

  -- 当前使用量
  current_active_sessions INT DEFAULT 0,
  current_gpu_hours_this_month DOUBLE PRECISION DEFAULT 0,
  current_storage_gb DOUBLE PRECISION DEFAULT 0,

  updated_at TIMESTAMP WITH TIME ZONE NOT NULL,

  -- 外键约束
  FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);