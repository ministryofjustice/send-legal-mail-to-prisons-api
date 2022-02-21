package uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.contact

import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.context.jdbc.Sql
import uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.IntegrationTest
import uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.config.AuthenticationError
import uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.config.NotFound
import java.time.format.DateTimeFormatter

@Sql("/createContacts.sql")
class ContactResourceGetContactByIdTest : IntegrationTest() {
  val testContact by lazy { contactRepository.findAll().first { it.dob != null } }

  @Test
  fun `unauthorised without a valid auth token`() {
    webTestClient.get()
      .uri("/contact/${testContact.id}")
      .accept(MediaType.APPLICATION_JSON)
      .exchange()
      .expectStatus().isUnauthorized
  }

  @Test
  fun `forbidden without a valid role`() {
    webTestClient.get()
      .uri("/contact/${testContact.id}")
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(user = "AUSER_GEN"))
      .exchange()
      .expectStatus().isForbidden
      .expectBody().jsonPath("$.errorCode.code").isEqualTo(AuthenticationError.code)
  }

  @Test
  fun `not found given unknown id`() {
    webTestClient.get()
      .uri("/contact/99999")
      .accept(MediaType.APPLICATION_JSON)
      .headers(setCreateBarcodeAuthorisation())
      .exchange()
      .expectStatus().isNotFound
      .expectBody().jsonPath("$.errorCode.code").isEqualTo(NotFound.code)
  }

  @Test
  fun `returns contact given valid prison number`() {
    webTestClient.get()
      .uri("/contact/${testContact.id}")
      .accept(MediaType.APPLICATION_JSON)
      .headers(setCreateBarcodeAuthorisation())
      .exchange()
      .expectStatus().isOk
      .expectBody()
      .jsonPath("$.id").isEqualTo(testContact.id)
      .jsonPath("$.prisonerName").isEqualTo(testContact.name)
      .jsonPath("$.prisonNumber").isEqualTo(testContact.prisonNumber)
      .jsonPath("$.dob").isEqualTo(DateTimeFormatter.ISO_LOCAL_DATE.format(testContact.dob))
      .jsonPath("$.prisonId").isEqualTo(testContact.prisonCode)
  }
}
