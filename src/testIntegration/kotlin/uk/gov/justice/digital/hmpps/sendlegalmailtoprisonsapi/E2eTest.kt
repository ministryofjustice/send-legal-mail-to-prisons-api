package uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.fail
import org.awaitility.kotlin.await
import org.awaitility.kotlin.until
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.BodyInserters
import org.springframework.web.reactive.function.client.WebClient
import uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.barcode.CreateBarcodeRequest
import uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.barcode.CreateBarcodeResponse
import uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.cjsm.CjsmDirectoryEntry
import uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.magiclink.VerifyLinkResponse
import uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.mocks.PrisonerSearchExtension

class E2eTest(
  @Value("\${mailcatcher.api.port}") private val mailcatcherApiPort: Int,
) : IntegrationTest() {

  private val mailCatcherWebClient = WebClient.builder().baseUrl("http://localhost:$mailcatcherApiPort").build()

  @BeforeEach
  fun `add the user's organisation to the CJSM directory`() {
    cjsmDirectoryRepository.save(CjsmDirectoryEntry(1L, "some.email@company.com.cjsm.net", "anyfirstname", "anylastname", "Some Company", "Anytown", "Any type"))
  }

  @BeforeEach
  fun `mock prisoner search response`() {
    PrisonerSearchExtension.prisonerSearchApi.stubMatchPrisoners()
    PrisonerSearchExtension.prisonerSearchApi.stubGlobalSearch()
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
    val barcode = requestCreateBarcode(jwt).barcode

    requestCheckBarcodeOk(barcode)
    requestCheckBarcodeDuplicate(barcode)
    with(PrisonerSearchExtension.prisonerSearchApi) {
      await until { matchPrisonersHasBeenCalled() && globalSearchHasBeenCalled() }
    }
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
    val messageSource = mailCatcherWebClient.get()
      .uri("/messages/1.source")
      .accept(MediaType.APPLICATION_JSON)
      .retrieve()
      .bodyToMono(String::class.java)
      .block() as String
    val (secretValue) = ".*secret=(.*)".toRegex().find(messageSource)!!.destructured
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

  private fun requestCreateBarcode(jwt: String): CreateBarcodeResponse =
    webTestClient.post()
      .uri("/barcode")
      .accept(MediaType.APPLICATION_JSON)
      .contentType(MediaType.APPLICATION_JSON)
      .header("Create-Barcode-Token", jwt)
      .bodyValue(aCreateBarcodeRequest())
      .exchange()
      .expectStatus().isCreated
      .returnResult(CreateBarcodeResponse::class.java)
      .responseBody
      .blockFirst()
      ?.also { response ->
        assertThat(response.barcode.length).isEqualTo(12)
        assertThat(response.barcode).containsOnlyDigits()
      }
      ?: fail("Did not receive a response from /barcode")

  private fun aCreateBarcodeRequest(): CreateBarcodeRequest =
    CreateBarcodeRequest(prisonerName = "Fred Bloggs", prisonId = "BXI", prisonNumber = "A1234BC")

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
