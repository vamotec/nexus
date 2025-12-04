# 组织功能实现总结

## 已完成功能

### 1. 领域模型 (Domain Models)

**位置**: `modules/nexus/src/main/scala/domain/model/organization/`

- **Organization.scala**: 组织实体
  - 支持免费版、高级版、企业版计划
  - 包含配额限制（最大用户数、存储空间、GPU 小时数）
  - 软删除支持

- **OrganizationMember.scala**: 组织成员关系
  - 角色：Owner, Admin, Member
  - 邀请机制支持
  - 成员权限验证方法

- **OrganizationRole.scala**: 组织角色枚举
  - Owner: 组织所有者
  - Admin: 管理员
  - Member: 普通成员

- **PlanType.scala**: 计划类型枚举
  - Free: 免费版（5 个成员，100GB 存储）
  - Premium: 高级版（50 个成员，1TB 存储）
  - Enterprise: 企业版（无限制）

- **OrganizationQuota.scala**: 组织配额模型
  - 根据计划类型自动设置配额
  - 成员数量和存储空间验证

### 2. 用户配额扩展 (User Quota)

**位置**: `modules/nexus/src/main/scala/domain/model/user/Quota.scala`

新增字段：
- `maxOwnedOrganizations`: 最多拥有的组织数（免费用户：2）
- `maxJoinedOrganizations`: 最多加入的组织数（免费用户：无限制）
- `currentOwnedOrganizations`: 当前拥有的组织数

新增方法：
- `canCreateOrganization`: 检查是否可以创建新组织
- `remainingOrganizations`: 剩余可创建组织数
- `hasStorageAvailable`: 检查存储空间是否充足
- `incrementOwnedOrganizations` / `decrementOwnedOrganizations`: 配额管理

### 3. 数据库迁移 (Database Migration)

**位置**: `modules/migration/src/main/resources/db/migration/postgres/`

- **V8__add_organizations_and_resources.sql** (已存在)
  - `organizations` 表
  - `organization_members` 表
  - `resources` 表
  - 相关索引和视图

- **V13__add_organization_quotas_to_users.sql** (新增)
  - 向 `user_quotas` 表添加组织相关配额字段
  - 更新免费用户存储配额为 5GB

### 4. 仓储层 (Repository)

**接口位置**: `modules/nexus/src/main/scala/domain/repository/`

- **OrganizationRepository.scala**: 组织仓储接口
  - CRUD 操作
  - 查询用户拥有/加入的组织
  - 成员数量统计
  - 名称唯一性检查

- **OrganizationMemberRepository.scala**: 成员仓储接口
  - 成员管理（添加、移除、更新）
  - 角色更新
  - 邀请管理
  - 所有者查询

**实现位置**: `modules/nexus/src/main/scala/infrastructure/persistence/postgres/repository/`

- **OrganizationRepositoryLive.scala**: 使用 Quill 实现
- **OrganizationMemberRepositoryLive.scala**: 使用 Quill 实现
- **UserRepositoryLive.scala**: 更新以支持新的配额字段

### 5. 应用服务层 (Application Service)

**位置**:
- 接口: `modules/nexus/src/main/scala/domain/services/app/OrganizationService.scala`
- 实现: `modules/nexus/src/main/scala/application/services/OrganizationServiceLive.scala`

**核心业务逻辑**:

1. **创建组织**
   - 配额检查（用户是否达到拥有组织上限）
   - 名称唯一性验证
   - 自动创建 Owner 成员关系

2. **邀请成员**
   - 权限验证（Admin/Owner 可邀请）
   - 组织成员数配额检查
   - 邀请状态管理

3. **转让所有权**
   - 当前所有者验证
   - 新所有者配额检查
   - 自动角色调整（旧 Owner → Admin，新成员 → Owner）

4. **成员管理**
   - 移除成员（权限验证）
   - 角色更新（仅 Owner 可操作）
   - 离开组织（Owner 需先转让）

5. **权限控制**
   - Owner: 所有权限（删除组织、转让、管理成员）
   - Admin: 邀请成员、更新组织信息
   - Member: 查看权限

### 6. DTO 层 (Data Transfer Objects)

**请求 DTO** (`modules/nexus/src/main/scala/application/dto/request/organization/`):
- `CreateOrganizationRequest`
- `UpdateOrganizationRequest`
- `InviteMemberRequest`
- `UpdateMemberRoleRequest`
- `TransferOwnershipRequest`

**响应 DTO** (`modules/nexus/src/main/scala/application/dto/response/organization/`):
- `OrganizationResponse`
- `OrganizationMemberResponse`

### 7. HTTP REST API

**位置**: `modules/nexus/src/main/scala/presentation/http/routes/OrganizationRoutes.scala`

**API 端点**:

| 方法 | 路径 | 功能 |
|------|------|------|
| POST | `/api/v1/organizations` | 创建组织 |
| GET | `/api/v1/organizations` | 列出用户的所有组织 |
| GET | `/api/v1/organizations/owned` | 列出用户拥有的组织 |
| GET | `/api/v1/organizations/:id` | 获取组织详情 |
| PUT | `/api/v1/organizations/:id` | 更新组织信息 |
| DELETE | `/api/v1/organizations/:id` | 删除组织 |
| GET | `/api/v1/organizations/:id/members` | 列出组织成员 |
| POST | `/api/v1/organizations/:id/members` | 邀请成员 |
| DELETE | `/api/v1/organizations/:id/members/:userId` | 移除成员 |
| POST | `/api/v1/organizations/:id/accept` | 接受邀请 |
| POST | `/api/v1/organizations/:id/leave` | 离开组织 |
| POST | `/api/v1/organizations/:id/transfer-ownership` | 转让所有权 |
| GET | `/api/v1/organizations/invites` | 列出待处理的邀请 |

## 待完成工作

### 1. 代码生成

由于修改了数据库表结构（添加了配额字段），需要重新生成 Quill 模型：

```bash
sbt "codegen/run"
```

这将更新 `UserQuotaRow.scala`，添加新的字段：
- `maxOwnedOrganizations`
- `maxJoinedOrganizations`
- `currentOwnedOrganizations`

### 2. 注册服务到 Main.scala

**位置**: `modules/nexus/src/main/scala/Main.scala`

需要在 ZLayer 中添加：

```scala
// Repository Layer
val organizationRepoLayer = OrganizationRepositoryLive.live
val organizationMemberRepoLayer = OrganizationMemberRepositoryLive.live

// Service Layer
val organizationServiceLayer = OrganizationServiceLive.live
  .provideSome[OrganizationRepository & OrganizationMemberRepository & UserRepository](
    organizationRepoLayer,
    organizationMemberRepoLayer,
    userRepoLayer
  )

// Routes Layer
val organizationRoutesLayer = OrganizationRoutes.live
  .provideSome[OrganizationService & Middleware](
    organizationServiceLayer,
    middlewareLayer
  )
```

并在 HTTP 服务器中注册路由：

```scala
val allRoutes =
  authRoutes.routes ++
  userRoutes.routes ++
  projectRoutes.routes ++
  organizationRoutes.routes ++ // 新增
  // ... 其他路由
```

### 3. GraphQL API (可选)

如果需要 GraphQL 支持，需要创建：

**位置**: `modules/nexus/src/main/scala/presentation/graphql/`

- `schema/OrganizationSchema.scala`: GraphQL schema 定义
- `resolver/OrganizationResolver.scala`: GraphQL resolver

示例：

```scala
// OrganizationSchema.scala
case class OrganizationQueries(
  organization: OrganizationId => ZIO[OrganizationService, AppError, Organization],
  organizations: ZIO[OrganizationService, AppError, List[Organization]],
  myOrganizations: ZIO[OrganizationService, AppError, List[Organization]]
)

case class OrganizationMutations(
  createOrganization: CreateOrganizationInput => ZIO[OrganizationService, AppError, Organization],
  updateOrganization: (OrganizationId, UpdateOrganizationInput) => ZIO[OrganizationService, AppError, Organization],
  deleteOrganization: OrganizationId => ZIO[OrganizationService, AppError, Unit],
  inviteMember: (OrganizationId, InviteMemberInput) => ZIO[OrganizationService, AppError, OrganizationMember],
  transferOwnership: (OrganizationId, UserId) => ZIO[OrganizationService, AppError, Unit]
)
```

### 4. 数据库迁移

运行应用时，Flyway 会自动执行迁移：

```bash
# 启动数据库
docker-compose -f docker-compose.dev.yml up -d

# 运行应用（会自动执行迁移）
sbt run
```

或手动运行迁移：

```bash
sbt "migration/run"
```

### 5. 测试

建议创建以下测试：

**单元测试**:
- `OrganizationServiceSpec.scala`: 测试业务逻辑
- `QuotaSpec.scala`: 测试配额验证

**集成测试**:
- `OrganizationRepositorySpec.scala`: 测试数据库操作
- `OrganizationRoutesSpec.scala`: 测试 API 端点

### 6. 用户配额同步

需要添加功能来同步用户的 `currentOwnedOrganizations` 字段：

在 `UserRepository` 中添加方法：

```scala
trait UserRepository:
  // ... 现有方法
  def updateOwnedOrganizationsCount(userId: UUID): AppTask[Unit]
```

实现：

```scala
override def updateOwnedOrganizationsCount(userId: UUID): AppTask[Unit] = runQuery:
  ZIO.attempt:
    val count = run(quote {
      memberSchema
        .filter(m => m.userId == lift(userId) && m.role == "owner" && m.isActive)
        .size
    })
    run(quote {
      quotaSchema
        .filter(_.userId == lift(userId))
        .update(_.currentOwnedOrganizations -> lift(count.toInt))
    })
```

在创建、转让、删除组织时调用此方法更新配额。

## 核心业务逻辑说明

### 配额系统

**用户配额**:
- 免费用户：最多拥有 2 个组织，5GB 存储空间
- 付费用户：最多拥有 10 个组织，100GB 存储空间

**组织配额**:
- 免费组织：最多 5 个成员，100GB 存储，500 GPU 小时/月
- 高级组织：最多 50 个成员，1TB 存储，5000 GPU 小时/月
- 企业组织：无限制

### 权限模型

| 角色 | 创建组织 | 邀请成员 | 移除成员 | 更新信息 | 删除组织 | 转让所有权 |
|------|---------|---------|---------|---------|---------|-----------|
| Owner | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ |
| Admin | ❌ | ✅ | ✅ (仅 Member) | ✅ | ❌ | ❌ |
| Member | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ |

### 所有权转让流程

1. 验证当前用户是 Owner
2. 验证新 Owner 是组织的激活成员
3. 检查新 Owner 的配额（是否还能拥有更多组织）
4. 更新角色：
   - 当前 Owner → Admin
   - 新 Owner → Owner
5. 更新用户配额计数器

## 使用示例

### 创建组织

```bash
curl -X POST http://localhost:8080/api/v1/organizations \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "My Organization",
    "description": "A great organization for robotics"
  }'
```

### 邀请成员

```bash
curl -X POST http://localhost:8080/api/v1/organizations/{org-id}/members \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "userId": "user-uuid",
    "role": "admin"
  }'
```

### 转让所有权

```bash
curl -X POST http://localhost:8080/api/v1/organizations/{org-id}/transfer-ownership \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "newOwnerId": "new-owner-uuid"
  }'
```

## 注意事项

1. **编译顺序**: 先运行代码生成，再编译项目
2. **事务处理**: 创建组织和添加 Owner 成员应该在同一事务中
3. **配额同步**: 确保用户配额计数器与实际数据一致
4. **软删除**: 组织删除使用软删除，不会真正删除数据
5. **权限验证**: 所有操作都有严格的权限检查

## 文件清单

### 新增文件 (30+)

**领域模型** (5 个):
- `domain/model/organization/Organization.scala`
- `domain/model/organization/OrganizationMember.scala`
- `domain/model/organization/OrganizationRole.scala`
- `domain/model/organization/PlanType.scala`
- `domain/model/organization/OrganizationQuota.scala`

**仓储** (4 个):
- `domain/repository/OrganizationRepository.scala`
- `domain/repository/OrganizationMemberRepository.scala`
- `infrastructure/persistence/postgres/repository/OrganizationRepositoryLive.scala`
- `infrastructure/persistence/postgres/repository/OrganizationMemberRepositoryLive.scala`

**服务** (2 个):
- `domain/services/app/OrganizationService.scala`
- `application/services/OrganizationServiceLive.scala`

**DTO** (7 个):
- `application/dto/request/organization/CreateOrganizationRequest.scala`
- `application/dto/request/organization/UpdateOrganizationRequest.scala`
- `application/dto/request/organization/InviteMemberRequest.scala`
- `application/dto/request/organization/UpdateMemberRoleRequest.scala`
- `application/dto/request/organization/TransferOwnershipRequest.scala`
- `application/dto/response/organization/OrganizationResponse.scala`
- `application/dto/response/organization/OrganizationMemberResponse.scala`

**API** (1 个):
- `presentation/http/routes/OrganizationRoutes.scala`

**数据库迁移** (1 个):
- `modules/migration/src/main/resources/db/migration/postgres/V13__add_organization_quotas_to_users.sql`

### 修改文件 (3 个)

- `domain/model/user/Quota.scala`: 添加组织配额字段
- `infrastructure/persistence/postgres/repository/UserRepositoryLive.scala`: 支持新配额字段
- `modules/nexus/src/main/scala/Main.scala`: (待修改) 注册新服务
