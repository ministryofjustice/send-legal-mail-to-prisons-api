package uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.integration.barcode

import com.nhaarman.mockitokotlin2.whenever
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.barcode.Barcode
import uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.barcode.BarcodeEvent
import uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.barcode.BarcodeStatus
import uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.integration.IntegrationTest

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
        .headers(setAuthorisation(user = "some.user@domain.com", roles = listOf("ROLE_SLM_CREATE_BARCODE")))
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
        .headers(setAuthorisation(user = "some.user@domain.com", roles = listOf("ROLE_SLM_CREATE_BARCODE")))
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
