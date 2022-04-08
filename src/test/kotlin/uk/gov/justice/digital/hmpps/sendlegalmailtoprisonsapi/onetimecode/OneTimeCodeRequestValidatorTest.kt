package uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.onetimecode

import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.config.EmailTooLong
import uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.config.SessionIdMandatory
import uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.config.ValidationException

class OneTimeCodeRequestValidatorTest {
  private val oneTimeCodeRequestValidator = OneTimeCodeRequestValidator()
  private val emailOfMaxLength = """${"a".repeat(32)}.${"b".repeat(31)}@${"c".repeat(180)}.cjsm.net"""
  private val sessionId = "12345678"

  @Test
  fun `request with session id and email of maximum length should not throw validation exception`() {
    oneTimeCodeRequestValidator.validate(OneTimeCodeRequest(emailOfMaxLength, sessionId))
  }

  @Test
  fun `request with session id and email greater than maximum length should throw validation exception`() {
    assertThatThrownBy {
      oneTimeCodeRequestValidator.validate(OneTimeCodeRequest("a$emailOfMaxLength", sessionId))
    }.isInstanceOf(ValidationException::class.java)
      .extracting("errorCode").isEqualTo(EmailTooLong)
  }

  @Test
  fun `request with empty session id and email of maximum length should throw validation exception`() {
    assertThatThrownBy {
      oneTimeCodeRequestValidator.validate(OneTimeCodeRequest(emailOfMaxLength, ""))
    }.isInstanceOf(ValidationException::class.java)
      .extracting("errorCode").isEqualTo(SessionIdMandatory)
  }
}
