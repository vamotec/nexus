-- authenticators (device keys, one row per device key)
CREATE TABLE authenticators (
  id UUID PRIMARY KEY,
  user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  device_id TEXT NOT NULL, -- device identifier (app-provided or generated)
  key_id TEXT NOT NULL,    -- key identifier for this public key
  public_key BYTEA NOT NULL, -- raw public key bytes (e.g., DER encoded) or PEM
  sign_count BIGINT DEFAULT 0, -- monotonic counter to detect cloned keys (optional)
  created_at TIMESTAMPTZ WITH TIME ZONE DEFAULT now(),
  last_used_at TIMESTAMPTZ WITH TIME ZONE
);

-- challenges (ephemeral)
CREATE TABLE auth_challenges (
  id UUID PRIMARY KEY,
  user_id UUID NULL,         -- optional, we might challenge by user or device
  device_id TEXT NULL,
  challenge TEXT NOT NULL,
  purpose TEXT NOT NULL,     -- e.g. 'webauthn_auth' or 'webauthn_register'
  expires_at TIMESTAMPTZ WITH TIME ZONE NOT NULL,
  consumed BOOLEAN DEFAULT FALSE,
  created_at TIMESTAMPTZ WITH TIME ZONE DEFAULT now()
);