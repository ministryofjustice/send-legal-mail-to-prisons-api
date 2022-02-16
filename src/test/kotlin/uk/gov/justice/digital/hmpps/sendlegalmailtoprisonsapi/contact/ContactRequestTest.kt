package uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.contact

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import java.time.LocalDate
import javax.validation.Validation

class ContactRequestTest {

  private val validator = Validation.buildDefaultValidatorFactory().validator

  private fun aRequest(
    prisonerName: String = "Andrew Barnes",
    prisonId: String = "BXI",
    dob: LocalDate? = null,
    prisonNumber: String = "A1234BC"
  ) = ContactRequest(prisonerName = prisonerName, prisonId = prisonId, dob = dob, prisonNumber = prisonNumber)

  @Nested
  inner class Name {
    @ParameterizedTest
    @ValueSource(strings = ["Andrew Barnes", "Declan o'Docherty", "Declan o`Docherty", "Trent Alexander-Arnold"])
    fun `should accept valid names`(name: String) {
      assertThat(validator.validate(aRequest(prisonerName = name))).isEmpty()
    }

    @ParameterizedTest
    @ValueSource(strings = ["<Andrew Barnes>", "andrew@barnes", "4ndrew Barnes", "Andrew Barne\$"])
    fun `should reject invalid characters in name`(name: String) {
      assertThat(validator.validate(aRequest(prisonerName = name))).isNotEmpty
    }

    @Test
    fun `should apply name length validation rule`() {
      assertThat(validator.validate(aRequest(prisonerName = "abcdefghi abcdefghi abcdefghi abcdefghi abcdefghi abcdefghij"))).isEmpty()
      assertThat(validator.validate(aRequest(prisonerName = "abcdefghi abcdefghi abcdefghi abcdefghi abcdefghi abcdefghi a"))).isNotEmpty
    }
  }
}
