package app.mosia.nexus
package application.dto.response.project

import sttp.tapir.Schema
import zio.json.*
import zio.*

/** 项目响应 DTO
  *
  * 包含项目基本信息和聚合统计信息
  */
case class ProjectResponse(
  id: String,
  name: String,
  description: Option[String],
  createdAt: Long, // epoch ms
  updatedAt: Option[Long],
  simulationCount: Int,
  lastRunAt: Option[Long], // 最近一次 session 完成时间
  status: Option[String] = None, // "active" | "archived"
  tags: List[String] = Nil
) derives JsonCodec,
      Schema
