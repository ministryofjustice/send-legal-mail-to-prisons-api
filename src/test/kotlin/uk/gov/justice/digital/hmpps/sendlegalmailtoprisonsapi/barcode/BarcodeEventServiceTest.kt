package uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.barcode

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.check
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.barcode.BarcodeStatus.CHECKED
import uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.barcode.BarcodeStatus.CREATED
import uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.barcode.BarcodeStatus.DUPLICATE
import uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.barcode.BarcodeStatus.EXPIRED
import uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.barcode.BarcodeStatus.RANDOM_CHECK
import uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.config.Duplicate
import uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.config.Expired
import uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.config.RandomCheck
import uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.config.ValidationException
import java.time.Duration
import java.time.Instant
import java.time.temporal.ChronoUnit
import javax.persistence.EntityNotFoundException

class BarcodeEventServiceTest {

  private val barcodeEventRepository = mock<BarcodeEventRepository>()
  private val barcodeConfig = mock<BarcodeConfig>()
  private val randomCheckService = mock<RandomCheckService>()
  private val barcodeEventService = BarcodeEventService(barcodeEventRepository, barcodeConfig, randomCheckService)

  @BeforeEach
  fun `defaults to not requiring a random security check`() {
    whenever(randomCheckService.requiresRandomCheck()).thenReturn(false)
  }

  @Nested
  inner class CreateEvent {
    @Test
    fun `should default empty location`() {
      mockSaveBarcodeEvent(CREATED)

      val createdEvent =
        barcodeEventService.createEvent(barcode = aBarcode(), userId = "some_user", status = CREATED)

      verify(barcodeEventRepository).save(check { assertThat(it.location).isEqualTo("") })
      assertThat(createdEvent.location).isEqualTo("")
    }
  }

  @Nested
  inner class CheckForCreated {
    @Test
    fun `should do nothing if created event exists`() {
      mockFindBarcodeEvents(CREATED, listOf(aBarcodeEvent(status = CREATED)))

      assertDoesNotThrow {
        barcodeEventService.checkForCreated(aBarcode())
      }
    }

    @Test
    fun `should throw not found if created event does not exist`() {
      mockFindBarcodeEvents(CREATED, listOf())

      assertThatThrownBy { barcodeEventService.checkForCreated(aBarcode()) }
        .isInstanceOf(EntityNotFoundException::class.java)
    }
  }

  @Nested
  inner class CheckForDuplicate {
    @Test
    fun `should do nothing if a single checked event exists`() {
      mockFindBarcodeEvents(CHECKED, listOf(aBarcodeEvent(status = CHECKED)))

      assertDoesNotThrow {
        barcodeEventService.checkForDuplicate(aBarcode(), "any_user", "any_location")
      }
    }

    @Test
    fun `should throw and create duplicate event if more than one checked event exists`() {
      val firstCheckTime = Instant.now().minus(1, ChronoUnit.DAYS)
      val secondCheckTime = Instant.now()
      mockFindBarcodeEvents(
        status = CHECKED,
        listOf(
          aBarcodeEvent(userId = "first_check_user", status = CHECKED, createdTime = firstCheckTime, location = "first_check_location"),
          aBarcodeEvent(userId = "second_check_user", status = CHECKED, createdTime = secondCheckTime, location = "second_check_location"),
        )
      )
      mockSaveBarcodeEvent(DUPLICATE)

      assertThatThrownBy {
        barcodeEventService.checkForDuplicate(aBarcode(), "second_check_user", "second_check_location")
      }.isInstanceOf(ValidationException::class.java)
        .extracting {
          val error = (it as ValidationException).errorCode as Duplicate
          assertThat(error.scannedDate).isEqualTo(firstCheckTime)
          assertThat(error.scannedLocation).isEqualTo("first_check_location")
        }

      verify(barcodeEventRepository).save(
        check {
          assertThat(it).extracting("barcode", "status", "location", "userId")
            .isEqualTo(listOf(aBarcode(), DUPLICATE, "second_check_location", "second_check_user"))
        }
      )
    }
  }

  @Nested
  inner class CheckForExpired {

    private val expiryDays = 28L
    private val expiryDuration = Duration.ofDays(expiryDays)
    private val expiredTime = Instant.now().minus(expiryDuration).minus(1, ChronoUnit.DAYS)
    private val notExpiredTime = Instant.now().minus(expiryDuration).plus(1, ChronoUnit.DAYS)

    @BeforeEach
    fun `mock barcode expiry`() {
      whenever(barcodeConfig.expiry).thenReturn(expiryDuration)
    }

    @Test
    fun `should do nothing if the barcode has not expired`() {
      mockFindBarcodeEvents(CREATED, listOf(aBarcodeEvent(status = CREATED, createdTime = notExpiredTime)))
      mockFindBarcodeEvents(CHECKED, listOf(aBarcodeEvent(status = CHECKED)))

      assertDoesNotThrow {
        barcodeEventService.checkForExpired(aBarcode(), "any_user", "any_location")
      }
    }

    @Test
    fun `should throw and create expired event if the barcode has expired`() {
      mockFindBarcodeEvents(CREATED, listOf(aBarcodeEvent(status = CREATED, createdTime = expiredTime)))
      mockFindBarcodeEvents(CHECKED, listOf(aBarcodeEvent(status = CHECKED)))
      mockSaveBarcodeEvent(EXPIRED)

      assertThatThrownBy {
        barcodeEventService.checkForExpired(aBarcode(), "check_user", "check_location")
      }.isInstanceOf(ValidationException::class.java)
        .extracting {
          val error = (it as ValidationException).errorCode as Expired
          assertThat(error.barcodeExpiryDays).isEqualTo(expiryDays)
          assertThat(error.createdDate).isEqualTo(expiredTime)
        }

      verify(barcodeEventRepository).save(
        check {
          assertThat(it).extracting("barcode", "status", "location", "userId")
            .isEqualTo(listOf(aBarcode(), EXPIRED, "check_location", "check_user"))
        }
      )
    }
  }

  @Nested
  inner class CheckForRandomSecurityCheck {
    @Test
    fun `should do nothing if not selected for a random check`() {
      assertDoesNotThrow {
        barcodeEventService.checkForRandomSecurityCheck(aBarcode(), "some_user", "some_location")
      }
    }

    @Test
    fun `should throw and create random check event if selected for a random check`() {
      whenever(randomCheckService.requiresRandomCheck()).thenReturn(true)
      mockSaveBarcodeEvent(RANDOM_CHECK)

      assertThatThrownBy {
        barcodeEventService.checkForRandomSecurityCheck(aBarcode(), "check_user", "check_location")
      }.isInstanceOf(ValidationException::class.java)
        .extracting {
          assertThat((it as ValidationException).errorCode).isInstanceOf(RandomCheck::class.java)
        }

      verify(barcodeEventRepository).save(
        check {
          assertThat(it).extracting("barcode", "status", "location", "userId")
            .isEqualTo(listOf(aBarcode(), RANDOM_CHECK, "check_location", "check_user"))
        }
      )
    }
  }

  @Nested
  inner class GetCeatedBy {
    @Test
    fun `Will return the user that created the barcode`() {
      mockFindBarcodeEvents(CREATED, listOf(aBarcodeEvent(userId = "some_user", status = CREATED)))

      val createdBy = barcodeEventService.getCreatedBy(aBarcode())

      assertThat(createdBy).isEqualTo("some_user")
    }
  }

  // Test helpers
  private fun aBarcode() = Barcode("SOME_BARCODE")

  private fun aBarcodeEvent(
    userId: String = "any_user",
    createdTime: Instant = Instant.now(),
    location: String = "",
    status: BarcodeStatus,
  ) =
    BarcodeEvent(
      barcode = aBarcode(),
      userId = userId,
      status = status,
      createdDateTime = createdTime,
      location = location
    )

  private fun mockFindBarcodeEvents(status: BarcodeStatus, barcodeEvents: List<BarcodeEvent>) =
    whenever(barcodeEventRepository.findByBarcodeAndStatusOrderByCreatedDateTime(any(), eq(status)))
      .thenReturn(barcodeEvents)

  private fun mockSaveBarcodeEvent(status: BarcodeStatus) =
    whenever(barcodeEventRepository.save(any())).thenReturn(aBarcodeEvent(status = status))
}
