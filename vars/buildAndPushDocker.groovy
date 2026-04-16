def call(Map config) {
    def appName = config.appName
    def branchName = config.branch
    def buildNum = config.buildNum
    def nexusDockerUrl = config.nexusDockerUrl
    def credId = config.credId

    def safeBranchName = branchName.replaceAll("/", "-")
    // Format tag: docker.abc:80/spring-petclinic:14-main
    def imageTag = "${nexusDockerUrl}/${appName}:${buildNum}-${safeBranchName}"

    echo "Bat dau tien trinh Build & Push Docker Image: ${imageTag}"

    withCredentials([usernamePassword(credentialsId: credId, passwordVariable: 'DOCKER_PASS', usernameVariable: 'DOCKER_USER')]) {
        sh """
            echo "1. Dang nhap vao Nexus Docker Registry..."
            echo "\${DOCKER_PASS}" | docker login ${nexusDockerUrl} -u "\${DOCKER_USER}" --password-stdin

            echo "2. Build Docker Image tu Dockerfile..."
            docker build -t ${imageTag} .

            echo "3. Push Docker Image len Nexus..."
            docker push ${imageTag}

            echo "4. Don dep rac (Xoa Image khoi VM2 đe giai phong dung luong)..."
            docker rmi ${imageTag}
        """
    }
}