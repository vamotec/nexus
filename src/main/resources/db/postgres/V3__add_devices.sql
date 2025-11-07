-- 设备表
CREATE TABLE devices (
  id UUID PRIMARY KEY,
  user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  device_id VARCHAR(255) NOT NULL,           -- 设备唯一标识
  device_name VARCHAR(255) NOT NULL,          -- 设备名称
  platform VARCHAR(20) NOT NULL,              -- ios, android, web
  os_version VARCHAR(50) NOT NULL,            -- 操作系统版本
  app_version VARCHAR(50) NOT NULL,           -- 应用版本
  push_token TEXT,                            -- FCM/APNs token
  last_active_at TIMESTAMPTZ NOT NULL,        -- 最后活跃时间
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  
  -- 约束：一个用户的同一设备只能注册一次
  UNIQUE(user_id, device_id)
);

-- 索引
CREATE INDEX idx_devices_user_id ON devices(user_id);
CREATE INDEX idx_devices_device_id ON devices(device_id);
CREATE INDEX idx_devices_last_active ON devices(last_active_at DESC);
CREATE INDEX idx_devices_platform ON devices(platform);

-- 触发器：自动更新 updated_at
CREATE OR REPLACE FUNCTION update_devices_updated_at()
RETURNS TRIGGER AS $$
BEGIN
  NEW.updated_at = NOW();
  RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trigger_update_devices_updated_at
BEFORE UPDATE ON devices
FOR EACH ROW
EXECUTE FUNCTION update_devices_updated_at();