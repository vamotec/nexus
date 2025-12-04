package app.mosia.nexus
package domain.model.verification

import zio.json.JsonCodec

import java.time.Instant

/** 验证码
  *
  * @param code
  *   验证码（6位数字）
  * @param target
  *   发送目标（邮箱或手机号）
  * @param codeType
  *   验证码类型
  * @param expiresAt
  *   过期时间
  * @param createdAt
  *   创建时间
  */
case class VerificationCode(
  code: String,
  token: String,
  target: String,
  codeType: VerificationCodeType,
  expiresAt: Instant,
  createdAt: Instant = Instant.now()
) derives JsonCodec:
  /** 验证码是否过期 */
  def isExpired: Boolean = Instant.now().isAfter(expiresAt)

  /** 验证码是否匹配 */
  def matches(inputCode: String, inputToken: String): Boolean =
    !isExpired && code == inputCode && token == inputToken

object VerificationCode:
  /** 生成随机6位数字验证码 */
  private def generateCode(): String =
    val random = scala.util.Random()
    (1 to 6).map(_ => random.nextInt(10)).mkString

  /** 创建验证码
    *
    * @param target
    *   发送目标
    * @param codeType
    *   验证码类型
    * @param validMinutes
    *   有效时长（分钟）
    */
  def create(target: String, codeType: VerificationCodeType, validMinutes: Int = 5): VerificationCode =
    val code = generateCode()
    val token = java.util.UUID.randomUUID().toString
    val now = Instant.now()
    val expiresAt = now.plusSeconds(validMinutes * 60)

    VerificationCode(
      code = code,
      token = token,
      target = target,
      codeType = codeType,
      expiresAt = expiresAt,
      createdAt = now
    )