package uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.barcode

import org.junit.jupiter.api.Test
import org.mockito.Mockito.`when`
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
      .headers(setSlmClientIp())
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
      .headers(setSlmClientIp())
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
      .headers(setSlmClientIp())
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
      .headers(setSlmClientIp())
      .exchange()
      .expectStatus().isBadRequest
      .expectBody().jsonPath("$.errorCode.code").isEqualTo(MalformedRequest.code)
  }

  @Test
  fun `Created if barcode exists`() {
    `when`(barcodeGeneratorService.generateBarcode()).thenReturn("SOME_BARCODE")

    webTestClient.post()
      .uri("/barcode")
      .accept(MediaType.APPLICATION_JSON)
      .contentType(MediaType.APPLICATION_JSON)
      .headers(setCreateBarcodeAuthorisation())
      .headers(setSlmClientIp())
      .bodyValue(aCreateBarcodeRequest())
      .exchange()
      .expectStatus().isCreated

    webTestClient.post()
      .uri("/barcode/event/more-checks-requested")
      .accept(MediaType.APPLICATION_JSON)
      .contentType(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(user = "AUSER_GEN", roles = listOf("ROLE_SLM_SCAN_BARCODE")))
      .headers(setSlmClientIp())
      .body(BodyInserters.fromValue("""{ "barcode": "SOME_BARCODE" }"""))
      .exchange()
      .expectStatus().isCreated
      .expectBody().isEmpty

    val barcode = Barcode("SOME_BARCODE")
    assertBarcodeEventCreated(barcode, BarcodeEventType.MORE_CHECKS_REQUESTED)
  }

  @Test
  fun `not found if barcode does not exist`() {
    webTestClient.post()
      .uri("/barcode/event/more-checks-requested")
      .accept(MediaType.APPLICATION_JSON)
      .contentType(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(user = "AUSER_GEN", roles = listOf("ROLE_SLM_SCAN_BARCODE")))
      .headers(setSlmClientIp())
      .body(BodyInserters.fromValue("""{ "barcode": "doesnt-exist" }"""))
      .exchange()
      .expectStatus().isNotFound
      .expectBody().jsonPath("$.errorCode.code").isEqualTo(NotFound.code)
  }
}
