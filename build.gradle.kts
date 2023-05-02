import org.openapitools.generator.gradle.plugin.tasks.GenerateTask

plugins {
  id("uk.gov.justice.hmpps.gradle-spring-boot") version "4.8.6"
  id("org.unbroken-dome.test-sets") version "4.0.0"
  kotlin("plugin.spring") version "1.8.10"
  kotlin("plugin.jpa") version "1.8.10"
  id("jacoco")
  id("org.openapi.generator") version "6.0.1"
}

testSets {
  "testIntegration"()
}

configurations {
  testImplementation { exclude(group = "org.junit.vintage") }
}

dependencies {
  annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")

  implementation("org.springframework.boot:spring-boot-starter-webflux")
  implementation("org.springframework.boot:spring-boot-starter-data-jpa")
  implementation("org.springframework.boot:spring-boot-starter-data-redis")
  implementation("org.springframework.boot:spring-boot-starter-oauth2-client")
  implementation("org.springframework.boot:spring-boot-starter-oauth2-resource-server")
  implementation("org.springframework.boot:spring-boot-starter-mail")

  implementation("org.springdoc:springdoc-openapi-ui:1.6.15")
  implementation("org.springdoc:springdoc-openapi-data-rest:1.6.15")
  implementation("org.springdoc:springdoc-openapi-kotlin:1.6.15")
  implementation("org.springdoc:springdoc-openapi-webmvc-core:1.6.15")

  implementation("io.jsonwebtoken:jjwt:0.9.1")
  implementation("io.github.microutils:kotlin-logging:3.0.5")

  implementation("com.amazonaws:aws-java-sdk-s3:1.12.439")
  implementation("org.apache.commons:commons-csv:1.10.0")

  runtimeOnly("org.flywaydb:flyway-core")
  runtimeOnly("org.postgresql:postgresql:42.6.0")

  testImplementation("org.testcontainers:postgresql:1.17.6")
  testImplementation("it.ozimov:embedded-redis:0.7.3")
  testImplementation("com.github.tomakehurst:wiremock-standalone:2.27.2")
  testImplementation("org.mockito:mockito-inline:5.2.0")
  testImplementation("io.swagger.parser.v3:swagger-parser-v3:2.1.13")
  testImplementation("org.testcontainers:localstack:1.17.6")
  testImplementation("org.awaitility:awaitility-kotlin:4.2.0")
  testImplementation("org.springframework.security:spring-security-test")
}

// Language versions
java {
  toolchain.languageVersion.set(JavaLanguageVersion.of(17))
}

tasks {
  withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions {
      jvmTarget = "17"
    }
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
      "serializationLibrary" to "jackson"
    )
  )
  globalProperties.set(
    mapOf(
      "models" to ""
    )
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
      "serializationLibrary" to "jackson"
    )
  )
  globalProperties.set(
    mapOf(
      "models" to ""
    )
  )
}

tasks.named("compileKotlin") {
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
