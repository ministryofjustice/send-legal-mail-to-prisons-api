package uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.notifications

import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import uk.gov.service.notify.NotificationClient

@Service
class NotificationService(
  private val notifyClient: NotificationClient,
  @Value("\${app.notify.enabled:}")
  private val enabled: Boolean,
) {
  private val log = KotlinLogging.logger {}

  fun sendEmail(templateId: String, emailAddress: String, values: HashMap<String, String>) {
    if (enabled) {
      val reference = generateReference()
      log.trace("Sending email to {} with reference - {}", emailAddress, reference)
      notifyClient.sendEmail(templateId, emailAddress, values, reference)
      log.trace("Sent email to {} with reference - {}", emailAddress, reference)
    } else {
      log.info("Notify email not enabled")
    }
  }

  private fun generateReference(): String {
    return ""
  }
}
