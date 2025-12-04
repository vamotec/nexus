package app.mosia.nexus
package infrastructure.persistence

import domain.error.*
import domain.model.session.SessionId
import domain.model.training.*
import domain.model.user.UserId
import domain.repository.TrainingRepository
import infrastructure.persistence.BaseSource.PostgresDataSource
import infrastructure.persistence.rows.TrainingJobRow

import io.getquill.*
import io.getquill.context.json.*
import zio.*

import java.time.Instant

final class TrainingRepositoryLive(ctx: DefaultDbContext, dataSource: PostgresDataSource)
    extends BaseRepository(ctx, dataSource)
    with TrainingRepository:

  import ctx.*
  
  private inline def trainingJobSchema = querySchema[TrainingJobRow]("training_jobs")

  override def save(job: TrainingJob): AppTask[Unit] = transaction:
    ZIO.attempt:
      run(quote {
        trainingJobSchema.insertValue(lift(toRow(job)))
      })

  override def update(job: TrainingJob): AppTask[Unit] = transaction:
    ZIO.attempt:
      val row = toRow(job)
      run(quote {
        trainingJobSchema
          .filter(_.id == lift(row.id))
          .updateValue(lift(row))
      })

  override def findById(id: TrainingJobId): AppTask[Option[TrainingJob]] = runQuery:
    run(quote {
      trainingJobSchema.filter(_.id == lift(id.value))
    }).map(_.headOption.map(toDomain))

  override def findBySessionId(sessionId: SessionId): AppTask[List[TrainingJob]] = runQuery:
    run(quote {
      trainingJobSchema
        .filter(_.sessionId == lift(sessionId.value))
        .sortBy(_.createdAt)(using Ord.desc)
    }).map(_.map(toDomain))

  override def findByUserId(userId: UserId, limit: Int): AppTask[List[TrainingJob]] = runQuery:
    run(quote {
      trainingJobSchema
        .filter(_.userId == lift(userId.value))
        .sortBy(_.createdAt)(using Ord.desc)
        .take(lift(limit))
    }).map(_.map(toDomain))

  private def toRow(domain: TrainingJob): TrainingJobRow =
    TrainingJobRow(
      id = domain.id.value,
      sessionId = domain.sessionId.value,
      userId = domain.userId.value,
      algorithm = domain.algorithm.toString,
      config = JsonbValue(domain.config.toJsonAst),
      status = domain.status.toString,
      progressPercentage = domain.progress.percentage,
      currentEpoch = domain.progress.currentEpoch,
      totalEpochs = domain.progress.totalEpochs,
      currentReward = domain.progress.currentReward,
      averageReward = domain.progress.averageReward,
      loss = domain.progress.loss,
      trainingInstanceId = domain.assignment.instanceId.map(_.value),
      gpuIds = domain.assignment.gpuIds,
      modelPath = domain.result.map(_.modelPath),
      checkpointPath = domain.result.map(_.checkpointPath),
      finalReward = domain.result.map(_.finalReward),
      finalMetrics = domain.result.map(v => JsonbValue(v.finalMetrics)),
      errorMessage = None,
      createdAt = domain.createdAt,
      startedAt = domain.startedAt,
      completedAt = domain.completedAt
    )

  private def toDomain(row: TrainingJobRow): TrainingJob =
    val progress = TrainingProgress(
      currentEpoch = row.currentEpoch,
      totalEpochs = row.totalEpochs,
      currentReward = row.currentReward,
      averageReward = row.currentReward, // 简化处理
      loss = Some(0.0),
      episodeLength = 0,
      metrics = Map.empty
    )

    val result =
      if row.modelPath.isDefined && row.finalReward.isDefined then
        Some(
          TrainingResult(
            modelPath = row.modelPath.get,
            checkpointPath = row.modelPath.get, // 使用 modelPath 作为 checkpoint
            finalReward = row.finalReward.get,
            finalMetrics = zio.json.ast.Json.Obj()
          )
        )
      else None

    TrainingJob(
      id = TrainingJobId(row.id),
      sessionId = SessionId(row.sessionId),
      userId = UserId(row.userId),
      algorithm = Some(RLAlgorithm.fromString(row.algorithm)),
      config = TrainingConfig.fromJsonAst(row.config.value),
      status = TrainingStatus.fromString(row.status),
      assignment = TrainingInstanceAssignment(
        instanceId = row.trainingInstanceId.map(v => TrainingInstanceId(v)),
        gpuIds = row.gpuIds,
        assignedAt = row.createdAt
      ),
      progress = progress,
      result = result,
      createdAt = row.createdAt,
      startedAt = row.startedAt,
      completedAt = row.completedAt
    )

object TrainingRepositoryLive:
  val live: ZLayer[DefaultDbContext & PostgresDataSource, Nothing, TrainingRepository] =
    ZLayer.fromFunction(TrainingRepositoryLive(_, _))
