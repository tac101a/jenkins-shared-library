def call(String mavenCommand) {
	container('maven') {
		sh "chmod +x mvnw"
		sh "./mvnw --batch-mode ${mavenCommand}"
	}
}
