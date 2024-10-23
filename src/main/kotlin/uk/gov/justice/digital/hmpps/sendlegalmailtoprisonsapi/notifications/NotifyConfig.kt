package uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.notifications

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import uk.gov.service.notify.NotificationClient

@Configuration
class NotifyConfig(@Value("\${app.notify.apikey:}") private val apiKey: String) {
  @Bean
  fun notifyClient(): NotificationClient {
    return NotificationClient(apiKey)
  }
}
