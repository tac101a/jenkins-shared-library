def call(String sonarServerName) {
	container('maven') {
		withSonarQubeEnv("${sonarServerName}") {
			sh './mvnw sonar:sonar'
		}
	}
}
