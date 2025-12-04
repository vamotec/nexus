package app.mosia.nexus
package application.dto.response.metrics

enum HealthStatus:
  case Excellent // FPS > 55
  case Good // FPS > 45
  case Fair // FPS > 30
  case Poor // FPS <= 30
