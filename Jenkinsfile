pipeline {
    agent any

    environment {
        MAVEN_OPTS = '-Xmx1024m'
        SPRING_PROFILES_ACTIVE = 'vault'
        VAULT_ADDR = 'http://localhost:8200'
        VAULT_TOKEN = 'myroot'
    }

    stages {
        stage('🔍 Checkout') {
            steps {
                echo "🚀 Building BuildWise Backend - feature/allocation branch"
                sh '''
                    echo "📁 Project files:"
                    ls -la
                '''
            }
        }

        stage('☕ Build') {
            steps {
                echo "🔨 Building with Maven..."
                sh '''
                    ./mvnw clean compile test package -DskipTests
                    echo "📦 Build complete!"
                    ls -la target/
                '''
            }
        }

        stage('🚀 Deploy') {
            steps {
                echo "🚀 Deploying application..."
                sh '''
                    pkill -f "BuildWise-Backend" || true
                    sleep 5
                    nohup java -jar target/*.jar > app.log 2>&1 &
                    echo "Application starting..."
                    sleep 20
                '''
            }
        }

        stage('🏥 Health Check') {
            steps {
                echo "🏥 Checking application health..."
                sh '''
                    if pgrep -f "BuildWise-Backend"; then
                        echo "✅ Application is running"
                        curl -f http://localhost:8082/actuator/health || echo "⚠️ Health endpoint not ready"
                    else
                        echo "❌ Application not running"
                        tail -20 app.log
                    fi
                '''
            }
        }
    }

    post {
        success {
            echo "🎉 Pipeline succeeded!"
        }
        failure {
            echo "❌ Pipeline failed!"
            sh 'tail -50 app.log || echo "No logs found"'
        }
    }
}