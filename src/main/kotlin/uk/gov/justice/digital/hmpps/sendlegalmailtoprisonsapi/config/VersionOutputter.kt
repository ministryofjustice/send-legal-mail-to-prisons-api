package uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.config

import mu.KotlinLogging
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.boot.info.BuildProperties
import org.springframework.context.annotation.Configuration
import org.springframework.context.event.EventListener

private val log = KotlinLogging.logger {}

@Configuration
class VersionOutputter(buildProperties: BuildProperties) {
  private val version = buildProperties.version

  @EventListener(ApplicationReadyEvent::class)
  fun logVersionOnStartup() {
    log.info("Version {} started", version)
  }
}
