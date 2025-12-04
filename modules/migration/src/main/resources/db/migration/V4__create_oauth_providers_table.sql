CREATE TABLE user_oauth_providers (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  provider VARCHAR(50) NOT NULL,
  provider_user_id VARCHAR(255) NOT NULL,
  provider_email VARCHAR(255),
  linked_at TIMESTAMP NOT NULL DEFAULT NOW(),
  last_used_at TIMESTAMP,

  CONSTRAINT uq_provider_user UNIQUE(provider, provider_user_id),
  CONSTRAINT uq_user_provider UNIQUE(user_id, provider)
);

CREATE INDEX idx_oauth_user_id ON user_oauth_providers(user_id);
CREATE INDEX idx_oauth_provider_lookup ON user_oauth_providers(provider, provider_user_id);

-- 启用 RLS
ALTER TABLE user_oauth_providers ENABLE ROW LEVEL SECURITY;

-- RLS 策略
CREATE POLICY "Service role has full access to event_outbox"
  ON user_oauth_providers FOR ALL TO service_role
  USING (true) WITH CHECK (true);