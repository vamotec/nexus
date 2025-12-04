package app.mosia.nexus
package application.services

import domain.error.*
import domain.event.UserRegistered
import domain.model.user.UserRole.Viewer
import domain.model.user.{OAuthProvider, Provider, Quota, User, UserId}
import domain.repository.{OAuthProviderRepository, UserRepository}
import domain.services.app.UserService
import domain.services.infra.{DomainEventPublisher, RedisService}

import org.mindrot.jbcrypt.BCrypt
import zio.*
import zio.json.*

import java.time.Instant
import java.util.UUID

final class UserServiceLive(
  userRepo: UserRepository, 
  eventPublisher: DomainEventPublisher, 
  redis: RedisService,
  oauthRepo: OAuthProviderRepository,
) extends UserService:
  override def createUser(email: String, plainPassword: String, name: Option[String]): AppTask[User] =
    for
      // Check if username already exists
      _ <- userRepo.findByEmail(email).flatMap {
        case Some(_) => ZIO.fail(AlreadyExists("User email", email))
        case None => ZIO.succeed(())
      }
      // Hash the password
      hashedPassword <- ZIO.succeed(BCrypt.hashpw(plainPassword, BCrypt.gensalt()))
      // Create the user object
      now <- Clock.instant
      newUser = User(
        id = UserId.generate(),
        email = email,
        name = name.getOrElse(email.split('@')(0)), // 使用邮箱前缀作为默认用户名
        avatar = None,
        role = Viewer, // 新用户默认为 Viewer 角色
        isActive = false, // 需要邮箱验证后才激活
        emailVerified = false, // 新用户邮箱未验证
        quota = Quota.free, // 免费用户默认配额：5GB存储，最多2个组织
        createdAt = now,
        updatedAt = now,
        lastLoginAt = None
      )
      // Persist the user
      _ <- userRepo.create(newUser, hashedPassword)
      // Publish domain event in the background
      event = UserRegistered(
        userId = newUser.id.value.toString,
        username = newUser.name,
        email = newUser.email,
        registeredAt = newUser.createdAt
      )
      _ <- eventPublisher
        .publish(event)
        .catchAll(e => ZIO.logError(s"Failed to publish user registration event: $e"))
        .fork
    yield newUser

  override def createUserWithOAuth(
                                    email: String,
                                    username: Option[String],
                                    provider: Provider,
                                    providerUserId: String,
                                    providerEmail: Option[String]
                                  ): AppTask[User] =
    for
      // Hash the uuid for oauth user
      hashedPassword <- ZIO.succeed(BCrypt.hashpw(UUID.randomUUID().toString, BCrypt.gensalt()))
      now <- Clock.instant
      // Create the user object
      newUser = User(
        id = UserId.generate(),
        email = email,
        name = username.getOrElse(email.split('@')(0)),
        avatar = None,
        role = Viewer, // 新用户默认为 Viewer 角色
        isActive = true, // oauth直接激活
        emailVerified = true, // oauth不需要验证邮箱
        quota = Quota.free,
        createdAt = now,
        updatedAt = now,
        lastLoginAt = None
      )
      // Persist the user
      _ <- userRepo.create(newUser, hashedPassword)

      // 绑定OAuth
      _ <- oauthRepo.create(OAuthProvider(
        id = UUID.randomUUID(),
        userId = newUser.id,
        provider = provider,
        providerUserId = providerUserId,
        providerEmail = providerEmail,
        linkedAt = now,
        lastUsedAt = Some(now)
      ))
    yield newUser

  override def linkProvider(
                            userId: UserId,
                            provider: Provider,
                            providerUserId: String,
                            providerEmail: Option[String]
                          ): AppTask[OAuthProvider] =
    val oauthProvider = OAuthProvider(
      id = UUID.randomUUID(),
      userId = userId,
      provider = provider,
      providerUserId = providerUserId,
      providerEmail = providerEmail,
      linkedAt = Instant.now(),
      lastUsedAt = Some(Instant.now())
    )
    oauthRepo.create(oauthProvider)
    
  override def findByEmail(email: String): AppTask[Option[User]] =
    userRepo.findByEmail(email)

  override def findById(id: String): AppTask[Option[User]] =
    val cacheKey = s"user-cache:$id"
    for
      // 1. 首先尝试从缓存获取
      cachedUserOpt <- redis.get(cacheKey).flatMap {
        case Some(str) =>
          ZIO.fromEither(str.fromJson[User])
            .mapError(err => InvalidInput("user", s"invalid JSON: $err"))
            .map(Some(_))
        case None =>
          ZIO.succeed(None)
      }.orDie // .orDie 将缓存的潜在错误视为不可恢复的缺陷，简化错误处理
      userOpt <- cachedUserOpt match
        // 2a. 缓存命中，直接返回
        case Some(user) => ZIO.succeed(Some(user))
        // 2b. 缓存未命中，从数据库获取
        case None =>
          for
            userId <- UserId.fromString(id)
            dbUserOpt <- userRepo.findById(userId)
            // 3. 如果数据库中存在，则将其写入缓存（在后台执行），然后再返回
            _ <- dbUserOpt match
              case Some(user) => redis.set(cacheKey, user.toJson, 1.hour.getSeconds).orDie.fork
              case None => ZIO.unit
          yield dbUserOpt
    yield userOpt

  override def authenticate(email: String, plainPassword: String): AppTask[Option[User]] =
    for
      _ <- ZIO.logInfo(s"[Authenticate] Attempting authentication for email: $email")

      maybeHash <- userRepo.findPasswordHashByEmail(email)
        .tap(hash => ZIO.logDebug(s"[Authenticate] Password hash found: ${hash.isDefined}"))

      result <- maybeHash match
        case Some(hash) if BCrypt.checkpw(plainPassword, hash) =>
          ZIO.logInfo(s"[Authenticate] Password verified successfully for $email") *>
            userRepo.findByEmail(email)
              .tap(user => ZIO.logInfo(s"[Authenticate] User retrieved: ${user.isDefined}"))
        case Some(_) =>
          ZIO.logWarning(s"[Authenticate] Password verification failed for $email") *>
            ZIO.succeed(None)
        case None =>
          ZIO.logWarning(s"[Authenticate] No password hash found for $email") *>
            ZIO.succeed(None)

      _ <- result match
        case Some(user) => ZIO.logInfo(s"[Authenticate] Authentication successful for ${user.email}")
        case None => ZIO.logInfo(s"[Authenticate] Authentication failed for $email")
    yield result

  override def markEmailAsVerified(email: String): AppTask[Unit] =
    for
      // 1. 更新数据库中的邮箱验证状态
      _ <- userRepo.markEmailAsVerified(email)

      // 2. 清除缓存（如果用户信息被缓存）
      userOpt <- userRepo.findByEmail(email)
      _ <- userOpt match
        case Some(user) =>
          val cacheKey = s"user-cache:${user.id.value}"
          redis.del(cacheKey).orDie
        case None =>
          ZIO.unit

      _ <- ZIO.logInfo(s"Email verified for user: $email")
    yield ()

  override def resetPassword(email: String, newPassword: String): AppTask[Unit] =
    for
      // 1. 检查用户是否存在
      userOpt <- userRepo.findByEmail(email)
      _ <- userOpt match
        case None => ZIO.fail(NotFound("User", email))
        case Some(_) => ZIO.unit

      // 2. 哈希新密码
      hashedPassword <- ZIO.succeed(BCrypt.hashpw(newPassword, BCrypt.gensalt()))

      // 3. 更新数据库中的密码
      _ <- userRepo.updatePasswordByEmail(email, hashedPassword)

      // 4. 清除缓存
      _ <- userOpt match
        case Some(user) =>
          val cacheKey = s"user-cache:${user.id.value}"
          redis.del(cacheKey).orDie
        case None =>
          ZIO.unit

      _ <- ZIO.logInfo(s"Password reset for user: $email")
    yield ()

object UserServiceLive:
  val live: ZLayer[UserRepository & DomainEventPublisher & RedisService & OAuthProviderRepository, Nothing, UserService] =
    ZLayer.fromFunction(new UserServiceLive(_, _, _, _))
