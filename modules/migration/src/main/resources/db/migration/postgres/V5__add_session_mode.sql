-- ============================================
-- V5: 添加会话模式（Manual/Training/Hybrid）
-- ============================================

-- 添加 mode 列到 sessions 表
ALTER TABLE sessions
  ADD COLUMN mode VARCHAR(20) NOT NULL DEFAULT 'manual';

-- 添加检查约束
ALTER TABLE sessions
  ADD CONSTRAINT sessions_mode_check CHECK (mode IN ('manual', 'training', 'hybrid'));

-- 创建索引以优化按模式查询
CREATE INDEX idx_sessions_mode ON sessions(mode);

-- 复合索引：项目 + 模式
CREATE INDEX idx_sessions_project_mode ON sessions(project_id, mode);

-- 复合索引：用户 + 模式 + 状态
CREATE INDEX idx_sessions_user_mode_status ON sessions(user_id, mode, status);

-- 添加注释
COMMENT ON COLUMN sessions.mode IS '会话模式: manual（手动控制）, training（AI训练）, hybrid（混合模式）';

-- 更新现有数据：将所有现有会话标记为 manual 模式
-- （如果有训练相关的会话，需要手动调整）
UPDATE sessions SET mode = 'manual' WHERE mode IS NULL;
