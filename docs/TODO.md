# TODO LIST

## 2025-11-13
## 高优先级
- [ ] 完善生物识别认证逻辑
  - [ ] 完善challenge模型设计
- [ ] 修复数据库连接池bug

## 进行中
- [x] 设计API接口
- [ ] 编写单元测试

## 待处理
- [ ] 优化查询性能
- [ ] 更新文档

## 2025-11-20
### 当前状态
- 刚重构了nexus，现在数据库迁移和代码生成都方便了很多
- [x] 下一步差点搞忘了，就是在nexus设计session-token和omniverse-token: 合并到了统一的generator中，通过tokenType区分
- [ ] 在neuro同步一下验证逻辑