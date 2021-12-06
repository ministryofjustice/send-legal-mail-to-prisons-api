package uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.integration.magiclink

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.fail
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.BodyInserters
import org.springframework.web.reactive.function.client.WebClient
import uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.config.EmailInvalid
import uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.config.EmailInvalidCjsm
import uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.config.EmailMandatory
import uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.config.MalformedRequest
import uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.integration.IntegrationTest
import uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.magiclink.MagicLinkSecret
import uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.magiclink.VerifyLinkResponse

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
        .body(BodyInserters.fromValue("""{ "email": "some.email@company.com.cjsm.net" }"""))
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
        .expectBody().jsonPath("$.errorCode").isEqualTo(MalformedRequest.code)
    }

    @Test
    fun `bad request with malformed body`() {
      webTestClient.post()
        .uri("/link/email")
        .accept(MediaType.APPLICATION_JSON)
        .contentType(MediaType.APPLICATION_JSON)
        .headers(setAuthorisation())
        .body(BodyInserters.fromValue("""{ "didnt-expect-this": "some.email@company.com.cjsm.net" }"""))
        .exchange()
        .expectStatus().isBadRequest
        .expectBody().jsonPath("$.errorCode").isEqualTo(MalformedRequest.code)
    }

    @Test
    fun `bad request with missing email`() {
      webTestClient.post()
        .uri("/link/email")
        .accept(MediaType.APPLICATION_JSON)
        .contentType(MediaType.APPLICATION_JSON)
        .headers(setAuthorisation())
        .body(BodyInserters.fromValue("""{ "email": "" }"""))
        .exchange()
        .expectStatus().isBadRequest
        .expectBody()
        .jsonPath("$.errorCode").isEqualTo(EmailMandatory.code)
        .jsonPath("$.userMessage").value<String> { it.contains("email address") }
    }

    @Test
    fun `bad request with invalid email`() {
      webTestClient.post()
        .uri("/link/email")
        .accept(MediaType.APPLICATION_JSON)
        .contentType(MediaType.APPLICATION_JSON)
        .headers(setAuthorisation())
        .body(BodyInserters.fromValue("""{ "email": "invalid@email" }"""))
        .exchange()
        .expectStatus().isBadRequest
        .expectBody()
        .jsonPath("$.errorCode").isEqualTo(EmailInvalid.code)
        .jsonPath("$.userMessage").value<String> { it.contains("email address") }
    }

    @Test
    fun `bad request with non CJSM email`() {
      webTestClient.post()
        .uri("/link/email")
        .accept(MediaType.APPLICATION_JSON)
        .contentType(MediaType.APPLICATION_JSON)
        .headers(setAuthorisation())
        .body(BodyInserters.fromValue("""{ "email": "some.email@company.com" }"""))
        .exchange()
        .expectStatus().isBadRequest
        .expectBody()
        .jsonPath("$.errorCode").isEqualTo(EmailInvalidCjsm.code)
        .jsonPath("$.userMessage").value<String> { it.contains("cjsm.net") }
    }

    @Test
    fun `saves secret and sends magic link`() {
      webTestClient.post()
        .uri("/link/email")
        .accept(MediaType.APPLICATION_JSON)
        .contentType(MediaType.APPLICATION_JSON)
        .headers(setAuthorisation())
        .body(BodyInserters.fromValue("""{ "email": "some.email@company.com.cjsm.net" }"""))
        .exchange()
        .expectStatus().isCreated

      val savedSecret = magicLinkSecretRepository.findAll().firstOrNull()
      val message = mailCatcherWebClient.get()
        .uri("/messages/1.json")
        .accept(MediaType.APPLICATION_JSON)
        .retrieve()
        .bodyToMono(Message::class.java)
        .block()

      assertThat(message?.recipients).containsExactly("<some.email@company.com.cjsm.net>")
      assertThat(message?.source).contains("${magicLinkConfig.url}?secret=${savedSecret?.secretValue}")
    }
  }

  @Nested
  inner class VerifyMagicLink {

    @Test
    fun `unauthorised without a valid auth token`() {
      webTestClient.post()
        .uri("/link/verify")
        .accept(MediaType.APPLICATION_JSON)
        .contentType(MediaType.APPLICATION_JSON)
        .body(BodyInserters.fromValue("""{ "secret": "some-secret-value" }"""))
        .exchange()
        .expectStatus().isUnauthorized
    }

    @Test
    fun `bad request without body`() {
      webTestClient.post()
        .uri("/link/verify")
        .accept(MediaType.APPLICATION_JSON)
        .contentType(MediaType.APPLICATION_JSON)
        .headers(setAuthorisation())
        .exchange()
        .expectStatus().isBadRequest
        .expectBody().jsonPath("$.errorCode").isEqualTo(MalformedRequest.code)
    }

    @Test
    fun `bad request with malformed body`() {
      webTestClient.post()
        .uri("/link/verify")
        .accept(MediaType.APPLICATION_JSON)
        .contentType(MediaType.APPLICATION_JSON)
        .headers(setAuthorisation())
        .body(BodyInserters.fromValue("""{ "didnt-expect-this": "some-secret-value" }"""))
        .exchange()
        .expectStatus().isBadRequest
        .expectBody().jsonPath("$.errorCode").isEqualTo(MalformedRequest.code)
    }

    @Test
    fun `not found with missing secret value`() {
      webTestClient.post()
        .uri("/link/verify")
        .accept(MediaType.APPLICATION_JSON)
        .contentType(MediaType.APPLICATION_JSON)
        .headers(setAuthorisation())
        .body(BodyInserters.fromValue("""{ "secret": "" }"""))
        .exchange()
        .expectStatus().isNotFound
    }

    @Test
    fun `generates a valid JWT for a magic link secret and deletes the secret`() {
      magicLinkSecretRepository.save(MagicLinkSecret("some-secret", "some.email@company.com.cjsm.net"))

      val jwt = webTestClient.post()
        .uri("/link/verify")
        .accept(MediaType.APPLICATION_JSON)
        .contentType(MediaType.APPLICATION_JSON)
        .headers(setAuthorisation())
        .body(BodyInserters.fromValue("""{ "secret": "some-secret" }"""))
        .exchange()
        .expectStatus().isCreated
        .returnResult(VerifyLinkResponse::class.java)
        .responseBody
        .blockFirst()
        ?: fail("Did not receive a response from /link/verify")

      assertThat(magicLinkSecretRepository.findById("some-secret")).isEmpty
      assertThat(jwtService.validateToken(jwt.token)).isTrue
      assertThat(jwtService.subject(jwt.token)).isEqualTo("some.email@company.com.cjsm.net")
      assertThat(jwtService.authorities(jwt.token)).containsExactly("ROLE_SLM_CREATE_BARCODE")
    }

    @Test
    fun `not found if the secret doesn't exist`() {
      assertThat(magicLinkSecretRepository.findById("some-secret")).isEmpty

      webTestClient.post()
        .uri("/link/verify")
        .accept(MediaType.APPLICATION_JSON)
        .contentType(MediaType.APPLICATION_JSON)
        .headers(setAuthorisation())
        .body(BodyInserters.fromValue("""{ "secret": "some-secret" }"""))
        .exchange()
        .expectStatus().isNotFound

      assertThat(magicLinkSecretRepository.findById("some-secret")).isEmpty
    }
  }
}

data class Message(
  val id: Int,
  val recipients: List<String>,
  val source: String,
)
