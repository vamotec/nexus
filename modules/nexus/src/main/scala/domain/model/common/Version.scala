package app.mosia.nexus
package domain.model.common

import domain.model.user.UserId

import java.time.Instant

import caliban.schema.{ArgBuilder, Schema as Cs}
import sttp.tapir.Schema
import zio.json.*
import zio.*
import zio.json.ast.Json

object Version:
  /** 模板版本信息 */
  case class TemplateVersion(
    // 版本标识
    version: VersionNumber,

    // 版本元数据
    metadata: VersionMetadata,

    // 变更信息
    changes: VersionChanges,

    // 兼容性信息
    compatibility: CompatibilityInfo,

    // 质量保证
    quality: QualityMetrics,

    // 部署状态
    deployment: DeploymentStatus
  )

  /** 版本号 (语义化版本) */
  case class VersionNumber(
    major: Int,
    minor: Int,
    patch: Int,
    prerelease: Option[String] = None, // e.g., "alpha", "beta", "rc1"
    build: Option[String] = None // e.g., "build123"
  ) {
    override def toString: String = {
      val base = s"$major.$minor.$patch"
      val pre  = prerelease.map("-" + _).getOrElse("")
      val bld  = build.map("+" + _).getOrElse("")
      base + pre + bld
    }

    def isStable: Boolean = prerelease.isEmpty

    def compare(other: VersionNumber): Int =
      if (major != other.major) major.compare(other.major)
      else if (minor != other.minor) minor.compare(other.minor)
      else if (patch != other.patch) patch.compare(other.patch)
      else 0 // 预发布版本比较需要更复杂的逻辑
  }

  object VersionNumber {
    def parse(versionString: String): Either[String, VersionNumber] = {
      val pattern = """^(\d+)\.(\d+)\.(\d+)(?:-([a-zA-Z0-9.-]+))?(?:\+([a-zA-Z0-9.-]+))?$""".r
      versionString match {
        case pattern(major, minor, patch, pre, build) =>
          Right(
            VersionNumber(
              major.toInt,
              minor.toInt,
              patch.toInt,
              Option(pre),
              Option(build)
            )
          )
        case _ => Left(s"Invalid version format: $versionString")
      }
    }

    val initial: VersionNumber = VersionNumber(1, 0, 0)
  }

  /** 版本元数据 */
  case class VersionMetadata(
    createdBy: UserId,
    createdAt: Instant,
    description: String,
    tags: Set[String] = Set.empty,
    attributes: Map[String, String] = Map.empty
  )

  /** 版本变更信息 */
  case class VersionChanges(
    changeType: ChangeType,
    summary: String,
    description: String,
    breakingChanges: List[BreakingChange],
    newFeatures: List[NewFeature],
    bugFixes: List[BugFix],
    improvements: List[Improvement],
    dependencies: DependencyChanges
  )

  /** 变更类型 */
  enum ChangeType:
    case Major, Minor, Patch, Hotfix, Experimental

  /** 破坏性变更 */
  case class BreakingChange(
    component: String,
    description: String,
    migrationGuide: Option[String],
    severity: BreakingChangeSeverity
  )

  enum BreakingChangeSeverity:
    case Low, Medium, High, Critical

  /** 新功能 */
  case class NewFeature(
    name: String,
    description: String,
    category: FeatureCategory,
    enabledByDefault: Boolean = true
  )

  enum FeatureCategory:
    case Perception, Planning, Control, Simulation, Visualization, Utility

  /** Bug修复 */
  case class BugFix(
    issueId: String, // e.g., "BUG-123"
    description: String,
    affectedComponents: Set[String],
    resolution: String
  )

  /** 改进 */
  case class Improvement(
    area: String,
    description: String,
    performanceImpact: PerformanceImpact,
    before: Option[String] = None, // 改进前的状态
    after: Option[String] = None // 改进后的状态
  )

  enum PerformanceImpact:
    case SignificantImprovement, ModerateImprovement,
      SlightImprovement, Neutral, Regression

  /** 依赖变更 */
  case class DependencyChanges(
    added: List[Dependency] = List.empty,
    removed: List[Dependency] = List.empty,
    updated: List[DependencyUpdate] = List.empty
  )

  case class Dependency(
    name: String,
    version: String,
    `type`: DependencyType
  )

  enum DependencyType:
    case SimulationEngine, SensorModel, PhysicsEngine,
      MLFramework, Visualization, Internal

  case class DependencyUpdate(
    dependency: Dependency,
    fromVersion: String,
    toVersion: String,
    changeType: DependencyChangeType
  )

  enum DependencyChangeType:
    case Major, Minor, Patch, Security

  /** 兼容性信息 */
  case class CompatibilityInfo(
    minSimulatorVersion: VersionNumber,
    maxSimulatorVersion: Option[VersionNumber] = None,
    compatibleTemplates: Set[TemplateId] = Set.empty,
    migrationRequired: Boolean = false,
    deprecated: Boolean = false,
    deprecationMessage: Option[String] = None
  )

  /** 质量指标 */
  case class QualityMetrics(
    validation: ValidationResult,
    performance: PerformanceMetrics,
    stability: StabilityMetrics,
    testing: TestCoverage
  )

  /** 验证结果 */
  case class ValidationResult(
    status: ValidationStatus,
    checks: List[ValidationCheck],
    score: Double // 0.0-1.0
//                               issues: List[ValidationIssue]
  )

  enum ValidationStatus:
    case Passed, Warning, Failed, Pending

  case class ValidationCheck(
    name: String,
    description: String,
    status: ValidationStatus,
    details: String
  )

  /** 性能指标 */
  case class PerformanceMetrics(
    cpuUsage: ResourceUsage, // CPU使用情况
    memoryUsage: ResourceUsage, // 内存使用情况
    frameRate: FrameRateMetrics, // 帧率表现
    loadTime: Duration, // 加载时间
    simulationSpeed: Double // 仿真速度倍率
  )

  case class ResourceUsage(
    average: Double,
    peak: Double,
    unit: String = "%"
  )

  case class FrameRateMetrics(
    average: Double,
    min: Double,
    max: Double,
    stability: Double // 稳定性 0.0-1.0
  )

  /** 稳定性指标 */
  case class StabilityMetrics(
    crashRate: Double, // 崩溃率 0.0-1.0
    meanTimeBetweenFailures: Duration,
    errorFrequency: Map[String, Int], // 各类错误频率
    recoveryTime: Duration // 平均恢复时间
  )

  /** 测试覆盖 */
  case class TestCoverage(
    unitTests: TestStats,
    integrationTests: TestStats,
    scenarioTests: TestStats,
    totalCoverage: Double // 总覆盖率 0.0-1.0
  )

  case class TestStats(
    total: Int,
    passed: Int,
    failed: Int,
    coverage: Double
  )

  /** 部署状态 */
  case class DeploymentStatus(
    environment: DeploymentEnvironment,
    status: DeploymentState,
    deployedAt: Option[Instant] = None,
    deployedBy: Option[UserId] = None,
    rollbackVersion: Option[VersionNumber] = None,
    health: DeploymentHealth
  )

  /** 部署环境 */
  enum DeploymentEnvironment:
    case Development, Staging, Production, Canary

  /** 部署状态 */
  enum DeploymentState:
    case NotDeployed, Deploying, Active, Failed, RollingBack, Retired

  /** 部署健康状态 */
  case class DeploymentHealth(
    status: HealthStatus,
    incidents: List[DeploymentIncident],
    uptime: Double, // 正常运行时间比例
    performance: HealthPerformance
  )

  enum HealthStatus:
    case Healthy, Degraded, Unhealthy, Unknown

  case class DeploymentIncident(
    severity: IncidentSeverity,
    description: String,
    occurredAt: Instant,
    resolvedAt: Option[Instant] = None
  )

  enum IncidentSeverity:
    case Low, Medium, High, Critical

  case class HealthPerformance(
    responseTime: Duration,
    errorRate: Double,
    throughput: Double
  )
