package uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.barcode

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import java.time.LocalDate
import javax.validation.Validation

class CreateBarcodeRequestTest {

  private val validator = Validation.buildDefaultValidatorFactory().validator

  private fun aRequest(
    prisonerName: String = "Andrew Barnes",
    prisonId: String = "BXI",
    dob: LocalDate? = null,
    prisonNumber: String = "A1234BC",
    contactId: Long? = 1L,
  ) = CreateBarcodeRequest(
    prisonerName = prisonerName,
    prisonId = prisonId,
    dob = dob,
    prisonNumber = prisonNumber,
    contactId = contactId
  )

  @Nested
  inner class Name {
    @ParameterizedTest
    @ValueSource(strings = ["Andrew Barnes", "Declan o'Docherty", "Declan o`Docherty", "Trent Alexander-Arnold"])
    fun `should accept valid names`(name: String) {
      Assertions.assertThat(validator.validate(aRequest(prisonerName = name))).isEmpty()
    }

    @ParameterizedTest
    @ValueSource(strings = ["<Andrew Barnes>", "andrew@barnes", "4ndrew Barnes", "Andrew Barne\$"])
    fun `should reject invalid characters in name`(name: String) {
      Assertions.assertThat(validator.validate(aRequest(prisonerName = name))).isNotEmpty
    }

    @Test
    fun `should apply name length validation rule`() {
      Assertions.assertThat(validator.validate(aRequest(prisonerName = "abcdefghi abcdefghi abcdefghi abcdefghi abcdefghi abcdefghij"))).isEmpty()
      Assertions.assertThat(validator.validate(aRequest(prisonerName = "abcdefghi abcdefghi abcdefghi abcdefghi abcdefghi abcdefghi a"))).isNotEmpty
    }
  }
}
