pipeline {
    agent any

    environment {
        MAVEN_OPTS = '-Xmx1024m'
        DOCKER_IMAGE = 'buildwise-backend'
        IMAGE_TAG = "${BUILD_NUMBER}"
    }

    stages {
        stage('🔍 Checkout') {
            steps {
                echo "🚀 Building BuildWise Backend - feature/allocation branch"
                sh 'ls -la'
            }
        }

        stage('🏗️ Build Docker Image') {
            steps {
                echo "🐳 Building Docker image..."
                sh '''
                    # Build the Docker image
                    docker build -f docker/backend.Dockerfile -t ${DOCKER_IMAGE}:${IMAGE_TAG} .
                    docker tag ${DOCKER_IMAGE}:${IMAGE_TAG} ${DOCKER_IMAGE}:latest

                    echo "📦 Docker image built successfully!"
                    docker images | grep ${DOCKER_IMAGE}
                '''
            }
        }

        stage('🚀 Deploy with Docker Compose') {
            steps {
                echo "🚀 Deploying with Docker Compose..."
                sh '''
                    # Stop any existing containers
                    docker-compose down || true

                    # Start infrastructure first
                    docker-compose up -d postgres minio

                    # Wait for database
                    echo "⏳ Waiting for database..."
                    sleep 15

                    # Start backend
                    SPRING_PROFILES_ACTIVE=docker docker-compose up -d backend

                    echo "🎉 All services started!"
                    docker-compose ps
                '''
            }
        }

        stage('🏥 Health Check') {
            steps {
                echo "🏥 Checking application health..."
                sh '''
                    # Wait for application to start
                    echo "⏳ Waiting for application startup..."
                    sleep 30

                    # Check container status
                    if docker ps | grep buildwise-backend; then
                        echo "✅ Backend container is running"

                        # Check health endpoint
                        for i in {1..5}; do
                            if curl -f http://localhost:8082/actuator/health; then
                                echo "✅ Health check passed!"
                                break
                            else
                                echo "⏳ Health check attempt $i failed, retrying..."
                                sleep 10
                            fi

                            if [ $i -eq 5 ]; then
                                echo "❌ Health check failed"
                                docker logs buildwise-backend
                                exit 1
                            fi
                        done
                    else
                        echo "❌ Backend container not running"
                        docker-compose logs backend
                        exit 1
                    fi
                '''
            }
        }
    }

    post {
        success {
            echo "🎉 Docker deployment succeeded!"
            sh '''
                echo "📊 Deployment Status:"
                docker-compose ps
                echo "📋 Backend logs (last 10 lines):"
                docker logs --tail 10 buildwise-backend
            '''
        }
        failure {
            echo "❌ Docker deployment failed!"
            sh '''
                echo "📋 Container logs:"
                docker-compose logs
            '''
        }
    }
}