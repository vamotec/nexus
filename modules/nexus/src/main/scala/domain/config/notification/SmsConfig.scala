package app.mosia.nexus
package domain.config.notification

/** 阿里云短信配置
  *
  * @param accessKeyId
  *   AccessKey ID
  * @param accessKeySecret
  *   AccessKey Secret
  * @param signName
  *   短信签名
  * @param templateCode
  *   短信模板代码（验证码模板）
  * @param endpoint
  *   API 端点（默认阿里云）
  */
case class SmsConfig(
  accessKeyId: String,
  accessKeySecret: String,
  signName: String,
  templateCode: String,
  endpoint: String = "dysmsapi.aliyuncs.com"
)
