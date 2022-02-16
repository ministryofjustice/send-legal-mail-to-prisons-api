package uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.barcode

import org.junit.jupiter.api.Test
import org.mockito.kotlin.whenever
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.BodyInserters
import uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.config.AuthenticationError
import uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.config.MalformedRequest
import uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.config.NotFound

class BarcodeResourceCreateMoreChecksRequestedEventTest : BarcodeResourceTest() {
  @Test
  fun `unauthorised without a valid auth token`() {
    webTestClient.post()
      .uri("/barcode/event/more-checks-requested")
      .accept(MediaType.APPLICATION_JSON)
      .contentType(MediaType.APPLICATION_JSON)
      .bodyValue(aCreateBarcodeRequest())
      .exchange()
      .expectStatus().isUnauthorized
  }

  @Test
  fun `forbidden without a valid role`() {
    webTestClient.post()
      .uri("/barcode/event/more-checks-requested")
      .accept(MediaType.APPLICATION_JSON)
      .contentType(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(user = "any.user@domain.com"))
      .body(BodyInserters.fromValue("""{ "barcode": "any-barcode" }"""))
      .exchange()
      .expectStatus().isForbidden
      .expectBody().jsonPath("$.errorCode.code").isEqualTo(AuthenticationError.code)
  }

  @Test
  fun `bad request with malformed body`() {
    webTestClient.post()
      .uri("/barcode/event/more-checks-requested")
      .accept(MediaType.APPLICATION_JSON)
      .contentType(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(user = "any.user@domain.com", roles = listOf("ROLE_SLM_SCAN_BARCODE")))
      .body(BodyInserters.fromValue("""{ "not-expecting-this": "missing-barcode" }"""))
      .exchange()
      .expectStatus().isBadRequest
      .expectBody().jsonPath("$.errorCode.code").isEqualTo(MalformedRequest.code)
  }

  @Test
  fun `bad request with missing body`() {
    webTestClient.post()
      .uri("/barcode/event/more-checks-requested")
      .accept(MediaType.APPLICATION_JSON)
      .contentType(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(user = "any.user@domain.com", roles = listOf("ROLE_SLM_SCAN_BARCODE")))
      .exchange()
      .expectStatus().isBadRequest
      .expectBody().jsonPath("$.errorCode.code").isEqualTo(MalformedRequest.code)
  }

  @Test
  fun `Created if barcode exists`() {
    whenever(barcodeGeneratorService.generateBarcode()).thenReturn("SOME_BARCODE")

    webTestClient.post()
      .uri("/barcode")
      .accept(MediaType.APPLICATION_JSON)
      .contentType(MediaType.APPLICATION_JSON)
      .headers(setCreateBarcodeAuthorisation())
      .bodyValue(aCreateBarcodeRequest())
      .exchange()
      .expectStatus().isCreated

    webTestClient.post()
      .uri("/barcode/event/more-checks-requested")
      .accept(MediaType.APPLICATION_JSON)
      .contentType(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(user = "AUSER_GEN", roles = listOf("ROLE_SLM_SCAN_BARCODE")))
      .body(BodyInserters.fromValue("""{ "barcode": "SOME_BARCODE" }"""))
      .exchange()
      .expectStatus().isCreated
      .expectBody().isEmpty
  }

  @Test
  fun `not found if barcode does not exist`() {
    webTestClient.post()
      .uri("/barcode/event/more-checks-requested")
      .accept(MediaType.APPLICATION_JSON)
      .contentType(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(user = "AUSER_GEN", roles = listOf("ROLE_SLM_SCAN_BARCODE")))
      .body(BodyInserters.fromValue("""{ "barcode": "doesnt-exist" }"""))
      .exchange()
      .expectStatus().isNotFound
      .expectBody().jsonPath("$.errorCode.code").isEqualTo(NotFound.code)
  }
}
