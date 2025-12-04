package app.mosia.nexus
package infrastructure.notification

import domain.config.AppConfig
import domain.error.*
import domain.services.infra.SmsService

import zio.*
import zio.http.*
import zio.json.*

import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.time.format.DateTimeFormatter
import java.time.{Instant, ZoneId}
import java.util.UUID
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import java.util.Base64

/** 阿里云短信服务实现
  *
  * 使用阿里云 OpenAPI 发送短信
  *
  * @param config
  *   应用配置
  * @param client
  *   HTTP 客户端
  */
private final class AliyunSmsService(config: AppConfig, client: Client) extends SmsService:

  private val smsConfig = config.notification.sms

  /** 发送验证码短信 */
  override def sendVerificationCode(phone: String, code: String): AppTask[Unit] =
    (for
      // 1. 构建请求参数
      params <- ZIO.succeed(Map(
        "PhoneNumbers" -> phone,
        "SignName" -> smsConfig.signName,
        "TemplateCode" -> smsConfig.templateCode,
        "TemplateParam" -> s"""{"code":"$code"}"""
      ))

      // 2. 发送短信
      _ <- sendSms(params)

      // 3. 记录日志
      _ <- ZIO.logInfo(s"Verification code SMS sent successfully to $phone")
    yield ()).mapError(toAppError)

  /** 发送通知短信 */
  override def sendNotification(phone: String, message: String): AppTask[Unit] =
    (for
      params <- ZIO.succeed(Map(
        "PhoneNumbers" -> phone,
        "SignName" -> smsConfig.signName,
        "TemplateCode" -> smsConfig.templateCode,
        "TemplateParam" -> s"""{"message":"$message"}"""
      ))

      _ <- sendSms(params)
      _ <- ZIO.logInfo(s"Notification SMS sent successfully to $phone")
    yield ()).mapError(toAppError)

  /** 发送短信（阿里云 API）*/
  private def sendSms(templateParams: Map[String, String]): Task[Unit] =
    for
      // 1. 构建公共参数
      timestamp <- ZIO.succeed(DateTimeFormatter.ISO_INSTANT.format(Instant.now()))
      nonce = UUID.randomUUID().toString

      commonParams = Map(
        "AccessKeyId" -> smsConfig.accessKeyId,
        "Action" -> "SendSms",
        "Format" -> "JSON",
        "RegionId" -> "cn-hangzhou",
        "SignatureMethod" -> "HMAC-SHA1",
        "SignatureNonce" -> nonce,
        "SignatureVersion" -> "1.0",
        "Timestamp" -> timestamp,
        "Version" -> "2017-05-25"
      )

      // 2. 合并所有参数
      allParams = commonParams ++ templateParams

      // 3. 签名
      signature <- generateSignature(allParams)

      // 4. 构建请求 URL
      url = s"https://${smsConfig.endpoint}/"

      queryParams = (allParams + ("Signature" -> signature))
        .map { case (k, v) => s"$k=${URLEncoder.encode(v, StandardCharsets.UTF_8.name())}" }
        .mkString("&")

      fullUrl = s"$url?$queryParams"

      // 5. 发送 HTTP 请求
      response <- ZIO.scoped {
        client
          .url(URL.decode(fullUrl).toOption.get)
          .get("")
      }

      // 6. 检查响应
      _ <- response.status match
        case Status.Ok =>
          ZIO.logDebug(s"Aliyun SMS API response: ${response.status}")
        case _ =>
          ZIO.fail(new RuntimeException(s"Aliyun SMS API error: ${response.status}"))
    yield ()

  /** 生成阿里云 API 签名 */
  private def generateSignature(params: Map[String, String]): Task[String] =
    ZIO.attempt {
      // 1. 参数排序
      val sortedParams = params.toSeq.sortBy(_._1)

      // 2. 构建待签名字符串
      val canonicalizedQueryString = sortedParams
        .map { case (k, v) =>
          s"${percentEncode(k)}=${percentEncode(v)}"
        }
        .mkString("&")

      val stringToSign = s"GET&${percentEncode("/")}&${percentEncode(canonicalizedQueryString)}"

      // 3. 计算 HMAC-SHA1 签名
      val key = s"${smsConfig.accessKeySecret}&"
      val mac = Mac.getInstance("HmacSHA1")
      mac.init(new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA1"))
      val signData = mac.doFinal(stringToSign.getBytes(StandardCharsets.UTF_8))

      // 4. Base64 编码
      Base64.getEncoder.encodeToString(signData)
    }

  /** URL 百分号编码 */
  private def percentEncode(value: String): String =
    URLEncoder
      .encode(value, StandardCharsets.UTF_8.name())
      .replace("+", "%20")
      .replace("*", "%2A")
      .replace("%7E", "~")

object AliyunSmsService:
  val live: ZLayer[AppConfig & Client, Nothing, SmsService] =
    ZLayer.fromFunction(AliyunSmsService(_, _))
