ALTER TABLE users DROP COLUMN IF EXISTS organization;
-- ============================================
-- 1. 组织表 (新增)
-- ============================================

CREATE TABLE IF NOT EXISTS organizations (
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

-- 索引
CREATE INDEX idx_organizations_name ON organizations(name);
CREATE INDEX idx_organizations_plan_type ON organizations(plan_type);
CREATE INDEX idx_organizations_is_active ON organizations(is_active) WHERE is_active = TRUE;
CREATE INDEX idx_organizations_created_at ON organizations(created_at DESC);

-- 更新时间触发器
CREATE TRIGGER organizations_updated_at
  BEFORE UPDATE ON organizations
  FOR EACH ROW
  EXECUTE FUNCTION update_updated_at_column();

-- ============================================
-- 2. 组织成员表 (新增)
-- ============================================

CREATE TABLE IF NOT EXISTS organization_members (
  organization_id UUID NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
  user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  role VARCHAR(50) NOT NULL DEFAULT 'member',

  -- 状态与权限
  is_active BOOLEAN NOT NULL DEFAULT TRUE,        -- 是否仍为成员
  is_invited BOOLEAN NOT NULL DEFAULT FALSE,      -- 是否为邀请状态
  joined_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
  left_at TIMESTAMPTZ,                            -- 被移除或主动离开的时间

  -- 邀请者
  invited_by UUID REFERENCES users(id),
  invited_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,

  -- 约束
  PRIMARY KEY (organization_id, user_id),
  CONSTRAINT org_member_role_check CHECK (role IN ('owner', 'admin', 'member')),
  CONSTRAINT chk_active_left_at CHECK (
    (is_active = TRUE AND left_at IS NULL) OR
    (is_active = FALSE AND left_at IS NOT NULL)
  )
);

-- 索引
CREATE INDEX idx_org_members_user_id ON organization_members(user_id);
CREATE INDEX idx_org_members_role ON organization_members(role);
CREATE INDEX idx_org_members_is_active ON organization_members(is_active) WHERE is_active = TRUE;
CREATE INDEX idx_org_members_is_invited ON organization_members(is_invited) WHERE is_invited = TRUE;

-- ============================================
-- 3. 资源表 (新增)
-- ============================================

CREATE TYPE resource_visibility AS ENUM ('private', 'organization', 'public');

CREATE TABLE IF NOT EXISTS resources (
  id UUID PRIMARY KEY,
  name VARCHAR(255) NOT NULL,
  description TEXT,
  type VARCHAR(50) NOT NULL,  -- 'robot_model', 'simulation', 'training_job', 'asset', etc.

  -- 可见性范围
  visibility resource_visibility NOT NULL DEFAULT 'private',

  -- 所属组织（所有资源必须属于一个组织）
  organization_id UUID NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,

  -- 仅当 visibility = 'private' 时，必须指定 project_id
  project_id UUID REFERENCES projects(id) ON DELETE CASCADE,

  -- 创建者
  created_by UUID NOT NULL REFERENCES users(id),

  -- 元数据
  metadata JSONB NOT NULL DEFAULT '{}',
  tags TEXT[] DEFAULT '{}',

  -- 公共资源相关
  is_published BOOLEAN NOT NULL DEFAULT FALSE,  -- 公共资源是否已上架
  license_type VARCHAR(50) DEFAULT 'proprietary', -- 公共资源的许可证
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

-- 索引
CREATE INDEX idx_resources_organization_id ON resources(organization_id);
CREATE INDEX idx_resources_project_id ON resources(project_id);
CREATE INDEX idx_resources_type ON resources(type);
CREATE INDEX idx_resources_visibility ON resources(visibility);
CREATE INDEX idx_resources_is_published ON resources(is_published) WHERE is_published = TRUE;
CREATE INDEX idx_resources_tags ON resources USING GIN(tags);
CREATE INDEX idx_resources_created_at ON resources(created_at DESC);
CREATE INDEX idx_resources_is_active ON resources(is_active) WHERE is_active = TRUE;

-- 全文搜索索引
CREATE INDEX idx_resources_name_trgm ON resources USING GIN(name gin_trgm_ops);
CREATE INDEX idx_resources_description_trgm ON resources USING GIN(description gin_trgm_ops);

-- 更新时间触发器
CREATE TRIGGER resources_updated_at
  BEFORE UPDATE ON resources
  FOR EACH ROW
  EXECUTE FUNCTION update_updated_at_column();

-- ============================================
-- 4. 项目资源关联表 (可选：用于项目引用组织/公共资产)
-- ============================================

-- 如果你需要显式记录“项目引用了哪些组织/公共资源”（用于权限/审计），可创建此表
CREATE TABLE IF NOT EXISTS project_resource_references (
  project_id UUID NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
  resource_id UUID NOT NULL REFERENCES resources(id) ON DELETE CASCADE,
  reference_type VARCHAR(20) NOT NULL DEFAULT 'imported',  -- 'imported', 'linked', 'template'

  created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
  created_by UUID NOT NULL REFERENCES users(id),

  PRIMARY KEY (project_id, resource_id)
);

-- 索引
CREATE INDEX idx_proj_resource_refs_resource_id ON project_resource_references(resource_id);

-- ============================================
-- 5. 视图：用户项目权限上下文 (可选：便于查询)
-- ============================================

CREATE OR REPLACE VIEW user_project_context AS
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
  -- 包含项目协作者
  SELECT project_id FROM project_collaborators WHERE user_id = u.id
)
LEFT JOIN project_collaborators pc ON u.id = pc.user_id AND pc.project_id = p.id
WHERE p.is_archived = FALSE OR p.is_archived IS NULL;

-- ============================================
-- 6. 视图：用户资源权限上下文 (可选：便于查询)
-- ============================================

CREATE OR REPLACE VIEW user_resource_context AS
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

-- ============================================
-- 7. 视图：用户组织上下文（便于查询）
-- ============================================

CREATE OR REPLACE VIEW user_organization_context AS
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