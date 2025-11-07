graph TB
A[用户] --> B[API Gateway]
B --> C[调度服务]
C --> D[GPU Pod 1]
C --> E[GPU Pod 2]
C --> F[GPU Pod N]
D --> G[Isaac Sim Instance 1]
E --> H[Isaac Sim Instance 2]
F --> I[Isaac Sim Instance N]
G --> J[共享存储]
H --> J
I --> J