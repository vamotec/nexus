# gRPC API 设计文档 - Neuro Orchestrator

> 版本: 1.0
> 日期: 2025-11-10
> 目的: 定义 Nexus ↔ Neuro 编排器的 gRPC 通信协议

## 目录

- [架构概览](#架构概览)
- [Proto 文件结构](#proto-文件结构)
- [核心服务 API](#核心服务-api)
- [使用场景](#使用场景)
- [与现有 Proto 的关系](#与现有-proto-的关系)

---

## 架构概览

```
┌─────────────┐                    ┌──────────────────────────┐
│   Nexus     │ ← gRPC (本文档) → │   Neuro Orchestrator     │
│ (Scala)     │                    │   (Python)               │
└─────────────┘                    └──────────┬───────────────┘
                                              │
                                              │ 管理
                                              ▼
                          ┌────────────────────────────────────┐
                          │                                    │
                    ┌─────▼─────┐                    ┌────────▼────────┐
                    │ Isaac Sim  │                    │  Training       │
                    │ Pool       │                    │  Instance Pool  │
                    └────────────┘                    └─────────────────┘
```

**关键设计原则:**
1. **Nexus 只与 Neuro 通信**，不直接调用 Isaac Sim 或 Training Instance
2. **Neuro 是核心编排器**，负责资源分配、配对、通道建立
3. **支持三种模式**: Training, Manual, Hybrid

---

## Proto 文件结构

### 新增文件

#### `neuro_orchestrator.proto` (新创建)
Neuro 编排器的主要 gRPC 服务定义，包含：
- `NeuroOrchestratorService` - 核心编排服务
- 三种会话模式的 RPC 方法
- 资源池管理接口

### 现有文件用途调整

| Proto 文件 | 原用途 | 新用途 | 调整建议 |
|-----------|--------|--------|---------|
| `isaac_sim.proto` | Nexus → Isaac Sim | Isaac Sim 内部服务定义 | **保留但不直接使用**，仅供 Isaac Sim 实例内部实现参考 |
| `training.proto` | Nexus → Training | Training Instance 内部服务 | **保留但不直接使用**，仅供 Training Instance 内部实现参考 |
| `resource.proto` | 资源查询 | 资源查询（可选） | 可以集成到 `neuro_orchestrator.proto` 的 `GetResourcePoolStatus` |
| `common.proto` | 公共消息类型 | **继续使用** | 保持不变 |

---

## 核心服务 API

### 1. Manual 模式 - 手动控制

**用途**: Frontend 直接通过 WebRTC 控制 Isaac Sim 实例

```protobuf
rpc CreateSession(CreateSessionRequest) returns (CreateSessionResponse);
```

**请求示例**:
```json
{
  "session_id": "session-123",
  "mode": "MANUAL",
  "scene_config": {
    "robot_urdf": "franka_panda.urdf",
    "obstacles": [...],
    "start_position": {"x": 0, "y": 0, "z": 0}
  },
  "stream_config": {
    "width": 1920,
    "height": 1080,
    "fps": 30,
    "transport": "webrtc"
  }
}
```

**响应示例**:
```json
{
  "success": true,
  "message": "Session created successfully",
  "session_id": "session-123",
  "instance_id": "isaac-sim-1",
  "control_ws_url": "ws://isaac-sim-1:8766/control/session-123",
  "webrtc_signaling_url": "ws://nexus:8080/api/webrtc/signaling/isaac-sim/session-123",
  "status": {
    "status": "READY",
    "fps": 0.0,
    "gpu_utilization": 15.2
  }
}
```

**Nexus 使用流程**:
1. Nexus 接收 Frontend 的 `createSession` GraphQL 请求
2. 调用 Neuro 的 `CreateSession` gRPC
3. Neuro 从 Isaac Sim Pool 分配一个空闲实例
4. 返回 `webrtc_signaling_url` 给 Frontend
5. Frontend 连接 Nexus 的 WebRTC 信令服务器
6. 信令服务器转发消息到 Isaac Sim 实例

---

### 2. Training 模式 - AI 训练

**用途**: Training Instance 控制多个 Isaac Sim 实例进行并行训练

```protobuf
rpc CreateTrainingSession(CreateTrainingSessionRequest) returns (CreateTrainingSessionResponse);
```

**请求示例**:
```json
{
  "session_id": "training-session-456",
  "mode": "TRAINING",
  "scene_config": {...},
  "training_config": {
    "algorithm": "PPO",
    "total_episodes": 10000,
    "parallel_envs": 4,
    "hyperparameters": {
      "learning_rate": "0.0003",
      "gamma": "0.99",
      "clip_range": "0.2"
    }
  },
  "resource_requirements": {
    "training_gpus": 4,
    "training_memory_gb": 32,
    "isaac_sim_instances": 4
  }
}
```

**响应示例**:
```json
{
  "success": true,
  "message": "Training session created",
  "session_id": "training-session-456",
  "training_instance_id": "training-1",
  "isaac_instance_ids": ["isaac-sim-1", "isaac-sim-2", "isaac-sim-3", "isaac-sim-4"],
  "zmq_channels": [
    {
      "isaac_instance_id": "isaac-sim-1",
      "zmq_endpoint": "tcp://isaac-sim-1:5555",
      "protocol": "REQ-REP"
    },
    ...
  ],
  "training_status": "INITIALIZING"
}
```

**Nexus 使用流程**:
1. Nexus 接收 Frontend 的 `createTrainingSession` GraphQL 请求
2. 调用 Neuro 的 `CreateTrainingSession` gRPC
3. Neuro:
   - 从 Training Pool 分配 1 个训练实例（4 GPU）
   - 从 Isaac Sim Pool 分配 4 个实例
   - 建立 4 条 ZMQ 通道（Training ↔ Isaac Sim）
4. Nexus 保存 `training_instance_id` 和 `isaac_instance_ids`
5. Frontend 通过 GraphQL Subscription 订阅训练进度

---

### 3. Hybrid 模式 - 混合模式

**用途**: AI 训练 + 可选的人工接管

```protobuf
rpc CreateHybridSession(CreateHybridSessionRequest) returns (CreateHybridSessionResponse);
```

**特点**:
- 默认由 Training Instance 控制
- Frontend 可以通过 WebRTC 观察或接管控制
- 支持 Learning from Demonstration

---

### 4. 资源池状态查询

```protobuf
rpc GetResourcePoolStatus(ResourcePoolStatusRequest) returns (ResourcePoolStatusResponse);
```

**用途**: 查询可用资源，用于前端显示和管理决策

**响应示例**:
```json
{
  "isaac_pool": {
    "total_instances": 10,
    "idle_instances": 6,
    "running_instances": 4,
    "instances": [
      {
        "instance_id": "isaac-sim-1",
        "gpu_id": "GPU-0",
        "status": "running",
        "paired_with": "training-1",
        "session_id": "session-123"
      },
      ...
    ]
  },
  "training_pool": {
    "total_instances": 3,
    "idle_instances": 2,
    "running_instances": 1,
    "instances": [...]
  }
}
```

---

## 使用场景

### 场景 1: Frontend 创建手动控制会话

```scala
// SessionServiceLive.scala
override def createSession(
  userId: UserId,
  simulationId: SimulationId,
  request: CreateSessionRequest
): Task[SessionResponse] =
  for
    sessionId = SessionId(UUID.randomUUID())

    // 调用 Neuro gRPC
    grpcRequest = CreateSessionRequest(
      sessionId = sessionId.value.toString,
      mode = SessionMode.MANUAL,
      sceneConfig = buildSceneConfig(request),
      streamConfig = buildStreamConfig(request)
    )
    grpcResponse <- neuroClient.createSession(grpcRequest)

    // 生成 control token
    controlToken <- jwtService.generateControlToken(sessionId, userId)

    // 返回响应（包含 WebRTC 信令 URL）
    response = SessionResponse(
      id = sessionId.value.toString,
      mode = SessionMode.Manual,
      controlEndpoint = Some(
        ControlEndpointResponse(
          controlWsUrl = grpcResponse.controlWsUrl,
          controlToken = controlToken,
          webrtcSignalingUrl = grpcResponse.webrtcSignalingUrl
        )
      ),
      ...
    )
  yield response
```

### 场景 2: Frontend 创建训练会话

```scala
override def createTrainingSession(
  userId: UserId,
  request: CreateTrainingSessionRequest
): Task[TrainingSessionResponse] =
  for
    sessionId = SessionId(UUID.randomUUID())

    // 调用 Neuro gRPC (Training 模式)
    grpcRequest = CreateTrainingSessionRequest(
      sessionId = sessionId.value.toString,
      mode = SessionMode.TRAINING,
      sceneConfig = buildSceneConfig(request),
      trainingConfig = TrainingConfig(
        algorithm = request.algorithm,
        totalEpisodes = request.totalEpisodes,
        parallelEnvs = request.parallelEnvs,
        hyperparameters = request.hyperparameters
      ),
      resourceRequirements = ResourceRequirements(
        trainingGpus = request.trainingGpus,
        trainingMemoryGb = request.trainingMemoryGb,
        isaacSimInstances = request.parallelEnvs
      )
    )
    grpcResponse <- neuroClient.createTrainingSession(grpcRequest)

    // 保存训练会话信息
    response = TrainingSessionResponse(
      sessionId = sessionId.value.toString,
      trainingInstanceId = grpcResponse.trainingInstanceId,
      isaacInstanceIds = grpcResponse.isaacInstanceIds,
      trainingStatus = grpcResponse.trainingStatus,
      ...
    )
  yield response
```

### 场景 3: 获取资源池状态（管理面板）

```scala
def getResourcePoolStatus(): Task[ResourcePoolStatus] =
  for
    grpcRequest = ResourcePoolStatusRequest(
      includeIsaacPool = true,
      includeTrainingPool = true
    )
    grpcResponse <- neuroClient.getResourcePoolStatus(grpcRequest)

    status = ResourcePoolStatus(
      isaacPool = grpcResponse.isaacPool,
      trainingPool = grpcResponse.trainingPool
    )
  yield status
```

---

## 与现有 Proto 的关系

### 迁移策略

| 当前调用 | 改为调用 | 说明 |
|---------|---------|------|
| `IsaacSimService.CreateSession` | `NeuroOrchestratorService.CreateSession` | Neuro 分配 Isaac Sim 实例 |
| `IsaacSimService.StartSimulation` | `NeuroOrchestratorService.StartSimulation` | 通过 session_id 路由到正确实例 |
| `TrainingService.StartTraining` | `NeuroOrchestratorService.CreateTrainingSession` | Neuro 分配 Training + Isaac Sim 资源 |
| `ResourceService.GetGPUStatus` | `NeuroOrchestratorService.GetResourcePoolStatus` | 统一资源查询接口 |

### 兼容性考虑

**方案 A: 完全替换** (推荐)
- 删除 Nexus 中对 `isaac_sim.proto` 和 `training.proto` 的直接引用
- 仅使用 `neuro_orchestrator.proto`
- 优点: 架构清晰，职责明确
- 缺点: 需要更新所有调用代码

**方案 B: 渐进式迁移**
- 保留现有 proto 文件和客户端代码
- 新增 `neuro_orchestrator.proto` 客户端
- 根据会话模式选择调用哪个客户端
- 优点: 平滑过渡
- 缺点: 维护两套代码

---

## 实施建议

### Phase 1: Neuro 侧实现 (Neuro 团队)
1. 实现 `NeuroOrchestratorService` gRPC 服务器
2. 实现资源池管理逻辑
3. 实现配对算法和通道建立

### Phase 2: Nexus 侧集成 (当前)
1. ✅ 创建 `neuro_orchestrator.proto`
2. ⏳ 生成 gRPC 客户端代码 (`sbt compile`)
3. ⏳ 创建 `NeuroOrchestratorClient` Scala 封装
4. ⏳ 更新 `SessionService` 调用新 API
5. ⏳ 更新 DTO 映射

### Phase 3: 测试验证
1. Manual 模式端到端测试
2. Training 模式端到端测试
3. 资源池管理测试

---

## 下一步

1. **编译 proto 文件**: `sbt compile` 生成 Scala gRPC 客户端代码
2. **创建 NeuroOrchestratorClient**: 封装 gRPC 调用
3. **更新 SessionService**: 使用新的 gRPC 客户端
4. **与 Neuro 团队对接**: 确认 proto 定义是否符合 Python 端实现

---

**文档维护**: 如有 API 调整，请及时更新本文档。
