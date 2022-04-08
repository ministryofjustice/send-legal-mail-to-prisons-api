package uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.onetimecode

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class OneTimeCodeGeneratorTest {

  private val oneTimeCodeGenerator = OneTimeCodeGenerator()

  @Test
  fun `should generate code`() {
    val oneTimeCode = oneTimeCodeGenerator.generateOneTimeCode()

    assertThat(oneTimeCode).matches("^[A-Z]{4}$")
  }
}
