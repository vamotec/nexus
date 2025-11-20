package app.mosia.nexus
package application.dto.request.common

case class ListQuery(
  page: Int = 1,
  pageSize: Int = 20,
  sort: String = "createdAt:desc", // 支持 createdAt:asc, name:desc
  search: Option[String] = None
)
