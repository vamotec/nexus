-- V3__create_outbox_table.sql
-- PostgreSQL Outbox Pattern 实现
-- 用于保证事务一致性的事件发布

-- =============================================================================
-- Outbox 表：存储待发布的领域事件
-- =============================================================================

CREATE TABLE IF NOT EXISTS event_outbox (
    id UUID PRIMARY KEY,

    -- 事件元数据
    event_type VARCHAR(100) NOT NULL,
    aggregate_id VARCHAR(255) NOT NULL,
    aggregate_type VARCHAR(100) NOT NULL,

    -- 事件内容
    payload JSONB NOT NULL,

    -- 发布状态
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    -- 状态枚举: PENDING, PROCESSING, PUBLISHED, FAILED

    -- 时间戳
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    processed_at TIMESTAMP WITH TIME ZONE,
    published_at TIMESTAMP WITH TIME ZONE,

    -- 重试控制
    retry_count INTEGER NOT NULL DEFAULT 0,
    max_retries INTEGER NOT NULL DEFAULT 3,
    next_retry_at TIMESTAMP WITH TIME ZONE,

    -- 错误信息
    last_error TEXT,

    -- 分区键（用于路由到不同的 Stream/Topic）
    partition_key VARCHAR(255),

    CONSTRAINT event_outbox_status_check
        CHECK (status IN ('PENDING', 'PROCESSING', 'PUBLISHED', 'FAILED'))
);

-- =============================================================================
-- RLS
-- =============================================================================

-- 启用 RLS
ALTER TABLE event_outbox ENABLE ROW LEVEL SECURITY;

-- RLS 策略
CREATE POLICY "Service role has full access to event_outbox"
  ON event_outbox FOR ALL TO service_role
  USING (true) WITH CHECK (true);

-- =============================================================================
-- 索引优化
-- =============================================================================

-- 1. 查询待处理事件（最重要的查询）
CREATE INDEX idx_outbox_pending_for_processing
    ON event_outbox(created_at)
    WHERE status = 'PENDING';

-- 2. 按状态查询
CREATE INDEX idx_outbox_status
    ON event_outbox(status, created_at);

-- 3. 按聚合查询（用于调试）
CREATE INDEX idx_outbox_aggregate
    ON event_outbox(aggregate_type, aggregate_id);

-- 4. 按事件类型查询（用于监控）
CREATE INDEX idx_outbox_event_type
    ON event_outbox(event_type, created_at);

-- 5. 清理过期事件
CREATE INDEX idx_outbox_published_at
    ON event_outbox(published_at)
    WHERE status = 'PUBLISHED';

-- =============================================================================
-- 函数：自动设置 processed_at
-- =============================================================================

CREATE OR REPLACE FUNCTION set_outbox_processed_at()
RETURNS TRIGGER AS $$
BEGIN
    IF NEW.status = 'PROCESSING' AND OLD.status != 'PROCESSING' THEN
        NEW.processed_at = NOW();
    END IF;

    IF NEW.status = 'PUBLISHED' AND OLD.status != 'PUBLISHED' THEN
        NEW.published_at = NOW();
    END IF;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trigger_outbox_processed_at
    BEFORE UPDATE ON event_outbox
    FOR EACH ROW
    EXECUTE FUNCTION set_outbox_processed_at();

-- =============================================================================
-- 注释说明
-- =============================================================================

COMMENT ON TABLE event_outbox IS
'Outbox 事件表：实现事务性事件发布模式（Transactional Outbox Pattern）';

COMMENT ON COLUMN event_outbox.event_type IS
'事件类型，如 UserCreated, SessionStarted';

COMMENT ON COLUMN event_outbox.aggregate_id IS
'聚合根 ID，用于追踪和调试';

COMMENT ON COLUMN event_outbox.aggregate_type IS
'聚合根类型，如 User, Session, TrainingJob';

COMMENT ON COLUMN event_outbox.payload IS
'事件 JSON 数据，完整的事件内容';

COMMENT ON COLUMN event_outbox.status IS
'处理状态: PENDING(待处理), PROCESSING(处理中), PUBLISHED(已发布), FAILED(失败)';

COMMENT ON COLUMN event_outbox.retry_count IS
'当前重试次数';

COMMENT ON COLUMN event_outbox.next_retry_at IS
'下次重试时间（指数退避）';

COMMENT ON COLUMN event_outbox.partition_key IS
'分区键，用于 Kafka/Redis Streams 分区路由';
