def call(Map config) {
    def appName = config.appName
    def branchName = config.branch
    def buildNum = config.buildNum
    def nexusDockerUrl = config.nexusDockerUrl
    def credId = config.credId
    def dbUrl = config.dbUrl
    def dbCredId = config.dbCredId

    def safeBranchName = branchName.replaceAll("/", "-")
    def imageTag = "${nexusDockerUrl}/${appName}:${buildNum}-${safeBranchName}"
    def containerName = "${appName}"
    def backupName = "${appName}-backup"
    def hostPort = "8080"
    
    echo "Tien hanh Deploy Docker Image: ${imageTag}"

    withCredentials([
        usernamePassword(credentialsId: credId, passwordVariable: 'DOCKER_PASS', usernameVariable: 'DOCKER_USER'),
        usernamePassword(credentialsId: dbCredId, passwordVariable: 'DB_PASS', usernameVariable: 'DB_USER')
    ]) {
        withEnv([
            'IMAGE_TAG=' + imageTag,
            'NEXUS_URL=' + nexusDockerUrl,
            'APP_NAME=' + containerName,
            'BACKUP_NAME=' + backupName,
            'DB_URL=' + dbUrl,
            'HOST_PORT=8080'
        ]) {
            sh '''
                echo "0. Don dep tài nguyên mồ côi..."
                docker volume prune -f || true

                echo "1. Dang nhap vao Nexus..."
                echo "$DOCKER_PASS" | docker login $NEXUS_URL -u "$DOCKER_USER" --password-stdin

                echo "2. Keo (Pull) Image..."
                docker pull $IMAGE_TAG

                echo "3. Khoi tao thu muc Log..."
                sudo mkdir -p /opt/petclinic/logs
                sudo chmod 777 /opt/petclinic/logs

                echo "4. Kiem tra va Backup phien ban cu..."
                if [ "$(docker ps -aq -f name=^/${APP_NAME}$)" ]; then
                    docker stop ${APP_NAME} || true
                    if [ "$(docker ps -aq -f name=^/${BACKUP_NAME}$)" ]; then
                        docker rm -f ${BACKUP_NAME}
                    fi
                    docker rename ${APP_NAME} ${BACKUP_NAME}
                fi

                echo "5. Khoi chay Container phien ban moi..."
                docker run -d --name ${APP_NAME} -p ${HOST_PORT}:8080 \
                -v /opt/petclinic/logs:/app/logs \
                -e SPRING_DATASOURCE_URL=$DB_URL \
                -e SPRING_DATASOURCE_USERNAME="$DB_USER" \
                -e SPRING_DATASOURCE_PASSWORD="$DB_PASS" \
                $IMAGE_TAG \
                --spring.profiles.active=postgres

                echo "6. Health Check..."
                IS_HEALTHY=false
                for i in {1..15}; do
                    HTTP_STATUS=$(curl -s -o /dev/null -w "%{http_code}" http://localhost:${HOST_PORT}/actuator/health || echo "000")
                    if [ "$HTTP_STATUS" = "200" ]; then
                        IS_HEALTHY=true
                        break
                    fi
                    sleep 5
                done

                echo "7. Phan quyet Sinh tu..."
                if [ "$IS_HEALTHY" = "true" ]; then
                    if [ "$(docker ps -aq -f name=^/${BACKUP_NAME}$)" ]; then
                        docker rm -f ${BACKUP_NAME}
                    fi
                else
                    docker logs ${APP_NAME}
                    docker rm -f ${APP_NAME}
                    if [ "$(docker ps -aq -f name=^/${BACKUP_NAME}$)" ]; then
                        docker rename ${BACKUP_NAME} ${APP_NAME}
                        docker start ${APP_NAME}
                    fi
                    exit 1
                fi
            '''
        }
    }
}