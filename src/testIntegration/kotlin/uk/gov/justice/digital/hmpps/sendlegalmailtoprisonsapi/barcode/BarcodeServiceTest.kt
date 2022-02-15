package uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.barcode

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatExceptionOfType
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.given
import org.springframework.dao.CannotAcquireLockException
import uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.IntegrationTest

class BarcodeServiceTest : IntegrationTest() {

  @Nested
  inner class CreateBarcode {
    @Test
    fun `should create barcode, barcode event, and barcode recipient`() {
      val createBarcodeRequest = CreateBarcodeRequest(prisonerName = "Fred Bloggs", prisonId = "BXI", prisonNumber = "A1234BC")
      given { barcodeGeneratorService.generateBarcode() }.willReturn("SOME_CODE")

      barcodeService.createBarcode("a-user@cjsm.net", createBarcodeRequest)

      val barcode = barcodeRepository.getById("SOME_CODE")
      assertThat(barcode).isNotNull
      assertThat(barcodeEventRepository.findByBarcodeAndEventTypeCreated(barcode)).isNotNull
      assertThat(barcodeRecipientRepository.getByBarcode(barcode)).isNotNull
    }

    @Test
    fun `should rollback given creation of barcode record fails`() {
      val createBarcodeRequest = CreateBarcodeRequest(prisonerName = "Fred Bloggs", prisonId = "BXI", prisonNumber = "A1234BC")
      given { barcodeGeneratorService.generateBarcode() }.willReturn("SOME_CODE")
      doThrow(CannotAcquireLockException("some exception saving to the database")).`when`(barcodeRepository).save(any())

      assertThatExceptionOfType(CannotAcquireLockException::class.java).isThrownBy {
        barcodeService.createBarcode("a-user@cjsm.net", createBarcodeRequest)
      }
      assertThat(barcodeRepository.findById("SOME_CODE")).isNotPresent
      assertThat(barcodeEventRepository.findAll()).isEmpty()
      assertThat(barcodeRecipientRepository.findAll()).isEmpty()
    }

    @Test
    fun `should rollback given creation of barcode event fails`() {
      val createBarcodeRequest = CreateBarcodeRequest(prisonerName = "Fred Bloggs", prisonId = "BXI", prisonNumber = "A1234BC")
      given { barcodeGeneratorService.generateBarcode() }.willReturn("SOME_CODE")
      doThrow(CannotAcquireLockException("some exception saving to the database")).`when`(barcodeEventRepository).save(any())

      assertThatExceptionOfType(CannotAcquireLockException::class.java).isThrownBy {
        barcodeService.createBarcode("a-user@cjsm.net", createBarcodeRequest)
      }
      assertThat(barcodeRepository.findById("SOME_CODE")).isNotPresent
      assertThat(barcodeEventRepository.findAll()).isEmpty()
      assertThat(barcodeRecipientRepository.findAll()).isEmpty()
    }

    @Test
    fun `should rollback given creation of barcode recipient fails`() {
      val createBarcodeRequest = CreateBarcodeRequest(prisonerName = "Fred Bloggs", prisonId = "BXI", prisonNumber = "A1234BC")
      given { barcodeGeneratorService.generateBarcode() }.willReturn("SOME_CODE")
      doThrow(CannotAcquireLockException("some exception saving to the database")).`when`(barcodeRecipientRepository).save(any())

      assertThatExceptionOfType(CannotAcquireLockException::class.java).isThrownBy {
        barcodeService.createBarcode("a-user@cjsm.net", createBarcodeRequest)
      }
      assertThat(barcodeRepository.findById("SOME_CODE")).isNotPresent
      assertThat(barcodeEventRepository.findAll()).isEmpty()
      assertThat(barcodeRecipientRepository.findAll()).isEmpty()
    }
  }
}
