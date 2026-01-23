import org.openapitools.generator.gradle.plugin.tasks.GenerateTask

plugins {
  id("uk.gov.justice.hmpps.gradle-spring-boot") version "10.0.0"
  id("org.unbroken-dome.test-sets") version "4.1.0"
  kotlin("plugin.spring") version "2.3.0"
  kotlin("plugin.jpa") version "2.3.0"
  id("jacoco")
  id("org.openapi.generator") version "6.6.0"
}

jacoco {
  toolVersion = "0.8.14"
}

testSets {
  "testIntegration"()
}

configurations {
  testImplementation { exclude(group = "org.junit.vintage") }

  testImplementation {
    exclude(module = "slf4j-simple")
  }
}

dependencies {
  annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")
  implementation("org.springframework.boot:spring-boot-starter-webclient")
  implementation("org.springframework.boot:spring-boot-starter-cache")
  implementation("org.springframework.boot:spring-boot-starter-webflux")
  implementation("org.springframework.boot:spring-boot-starter-security")
  implementation("org.springframework.boot:spring-boot-starter-oauth2-resource-server")
  implementation("org.springframework.boot:spring-boot-starter-oauth2-client")
  implementation("uk.gov.justice.service.hmpps:hmpps-sqs-spring-boot-starter:6.0.0")
  implementation("org.springframework.boot:spring-boot-starter-flyway")
  implementation("org.springframework.boot:spring-boot-starter-data-redis")
  implementation("org.springframework.boot:spring-boot-starter-mail")
  implementation("org.springframework.boot:spring-boot-starter-data-jpa")

  implementation("org.springframework.data:spring-data-commons:4.0.1")
  implementation("org.springdoc:springdoc-openapi-starter-webmvc-api:3.0.1")
  implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:3.0.1")
  implementation("org.springdoc:springdoc-openapi-starter-common:3.0.1")

  implementation("io.jsonwebtoken:jjwt:0.13.0")
  implementation("io.github.microutils:kotlin-logging:3.0.5")

  implementation("software.amazon.awssdk:s3:2.41.11")
  implementation("software.amazon.awssdk:sts:2.41.11")
  implementation("org.apache.commons:commons-csv:1.14.1")
  implementation("uk.gov.service.notify:notifications-java-client:5.2.1-RELEASE")

  runtimeOnly("org.flywaydb:flyway-core")
  runtimeOnly("org.flywaydb:flyway-database-postgresql")
  runtimeOnly("org.postgresql:postgresql:42.7.9")

  testImplementation("uk.gov.justice.service.hmpps:hmpps-kotlin-spring-boot-starter-test:1.8.2")
  testImplementation("org.springframework.boot:spring-boot-starter-webclient-test")
  testImplementation("org.springframework.boot:spring-boot-starter-webflux-test")
  testImplementation("org.wiremock:wiremock-standalone:3.13.2")
  testImplementation("io.swagger.parser.v3:swagger-parser:2.1.37") {
    exclude(group = "io.swagger.core.v3")
  }
  testImplementation("org.testcontainers:localstack:1.21.4")
  testImplementation("org.awaitility:awaitility-kotlin:4.3.0")
  testImplementation("com.github.codemonstur:embedded-redis:1.4.3")
  testImplementation("com.github.tomakehurst:wiremock-standalone:3.0.1")
  testImplementation("com.microsoft.azure:applicationinsights-web:3.7.6")
  testImplementation("io.opentelemetry:opentelemetry-sdk-testing:1.58.0")
  testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test:4.0.1")
  testImplementation("org.springframework.boot:spring-boot-starter-test")
  testImplementation("org.springframework.security:spring-security-test")

  testImplementation("org.mockito:mockito-inline:5.2.0")
  testImplementation("org.junit.jupiter:junit-jupiter:6.0.2")
  testImplementation("org.testcontainers:testcontainers:2.0.3")
  testImplementation("org.testcontainers:localstack:1.21.4")
  testImplementation("org.testcontainers:postgresql:1.21.4")
  testImplementation("org.testcontainers:junit-jupiter:1.21.4")
  testImplementation("io.github.hakky54:logcaptor:2.12.2")
  testImplementation("org.mockito.kotlin:mockito-kotlin")
  testImplementation("org.springframework.boot:spring-boot-webclient-test")
  testImplementation("org.springframework.boot:spring-boot-webtestclient")

  /*
    //testImplementation("org.springframework.boot:spring-boot-starter-webclient-test")
   // testImplementation("org.springframework.boot:spring-boot-starter-webflux-test")
    testImplementation("com.github.codemonstur:embedded-redis:1.4.3")
    testImplementation("com.github.tomakehurst:wiremock-standalone:3.0.1")
    testImplementation("io.swagger.parser.v3:swagger-parser-v3:2.1.37")
    testImplementation("com.microsoft.azure:applicationinsights-web:3.7.6")
    testImplementation("io.opentelemetry:opentelemetry-sdk-testing:1.58.0")

    // Test dependencies
    testImplementation("uk.gov.justice.service.hmpps:hmpps-kotlin-spring-boot-starter-test:1.8.2")
    testImplementation("org.wiremock:wiremock-standalone:3.13.2")
    testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test:4.0.1")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.security:spring-security-test")

    testImplementation("org.springframework.boot:spring-boot-starter-security-test")
    testImplementation("org.springframework.boot:spring-boot-starter-webclient-test")
    testImplementation("org.springframework.boot:spring-boot-webtestclient")
    implementation("org.springframework.boot:spring-boot-webtestclient")
    testImplementation("org.awaitility:awaitility-kotlin:4.3.0")
    testImplementation("net.javacrumbs.json-unit:json-unit-assertj:5.1.0")
    testImplementation("io.swagger.parser.v3:swagger-parser-v2-converter:2.1.37")
    testImplementation("org.mockito:mockito-inline:5.2.0")
    testImplementation("org.junit.jupiter:junit-jupiter:6.0.2")
    testImplementation("org.testcontainers:testcontainers:2.0.3")
    testImplementation("org.testcontainers:localstack:1.21.4")
    testImplementation("org.testcontainers:postgresql:1.21.4")
    testImplementation("org.testcontainers:junit-jupiter:1.21.4")
    testImplementation("io.github.hakky54:logcaptor:2.12.2")
    testImplementation("org.mockito.kotlin:mockito-kotlin")
   */
  testImplementation(kotlin("test"))
}

// Language versions
kotlin {
  jvmToolchain(21)
}

tasks {
  withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    compilerOptions.jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21
  }
}

// Jacoco code coverage
tasks.named("test") {
  finalizedBy("jacocoTestReport")
}
tasks.named("testIntegration") {
  finalizedBy("jacocoTestIntegrationReport")
}

tasks.named<JacocoReport>("jacocoTestReport") {
  reports {
    html.required.set(true)
  }
}
tasks.named<JacocoReport>("jacocoTestIntegrationReport") {
  reports {
    html.required.set(true)
  }
}

tasks.register<JacocoReport>("combineJacocoReports") {
  executionData(fileTree(project.buildDir.absolutePath).include("jacoco/*.exec"))
  classDirectories.setFrom(files(project.sourceSets.main.get().output))
  sourceDirectories.setFrom(files(project.sourceSets.main.get().allSource))
  reports {
    html.required.set(true)
  }
}

tasks.register<GenerateTask>("buildPrisonerSearchModel") {
  generatorName.set("kotlin")
  inputSpec.set("$projectDir/src/main/resources/prisoner-offender-search-open-api.yml")
  outputDir.set("$buildDir/generated")
  modelPackage.set("uk.gov.justice.digital.hmpps.prisonersearch.model")
  configOptions.set(
    mapOf(
      "dateLibrary" to "java8",
      "serializationLibrary" to "jackson",
    ),
  )
  globalProperties.set(
    mapOf(
      "models" to "",
    ),
  )
}

tasks.register<GenerateTask>("buildPrisonRegisterModel") {
  generatorName.set("kotlin")
  inputSpec.set("$projectDir/src/main/resources/prison-register-open-api.yml")
  outputDir.set("$buildDir/generated")
  modelPackage.set("uk.gov.justice.digital.hmpps.prisonregister.model")
  configOptions.set(
    mapOf(
      "dateLibrary" to "java8",
      "serializationLibrary" to "jackson",
    ),
  )
  globalProperties.set(
    mapOf(
      "models" to "",
    ),
  )
}

tasks.named("compileKotlin") {
  dependsOn("buildPrisonerSearchModel")
  dependsOn("buildPrisonRegisterModel")
}

tasks.named("runKtlintCheckOverMainSourceSet") {
  dependsOn("buildPrisonerSearchModel")
  dependsOn("buildPrisonRegisterModel")
}

repositories {
  mavenCentral()
}

kotlin {
  sourceSets["main"].apply {
    kotlin.srcDir("$buildDir/generated/src/main/kotlin")
  }
}

configure<org.jlleitschuh.gradle.ktlint.KtlintExtension> {
  filter {
    exclude {
      it.file.path.contains("build/generated/src/main/")
    }
  }
}
