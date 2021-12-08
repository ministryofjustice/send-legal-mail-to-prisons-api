package uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.barcode

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.check
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.config.Duplicate
import uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.config.ValidationException
import java.time.Instant
import java.time.temporal.ChronoUnit
import javax.persistence.EntityNotFoundException

class BarcodeEventServiceTest {

  private val barcodeEventRepository = mock<BarcodeEventRepository>()
  private val barcodeEventService = BarcodeEventService(barcodeEventRepository)

  @Nested
  inner class CreateEvent {
    @Test
    fun `should default empty location`() {
      whenever(barcodeEventRepository.save(any())).thenReturn(
        BarcodeEvent(barcode = Barcode("SOME_BARCODE"), userId = "some_user", status = BarcodeStatus.CREATED)
      )

      val createdEvent = barcodeEventService.createEvent(barcode = Barcode("SOME_BARCODE"), userId = "some_user", status = BarcodeStatus.CREATED)

      verify(barcodeEventRepository).save(
        check {
          assertThat(it.location).isEqualTo("")
        }
      )
      assertThat(createdEvent.location).isEqualTo("")
    }
  }

  @Nested
  inner class CheckForCreated {
    @Test
    fun `should do nothing if created event exists`() {
      whenever(barcodeEventRepository.findByBarcodeAndStatusOrderByCreatedDateTime(any(), eq(BarcodeStatus.CREATED)))
        .thenReturn(listOf(BarcodeEvent(1L, Barcode("SOME_BARCODE"), "any_user", BarcodeStatus.CREATED)))

      barcodeEventService.checkForCreated(Barcode("SOME_BARCODE"))
    }

    @Test
    fun `should throw not found if created event does not exist`() {
      whenever(barcodeEventRepository.findByBarcodeAndStatusOrderByCreatedDateTime(any(), eq(BarcodeStatus.CREATED)))
        .thenReturn(listOf())

      assertThatThrownBy { barcodeEventService.checkForCreated(Barcode("SOME_BARCODE")) }
        .isInstanceOf(EntityNotFoundException::class.java)
    }
  }

  @Nested
  inner class CheckForDuplicate {
    @Test
    fun `should do nothing if a single checked event exists`() {
      whenever(barcodeEventRepository.findByBarcodeAndStatusOrderByCreatedDateTime(any(), eq(BarcodeStatus.CHECKED)))
        .thenReturn(listOf(BarcodeEvent(1L, Barcode("SOME_BARCODE"), "any_user", BarcodeStatus.CHECKED)))

      barcodeEventService.checkForDuplicate(Barcode("SOME_BARCODE"), "any_user", "any_location")
    }

    @Test
    fun `should throw and create duplicate event if more than one checked event exists`() {
      val firstCheckTime = Instant.now().minus(1, ChronoUnit.DAYS)
      val secondCheckTime = Instant.now()
      whenever(barcodeEventRepository.findByBarcodeAndStatusOrderByCreatedDateTime(any(), eq(BarcodeStatus.CHECKED)))
        .thenReturn(
          listOf(
            BarcodeEvent(1L, Barcode("SOME_BARCODE"), "first_user", BarcodeStatus.CHECKED, firstCheckTime, "first_location"),
            BarcodeEvent(2L, Barcode("SOME_BARCODE"), "second_user", BarcodeStatus.CHECKED, secondCheckTime, "second_location"),
          )
        )

      assertThatThrownBy {
        barcodeEventService.checkForDuplicate(Barcode("SOME_BARCODE"), "some_user", "some_location")
      }.isInstanceOf(ValidationException::class.java)
        .extracting {
          val error = (it as ValidationException).errorCode as Duplicate
          assertThat(error.scannedDate).isEqualTo(firstCheckTime)
          assertThat(error.scannedLocation).isEqualTo("first_location")
        }

      verify(barcodeEventRepository).save(
        check {
          assertThat(it).extracting("barcode", "status", "location", "userId")
            .isEqualTo(listOf(Barcode("SOME_BARCODE"), BarcodeStatus.DUPLICATE, "some_location", "some_user"))
        }
      )
    }
  }
}
