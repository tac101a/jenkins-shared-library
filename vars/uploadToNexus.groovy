def call(Map config) {
	def branchName = config.branch
	def buildNum = config.buildNum
	def nexusUrl = config.nexusUrl
	def credId = config.credId

	withCredentials([usernamePassword(credentialsId: credId, passwordVariable: 'NEXUS_PSW', usernameVariable: 'NEXUS_USR')]) {
		withEnv(["APP_BRANCH=${branchName}", "BUILD_NUM=${buildNum}", "NEXUS_URL=${nexusUrl}"]) {
			sh '''
				SAFE_BRANCH_NAME=$(echo "$APP_BRANCH" | tr '/' '-')
				JAR_FILE=$(ls target/*.jar | grep -v plain)

				echo "Dang day artifact cua nhanh $APP_BRANCH len Nexus..."

				curl -fSsl -u "$NEXUS_USR:$NEXUS_PSW" \
					 --upload-file "$JAR_FILE" \
					 "$NEXUS_URL/com/fpt/petclinic/petclinic/$BUILD_NUM-$SAFE_BRANCH_NAME/petclinic-$BUILD_NUM-$SAFE_BRANCH_NAME.jar"
			'''
		}
	}
}
