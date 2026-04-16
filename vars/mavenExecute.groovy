def call(String mavenCommand) {
	sh "./mvnw ${mavenCommand}"
}
