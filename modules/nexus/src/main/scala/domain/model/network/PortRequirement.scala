package app.mosia.nexus
package domain.model.network

/** 端口需求配置 */
case class PortRequirement(
  port: PortRange, // 端口范围
  protocol: PortProtocol, // 协议类型
  direction: TrafficDirection, // 流量方向
  purpose: String, // 用途描述
  security: PortSecurity, // 安全配置
  loadBalancing: Option[LoadBalancing] = None // 负载均衡配置
)
