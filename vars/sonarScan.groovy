def call(String sonarServerName) {
	withSonarQubeEnv("${sonarServerName}") {
		sh './mvnw sonar:sonar'
	}
}
