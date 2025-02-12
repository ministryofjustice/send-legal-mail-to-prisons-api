package uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.config

import mu.KotlinLogging
import org.springframework.context.ApplicationListener
import org.springframework.context.event.ContextRefreshedEvent
import org.springframework.stereotype.Component

private val log = KotlinLogging.logger {}

@Component
class LogMemoryConfigurationAtStartupEvent : ApplicationListener<ContextRefreshedEvent> {

  override fun onApplicationEvent(event: ContextRefreshedEvent) {
    with(Runtime.getRuntime()) {
      log.info { "heapSize: ${totalMemory().renderAsMb()}, heapMaxSize: ${maxMemory().renderAsMb()}, heapFreeSize: ${freeMemory().renderAsMb()}" }
    }
  }

  private fun Long.renderAsMb(): String = "${this / 1024 / 1024}MB"
}
