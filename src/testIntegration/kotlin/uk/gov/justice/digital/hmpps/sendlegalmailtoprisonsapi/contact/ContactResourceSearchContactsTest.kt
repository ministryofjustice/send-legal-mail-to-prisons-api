package uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.contact

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.context.jdbc.Sql
import uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.IntegrationTest
import uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.config.AuthenticationError
import uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.config.MalformedRequest

@Sql("/createContacts.sql")
class ContactResourceSearchContactsTest : IntegrationTest() {
  @Test
  fun `unauthorised without a valid auth token`() {
    webTestClient.get()
      .uri("/contacts?name=fred")
      .accept(MediaType.APPLICATION_JSON)
      .exchange()
      .expectStatus().isUnauthorized
  }

  @Test
  fun `forbidden without a valid role`() {
    webTestClient.get()
      .uri("/contacts?name=fred")
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(user = "AUSER_GEN"))
      .exchange()
      .expectStatus().isForbidden
      .expectBody().jsonPath("$.errorCode.code").isEqualTo(AuthenticationError.code)
  }

  @Test
  fun `bad request without required query string parameter`() {
    webTestClient.get()
      .uri("/contacts")
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation())
      .exchange()
      .expectStatus().isBadRequest
      .expectBody().jsonPath("$.errorCode.code").isEqualTo(MalformedRequest.code)
  }

  @Test
  fun `returns matching Contacts given owner and name that match records`() {
    val contacts: List<ContactResponse> = webTestClient.get()
      .uri("/contacts?name=john")
      .accept(MediaType.APPLICATION_JSON)
      .headers(setCreateBarcodeAuthorisation(email = "some.user@company.com.cjsm.net"))
      .exchange()
      .expectStatus().isOk
      .expectBodyList(ContactResponse::class.java).hasSize(4).returnResult().responseBody

    val contactNames = contacts.map { it.prisonerName }
    Assertions.assertThat(contactNames).containsExactly("John Smith", "Johnathon Evans", "Malcolm Johnston", "Bart Johnson")
  }

  @Test
  fun `returns zero matching Contacts given owner and name that match no records`() {
    webTestClient.get()
      .uri("/contacts?name=john")
      .accept(MediaType.APPLICATION_JSON)
      .headers(setCreateBarcodeAuthorisation(email = "some.user.with.no.contacts.in.the.database@company.com.cjsm.net"))
      .exchange()
      .expectStatus().isOk
      .expectBodyList(ContactResponse::class.java).hasSize(0)
  }
}
