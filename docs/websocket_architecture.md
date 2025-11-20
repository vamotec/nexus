# WebSocket 通信架构设计 (仿真-训练分离架构)

> 文档版本: 2.0
> 创建日期: 2025-11-10
> 更新日期: 2025-11-10
> 设计目标: 实现仿真环境与 AI 训练的分离，支持大规模机器学习训练

## 目录

- [架构概览](#架构概览)
- [核心理念](#核心理念)
- [完整架构图](#完整架构图)
- [三种运行模式](#三种运行模式)
- [工作流程](#工作流程)
- [关键实现细节](#关键实现细节)
- [通信协议](#通信协议)
- [安全认证](#安全认证)
- [实施步骤](#实施步骤)

---

## 架构概览

### 核心设计理念

**仿真与训练分离，Neuro 统一编排**

本架构将计算密集型任务分为两类独立资源：

1. **Isaac Sim Instance (环境仿真)**
   - GPU 资源：适合图形渲染和物理模拟 (单 GPU)
   - 职责：提供仿真环境、传感器数据、执行动作
   - 不负责 AI 训练

2. **Training Instance (AI 训练)**
   - GPU 资源：适合大规模并行计算 (多 GPU 或 TPU)
   - 职责：运行深度学习模型训练、策略优化
   - 不负责仿真渲染

3. **Neuro (编排器)**
   - 协调 Isaac Sim 和 Training Instance 的配对
   - 建立高速数据通道
   - 负载均衡和资源调度

### 架构优势

1. **资源专用化**: 仿真 GPU 和训练 GPU 独立扩展
2. **高效训练**: 训练实例可使用多 GPU 并行，Isaac Sim 只需单 GPU
3. **灵活部署**:
   - 1 个训练实例 ↔ N 个 Isaac Sim (分布式采样)
   - M 个训练实例共享 Isaac Sim 池 (多任务训练)
4. **成本优化**: 按需分配，仿真和训练可独立扩缩容

---

## 完整架构图

```
┌─────────────────────────────────────────────────────────────────┐
│                         Frontend (Web)                          │
│                                                                 │
│  职责:                                                           │
│  • 创建训练任务/会话                                             │
│  • 监控训练进度和仿真状态 (GraphQL Subscription)                 │
│  • 可选: 手动控制仿真 (调试模式 WebSocket)                        │
│  • 查看训练结果和可视化                                          │
└─────┬───────────────────────────────────────────────────────────┘
      │
      │ ① GraphQL/REST: createTrainingSession()
      │    ↓ 返回: {sessionId, isaacWsUrl, trainingStatus}
      │
┌─────▼───────────────────────────────────────────────────────────┐
│                      Nexus (Scala Backend)                      │
│                                                                 │
│  职责:                                                           │
│  • 统一 API 入口 (REST/GraphQL)                                  │
│  • 用户认证和授权                                                │
│  • 配额管理                                                      │
│  • 会话元数据存储 (PostgreSQL)                                   │
│  • 聚合状态推送 (Kafka → GraphQL Subscription)                   │
└─────┬───────────────────────────────────────────────────────────┘
      │
      │ ② gRPC: CreateTrainingSession(config, resources)
      │    ↓ 返回: {isaacInstanceId, trainingInstanceId, endpoints}
      │
┌─────▼───────────────────────────────────────────────────────────┐
│              Neuro (Python - Orchestrator)                      │
│                                                                 │
│  核心职责:                                                       │
│  • 管理 Isaac Sim 实例池                                         │
│  • 管理 Training Instance 池                                     │
│  • 智能配对算法 (根据任务需求匹配仿真和训练资源)                  │
│  • 建立仿真-训练高速通道 (ZMQ/共享内存/RDMA)                       │
│  • 健康检查和故障恢复                                            │
│  • 负载均衡                                                      │
│                                                                 │
│  实例池状态:                                                     │
│  【Isaac Sim Pool】                                             │
│  • isaac-sim-1: GPU 0, Idle,    Paired: None                   │
│  • isaac-sim-2: GPU 1, Running, Paired: training-1             │
│  • isaac-sim-3: GPU 2, Running, Paired: training-1 (multi-env) │
│                                                                 │
│  【Training Pool】                                              │
│  • training-1: 4×A100, Running, Algorithm: PPO                 │
│  • training-2: 8×H100, Idle,    Reserved for: project-xyz     │
│                                                                 │
└─────┬────────────────────────────┬────────────────────────────┘
      │                            │
      │ ③ 启动并配对               │ ④ 建立高速通道
      │                            │
      ├────────────────┐          ┌▼─────────────────────────────┐
      │                │          │                              │
┌─────▼──────┐   ┌────▼──────┐   │  ┌─────────────────────────┐ │
│Isaac Sim 1 │   │Isaac Sim 2│◄──┼─▶│  Training Instance 1    │ │
│            │   │           │   │  │                         │ │
│GPU: RTX4090│   │GPU: RTX4090  │  │  GPU: 4×A100             │ │
│Port: 8766  │   │Port: 8767 │   │  │ (PyTorch/JAX)           │ │
│            │   │           │   │  │                         │ │
│职责:        │   │职责:       │   │  │职责:                    │ │
│• 场景渲染   │   │• 场景渲染  │   │  │• 神经网络训练           │ │
│• 物理模拟   │   │• 物理模拟  │   │  │• 策略优化               │ │
│• 执行动作   │   │• 执行动作  │   │  │• 经验回放               │ │
│• 传感器数据 │   │• 传感器数据│   │  │• 模型推理               │ │
│            │   │           │   │  │                         │ │
│WebSocket   │   │WebSocket  │   │  │无 WebSocket             │ │
│(可选前端连接)│  │(可选)      │   │  │(仅内部通信)             │ │
└────────────┘   └───────────┘   │  └─────────────────────────┘ │
                                 │                              │
                                 │  高频数据通道 (ZMQ/gRPC Stream):│
                                 │  Isaac → Training: Observation │
                                 │  Training → Isaac: Action      │
                                 │  频率: 100-1000 Hz             │
                                 └──────────────────────────────┘


                    ┌────────────────────────────────┐
                    │      Kafka Event Bus           │
                    │                                │
                    │  Topics:                       │
                    │  • training.progress           │
                    │  • training.metrics            │
                    │  • simulation.status           │
                    │  • simulation.episodes         │
                    └──┬──────────────────────────┬──┘
                       ▲                          │
                       │ 发布事件                  │ 消费事件
                       │                          ▼
              Training Instance              Nexus
              Isaac Sim Instance        (更新DB + 推送前端)
```

---

## 三种运行模式

### 模式 1: AI 训练模式 (主要场景) ⭐

**适用场景**: 强化学习、模仿学习、策略优化

```
Frontend
    ↓ createTrainingSession(algorithm: "PPO", episodes: 10000)
Nexus
    ↓ gRPC
Neuro
    ├─> 分配 Training Instance (4×GPU)
    └─> 分配 Isaac Sim Instance (1×GPU)
         └─> 建立高速通道

训练循环 (在 Training Instance 内部):
┌─────────────────────────────────────────────┐
│ for episode in range(10000):                │
│     obs = isaac_sim.reset()  ←─ ZMQ/gRPC   │
│                                             │
│     for step in range(max_steps):          │
│         action = policy(obs)  ← GPU 计算    │
│         obs, reward = isaac_sim.step(action)│
│         buffer.add(obs, action, reward)     │
│                                             │
│     # 训练神经网络                           │
│     policy.update(buffer)  ← GPU 训练       │
│                                             │
│     # 推送进度 (低频)                        │
│     kafka.send('training.progress', ...)    │
└─────────────────────────────────────────────┘

前端只需要:
• 订阅训练进度 (GraphQL Subscription)
• 查看实时指标和曲线
• 无需 WebSocket 连接
```

**数据流**:
- **高频** (100-1000 Hz): Isaac Sim ↔ Training Instance (ZMQ/gRPC)
- **低频** (1 Hz): Training Instance → Kafka → Nexus → Frontend

---

### 模式 2: 手动控制模式 (调试/演示)

**适用场景**: 远程操控、参数调试、故障排查

```
Frontend
    ↓ createSession(mode: "manual")
Nexus
    ↓ gRPC
Neuro
    └─> 分配 Isaac Sim Instance (不分配 Training Instance)

Frontend ←WebSocket→ Isaac Sim
    ↓ 键盘/手柄输入
    ↓ 实时控制命令 (50 Hz)
Isaac Sim 执行并渲染
```

**特点**:
- 不启动 Training Instance
- 前端直连 Isaac Sim WebSocket
- 成本低，仅用于调试

---

### 模式 3: 混合模式 (人机协作)

**适用场景**: Learning from Demonstration, 人工纠正

```
Training Instance ←ZMQ→ Isaac Sim ←WebSocket→ Frontend
         ↑                                      ↓
         └────── 训练时由 AI 控制 ───────────────┘
         └────── 需要时由人工接管 ───────────────┘
```

**流程**:
1. 默认 AI 训练运行
2. 前端监控发现异常行为
3. 用户通过 WebSocket 发送 `{"mode": "manual"}`
4. Isaac Sim 切换为接收前端命令
5. 用户演示正确行为，Training Instance 记录
6. 恢复 AI 控制，继续训练

---

## 工作流程

### Workflow 1: 创建 AI 训练会话

```
Frontend                 Nexus            Neuro           Training Pool    Isaac Pool
   |                      |                |                    |             |
   |--createTrainingSession(PPO, 10000)--->|                    |             |
   |                      |                |                    |             |
   |                      |--gRPC: CreateTrainingSession------->|             |
   |                      |   {algorithm: "PPO",                |             |
   |                      |    episodes: 10000,                 |             |
   |                      |    envs: 4}                         |             |
   |                      |                |                    |             |
   |                      |                |--调度算法:          |             |
   |                      |                |  选择训练实例------->|             |
   |                      |                |  (需要 4 GPU)       |             |
   |                      |                |                    |             |
   |                      |                |--分配 4 个          |             |
   |                      |                |  Isaac Sim 实例-----|------------>|
   |                      |                |  (并行采样)          |             |
   |                      |                |                    |             |
   |                      |                |--建立 ZMQ 通道----->|             |
   |                      |                |  training ↔ isaac_1|             |
   |                      |                |  training ↔ isaac_2|             |
   |                      |                |  training ↔ isaac_3|             |
   |                      |                |  training ↔ isaac_4|             |
   |                      |                |                    |             |
   |                      |<--返回配对信息--|                    |             |
   |                      |  {trainingInstanceId: "t-1",        |             |
   |                      |   isaacInstances: ["i-1","i-2",...],|             |
   |                      |   status: "running"}                |             |
   |                      |                |                    |             |
   |<--TrainingSessionResponse-------------|                    |             |
   |  {sessionId: "session-123",           |                    |             |
   |   trainingStatus: "initializing",     |                    |             |
   |   isaacWsUrl: "ws://i-1:8766"  ← 可选 |                   |             |
   |   (仅调试模式需要)}                    |                    |             |
   |                      |                |                    |             |
   |--订阅训练进度-------->|                |                    |             |
   |  (GraphQL Subscription)               |                    |             |
```

### Workflow 2: 训练循环执行

```
Training Instance                    Isaac Sim Instance 1-4           Kafka
      |                                      |                         |
      |--ZMQ: reset_env()------------------→|                         |
      |←-ZMQ: initial_observation------------|                         |
      |                                      |                         |
      |  # 训练循环                           |                         |
      |  for step in range(max_steps):       |                         |
      |                                      |                         |
      |    action = policy(obs)  ← GPU 推理  |                         |
      |                                      |                         |
      |--ZMQ: step(action)------------------→|                         |
      |                                      |--执行动作                |
      |                                      |--物理模拟 (10ms)         |
      |←-ZMQ: (obs, reward, done)------------|                         |
      |                                      |                         |
      |  buffer.add(transition)              |                         |
      |                                      |                         |
      |  # 每 N 步训练一次                    |                         |
      |  if step % N == 0:                   |                         |
      |    policy.update(buffer) ← GPU 训练  |                         |
      |                                      |                         |
      |  # 每个 episode 结束后推送进度         |                         |
      |  if done:                            |                         |
      |    metrics = {episode, reward, ...}  |                         |
      |--发布事件-----------------------------------------→Kafka------→|
      |                                      |                         |
```

### Workflow 3: 前端监控

```
Kafka                Nexus               Frontend
  |                    |                     |
  |--training.progress→|                     |
  |  {episode: 100,    |--GraphQL Subscription→|
  |   reward: 250.5,   |   trainingProgress  |--更新 UI
  |   loss: 0.023}     |                     |  (实时曲线)
  |                    |                     |
  |--simulation.status→|                     |
  |  {fps: 60,         |--GraphQL Subscription→|
  |   gpu: 85%}        |   simulationStatus  |--显示状态
```

---

## 关键实现细节

### 1. Neuro 编排器 - 配对算法

```python
class NeuroOrchestrator:
    def __init__(self):
        self.isaac_pool = IsaacSimPool()
        self.training_pool = TrainingInstancePool()

    async def create_training_session(
        self,
        request: CreateTrainingSessionRequest
    ) -> TrainingSessionResponse:
        """
        创建训练会话，分配并配对仿真和训练资源
        """
        # 1. 根据算法和资源需求选择训练实例
        training_requirements = self._parse_requirements(request)
        training_instance = await self.training_pool.allocate(
            gpus=training_requirements.gpus,
            memory=training_requirements.memory,
            algorithm=request.algorithm
        )

        # 2. 分配 Isaac Sim 实例（数量 = 并行环境数）
        num_envs = request.parallel_envs or 4
        isaac_instances = []
        for _ in range(num_envs):
            instance = await self.isaac_pool.allocate(
                scene_type=request.scene_type,
                robot_type=request.robot_type
            )
            isaac_instances.append(instance)

        # 3. 建立高速数据通道
        channels = await self._establish_channels(
            training_instance,
            isaac_instances,
            protocol="zmq"  # 或 "grpc_stream", "shared_memory"
        )

        # 4. 初始化训练环境
        await training_instance.initialize_training(
            algorithm=request.algorithm,
            env_channels=channels,
            hyperparameters=request.hyperparameters
        )

        # 5. 启动 Isaac Sim 场景
        for isaac in isaac_instances:
            await isaac.load_scene(request.scene_config)

        # 6. 记录配对关系
        session = TrainingSession(
            id=generate_session_id(),
            training_instance_id=training_instance.id,
            isaac_instances=[i.id for i in isaac_instances],
            status="running",
            created_at=datetime.now()
        )
        await self.db.save_session(session)

        return TrainingSessionResponse(
            session_id=session.id,
            training_instance_id=training_instance.id,
            isaac_instances=[i.id for i in isaac_instances],
            # 可选：仅调试模式返回
            isaac_ws_url=isaac_instances[0].ws_url if request.debug_mode else None
        )

    async def _establish_channels(
        self,
        training_instance: TrainingInstance,
        isaac_instances: List[IsaacSimInstance],
        protocol: str
    ) -> List[Channel]:
        """建立 Training Instance 和 Isaac Sim 之间的高速通道"""
        channels = []

        for isaac in isaac_instances:
            if protocol == "zmq":
                # ZeroMQ: 高性能消息队列
                channel = await self._setup_zmq_channel(
                    training_instance.internal_ip,
                    isaac.internal_ip
                )
            elif protocol == "grpc_stream":
                # gRPC 双向流
                channel = await self._setup_grpc_stream(
                    training_instance, isaac
                )
            elif protocol == "shared_memory":
                # 共享内存 (仅限同机部署)
                channel = await self._setup_shared_memory(
                    training_instance, isaac
                )

            channels.append(channel)

        return channels

    async def _setup_zmq_channel(self, training_ip: str, isaac_ip: str):
        """配置 ZeroMQ 通道"""
        # Training Instance 作为 REQ (请求方)
        # Isaac Sim 作为 REP (应答方)

        # 在 Training Instance 上创建 ZMQ socket
        training_socket_config = {
            "type": "REQ",
            "connect": f"tcp://{isaac_ip}:5555",
            "timeout": 100  # ms
        }

        # 在 Isaac Sim 上创建 ZMQ socket
        isaac_socket_config = {
            "type": "REP",
            "bind": "tcp://*:5555",
            "timeout": 100
        }

        await training_instance.configure_zmq(training_socket_config)
        await isaac.configure_zmq(isaac_socket_config)

        return Channel(
            protocol="zmq",
            training_endpoint=f"tcp://{isaac_ip}:5555",
            isaac_endpoint="tcp://*:5555"
        )
```

### 2. Training Instance - 训练循环

```python
# Training Instance 内部运行

import zmq
import torch
from stable_baselines3 import PPO

class TrainingWorker:
    def __init__(self, env_channels: List[Channel], algorithm: str):
        self.env_channels = env_channels
        self.num_envs = len(env_channels)

        # 创建 ZMQ sockets
        self.sockets = []
        context = zmq.Context()
        for channel in env_channels:
            socket = context.socket(zmq.REQ)
            socket.connect(channel.training_endpoint)
            self.sockets.append(socket)

        # 初始化 RL 算法
        self.policy = PPO("MlpPolicy", n_envs=self.num_envs)

    async def train(self, total_episodes: int):
        """训练主循环"""
        for episode in range(total_episodes):
            # 并行重置所有环境
            observations = await self._parallel_reset()

            episode_rewards = [0] * self.num_envs

            for step in range(self.max_steps_per_episode):
                # GPU 推理：批量计算所有环境的动作
                with torch.no_grad():
                    actions = self.policy.predict(observations)

                # 并行发送动作到所有 Isaac Sim 实例
                results = await self._parallel_step(actions)

                # 收集数据
                next_observations, rewards, dones = [], [], []
                for i, result in enumerate(results):
                    next_observations.append(result['observation'])
                    rewards.append(result['reward'])
                    dones.append(result['done'])
                    episode_rewards[i] += result['reward']

                # 存入经验回放缓冲区
                self.policy.replay_buffer.add(
                    observations, actions, rewards,
                    next_observations, dones
                )

                observations = next_observations

                # 每 N 步训练一次神经网络
                if step % self.train_freq == 0:
                    self.policy.train()  # GPU 密集计算

            # Episode 结束，推送进度到 Kafka
            avg_reward = sum(episode_rewards) / self.num_envs
            await self.kafka_producer.send('training.progress', {
                'session_id': self.session_id,
                'episode': episode,
                'avg_reward': avg_reward,
                'policy_loss': self.policy.logger.get('policy_loss'),
                'timestamp': time.time()
            })

    async def _parallel_reset(self) -> List[np.ndarray]:
        """并行重置所有环境"""
        tasks = []
        for socket in self.sockets:
            task = asyncio.create_task(
                self._send_command(socket, {"cmd": "reset"})
            )
            tasks.append(task)

        results = await asyncio.gather(*tasks)
        return [r['observation'] for r in results]

    async def _parallel_step(self, actions: np.ndarray) -> List[dict]:
        """并行执行动作"""
        tasks = []
        for i, socket in enumerate(self.sockets):
            task = asyncio.create_task(
                self._send_command(socket, {
                    "cmd": "step",
                    "action": actions[i].tolist()
                })
            )
            tasks.append(task)

        return await asyncio.gather(*tasks)

    async def _send_command(self, socket: zmq.Socket, command: dict) -> dict:
        """通过 ZMQ 发送命令并等待响应"""
        socket.send_json(command)
        response = socket.recv_json()
        return response
```

### 3. Isaac Sim Instance - ZMQ 服务器

```python
# Isaac Sim Instance 内部运行

import zmq
from omni.isaac.kit import SimulationApp

class IsaacSimZMQServer:
    def __init__(self, port: int = 5555):
        self.context = zmq.Context()
        self.socket = self.context.socket(zmq.REP)
        self.socket.bind(f"tcp://*:{port}")

        # 初始化 Isaac Sim
        self.sim_app = SimulationApp({"headless": True})
        self.env = None  # 场景环境

    async def serve(self):
        """ZMQ 服务循环"""
        while True:
            # 接收命令 (阻塞)
            message = self.socket.recv_json()

            # 处理命令
            if message['cmd'] == 'reset':
                result = self._handle_reset()
            elif message['cmd'] == 'step':
                result = self._handle_step(message['action'])
            elif message['cmd'] == 'close':
                result = {'status': 'ok'}
                self.socket.send_json(result)
                break
            else:
                result = {'error': 'unknown command'}

            # 发送响应
            self.socket.send_json(result)

    def _handle_reset(self) -> dict:
        """重置环境"""
        observation = self.env.reset()

        return {
            'observation': observation.tolist(),
            'status': 'ok'
        }

    def _handle_step(self, action: list) -> dict:
        """执行一步仿真"""
        # 1. 应用动作
        self.env.apply_action(action)

        # 2. 推进物理模拟
        for _ in range(self.physics_steps):
            self.sim_app.update()

        # 3. 获取传感器数据
        observation = self.env.get_observation()
        reward = self.env.compute_reward()
        done = self.env.is_done()

        return {
            'observation': observation.tolist(),
            'reward': float(reward),
            'done': bool(done),
            'info': {}
        }
```

### 4. Nexus 集成

#### 扩展 CreateSessionRequest

```scala
// src/main/scala/app/mosia/nexus/application/dto/request/session/CreateSessionRequest.scala

case class CreateSessionRequest(
  projectId: String,
  sceneName: String,
  robotType: String,
  robotPosition: Position3D,
  obstacles: List[ObstacleDto],
  environment: String,

  // 新增：会话模式
  mode: SessionMode = SessionMode.Training,  // Training, Manual, Hybrid

  // 训练配置（mode = Training 时必需）
  trainingConfig: Option[TrainingConfig] = None,

  realTime: Boolean = true,
  renderQuality: String = "medium"
) derives JsonCodec, Schema

enum SessionMode derives JsonCodec, Schema:
  case Training   // AI 训练模式
  case Manual     // 手动控制模式
  case Hybrid     // 混合模式

case class TrainingConfig(
  algorithm: String,           // "PPO", "SAC", "TD3"
  totalEpisodes: Int = 10000,
  parallelEnvs: Int = 4,       // 并行环境数

  // 资源需求
  trainingGpus: Int = 4,       // 训练 GPU 数量
  trainingMemoryGB: Int = 32,

  // 超参数
  hyperparameters: Map[String, Any] = Map.empty,

  // 可选：人工演示
  enableHumanDemo: Boolean = false
) derives JsonCodec, Schema
```

#### 更新 SessionResponse

```scala
case class SessionResponse(
  id: String,
  userId: String,
  projectId: String,
  status: SessionStatus,
  mode: SessionMode,  // 新增

  scene: SceneResponse,
  metrics: Option[MetricsResponse],

  // Isaac Sim 端点（仅 Manual/Hybrid 模式）
  streamEndpoint: Option[StreamEndpointResponse],
  controlEndpoint: Option[ControlEndpointResponse],

  // Training Instance 信息（仅 Training/Hybrid 模式）
  trainingInfo: Option[TrainingInfo],

  createdAt: Long,
  startedAt: Option[Long]
) derives Schema.SemiAuto, ArgBuilder

case class TrainingInfo(
  trainingInstanceId: String,
  isaacInstanceIds: List[String],  // 多个并行环境
  algorithm: String,
  currentEpisode: Int,
  totalEpisodes: Int,
  status: TrainingStatus
) derives Schema.SemiAuto, ArgBuilder

enum TrainingStatus derives JsonCodec, Schema:
  case Initializing
  case Running
  case Paused
  case Completed
  case Failed
```

#### 更新 SessionService.createSession

```scala
override def createSession(
  userId: UserId,
  simulationId: SimulationId,
  request: CreateSessionRequest,
): Task[SessionResponse] =
  for
    sessionId = SessionId(UUID.randomUUID())

    // 根据模式选择不同的 gRPC 调用
    response <- request.mode match
      case SessionMode.Training =>
        // 调用 Neuro 创建训练会话
        neuroClient.createTrainingSession(
          GrpcCreateTrainingSessionRequest(
            sessionId = sessionId.value.toString,
            sceneConfig = buildSceneConfig(request),
            trainingConfig = request.trainingConfig.get  // 必需
          )
        )

      case SessionMode.Manual =>
        // 仅分配 Isaac Sim 实例
        neuroClient.createSession(
          GrpcCreateSessionRequest(
            sessionId = sessionId.value.toString,
            sceneConfig = buildSceneConfig(request),
            streamConfig = buildStreamConfig(request)
          )
        )

      case SessionMode.Hybrid =>
        // 分配训练实例 + Isaac Sim，并启用前端 WebSocket
        neuroClient.createHybridSession(...)

    // 生成 control token (仅 Manual/Hybrid 模式)
    controlToken <- request.mode match
      case SessionMode.Manual | SessionMode.Hybrid =>
        jwtService.generateControlToken(sessionId, userId).map(Some(_))
      case _ =>
        ZIO.succeed(None)

    // 构建响应
    sessionResponse = SessionResponse(
      id = sessionId.value.toString,
      mode = request.mode,
      trainingInfo = response match
        case r: GrpcTrainingSessionResponse =>
          Some(TrainingInfo(
            trainingInstanceId = r.trainingInstanceId,
            isaacInstanceIds = r.isaacInstanceIds,
            algorithm = request.trainingConfig.get.algorithm,
            currentEpisode = 0,
            totalEpisodes = request.trainingConfig.get.totalEpisodes,
            status = TrainingStatus.Initializing
          ))
        case _ => None
      ,
      controlEndpoint = controlToken.map(token =>
        ControlEndpointResponse(
          controlWsUrl = response.controlWsUrl,
          controlToken = token,
          webrtcSignalingUrl = response.webrtcSignalingUrl
        )
      ),
      ...
    )

  yield sessionResponse
```

---

## 通信协议

### 1. Training Instance ↔ Isaac Sim (ZMQ)

#### 协议选择：ZeroMQ

**优势**:
- 低延迟 (< 1ms)
- 高吞吐 (> 100K msg/s)
- 支持多种模式 (REQ-REP, PUB-SUB)
- 跨语言支持

#### 消息格式

```python
# Training → Isaac: Step 命令
{
  "cmd": "step",
  "action": [0.5, -0.3, 1.0],  # 机器人关节速度
  "timestamp": 1699999999.123
}

# Isaac → Training: 响应
{
  "observation": {
    "lidar": [1.2, 1.5, ...],      # 360 个点
    "camera": "base64_image_data",
    "joint_positions": [0.1, 0.2, 0.3],
    "joint_velocities": [0.01, 0.02, 0.03]
  },
  "reward": 1.5,
  "done": false,
  "info": {
    "collision": false,
    "success": false
  },
  "timestamp": 1699999999.133
}

# Training → Isaac: Reset 命令
{
  "cmd": "reset",
  "seed": 42  # 可选
}

# Isaac → Training: Reset 响应
{
  "observation": {...},
  "info": {}
}
```

### 2. Isaac Sim → Kafka (低频事件)

```python
# 仿真状态更新 (每秒一次)
topic: "simulation.status"
{
  "session_id": "session-123",
  "isaac_instance_id": "isaac-sim-2",
  "fps": 60.0,
  "gpu_utilization": 85.5,
  "physics_steps": 1000,
  "timestamp": 1699999999.0
}

# Episode 完成事件
topic: "simulation.episodes"
{
  "session_id": "session-123",
  "episode": 150,
  "total_reward": 250.5,
  "steps": 500,
  "success": true,
  "timestamp": 1699999999.0
}
```

### 3. Training Instance → Kafka (训练进度)

```python
# 训练进度更新
topic: "training.progress"
{
  "session_id": "session-123",
  "training_instance_id": "training-1",
  "episode": 150,
  "avg_reward": 250.5,
  "policy_loss": 0.023,
  "value_loss": 0.015,
  "entropy": 1.2,
  "learning_rate": 0.0003,
  "timestamp": 1699999999.0
}

# 训练指标更新
topic: "training.metrics"
{
  "session_id": "session-123",
  "metrics": {
    "success_rate": 0.85,
    "avg_episode_length": 450,
    "exploration_rate": 0.1
  },
  "timestamp": 1699999999.0
}
```

### 4. Frontend ↔ Nexus (GraphQL Subscription)

```graphql
subscription TrainingProgress($sessionId: ID!) {
  trainingProgress(sessionId: $sessionId) {
    episode
    avgReward
    policyLoss
    valueLoss

    # 仿真状态
    simulationStatus {
      fps
      gpuUtilization
    }

    timestamp
  }
}
```

---

## 安全认证

### 1. Frontend ↔ Nexus

标准 JWT 认证 (已实现)

### 2. Nexus ↔ Neuro

gRPC 双向 TLS + API Key

```python
# Neuro gRPC 服务端
credentials = grpc.ssl_server_credentials([
    (server_key, server_cert)
])

server = grpc.aio.server()
server.add_secure_port('[::]:50051', credentials)
```

### 3. Training Instance ↔ Isaac Sim

**内网通信 + 网络隔离**

- ZMQ 通道仅在内网绑定 (不对外暴露)
- 使用 VPC/虚拟网络隔离
- 可选：ZMQ CURVE 加密

```python
# ZMQ CURVE 加密示例
import zmq.auth

# 生成密钥对
server_public, server_secret = zmq.curve_keypair()
client_public, client_secret = zmq.curve_keypair()

# 服务端配置
socket.curve_secretkey = server_secret
socket.curve_publickey = server_public
socket.curve_server = True

# 客户端配置
socket.curve_serverkey = server_public
socket.curve_publickey = client_public
socket.curve_secretkey = client_secret
```

---

## 资源调度策略

### 1. 1:N 模式 (一个训练实例 ↔ 多个仿真环境)

**适用**: 样本效率优先，需要大量并行采样

```
Training Instance (4×GPU)
    ├─ ZMQ Channel 1 ─→ Isaac Sim 1 (环境 A)
    ├─ ZMQ Channel 2 ─→ Isaac Sim 2 (环境 A)
    ├─ ZMQ Channel 3 ─→ Isaac Sim 3 (环境 B)
    └─ ZMQ Channel 4 ─→ Isaac Sim 4 (环境 B)
```

**Neuro 配置**:
```python
await neuro.create_training_session(
    training_gpus=4,
    parallel_envs=4,  # 分配 4 个 Isaac Sim 实例
    env_diversity=True  # 不同场景变体
)
```

### 2. N:1 模式 (多个训练任务 → 共享仿真池)

**适用**: 资源受限，多个小任务

```
Training Instance 1 (Algorithm: PPO)  ─┐
Training Instance 2 (Algorithm: SAC)  ─┼→ Isaac Sim Pool
Training Instance 3 (Algorithm: TD3)  ─┘    (按需分配)
```

### 3. 异构配置

**示例**：不同任务的资源需求

| 任务类型 | 训练 GPU | Isaac Sim 数量 | 配置 |
|---------|---------|---------------|------|
| 快速验证 | 1×A100 | 1 | 小规模测试 |
| 标准训练 | 4×A100 | 4 | 并行采样 |
| 大规模训练 | 8×H100 | 16 | 分布式训练 |

---

## 实施步骤

### Phase 1: Neuro 编排器增强 (5-7 天)

- [ ] **实现 Training Instance Pool 管理**
  - Instance 生命周期管理 (Docker/K8s)
  - 资源监控 (GPU/内存/网络)
  - 健康检查

- [ ] **实现配对算法**
  - 根据训练配置选择资源
  - 建立 ZMQ 通道
  - 配对关系持久化

- [ ] **扩展 gRPC API**
  - `CreateTrainingSession`
  - `CreateHybridSession`
  - `StopTrainingSession`

**关键文件**:
- `neuro/training_pool.py`
- `neuro/pairing_engine.py`
- `neuro/grpc/training_service.py`

### Phase 2: Training Instance 实现 (7-10 天)

- [ ] **Docker 镜像构建**
  - PyTorch + RL 库 (Stable-Baselines3, RLlib)
  - ZMQ 客户端
  - Kafka Producer

- [ ] **训练循环实现**
  - 多环境并行采样
  - 经验回放缓冲区
  - 策略更新

- [ ] **监控和日志**
  - 训练指标推送到 Kafka
  - TensorBoard 集成

**关键文件**:
- `training-instance/Dockerfile`
- `training-instance/trainer.py`
- `training-instance/env_manager.py`

### Phase 3: Isaac Sim Instance 增强 (5-7 天)

- [ ] **ZMQ 服务器**
  - REP socket 监听
  - reset/step 命令处理
  - 高性能序列化 (protobuf/msgpack)

- [ ] **仿真优化**
  - Headless 模式
  - 物理引擎优化
  - 传感器数据压缩

- [ ] **可选：WebSocket 服务**
  - 调试模式支持
  - 前端直连

**关键文件**:
- `isaac-sim-instance/zmq_server.py`
- `isaac-sim-instance/env_wrapper.py`

### Phase 4: Nexus 集成 (3-5 天)

- [ ] **更新 DTO**
  - `SessionMode` enum
  - `TrainingConfig`
  - `TrainingInfo`

- [ ] **更新 Service**
  - `SessionService.createSession` 支持多模式
  - Kafka 消费者订阅训练事件

- [ ] **GraphQL Schema**
  - `trainingProgress` subscription
  - `trainingMetrics` query

**关键文件**:
- `src/main/scala/.../dto/request/session/CreateSessionRequest.scala`
- `src/main/scala/.../service/session/SessionServiceLive.scala`
- `src/main/scala/.../graphql/schema/SessionSchema.scala`

### Phase 5: 前端集成 (3-5 天)

- [ ] **创建训练会话 UI**
  - 选择训练算法
  - 配置超参数
  - 选择并行环境数

- [ ] **训练监控面板**
  - 实时曲线 (reward, loss)
  - 仿真状态显示
  - Episode 列表

- [ ] **可选：调试模式**
  - WebSocket 连接
  - 手动控制

### Phase 6: 端到端测试 (5-7 天)

- [ ] **性能测试**
  - ZMQ 延迟 (目标 < 1ms)
  - 吞吐量 (> 1000 steps/s)
  - GPU 利用率

- [ ] **扩展性测试**
  - 10 个并行训练任务
  - 100 个 Isaac Sim 实例

- [ ] **容错测试**
  - Isaac Sim 崩溃恢复
  - Training Instance 故障转移

---

## 性能优化建议

### 1. 网络优化

- **使用 RDMA** (如果硬件支持)
  - 延迟降至 < 100μs
  - 适合同机房部署

- **ZMQ 参数调优**
```python
socket.setsockopt(zmq.SNDHWM, 1000)  # 发送高水位
socket.setsockopt(zmq.RCVHWM, 1000)  # 接收高水位
socket.setsockopt(zmq.TCP_KEEPALIVE, 1)
```

### 2. 数据压缩

- **Observation 压缩**
  - 图像：JPEG/PNG 压缩
  - 点云：下采样 + 量化
  - 使用 protobuf/msgpack 替代 JSON

```python
# 使用 msgpack
import msgpack

# 发送
data = msgpack.packb(observation, use_bin_type=True)
socket.send(data)

# 接收
packed_data = socket.recv()
observation = msgpack.unpackb(packed_data, raw=False)
```

### 3. GPU 利用率优化

- **批量推理**: 将多个环境的 observation 合并为 batch
- **异步执行**: GPU 推理和环境仿真并行

```python
# 异步模式
async def training_loop():
    # 同时进行 GPU 推理和环境仿真
    async with asyncio.TaskGroup() as tg:
        inference_task = tg.create_task(policy.predict_async(obs))
        env_task = tg.create_task(envs.step_async(actions))
```

---

## 监控指标

### 关键指标

| 指标 | 目标 | 监控方式 |
|-----|------|---------|
| ZMQ 往返延迟 | < 1ms | Prometheus |
| Isaac Sim FPS | > 30 | Kafka event |
| Training GPU 利用率 | > 80% | nvidia-smi |
| 样本吞吐量 | > 1000 steps/s | 自定义指标 |
| 训练收敛速度 | 根据任务 | TensorBoard |

### Prometheus 配置

```yaml
# prometheus.yml
scrape_configs:
  - job_name: 'training-instances'
    static_configs:
      - targets: ['training-1:9090', 'training-2:9090']

  - job_name: 'isaac-sim-instances'
    static_configs:
      - targets: ['isaac-sim-1:9090', 'isaac-sim-2:9090']
```

---

## 故障恢复策略

### 1. Isaac Sim 崩溃

```python
# Neuro 监控
async def monitor_isaac_health():
    while True:
        for isaac in isaac_pool.instances.values():
            if not await isaac.health_check():
                # 重启实例
                await isaac.restart()

                # 重新建立通道
                training = isaac.paired_training_instance
                await establish_channel(training, isaac)

                # 通知 Training Instance 重置环境
                await training.notify_env_reset(isaac.id)

        await asyncio.sleep(10)
```

### 2. Training Instance 故障

- **Checkpoint 机制**: 定期保存模型和经验回放缓冲区
- **迁移**: 将训练任务迁移到其他实例

```python
# 每 N 个 episode 保存 checkpoint
if episode % checkpoint_interval == 0:
    torch.save({
        'episode': episode,
        'policy_state_dict': policy.state_dict(),
        'optimizer_state_dict': optimizer.state_dict(),
        'replay_buffer': replay_buffer.state
    }, f's3://checkpoints/session-{session_id}/ep-{episode}.pt')
```

---

## 成本优化

### 资源分配策略

| 场景 | 训练 GPU | Isaac Sim GPU | 说明 |
|-----|---------|--------------|------|
| 小规模验证 | 1×RTX4090 | 1×RTX4090 | 成本低 |
| 标准训练 | 4×A100 | 4×RTX4090 | 平衡性能/成本 |
| 大规模训练 | 8×H100 | 16×RTX4090 | 高吞吐 |

### Spot Instance 策略

- Training Instance 使用 Spot/Preemptible
- Isaac Sim 使用 On-Demand (更稳定)
- 通过 checkpoint 实现 Spot 中断恢复

---

## 总结

### 核心架构优势

1. **算力专用化**
   - 仿真 GPU: 图形渲染优化 (RTX 系列)
   - 训练 GPU: AI 计算优化 (A100/H100)

2. **高效训练**
   - 1 个训练实例 ↔ N 个仿真环境 (并行采样)
   - 低延迟数据交换 (ZMQ < 1ms)
   - GPU 利用率 > 80%

3. **灵活部署**
   - 支持多种运行模式 (Training/Manual/Hybrid)
   - 水平扩展
   - 按需分配

4. **成本可控**
   - 独立扩缩容
   - Spot Instance 支持
   - 资源复用

### 下一步

根据实施阶段推进，建议优先级：

1. **Phase 1-2**: Neuro 编排 + Training Instance (核心)
2. **Phase 3**: Isaac Sim ZMQ 集成
3. **Phase 4-5**: Nexus + 前端
4. **Phase 6**: 测试优化

---

**文档维护**: 如有架构调整，请及时更新本文档。

**参考资料**:
- [ZeroMQ Guide](https://zguide.zeromq.org/)
- [Isaac Sim Documentation](https://docs.omniverse.nvidia.com/isaacsim/)
- [Stable-Baselines3](https://stable-baselines3.readthedocs.io/)
