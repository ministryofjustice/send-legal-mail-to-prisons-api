package uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.barcode

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.mockito.ArgumentMatchers.anyString
import uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.config.Duplicate
import uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.config.Expired
import uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.config.RandomCheck
import uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.config.ValidationException
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.Optional
import javax.persistence.EntityNotFoundException

class BarcodeServiceTest {

  private val barcodeRepository = mock<BarcodeRepository>()
  private val barcodeEventService = mock<BarcodeEventService>()
  private val barcodeGeneratorService = mock<BarcodeGeneratorService>()
  private val barcodeService = BarcodeService(barcodeRepository, barcodeEventService, barcodeGeneratorService)

  @Nested
  inner class CreateBarcode {
    @Test
    fun `should create barcode and created event if ok`() {
      whenever(barcodeGeneratorService.generateBarcode()).thenReturn("SOME_BARCODE")
      mockFindBarcode(null)
      mockSaveBarcode()
      whenever(barcodeEventService.createEvent(any(), anyString(), any(), anyString())).thenReturn(
        BarcodeEvent(1L, aBarcode(), "some_user", BarcodeStatus.CREATED)
      )

      val code = barcodeService.createBarcode("some_user")

      assertThat(code).isEqualTo("SOME_BARCODE")
      verify(barcodeRepository).save(aBarcode())
      verify(barcodeEventService).createEvent(aBarcode(), "some_user", BarcodeStatus.CREATED, "")
    }
  }

  @Nested
  inner class CheckBarcodeOk {
    @Test
    fun `should complete ok and create checked event if barcode exists`() {
      mockFindBarcode()

      assertDoesNotThrow {
        barcodeService.checkBarcode("some_user", "SOME_BARCODE", "some_location")
      }

      verify(barcodeEventService)
        .createEvent(aBarcode(), "some_user", BarcodeStatus.CHECKED, "some_location")
    }
  }

  @Nested
  inner class CheckBarcodeNotFound {
    @Test
    fun `should throw not found and create barcode checked event if barcode does not exist`() {
      val expectedException = EntityNotFoundException("some_message")
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
      val expectedException = ValidationException(Duplicate(yesterday, "previous_location"))
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
      val expectedException = ValidationException(Expired(expired, barcodeExpiryDays))
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
      val expectedException = ValidationException(RandomCheck)
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
