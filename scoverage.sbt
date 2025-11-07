// 测试覆盖率配置

// 最小覆盖率要求
coverageMinimumStmtTotal := 80
coverageMinimumBranchTotal := 70

// 覆盖率不足时是否失败构建
coverageFailOnMinimum := false

// 排除不需要测试覆盖的文件
coverageExcludedPackages := Seq(
  "<empty>",
  ".*\\.proto\\..*",  // gRPC 生成的代码
  ".*Main.*",         // 主入口
  ".*\\.config\\..*"  // 配置类
).mkString(";")

// 排除的文件
coverageExcludedFiles := Seq(
  ".*Module.*",
  ".*Routes.*"
).mkString(";")

// 高亮阈值
coverageHighlighting := true