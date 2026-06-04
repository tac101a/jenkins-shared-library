# Jenkins Shared Library - Spring Petclinic

A centralized, declarative Jenkins Shared Library designed for Enterprise CI/CD automation. This library encapsulates complex pipeline logic into reusable, modular functions.

## 🚀 Usage

To import this library into your `Jenkinsfile`, use the following annotation at the top of the file:

```groovy
@Library('anhcnt1-shared-library') _
```

## Available Custom Steps (`vars`)

This library provides the following standardized pipeline steps:

- `mavenExecute(String command)`: Executes Maven commands in batch mode within an isolated Maven container.
- `sonarScan(String sonarServerName)`: Triggers static code analysis using SonarQube Scanner.
- `uploadToNexus(Map config)`: Packages and uploads the compiled `.jar` artifact to the Nexus Repository Manager.
- `buildAndPushDocker(Map config)`: Builds a Docker image using Buildah and pushes it to the target Container Registry.
- `deployOpenshift(Map config)`: Declaratively deploys the application to a Red Hat OpenShift cluster.
- `deployApp(Map config)`: Performs a graceful shutdown and deploys the `.jar` artifact on a traditional Linux VM.
- `createGitTag(Map config)`: Automatically generates and pushes Git tags for `uat` and `main` release pipelines.
- `notifySlack(Map config)`: Sends formatted build status alerts (Success/Failure) to the team's Slack channel.
