-- authenticators (device keys, one row per device key)
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

CREATE INDEX idx_authenticators_user_id ON authenticators(user_id);
CREATE INDEX idx_authenticators_key_id ON authenticators(key_id);

-- challenges (ephemeral)
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

CREATE INDEX idx_auth_challenges_user_id ON auth_challenges(user_id);
CREATE INDEX idx_auth_challenges_expires_at ON auth_challenges(expires_at);
CREATE INDEX idx_auth_challenges_consumed ON auth_challenges(consumed, expires_at);