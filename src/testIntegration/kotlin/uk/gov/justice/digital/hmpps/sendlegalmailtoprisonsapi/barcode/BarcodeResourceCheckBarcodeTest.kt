package uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.barcode

import org.assertj.core.api.Assertions
import org.awaitility.kotlin.await
import org.awaitility.kotlin.until
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.whenever
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.BodyInserters
import uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.cjsm.CjsmDirectoryEntry
import uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.config.AuthenticationError
import uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.config.MalformedRequest
import uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.config.NotFound
import uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.mocks.PrisonerSearchExtension
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Nested
class BarcodeResourceCheckBarcodeTest : BarcodeResourceTest() {
  @BeforeEach
  fun `add the user's organisation to the CJSM directory`() {
    cjsmDirectoryRepository.save(CjsmDirectoryEntry(1L, "some.user@company.com.cjsm.net", "anyfirstname", "anylastname", "Some Company", "Anytown", "Any type"))
  }

  @BeforeEach
  fun `mock prisoner search response`() {
    PrisonerSearchExtension.prisonerSearchApi.stubMatchPrisoners()
    PrisonerSearchExtension.prisonerSearchApi.stubGlobalSearch()
  }

  @Test
  fun `unauthorised without a valid auth token`() {
    webTestClient.post()
      .uri("/barcode/check")
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
      .uri("/barcode/check")
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
      .uri("/barcode/check")
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
      .uri("/barcode/check")
      .accept(MediaType.APPLICATION_JSON)
      .contentType(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(user = "any.user@domain.com", roles = listOf("ROLE_SLM_SCAN_BARCODE")))
      .headers(setSlmClientIp())
      .exchange()
      .expectStatus().isBadRequest
      .expectBody().jsonPath("$.errorCode.code").isEqualTo(MalformedRequest.code)
  }

  @Test
  fun `not found if barcode does not exist`() {
    webTestClient.post()
      .uri("/barcode/check")
      .accept(MediaType.APPLICATION_JSON)
      .contentType(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(user = "AUSER_GEN", roles = listOf("ROLE_SLM_SCAN_BARCODE")))
      .headers(setSlmClientIp())
      .body(BodyInserters.fromValue("""{ "barcode": "doesnt-exist" }"""))
      .exchange()
      .expectStatus().isNotFound
      .expectBody().jsonPath("$.errorCode.code").isEqualTo(NotFound.code)
  }

  @Test
  fun `OK if barcode exists`() {
    whenever(barcodeGeneratorService.generateBarcode()).thenReturn("SOME_BARCODE")

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
      .uri("/barcode/check")
      .accept(MediaType.APPLICATION_JSON)
      .contentType(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(user = "AUSER_GEN", roles = listOf("ROLE_SLM_SCAN_BARCODE")))
      .headers(setSlmClientIp())
      .body(BodyInserters.fromValue("""{ "barcode": "SOME_BARCODE" }"""))
      .exchange()
      .expectStatus().isOk
      .expectBody().jsonPath("$.createdBy").isEqualTo("Some Company")

    val barcode = Barcode("SOME_BARCODE")
    assertBarcodeEventCreated(barcode, BarcodeEventType.CHECKED)
    with(PrisonerSearchExtension.prisonerSearchApi) {
      await until { matchPrisonersHasBeenCalled() && globalSearchHasBeenCalled() }
    }
  }

  @Test
  fun `OK if barcode exists but no CJSM organisation in the directory`() {
    whenever(barcodeGeneratorService.generateBarcode()).thenReturn("SOME_BARCODE")
    cjsmDirectoryRepository.deleteAll()

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
      .uri("/barcode/check")
      .accept(MediaType.APPLICATION_JSON)
      .contentType(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(user = "AUSER_GEN", roles = listOf("ROLE_SLM_SCAN_BARCODE")))
      .headers(setSlmClientIp())
      .body(BodyInserters.fromValue("""{ "barcode": "SOME_BARCODE" }"""))
      .exchange()
      .expectStatus().isOk
      .expectBody().jsonPath("$.createdBy").isEqualTo("some.user@company.com.cjsm.net")

    with(PrisonerSearchExtension.prisonerSearchApi) {
      await until { matchPrisonersHasBeenCalled() && globalSearchHasBeenCalled() }
    }
  }

  @Test
  fun `Bad request duplicate if barcode already checked`() {
    whenever(barcodeGeneratorService.generateBarcode()).thenReturn("SOME_BARCODE")

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
      .uri("/barcode/check")
      .accept(MediaType.APPLICATION_JSON)
      .contentType(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(user = "AUSER_GEN", roles = listOf("ROLE_SLM_SCAN_BARCODE")))
      .headers(setSlmClientIp())
      .body(BodyInserters.fromValue("""{ "barcode": "SOME_BARCODE" }"""))
      .exchange()
      .expectStatus().isOk

    val today = DateTimeFormatter.ofPattern("yyyy-MM-dd").format(Instant.now().atZone(ZoneId.systemDefault()))
    webTestClient.post()
      .uri("/barcode/check")
      .accept(MediaType.APPLICATION_JSON)
      .contentType(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(user = "AUSER_GEN", roles = listOf("ROLE_SLM_SCAN_BARCODE")))
      .headers(setSlmClientIp())
      .body(BodyInserters.fromValue("""{ "barcode": "SOME_BARCODE" }"""))
      .exchange()
      .expectStatus().isBadRequest
      .expectBody()
      .jsonPath("$.errorCode.code").isEqualTo("DUPLICATE")
      .jsonPath("$.errorCode.scannedDate").value<String> { Assertions.assertThat(it).contains(today) }
      .jsonPath("$.errorCode.scannedLocation").isEqualTo("LEI")
      .jsonPath("$.errorCode.createdBy").isEqualTo("Some Company")
      .jsonPath("$.errorCode.recipientName").isEqualTo("Fred Bloggs")
      .jsonPath("$.errorCode.recipientPrisonNumber").isEqualTo("A1234BC")
      .jsonPath("$.errorCode.recipientDob").doesNotExist()

    val barcode = Barcode("SOME_BARCODE")
    assertBarcodeEventCreated(barcode, BarcodeEventType.DUPLICATE)
    with(PrisonerSearchExtension.prisonerSearchApi) {
      await until { matchPrisonersHasBeenCalled() && globalSearchHasBeenCalled() }
    }
  }

  @Test
  fun `Bad request expired if barcode created before expiry period`() {
    whenever(barcodeConfig.expiry).thenReturn(Duration.ZERO)
    whenever(barcodeGeneratorService.generateBarcode()).thenReturn("SOME_BARCODE")

    webTestClient.post()
      .uri("/barcode")
      .accept(MediaType.APPLICATION_JSON)
      .contentType(MediaType.APPLICATION_JSON)
      .headers(setCreateBarcodeAuthorisation())
      .headers(setSlmClientIp())
      .bodyValue(aCreateBarcodeRequest())
      .exchange()
      .expectStatus().isCreated

    val expiredDayString = DateTimeFormatter.ofPattern("yyyy-MM-dd").format(Instant.now().atZone(ZoneId.systemDefault()))
    webTestClient.post()
      .uri("/barcode/check")
      .accept(MediaType.APPLICATION_JSON)
      .contentType(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(user = "AUSER_GEN", roles = listOf("ROLE_SLM_SCAN_BARCODE")))
      .headers(setSlmClientIp())
      .body(BodyInserters.fromValue("""{ "barcode": "SOME_BARCODE" }"""))
      .exchange()
      .expectStatus().isBadRequest
      .expectBody()
      .jsonPath("$.errorCode.code").isEqualTo("EXPIRED")
      .jsonPath("$.errorCode.barcodeExpiryDays").isEqualTo("0")
      .jsonPath("$.errorCode.createdDate").value<String> { Assertions.assertThat(it).contains(expiredDayString) }
      .jsonPath("$.errorCode.createdBy").isEqualTo("Some Company")

    val barcode = Barcode("SOME_BARCODE")
    assertBarcodeEventCreated(barcode, BarcodeEventType.EXPIRED)
    with(PrisonerSearchExtension.prisonerSearchApi) {
      await until { matchPrisonersHasBeenCalled() && globalSearchHasBeenCalled() }
    }
  }

  @Test
  fun `Bad request with random check if barcode has been selected for a random check`() {
    whenever(randomCheckService.requiresRandomCheck()).thenReturn(true)
    whenever(barcodeGeneratorService.generateBarcode()).thenReturn("SOME_BARCODE")

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
      .uri("/barcode/check")
      .accept(MediaType.APPLICATION_JSON)
      .contentType(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(user = "AUSER_GEN", roles = listOf("ROLE_SLM_SCAN_BARCODE")))
      .headers(setSlmClientIp())
      .body(BodyInserters.fromValue("""{ "barcode": "SOME_BARCODE" }"""))
      .exchange()
      .expectStatus().isBadRequest
      .expectBody()
      .jsonPath("$.errorCode.code").isEqualTo("RANDOM_CHECK")
      .jsonPath("$.errorCode.createdBy").isEqualTo("Some Company")

    val barcode = Barcode("SOME_BARCODE")
    assertBarcodeEventCreated(barcode, BarcodeEventType.RANDOM_CHECK)
    with(PrisonerSearchExtension.prisonerSearchApi) {
      await until { matchPrisonersHasBeenCalled() && globalSearchHasBeenCalled() }
    }
  }
}
