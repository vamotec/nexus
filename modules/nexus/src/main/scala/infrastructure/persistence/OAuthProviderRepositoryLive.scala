package app.mosia.nexus
package infrastructure.persistence

import domain.error.*
import domain.model.user.{OAuthProvider, Provider, UserId}
import domain.repository.OAuthProviderRepository
import domain.model.user.Provider.toStr
import infrastructure.persistence.BaseSource.PostgresDataSource
import infrastructure.persistence.rows.UserOauthProviderRow

import io.getquill.*
import zio.ZLayer

import java.time.Instant

private class OAuthProviderRepositoryLive(
  ctx: DefaultDbContext,
  dataSource: PostgresDataSource
) extends BaseRepository(ctx, dataSource)
  with OAuthProviderRepository:
  
  import ctx.*

  private inline def oauthProviders = querySchema[UserOauthProviderRow]("user_oauth_providers")

  override def findByProviderAndProviderId(
                                            provider: Provider, 
                                            providerId: String
                                          ): AppTask[Option[OAuthProvider]] = runQuery:
    val providerStr = Provider.toStr(provider)
    run(
      quote {
        oauthProviders
          .filter(op => op.provider == lift(providerStr) && op.providerUserId == lift(providerId))
      }
    ).map(_.headOption.map(toDomain))

  override def create(oauthProvider: OAuthProvider): AppTask[OAuthProvider] = transaction:
    val row = toRow(oauthProvider)
    run(
      quote {
        oauthProviders
          .insertValue(lift(row))
          .onConflictIgnore(_.provider, _.providerUserId) // 防止重复插入
      }
    ).as(oauthProvider)

  override def updateLastUsed(provider: Provider, providerId: String, timestamp: Instant): AppTask[Unit] = transaction:
    val providerStr = Provider.toStr(provider)
    run(
      quote {
        oauthProviders
          .filter(op => op.provider == lift(providerStr) && op.providerUserId == lift(providerId))
          .update(_.lastUsedAt -> Some(lift(timestamp)))
      }
    ).unit

  override def findByUserId(userId: UserId): AppTask[List[OAuthProvider]] = runQuery:
    run(
      quote {
        oauthProviders.filter(_.userId == lift(userId.value))
      }
    ).map(_.map(toDomain))

  def toRow(domain: OAuthProvider): UserOauthProviderRow =
    UserOauthProviderRow(
      id = domain.id, 
      userId = domain.userId.value,
      provider = toStr(domain.provider), 
      providerUserId = domain.providerUserId,
      providerEmail = domain.providerEmail, 
      linkedAt = domain.linkedAt, 
      lastUsedAt = domain.lastUsedAt
    )
    
  def toDomain(row: UserOauthProviderRow): OAuthProvider =
    OAuthProvider(
      id = row.id, 
      userId = UserId(row.userId), 
      provider = Provider.fromString(row.provider), 
      providerUserId = row.providerUserId, 
      providerEmail = row.providerEmail, 
      linkedAt = row.linkedAt, 
      lastUsedAt = row.lastUsedAt
    )
    
object OAuthProviderRepositoryLive:
  val live: ZLayer[DefaultDbContext & PostgresDataSource, Nothing, OAuthProviderRepository] =
    ZLayer.fromFunction(OAuthProviderRepositoryLive(_, _))
