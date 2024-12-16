package uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.fail
import org.junit.jupiter.api.BeforeEach
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.BodyInserters
import uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.barcode.CreateBarcodeRequest
import uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.barcode.CreateBarcodeResponse
import uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.cjsm.CjsmDirectoryEntry
import uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.mocks.PrisonerSearchExtension

class E2eTest : IntegrationTest() {

  @BeforeEach
  fun `add the user's organisation to the CJSM directory`() {
    cjsmDirectoryRepository.save(
      CjsmDirectoryEntry(
        secureEmail = "some.email@company.com.cjsm.net",
        firstName = "anyfirstname",
        lastName = "anylastname",
        organisation = "Some Company",
        townCity = "Anytown",
        businessType = "Any type",
      ),
    )
  }

  @BeforeEach
  fun `mock prisoner search response`() {
    PrisonerSearchExtension.prisonerSearchApi.stubMatchPrisoners()
    PrisonerSearchExtension.prisonerSearchApi.stubGlobalSearch()
  }

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
