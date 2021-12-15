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
The easiest (and slowest) way to run the app is to use docker compose to create the service and all dependencies.

`docker-compose pull`

`docker-compose up`

### Running the app for development - Intellij
First start the dependent containers with command:

`docker-compose up --scale send-legal-mail-to-prisons-api=0`

In Intellij find the Run Configuration for Spring Boot called SendLegalMailToPrisonsApi. In the `ActiveProfiles` section enter `dev,stdout`.

Run the configuration and the app should start. Check `http://localhost:8080/health` to check the app is running.

### Running the app for development - Gradle
First start the dependent containers with command:

`docker-compose up --scale send-legal-mail-to-prisons-api=0`

Then run the following command:

`./gradlew bootRun --args='--spring.profiles.active=dev,stdout'`

## Running the tests
Note that there are two test source sets - `test` for unit tests and `testIntegration` for integration tests.

### Dependent containers for integration tests
The integration tests depend on:
* a Postgres container running on port 5432
* a mailcatcher container running on port 1080
* a LocalStack instance emulating S3 on port 4566

By default Testcontainers will notice the dependent containers are not running and start an instance of each.

To speed up the tests you can start the dependent containers with the command:

`docker-compose -f docker-compose-test.yml up`

### Intellij
Right click on the `test` or `testIntegration` source directory and select `Run`.

### Gradle
Run the following command:

`./gradlew test` or `./gradlew testIntegration`

Or to run all checks including ktlintCheck run command:

`./gradlew check testIntegration`

## Dependency checks

### Vulnerable dependencies
To find any dependencies with vulnerabilities run command:

`./gradlew dependencyCheckAnalyze`

### Update dependencies
To update all dependencies to their latest stable versions run command:

`./gradlew useLatestVersions`

## Authorisation via Magic Link (CJSM users)

For the `create barcode` user story we verify users by sending a magic link to their CJSM email account. Once the user clicks the link we issue a JWT giving the user authorisation to use the create barcode function.

### Signing the JWT

In order to sign the JWT generated for Magic Link users there are private/public keys saved in configuration properties `app.jwt.private-key` and `app.jwt.public-key`.

A different public/private keypair is required locally and for each deployment environment.

To create a public/private keypair for an environment:
* Create a new directory to hold the keys, we'll call this `/tmp/keys`, and `cd` into the directory.
* Run command `ssh-keygen -t rsa -m PEM`. When prompted enter filename `rsa-key` and leave the passphrase empty.
* Run command `ls` - you should see files `rsa-key` and `rsa-key.pub`
* To generate the public key run command `ssh-keygen -m PKCS8 -e` and when prompted enter the key `rsa-key`. This will produce a public key and print it out to screen. Copy the contents into new file `rsa-key.x509.public`.
* To generate the private key run command `openssl pkcs8 -topk8 -inform pem -in rsa-key -outform pem -nocrypt -out rsa-key.pkcs8.private`
* To convert the public key into a string we can use in a Kubernetes secret run command `cat rsa-key.x509.public | tr -d '\n' | sed -e 's/-----BEGIN PUBLIC KEY-----//g' | sed -e 's/-----END PUBLIC KEY-----//g' | base64`. (On some systems you may need to add an extra `| tr -d '\n'` to remove new lines).
* To convert the private key into a string we can use in a Kubernetes secret run command `cat rsa-key.pkcs8.private | tr -d '\n' | sed -e 's/-----BEGIN PRIVATE KEY-----//g' | sed -e 's/-----END PRIVATE KEY-----//g' | base64`. (On some systems you may need to add an extra `| tr -d '\n'` to remove new lines).
* We now need to save the keys into Kubernetes secrets for the environment. A guide for creating secrets can be found on Cloud Platforms documentation [here](https://user-guide.cloud-platform.service.justice.gov.uk/documentation/deploying-an-app/add-secrets-to-deployment.html#adding-a-secret-to-an-application)
* The public key should be saved in Kubernetes secret `send-legal-mail-to-prisons-api` with key `JWT_PUBLIC_KEY`
* The private key should be saved in Kubernetes secret `send-legal-mail-to-prisons-api` with key `JWT_PRIVATE_KEY`
