package uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.integration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.BodyInserters
import org.springframework.web.reactive.function.client.WebClient
import uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.integration.magiclink.Message

class E2eTest(
  @Value("\${mailcatcher.api.port}") private val mailcatcherApiPort: Int,
) : IntegrationTest() {

  private val mailCatcherWebClient = WebClient.builder().baseUrl("http://localhost:$mailcatcherApiPort").build()

  @AfterEach
  fun `clear mail server`() {
    mailCatcherWebClient.delete().uri("/messages").retrieve().bodyToMono(Void::class.java).block()
  }

  @Test
  fun `can request and verify a magic link to receive an auth token`() {
    val email = "some.email@company.com.cjsm.net"
    requestMagicLink(email)
    val secretValue = getSecretFromReceivedEmail()
    val jwt = verifySecret(secretValue)

    checkJwt(jwt, email)
    verifySecretNotFound(secretValue)

    // TODO SLM-12 instead of checking the JWT use it to call the create barcode endpoint
  }

  private fun requestMagicLink(email: String) {
    webTestClient.post()
      .uri("/link/email")
      .accept(MediaType.APPLICATION_JSON)
      .contentType(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation())
      .body(BodyInserters.fromValue("""{ "email": "$email" }"""))
      .exchange()
      .expectStatus().isCreated
  }

  private fun getSecretFromReceivedEmail(): String {
    val message = mailCatcherWebClient.get()
      .uri("/messages/1.json")
      .accept(MediaType.APPLICATION_JSON)
      .retrieve()
      .bodyToMono(Message::class.java)
      .block() as Message
    val (secretValue) = ".*secret=(.*)$".toRegex().find(message.source)!!.destructured
    return secretValue
  }

  private fun verifySecret(secretValue: String) =
    webTestClient.post()
      .uri("/link/verify")
      .accept(MediaType.APPLICATION_JSON)
      .contentType(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation())
      .body(BodyInserters.fromValue("""{ "secret": "$secretValue" }"""))
      .exchange()
      .expectStatus().isCreated
      .returnResult(String::class.java)
      .responseBody
      .blockFirst()
      .orEmpty()

  private fun verifySecretNotFound(secretValue: String) =
    webTestClient.post()
      .uri("/link/verify")
      .accept(MediaType.APPLICATION_JSON)
      .contentType(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation())
      .body(BodyInserters.fromValue("""{ "secret": "$secretValue" }"""))
      .exchange()
      .expectStatus().isNotFound

  private fun checkJwt(jwt: String, email: String) {
    assertThat(jwtService.validateToken(jwt)).isTrue
    assertThat(jwtService.subject(jwt)).isEqualTo(email)
    assertThat(jwtService.authorities(jwt)).containsExactly("ROLE_SLM_CREATE_BARCODE")
  }
}
