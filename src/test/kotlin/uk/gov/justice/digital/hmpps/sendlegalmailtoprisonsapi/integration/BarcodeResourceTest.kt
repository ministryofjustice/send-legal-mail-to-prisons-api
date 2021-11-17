package uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.integration

import com.nhaarman.mockitokotlin2.whenever
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.BodyInserters
import uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.barcode.Barcode
import uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.barcode.BarcodeEvent
import uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.barcode.BarcodeStatus

class BarcodeResourceTest : IntegrationTest() {

  @Nested
  inner class CreateBarcode {
    @Test
    fun `unauthorised without a valid auth token`() {
      webTestClient.post()
        .uri("/barcode")
        .accept(MediaType.APPLICATION_JSON)
        .contentType(MediaType.APPLICATION_JSON)
        .body(BodyInserters.fromValue("""{ "userId": "some.user@domain.com" }"""))
        .exchange()
        .expectStatus().isUnauthorized
    }

    @Test
    fun `bad request with unknown request parameter`() {
      webTestClient.post()
        .uri("/barcode")
        .accept(MediaType.APPLICATION_JSON)
        .contentType(MediaType.APPLICATION_JSON)
        .headers(setAuthorisation())
        .body(BodyInserters.fromValue("""{ "unknown-request-property": "some.user@domain.com" }"""))
        .exchange()
        .expectStatus().isBadRequest
    }

    @Test
    fun `bad request with malformed request object`() {
      webTestClient.post()
        .uri("/barcode")
        .accept(MediaType.APPLICATION_JSON)
        .contentType(MediaType.APPLICATION_JSON)
        .headers(setAuthorisation())
        .body(BodyInserters.fromValue("""{ "this is bad json": }"""))
        .exchange()
        .expectStatus().isBadRequest
    }

    @Test
    fun `can create a barcode with the service`() {
      whenever(barcodeGeneratorService.generateBarcode()).thenReturn("SOME_CODE")

      webTestClient.post()
        .uri("/barcode")
        .accept(MediaType.APPLICATION_JSON)
        .contentType(MediaType.APPLICATION_JSON)
        .headers(setAuthorisation())
        .body(BodyInserters.fromValue("""{ "userId": "some.user@domain.com" }"""))
        .exchange()
        .expectStatus().isCreated
        .expectBody()
        .jsonPath("$").isEqualTo("SOME_CODE")

      val savedBarcode = barcodeRepository.findById("SOME_CODE").orElseThrow()
      val savedBarcodeEvents = barcodeEventRepository.findByBarcode(savedBarcode)
      assertThat(savedBarcodeEvents).extracting<String> { it.barcode.code }.containsExactly("SOME_CODE")
      assertThat(savedBarcodeEvents).extracting<String>(BarcodeEvent::userId).containsExactly("some.user@domain.com")
      assertThat(savedBarcodeEvents).extracting<BarcodeStatus>(BarcodeEvent::status)
        .containsExactly(BarcodeStatus.CREATED)
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
        .headers(setAuthorisation())
        .body(BodyInserters.fromValue("""{ "userId": "some.user@domain.com" }"""))
        .exchange()
        .expectStatus().isCreated
        .expectBody()
        .jsonPath("$").isEqualTo("ANOTHER_CODE")

      val savedBarcode = barcodeRepository.findById("ANOTHER_CODE").orElseThrow()
      val savedBarcodeEvents = barcodeEventRepository.findByBarcode(savedBarcode)
      assertThat(savedBarcodeEvents).extracting<String> { it.barcode.code }.containsExactly("ANOTHER_CODE")
    }
  }
}
