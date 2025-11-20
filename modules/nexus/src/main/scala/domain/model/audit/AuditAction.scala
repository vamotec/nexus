package app.mosia.nexus
package domain.model.audit

enum AuditAction:
  case Login
  case Logout
  case LoginFailed
  case PasswordChanged
  case PasswordResetRequested
  case PasswordReset
  case UserCreated
  case UserUpdated
  case UserDeleted
  case ProjectCreated
  case ProjectUpdated
  case ProjectDeleted
  case SimulationStarted
  case SimulationStopped
  case SessionCreated
  case FileUploaded
  case FileDeleted
  case SettingsChanged
  case DeviceRegistered
  case DeviceRemoved
  case ApiKeyCreated
  case ApiKeyRevoked

object AuditAction:
  def fromString(s: String): Option[AuditAction] =
    values.find(_.toString.toLowerCase == s.toLowerCase)
