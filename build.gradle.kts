plugins {
  id("uk.gov.justice.hmpps.gradle-spring-boot") version "3.3.15"
  id("org.unbroken-dome.test-sets") version "4.0.0"
  kotlin("plugin.spring") version "1.5.31"
  kotlin("plugin.jpa") version "1.5.31"
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

  implementation("org.springdoc:springdoc-openapi-ui:1.6.0")
  implementation("org.springdoc:springdoc-openapi-data-rest:1.6.0")
  implementation("org.springdoc:springdoc-openapi-kotlin:1.6.0")
  implementation("org.springdoc:springdoc-openapi-webmvc-core:1.6.0")

  implementation("io.jsonwebtoken:jjwt:0.9.1")
  implementation("io.github.microutils:kotlin-logging:2.1.16")

  runtimeOnly("org.flywaydb:flyway-core")
  runtimeOnly("org.postgresql:postgresql:42.3.1")

  testImplementation("org.testcontainers:postgresql:1.16.2")
  testImplementation("it.ozimov:embedded-redis:0.7.3")
  testImplementation("com.github.tomakehurst:wiremock-standalone:2.27.2")
  testImplementation("org.mockito:mockito-inline:4.1.0")
  testImplementation("io.swagger.parser.v3:swagger-parser-v3:2.0.28")
}

java {
  toolchain.languageVersion.set(JavaLanguageVersion.of(16))
}

tasks {
  withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions {
      jvmTarget = "16"
    }
  }
}
