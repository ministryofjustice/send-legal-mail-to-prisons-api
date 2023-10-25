package uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.barcode

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.check
import org.mockito.kotlin.eq
import org.mockito.kotlin.isNull
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
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
      .headers(setSlmClientIp())
      .bodyValue(aCreateBarcodeRequest())
      .exchange()
      .expectStatus().isUnauthorized

    verify(telemetryClient, times(0)).trackEvent(any(), any(), isNull())
  }

  @Test
  fun `forbidden without a valid role`() {
    webTestClient.post()
      .uri("/barcode")
      .accept(MediaType.APPLICATION_JSON)
      .contentType(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(user = "AUSER_GEN"))
      .headers(setSlmClientIp())
      .bodyValue(aCreateBarcodeRequest())
      .exchange()
      .expectStatus().isForbidden
      .expectBody().jsonPath("$.errorCode.code").isEqualTo(AuthenticationError.code)

    verify(telemetryClient, times(0)).trackEvent(any(), any(), isNull())
  }

  @Test
  fun `can create a barcode and record the recipient`() {
    whenever(barcodeGeneratorService.generateBarcode()).thenReturn("SOME_CODE")

    webTestClient.post()
      .uri("/barcode")
      .accept(MediaType.APPLICATION_JSON)
      .contentType(MediaType.APPLICATION_JSON)
      .headers(setCreateBarcodeAuthorisation())
      .headers(setSlmClientIp())
      .bodyValue(aCreateBarcodeRequest())
      .exchange()
      .expectStatus().isCreated
      .expectBody()
      .jsonPath("$.barcode").isEqualTo("SOME_CODE")

    val barcode = barcodeRepository.findById("SOME_CODE").orElseThrow()
    assertBarcodeEventCreated(barcode, BarcodeEventType.CREATED)
    assertThat(barcodeRecipientRepository.getByBarcode(barcode)).isNotNull

    verify(telemetryClient).trackEvent(
      eq("barcode-created"),
      check {
        assertThat(it["establishment"]).isEqualTo(aCreateBarcodeRequest().prisonId)
        assertThat(it["prisonNumber"]).isEqualTo(aCreateBarcodeRequest().prisonNumber)
        assertThat(it["barcodeNumber"]).isEqualTo("SOME_CODE")
        assertThat(it["sender"]).isEqualTo("some.user@company.com.cjsm.net")
      },
      isNull(),
    )
    verify(telemetryClient, times(1)).trackEvent(eq("barcode-created"), any(), isNull())
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
      .headers(setSlmClientIp())
      .bodyValue(aCreateBarcodeRequest())
      .exchange()
      .expectStatus().isCreated
      .expectBody()
      .jsonPath("$.barcode").isEqualTo("ANOTHER_CODE")

    val barcode = barcodeRepository.findById("ANOTHER_CODE").orElseThrow()
    assertBarcodeEventCreated(barcode, BarcodeEventType.CREATED)

    verify(telemetryClient).trackEvent(
      eq("barcode-created"),
      check {
        assertThat(it["establishment"]).isEqualTo(aCreateBarcodeRequest().prisonId)
        assertThat(it["prisonNumber"]).isEqualTo(aCreateBarcodeRequest().prisonNumber)
        assertThat(it["barcodeNumber"]).isEqualTo("SOME_CODE")
        assertThat(it["sender"]).isEqualTo("some.user@company.com.cjsm.net")
      },
      isNull(),
    )
    verify(telemetryClient, times(1)).trackEvent(eq("barcode-created"), any(), isNull())
  }

  @Test
  fun `bad request given neither prison number or dob for the recipient`() {
    webTestClient.post()
      .uri("/barcode")
      .accept(MediaType.APPLICATION_JSON)
      .contentType(MediaType.APPLICATION_JSON)
      .headers(setCreateBarcodeAuthorisation())
      .headers(setSlmClientIp())
      .bodyValue(
        """{ 
            "prisonerName": "John Smith",
            "prisonId": "BXI"
          }""",
      )
      .exchange()
      .expectStatus().isBadRequest
      .expectBody().jsonPath("$.errorCode.code").isEqualTo(MalformedRequest.code)

    verify(telemetryClient, times(0)).trackEvent(any(), any(), isNull())
  }

  @Test
  fun `bad request given fails validation for name format`() {
    webTestClient.post()
      .uri("/barcode")
      .accept(MediaType.APPLICATION_JSON)
      .contentType(MediaType.APPLICATION_JSON)
      .headers(setCreateBarcodeAuthorisation())
      .headers(setSlmClientIp())
      .bodyValue(
        """{ 
            "prisonerName": "<John Smith>",
            "prisonId": "BXI",
            "prisonNumber": "A1234BC"
          }""",
      )
      .exchange()
      .expectStatus().isBadRequest
      .expectBody().jsonPath("$.errorCode.code").isEqualTo(MalformedRequest.code)

    verify(telemetryClient, times(0)).trackEvent(any(), any(), isNull())
  }
}
