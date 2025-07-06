# File Operation Service (FOS)

A distributed file system service providing secure file operations through JSON-RPC 2.0 API with Redis-based distributed locking for concurrent access control.

## Overview

The File Operation Service implements a scalable architecture supporting horizontal scaling through Redis-based coordination, ensuring data consistency across multiple service instances while maintaining operational simplicity.

## Features

- **JSON-RPC 2.0 API** - Standard protocol for file system operations
- **Distributed Locking** - Redis-based coordination for atomic append operations
- **Configurable Root Directory** - Flexible storage configuration
- **Health Monitoring** - Kubernetes-ready health endpoints
- **Comprehensive Testing** - Unit and integration test coverage
- **Container Deployment** - Docker and Helm chart support

## API Operations

- Retrieve file/folder information (name, path, size)
- List directory contents
- Create empty files/folders
- Delete files/folders
- Move/copy files/folders
- Append data to files (with distributed locking)
- Read N bytes from files at specific offset

## Technology Stack

- **Build Tool:** Gradle
- **DI Framework:** Google Guice
- **Application Server:** Jetty
- **Distributed Cache:** Redis
- **Container:** Docker
- **Orchestration:** Kubernetes with Helm

## Prerequisites

- [JDK 21](https://openjdk.org/projects/jdk/21/)
- [Gradle](https://docs.gradle.org/current/userguide/installation.html)
- [Docker Desktop](https://www.docker.com/products/docker-desktop/)
- [kubectl](https://kubernetes.io/docs/tasks/tools/)
- [Minikube](https://minikube.sigs.k8s.io/docs/start/)
- [Helm](https://helm.sh/docs/intro/install/)

### macOS Installation

```bash
# Docker Desktop
brew install --cask docker

# Kubernetes CLI
brew install kubectl

# Local Kubernetes cluster
brew install minikube

# Kubernetes package manager
brew install helm
```

## Configuration

Root folder configuration supports multiple sources (by priority):

1. **Command Line Arguments:** `java -jar app.jar /path/to/root`
2. **Environment Variables:** `export FOS_ROOT_DIR=/path/to/root`
3. **JVM System Properties:** `-Dfos.root.dir=/path/to/root`

## Local Development

### Build and Test

```bash
# Build project
gradle clean build

# Run unit tests
gradle test

# Create local root directory
mkdir -p /var/tmp/fos

# Start server locally
gradle run --args="/var/tmp/fos dummy 0 dummy"
```

### Integration Testing

```bash
# Configure environment
export INTEG_TEST=ENABLED
export FOS_SERVER_BASE_URL=http://localhost:8080

# Run basic integration tests (without append operations)
gradle test --tests 'com.fos.integ.FileOperationServiceNonAppendIntegrationTest'

# Run full integration tests (requires Redis)
export INTEG_TEST_TEST_ALL_OPERATION=ENABLED
gradle test --tests 'com.fos.integ.FileOperationServiceIntegrationTest'
```

## Kubernetes Deployment

### 1. Setup Local Environment

```bash
# Start Docker Desktop
docker desktop start

# Create local storage directory
mkdir -p /var/tmp/fos

# Start Minikube with mounted volume
minikube start --mount-string="/var/tmp/fos:/fosroot" --mount
```

### 2. Install Redis

```bash
# Install Redis for distributed locking
helm install fos-redis bitnami/redis

# Get Redis password
echo REDIS_PASSWORD=$(kubectl get secret --namespace default fos-redis -o jsonpath="{.data.redis-password}" | base64 -d)
```

### 3. Build and Deploy Service

```bash
# Update Dockerfile with Redis password
# Edit REDIS_PASSWORD in Dockerfile

# Build Docker image
docker build -t fos .

# Load image to Minikube
minikube image load fos:latest

# Deploy with Helm
helm install fos-service ./src/main/resources/helm/fos-chart
```

### 4. Access and Test

```bash
# Get service URL
minikube service fos-service-fos-chart --url

# Test health endpoint
curl -X GET http://localhost:<port>/health

# Run full integration tests
export INTEG_TEST_TEST_ALL_OPERATION=ENABLED
export FOS_SERVER_BASE_URL=<minikube_service_url>
gradle test --tests 'com.fos.integ.FileOperationServiceIntegrationTest'
```

## Architecture

### Scalability Design

- **Multi-Instance:** Redis distributed locking for cross-instance coordination

### Concurrency Control

Only append operations require synchronization:
- Other operations (create, read, delete, list) are not synchronized
- Distributed locking ensures atomic append operations across instances
- Redis TTL prevents lock leakage with millisecond precision

## Testing

### Coverage Report

After building, view coverage at: `build/reports/jacoco/test/html/index.html`

### Test Categories

- **Unit Tests:** Component-level validation
- **Integration Tests:** End-to-end API validation
- **Basic Integration:** All operations except append
- **Full Integration:** Complete functionality including distributed locking

## Logging

Comprehensive logging with console output for operational visibility. Customize logging behavior by modifying the log4j configuration.

## Future Enhancements

- **Dynamic Root Configuration:** Runtime root folder reconfiguration
- **Multi-tenant Support:** Isolated workspaces per tenant
- **Enhanced Durability:** Synchronous replication across storage systems
- **CI/CD Integration:** Automated integration testing pipeline

## Source Code

Complete implementation with commit history available on GitHub.

## License

This project is part of a technical assessment demonstrating distributed system design and implementation capabilities.