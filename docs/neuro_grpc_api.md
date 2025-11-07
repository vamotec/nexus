# Neuro Orchestrator gRPC Service API

本文档详细说明了由 `main.py` 提供的 Neuro 编排微服务所暴露的 gRPC 接口。这些服务定义在 `neuro.proto` 文件中。

## 1. IsaacSimService

管理 Isaac Sim 仿真实例的生命周期和交互。

---

### `rpc CreateSession(CreateSessionRequest) returns (CreateSessionResponse)`

- **功能**: 请求一个可用的 Isaac Sim 实例来创建一个新的仿真会话。编排器会找到一个空闲实例，通过 REST API 指示它创建并加载一个场景，然后返回会话信息。
- **请求消息 (`CreateSessionRequest`)**:
```protobuf
  message CreateSessionRequest {
    string session_id = 1;
    SceneConfig scene_config = 2;
    StreamConfig stream_config = 3;
  }
```
- **响应消息 (`CreateSessionResponse`)**:
  ```protobuf
  message CreateSessionResponse {
    bool success = 1;
    string message = 2;
    string session_id = 3;
  }
  ```

### `rpc StartSimulation(StartSimulationRequest) returns (StartSimulationResponse)`

- **功能**: 启动指定会话的仿真。
- **请求消息 (`StartSimulationRequest`)**:
  ```protobuf
  message StartSimulationRequest {
    string session_id = 1;
    bool real_time = 2;
    double time_scale = 3; // 控制仿真速度
  }
  ```
- **响应消息 (`StartSimulationResponse`)**:
  ```protobuf
  message StartSimulationResponse {
    bool success = 1;
    string message = 2;
  }
  ```

### `rpc StopSimulation(StopSimulationRequest) returns (StopSimulationResponse)`

- **功能**: 停止指定会话的仿真。
- **请求消息 (`StopSimulationRequest`)**:
  ```protobuf
  message StopSimulationRequest {
    string session_id = 1;
    bool cleanup = 2; // 如果为 true，将删除场景并释放实例
  }
  ```
- **响应消息 (`StopSimulationResponse`)**:
  ```protobuf
  message StopSimulationResponse {
    bool success = 1;
    string message = 2;
  }
  ```

### `rpc GetSessionStatus(GetSessionStatusRequest) returns (SessionStatus)`

- **功能**: 获取指定会话的当前状态，如 FPS、仿真时间等。
- **请求消息 (`GetSessionStatusRequest`)**:
  ```protobuf
  message GetSessionStatusRequest {
    string session_id = 1;
  }
  ```
- **响应消息 (`SessionStatus`)**:
  ```protobuf
  message SessionStatus {
    // ... (包含 status, fps, frame_count, simulation_time 等字段)
  }
  ```

### `rpc UpdateScene(UpdateSceneRequest) returns (UpdateSceneResponse)`

- **功能**: 动态更新场景，例如添加或移除障碍物。
- **请求消息 (`UpdateSceneRequest`)**:
  ```protobuf
  message UpdateSceneRequest {
    string session_id = 1;
    repeated Obstacle obstacles_to_add = 2;
    repeated int32 obstacles_to_remove = 3; // (注意：实现中使用的是 prim path 字符串)
  }
  ```
- **响应消息 (`UpdateSceneResponse`)**:
  ```protobuf
  message UpdateSceneResponse {
    bool success = 1;
    string message = 2;
  }
  ```

### `rpc GetStreamEndpoint(GetStreamEndpointRequest) returns (StreamEndpoint)`

- **功能**: 获取用于视频流的 WebRTC 端点信息。
- **请求消息 (`GetStreamEndpointRequest`)**:
  ```protobuf
  message GetStreamEndpointRequest {
    string session_id = 1;
  }
  ```
- **响应消息 (`StreamEndpoint`)**:
  ```protobuf
  message StreamEndpoint {
    string protocol = 1;
    string url = 2;
    int32 port = 3;
    map<string, string> metadata = 4;
  }
  ```

### `rpc HealthCheck(HealthCheckRequest) returns (HealthCheckResponse)`

- **功能**: 检查编排器服务的健康状况。
- **请求消息 (`HealthCheckRequest`)**: (可以为空)
- **响应消息 (`HealthCheckResponse`)**:
  ```protobuf
  message HealthCheckResponse {
    bool healthy = 1;
    string version = 2;
    int32 active_sessions = 3;
  }
  ```

### `rpc StreamRobotControl(stream RobotAction) returns (stream RobotState)`

- **功能**: 一个双向流式接口，用于实时机器人控制。编排器充当 gRPC 和 Isaac Sim WebSocket 之间的桥梁。
- **客户端流 (`RobotAction`)**: 客户端持续发送机器人动作指令。
- **服务器流 (`RobotState`)**: 服务器实时返回机器人的状态信息。

## 2. TrainingService

管理 AI 训练节点的任务。**（注意：当前实现为占位符）**

---

### `rpc StartTraining(StartTrainingRequest) returns (StartTrainingResponse)`

- **功能**: 在一个可用的训练节点上启动一个新的训练任务。
- **请求/响应**: (根据 `neuro.proto` 定义)

### `rpc StopTraining(StopTrainingRequest) returns (StopTrainingResponse)`

- **功能**: 停止一个正在进行的训练任务。
- **请求/响应**: (根据 `neuro.proto` 定义)

### `rpc GetTrainingProgress(GetTrainingProgressRequest) returns (stream TrainingProgress)`

- **功能**: 以流的形式获取训练任务的实时进度。
- **请求/响应**: (根据 `neuro.proto` 定义)

### `rpc GetTrainingResult(GetTrainingResultRequest) returns (TrainingResult)`

- **功能**: **(未实现)** 获取已完成训练任务的最终结果。
- **请求/响应**: (根据 `neuro.proto` 定义)

## 3. ResourceService

提供对集群资源的查询和管理功能。

---

### `rpc GetGPUStatus(Empty) returns (GPUStatus)`

- **功能**: **(占位符)** 获取集群中 GPU 节点的详细状态。
- **请求/响应**: (根据 `neuro.proto` 定义)

### `rpc ListActiveSessions(Empty) returns (SessionList)`

- **功能**: 返回当前所有活跃的 Isaac Sim 会话 ID 列表。
- **请求/响应**: (根据 `neuro.proto` 定义)

### `rpc ReleaseResources(ReleaseResourcesRequest) returns (ReleaseResourcesResponse)`

- **功能**: 强制释放一个会话所占用的所有资源，等同于调用 `StopSimulation` 并设置 `cleanup=true`。
- **请求/响应**: (根据 `neuro.proto` 定义)
