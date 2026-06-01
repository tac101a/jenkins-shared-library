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

    echo "Tien hanh Deploy OpenShift Image: ${imageTag}"

    withCredentials([string(credentialsId: 'openshift-crc-token', variable: 'OS_TOKEN')]) {
        withCredentials([
            usernamePassword(credentialsId: credId, passwordVariable: 'DOCKER_PASS', usernameVariable: 'DOCKER_USER'),
            usernamePassword(credentialsId: dbCredId, passwordVariable: 'DB_PASS', usernameVariable: 'DB_USER'),
            usernamePassword(credentialsId: 'node-145-cred', passwordVariable: 'NODE_PASS', usernameVariable: 'NODE_USER')
        ]) {
            withEnv([
                'IMAGE_TAG=' + imageTag,
                'NEXUS_URL=' + nexusDockerUrl,
                'APP_NAME=' + appName,
                'DB_URL=' + dbUrl
            ]) {
                sh '''
                    set +x
                    echo "1. Dang nhap OpenShift CRC..."
                    oc login --token=${OS_TOKEN} --server=https://api.ocp.bankhub.s68:6443 --insecure-skip-tls-verify=true
                    oc project training

                    echo "2. Cau hinh image pull secret..."
                    oc create secret docker-registry nexus-docker-credentials \
                        --docker-server="$NEXUS_URL" \
                        --docker-username="$DOCKER_USER" \
                        --docker-password="$DOCKER_PASS" \
                        --dry-run=client -o yaml | oc apply -f -
                    oc secrets link default nexus-docker-credentials --for=pull >/dev/null 2>&1 || true

                    echo "2.5. Cache Image on Node 145 via SSH (Bypass Insecure Registry)..."
                    sshpass -p "$NODE_PASS" ssh -o StrictHostKeyChecking=no "$NODE_USER"@10.89.25.145 "podman pull --tls-verify=false --creds=\"$DOCKER_USER:$DOCKER_PASS\" $IMAGE_TAG"

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
}
