package uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.onetimecode

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.Duration
import java.time.temporal.ChronoUnit

class OneTimeCodeGeneratorTest {

  private val oneTimeCodeConfig = OneTimeCodeConfig(
    codeExpiry = Duration.of(10, ChronoUnit.MINUTES),
    codeLength = 6,
    supportedCharacters = listOf('A', 'B', 'C', 'D', 'E', 'F'),
    maxAttempts = 6,
  )
  private val oneTimeCodeGenerator = OneTimeCodeGenerator(oneTimeCodeConfig)

  @Test
  fun `should generate code`() {
    val oneTimeCode = oneTimeCodeGenerator.generateOneTimeCode()

    assertThat(oneTimeCode).matches("^[A-F]{6}$")
  }
}
