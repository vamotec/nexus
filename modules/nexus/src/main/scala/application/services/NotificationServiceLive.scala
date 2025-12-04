package app.mosia.nexus
package application.services

import domain.error.AppTask
import domain.event.{EmailNotificationEvent, SmsNotificationEvent}
import domain.model.verification.VerificationCodeType
import domain.services.app.NotificationService
import domain.services.infra.{DomainEventPublisher, VerificationCodeService}

import zio.*

import java.util.UUID

/**
 * 📊 架构全景图
 *
 * ┌─────────────────────────────────────────────────────────────────┐
 * │                        Presentation 层                           │
 * │  AuthEndpoint.register → NotificationService                    │
 * └─────────────────────────┬───────────────────────────────────────┘
 * │
 * ┌─────────────────────────▼───────────────────────────────────────┐
 * │                       Application 层                             │
 * │  NotificationServiceLive (业务编排)                             │
 * │    ├─→ VerificationCodeService.generate()  (生成验证码)         │
 * │    └─→ DomainEventPublisher.publish()      (发布事件)           │
 * └─────────────────────────┬───────────────────────────────────────┘
 * │
 * ┌───────────────┴───────────────┐
 * │                               │
 * ▼                               ▼
 * ┌─────────────────────┐         ┌─────────────────────┐
 * │ VerificationCodeService│         │  DomainEventPublisher│
 * │  (Redis 存储验证码)   │         │  (发布到 RabbitMQ)   │
 * └─────────────────────┘         └──────────┬──────────┘
 * │
 * ┌────────────────▼────────────────┐
 * │        RabbitMQ Queue           │
 * │  nexus.notifications.email      │
 * └────────────┬────────────────────┘
 * │
 * ┌──────────────────────────────────────▼─────────────────────────┐
 * │                      Infrastructure 层                          │
 * │  EmailNotificationConsumer (消息消费者)                         │
 * │    └─→ EmailService.sendVerificationCode()  (实际发送邮件)     │
 * └────────────────────────────────────────────────────────────────┘
 * │
 * ▼
 * SMTP 服务器 → 用户邮箱
 */

/** 通知服务实现
  *
  * 整合验证码生成和通知发送
  *
  * @param verificationCodeService
  *   验证码服务
  * @param eventPublisher
  *   事件发布器
  */
private final class NotificationServiceLive(
  verificationCodeService: VerificationCodeService,
  eventPublisher: DomainEventPublisher
) extends NotificationService:

  /** 发送邮箱验证码 返回token给前端*/
  override def sendEmailVerificationCode(email: String, codeType: VerificationCodeType): AppTask[String] =
    for
      // 1. 生成验证码
      verificationCode <- verificationCodeService.generate(email, codeType)

      // 2. 发布邮件通知事件（自动路由到 RabbitMQ）
      _ <- eventPublisher.publish(
        EmailNotificationEvent(
          eventId = UUID.randomUUID().toString,
          to = email,
          subject = s"【Nexus】${getPurpose(codeType)}验证码",
          body = "", // HTML 内容由 EmailService 生成
          templateId = Some("verification-code"),
          templateData = Some(
            Map(
              "code" -> verificationCode.code,
              "purpose" -> getPurpose(codeType)
            )
          )
        )
      )

      _ <- ZIO.logInfo(s"Verification code sent to email: $email (type=$codeType)")
    yield verificationCode.token

  /** 发送短信验证码 */
  override def sendSmsVerificationCode(phone: String, codeType: VerificationCodeType): AppTask[String] =
    for
      // 1. 生成验证码
      verificationCode <- verificationCodeService.generate(phone, codeType, validMinutes = 5)

      // 2. 发布短信通知事件（自动路由到 RabbitMQ）
      _ <- eventPublisher.publish(
        SmsNotificationEvent(
          eventId = UUID.randomUUID().toString,
          to = phone,
          message = s"您的验证码是：${verificationCode.code}，5分钟内有效。",
          templateId = Some("verification-code"),
          templateData = Some(Map("code" -> verificationCode.code))
        )
      )

      _ <- ZIO.logInfo(s"Verification code sent to phone: $phone (type=$codeType)")
    yield verificationCode.code

  /** 验证邮箱验证码 */
  override def verifyEmailCode(email: String, code: String, token: String, codeType: VerificationCodeType): AppTask[Boolean] =
    verificationCodeService.verify(email, code, token, codeType)

  /** 验证短信验证码 */
  override def verifySmsCode(phone: String, code: String, token: String, codeType: VerificationCodeType): AppTask[Boolean] =
    verificationCodeService.verify(phone, code, token, codeType)

  /** 发送欢迎邮件 */
  override def sendWelcomeEmail(email: String, username: String): AppTask[Unit] =
    eventPublisher.publish(
      EmailNotificationEvent(
        eventId = UUID.randomUUID().toString,
        to = email,
        subject = "欢迎加入 Nexus！",
        body = s"""
          <h1>欢迎，$username！</h1>
          <p>感谢您注册 Nexus 平台。</p>
          <p>立即开始您的机器人仿真之旅！</p>
        """,
        templateId = Some("welcome")
      )
    )

  /** 获取验证码用途描述 */
  private def getPurpose(codeType: VerificationCodeType): String =
    codeType match
      case VerificationCodeType.Email         => "邮箱验证"
      case VerificationCodeType.Sms           => "手机验证"
      case VerificationCodeType.Login         => "登录"
      case VerificationCodeType.Register      => "注册"
      case VerificationCodeType.ResetPassword => "重置密码"

object NotificationServiceLive:
  val live: ZLayer[VerificationCodeService & DomainEventPublisher, Nothing, NotificationService] =
    ZLayer.fromFunction(NotificationServiceLive(_, _))
