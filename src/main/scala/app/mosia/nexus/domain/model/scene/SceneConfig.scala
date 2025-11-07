package app.mosia.nexus.domain.model.scene

import app.mosia.nexus.domain.model.common.{Position3D, ValueObject}
import zio.{IO, ZIO}

/** 场景配置 * */
case class SceneConfig(
  name: String,
  robotType: RobotType,
  robotUrdf: String,
  environment: Environment,
  obstacles: List[Obstacle],
  startPosition: Position3D,
  goalPosition: Option[Position3D], // 有目标的任务
  sensors: List[Sensor]
) extends ValueObject:

  /** 验证场景有效性 */
  def validate: Either[String, SceneConfig] =
    if (name.isEmpty) Left("Scene name cannot be empty")
    else if (obstacles.length > 100) Left("Too many obstacles (max 100)")
    else Right(this)

  /** ZIO版本：错误类型变为 Throwable */
  def validateZIO: IO[Throwable, SceneConfig] =
    ZIO.fromEither(validate.left.map(new IllegalArgumentException(_)))
