plugins {
  id("uk.gov.justice.hmpps.gradle-spring-boot") version "4.0.0-beta-4"
  id("org.unbroken-dome.test-sets") version "4.0.0"
  kotlin("plugin.spring") version "1.6.10"
  kotlin("plugin.jpa") version "1.6.10"
  id("jacoco")
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

  implementation("org.springdoc:springdoc-openapi-ui:1.6.1")
  implementation("org.springdoc:springdoc-openapi-data-rest:1.6.1")
  implementation("org.springdoc:springdoc-openapi-kotlin:1.6.1")
  implementation("org.springdoc:springdoc-openapi-webmvc-core:1.6.1")

  implementation("io.jsonwebtoken:jjwt:0.9.1")
  implementation("io.github.microutils:kotlin-logging:2.1.20")

  implementation("com.amazonaws:aws-java-sdk-s3:1.12.129")
  implementation("org.apache.commons:commons-csv:1.9.0")

  runtimeOnly("org.flywaydb:flyway-core")
  runtimeOnly("org.postgresql:postgresql:42.3.1")

  testImplementation("org.testcontainers:postgresql:1.16.2")
  testImplementation("it.ozimov:embedded-redis:0.7.3")
  testImplementation("com.github.tomakehurst:wiremock-standalone:2.27.2")
  testImplementation("org.mockito:mockito-inline:4.1.0")
  testImplementation("io.swagger.parser.v3:swagger-parser-v3:2.0.28")
  testImplementation("org.testcontainers:localstack:1.16.2")
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
    xml.required.set(true)
    html.required.set(true)
  }
}
tasks.named<JacocoReport>("jacocoTestIntegrationReport") {
  reports {
    xml.required.set(true)
    html.required.set(true)
  }
}

tasks.register<JacocoReport>("mergeJacoco") {
  executionData(fileTree(project.buildDir.absolutePath).include("jacoco/*.exec"))
  classDirectories.setFrom(files(project.sourceSets.main.get().output))
  sourceDirectories.setFrom(files(project.sourceSets.main.get().allSource))
  reports {
    xml.required.set(true)
    html.required.set(true)
  }
}
