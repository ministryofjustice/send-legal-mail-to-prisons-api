package uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.integration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.BodyInserters
import org.springframework.web.reactive.function.client.WebClient

class MagicLinkResourceTest(
  @Value("\${mailcatcher.api.port}") private val mailcatcherApiPort: Int,
) : IntegrationTest() {

  private val mailCatcherWebClient = WebClient.builder().baseUrl("http://localhost:$mailcatcherApiPort").build()

  @Nested
  inner class CreateMagicLink {

    @AfterEach
    fun `clear mail server`() {
      mailCatcherWebClient.delete().uri("/messages").retrieve().bodyToMono(Void::class.java).block()
    }

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
    fun `saves secret and sends magic link`() {
      webTestClient.post()
        .uri("/link/email")
        .accept(MediaType.APPLICATION_JSON)
        .contentType(MediaType.APPLICATION_JSON)
        .headers(setAuthorisation())
        .body(BodyInserters.fromValue("""{ "email": "some.email@company.com" }"""))
        .exchange()
        .expectStatus().isCreated

      val savedSecret = magicLinkSecretRepository.findAll().firstOrNull()
      val message = mailCatcherWebClient.get()
        .uri("/messages/1.json")
        .accept(MediaType.APPLICATION_JSON)
        .retrieve()
        .bodyToMono(Message::class.java)
        .block()

      assertThat(message?.recipients).containsExactly("<some.email@company.com>")
      assertThat(message?.source).contains("${magicLinkConfig.url}?secret=${savedSecret?.secretValue}")
    }
  }
}

data class Message(
  val id: Int,
  val recipients: List<String>,
  val source: String,
)
