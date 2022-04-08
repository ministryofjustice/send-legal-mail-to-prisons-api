package uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.onetimecode

import org.junit.jupiter.api.Test
import org.mockito.kotlin.given
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

class OneTimeCodeServiceTest {

  private val oneTimeCodeGenerator = mock<OneTimeCodeGenerator>()
  private val oneTimeCodeRepository = mock<OneTimeCodeRepository>()
  private val oneTimeCodeEmailSender = mock<OneTimeCodeEmailSender>()
  private val oneTimeCodeService = OneTimeCodeService(oneTimeCodeGenerator, oneTimeCodeRepository, oneTimeCodeEmailSender)

  @Test
  fun `should create and send one time code`() {
    val email = "someone@somewhere.cjsm.net"
    val sessionId = "12345678"
    val code = "ABCD"

    given { oneTimeCodeGenerator.generateOneTimeCode() }.willReturn(code)

    oneTimeCodeService.createAndSendOneTimeCode(email, sessionId)

    verify(oneTimeCodeRepository).save(OneTimeCode(sessionId, code, email))
    verify(oneTimeCodeEmailSender).send(email, code)
  }
}
