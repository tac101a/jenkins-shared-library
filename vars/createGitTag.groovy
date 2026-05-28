def call(Map config) {
	def branchName = config.branch
	def buildNum = config.buildNum

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
		withCredentials([usernamePassword(credentialsId: 'gitlab-token-credentials', passwordVariable: 'GIT_PASS', usernameVariable: 'GIT_USER')]) {
			sh '''
				set +x
				git config user.email "jenkins@fpt.com"
				git config user.name "Jenkins CI"
				git tag -a "$TAG_NAME" -m "Auto deploy from Jenkins"
				git push "http://${GIT_USER}:${GIT_PASS}@10.89.25.145/devops-training/2026/anhcnt1/spring-petclinic.git" "$TAG_NAME"
			'''
		}
	}
}
