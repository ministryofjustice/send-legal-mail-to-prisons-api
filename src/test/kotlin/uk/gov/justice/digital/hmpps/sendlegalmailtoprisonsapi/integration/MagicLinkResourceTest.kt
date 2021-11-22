package uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.integration

import com.nhaarman.mockitokotlin2.verify
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.BodyInserters

class MagicLinkResourceTest : IntegrationTest() {

  @Nested
  inner class CreateMagicLink {

    @Test
    fun `unauthorised without a valid auth token`() {
      webTestClient.post()
        .uri("/link/email")
        .accept(MediaType.APPLICATION_JSON)
        .contentType(MediaType.APPLICATION_JSON)
        .body(BodyInserters.fromValue("""{ "email": "some.email@company.com" }"""))
        .exchange()
        .expectStatus().isUnauthorized
    }

    @Test
    fun `bad request without body`() {
      webTestClient.post()
        .uri("/link/email")
        .accept(MediaType.APPLICATION_JSON)
        .contentType(MediaType.APPLICATION_JSON)
        .headers(setAuthorisation())
        .exchange()
        .expectStatus().isBadRequest
    }

    @Test
    fun `bad request with malformed body`() {
      webTestClient.post()
        .uri("/link/email")
        .accept(MediaType.APPLICATION_JSON)
        .contentType(MediaType.APPLICATION_JSON)
        .headers(setAuthorisation())
        .body(BodyInserters.fromValue("""{ "didnt-expect-this": "some.email@company.com" }"""))
        .exchange()
        .expectStatus().isBadRequest
    }

    @Test
    fun `created for a good request`() {
      webTestClient.post()
        .uri("/link/email")
        .accept(MediaType.APPLICATION_JSON)
        .contentType(MediaType.APPLICATION_JSON)
        .headers(setAuthorisation())
        .body(BodyInserters.fromValue("""{ "email": "some.email@company.com" }"""))
        .exchange()
        .expectStatus().isCreated

      verify(magicLinkService).createAndSendMagicLink("some.email@company.com")
    }
  }
}
