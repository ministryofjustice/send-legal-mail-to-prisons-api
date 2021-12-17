package uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.fail
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.BodyInserters
import org.springframework.web.reactive.function.client.WebClient
import uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.cjsm.CjsmDirectoryEntry
import uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.magiclink.Message
import uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.magiclink.VerifyLinkResponse

class E2eTest(
  @Value("\${mailcatcher.api.port}") private val mailcatcherApiPort: Int,
) : IntegrationTest() {

  private val mailCatcherWebClient = WebClient.builder().baseUrl("http://localhost:$mailcatcherApiPort").build()

  @BeforeEach
  fun `add the user's organisation to the CJSM directory`() {
    cjsmDirectoryRepository.save(CjsmDirectoryEntry(1L, "some.email@company.com.cjsm.net", "anyfirstname", "anylastname", "Some Company", "Anytown", "Any type"))
  }

  @AfterEach
  fun `clear mail server`() {
    mailCatcherWebClient.delete().uri("/messages").retrieve().bodyToMono(Void::class.java).block()
  }

  @Test
  fun `can sign in with magic link then create and check a barcode`() {
    requestMagicLink("some.email@company.com.cjsm.net")
    val secretValue = getSecretFromReceivedEmail()
    val jwt = requestVerifySecret(secretValue)
    requestVerifySecretFails(secretValue)
    val barcode = requestCreateBarcode(jwt)

    requestCheckBarcodeOk(barcode)
    requestCheckBarcodeDuplicate(barcode)
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

  private fun requestVerifySecret(secretValue: String) =
    webTestClient.post()
      .uri("/link/verify")
      .accept(MediaType.APPLICATION_JSON)
      .contentType(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation())
      .body(BodyInserters.fromValue("""{ "secret": "$secretValue" }"""))
      .exchange()
      .expectStatus().isCreated
      .returnResult(VerifyLinkResponse::class.java)
      .responseBody
      .blockFirst()
      ?.token
      ?: fail("Did not receive a response from /link/verify")

  private fun requestVerifySecretFails(secretValue: String) =
    webTestClient.post()
      .uri("/link/verify")
      .accept(MediaType.APPLICATION_JSON)
      .contentType(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation())
      .body(BodyInserters.fromValue("""{ "secret": "$secretValue" }"""))
      .exchange()
      .expectStatus().isNotFound

  private fun requestCreateBarcode(jwt: String): String =
    webTestClient.post()
      .uri("/barcode")
      .accept(MediaType.APPLICATION_JSON)
      .contentType(MediaType.APPLICATION_JSON)
      .header("Create-Barcode-Token", jwt)
      .exchange()
      .expectStatus().isCreated
      .returnResult(String::class.java)
      .responseBody
      .blockFirst()
      ?.also { barcode ->
        assertThat(barcode.length).isEqualTo(12)
        assertThat(barcode).containsOnlyDigits()
      }
      ?: fail("Did not receive a response from /barcode")

  private fun requestCheckBarcodeOk(barcode: String) =
    webTestClient.post()
      .uri("/barcode/check")
      .accept(MediaType.APPLICATION_JSON)
      .contentType(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(user = "AUSER_GEN", roles = listOf("ROLE_SLM_SCAN_BARCODE")))
      .body(BodyInserters.fromValue("""{ "barcode": "$barcode" }"""))
      .exchange()
      .expectStatus().isOk
      .expectBody().jsonPath("$.createdBy").isEqualTo("Some Company")

  private fun requestCheckBarcodeDuplicate(barcode: String) =
    webTestClient.post()
      .uri("/barcode/check")
      .accept(MediaType.APPLICATION_JSON)
      .contentType(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(user = "AUSER_GEN", roles = listOf("ROLE_SLM_SCAN_BARCODE")))
      .body(BodyInserters.fromValue("""{ "barcode": "$barcode" }"""))
      .exchange()
      .expectStatus().isBadRequest
      .expectBody()
      .jsonPath("$.errorCode.code").isEqualTo("DUPLICATE")
      .jsonPath("$.errorCode.createdBy").isEqualTo("Some Company")
}
