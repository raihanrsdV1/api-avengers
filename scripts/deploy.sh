#!/bin/bash

# CareForAll Platform - Deployment Script
# This script builds and starts the entire platform

set -e

echo "🚀 CareForAll Platform - Full Stack Deployment"
echo "=============================================="
echo ""

# Colors
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Step 1: Clean up old infrastructure (if exists)
echo "${YELLOW}📦 Step 1: Cleaning up old containers...${NC}"
docker-compose down -v 2>/dev/null || true
echo ""

# Step 2: Build services
echo "${YELLOW}🔨 Step 2: Building Java services...${NC}"
echo "This may take 5-10 minutes on first run..."
docker-compose build --parallel
echo "${GREEN}✅ Build complete${NC}"
echo ""

# Step 3: Start infrastructure
echo "${YELLOW}🏗️  Step 3: Starting infrastructure services...${NC}"
docker-compose up -d postgres-payment postgres-campaign redis rabbitmq zipkin prometheus grafana elasticsearch kibana
echo "${GREEN}✅ Infrastructure started${NC}"
echo ""

# Step 4: Wait for infrastructure
echo "${YELLOW}⏳ Step 4: Waiting for infrastructure to be healthy...${NC}"
echo "Waiting 30 seconds for databases and message brokers..."
sleep 30
echo "${GREEN}✅ Infrastructure ready${NC}"
echo ""

# Step 5: Start application services
echo "${YELLOW}🎯 Step 5: Starting application services...${NC}"
docker-compose up -d payment-service campaign-service
echo "Waiting 20 seconds for services to initialize..."
sleep 20
echo "${GREEN}✅ Services started${NC}"
echo ""

# Step 6: Start API Gateway
echo "${YELLOW}🌐 Step 6: Starting API Gateway...${NC}"
docker-compose up -d api-gateway
echo "Waiting 15 seconds for gateway to start..."
sleep 15
echo "${GREEN}✅ Gateway started${NC}"
echo ""

# Step 7: Health checks
echo "${YELLOW}🏥 Step 7: Running health checks...${NC}"
echo ""

check_health() {
    local service_name=$1
    local url=$2
    
    if curl -s "$url" > /dev/null 2>&1; then
        echo "${GREEN}✅ $service_name is healthy${NC}"
        return 0
    else
        echo "❌ $service_name is not responding"
        return 1
    fi
}

check_health "API Gateway" "http://localhost:8080/actuator/health"
check_health "Payment Service" "http://localhost:8081/api/v1/payment/health"
check_health "Campaign Service" "http://localhost:8082/api/v1/campaign/health"
check_health "RabbitMQ Management" "http://localhost:15672"
check_health "Zipkin" "http://localhost:9411"
check_health "Prometheus" "http://localhost:9090"
check_health "Grafana" "http://localhost:3000"
check_health "Elasticsearch" "http://localhost:9200"
check_health "Kibana" "http://localhost:5601"

echo ""
echo "=============================================="
echo "${GREEN}🎉 Deployment Complete!${NC}"
echo "=============================================="
echo ""
echo "📊 Access Points:"
echo "  - API Gateway:        http://localhost:8080"
echo "  - Payment Service:    http://localhost:8081"
echo "  - Campaign Service:   http://localhost:8082"
echo ""
echo "🔍 Observability:"
echo "  - Zipkin (Tracing):   http://localhost:9411"
echo "  - Prometheus:         http://localhost:9090"
echo "  - Grafana:            http://localhost:3000 (admin/admin)"
echo "  - Kibana (Logs):      http://localhost:5601"
echo "  - RabbitMQ UI:        http://localhost:15672 (guest/guest)"
echo ""
echo "🧪 Run tests:"
echo "  ./scripts/test-full-stack.sh"
echo ""
