#!/bin/bash

# Usage: ./application.sh [--start] [--kill] [--restart]

START=false
KILL=false
RESTART=false

for arg in "$@"; do
    case $arg in
        --start)
            START=true
            ;;
        --kill)
            KILL=true
            ;;
        --restart)
            RESTART=true
            ;;
    esac
done

# If no arguments provided, show usage
if [ "$START" = false ] && [ "$KILL" = false ] && [ "$RESTART" = false ]; then
    echo "Usage: ./application.sh [--start] [--kill] [--restart]"
    echo ""
    echo "  --start    Start the application (skipping tests)"
    echo "  --kill     Stop the application"
    echo "  --restart  Stop and then start the application"
    exit 1
fi

port_in_use() {
    ss -tulpn | grep ":$1" >/dev/null 2>&1
}

stop_application() {
    echo "Stopping Application..."
    
    # Stop backend (both Maven wrapper and Quarkus processes)
    echo "Stopping backend..."
    pkill -f "quarkus:dev" 2>/dev/null || echo "Maven quarkus:dev not running"
    pkill -f "core-management-dev.jar" 2>/dev/null || echo "Quarkus app not running"
    
    # Stop Docker containers
    echo "Stopping Docker containers..."
    docker stop mariadb-stayinsync rabbitmq 2>/dev/null || echo "Containers already stopped"
    
    echo "Application stopped!"
}

start_application() {
    echo "Starting Application..."
    
    # Start MariaDB
    if port_in_use 3306; then
        echo "MariaDB already running"
    else
        echo "Starting MariaDB..."
        docker rm -f mariadb-stayinsync 2>/dev/null || true
        docker run -d \
            --name mariadb-stayinsync \
            -e MYSQL_ROOT_PASSWORD=root \
            -e MYSQL_DATABASE=stayinsync_core \
            -p 3306:3306 \
            mariadb:latest
    fi

    # Start RabbitMQ
    if port_in_use 5672; then
        echo "RabbitMQ already running"
    else
        echo "Starting RabbitMQ..."
        docker rm -f rabbitmq 2>/dev/null || true
        docker run -d \
            --name rabbitmq \
            -p 5672:5672 \
            -p 15672:15672 \
            rabbitmq:3-management
    fi

    # Build application
    echo "Building application (skipping tests)..."
    mvn clean install -DskipTests
    BUILD_EXIT_CODE=$?
    if [ $BUILD_EXIT_CODE -eq 0 ]; then
        echo "Build completed successfully!"
    else
        echo "Build failed!"
        exit 1
    fi

    # Start backend
    if port_in_use 8090; then
        echo "Backend already running on port 8090"
    else
        echo "Starting backend in background..."
        cd stay-in-sync-core/core-management
        
        nohup ./mvnw quarkus:dev -Dquarkus.test.continuous-testing=disabled > ../../backend.log 2>&1 &
        BACKEND_PID=$!
        echo "Backend started with PID: $BACKEND_PID"
        echo "Logs are being written to: backend.log"
        
        cd ../..
        
        sleep 5
        if ! kill -0 $BACKEND_PID 2>/dev/null; then
            echo "Backend failed to start! Check backend.log for details."
            exit 1
        fi
        
        # Wait for HTTP response to confirm startup
        echo "Waiting for backend to be ready..."
        TIMEOUT=60
        ELAPSED=0
        while [ $ELAPSED -lt $TIMEOUT ]; do
            if curl -s http://localhost:8090 >/dev/null 2>&1; then
                echo "Backend is ready!"
                break
            fi
            if ! kill -0 $BACKEND_PID 2>/dev/null; then
                echo "Backend process died during startup! Check backend.log for details."
                exit 1
            fi
            sleep 3
            ELAPSED=$((ELAPSED + 3))
        done
        
        if ! curl -s http://localhost:8090 >/dev/null 2>&1; then
            echo "Backend did not respond within $TIMEOUT seconds. Check backend.log for details."
            echo "Backend process is still running with PID: $BACKEND_PID"
        fi
    fi

    echo ""
    echo "Application started successfully!"
    echo "Webapp: http://localhost:8090"
    echo "RabbitMQ: http://localhost:15672"
    echo ""
    echo "Use './application.sh --kill' to stop the application"
    echo "Use 'tail -f backend.log' to monitor backend logs"
}

if [ "$RESTART" = true ]; then
    stop_application
    echo ""
    start_application
elif [ "$KILL" = true ]; then
    stop_application
elif [ "$START" = true ]; then
    start_application
fi
