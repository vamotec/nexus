package app.mosia.nexus
package infrastructure.notification

import domain.config.AppConfig
import domain.error.*
import domain.services.infra.EmailService

import zio.*

import java.util.Properties
import javax.mail.*
import javax.mail.internet.{InternetAddress, MimeMessage}

/** 基于 SMTP 的邮件服务实现
  *
  * 使用 JavaMail API 发送邮件
  *
  * @param config
  *   应用配置
  */
private final class SmtpEmailService(config: AppConfig) extends EmailService:

  private val emailConfig = config.notification.email

  /** 创建邮件会话 */
  private def createSession: Session =
    val props = new Properties()

    props.put("mail.smtp.host", emailConfig.host)
    props.put("mail.smtp.port", emailConfig.port.toString)
    props.put("mail.smtp.auth", "true")

    // SSL/TLS 配置
    if (emailConfig.port == 465) {
      props.put("mail.smtp.ssl.enable", "true")
      props.put("mail.smtp.socketFactory.port", "465")
      props.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory")
    } else if (emailConfig.port == 587) {
      props.put("mail.smtp.starttls.enable", "true")
      props.put("mail.smtp.starttls.required", "true")
    }

    // 调试（生产环境改为 false）
    props.put("mail.debug", "true")

    // 超时配置
    props.put("mail.smtp.connectiontimeout", "10000")
    props.put("mail.smtp.timeout", "10000")
    props.put("mail.smtp.writetimeout", "10000")

    Session.getInstance(props, new Authenticator {
      override protected def getPasswordAuthentication: PasswordAuthentication =
        new PasswordAuthentication(
          emailConfig.username,
          emailConfig.password
        )
    })

  /** 发送文本邮件 */
  override def sendText(to: String, subject: String, body: String): AppTask[Unit] =
    (for
      session <- ZIO.attempt(createSession)

      message <- ZIO.attempt {
        val msg = new MimeMessage(session)
        msg.setFrom(new InternetAddress(emailConfig.from, emailConfig.fromName))
        msg.setRecipients(Message.RecipientType.TO, to)
        msg.setSubject(subject)
        msg.setText(body, "UTF-8")
        msg
      }

      _ <- ZIO.attempt(Transport.send(message))
        .mapError(err => new RuntimeException(s"Failed to send email: ${err.getMessage}", err))

      _ <- ZIO.logInfo(s"Email sent successfully to $to: $subject")
    yield ()).mapError(toAppError)

  /** 发送 HTML 邮件 */
  override def sendHtml(to: String, subject: String, htmlBody: String): AppTask[Unit] =
    (for {
      _ <- ZIO.logInfo(s"Preparing to send email to: $to")
      _ <- ZIO.logInfo(s"SMTP config: ${emailConfig.host}:${emailConfig.port}")

      session <- ZIO.attempt(createSession)

      message <- ZIO.attempt {
        val msg = new MimeMessage(session)
        msg.setFrom(new InternetAddress(emailConfig.from, emailConfig.fromName))
        msg.setRecipients(Message.RecipientType.TO, to)
        msg.setSubject(subject)
        msg.setContent(htmlBody, "text/html; charset=UTF-8")
        msg.saveChanges()  // 👈 保存变更
        msg
      }

      _ <- ZIO.logInfo("Connecting to SMTP server...")

      _ <- ZIO.attemptBlocking {
        Transport.send(message)
      }.mapError(err =>
        new RuntimeException(s"Failed to send HTML email: ${err.getMessage}", err)
      )

      _ <- ZIO.logInfo(s"✅ HTML email sent successfully to $to: $subject")
    } yield ()).mapError(toAppError)

  /** 发送验证码邮件 */
  override def sendVerificationCode(to: String, code: String, purpose: String = "验证"): AppTask[Unit] =
    val subject = s"【Nexus】${purpose}验证码"
    val htmlBody = s"""
      <!DOCTYPE html>
      <html lang="zh-CN">
      <head>
        <meta charset="UTF-8">
        <meta name="viewport" content="width=device-width, initial-scale=1.0">
        <title>验证码</title>
      </head>
      <body style="font-family: Arial, sans-serif; line-height: 1.6; color: #333; max-width: 600px; margin: 0 auto; padding: 20px;">
        <div style="background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); padding: 30px; text-align: center; border-radius: 10px 10px 0 0;">
          <h1 style="color: white; margin: 0;">Nexus</h1>
        </div>
        <div style="background: #f9f9f9; padding: 30px; border-radius: 0 0 10px 10px;">
          <h2 style="color: #333; margin-top: 0;">您的${purpose}验证码</h2>
          <p style="font-size: 16px; color: #666;">您正在进行${purpose}操作，请使用以下验证码完成验证：</p>
          <div style="background: white; border: 2px dashed #667eea; border-radius: 8px; padding: 20px; text-align: center; margin: 25px 0;">
            <span style="font-size: 36px; font-weight: bold; color: #667eea; letter-spacing: 5px;">$code</span>
          </div>
          <p style="font-size: 14px; color: #999;">
            <strong>注意：</strong>验证码有效期为 <strong style="color: #667eea;">5分钟</strong>，请尽快完成验证。
          </p>
          <p style="font-size: 14px; color: #999;">
            如果这不是您本人的操作，请忽略此邮件。
          </p>
          <hr style="border: none; border-top: 1px solid #eee; margin: 30px 0;">
          <p style="font-size: 12px; color: #999; text-align: center;">
            此邮件由系统自动发送，请勿回复。<br>
            © ${java.time.Year.now().getValue} Nexus. All rights reserved.
          </p>
        </div>
      </body>
      </html>
    """

    sendHtml(to, subject, htmlBody)

object SmtpEmailService:
  val live: ZLayer[AppConfig, Nothing, EmailService] =
    ZLayer.fromFunction(SmtpEmailService(_))
