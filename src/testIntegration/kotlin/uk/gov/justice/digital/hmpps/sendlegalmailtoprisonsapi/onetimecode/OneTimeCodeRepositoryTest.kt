package uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.onetimecode

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.IntegrationTest

class OneTimeCodeRepositoryTest : IntegrationTest() {

  @Test
  fun `can create and retrieve a one time code`() {
    val sessionId = "12345678"

    val oneTimeCode = oneTimeCodeRepository.save(
      OneTimeCode(sessionId, code = "ABCD", email = "someone@somewhere.com")
    )

    val savedSecret = oneTimeCodeRepository.findById(sessionId).orElseThrow()

    assertThat(savedSecret).isEqualTo(oneTimeCode)
  }
}
