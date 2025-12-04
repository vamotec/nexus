package app.mosia.nexus
package domain.model.training

import zio.*
import zio.json.*

enum RLAlgorithm derives JsonCodec:
  case PPO, SAC, TD3, DQN

object RLAlgorithm:
  def fromString(str: String): RLAlgorithm =
    str.toUpperCase match
      case "PPO" => RLAlgorithm.PPO
      case "SAC" => RLAlgorithm.SAC
      case "TD3" => RLAlgorithm.TD3
      case "DQN" => RLAlgorithm.DQN
      case _ => throw new IllegalArgumentException(s"Unknown RLAlgorithm: $str")

  def toString(algorithm: RLAlgorithm): String = algorithm match
    case RLAlgorithm.PPO => "PPO"
    case RLAlgorithm.SAC => "SAC"
    case RLAlgorithm.TD3 => "TD3"
    case RLAlgorithm.DQN => "DQN"
