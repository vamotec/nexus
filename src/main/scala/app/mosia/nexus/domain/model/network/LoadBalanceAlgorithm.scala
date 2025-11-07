package app.mosia.nexus.domain.model.network

/** 负载均衡算法 */
enum LoadBalanceAlgorithm:
  case RoundRobin, LeastConnections, IPHash, LeastTime
