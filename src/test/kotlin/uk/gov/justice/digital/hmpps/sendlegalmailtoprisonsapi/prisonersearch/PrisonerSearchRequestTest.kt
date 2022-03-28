package uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.prisonersearch

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate

class PrisonerSearchRequestTest {

  @Test
  fun `should convert to request body given request with all fields`() {
    val prisonerSearchRequest = PrisonerSearchRequest(prisonNumber = "A1234BC", firstName = "John", lastName = "Smith", dob = LocalDate.of(1965, 2, 16))
    val expectedRequestBody = mapOf(
      "nomsNumber" to "A1234BC",
      "firstName" to "John",
      "lastName" to "Smith",
      "dateOfBirth" to "1965-02-16"
    )

    assertThat(prisonerSearchRequest.toRequestBody()).isEqualTo(expectedRequestBody)
  }

  @Test
  fun `should convert to request body given request with date of birth`() {
    val prisonerSearchRequest = PrisonerSearchRequest(prisonNumber = null, firstName = "John", lastName = "Smith", dob = LocalDate.of(1965, 2, 16))
    val expectedRequestBody = mapOf(
      "nomsNumber" to null,
      "firstName" to "John",
      "lastName" to "Smith",
      "dateOfBirth" to "1965-02-16"
    )

    assertThat(prisonerSearchRequest.toRequestBody()).isEqualTo(expectedRequestBody)
  }

  @Test
  fun `should convert to request body given request with prison number`() {
    val prisonerSearchRequest = PrisonerSearchRequest(prisonNumber = "A1234BC", firstName = "John", lastName = "Smith", dob = null)
    val expectedRequestBody = mapOf(
      "nomsNumber" to "A1234BC",
      "firstName" to "John",
      "lastName" to "Smith",
      "dateOfBirth" to null
    )

    assertThat(prisonerSearchRequest.toRequestBody()).isEqualTo(expectedRequestBody)
  }

  @Test
  fun `should convert to request body given request without firstname`() {
    val prisonerSearchRequest = PrisonerSearchRequest(prisonNumber = "A1234BC", firstName = null, lastName = "Smith", dob = null)
    val expectedRequestBody = mapOf(
      "nomsNumber" to "A1234BC",
      "firstName" to null,
      "lastName" to "Smith",
      "dateOfBirth" to null
    )

    assertThat(prisonerSearchRequest.toRequestBody()).isEqualTo(expectedRequestBody)
  }
}
