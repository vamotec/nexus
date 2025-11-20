package app.mosia.nexus
package domain.model.device

enum DevicePlatform:
  case iOS, Android, Web

object DevicePlatform:
  def fromString(s: String): Option[DevicePlatform] = s.toLowerCase match
    case "ios" => Some(iOS)
    case "android" => Some(Android)
    case "web" => Some(Web)
    case _ => None
