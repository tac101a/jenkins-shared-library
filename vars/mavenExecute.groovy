def call(String mavenCommand) {
	sh "chmod +x mvnw"
	sh "./mvnw ${mavenCommand}"
}
