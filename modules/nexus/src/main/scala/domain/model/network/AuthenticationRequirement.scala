package app.mosia.nexus
package domain.model.network

/** 认证要求 */
enum AuthenticationRequirement:
  case Certificate, Token, Basic, None
