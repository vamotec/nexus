package app.mosia.nexus.application.service.user

import app.mosia.nexus.domain.event.UserRegistered
import app.mosia.nexus.domain.model.user.{User, UserId}
import app.mosia.nexus.domain.repository.UserRepository
import app.mosia.nexus.infra.error.*
import app.mosia.nexus.infra.messaging.kafka.KafkaProducerService
import app.mosia.nexus.infra.persistence.redis.RedisService
import org.mindrot.jbcrypt.BCrypt
import zio.*

import java.sql.SQLException
import java.util.UUID

final class UserServiceLive(userRepo: UserRepository, kafkaProducer: KafkaProducerService, redis: RedisService)
    extends UserService:
  override def createUser(email: String, plainPassword: String): AppTask[User] =
    for
      // Check if username already exists
      _ <- userRepo.findByEmail(email).flatMap {
        case Some(_) => ZIO.fail(UsernameAlreadyExists)
        case None => ZIO.succeed(())
      }
      // Hash the password
      hashedPassword <- ZIO.succeed(BCrypt.hashpw(plainPassword, BCrypt.gensalt()))
      // Create the user object
      now <- Clock.instant
      newUser = User(
        id = ???,
        email = ???,
        name = ???,
        avatar = ???,
        organization = ???,
        role = ???,
        isActive = ???,
        emailVerified = ???,
        quota = ???,
        createdAt = ???,
        updatedAt = ???,
        lastLoginAt = ???
      )
      // Persist the user
      createdUser <- userRepo.create(newUser, plainPassword)
      // Publish the event to Kafka in the background
      _ <- kafkaProducer
        .publish(
          topic = "user-events",
          key = createdUser.id.toString,
          value = UserRegistered(
            userId = createdUser.id.value.toString,
            username = createdUser.name,
            email = createdUser.email,
            registeredAt = createdUser.createdAt
          )
        )
        .catchAll(e => ZIO.logError(s"Failed to publish user registration event: $e"))
        .fork
    yield createdUser

  override def findByEmail(email: String): AppTask[Option[User]] =
    userRepo.findByEmail(email)

  override def findById(id: UserId): AppTask[Option[User]] =
    val cacheKey = s"user-cache:${id.value}"
    for
      // 1. 首先尝试从缓存获取
      cachedUserOpt <- redis.get[User](cacheKey).orDie // .orDie 将缓存的潜在错误视为不可恢复的缺陷，简化错误处理
      userOpt <- cachedUserOpt match
        // 2a. 缓存命中，直接返回
        case Some(user) => ZIO.logInfo(s"Cache hit for user ${id.value}") *> ZIO.succeed(Some(user))
        // 2b. 缓存未命中，从数据库获取
        case None =>
          for
            _ <- ZIO.logInfo(s"Cache miss for user ${id.value}, fetching from DB")
            dbUserOpt <- userRepo.findById(id.value)
            // 3. 如果数据库中存在，则将其写入缓存（在后台执行），然后再返回
            _ <- dbUserOpt match
              case Some(user) => redis.set(cacheKey, user, Some(1.hour)).orDie.fork
              case None => ZIO.unit
          yield dbUserOpt
    yield userOpt

  override def authenticate(email: String, plainPassword: String): AppTask[Option[User]] =
    for
      maybeHash <- userRepo.findPasswordHashByEmail(email)
      result <- maybeHash match
        case Some(hash) if BCrypt.checkpw(plainPassword, hash) =>
          userRepo.findByEmail(email)
        case _ =>
          ZIO.succeed(None)
    yield result

object UserServiceLive:
  val live: ZLayer[UserRepository & KafkaProducerService & RedisService, Nothing, UserService] =
    ZLayer.fromFunction(new UserServiceLive(_, _, _))
