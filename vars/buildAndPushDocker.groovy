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

    withCredentials([string(credentialsId: 'openshift-crc-token', variable: 'OS_TOKEN')]) {
        withEnv([
            'IMAGE_TAG=' + imageTag,
            'REGISTRY_URL=' + 'image-registry.openshift-image-registry.svc:5000'
        ]) {
            container('buildah') {
                sh '''
                    echo "1. Dang nhap vao OpenShift Internal Registry..."
                    buildah login --tls-verify=false -u serviceaccount -p "$OS_TOKEN" $REGISTRY_URL

                    echo "2. Build Docker Image tu Dockerfile..."
                    buildah bud --tls-verify=false -t $IMAGE_TAG .

                    echo "3. Push Docker Image len Internal Registry..."
                    buildah push --tls-verify=false --format docker $IMAGE_TAG

                    echo "4. Don dep rac..."
                    buildah rmi $IMAGE_TAG
                '''
            }
        }
    }
}
