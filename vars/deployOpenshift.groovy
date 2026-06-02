def call(Map config) {
    def appName = config.appName
    def branchName = config.branch
    def buildNum = config.buildNum
    def nexusDockerUrl = config.nexusDockerUrl

    def safeBranchName = branchName.replaceAll("/", "-")
    def imageTag = "${nexusDockerUrl}/${appName}:${buildNum}-${safeBranchName}"

    echo "Tien hanh Deploy OpenShift Image: ${imageTag}"

    withCredentials([string(credentialsId: 'openshift-crc-token', variable: 'OS_TOKEN')]) {
        withEnv([
            'IMAGE_TAG=' + imageTag,
            'NEXUS_URL=' + nexusDockerUrl,
            'APP_NAME=' + appName
        ]) {
            sh '''
                set +x
                echo "1. Dang nhap OpenShift CRC..."
                oc login --token=${OS_TOKEN} --server=https://api.ocp.bankhub.s68:6443 --insecure-skip-tls-verify=true
                oc project training

                echo "3. Cap nhat Deployment..."
                if oc get deployment "$APP_NAME" >/dev/null 2>&1; then
                    oc set image deployment/"$APP_NAME" "$APP_NAME"="$IMAGE_TAG"
                else
                    oc create deployment "$APP_NAME" --image="$IMAGE_TAG"
                fi

                echo "5. Publish Service/Route..."
                oc expose deployment "$APP_NAME" --port=8080 --target-port=8080 >/dev/null 2>&1 || true
                oc expose service "$APP_NAME" >/dev/null 2>&1 || true

                echo "6. Rollout Status..."
                oc rollout status deployment/"$APP_NAME" --timeout=180s
            '''
        }
    }
}
