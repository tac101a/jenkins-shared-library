def call(Map config) {
    def appName = config.appName
    def branchName = config.branch
    def buildNum = config.buildNum
    def nexusDockerUrl = config.nexusDockerUrl
    def credId = config.credId

    def safeBranchName = branchName.replaceAll("/", "-")
    def imageTag = "${nexusDockerUrl}/${appName}:${buildNum}-${safeBranchName}"

    echo "Bat dau tien trinh Build & Push Docker Image: ${imageTag}"

    withCredentials([usernamePassword(credentialsId: credId, passwordVariable: 'DOCKER_PASS', usernameVariable: 'DOCKER_USER')]) {
        withEnv([
            'IMAGE_TAG=' + imageTag,
            'NEXUS_URL=' + nexusDockerUrl
        ]) {
            container('buildah') {
                sh '''
                    echo "1. Dang nhap vao Nexus Docker Registry..."
                    buildah login --tls-verify=false -u "$DOCKER_USER" -p "$DOCKER_PASS" $NEXUS_URL

                    echo "2. Build Docker Image tu Dockerfile..."
                    buildah bud --tls-verify=false -t $IMAGE_TAG .

                    echo "3. Push Docker Image len Nexus..."
                    buildah push --tls-verify=false --format docker $IMAGE_TAG

                    echo "4. Don dep rac..."
                    buildah rmi $IMAGE_TAG
                '''
            }
        }
    }
}
