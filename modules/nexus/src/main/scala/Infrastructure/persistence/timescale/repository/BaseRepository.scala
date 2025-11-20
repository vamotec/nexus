package app.mosia.nexus
package infrastructure.persistence.timescale.repository

import infrastructure.persistence.BaseSource.TimescaleDataSource
import domain.error.*

import javax.sql.DataSource
import zio.*
// ============ Base Repository ============
abstract class BaseRepository(
  protected val ctx: TimescaleDbContext,
  protected val dataSource: TimescaleDataSource
):

  import ctx.*

  /** 执行查询并自动提供 DataSource
    */
  protected def runQuery[A](query: ZIO[DataSource, Throwable, A]): AppTask[A] =
    query.provide(ZLayer.succeed(dataSource.ds)).mapError(toAppError)

  /** 执行事务（简化版，只处理 DataSource）
    */
  protected def transaction[A](
    query: ZIO[DataSource, Throwable, A]
  ): AppTask[A] =
    ctx
      .transaction(query)
      .provide(ZLayer.succeed(dataSource.ds))
      .mapError(toAppError)

  /** 执行事务（高级版，支持额外的环境 R）
    */
  protected def transactionR[R, A](
    query: ZIO[R & DataSource, Throwable, A]
  ): ZIO[R, AppError, A] =
    ctx
      .transaction(query)
      .provideSomeLayer[R](ZLayer.succeed(dataSource.ds))
      .mapError(toAppError)
