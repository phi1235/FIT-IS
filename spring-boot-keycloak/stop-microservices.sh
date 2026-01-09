#!/bin/bash

echo "🛑 Stopping FIS Bank Microservices..."
pkill -f 'bootRun'
pkill -f 'spring-boot-keycloak'
echo "✅ All services stopped."
