package uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.magiclink

import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.config.ErrorCode
import uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.config.ValidationException

class MagicLinkRequestValidatorTest {

  private val magicLinkRequestValidator = MagicLinkRequestValidator()
  private val emailOfMaxLength = """${"a".repeat(32)}.${"b".repeat(31)}@${"c".repeat(180)}.cjsm.net"""

  @Test
  fun `email of maximum length should not throw validation exception`() {
    magicLinkRequestValidator.validate(MagicLinkRequest(emailOfMaxLength))
  }

  @Test
  fun `email greater than maximum length should throw validation exception`() {
    assertThatThrownBy {
      magicLinkRequestValidator.validate(MagicLinkRequest("a$emailOfMaxLength"))
    }.isInstanceOf(ValidationException::class.java)
      .extracting("errorCode").isEqualTo(ErrorCode.EMAIL_TOO_LONG)
  }
}
