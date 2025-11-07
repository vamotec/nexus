# DTO 设计策略文档

## 概述

本文档说明 Nexus 项目中 DTO (Data Transfer Object) 的设计策略，基于 DDD (Domain-Driven Design) 和企业级最佳实践。

## 核心原则

### 🎯 混合策略

采用**分层隔离 + 选择性复用**的混合策略，在类型安全和维护成本之间取得平衡。

```
┌─────────────────────────────────────────┐
│  Presentation Layer (API)               │
│  - 简化的 DTO                            │
│  - 扁平化枚举                            │
│  - 复用简单值对象                        │
│  - JSON 处理复杂配置                     │
└─────────────────────────────────────────┘
              ↓ Mapper
┌─────────────────────────────────────────┐
│  Application Layer                      │
│  - DTO ↔ Domain 转换                     │
│  - 验证和业务编排                        │
└─────────────────────────────────────────┘
              ↓
┌─────────────────────────────────────────┐
│  Domain Layer                           │
│  - 完整的领域模型                        │
│  - 复杂的业务逻辑                        │
└─────────────────────────────────────────┘
```

## 分层决策矩阵

### ✅ 直接复用 Domain 模型

**适用场景:**
- 简单的值对象 (Value Objects)
- 不太可能改变的数据结构
- 不包含业务逻辑的纯数据类

**示例:**
```scala
// Domain Value Object
case class Position3D(x: Double, y: Double, z: Double)
case class Quaternion(w: Double, x: Double, y: Double, z: Double)
case class Dimensions3D(width: Double, height: Double, depth: Double)

// DTO 直接复用
case class ObstacleDto(
  position: Position3D,      // ✅ 直接复用
  rotation: Quaternion,      // ✅ 直接复用
  dimensions: Dimensions3D,  // ✅ 直接复用
  obstacleType: String       // 枚举扁平化
)
```

**好处:**
- ✅ 减少重复代码
- ✅ 保持类型安全
- ✅ 自动生成 JSON Schema
- ✅ IDE 智能提示

### ✅ 扁平化枚举类型

**适用场景:**
- 枚举类型 (enum)
- ADT (Algebraic Data Types)

**示例:**
```scala
// Domain 枚举
enum RobotType:
  case FrankaPanda
  case UR5
  case Kuka
  case Custom(name: String)

// API DTO 扁平化为字符串
case class SceneConfigDto(
  robotType: String  // "franka_panda", "ur5", "kuka", "custom:xxx"
)

// Mapper 负责转换
object SimulationMapper:
  def parseRobotType(str: String): Either[String, RobotType] =
    str.toLowerCase match
      case "franka_panda" => Right(RobotType.FrankaPanda)
      case "ur5" => Right(RobotType.UR5)
      case s if s.startsWith("custom:") => Right(RobotType.Custom(s.drop(7)))
      case _ => Left(s"Unknown robot type: $str")
```

**好处:**
- ✅ API 文档清晰
- ✅ 前端易于使用
- ✅ 版本兼容性好
- ✅ 国际化友好

### ✅ 简化复杂嵌套

**适用场景:**
- 复杂的聚合根
- 深层嵌套结构
- 包含多个子对象

**示例:**
```scala
// Domain - 复杂的场景配置
case class SceneConfig(
  name: String,
  robotType: RobotType,
  environment: Environment,        // 复杂对象
  obstacles: List[Obstacle],       // 复杂列表
  sensors: List[Sensor],           // 复杂列表
  // ... 更多复杂配置
)

// DTO - 简化版本
case class SceneConfigDto(
  name: String,
  robotType: String,               // 扁平化
  environment: String,             // 扁平化
  startPosition: Position3D,       // 复用简单 VO
  obstacles: List[ObstacleDto],    // 简化的子 DTO
  sensors: List[SensorDto],        // 简化的子 DTO
  advancedConfig: Option[Json]     // 复杂配置用 JSON
)
```

**好处:**
- ✅ API 接口简洁
- ✅ 减少客户端复杂度
- ✅ 易于文档化
- ✅ 支持灵活扩展

### ✅ JSON 处理高级配置

**适用场景:**
- 高度可变的配置
- 不同类型的参数
- 实验性功能
- 向后兼容

**示例:**
```scala
case class SimulationConfigDto(
  // 核心配置 - 类型安全
  sceneConfig: SceneConfigDto,
  simulationParams: SimulationParams,

  // 训练配置 - JSON (不同算法有不同参数)
  trainingConfig: Option[Json] = None,
  // 例如: {"algorithm": "PPO", "episodes": 1000, "learningRate": 0.001}

  // 高级配置 - JSON (可选功能)
  advancedConfig: Option[Json] = None
  // 例如: {"parallel": true, "workers": 4, "checkpoint": "path"}
)
```

**好处:**
- ✅ 灵活性高
- ✅ 易于扩展
- ✅ 向后兼容
- ✅ 适合实验功能

## 实际应用

### Simulation DTO 架构

```
CreateSimulationRequest
  ├─ projectId: ProjectId               (复用 Domain VO)
  ├─ name: String                       (基础类型)
  ├─ description: Option[String]        (基础类型)
  ├─ config: SimulationConfigDto        (简化 DTO)
  │    ├─ sceneConfig: SceneConfigDto   (简化 DTO)
  │    │    ├─ robotType: String        (枚举扁平化)
  │    │    ├─ environment: String      (枚举扁平化)
  │    │    ├─ startPosition: Position3D (复用 Domain VO)
  │    │    ├─ obstacles: List[ObstacleDto] (简化 DTO)
  │    │    └─ advancedConfig: Json     (JSON 灵活配置)
  │    ├─ simulationParams: SimulationParams (复用 Domain)
  │    └─ trainingConfig: Json          (JSON 灵活配置)
  └─ tags: List[String]                 (基础类型)
```

### Mapper 职责

```scala
object SimulationMapper:
  // DTO → Domain
  def toSceneConfig(dto: SceneConfigDto): Either[String, SceneConfig]
  def toObstacle(dto: ObstacleDto): Either[String, Obstacle]

  // Domain → DTO
  def toSceneConfigDto(domain: SceneConfig): SceneConfigDto
  def toObstacleDto(domain: Obstacle): ObstacleDto

  // 枚举转换
  def parseRobotType(str: String): Either[String, RobotType]
  def robotTypeToString(robotType: RobotType): String
```

## ❌ 反模式

### 不要: 完全复制 Domain 模型

```scala
// ❌ 错误：为每个 Domain 对象创建对应的 DTO
case class Position3Ddto(x: Double, y: Double, z: Double)  // 重复！
case class QuaternionDto(w: Double, x: Double, y: Double, z: Double)  // 重复！

// ✅ 正确：直接复用简单值对象
import app.mosia.nexus.domain.model.common.{Position3D, Quaternion}
```

### 不要: 在 API 层暴露完整 Domain

```scala
// ❌ 错误：直接暴露复杂的 Domain 对象
case class CreateSimulationRequest(
  config: SceneConfig  // 完整的 Domain 对象，耦合度高
)

// ✅ 正确：使用简化的 DTO
case class CreateSimulationRequest(
  config: SceneConfigDto  // 简化的 DTO
)
```

### 不要: 滥用 Map[String, String]

```scala
// ❌ 错误：失去类型安全
case class CreateSimulationRequest(
  config: Map[String, String]  // 无法生成文档，无类型检查
)

// ✅ 正确：核心配置类型安全，高级配置用 JSON
case class CreateSimulationRequest(
  config: SimulationConfigDto,
  advancedConfig: Option[Json]
)
```

### 不要: 在 DTO 中包含业务逻辑

```scala
// ❌ 错误：DTO 包含验证逻辑
case class SceneConfigDto(...):
  def validate: Either[String, SceneConfigDto] = ???

// ✅ 正确：验证在 Application 层
object SimulationService:
  def createSimulation(request: CreateSimulationRequest): AppTask[Simulation] =
    for
      // 验证在这里
      sceneConfig <- SimulationMapper.toSceneConfig(request.config.sceneConfig)
      _ <- sceneConfig.validateZIO
      // ...
    yield simulation
```

## 最佳实践总结

1. **值对象直接复用** - Position3D, Quaternion 等简单类型
2. **枚举扁平化** - API 层用字符串，Application 层转换
3. **复杂嵌套简化** - 创建独立的简化 DTO
4. **稳定配置复用** - SimulationParams 等稳定结构直接复用
5. **高级功能用 JSON** - 实验性、可变配置用 JSON
6. **纯数据 DTO** - 不包含业务逻辑
7. **Mapper 在 Application 层** - 清晰的转换边界
8. **类型安全优先** - 除非有充分理由，否则避免 Map/Json

## 维护指南

### 添加新的 DTO

1. 评估是否可以复用 Domain 模型
2. 如果需要创建 DTO，采用混合策略
3. 在 Mapper 中添加转换方法
4. 更新相关的 Request/Response
5. 更新 API 文档

### 修改现有 DTO

1. 评估影响范围 (API 兼容性)
2. 更新 DTO 定义
3. 更新 Mapper 转换逻辑
4. 更新测试
5. 更新 API 文档
6. 考虑版本化策略

## 参考

- [Domain-Driven Design](https://martinfowler.com/bliki/DomainDrivenDesign.html)
- [Clean Architecture](https://blog.cleancoder.com/uncle-bob/2012/08/13/the-clean-architecture.html)
- [DTO Pattern](https://martinfowler.com/eaaCatalog/dataTransferObject.html)
