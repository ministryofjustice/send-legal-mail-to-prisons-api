# send-legal-mail-to-prisons-api

## About
A Kotlin application providing APIs to support a UI application for creating and scanning barcodes for legal mail (aka rule39 mail).

### Team
This application is in development by the Farsight Consulting team `Send legal mail to prisons`. They can be contacted on MOJ Slack channel `#prisoner_transactions_team`.

### Health
The application has a health endpoint found at `/health` which indicates if the app is running and is healthy.

### Ping
The application has a ping endpoint found at `/ping` which indicates that the app is responding to requests.

### Build
<em>Requires membership of Github team `farsight-devs`</em>

The application is built on [CircleCI](https://app.circleci.com/pipelines/github/ministryofjustice/send-legal-mail-to-prisons-api).

### Versions
The application version currently running can be found on the `/health` endpoint at node `build.buildNumber`. The format of the version number is `YYY-MM-DD.ccc.gggggg` where `ccc` is the Circle job number and `gggggg` is the git commit reference.

### Rolling back the application

* <em>Requires CLI tools `kubectl` and `helm`</em>
* <em>Requires access to Cloud Platform Kubernetes `live` cluster</em>
* <em>Requires membership of Github team `farsight-devs`</em>

For example in the dev environment:
1. Set the Kube context with command `kubectl config use-context live.cloud-platform.service.justice.gov.uk`
2. Set the Kube namespace with command `kubectl config set-context --current --namespace send-legal-mail-to-prisons-dev`
3. List the charts deployed by helm with command `helm list`
4. List the deployments for this application with command `helm history send-legal-mail-to-prisons-api`
5. Given the application version you wish to rollback to, find the related revision number
6. Rollback to that version with command `helm rollback <revision-number>` replacing `<revision-number>` as appropriate

## Configuring the project

### Ktlint formatting
Ktlint is used to format the source code and a task runs in the Circle build to check the formatting.

You should run the following commands to make sure that the source code is formatted locally before it breaks the Circle build.

#### Apply ktlint formatting rules to Intellij
`./gradlew ktlintApplyToIdea`

Or to apply to all Intellij projects:

`./gradlew ktlintApplyToIdeaGlobally`

#### Run ktlint formatter on git commit
`./gradlew addKtlintFormatGitPreCommitHook`

## Running the app
The easiest way to run the app is to use docker compose to create the service and all dependencies.

`docker-compose pull`

`docker-compose up`

### Running the app for development - Intellij
First start the Postgres container with command:

`docker-compose up send-legal-mail-api-db`

In Intellij find the Run Configuration for Spring Boot called SendLegalMailToPrisonsApi. In the `ActiveProfiles` section enter `dev,stdout`.

Run the configuration and the app should start. Check `http://localhost:8080/health` to check the app is running.

### Running the app for development - Gradle
    First start the Postgres container with command:

`docker-compose up send-legal-mail-api-db`

Then run the following command:

`./gradlew bootRun --args='--spring.profiles.active=dev,stdout'`

## Running the tests

### Postgres for integration tests
The integration tests rely on a Postgres container running on port 5432.

By default if post 5432 is not in use the tests will use Testcontainers to start a Postgres instance.

To speed up the test you can start the Postgres instance with command:

`docker-compose up send-legal-mail-api-db`

This will remove the need to start a new Postgres instance each time you run the tests.

### Intellij
Right click on the `test` source directory and select `Run`.

### Running the tests - Gradle
Run the following command:

`./gradlew test`

Or to run all checks including ktlintCheck run command:

`./gradlew check`

## Dependency checks

### Vulnerable dependencies
To find any dependencies with vulnerabilities run command:

`./gradlew dependencyCheckAnalyze`

### Update dependencies
To update all dependencies to their latest stable versions run command:

`./gradlew useLatestVersions`