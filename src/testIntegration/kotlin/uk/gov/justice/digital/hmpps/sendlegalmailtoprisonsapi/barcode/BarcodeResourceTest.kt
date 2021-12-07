package uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.barcode

import com.nhaarman.mockitokotlin2.whenever
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.BodyInserters
import uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.IntegrationTest
import uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.config.MalformedRequest

class BarcodeResourceTest : IntegrationTest() {

  @Nested
  inner class CreateBarcode {
    @Test
    fun `unauthorised without a valid auth token`() {
      webTestClient.post()
        .uri("/barcode")
        .accept(MediaType.APPLICATION_JSON)
        .contentType(MediaType.APPLICATION_JSON)
        .exchange()
        .expectStatus().isUnauthorized
    }

    @Test
    fun `forbidden without a valid role`() {
      webTestClient.post()
        .uri("/barcode")
        .accept(MediaType.APPLICATION_JSON)
        .contentType(MediaType.APPLICATION_JSON)
        .headers(setAuthorisation(user = "some.user@domain.com"))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `can create a barcode with the service`() {
      whenever(barcodeGeneratorService.generateBarcode()).thenReturn("SOME_CODE")

      webTestClient.post()
        .uri("/barcode")
        .accept(MediaType.APPLICATION_JSON)
        .contentType(MediaType.APPLICATION_JSON)
        .headers(setCreateBarcodeAuthorisation())
        .exchange()
        .expectStatus().isCreated
        .expectBody()
        .jsonPath("$").isEqualTo("SOME_CODE")

      val barcode = barcodeRepository.findById("SOME_CODE").orElseThrow()
      barcodeEventRepository.findByBarcodeAndStatus(barcode, BarcodeStatus.CREATED).firstOrNull()
    }

    @Test
    fun `will retry if the barcode already exists`() {
      barcodeRepository.save(Barcode(code = "SOME_CODE"))
      whenever(barcodeGeneratorService.generateBarcode())
        .thenReturn("SOME_CODE")
        .thenReturn("ANOTHER_CODE")

      webTestClient.post()
        .uri("/barcode")
        .accept(MediaType.APPLICATION_JSON)
        .contentType(MediaType.APPLICATION_JSON)
        .headers(setCreateBarcodeAuthorisation())
        .exchange()
        .expectStatus().isCreated
        .expectBody()
        .jsonPath("$").isEqualTo("ANOTHER_CODE")
    }
  }

  @Nested
  inner class CheckBarcode {
    @Test
    fun `unauthorised without a valid auth token`() {
      webTestClient.post()
        .uri("/barcode/check")
        .accept(MediaType.APPLICATION_JSON)
        .contentType(MediaType.APPLICATION_JSON)
        .body(BodyInserters.fromValue("""{ "barcode": "any-barcode" }"""))
        .exchange()
        .expectStatus().isUnauthorized
    }

    @Test
    fun `forbidden without a valid role`() {
      webTestClient.post()
        .uri("/barcode/check")
        .accept(MediaType.APPLICATION_JSON)
        .contentType(MediaType.APPLICATION_JSON)
        .headers(setAuthorisation(user = "any.user@domain.com"))
        .body(BodyInserters.fromValue("""{ "barcode": "any-barcode" }"""))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `bad request with malformed body`() {
      webTestClient.post()
        .uri("/barcode/check")
        .accept(MediaType.APPLICATION_JSON)
        .contentType(MediaType.APPLICATION_JSON)
        .headers(setAuthorisation(user = "any.user@domain.com", roles = listOf("ROLE_SLM_SCAN_BARCODE")))
        .body(BodyInserters.fromValue("""{ "not-expecting-this": "missing-barcode" }"""))
        .exchange()
        .expectStatus().isBadRequest
        .expectBody().jsonPath("$.errorCode").isEqualTo(MalformedRequest.code)
    }

    @Test
    fun `bad request with missing body`() {
      webTestClient.post()
        .uri("/barcode/check")
        .accept(MediaType.APPLICATION_JSON)
        .contentType(MediaType.APPLICATION_JSON)
        .headers(setAuthorisation(user = "any.user@domain.com", roles = listOf("ROLE_SLM_SCAN_BARCODE")))
        .exchange()
        .expectStatus().isBadRequest
        .expectBody().jsonPath("$.errorCode").isEqualTo(MalformedRequest.code)
    }

    @Test
    fun `not found if barcode does not exist`() {
      webTestClient.post()
        .uri("/barcode/check")
        .accept(MediaType.APPLICATION_JSON)
        .contentType(MediaType.APPLICATION_JSON)
        .headers(setAuthorisation(user = "some.user@domain.com", roles = listOf("ROLE_SLM_SCAN_BARCODE")))
        .body(BodyInserters.fromValue("""{ "barcode": "doesnt-exist" }"""))
        .exchange()
        .expectStatus().isNotFound
    }

    @Test
    fun `OK if barcode exists`() {
      whenever(barcodeGeneratorService.generateBarcode()).thenReturn("SOME_BARCODE")

      webTestClient.post()
        .uri("/barcode")
        .accept(MediaType.APPLICATION_JSON)
        .contentType(MediaType.APPLICATION_JSON)
        .headers(setCreateBarcodeAuthorisation())
        .exchange()
        .expectStatus().isCreated

      webTestClient.post()
        .uri("/barcode/check")
        .accept(MediaType.APPLICATION_JSON)
        .contentType(MediaType.APPLICATION_JSON)
        .headers(setAuthorisation(user = "some.user@domain.com", roles = listOf("ROLE_SLM_SCAN_BARCODE")))
        .body(BodyInserters.fromValue("""{ "barcode": "SOME_BARCODE" }"""))
        .exchange()
        .expectStatus().isOk
    }
  }
}
