package app.mosia.nexus
package infrastructure.persistence

import domain.error.AppTask
import domain.model.user.{RefreshToken, UserId}
import domain.repository.RefreshTokenRepository
import infrastructure.persistence.BaseSource.PostgresDataSource
import infrastructure.persistence.rows.RefreshTokenRow

import io.getquill.*
import zio.*

import java.util.UUID
import javax.sql.DataSource

final class RefreshTokenRepositoryLive(ctx: DefaultDbContext, dataSource: PostgresDataSource)
    extends BaseRepository(ctx, dataSource)
    with RefreshTokenRepository:
  
  import ctx.*
  
  private inline def refreshTokenSchema = querySchema[RefreshTokenRow]("refresh_tokens")

  override def save(refreshToken: RefreshToken): AppTask[Unit] = runQuery:
    ZIO.attempt:
      val entity = toRefreshTokenRow(refreshToken)
      run(quote {
        refreshTokenSchema.insertValue(lift(entity))
      })

  override def findByToken(token: String): AppTask[Option[RefreshToken]] = runQuery:
    run(quote {
      refreshTokenSchema
        .filter(_.token == lift(token))
    }).map(_.headOption.map(toRefreshToken))

  override def markAsRevoked(token: String): AppTask[Unit] = runQuery:
    ZIO.attempt:
      run(quote {
        refreshTokenSchema
          .filter(_.token == lift(token))
          .update(_.isRevoked -> lift(Option(true)))
      })

  override def delete(token: String): AppTask[Unit] = runQuery:
    ZIO.attempt:
      run(quote {
        refreshTokenSchema
          .filter(_.token == lift(token))
          .delete
      })

  // ============ 转换方法 ============
  private def toRefreshTokenRow(token: RefreshToken): RefreshTokenRow =
    RefreshTokenRow(
      id = token.id, // 数据库自动生成
      token = token.token,
      userId = UUID.fromString(token.userId),
      expiresAt = token.expiresAt,
      deviceInfo = None, // RefreshToken 领域模型中没有 deviceInfo 字段
      isRevoked = token.isRevoked,
      createdAt = None // 数据库自动生成
    )

  private def toRefreshToken(entity: RefreshTokenRow): RefreshToken =
    RefreshToken(
      id = entity.id, // 从 token 生成稳定的 UUID
      token = entity.token,
      userId = entity.userId.toString,
      expiresAt = entity.expiresAt,
      isRevoked = entity.isRevoked
    )

object RefreshTokenRepositoryLive:
  val live: ZLayer[DefaultDbContext & PostgresDataSource, Nothing, RefreshTokenRepository] =
    ZLayer.fromFunction(RefreshTokenRepositoryLive(_, _))
