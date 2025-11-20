package app.mosia.nexus
package infrastructure.persistence.postgres.repository

import infrastructure.persistence.BaseSource.PostgresDataSource
import infrastructure.persistence.postgres.rows.{SimulationObstacleRow, SimulationRow, SimulationSensorRow}
import domain.error.*
import domain.model.common.{Dimensions3D, Position3D, Quaternion}
import domain.model.project.ProjectId
import domain.model.scene.{Environment as SceneEnvironment, *}
import domain.model.scene.RobotType.toRoString
import domain.model.session.SessionId
import domain.model.simulation.*
import domain.model.training.TrainingConfig
import domain.model.user.UserId
import domain.repository.SimulationRepository

import io.getquill.*
import io.getquill.context.json.*
import zio.*
import zio.json.*
import zio.json.ast.Json

final class SimulationRepositoryLive(ctx: DefaultDbContext, dataSource: PostgresDataSource)
    extends BaseRepository(ctx, dataSource)
    with SimulationRepository:

  import ctx.*
  private inline def simulationSchema = querySchema[SimulationRow]("simulations")
  private inline def obstacleSchema   = querySchema[SimulationObstacleRow]("simulation_obstacles")
  private inline def sensorSchema     = querySchema[SimulationSensorRow]("simulation_sensors")

  override def save(simulation: Simulation): AppTask[Unit] =
    val row = toRow(simulation)

    val obstacleEntities = simulation.sceneConfig.obstacles.map: obs =>
      SimulationObstacleRow(
        simulationId = simulation.id.value,
        obstacleId = obs.id,
        obstacleType = ObstacleType.toString(obs.obstacleType),
        positionX = obs.position.x,
        positionY = obs.position.y,
        positionZ = obs.position.z,
        rotationQx = obs.rotation.x,
        rotationQy = obs.rotation.y,
        rotationQz = obs.rotation.z,
        rotationQw = obs.rotation.w,
        dimensionsX = obs.dimensions.width,
        dimensionsY = obs.dimensions.height,
        dimensionsZ = obs.dimensions.depth,
        material = obs.material.map(v => JsonbValue(v.toJsonAST.getOrElse(Json.Obj()))),
        dynamic = obs.dynamic
      )

    val sensorEntities = simulation.sceneConfig.sensors.map: sensor =>
      SimulationSensorRow(
        simulationId = simulation.id.value,
        sensorId = sensor.id,
        sensorType = SensorType.toString(sensor.sensorType),
        positionX = sensor.position.x,
        positionY = sensor.position.y,
        positionZ = sensor.position.z,
        orientationQx = sensor.orientation.x,
        orientationQy = sensor.orientation.y,
        orientationQz = sensor.orientation.z,
        orientationQw = sensor.orientation.w,
        config = JsonbValue(sensor.config.toJsonAST.getOrElse(Json.Obj()))
      )

    transaction:
      for
        _ <- run(simulationSchema.insertValue(lift(row)))
        _ <- ZIO.when(obstacleEntities.nonEmpty)(
          run(liftQuery(obstacleEntities).foreach(o => obstacleSchema.insertValue(o)))
        )
        _ <- ZIO.when(sensorEntities.nonEmpty)(
          run(liftQuery(sensorEntities).foreach(s => sensorSchema.insertValue(s)))
        )
      yield ()

  override def findById(id: SimulationId): AppTask[Option[Simulation]] = runQuery:
    val uuid = id.value
    for
      simOpt <- run(simulationSchema.filter(_.id == lift(uuid))).map(_.headOption)
      result <- simOpt match
        case None => ZIO.succeed(None)
        case Some(sim) =>
          for
            obstacles <- run(obstacleSchema.filter(_.simulationId == lift(uuid)))
            sensors <- run(sensorSchema.filter(_.simulationId == lift(uuid)))
          yield Some(toDomain(sim, obstacles, sensors))
    yield result

  override def findByProjectId(projectId: ProjectId): AppTask[List[Simulation]] = runQuery:
    val uuid = projectId.value
    for
      sims <- run(simulationSchema.filter(_.projectId == lift(uuid)))
      results <- ZIO.foreach(sims): sim =>
        for
          obstacles <- run(obstacleSchema.filter(_.simulationId == lift(sim.id)))
          sensors <- run(sensorSchema.filter(_.simulationId == lift(sim.id)))
        yield toDomain(sim, obstacles, sensors)
    yield results

  override def update(simulation: Simulation): AppTask[Unit] = transaction:
    val row  = toRow(simulation)
    val uuid = simulation.id.value

    // 准备关联数据
    val obstacleEntities = simulation.sceneConfig.obstacles.map { obs =>
      SimulationObstacleRow(
        simulationId = uuid,
        obstacleId = obs.id,
        obstacleType = ObstacleType.toString(obs.obstacleType),
        positionX = obs.position.x,
        positionY = obs.position.y,
        positionZ = obs.position.z,
        rotationQx = obs.rotation.x,
        rotationQy = obs.rotation.y,
        rotationQz = obs.rotation.z,
        rotationQw = obs.rotation.w,
        dimensionsX = obs.dimensions.width,
        dimensionsY = obs.dimensions.height,
        dimensionsZ = obs.dimensions.depth,
        material = obs.material.map(v => JsonbValue(v.toJsonAST.getOrElse(Json.Obj()))),
        dynamic = obs.dynamic
      )
    }

    val sensorEntities = simulation.sceneConfig.sensors.map { sensor =>
      SimulationSensorRow(
        simulationId = uuid,
        sensorId = sensor.id,
        sensorType = SensorType.toString(sensor.sensorType),
        positionX = sensor.position.x,
        positionY = sensor.position.y,
        positionZ = sensor.position.z,
        orientationQx = sensor.orientation.x,
        orientationQy = sensor.orientation.y,
        orientationQz = sensor.orientation.z,
        orientationQw = sensor.orientation.w,
        config = JsonbValue(sensor.config.toJsonAST.getOrElse(Json.Obj()))
      )
    }

    // 更新主表和子表（事务操作）
    for
      // 更新主表
      _ <- run(simulationSchema.filter(_.id == lift(uuid)).updateValue(lift(row)))

      // 删除旧的关联数据
      _ <- run(obstacleSchema.filter(_.simulationId == lift(uuid)).delete)
      _ <- run(sensorSchema.filter(_.simulationId == lift(uuid)).delete)

      // 插入新的关联数据
      _ <- ZIO.when(obstacleEntities.nonEmpty)(
        run(liftQuery(obstacleEntities).foreach(o => obstacleSchema.insertValue(o)))
      )
      _ <- ZIO.when(sensorEntities.nonEmpty)(
        run(liftQuery(sensorEntities).foreach(s => sensorSchema.insertValue(s)))
      )
    yield ()

  override def delete(id: SimulationId): AppTask[Unit] = runQuery:
    val uuid = id.value
    for
      // 由于外键设置了 ON DELETE CASCADE，删除主表会自动删除子表
      // 但为了明确，也可以手动删除
      _ <- run(obstacleSchema.filter(_.simulationId == lift(uuid)).delete)
      _ <- run(sensorSchema.filter(_.simulationId == lift(uuid)).delete)
      _ <- run(simulationSchema.filter(_.id == lift(uuid)).delete)
    yield ()

  private def toRow(domain: Simulation): SimulationRow =
    SimulationRow(
      id = domain.id.value,
      projectId = domain.projectId.value,
      name = domain.name,
      description = domain.description,

      // 版本
      versionMajor = domain.version.major,
      versionMinor = domain.version.minor,

      // SceneConfig 字段展平
      sceneName = domain.sceneConfig.name,
      robotType = toRoString(domain.sceneConfig.robotType),
      robotUrdf = domain.sceneConfig.robotUrdf,
      environment = JsonbValue(domain.sceneConfig.environment.toJsonAST.getOrElse(Json.Obj())),

      // 位置
      startPosX = domain.sceneConfig.startPosition.x,
      startPosY = domain.sceneConfig.startPosition.y,
      startPosZ = domain.sceneConfig.startPosition.z,
      goalPosX = domain.sceneConfig.goalPosition.map(_.x),
      goalPosY = domain.sceneConfig.goalPosition.map(_.y),
      goalPosZ = domain.sceneConfig.goalPosition.map(_.z),

      // JSON 字段
      simulationParams = JsonbValue(domain.simulationParams.toJsonAST.getOrElse(Json.Obj())),
      trainingConfig = domain.trainingConfig.map(v => JsonbValue(v.toJsonAST.getOrElse(Json.Obj()))),

      // 统计信息
      totalRuns = domain.statistics.totalRuns,
      successfulRuns = domain.statistics.successfulRuns,
      failedRuns = domain.statistics.failedRuns,
      avgCompletionTime = domain.statistics.avgCompletionTime,
      avgCollisions = domain.statistics.avgCollisions,
      bestSessionId = domain.statistics.bestSessionId.map(_.value),
      lastRunAt = domain.statistics.lastRunAt,

      // 元数据
      tags = domain.tags,
      createdBy = domain.createdBy.value,
      isArchived = false, // 假设默认值，根据需求调整

      // 时间戳
      createdAt = domain.createdAt,
      updatedAt = domain.updatedAt
    )

  private def toDomain(
    simulationEntity: SimulationRow,
    obstacleEntities: List[SimulationObstacleRow],
    sensorEntities: List[SimulationSensorRow]
  ): Simulation =
    val obstacles = obstacleEntities.map { oe =>
      Obstacle(
        id = oe.obstacleId,
        obstacleType = ObstacleType.fromString(oe.obstacleType),
        position = Position3D(oe.positionX, oe.positionY, oe.positionZ),
        rotation = Quaternion(oe.rotationQx, oe.rotationQy, oe.rotationQz, oe.rotationQw),
        dimensions = Dimensions3D(oe.dimensionsX, oe.dimensionsY, oe.dimensionsZ),
        material = oe.material.map(json =>
          json.value.toJson
            .fromJson[Material]
            .getOrElse(throw InvalidFieldValue("Material", "none", "json"))
        ),
        dynamic = oe.dynamic
      )
    }

    val sensors = sensorEntities.map { se =>
      Sensor(
        id = se.sensorId,
        sensorType = SensorType.fromString(se.sensorType), // 需要实现 fromString
        position = Position3D(se.positionX, se.positionY, se.positionZ),
        orientation = Quaternion(se.orientationQx, se.orientationQy, se.orientationQz, se.orientationQw),
        config = se.config.value.toJson
          .fromJson[SensorConfig]
          .getOrElse(throw InvalidFieldValue("SensorConfig", "none", "json"))
      )
    }

    Simulation(
      id = SimulationId(simulationEntity.id),
      projectId = ProjectId(simulationEntity.projectId),
      name = simulationEntity.name,
      description = simulationEntity.description,
      version = SimulationVersion(simulationEntity.versionMajor, simulationEntity.versionMinor),

      // 重建 SceneConfig
      sceneConfig = SceneConfig(
        name = simulationEntity.sceneName,
        robotType = RobotType
          .fromString(simulationEntity.robotType)
          .getOrElse(throw InvalidFieldValue("RobotType", "none", "json")),
        robotUrdf = simulationEntity.robotUrdf,
        environment = simulationEntity.environment.value.toJson
          .fromJson[SceneEnvironment]
          .getOrElse(throw InvalidFieldValue("SceneEnvironment", "none", "json")),
        startPosition = Position3D(simulationEntity.startPosX, simulationEntity.startPosY, simulationEntity.startPosZ),
        goalPosition = for {
          x <- simulationEntity.goalPosX
          y <- simulationEntity.goalPosY
          z <- simulationEntity.goalPosZ
        } yield Position3D(x, y, z),
        obstacles = obstacles,
        sensors = sensors
      ),

      // JSON 反序列化
      simulationParams = simulationEntity.simulationParams.value.toJson
        .fromJson[SimulationParams]
        .getOrElse(throw InvalidFieldValue("SimulationParams", "none", "json")),
      trainingConfig = simulationEntity.trainingConfig.map(
        _.value.toJson
          .fromJson[TrainingConfig]
          .getOrElse(throw InvalidFieldValue("TrainingConfig", "none", "json"))
      ),

      // 统计信息
      statistics = SimulationStatistics(
        totalRuns = simulationEntity.totalRuns,
        successfulRuns = simulationEntity.successfulRuns,
        failedRuns = simulationEntity.failedRuns,
        avgCompletionTime = simulationEntity.avgCompletionTime,
        avgCollisions = simulationEntity.avgCollisions,
        bestSessionId = simulationEntity.bestSessionId.map(SessionId(_)),
        lastRunAt = simulationEntity.lastRunAt
      ),

      // 元数据
      tags = simulationEntity.tags,
      createdBy = UserId(simulationEntity.createdBy),
      createdAt = simulationEntity.createdAt,
      updatedAt = simulationEntity.updatedAt
    )

object SimulationRepositoryLive:
  val live: ZLayer[DefaultDbContext & PostgresDataSource, Nothing, SimulationRepository] =
    ZLayer.fromFunction(SimulationRepositoryLive(_, _))
