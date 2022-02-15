package uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.barcode

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
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
    @Test
    fun `should accept valid names`() {
      listOf("Andrew Barnes", "Declan o'Docherty", "Declan o`Docherty", "Trent Alexander-Arnold").forEach {
        Assertions.assertThat(validator.validate(aRequest(prisonerName = it)))
          .withFailMessage("Expected no validation errors for name=$it")
          .isEmpty()
      }
    }

    @Test
    fun `should reject invalid characters in name`() {
      listOf("<Andrew Barnes>", "andrew@barnes", "4ndrew Barnes", "Andrew Barne\$").forEach {
        Assertions.assertThat(validator.validate(aRequest(prisonerName = it)))
          .withFailMessage("Expected a validation error for name=$it due to an invalid character")
          .isNotEmpty
      }
    }

    @Test
    fun `should apply name length validation rule`() {
      Assertions.assertThat(validator.validate(aRequest(prisonerName = "abcdefghi abcdefghi abcdefghi abcdefghi abcdefghi abcdefghij")))
        .isEmpty()
      Assertions.assertThat(validator.validate(aRequest(prisonerName = "abcdefghi abcdefghi abcdefghi abcdefghi abcdefghi abcdefghi a"))).isNotEmpty
    }
  }
}
