package uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.magiclink

import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.config.EmailTooLong
import uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.config.ValidationException

class MagicLinkRequestValidatorTest {

  private val magicLinkRequestValidator = MagicLinkRequestValidator()
  private val emailOfMaxLength = """${"a".repeat(32)}.${"b".repeat(31)}@${"c".repeat(180)}.cjsm.net"""

  @Test
  fun `email of maximum length should not throw validation exception`() {
    assertDoesNotThrow {
      magicLinkRequestValidator.validate(MagicLinkRequest(emailOfMaxLength))
    }
  }

  @Test
  fun `email greater than maximum length should throw validation exception`() {
    assertThatThrownBy {
      magicLinkRequestValidator.validate(MagicLinkRequest("a$emailOfMaxLength"))
    }.isInstanceOf(ValidationException::class.java)
      .extracting("errorCode").isEqualTo(EmailTooLong)
  }

  @Test
  fun `email with apostrophe should not throw exception`() {
    assertDoesNotThrow {
      magicLinkRequestValidator.validate(MagicLinkRequest("""anemailw'ithapostrophes@something.cjsm.net"""))
    }
  }
}
