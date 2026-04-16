def call(Map config) {
	def branchName = config.branch
	def buildNum = config.buildNum
	def nexusUrl = config.nexusUrl
	def credId = config.credId

	withCredentials([usernamePassword(credentialsId: credId, passwordVariable: 'NEXUS_PSW', usernameVariable: 'NEXUS_USR')]) {
		sh """
			SAFE_BRANCH_NAME=\$(echo "${branchName}" | tr '/' '-')
			JAR_FILE=\$(ls target/*.jar | grep -v plain)

			echo "Dang day artifact cua nhanh ${branchName} len Nexus..."

			curl -fSsl -u \${NEXUS_USR}:\${NEXUS_PSW} \\
				 --upload-file \${JAR_FILE} \\
				 ${nexusUrl}/com/fpt/petclinic/petclinic/${buildNum}-\${SAFE_BRANCH_NAME}/petclinic-${buildNum}-\${SAFE_BRANCH_NAME}.jar
		"""
	}
}
