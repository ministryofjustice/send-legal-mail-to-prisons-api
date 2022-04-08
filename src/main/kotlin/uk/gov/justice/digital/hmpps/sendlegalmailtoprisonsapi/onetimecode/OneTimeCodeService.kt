package uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.onetimecode

import org.springframework.stereotype.Service

@Service
class OneTimeCodeService(
  private val oneTimeCodeGenerator: OneTimeCodeGenerator,
  private val oneTimeCodeRepository: OneTimeCodeRepository,
  private val oneTimeCodeEmailSender: OneTimeCodeEmailSender,
) {

  fun createAndSendOneTimeCode(email: String, sessionId: String) {
    OneTimeCode(sessionId, oneTimeCodeGenerator.generateOneTimeCode(), email)
      .also { oneTimeCode -> oneTimeCodeRepository.save(oneTimeCode) }
      .also { oneTimeCode -> oneTimeCodeEmailSender.send(email, oneTimeCode.code) }
  }
}
