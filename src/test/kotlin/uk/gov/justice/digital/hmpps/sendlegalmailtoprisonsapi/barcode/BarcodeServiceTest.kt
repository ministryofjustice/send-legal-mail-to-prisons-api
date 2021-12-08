package uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.barcode

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.check
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.config.Duplicate
import uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.config.ValidationException
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Optional
import javax.persistence.EntityNotFoundException

class BarcodeServiceTest {

  private val barcodeRepository = mock<BarcodeRepository>()
  private val barcodeEventRepository = mock<BarcodeEventRepository>()
  private val barcodeGeneratorService = mock<BarcodeGeneratorService>()
  private val barcodeService = BarcodeService(barcodeRepository, barcodeEventRepository, barcodeGeneratorService)

  @Nested
  inner class CreateBarcode {
    @Test
    fun `should create barcode and created event if ok`() {
      whenever(barcodeGeneratorService.generateBarcode()).thenReturn("SOME_BARCODE")
      whenever(barcodeRepository.findById("SOME_BARCODE")).thenReturn(Optional.empty())
      whenever(barcodeRepository.save(any())).thenReturn(Barcode("SOME_BARCODE"))
      whenever(barcodeEventRepository.save(any())).thenReturn(
        BarcodeEvent(1L, Barcode("SOME_BARCODE"), "some_user", BarcodeStatus.CREATED, Instant.now())
      )

      val code = barcodeService.createBarcode("some_user")

      assertThat(code).isEqualTo("SOME_BARCODE")
      verify(barcodeRepository).save(Barcode("SOME_BARCODE"))
      verify(barcodeEventRepository).save(
        check {
          assertThat(it.barcode).isEqualTo(Barcode("SOME_BARCODE"))
          assertThat(it.userId).isEqualTo("some_user")
          assertThat(it.status).isEqualTo(BarcodeStatus.CREATED)
        }
      )
    }
  }

  @Nested
  inner class CheckBarcodeOk {
    @Test
    fun `should complete ok and create checked event if barcode exists`() {
      whenever(barcodeRepository.findById("SOME_BARCODE")).thenReturn(Optional.of(Barcode("SOME_BARCODE")))
      whenever(barcodeEventRepository.findByBarcodeAndStatusOrderByCreatedDateTime(Barcode("SOME_BARCODE"), BarcodeStatus.CREATED))
        .thenReturn(
          listOf(BarcodeEvent(1L, Barcode("SOME_BARCODE"), "some_user", BarcodeStatus.CREATED, Instant.now()))
        )

      barcodeService.checkBarcode("some_user", "SOME_BARCODE", "some_location")

      verify(barcodeEventRepository).save(
        check {
          assertThat(it.barcode.code).isEqualTo("SOME_BARCODE")
          assertThat(it.userId).isEqualTo("some_user")
          assertThat(it.status).isEqualTo(BarcodeStatus.CHECKED)
          assertThat(it.location).isEqualTo("some_location")
        }
      )
    }
  }

  @Nested
  inner class CheckBarcodeNotFound {
    @Test
    fun `should throw not found and create barcode checked event if barcode does not exist`() {
      whenever(barcodeRepository.findById("SOME_BARCODE")).thenReturn(Optional.empty())
      whenever(barcodeRepository.save(any())).thenReturn(Barcode("SOME_BARCODE"))

      assertThatThrownBy { barcodeService.checkBarcode("some_user", "SOME_BARCODE", "some_location") }
        .isInstanceOf(EntityNotFoundException::class.java)

      verify(barcodeEventRepository).save(
        check {
          assertThat(it.barcode.code).isEqualTo("SOME_BARCODE")
          assertThat(it.userId).isEqualTo("some_user")
          assertThat(it.status).isEqualTo(BarcodeStatus.CHECKED)
        }
      )
    }

    @Test
    fun `should throw not found for every check if the barcode wasn't created by us`() {
      whenever(barcodeRepository.findById("SOME_BARCODE"))
        .thenReturn(Optional.empty())
        .thenReturn(Optional.of(Barcode("SOME_BARCODE")))
      whenever(barcodeRepository.save(any())).thenReturn(Barcode("SOME_BARCODE"))

      repeat(3) {
        assertThatThrownBy { barcodeService.checkBarcode("some_user", "SOME_BARCODE", "some_location") }
          .isInstanceOf(EntityNotFoundException::class.java)
      }

      verify(barcodeEventRepository, times(3)).save(
        check {
          assertThat(it.barcode.code).isEqualTo("SOME_BARCODE")
          assertThat(it.userId).isEqualTo("some_user")
          assertThat(it.status).isEqualTo(BarcodeStatus.CHECKED)
        }
      )
    }
  }

  @Nested
  inner class CheckBarcodeDuplicate {
    @Test
    fun `should throw validation exception and create checked and duplicate events`() {
      whenever(barcodeRepository.findById("SOME_BARCODE")).thenReturn(Optional.of(Barcode("SOME_BARCODE")))
      whenever(barcodeEventRepository.findByBarcodeAndStatusOrderByCreatedDateTime(Barcode("SOME_BARCODE"), BarcodeStatus.CREATED))
        .thenReturn(
          listOf(BarcodeEvent(1L, Barcode("SOME_BARCODE"), "some_user", BarcodeStatus.CREATED, Instant.now().minus(2, ChronoUnit.DAYS)))
        )
      whenever(barcodeEventRepository.findByBarcodeAndStatusOrderByCreatedDateTime(Barcode("SOME_BARCODE"), BarcodeStatus.CHECKED))
        .thenReturn(
          listOf(
            BarcodeEvent(2L, Barcode("SOME_BARCODE"), "some_user", BarcodeStatus.CHECKED, Instant.now().minus(1, ChronoUnit.DAYS), "prev_location"),
            BarcodeEvent(3L, Barcode("SOME_BARCODE"), "some_user", BarcodeStatus.CHECKED, Instant.now(), "current_location"),
          )
        )

      assertThatThrownBy { barcodeService.checkBarcode("some_user", "SOME_BARCODE", "current_location") }
        .isInstanceOf(ValidationException::class.java)
        .extracting {
          val error = (it as ValidationException).errorCode as Duplicate
          val yesterday = DateTimeFormatter.ofPattern("d MMMM y").withZone(ZoneId.systemDefault()).format(Instant.now().minus(1, ChronoUnit.DAYS))
          assertThat(error.userMessage).contains(yesterday)
          assertThat(error.userMessage).contains("prev_location")
        }

      verify(barcodeEventRepository).save(
        check {
          assertThat(it.barcode.code).isEqualTo("SOME_BARCODE")
          assertThat(it.userId).isEqualTo("some_user")
          assertThat(it.status).isEqualTo(BarcodeStatus.CHECKED)
          assertThat(it.location).isEqualTo("current_location")
        }
      )
      verify(barcodeEventRepository).save(
        check {
          assertThat(it.barcode.code).isEqualTo("SOME_BARCODE")
          assertThat(it.userId).isEqualTo("some_user")
          assertThat(it.status).isEqualTo(BarcodeStatus.DUPLICATE)
          assertThat(it.location).isEqualTo("current_location")
        }
      )
    }
  }
}
