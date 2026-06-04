# Jenkins Shared Library - Spring Petclinic

A centralized, declarative Jenkins Shared Library designed for Enterprise CI/CD automation. This library encapsulates complex pipeline logic into reusable, modular functions.

## Usage

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

## Architectural Decision: Declarative vs. Scripted Pipeline

In this library and the accompanying `Jenkinsfile`, we strictly enforce the **Declarative Pipeline** architecture over the traditional Scripted `podTemplate()`.

1. **Native Kubernetes Integration:** The declarative `agent { kubernetes { yaml '''...''' } }` block is automatically translated into a Kubernetes Pod Template under the hood. It provides a cleaner, highly structured, and universally readable syntax.
2. **Separation of Concerns & Security:** Scripted pipelines often encourage wrapping the entire CI/CD flow inside a single `podTemplate` with a highly privileged `ServiceAccount`. Our declarative approach enforces the **Principle of Least Privilege**.
   - **Stages 1 to 4.5 (Build/CI):** Run inside an isolated, ephemeral Kubernetes Pod using the default unprivileged account.
   - **Stage 5 (Deploy/CD):** Uses `agent any` to step out of the build Pod and securely inject the OpenShift API token (`oc login`) only when necessary. This ensures that the build environment never inadvertently exposes cluster admin privileges.
