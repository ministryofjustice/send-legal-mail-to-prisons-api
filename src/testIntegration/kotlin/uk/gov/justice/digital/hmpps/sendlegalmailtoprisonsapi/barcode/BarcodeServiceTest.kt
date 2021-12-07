package uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.barcode

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.check
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.Instant
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
    fun `should create barcode and barcode created event if ok`() {
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
  inner class CheckBarcode {
    @Test
    fun `should complete ok and create barcode checked event if barcode exists`() {
      whenever(barcodeRepository.findById("SOME_BARCODE")).thenReturn(Optional.of(Barcode("SOME_BARCODE")))
      whenever(barcodeEventRepository.findByBarcodeAndStatus(Barcode("SOME_BARCODE"), BarcodeStatus.CREATED))
        .thenReturn(listOf(BarcodeEvent(1L, Barcode("SOME_BARCODE"), "some_user", BarcodeStatus.CREATED, Instant.now())))

      barcodeService.checkBarcode("some_user", "SOME_BARCODE")

      verify(barcodeEventRepository).save(
        check {
          assertThat(it.barcode.code).isEqualTo("SOME_BARCODE")
          assertThat(it.userId).isEqualTo("some_user")
          assertThat(it.status).isEqualTo(BarcodeStatus.CHECKED)
        }
      )
    }

    @Test
    fun `should throw not found and create barcode checked event if barcode does not exist`() {
      whenever(barcodeRepository.findById("SOME_BARCODE")).thenReturn(Optional.empty())
      whenever(barcodeRepository.save(any())).thenReturn(Barcode("SOME_BARCODE"))

      assertThatThrownBy { barcodeService.checkBarcode("some_user", "SOME_BARCODE") }
        .isInstanceOf(EntityNotFoundException::class.java)

      verify(barcodeEventRepository).save(
        check {
          assertThat(it.barcode.code).isEqualTo("SOME_BARCODE")
          assertThat(it.userId).isEqualTo("some_user")
          assertThat(it.status).isEqualTo(BarcodeStatus.CHECKED)
        }
      )
    }
  }
}
