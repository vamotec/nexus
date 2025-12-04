package app.mosia.nexus
package domain.config.notification

/** 通知配置（邮件 + 短信）*/
case class NotificationConfig(
  email: EmailConfig,
  sms: SmsConfig
)
