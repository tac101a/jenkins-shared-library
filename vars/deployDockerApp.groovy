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
        sh """
            echo "1. Dang nhap vao Nexus Docker Registry..."
            echo "\${DOCKER_PASS}" | docker login ${nexusDockerUrl} -u "\${DOCKER_USER}" --password-stdin
            
            echo "2. Keo (Pull) Image phien ban moi nhat ve VM2..."
            docker pull ${imageTag}

            echo "3. Khoi tao thu muc Log tren Host (Dam bao phan quyen cho Non-root user)..."
            sudo mkdir -p /opt/petclinic/logs
            sudo chmod 777 /opt/petclinic/logs

            echo "4. Kiem tra va Backup phien ban cu (Co may thoi gian)..."
            if [ "\$(docker ps -aq -f name=^/${containerName}\$)" ]; then
                echo "Tim thay container cu. Dang tam dung va tao backup..."
                docker stop ${containerName} || true
                # Xoa backup cu neu co
                if [ "\$(docker ps -aq -f name=^/${backupName}\$)" ]; then
                    docker rm -f ${backupName}
                fi
                docker rename ${containerName} ${backupName}
            else
                echo "Khong co container cu. Day la lan deploy dau tien."
            fi

            echo "5. Khoi chay Container phien ban moi (Mount Log)..."
            docker run -d --name ${containerName} -p ${hostPort}:8080 \\
            -v /opt/petclinic/logs:/app/logs \\
            -e SPRING_DATASOURCE_URL=${dbUrl} \\
            -e SPRING_DATASOURCE_USERNAME="\$DB_USER" \\
            -e SPRING_DATASOURCE_PASSWORD="\$DB_PASS" \\
            ${imageTag} \\
            --spring.profiles.active=postgres

            echo "6. Health Check (Cho Spring Boot khoi đong)..."
            IS_HEALTHY=false
            for i in {1..15}; do
                HTTP_STATUS=\$(curl -s -o /dev/null -w "%{http_code}" http://localhost:${hostPort}/actuator/health || echo "000")
                if [ "\$HTTP_STATUS" = "200" ]; then
                    echo "Ung dung đa SỐNG (HTTP 200) sau \$i lan thu!"
                    IS_HEALTHY=true
                    break
                fi
                echo "Cho ung dung khoi đong... (Lan \$i/15) - HTTP Status: \$HTTP_STATUS"
                sleep 5
            done

            echo "7. Phan quyet Sinh tu (Rollback Logic)..."
            if [ "\$IS_HEALTHY" = "true" ]; then
                echo "DEPLOY THÀNH CÔNG! Dang xoa ban backup đe giai phong tai nguyen..."
                if [ "\$(docker ps -aq -f name=^/${backupName}\$)" ]; then
                    docker rm -f ${backupName}
                fi
            else
                echo "🚨 DEPLOY THẤT BẠI! Ung dung chet lam sang."
                echo "================================================="
                echo "🔍 TRÍCH XUẤT LOG CONTAINER TRƯỚC KHI BỊ TIÊU DIỆT:"
                docker logs ${containerName}
                echo "================================================="
                echo "KÍCH HOẠT ROLLBACK... Xoa container phien ban loi..."
                docker rm -f ${containerName}
                
                if [ "\$(docker ps -aq -f name=^/${backupName}\$)" ]; then
                    echo "Hoi sinh ban backup: ${backupName} -> ${containerName}"
                    docker rename ${backupName} ${containerName}
                    docker start ${containerName}
                    echo "He thong đa đuoc khoi phuc ve trang thai an toan!"
                else
                    echo "Khong co ban backup nao đe hoi sinh. He thong đang DOWN!"
                fi

                exit 1
            fi
        """
    }
}