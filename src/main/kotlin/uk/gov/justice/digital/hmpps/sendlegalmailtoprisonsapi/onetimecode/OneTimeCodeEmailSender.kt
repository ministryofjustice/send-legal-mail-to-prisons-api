package uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.onetimecode

import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.notifications.NotificationService

@Service
class OneTimeCodeEmailSender(
  private val notificationService: NotificationService,
  @Value("\${app.notify.template-id.one-time-code-email:}") private val oneTimeCodeEmailTemplateId: String,
) {
  private val log = KotlinLogging.logger {}

  fun send(email: String, oneTmeCode: String) {
    log.info("sending one time code to email address - {}", email)
    notificationService.sendEmail(oneTimeCodeEmailTemplateId, email, getOneTimeCodeValueMap(oneTmeCode))
    log.info("sent one time code to email address - {}", email)
  }

  private fun getOneTimeCodeValueMap(oneTmeCode: String): HashMap<String, String> {
    return hashMapOf(Pair("code", oneTmeCode))
  }
}
