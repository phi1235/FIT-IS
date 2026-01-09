#!/bin/bash

# FIS Bank Microservices Runner
# This script starts all microservices in the background

# Load environment variables safely (POSIX compliant)
load_env() {
    local env_file=$1
    if [ -f "$env_file" ]; then
        while IFS='=' read -r key value || [ -n "$key" ]; do
            # Bỏ qua dòng trống và comment (bắt đầu bằng #)
            case "$key" in
                '#'*|'') continue ;;
            esac
            # Loại bỏ ký tự xuống dòng dư thừa nếu có (DOS style)
            key=$(echo "$key" | tr -d '\r')
            value=$(echo "$value" | tr -d '\r')
            # Export biến
            export "$key=$value"
        done < "$env_file"
    fi
}

load_env ".env"
load_env "../.env"

echo "🚀 Starting FIS Bank Microservices..."

# 1. Start Shared-lib (Build first)
echo "📦 Building shared-lib..."
./gradlew :shared-lib:build -x test

# 2. Start Microservices in Parallel
echo "⚙️  Starting services..."

nohup ./gradlew :auth-service:bootRun > auth.log 2>&1 &
echo "✅ Auth Service starting (Port $AUTH_SERVICE_PORT)..."

nohup ./gradlew :user-service:bootRun > user.log 2>&1 &
echo "✅ User Service starting (Port $USER_SERVICE_PORT)..."

nohup ./gradlew :ticket-service:bootRun > ticket.log 2>&1 &
echo "✅ Ticket Service starting (Port $TICKET_SERVICE_PORT)..."

nohup ./gradlew :report-service:bootRun > report.log 2>&1 &
echo "✅ Report Service starting (Port $REPORT_SERVICE_PORT)..."

# 3. Start Gateway last
sleep 5
nohup ./gradlew :gateway-service:bootRun > gateway.log 2>&1 &
echo "✅ Gateway Service starting (Port $GATEWAY_PORT)..."

echo "------------------------------------------------"
echo "🌐 All services are launching!"
echo "📍 Gateway: http://localhost:$GATEWAY_PORT"
echo "📄 Logs: *.log"
echo "------------------------------------------------"
