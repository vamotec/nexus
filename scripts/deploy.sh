#!/bin/bash
# deploy.sh

set -e

NAMESPACE="production"
IMAGE_TAG="${1:-latest}"

echo "🚀 Deploying RoboSim Nexus (${IMAGE_TAG})"

# 1. 应用配置
echo "📝 Applying configurations..."
kubectl apply -f k8s/configmap.yaml
kubectl apply -f k8s/secret.yaml

# 2. 运行数据库迁移
echo "🗄️  Running database migration..."
kubectl delete job nexus-db-migration -n ${NAMESPACE} --ignore-not-found
kubectl apply -f k8s/migration-job.yaml

# 3. 等待迁移完成
echo "⏳ Waiting for migration to complete..."
kubectl wait --for=condition=complete \
  --timeout=600s \
  job/nexus-db-migration \
  -n ${NAMESPACE}

# 检查迁移日志
echo "📋 Migration logs:"
kubectl logs -l component=migration -n ${NAMESPACE} --tail=50

# 4. 部署应用
echo "🚢 Deploying application..."
kubectl apply -f k8s/deployment.yaml

# 5. 等待应用就绪
echo "⏳ Waiting for application to be ready..."
kubectl rollout status deployment/nexus-api -n ${NAMESPACE}

echo "✅ Deployment completed successfully!"