package uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.barcode

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.whenever
import org.springframework.http.MediaType
import uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.config.AuthenticationError
import uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.config.MalformedRequest

@Nested
class BarcodeResourceCreateBarcodeTest : BarcodeResourceTest() {
  @Test
  fun `unauthorised without a valid auth token`() {
    webTestClient.post()
      .uri("/barcode")
      .accept(MediaType.APPLICATION_JSON)
      .contentType(MediaType.APPLICATION_JSON)
      .bodyValue(aCreateBarcodeRequest())
      .exchange()
      .expectStatus().isUnauthorized
  }

  @Test
  fun `forbidden without a valid role`() {
    webTestClient.post()
      .uri("/barcode")
      .accept(MediaType.APPLICATION_JSON)
      .contentType(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(user = "AUSER_GEN"))
      .bodyValue(aCreateBarcodeRequest())
      .exchange()
      .expectStatus().isForbidden
      .expectBody().jsonPath("$.errorCode.code").isEqualTo(AuthenticationError.code)
  }

  @Test
  fun `can create a barcode and record the recipient`() {
    whenever(barcodeGeneratorService.generateBarcode()).thenReturn("SOME_CODE")

    webTestClient.post()
      .uri("/barcode")
      .accept(MediaType.APPLICATION_JSON)
      .contentType(MediaType.APPLICATION_JSON)
      .headers(setCreateBarcodeAuthorisation())
      .bodyValue(aCreateBarcodeRequest())
      .exchange()
      .expectStatus().isCreated
      .expectBody()
      .jsonPath("$.barcode").isEqualTo("SOME_CODE")

    val barcode = barcodeRepository.findById("SOME_CODE").orElseThrow()
    Assertions.assertThat(
      barcodeEventRepository.findByBarcodeAndEventTypeOrderByCreatedDateTime(
        barcode,
        BarcodeEventType.CREATED
      )
    ).isNotEmpty
    Assertions.assertThat(barcodeRecipientRepository.getByBarcode(barcode)).isNotNull
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
      .bodyValue(aCreateBarcodeRequest())
      .exchange()
      .expectStatus().isCreated
      .expectBody()
      .jsonPath("$.barcode").isEqualTo("ANOTHER_CODE")
  }

  @Test
  fun `bad request given neither prison number or dob for the recipient`() {
    webTestClient.post()
      .uri("/barcode")
      .accept(MediaType.APPLICATION_JSON)
      .contentType(MediaType.APPLICATION_JSON)
      .headers(setCreateBarcodeAuthorisation())
      .bodyValue(
        """{ 
            "prisonerName": "John Smith",
            "prisonId": "BXI"
          }"""
      )
      .exchange()
      .expectStatus().isBadRequest
      .expectBody().jsonPath("$.errorCode.code").isEqualTo(MalformedRequest.code)
  }

  @Test
  fun `bad request given fails validation for name format`() {
    webTestClient.post()
      .uri("/barcode")
      .accept(MediaType.APPLICATION_JSON)
      .contentType(MediaType.APPLICATION_JSON)
      .headers(setCreateBarcodeAuthorisation())
      .bodyValue(
        """{ 
            "prisonerName": "<John Smith>",
            "prisonId": "BXI",
            "prisonNumber": "A1234BC"
          }"""
      )
      .exchange()
      .expectStatus().isBadRequest
      .expectBody().jsonPath("$.errorCode.code").isEqualTo(MalformedRequest.code)
  }
}
