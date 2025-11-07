package app.mosia.nexus.domain.model.training

import zio.json.JsonCodec

enum RLAlgorithm derives JsonCodec:
  case PPO, SAC, TD3, DQN
