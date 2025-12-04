package app.mosia.nexus
package domain.model.user

import zio.json.JsonCodec

enum Provider derives JsonCodec:
  case Google, GitHub

object Provider:
  def fromString(s: String): Provider =
    s.toLowerCase match
      case "google" => Google
      case "github" => GitHub
      case _ => throw new IllegalArgumentException(s"Unknow provider: $s")
    
  def toStr(provider: Provider): String = provider match
    case Google => "google"
    case GitHub => "github"