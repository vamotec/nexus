package app.mosia.nexus
package domain.model.network

/** 负载均衡算法 */
enum LoadBalanceAlgorithm:
  case RoundRobin, LeastConnections, IPHash, LeastTime
