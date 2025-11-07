-- 审计日志表
CREATE TABLE audit_logs (
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
-- FOR VALUES FROM ('2025-01-01') TO ('2025-02-01');