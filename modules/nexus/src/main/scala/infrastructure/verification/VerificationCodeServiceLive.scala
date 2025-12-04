package app.mosia.nexus
package infrastructure.verification

import domain.error.*
import domain.model.verification.{VerificationCode, VerificationCodeType}
import domain.services.infra.{RedisService, VerificationCodeService}

import zio.*
import zio.json.*

/** 基于 Redis 的验证码服务实现
  *
  * 验证码存储在 Redis 中，使用 TTL 自动过期
  *
  * Key 格式: verification_code:{type}:{target}
  * Value: JSON 格式的 VerificationCode
  *
  * @param redis
  *   Redis 服务
  */
private final class VerificationCodeServiceLive(redis: RedisService) extends VerificationCodeService:

  /** 生成并存储验证码 */
  override def generate(
    target: String,
    codeType: VerificationCodeType,
    validMinutes: Int = 5
  ): AppTask[VerificationCode] =
    for
      // 1. 限流检查
      _ <- checkRateLimit(target, codeType)

      // 2. 生成验证码
      verificationCode <- ZIO.succeed(VerificationCode.create(target, codeType, validMinutes))

      // 3. 构建 Redis Key
      key = buildKey(target, codeType)

      // 4. 序列化为 JSON
      json <- ZIO.succeed(verificationCode.toJson)

      // 5. 存储到 Redis（设置过期时间）
      _ <- redis.set(key, json)
      _ <- redis.expire(key, validMinutes * 60)

      // 6. 更新限流计数
      _ <- updateRateLimit(target, codeType)

      // 7. 记录日志
      _ <- ZIO.logInfo(s"Generated verification code for $target (type=$codeType, valid=${validMinutes}min)")
    yield verificationCode

  /** 验证并消费验证码 */
  override def verify(target: String, code: String, token: String, codeType: VerificationCodeType): AppTask[Boolean] =
    for
      // 1. 获取存储的验证码
      storedCodeOpt <- get(target, codeType)

      // 2. 验证码匹配检查
      isValid <- storedCodeOpt match
        case Some(storedCode) =>
          if storedCode.matches(code, token) then
            // 验证成功，删除验证码（一次性使用）
            delete(target, codeType) *>
              ZIO.logInfo(s"Verification code verified successfully for $target (type=$codeType)") *>
              ZIO.succeed(true)
          else
            // 验证失败
            ZIO.logWarning(s"Verification code mismatch for $target (type=$codeType)") *>
              ZIO.succeed(false)

        case None =>
          // 验证码不存在或已过期
          ZIO.logWarning(s"Verification code not found or expired for $target (type=$codeType)") *>
            ZIO.succeed(false)
    yield isValid

  /** 获取验证码（不消费）*/
  override def get(target: String, codeType: VerificationCodeType): AppTask[Option[VerificationCode]] =
    (for
      // 1. 构建 Redis Key
      key <- ZIO.succeed(buildKey(target, codeType))

      // 2. 从 Redis 获取
      jsonOpt <- redis.get(key)

      // 3. 反序列化
      codeOpt <- jsonOpt match
        case Some(json) =>
          ZIO
            .fromEither(json.fromJson[VerificationCode])
            .mapError(err => new RuntimeException(s"Failed to parse verification code: $err"))
            .map(Some(_))
        case None =>
          ZIO.succeed(None)
    yield codeOpt).mapError(toAppError)

  /** 删除验证码 */
  override def delete(target: String, codeType: VerificationCodeType): AppTask[Unit] =
    for
      key <- ZIO.succeed(buildKey(target, codeType))
      _ <- redis.del(key)
      _ <- ZIO.logDebug(s"Deleted verification code for $target (type=$codeType)")
    yield ()

  /** 构建 Redis Key
    *
    * 格式: verification_code:{type}:{target}
    */
  private def buildKey(target: String, codeType: VerificationCodeType): String =
    s"verification_code:${codeType.toString.toLowerCase}:$target"

  /** 限流检查
    *
    * 规则：
    * 1. 60秒内不能重复发送
    * 2. 每天最多发送5次
    */
  private def checkRateLimit(target: String, codeType: VerificationCodeType): AppTask[Unit] =
    for
      // 1. 检查时间间隔限制（60秒）
      lastSentKey <- ZIO.succeed(buildRateLimitKey(target, codeType, "last_sent"))
      lastSentExists <- redis.exists(lastSentKey)
      _ <- if lastSentExists then
        ZIO.fail(RateLimitExceeded(2, 60))
      else
        ZIO.unit

      // 2. 检查每日次数限制（5次）
      dailyCountKey = buildRateLimitKey(target, codeType, "daily_count")
      dailyCountStr <- redis.get(dailyCountKey)
      dailyCount = dailyCountStr.flatMap(_.toIntOption).getOrElse(0)
      _ <- if dailyCount >= 5 then
        ZIO.fail(RateLimitExceeded(6, 5))
      else
        ZIO.unit

      _ <- ZIO.logDebug(s"Rate limit check passed for $target (type=$codeType, dailyCount=$dailyCount)")
    yield ()

  /** 更新限流计数 */
  private def updateRateLimit(target: String, codeType: VerificationCodeType): AppTask[Unit] =
    for
      // 1. 设置最后发送时间（60秒过期）
      lastSentKey <- ZIO.succeed(buildRateLimitKey(target, codeType, "last_sent"))
      now <- Clock.instant
      _ <- redis.set(lastSentKey, now.toString)
      _ <- redis.expire(lastSentKey, 60)

      // 2. 增加每日计数（24小时过期）
      dailyCountKey = buildRateLimitKey(target, codeType, "daily_count")
      _ <- redis.incr(dailyCountKey)
      _ <- redis.expire(dailyCountKey, 24 * 3600)

      _ <- ZIO.logDebug(s"Updated rate limit for $target (type=$codeType)")
    yield ()

  /** 构建限流 Redis Key
    *
    * 格式: rate_limit:{suffix}:{type}:{target}
    */
  private def buildRateLimitKey(target: String, codeType: VerificationCodeType, suffix: String): String =
    s"rate_limit:$suffix:${codeType.toString.toLowerCase}:$target"

object VerificationCodeServiceLive:
  val live: ZLayer[RedisService, Nothing, VerificationCodeService] =
    ZLayer.fromFunction(VerificationCodeServiceLive(_))
