def call(Map config) {
	def branchName = config.branch
	def buildNum = config.buildNum
	def nexusUrl = config.nexusUrl
	def credId = config.credId

	withEnv(["APP_BRANCH=${branchName}", "BUILD_NUM=${buildNum}", "NEXUS_URL=${nexusUrl}"]) {
		echo "1. Tai file artifact tu Nexus ve VM2..."
		withCredentials([usernamePassword(credentialsId: credId, passwordVariable: 'NEXUS_PSW', usernameVariable: 'NEXUS_USR')]) {
			sh '''
				SAFE_BRANCH_NAME=$(echo "$APP_BRANCH" | tr '/' '-')
				curl -fSsl -u "$NEXUS_USR:$NEXUS_PSW" -o app.jar "$NEXUS_URL/com/fpt/petclinic/petclinic/$BUILD_NUM-$SAFE_BRANCH_NAME/petclinic-$BUILD_NUM-$SAFE_BRANCH_NAME.jar"
			'''
		}

		echo "2. Graceful Shutdown & Start App..."
		sh '''
			SAFE_BRANCH_NAME=$(echo "$APP_BRANCH" | tr '/' '-')
			APP_FILE="app-${SAFE_BRANCH_NAME}.jar"

			PID=$(pgrep -f "$APP_FILE") || true
			if [ -n "$PID" ]; then
				kill -15 $PID; sleep 5; kill -9 $PID 2>/dev/null || true
			fi
			mv app.jar $APP_FILE

			case "$APP_BRANCH" in
				develop/*) SERVER_PORT=8082 ;;
				uat/*)     SERVER_PORT=8081 ;;
				*)         SERVER_PORT=8080 ;;
			esac

			BUILD_ID=dontKillMe JENKINS_NODE_COOKIE=dontKillMe nohup java -Xmx256m -jar $APP_FILE --server.port=$SERVER_PORT > app.log 2>&1 &
		'''

		echo "3. Health Check..."
		sh '''
			case "$APP_BRANCH" in
				develop/*) SERVER_PORT=8082 ;;
				uat/*)     SERVER_PORT=8081 ;;
				*)         SERVER_PORT=8080 ;;
			esac

			MAX_RETRIES=12
			RETRY_INTERVAL=5
			for i in $(seq 1 $MAX_RETRIES); do
				HTTP_STATUS=$(curl -s -o /dev/null -w "%{http_code}" http://localhost:$SERVER_PORT/ || echo "000")
				if [ "$HTTP_STATUS" -eq 200 ]; then exit 0; fi
				sleep $RETRY_INTERVAL
			done
			exit 1
		'''
	}
}
