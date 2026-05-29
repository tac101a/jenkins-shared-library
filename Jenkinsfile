@Library('anhcnt1-shared-library') _

pipeline {
    agent {
        kubernetes {
            cloud 'openshift4'
            yaml '''
            apiVersion: v1
            kind: Pod
            spec:
              volumes:
                - name: buildah-storage
                  emptyDir: {}
              containers:
              - name: maven
                image: 10.89.25.146:9006/jenkins/maven:3.9.12-eclipse-temurin-21-noble
                command: ['cat']
                tty: true
                env:
                  - name: MAVEN_OPTS
                    value: "-Xmx1024m"
                resources:
                  requests:
                    memory: "1Gi"
                    cpu: "250m"
                  limits:
                    memory: "2Gi"
                    cpu: "500m"
              - name: buildah
                image: 10.89.25.146:9006/jenkins/buildah:v1.38-stable
                command: ['cat']
                tty: true
                securityContext:
                  privileged: true
                resources:
                  requests:
                    memory: "1Gi"
                  limits:
                    memory: "2Gi"
                volumeMounts:
                  - name: buildah-storage
                    mountPath: /var/lib/containers
            '''
        }
    }
    
    environment {
        APP_NAME = 'spring-petclinic'
        GITHUB_REPO_DOMAIN = '10.89.25.145/devops-training/2026/anhcnt1/spring-petclinic.git'
        SONAR_SERVER_NAME = 'sonar-server'
        NEXUS_URL = 'http' + '://10.89.25.146:8081/repository/maven-releases'
        NEXUS_DOCKER_URL = '10.89.25.146:8082'
        DB_URL = 'jdbc:postgresql://10.0.0.5:5432/petclinic'
        DB_CREDENTIALS_ID = 'postgres-credentials'
        GIT_CREDENTIALS_ID = 'gitlab-token-credentials'
        NEXUS_CREDENTIALS_ID = 'nexus-credentials'
    }

    stages {
        // GROUP 1: RUN FOR ALL (develop, uat, main, PR)
        stage('stage 1: Compile') {
            steps {
                mavenExecute('clean compile')
            }
        }
        
        stage('stage 2: Unit Test') {
            steps {
                mavenExecute('test')
            }
        }
        
        stage('stage 3: SonarQube') {
            steps {
                sonarScan(env.SONAR_SERVER_NAME)
            }
        }

        // GROUP 2: RUN FOR ALL 3 ENVIRONMENTS (DEV, UAT, MAIN)
        stage('stage 4: Deploy Nexus') {
            when {
                anyOf {
                    branch 'develop/*'
                    branch 'uat/*'
                    branch 'main'
                }
            }
            steps {
                mavenExecute('package -DskipTests')
                uploadToNexus(
                    branch: env.BRANCH_NAME,
                    buildNum: env.BUILD_NUMBER,
                    nexusUrl: env.NEXUS_URL,
                    credId: env.NEXUS_CREDENTIALS_ID
                )
            }
        }

        stage('stage 4.5: Build & Push Docker') {
            when {
                anyOf {
                    branch 'develop/*'
                    branch 'uat/*'
                    branch 'main'
                }
            }
            steps {
                buildAndPushDocker(
                    appName: env.APP_NAME,
                    branch: env.BRANCH_NAME,
                    buildNum: env.BUILD_NUMBER,
                    nexusDockerUrl: env.NEXUS_DOCKER_URL,
                    credId: env.NEXUS_CREDENTIALS_ID
                )
            }
        }
        
        stage('stage 5: Deploy App & Health Check') {
            when {
                anyOf {
                    branch 'develop/*'
                    branch 'uat/*'
                    branch 'main'
                }
            }
            steps {
                deployDockerApp(
                    appName: env.APP_NAME,
                    branch: env.BRANCH_NAME,
                    buildNum: env.BUILD_NUMBER,
                    nexusDockerUrl: env.NEXUS_DOCKER_URL,
                    dbUrl: env.DB_URL,
                    dbCredId: env.DB_CREDENTIALS_ID,
                    credId: env.NEXUS_CREDENTIALS_ID
                )
            }
        }

        // GROUP 3: RUN ONLY FOR UAT AND MAIN
        stage('stage 6: Auto-Tagging') {
            when { 
                beforeAgent true
                anyOf {
                    branch 'uat/*'
                    branch 'main'
                }
            }
            steps {
                createGitTag(
                    branch: env.BRANCH_NAME,
                    buildNum: env.BUILD_NUMBER,
                    gitRepoDomain: env.GITHUB_REPO_DOMAIN,
                    credId: env.GIT_CREDENTIALS_ID
                )
            }
        }
    }
    post {
        success {
            notifySlack(
                status: 'SUCCESS',
                appName: env.APP_NAME,
                branch: env.BRANCH_NAME,
                buildNum: env.BUILD_NUMBER,
                buildUrl: env.BUILD_URL
            )
        }
        failure {
            notifySlack(
                status: 'FAILURE',
                appName: env.APP_NAME,
                branch: env.BRANCH_NAME,
                buildNum: env.BUILD_NUMBER,
                buildUrl: "${env.BUILD_URL}console"
            )
        }
    }
}
