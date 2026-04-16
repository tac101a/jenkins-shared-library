def call(Map config) {
	def status = config.status
	def appName = config.appName
	def branchName = config.branch.replaceAll("/", "-")
	def buildNum = config.buildNum
	def buildUrl = config.buildUrl

	def colorCode = (status == 'SUCCESS') ? '#36a64f' : '#eb4034'
	def msgPrefix = (status == 'SUCCESS') ? '✅ *BUILD SUCCESS*' : '❌ *BUILD FAILED*'

	slackSend(
		teamDomain: 'anhcnt-devops-lab',
		channel: '#jenkins-alerts',
		tokenCredentialId: 'slack-token',
		color: colorCode,
		message: "${msgPrefix}\n*Project:* ${appName}\n*Branch:* ${branchName}\n*Build:* #${buildNum}\n*URL:* ${buildUrl}"
	)
}
