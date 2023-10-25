package uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.barcode

import com.microsoft.applicationinsights.TelemetryClient
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatExceptionOfType
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.check
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.eq
import org.mockito.kotlin.given
import org.mockito.kotlin.isNull
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.springframework.boot.test.mock.mockito.SpyBean
import org.springframework.dao.CannotAcquireLockException
import uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.IntegrationTest

class BarcodeServiceTest : IntegrationTest() {
  @SpyBean
  private lateinit var telemetryClient: TelemetryClient

  @Nested
  inner class CreateBarcode {
    @Test
    fun `should create barcode, barcode event, and barcode recipient`() {
      val createBarcodeRequest = CreateBarcodeRequest(prisonerName = "Fred Bloggs", prisonId = "BXI", prisonNumber = "A1234BC")
      given { barcodeGeneratorService.generateBarcode() }.willReturn("SOME_CODE")

      barcodeService.createBarcode("a-user@cjsm.net", "127.0.0.1", createBarcodeRequest)

      val barcode = barcodeRepository.getById("SOME_CODE")
      assertThat(barcode).isNotNull
      assertThat(barcodeEventRepository.findByBarcodeAndEventTypeCreated(barcode)).isNotNull
      assertThat(barcodeRecipientRepository.getByBarcode(barcode)).isNotNull

      verify(telemetryClient).trackEvent(
        eq("barcode-created"),
        check {
          assertThat(it["establishment"]).isEqualTo(createBarcodeRequest.prisonId)
          assertThat(it["prisonNumber"]).isEqualTo(createBarcodeRequest.prisonNumber)
          assertThat(it["barcodeNumber"]).isEqualTo(barcode.code)
          assertThat(it["sender"]).isEqualTo("a-user@cjsm.net")
        },
        isNull(),
      )
      verify(telemetryClient, times(1)).trackEvent(eq("barcode-created"), any(), isNull())
    }

    @Test
    fun `should rollback given creation of barcode record fails`() {
      val createBarcodeRequest = CreateBarcodeRequest(prisonerName = "Fred Bloggs", prisonId = "BXI", prisonNumber = "A1234BC")
      given { barcodeGeneratorService.generateBarcode() }.willReturn("SOME_CODE")
      doThrow(CannotAcquireLockException("some exception saving to the database")).`when`(barcodeRepository).save(any())

      assertThatExceptionOfType(CannotAcquireLockException::class.java).isThrownBy {
        barcodeService.createBarcode("a-user@cjsm.net", "127.0.0.1", createBarcodeRequest)
      }
      assertThat(barcodeRepository.findById("SOME_CODE")).isNotPresent
      assertThat(barcodeEventRepository.findAll()).isEmpty()
      assertThat(barcodeRecipientRepository.findAll()).isEmpty()
      verify(telemetryClient, times(0)).trackEvent(any(), any(), isNull())
    }

    @Test
    fun `should rollback given creation of barcode event fails`() {
      val createBarcodeRequest = CreateBarcodeRequest(prisonerName = "Fred Bloggs", prisonId = "BXI", prisonNumber = "A1234BC")
      given { barcodeGeneratorService.generateBarcode() }.willReturn("SOME_CODE")
      doThrow(CannotAcquireLockException("some exception saving to the database")).`when`(barcodeEventRepository).save(any())

      assertThatExceptionOfType(CannotAcquireLockException::class.java).isThrownBy {
        barcodeService.createBarcode("a-user@cjsm.net", "127.0.0.1", createBarcodeRequest)
      }
      assertThat(barcodeRepository.findById("SOME_CODE")).isNotPresent
      assertThat(barcodeEventRepository.findAll()).isEmpty()
      assertThat(barcodeRecipientRepository.findAll()).isEmpty()
      verify(telemetryClient, times(0)).trackEvent(any(), any(), isNull())
    }

    @Test
    fun `should rollback given creation of barcode recipient fails`() {
      val createBarcodeRequest = CreateBarcodeRequest(prisonerName = "Fred Bloggs", prisonId = "BXI", prisonNumber = "A1234BC")
      given { barcodeGeneratorService.generateBarcode() }.willReturn("SOME_CODE")
      doThrow(CannotAcquireLockException("some exception saving to the database")).`when`(barcodeRecipientRepository).save(any())

      assertThatExceptionOfType(CannotAcquireLockException::class.java).isThrownBy {
        barcodeService.createBarcode("a-user@cjsm.net", "127.0.0.1", createBarcodeRequest)
      }
      assertThat(barcodeRepository.findById("SOME_CODE")).isNotPresent
      assertThat(barcodeEventRepository.findAll()).isEmpty()
      assertThat(barcodeRecipientRepository.findAll()).isEmpty()
    }
  }
}
