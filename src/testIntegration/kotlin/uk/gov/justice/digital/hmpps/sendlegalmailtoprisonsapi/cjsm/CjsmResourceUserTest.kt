package uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.cjsm

import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.IntegrationTest
import uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.config.AuthenticationError

class CjsmResourceUserTest : IntegrationTest() {

  @Test
  fun `unauthorised without a valid auth token`() {
    webTestClient.get()
      .uri("/cjsm/user/me")
      .accept(MediaType.APPLICATION_JSON)
      .exchange()
      .expectStatus().isUnauthorized
  }

  @Test
  fun `forbidden without a valid role`() {
    webTestClient.get()
      .uri("/cjsm/user/me")
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(user = "AUSER_GEN"))
      .exchange()
      .expectStatus().isForbidden
      .expectBody().jsonPath("$.errorCode.code").isEqualTo(AuthenticationError.code)
  }

  @Test
  fun `Not found if user not in directory`() {
    webTestClient.get()
      .uri("/cjsm/user/me")
      .accept(MediaType.APPLICATION_JSON)
      .headers(setCreateBarcodeAuthorisation(email = "some-email"))
      .exchange()
      .expectStatus().isNotFound
  }

  @Test
  fun `OK if user found`() {
    cjsmDirectoryRepository.save(
      CjsmDirectoryEntry(
        secureEmail = "some-email",
        firstName = "some-first-name",
        lastName = "some-last-name",
        organisation = "some-org",
        townCity = "some-town-city",
        businessType = "some-business-type",
      ),
    )

    webTestClient.get()
      .uri("/cjsm/user/me")
      .accept(MediaType.APPLICATION_JSON)
      .headers(setCreateBarcodeAuthorisation(email = "some-email"))
      .exchange()
      .expectStatus().isOk
      .expectBody()
      .jsonPath("$.userId").isEqualTo("some-email")
      .jsonPath("$.organisation").isEqualTo("some-org")
      .jsonPath("$.organisationType").isEqualTo("some-business-type")
      .jsonPath("$.townOrCity").isEqualTo("some-town-city")
  }
}
