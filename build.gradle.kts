import org.openapitools.generator.gradle.plugin.tasks.GenerateTask

plugins {
  id("uk.gov.justice.hmpps.gradle-spring-boot") version "8.1.0"
  id("org.unbroken-dome.test-sets") version "4.1.0"
  kotlin("plugin.spring") version "2.1.20"
  kotlin("plugin.jpa") version "2.1.20"
  id("jacoco")
  id("org.openapi.generator") version "6.6.0"
}

jacoco {
  toolVersion = "0.8.13"
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

  implementation("org.springframework.boot:spring-boot-starter-webflux")
  implementation("org.springframework.boot:spring-boot-starter-data-jpa")
  implementation("org.springframework.boot:spring-boot-starter-data-redis")
  implementation("org.springframework.boot:spring-boot-starter-oauth2-client")
  implementation("org.springframework.boot:spring-boot-starter-oauth2-resource-server")
  implementation("org.springframework.boot:spring-boot-starter-mail")

  implementation("org.springdoc:springdoc-openapi-starter-webmvc-api:2.8.6")
  implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.8.6")
  implementation("org.springdoc:springdoc-openapi-starter-common:2.8.6")

  implementation("io.jsonwebtoken:jjwt:0.12.6")
  implementation("io.github.microutils:kotlin-logging:3.0.5")

  implementation("software.amazon.awssdk:s3:2.31.32")
  implementation("software.amazon.awssdk:sts:2.31.32")
  implementation("org.apache.commons:commons-csv:1.14.0")
  implementation("uk.gov.service.notify:notifications-java-client:5.2.1-RELEASE")

  runtimeOnly("org.flywaydb:flyway-core")
  runtimeOnly("org.flywaydb:flyway-database-postgresql")
  runtimeOnly("org.postgresql:postgresql:42.7.5")

  testImplementation("org.testcontainers:postgresql:1.21.0")
  testImplementation("com.github.codemonstur:embedded-redis:1.4.3")
  testImplementation("com.github.tomakehurst:wiremock-standalone:3.0.1")
  testImplementation("org.mockito:mockito-inline:5.2.0")
  testImplementation("io.swagger.parser.v3:swagger-parser-v3:2.1.26")
  testImplementation("org.testcontainers:localstack:1.21.0")
  testImplementation("org.awaitility:awaitility-kotlin:4.3.0")
  testImplementation("org.springframework.security:spring-security-test")
  testImplementation("com.microsoft.azure:applicationinsights-web:3.7.2")
  testImplementation("io.opentelemetry:opentelemetry-sdk-testing:1.49.0")
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
