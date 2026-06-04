def call(Map config) {
    def appName = config.appName
    def branchName = config.branch
    def buildNum = config.buildNum
    def nexusDockerUrl = config.nexusDockerUrl
    def credId = config.credId

    def safeBranchName = branchName.replaceAll("/", "-")
    def imageTag = "${nexusDockerUrl}/${appName}:${buildNum}-${safeBranchName}"

    echo "Proceeding with OpenShift Image Deployment: ${imageTag}"

    withCredentials([string(credentialsId: 'openshift-crc-token', variable: 'OS_TOKEN')]) {
        withCredentials([
            usernamePassword(credentialsId: credId, passwordVariable: 'DOCKER_PASS', usernameVariable: 'DOCKER_USER')
        ]) {
            withEnv([
                'IMAGE_TAG=' + imageTag,
                'NEXUS_URL=' + nexusDockerUrl,
                'APP_NAME=' + appName
            ]) {
                sh '''
                    set +x
                    echo "1. Logging into OpenShift CRC..."
                    oc login --token=${OS_TOKEN} --server=https://api.ocp.bankhub.s68:6443 --insecure-skip-tls-verify=true
                    oc project training

                    echo "2. Configuring image pull secret..."
                    oc create secret docker-registry nexus-docker-credentials \\
                        --docker-server="$NEXUS_URL" \\
                        --docker-username="$DOCKER_USER" \\
                        --docker-password="$DOCKER_PASS" \\
                        --dry-run=client -o yaml | oc apply -f -
                    oc secrets link default nexus-docker-credentials --for=pull >/dev/null 2>&1 || true

                    echo "3. Updating Deployment..."
                    if oc get deployment "$APP_NAME" >/dev/null 2>&1; then
                        oc set image deployment/"$APP_NAME" "$APP_NAME"="$IMAGE_TAG"
                    else
                        oc create deployment "$APP_NAME" --image="$IMAGE_TAG"
                    fi

                    echo "4. Publishing Service/Route..."
                    oc expose deployment "$APP_NAME" --port=8080 --target-port=8080 >/dev/null 2>&1 || true
                    oc expose service "$APP_NAME" >/dev/null 2>&1 || true

                    echo "5. Checking Rollout Status..."
                    oc rollout status deployment/"$APP_NAME" --timeout=180s
                '''
            }
        }
    }
}
