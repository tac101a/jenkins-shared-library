def call(String sonarServerName) {
	container('maven') {
		withSonarQubeEnv("${sonarServerName}") {
			sh './mvnw --batch-mode sonar:sonar'
		}
	}
}
