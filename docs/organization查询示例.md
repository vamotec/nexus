``` roomsql

-- ============================================
-- 4. 查询示例：用户的所有组织
-- ============================================

-- 获取用户当前所属的所有组织
SELECT o.id, o.name, om.role, om.joined_at
FROM organizations o
JOIN organization_members om ON o.id = om.organization_id
WHERE om.user_id = 'U1' AND om.is_active = TRUE;

-- ============================================
-- 5. 查询示例：用户在特定组织中的角色
-- ============================================

SELECT om.role
FROM organization_members om
WHERE om.user_id = 'U1' AND om.organization_id = 'O1' AND om.is_active = TRUE;

-- ============================================
-- 6. 查询示例：用户可访问的资源（权限检查）
-- ============================================
-- 获取用户 U1 可访问的所有资源（包括公共、组织、项目）
SELECT r.* FROM resources r
WHERE r.is_active = TRUE AND r.is_deleted = FALSE
  AND (
    -- 公共资源
    (r.visibility = 'public' AND r.is_published = TRUE)
    OR
    -- 组织资源（用户属于该组织）
    (r.visibility = 'organization' AND r.organization_id IN (
      SELECT organization_id FROM organization_members
      WHERE user_id = 'U1' AND is_active = TRUE
    ))
    OR
    -- 私有资源（用户属于该项目）
    (r.visibility = 'private' AND r.project_id IN (
      SELECT project_id FROM project_collaborators
      WHERE user_id = 'U1'
    ))
  );

-- ============================================
-- 7. 移除用户（从组织）的 SQL 示例
-- ============================================

-- 将用户从组织中移除（软删除：设为非活跃）
UPDATE organization_members
SET is_active = FALSE, left_at = CURRENT_TIMESTAMP
WHERE user_id = 'U1' AND organization_id = 'O1';

-- （可选）删除用户的所有项目权限（如果这是业务逻辑的一部分）
DELETE FROM project_collaborators
WHERE user_id = 'U1' AND project_id IN (
SELECT id FROM projects WHERE owner_id = 'U1' -- 或其他业务规则
);

-- ============================================
-- 10. 应用层建议：用户上下文
-- ============================================

-- 在应用层（如 Node.js / Python），建议构建用户上下文如下：
/*
currentUserContext = {
  userId: 'U1',
  activeOrganizations: [
    { id: 'O1', role: 'admin' },
    { id: 'O2', role: 'member' }
  ],
  currentOrganizationId: 'O1',  // 用户当前正在操作的组织
  currentProjectId: 'P1'        // 用户当前正在操作的项目
}
*/
```