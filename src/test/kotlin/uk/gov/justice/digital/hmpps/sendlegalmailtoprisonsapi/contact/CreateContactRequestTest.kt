package uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.contact

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDate
import javax.validation.Validation

class CreateContactRequestTest {

  private val validator = Validation.buildDefaultValidatorFactory().validator

  private fun aRequest(
    prisonerName: String = "Andrew Barnes",
    prisonId: String = "BXI",
    dob: LocalDate? = null,
    prisonNumber: String = "A1234BC"
  ) = CreateContactRequest(prisonerName = prisonerName, prisonId = prisonId, dob = dob, prisonNumber = prisonNumber)

  @Nested
  inner class Name {
    @Test
    fun `should accept valid names`() {
      listOf("Andrew Barnes", "Declan o'Docherty", "Declan o`Docherty", "Trent Alexander-Arnold").forEach {
        assertThat(validator.validate(aRequest(prisonerName = it)))
          .withFailMessage("Expected no validation errors for name=$it")
          .isEmpty()
      }
    }

    @Test
    fun `should reject invalid characters in name`() {
      listOf("<Andrew Barnes>", "andrew@barnes", "4ndrew Barnes", "Andrew Barne\$").forEach {
        assertThat(validator.validate(aRequest(prisonerName = it)))
          .withFailMessage("Expected a validation error for name=$it due to an invalid character")
          .isNotEmpty
      }
    }

    @Test
    fun `should apply name length validation rule`() {
      assertThat(validator.validate(aRequest(prisonerName = "abcdefghi abcdefghi abcdefghi abcdefghi abcdefghi abcdefghij"))).isEmpty()
      assertThat(validator.validate(aRequest(prisonerName = "abcdefghi abcdefghi abcdefghi abcdefghi abcdefghi abcdefghi a"))).isNotEmpty
    }
  }
}
