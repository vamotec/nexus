package app.mosia.nexus
package presentation.graphql.schema

import domain.error.CalTask
import application.dto.request.training.CreateTrainingRequest
import application.dto.response.training.{TrainingJobResponse, TrainingProgressResponse}
import domain.model.training.TrainingJobId
import domain.model.user.UserId

import caliban.schema.{ArgBuilder, Schema as Cs}
import sttp.tapir.Schema
import zio.json.*
import zio.*
import zio.json.ast.Json
import zio.stream.ZStream

object TrainingSchema:

  /** 查询参数 */
  case class TrainingJobArgs(
    jobId: TrainingJobId
  ) derives Cs.SemiAuto,
        ArgBuilder

  case class MyTrainingJobsArgs(
    userId: UserId,
    limit: Int = 10
  ) derives Cs.SemiAuto,
        ArgBuilder

  case class TrainingQueries(
    trainingJob: TrainingJobId => CalTask[TrainingJobResponse],
    myTrainingJobs: Int => CalTask[List[TrainingJobResponse]],
    trainingProgress: TrainingJobId => CalTask[TrainingProgressResponse]
  ) derives Cs.SemiAuto

  /** 变更参数 */
  case class CreateTrainingJobArgs(
    userId: UserId,
    request: CreateTrainingRequest
  ) derives Cs.SemiAuto,
        ArgBuilder

  case class TrainingMutations(
    createTrainingJob: CreateTrainingRequest => CalTask[TrainingJobResponse],
    stopTrainingJob: TrainingJobId => CalTask[TrainingJobResponse]
  ) derives Cs.SemiAuto

  /** 订阅参数 */
  case class TrainingProgressStreamArgs(
    jobId: TrainingJobId,
    intervalMs: Option[Int] = Some(2000) // 默认 2 秒推送一次
  ) derives Cs.SemiAuto,
        ArgBuilder

  /** GraphQL 订阅 */
  case class TrainingSubscriptions(
    // 训练进度流订阅（实时进度更新）
    trainingProgressStream: TrainingProgressStreamArgs => ZStream[Any, Throwable, TrainingProgressResponse]
  ) derives Cs.SemiAuto
