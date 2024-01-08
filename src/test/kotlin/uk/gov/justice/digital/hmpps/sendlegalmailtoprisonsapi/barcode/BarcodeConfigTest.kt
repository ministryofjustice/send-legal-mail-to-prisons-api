package uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.barcode

import jakarta.validation.Validation
import jakarta.validation.Validator
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.Duration

class BarcodeConfigTest {

  private val propertyValidator: Validator = Validation.buildDefaultValidatorFactory().validator

  @Test
  fun `should allow valid config`() {
    val barcodeConfig = BarcodeConfig(Duration.ofDays(28), 10)

    assertThat(propertyValidator.validate(barcodeConfig).size).isEqualTo(0)
  }

  @Test
  fun `should validate the percentage`() {
    val barcodeConfig = BarcodeConfig(Duration.ofDays(28), 110)

    val errors = propertyValidator.validate(barcodeConfig)
    assertThat(errors.size).isEqualTo(1)
    assertThat(errors.firstOrNull()?.message).isEqualTo("must be between 0 and 100")
  }
}
