package uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.barcode

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyString
import uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.config.Duplicate
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
      whenever(barcodeRepository.findById("SOME_BARCODE")).thenReturn(Optional.empty())
      whenever(barcodeRepository.save(any())).thenReturn(Barcode("SOME_BARCODE"))
      whenever(barcodeEventService.createEvent(any(), anyString(), any(), anyString())).thenReturn(
        BarcodeEvent(1L, Barcode("SOME_BARCODE"), "some_user", BarcodeStatus.CREATED, Instant.now())
      )

      val code = barcodeService.createBarcode("some_user")

      assertThat(code).isEqualTo("SOME_BARCODE")
      verify(barcodeRepository).save(Barcode("SOME_BARCODE"))
      verify(barcodeEventService).createEvent(Barcode("SOME_BARCODE"), "some_user", BarcodeStatus.CREATED, "")
    }
  }

  @Nested
  inner class CheckBarcodeOk {
    @Test
    fun `should complete ok and create checked event if barcode exists`() {
      whenever(barcodeRepository.findById("SOME_BARCODE")).thenReturn(Optional.of(Barcode("SOME_BARCODE")))

      barcodeService.checkBarcode("some_user", "SOME_BARCODE", "some_location")

      verify(barcodeEventService).createEvent(Barcode("SOME_BARCODE"), "some_user", BarcodeStatus.CHECKED, "some_location")
    }
  }

  @Nested
  inner class CheckBarcodeNotFound {
    @Test
    fun `should throw not found and create barcode checked event if barcode does not exist`() {
      val expectedException = EntityNotFoundException("some_message")
      whenever(barcodeRepository.findById("SOME_BARCODE")).thenReturn(Optional.empty())
      whenever(barcodeRepository.save(any())).thenReturn(Barcode("SOME_BARCODE"))
      whenever(barcodeEventService.checkForCreated(Barcode("SOME_BARCODE")))
        .thenThrow(expectedException)

      assertThatThrownBy { barcodeService.checkBarcode("some_user", "SOME_BARCODE", "some_location") }
        .isEqualTo(expectedException)

      verify(barcodeEventService)
        .createEvent(Barcode("SOME_BARCODE"), "some_user", BarcodeStatus.CHECKED, "some_location")
    }
  }

  @Nested
  inner class CheckBarcodeDuplicate {
    @Test
    fun `should throw validation exception and create checked event if duplicate`() {
      val expectedException = ValidationException(Duplicate(Instant.now().minus(1, ChronoUnit.DAYS), "previous_location"))
      whenever(barcodeRepository.findById("SOME_BARCODE")).thenReturn(Optional.of(Barcode("SOME_BARCODE")))
      whenever(barcodeEventService.checkForDuplicate(Barcode("SOME_BARCODE"), "current_user", "current_location"))
        .thenThrow(expectedException)

      assertThatThrownBy { barcodeService.checkBarcode("current_user", "SOME_BARCODE", "current_location") }
        .isEqualTo(expectedException)

      verify(barcodeEventService)
        .createEvent(Barcode("SOME_BARCODE"), "current_user", BarcodeStatus.CHECKED, "current_location")
    }
  }
}
