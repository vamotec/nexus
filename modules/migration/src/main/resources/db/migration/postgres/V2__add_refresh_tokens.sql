CREATE TABLE refresh_tokens (
  id BIGSERIAL PRIMARY KEY,
  token VARCHAR(255) UNIQUE NOT NULL,  -- UNIQUE 自动创建索引
  user_id UUID NOT NULL REFERENCES users(id),
  expires_at TIMESTAMP NOT NULL,
  device_info VARCHAR(500),
  is_revoked BOOLEAN DEFAULT FALSE,
  created_at TIMESTAMPTZ DEFAULT NOW()
);

-- 只需要为 user_id 创建索引
CREATE INDEX idx_refresh_tokens_user_id ON refresh_tokens(user_id);

-- 可选: 为常用查询创建复合索引
CREATE INDEX idx_refresh_tokens_user_active ON refresh_tokens(user_id, expires_at)
WHERE is_revoked = FALSE;