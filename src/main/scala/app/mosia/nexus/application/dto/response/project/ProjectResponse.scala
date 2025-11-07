package app.mosia.nexus.application.dto.response.project

import sttp.tapir.Schema
import zio.json.JsonCodec
//
//case class ProjectResponse(
//                            id: String,
//                            name: String,
//                            description: Option[String],
//                            createdAt: Long,
//                            simulationCount: Int
//                          ) derives JsonCodec
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
