package app.mosia.nexus
package infrastructure.persistence.postgres.repository

import infrastructure.persistence.BaseSource.PostgresDataSource
import domain.error.*

import zio.*

import javax.sql.DataSource

// ============ Base Repository ============
abstract class BaseRepository(
  protected val ctx: DefaultDbContext,
  protected val dataSource: PostgresDataSource
):

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

  protected def withTransaction[A](f: java.sql.Connection => A): AppTask[A] =
    ZIO
      .scoped {
        ZIO.acquireReleaseWith(
          ZIO.attempt {
            val conn = dataSource.ds.getConnection
            conn.setAutoCommit(false)
            conn
          }
        )(conn =>
          ZIO.succeed(
            try conn.close()
            catch {
              case _: Exception => ()
            }
          )
        )(conn =>
          ZIO
            .attempt(f(conn))
            .tapBoth(
              _ => ZIO.attempt(conn.rollback()),
              _ => ZIO.attempt(conn.commit())
            )
        )
      }
      .mapError(toAppError)
