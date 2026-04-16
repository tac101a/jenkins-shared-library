def call(Map config) {
	def branchName = config.branch
	def buildNum = config.buildNum
	def gitRepoDomain = config.gitRepoDomain
	def credId = config.credId

	def date = sh(script: "date +'%y%m%d'", returnStdout: true).trim()
	def gitHash = sh(script: 'git rev-parse --short HEAD', returnStdout: true).trim()
	def generatedTagName = ''

	if (branchName ==~ /uat\/.*/) {
		generatedTagName = "${date}-uat-${gitHash}"
	} else if (branchName == 'main') {
		generatedTagName = "${date}-b${buildNum}-release"
	} else {
		echo "Skip Auto Tagging for branch: ${branchName}"
		return
	}

	echo "Kich hoat Auto Tagging: ${generatedTagName}"

	withEnv(["TAG_NAME=${generatedTagName}"]) {
		withCredentials([usernamePassword(credentialsId: credId, passwordVariable: 'GIT_PASS', usernameVariable: 'GIT_USER')]) {
			sh """
				git config user.email "jenkins@fpt.com"
				git config user.name "Jenkins CI"
				git tag -a \${TAG_NAME} -m "Auto deploy from Jenkins"
				git push https://\${GIT_USER}:\${GIT_PASS}@${gitRepoDomain} \${TAG_NAME}
			"""
		}
	}
}
