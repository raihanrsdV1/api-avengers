#!/bin/bash

echo "🔍 CareForAll Platform - Status Check"
echo "====================================="
echo ""

# Function to check a service
check_service() {
    local name=$1
    local url=$2
    local expected_status=${3:-200}
    
    printf "%-25s" "$name..."
    
    response=$(curl -s -o /dev/null -w "%{http_code}" "$url")
    
    if [ "$response" -eq "$expected_status" ]; then
        echo "✅ UP (HTTP $response)"
    else
        echo "❌ DOWN (HTTP $response)"
    fi
}

# Check Docker Containers
echo "� Container Status:"
if docker-compose ps | grep -q "Up"; then
    docker-compose ps --format "table {{.Service}}\t{{.State}}\t{{.Status}}"
else
    echo "❌ Docker Compose is not running."
    exit 1
fi
echo ""

# Check Application Services
echo "🏥 Service Health Checks:"
check_service "API Gateway" "http://localhost:8080/actuator/health"
check_service "Payment Service" "http://localhost:8081/api/v1/payment/health"
check_service "Campaign Service" "http://localhost:8082/api/v1/campaign/health"
check_service "Pledge Service" "http://localhost:8083/api/v1/pledge/health"
check_service "Mock Gateway" "http://localhost:8084/api/v1/mock-gateway/health"
echo ""

# Check Infrastructure
echo "🏗️  Infrastructure Health Checks:"
check_service "Zipkin" "http://localhost:9411/health"
check_service "Prometheus" "http://localhost:9090/-/healthy"
check_service "Grafana" "http://localhost:3000/api/health"
check_service "Kibana" "http://localhost:5601/api/status"
echo ""

echo "🌐 Access Points:"
echo "  API Gateway:  http://localhost:8080"
echo "  Zipkin:       http://localhost:9411"
echo "  Prometheus:   http://localhost:9090"
echo "  Grafana:      http://localhost:3000"
echo "  Kibana:       http://localhost:5601"
echo ""
