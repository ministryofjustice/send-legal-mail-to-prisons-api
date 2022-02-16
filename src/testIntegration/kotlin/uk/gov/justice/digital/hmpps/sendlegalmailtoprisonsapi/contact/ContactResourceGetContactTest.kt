package uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.contact

import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.context.jdbc.Sql
import uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.IntegrationTest
import uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.config.AuthenticationError
import uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.config.NotFound

@Sql("/createContacts.sql")
class ContactResourceGetContactTest : IntegrationTest() {
  @Test
  fun `unauthorised without a valid auth token`() {
    webTestClient.get()
      .uri("/contact/A1234BC")
      .accept(MediaType.APPLICATION_JSON)
      .exchange()
      .expectStatus().isUnauthorized
  }

  @Test
  fun `forbidden without a valid role`() {
    webTestClient.get()
      .uri("/contact/A1234BC")
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(user = "AUSER_GEN"))
      .exchange()
      .expectStatus().isForbidden
      .expectBody().jsonPath("$.errorCode.code").isEqualTo(AuthenticationError.code)
  }

  @Test
  fun `not found given unknown prison number`() {
    webTestClient.get()
      .uri("/contact/X9999ZZ")
      .accept(MediaType.APPLICATION_JSON)
      .headers(setCreateBarcodeAuthorisation())
      .exchange()
      .expectStatus().isNotFound
      .expectBody().jsonPath("$.errorCode.code").isEqualTo(NotFound.code)
  }

  @Test
  fun `returns contact given valid prison number`() {
    webTestClient.get()
      .uri("/contact/A1234BC")
      .accept(MediaType.APPLICATION_JSON)
      .headers(setCreateBarcodeAuthorisation())
      .exchange()
      .expectStatus().isOk
      .expectBody()
      .jsonPath("$.prisonNumber").isEqualTo("A1234BC")
      .jsonPath("$.prisonerName").isEqualTo("John Smith")
  }
}
