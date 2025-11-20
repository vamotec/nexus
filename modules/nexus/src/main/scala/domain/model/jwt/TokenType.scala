package app.mosia.nexus
package domain.model.jwt

enum TokenType:
  case Access, Refresh, Session, Omniverse
