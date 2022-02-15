package uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.barcode

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.whenever
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.BodyInserters
import uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.IntegrationTest
import uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.barcode.BarcodeEventType.CREATED
import uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.cjsm.CjsmDirectoryEntry
import uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.config.AuthenticationError
import uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.config.MalformedRequest
import uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.config.NotFound
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class BarcodeResourceTest : IntegrationTest() {

  @Nested
  inner class CreateBarcode {
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
      assertThat(barcodeEventRepository.findByBarcodeAndEventTypeOrderByCreatedDateTime(barcode, CREATED)).isNotEmpty
      assertThat(barcodeRecipientRepository.getByBarcode(barcode)).isNotNull
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

  @Nested
  inner class CheckBarcode {
    @BeforeEach
    fun `add the user's organisation to the CJSM directory`() {
      cjsmDirectoryRepository.save(CjsmDirectoryEntry(1L, "some.user@company.com.cjsm.net", "anyfirstname", "anylastname", "Some Company", "Anytown", "Any type"))
    }

    @Test
    fun `unauthorised without a valid auth token`() {
      webTestClient.post()
        .uri("/barcode/check")
        .accept(MediaType.APPLICATION_JSON)
        .contentType(MediaType.APPLICATION_JSON)
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
        .bodyValue(aCreateBarcodeRequest())
        .exchange()
        .expectStatus().isCreated

      webTestClient.post()
        .uri("/barcode/check")
        .accept(MediaType.APPLICATION_JSON)
        .contentType(MediaType.APPLICATION_JSON)
        .headers(setAuthorisation(user = "AUSER_GEN", roles = listOf("ROLE_SLM_SCAN_BARCODE")))
        .body(BodyInserters.fromValue("""{ "barcode": "SOME_BARCODE" }"""))
        .exchange()
        .expectStatus().isOk
        .expectBody().jsonPath("$.createdBy").isEqualTo("Some Company")
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
        .bodyValue(aCreateBarcodeRequest())
        .exchange()
        .expectStatus().isCreated

      webTestClient.post()
        .uri("/barcode/check")
        .accept(MediaType.APPLICATION_JSON)
        .contentType(MediaType.APPLICATION_JSON)
        .headers(setAuthorisation(user = "AUSER_GEN", roles = listOf("ROLE_SLM_SCAN_BARCODE")))
        .body(BodyInserters.fromValue("""{ "barcode": "SOME_BARCODE" }"""))
        .exchange()
        .expectStatus().isOk
        .expectBody().jsonPath("$.createdBy").isEqualTo("some.user@company.com.cjsm.net")
    }

    @Test
    fun `Bad request duplicate if barcode already checked`() {
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
        .uri("/barcode/check")
        .accept(MediaType.APPLICATION_JSON)
        .contentType(MediaType.APPLICATION_JSON)
        .headers(setAuthorisation(user = "AUSER_GEN", roles = listOf("ROLE_SLM_SCAN_BARCODE")))
        .body(BodyInserters.fromValue("""{ "barcode": "SOME_BARCODE" }"""))
        .exchange()
        .expectStatus().isOk

      val today = DateTimeFormatter.ISO_DATE.withZone(ZoneId.systemDefault()).format(Instant.now()).removeSuffix("Z")
      webTestClient.post()
        .uri("/barcode/check")
        .accept(MediaType.APPLICATION_JSON)
        .contentType(MediaType.APPLICATION_JSON)
        .headers(setAuthorisation(user = "AUSER_GEN", roles = listOf("ROLE_SLM_SCAN_BARCODE")))
        .body(BodyInserters.fromValue("""{ "barcode": "SOME_BARCODE" }"""))
        .exchange()
        .expectStatus().isBadRequest
        .expectBody()
        .jsonPath("$.errorCode.code").isEqualTo("DUPLICATE")
        .jsonPath("$.errorCode.scannedDate").value<String> { assertThat(it).contains(today) }
        .jsonPath("$.errorCode.scannedLocation").isEqualTo("LEI")
        .jsonPath("$.errorCode.createdBy").isEqualTo("Some Company")
        .jsonPath("$.errorCode.recipientName").isEqualTo("Fred Bloggs")
        .jsonPath("$.errorCode.recipientPrisonNumber").isEqualTo("A1234BC")
        .jsonPath("$.errorCode.recipientDob").doesNotExist()
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
        .bodyValue(aCreateBarcodeRequest())
        .exchange()
        .expectStatus().isCreated

      val expiredDayString = DateTimeFormatter.ISO_DATE.withZone(ZoneId.systemDefault()).format(Instant.now()).removeSuffix("Z")
      webTestClient.post()
        .uri("/barcode/check")
        .accept(MediaType.APPLICATION_JSON)
        .contentType(MediaType.APPLICATION_JSON)
        .headers(setAuthorisation(user = "AUSER_GEN", roles = listOf("ROLE_SLM_SCAN_BARCODE")))
        .body(BodyInserters.fromValue("""{ "barcode": "SOME_BARCODE" }"""))
        .exchange()
        .expectStatus().isBadRequest
        .expectBody()
        .jsonPath("$.errorCode.code").isEqualTo("EXPIRED")
        .jsonPath("$.errorCode.barcodeExpiryDays").isEqualTo("0")
        .jsonPath("$.errorCode.createdDate").value<String> { assertThat(it).contains(expiredDayString) }
        .jsonPath("$.errorCode.createdBy").isEqualTo("Some Company")
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
        .bodyValue(aCreateBarcodeRequest())
        .exchange()
        .expectStatus().isCreated

      webTestClient.post()
        .uri("/barcode/check")
        .accept(MediaType.APPLICATION_JSON)
        .contentType(MediaType.APPLICATION_JSON)
        .headers(setAuthorisation(user = "AUSER_GEN", roles = listOf("ROLE_SLM_SCAN_BARCODE")))
        .body(BodyInserters.fromValue("""{ "barcode": "SOME_BARCODE" }"""))
        .exchange()
        .expectStatus().isBadRequest
        .expectBody()
        .jsonPath("$.errorCode.code").isEqualTo("RANDOM_CHECK")
        .jsonPath("$.errorCode.createdBy").isEqualTo("Some Company")
    }
  }

  private fun aCreateBarcodeRequest(): CreateBarcodeRequest =
    CreateBarcodeRequest(prisonerName = "Fred Bloggs", prisonId = "BXI", prisonNumber = "A1234BC")
}
