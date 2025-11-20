package app.mosia.nexus
package domain.model.network

/** 加密要求 */
enum EncryptionRequirement:
  case Required, Optional, None
