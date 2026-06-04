def call(Map config) {
    def appName = config.appName
    def branchName = config.branch
    def buildNum = config.buildNum
    def nexusDockerUrl = config.nexusDockerUrl
    def credId = config.credId

    def safeBranchName = branchName.replaceAll("/", "-")
    def imageTag = "${nexusDockerUrl}/${appName}:${buildNum}-${safeBranchName}"

    echo "Starting Build & Push Docker Image process: ${imageTag}"

    withCredentials([usernamePassword(credentialsId: credId, passwordVariable: 'DOCKER_PASS', usernameVariable: 'DOCKER_USER')]) {
        withEnv([
            'IMAGE_TAG=' + imageTag,
            'NEXUS_URL=' + nexusDockerUrl
        ]) {
            container('buildah') {
                sh '''
                    echo "1. Logging into Nexus Docker Registry..."
                    buildah login --tls-verify=false -u "$DOCKER_USER" -p "$DOCKER_PASS" $NEXUS_URL

                    echo "2. Building Docker Image from Dockerfile..."
                    buildah bud --tls-verify=false -t $IMAGE_TAG .

                    echo "3. Pushing Docker Image to Nexus..."
                    buildah push --tls-verify=false --format docker $IMAGE_TAG

                    echo "4. Cleaning up dangling images..."
                    buildah rmi $IMAGE_TAG
                '''
            }
        }
    }
}
