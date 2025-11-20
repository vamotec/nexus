package app.mosia.nexus
package domain.model.network

/** 端口协议类型 */
enum PortProtocol:
  case TCP, UDP, SCTP, HTTP, HTTPS, WebSocket, GRPC
