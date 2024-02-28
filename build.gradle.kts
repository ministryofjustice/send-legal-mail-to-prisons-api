import org.openapitools.generator.gradle.plugin.tasks.GenerateTask

plugins {
  id("uk.gov.justice.hmpps.gradle-spring-boot") version "5.15.3"
  id("org.unbroken-dome.test-sets") version "4.0.0"
  kotlin("plugin.spring") version "1.9.22"
  kotlin("plugin.jpa") version "1.9.22"
  id("jacoco")
  id("org.openapi.generator") version "6.0.1"
}

jacoco {
  toolVersion = "0.8.11"
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

  implementation("org.springdoc:springdoc-openapi-starter-webmvc-api:2.3.0")
  implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.3.0")
  implementation("org.springdoc:springdoc-openapi-starter-common:2.3.0")

  implementation("io.jsonwebtoken:jjwt:0.12.5")
  implementation("io.github.microutils:kotlin-logging:3.0.5")

  implementation("com.amazonaws:aws-java-sdk-s3:1.12.668")
  implementation("org.apache.commons:commons-csv:1.10.0")

  runtimeOnly("org.flywaydb:flyway-core")
  runtimeOnly("org.postgresql:postgresql:42.7.2")

  testImplementation("org.testcontainers:postgresql:1.19.6")
  testImplementation("it.ozimov:embedded-redis:0.7.3")
  testImplementation("com.github.tomakehurst:wiremock-standalone:3.0.1")
  testImplementation("org.mockito:mockito-inline:5.2.0")
  testImplementation("io.swagger.parser.v3:swagger-parser-v3:2.1.20")
  testImplementation("org.testcontainers:localstack:1.19.6")
  testImplementation("org.awaitility:awaitility-kotlin:4.2.0")
  testImplementation("org.springframework.security:spring-security-test")
  testImplementation("com.microsoft.azure:applicationinsights-web:3.4.19")
  testImplementation("io.opentelemetry:opentelemetry-sdk-testing")
}

// Language versions
kotlin {
  jvmToolchain(21)
}

tasks {
  withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions {
      jvmTarget = "21"
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
