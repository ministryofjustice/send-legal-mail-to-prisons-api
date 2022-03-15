package uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.security

import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.given
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.BodyInserters
import uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.barcode.Barcode
import uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.barcode.BarcodeEvent
import uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.barcode.BarcodeEventType
import uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.barcode.BarcodeResourceTest
import uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.magiclink.VerifyLinkResponse
import uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.mocks.HmppsAuthExtension

class SmoketestTest : BarcodeResourceTest() {

  @Nested
  inner class SmokeTestAllowed {

    @BeforeEach
    fun `smoke test configuration`() {
      given(smokeTestConfig.lsjSecret).willReturn("some-lsj-secret")
      given(smokeTestConfig.msjSecret).willReturn("some-msj-secret")
    }

    @Test
    fun `A legal sender smoke test can sign in using the back door`() {
      val jwt = webTestClient.post()
        .uri("/link/verify")
        .accept(MediaType.APPLICATION_JSON)
        .contentType(MediaType.APPLICATION_JSON)
        .headers(setAuthorisation(user = null))
        .body(BodyInserters.fromValue("""{ "secret": "some-lsj-secret" }"""))
        .exchange()
        .expectStatus().isCreated
        .returnResult(VerifyLinkResponse::class.java)
        .responseBody
        .blockFirst()
        ?: Assertions.fail("Did not receive a response from /link/verify")

      assertThat(jwtService.validateToken(jwt.token)).isTrue
      assertThat(jwtService.subject(jwt.token)).isEqualTo("smoke-test-lsj")
      assertThat(jwtService.authorities(jwt.token)).containsExactly("ROLE_SLM_CREATE_BARCODE")
    }

    @Test
    fun `A mailroom staff smoke test can check barcodes without an auth error`() {
      val barcode = barcodeRepository.save(Barcode(code = "some-barcode"))
      barcodeEventRepository.save(BarcodeEvent(barcode = barcode, userId = "any-user", eventType = BarcodeEventType.CREATED, ipAddress = "127.0.0.1"))

      webTestClient.post()
        .uri("/barcode/check")
        .accept(MediaType.APPLICATION_JSON)
        .contentType(MediaType.APPLICATION_JSON)
        .headers(setAuthorisation(user = "some-msj-secret", roles = listOf("ROLE_SLM_SCAN_BARCODE")))
        .body(BodyInserters.fromValue("""{ "barcode": "some-barcode" }"""))
        .exchange()
        .expectStatus().isOk

      val checkedEvent = barcodeEventRepository.findByBarcodeAndEventTypeOrderByCreatedDateTime(barcode, BarcodeEventType.CHECKED).first()

      assertThat(checkedEvent.location).isEqualTo("SKI") // LEI is stubbed by Auth but SKI is hardcoded for smoke tests
    }
  }

  @Nested
  inner class SmokeTestDisallowed {

    @Test
    fun `A legal sender smoke test cannot sign in using the back door`() {
      webTestClient.post()
        .uri("/link/verify")
        .accept(MediaType.APPLICATION_JSON)
        .contentType(MediaType.APPLICATION_JSON)
        .headers(setAuthorisation(user = null))
        .body(BodyInserters.fromValue("""{ "secret": "some-lsj-secret" }"""))
        .exchange()
        .expectStatus().isNotFound
    }

    @Test
    fun `A mailroom staff smoke test checking barcodes receives an error because the user doesn't exist`() {
      HmppsAuthExtension.hmppsAuthApi.stubFailToGetUserDetails()
      val barcode = barcodeRepository.save(Barcode(code = "no-smoketest"))
      barcodeEventRepository.save(BarcodeEvent(barcode = barcode, userId = "any-user", eventType = BarcodeEventType.CREATED, ipAddress = "127.0.0.1"))

      webTestClient.post()
        .uri("/barcode/check")
        .accept(MediaType.APPLICATION_JSON)
        .contentType(MediaType.APPLICATION_JSON)
        .headers(setAuthorisation(user = "some-msj-user", roles = listOf("ROLE_SLM_SCAN_BARCODE")))
        .body(BodyInserters.fromValue("""{ "barcode": "no-smoketest" }"""))
        .exchange()
        .expectStatus().is5xxServerError

      val checkedEvents = barcodeEventRepository.findByBarcodeAndEventTypeOrderByCreatedDateTime(barcode, BarcodeEventType.CHECKED)

      assertThat(checkedEvents).isEmpty()
    }
  }
}
