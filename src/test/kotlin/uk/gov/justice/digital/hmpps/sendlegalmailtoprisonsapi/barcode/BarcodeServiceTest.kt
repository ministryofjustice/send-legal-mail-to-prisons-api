package uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.barcode

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyString
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.config.Duplicate
import uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.config.Expired
import uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.config.RandomCheck
import uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.config.ResourceNotFoundException
import uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.config.ValidationException
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.Optional

class BarcodeServiceTest {

  private val barcodeRepository = mock<BarcodeRepository>()
  private val barcodeEventService = mock<BarcodeEventService>()
  private val barcodeGeneratorService = mock<BarcodeGeneratorService>()
  private val barcodeRecipientService = mock<BarcodeRecipientService>()
  private val barcodeService = BarcodeService(barcodeRepository, barcodeEventService, barcodeGeneratorService, barcodeRecipientService)

  @Nested
  inner class CreateBarcode {
    @Test
    fun `should create barcode and created event and barcode recipient if ok`() {
      whenever(barcodeGeneratorService.generateBarcode()).thenReturn("SOME_BARCODE")
      mockFindBarcode(null)
      mockSaveBarcode()
      whenever(barcodeEventService.createEvent(any(), anyString(), any(), anyString())).thenReturn(
        BarcodeEvent(1L, aBarcode(), "some_user", BarcodeStatus.CREATED)
      )
      val createBarcodeRequest = CreateBarcodeRequest(prisonerName = "Fred Bloggs", prisonId = "BXI", prisonNumber = "A1234BC")

      val code = barcodeService.createBarcode("some_user", createBarcodeRequest)

      assertThat(code).isEqualTo("SOME_BARCODE")
      verify(barcodeRepository).save(aBarcode())
      verify(barcodeEventService).createEvent(aBarcode(), "some_user", BarcodeStatus.CREATED, "")
      verify(barcodeRecipientService).saveBarcodeRecipient(aBarcode(), createBarcodeRequest)
    }
  }

  @Nested
  inner class CheckBarcodeOk {
    @Test
    fun `should complete ok and create checked event if barcode exists`() {
      mockFindBarcode()
      whenever(barcodeEventService.getCreatedBy(aBarcode())).thenReturn("some_sender")

      val createdBy = barcodeService.checkBarcode("some_user", "SOME_BARCODE", "some_location")

      assertThat(createdBy).isEqualTo("some_sender")
      verify(barcodeEventService)
        .createEvent(aBarcode(), "some_user", BarcodeStatus.CHECKED, "some_location")
    }
  }

  @Nested
  inner class CheckBarcodeNotFound {
    @Test
    fun `should throw not found and create barcode checked event if barcode does not exist`() {
      val expectedException = ResourceNotFoundException("some_message")
      mockFindBarcode(null)
      mockSaveBarcode()
      whenever(barcodeEventService.checkForCreated(aBarcode())).thenThrow(expectedException)

      assertThatThrownBy { barcodeService.checkBarcode("some_user", "SOME_BARCODE", "some_location") }
        .isEqualTo(expectedException)

      verify(barcodeEventService)
        .createEvent(aBarcode(), "some_user", BarcodeStatus.CHECKED, "some_location")
    }
  }

  @Nested
  inner class CheckBarcodeDuplicate {
    @Test
    fun `should throw validation exception and create checked event if duplicate`() {
      val yesterday = Instant.now().minus(1, ChronoUnit.DAYS)
      val expectedException = ValidationException(Duplicate(yesterday, "previous_location", "some_sender"))
      mockFindBarcode()
      whenever(barcodeEventService.checkForDuplicate(aBarcode(), "current_user", "current_location"))
        .thenThrow(expectedException)

      assertThatThrownBy { barcodeService.checkBarcode("current_user", "SOME_BARCODE", "current_location") }
        .isEqualTo(expectedException)

      verify(barcodeEventService)
        .createEvent(aBarcode(), "current_user", BarcodeStatus.CHECKED, "current_location")
    }
  }

  @Nested
  inner class CheckBarcodeExpired {
    private val barcodeExpiryDays = 28L

    @Test
    fun `should throw validation exception and create checked event if duplicate`() {
      val expired = Instant.now().minus(barcodeExpiryDays + 1, ChronoUnit.DAYS)
      val expectedException = ValidationException(Expired(expired, barcodeExpiryDays, "some_sender"))
      mockFindBarcode()
      whenever(barcodeEventService.checkForExpired(aBarcode(), "current_user", "current_location"))
        .thenThrow(expectedException)

      assertThatThrownBy { barcodeService.checkBarcode("current_user", "SOME_BARCODE", "current_location") }
        .isEqualTo(expectedException)

      verify(barcodeEventService)
        .createEvent(aBarcode(), "current_user", BarcodeStatus.CHECKED, "current_location")
    }
  }

  @Nested
  inner class CheckBarcodeRandomSecurityCheck {
    @Test
    fun `should throw validation exception and create checked event if duplicate`() {
      val expectedException = ValidationException(RandomCheck("some_sender"))
      mockFindBarcode()
      whenever(barcodeEventService.checkForRandomSecurityCheck(aBarcode(), "current_user", "current_location"))
        .thenThrow(expectedException)

      assertThatThrownBy { barcodeService.checkBarcode("current_user", "SOME_BARCODE", "current_location") }
        .isEqualTo(expectedException)

      verify(barcodeEventService)
        .createEvent(aBarcode(), "current_user", BarcodeStatus.CHECKED, "current_location")
    }
  }

  // Test helpers
  private fun aBarcode() = Barcode("SOME_BARCODE")

  private fun mockFindBarcode(barcode: Barcode? = aBarcode()) =
    whenever(barcodeRepository.findById("SOME_BARCODE")).thenReturn(Optional.ofNullable(barcode))

  private fun mockSaveBarcode() =
    whenever(barcodeRepository.save(any())).thenReturn(aBarcode())
}
