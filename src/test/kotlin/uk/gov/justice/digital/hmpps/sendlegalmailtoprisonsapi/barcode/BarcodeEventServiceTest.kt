package uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.barcode

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.mockito.kotlin.any
import org.mockito.kotlin.check
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.barcode.BarcodeEventType.CHECKED
import uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.barcode.BarcodeEventType.CREATED
import uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.barcode.BarcodeEventType.DUPLICATE
import uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.barcode.BarcodeEventType.EXPIRED
import uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.barcode.BarcodeEventType.RANDOM_CHECK
import uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.cjsm.CjsmService
import uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.config.Duplicate
import uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.config.Expired
import uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.config.RandomCheck
import uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.config.ResourceNotFoundException
import uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.config.ValidationException
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.temporal.ChronoUnit

class BarcodeEventServiceTest {

  private val barcodeEventRepository = mock<BarcodeEventRepository>()
  private val barcodeRecipientRepository = mock<BarcodeRecipientRepository>()
  private val barcodeConfig = mock<BarcodeConfig>()
  private val randomCheckService = mock<RandomCheckService>()
  private val cjsmService = mock<CjsmService>()
  private val barcodeEventService = BarcodeEventService(
    barcodeEventRepository,
    barcodeRecipientRepository,
    barcodeConfig,
    randomCheckService,
    cjsmService
  )

  val IP_ADDRESS = "127.0.0.1"

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
        barcodeEventService.createEvent(barcode = aBarcode(), userId = "some_user", eventType = CREATED, sourceIp = IP_ADDRESS)

      verify(barcodeEventRepository).save(check { assertThat(it.location).isEqualTo("") })
      assertThat(createdEvent.location).isEqualTo("")
    }
  }

  @Nested
  inner class CheckForCreated {
    @Test
    fun `should do nothing if created event exists`() {
      mockFindBarcodeEventCreated(aBarcodeEvent(eventType = CREATED))

      assertDoesNotThrow {
        barcodeEventService.checkForCreated(aBarcode())
      }
    }

    @Test
    fun `should throw not found if created event does not exist`() {
      mockFindBarcodeEvents(CREATED, listOf())

      assertThatThrownBy { barcodeEventService.checkForCreated(aBarcode()) }
        .isInstanceOf(ResourceNotFoundException::class.java)
    }
  }

  @Nested
  inner class CheckForDuplicate {
    val firstCheckTime = Instant.now().minus(1, ChronoUnit.DAYS)

    fun mockDuplicateBarcodeEvents() {
      val secondCheckTime = Instant.now()
      mockFindBarcodeEvents(
        eventType = CHECKED,
        listOf(
          aBarcodeEvent(userId = "first_check_user", eventType = CHECKED, createdTime = firstCheckTime, location = "first_check_location"),
          aBarcodeEvent(userId = "second_check_user", eventType = CHECKED, createdTime = secondCheckTime, location = "second_check_location"),
        )
      )
    }

    @Test
    fun `should do nothing if a single checked event exists`() {
      mockFindBarcodeEvents(CHECKED, listOf(aBarcodeEvent(eventType = CHECKED)))

      assertDoesNotThrow {
        barcodeEventService.checkForDuplicate(aBarcode(), "any_user", "any_location", IP_ADDRESS)
      }
    }

    @Test
    fun `should throw and create duplicate event if more than one checked event exists`() {
      mockDuplicateBarcodeEvents()
      mockGetBarcodeRecipient(aBarcodeRecipient())
      mockSaveBarcodeEvent(DUPLICATE)

      assertThatThrownBy {
        barcodeEventService.checkForDuplicate(aBarcode(), "second_check_user", "second_check_location", IP_ADDRESS)
      }.isInstanceOf(ValidationException::class.java)
        .extracting {
          val error = (it as ValidationException).errorCode as Duplicate
          assertThat(error.scannedDate).isEqualTo(firstCheckTime)
          assertThat(error.scannedLocation).isEqualTo("first_check_location")
          assertThat(error.recipientName).isEqualTo("some_name")
          assertThat(error.recipientPrisonNumber).isEqualTo("some_prison_number")
          assertThat(error.recipientDob).isEqualTo(LocalDate.of(1990, 1, 1))
        }

      verify(barcodeEventRepository).save(
        check {
          assertThat(it).extracting("barcode", "eventType", "location", "userId", "ipAddress")
            .isEqualTo(listOf(aBarcode(), DUPLICATE, "second_check_location", "second_check_user", IP_ADDRESS))
        }
      )
    }

    @Test
    fun `should return unknown if there is no barcode recipient`() {
      mockDuplicateBarcodeEvents()
      mockGetBarcodeRecipient(null)
      mockSaveBarcodeEvent(DUPLICATE)

      assertThatThrownBy {
        barcodeEventService.checkForDuplicate(aBarcode(), "second_check_user", "second_check_location", IP_ADDRESS)
      }.isInstanceOf(ValidationException::class.java)
        .extracting {
          val error = (it as ValidationException).errorCode as Duplicate
          assertThat(error.scannedDate).isEqualTo(firstCheckTime)
          assertThat(error.scannedLocation).isEqualTo("first_check_location")
          assertThat(error.recipientName).isEqualTo("unknown")
          assertThat(error.recipientPrisonNumber).isEqualTo("unknown")
          assertThat(error.recipientDob).isNull()
        }
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
      mockFindBarcodeEvents(CREATED, listOf(aBarcodeEvent(eventType = CREATED, createdTime = notExpiredTime)))
      mockFindBarcodeEvents(CHECKED, listOf(aBarcodeEvent(eventType = CHECKED)))

      assertDoesNotThrow {
        barcodeEventService.checkForExpired(aBarcode(), "any_user", "any_location", IP_ADDRESS)
      }
    }

    @Test
    fun `should throw and create expired event if the barcode has expired`() {
      mockFindBarcodeEventCreated(aBarcodeEvent(eventType = CREATED, createdTime = expiredTime))
      mockFindBarcodeEvents(CHECKED, listOf(aBarcodeEvent(eventType = CHECKED)))
      mockSaveBarcodeEvent(EXPIRED)

      assertThatThrownBy {
        barcodeEventService.checkForExpired(aBarcode(), "check_user", "check_location", IP_ADDRESS)
      }.isInstanceOf(ValidationException::class.java)
        .extracting {
          val error = (it as ValidationException).errorCode as Expired
          assertThat(error.barcodeExpiryDays).isEqualTo(expiryDays)
          assertThat(error.createdDate).isEqualTo(expiredTime)
        }

      verify(barcodeEventRepository).save(
        check {
          assertThat(it).extracting("barcode", "eventType", "location", "userId", "ipAddress")
            .isEqualTo(listOf(aBarcode(), EXPIRED, "check_location", "check_user", IP_ADDRESS))
        }
      )
    }
  }

  @Nested
  inner class CheckForRandomSecurityCheck {
    @Test
    fun `should do nothing if not selected for a random check`() {
      assertDoesNotThrow {
        barcodeEventService.checkForRandomSecurityCheck(aBarcode(), "some_user", "some_location", IP_ADDRESS)
      }
    }

    @Test
    fun `should throw and create random check event if selected for a random check`() {
      whenever(randomCheckService.requiresRandomCheck()).thenReturn(true)
      mockSaveBarcodeEvent(RANDOM_CHECK)

      assertThatThrownBy {
        barcodeEventService.checkForRandomSecurityCheck(aBarcode(), "check_user", "check_location", IP_ADDRESS)
      }.isInstanceOf(ValidationException::class.java)
        .extracting {
          assertThat((it as ValidationException).errorCode).isInstanceOf(RandomCheck::class.java)
        }

      verify(barcodeEventRepository).save(
        check {
          assertThat(it).extracting("barcode", "eventType", "location", "userId", "ipAddress")
            .isEqualTo(listOf(aBarcode(), RANDOM_CHECK, "check_location", "check_user", IP_ADDRESS))
        }
      )
    }
  }

  @Nested
  inner class GetCreatedBy {
    @Test
    fun `should return the organisation of the user that created the barcode`() {
      mockFindBarcodeEventCreated(aBarcodeEvent(userId = "some.user@company.com.cjsm.net", eventType = CREATED))
      mockUserOrganisation("some.user@company.com.cjsm.net", "some organisation")

      val createdBy = barcodeEventService.getCreatedBy(aBarcode())

      assertThat(createdBy).isEqualTo("some organisation")
    }

    @Test
    fun `should return user if we cannot find the user in the CJSM directory`() {
      mockFindBarcodeEventCreated(aBarcodeEvent(userId = "some.user@company.com.cjsm.net", eventType = CREATED))
      mockUserOrganisation("some.user@copmany.com.cjsm.net", null)

      val createdBy = barcodeEventService.getCreatedBy(aBarcode())

      assertThat(createdBy).isEqualTo("some.user@company.com.cjsm.net")
    }

    @Test
    fun `should return user if the user has a blank organisation`() {
      mockFindBarcodeEventCreated(aBarcodeEvent(userId = "some.user@company.com.cjsm.net", eventType = CREATED))
      mockUserOrganisation("some.user@copmany.com.cjsm.net", "")

      val createdBy = barcodeEventService.getCreatedBy(aBarcode())

      assertThat(createdBy).isEqualTo("some.user@company.com.cjsm.net")
    }

    @Test
    fun `should return with error text if cannot find user created event - in theory this cannot happen`() {
      mockFindBarcodeEventCreated(null)

      val createdBy = barcodeEventService.getCreatedBy(aBarcode())

      assertThat(createdBy).contains("error")
    }
  }

  // Test helpers
  private fun aBarcode() = Barcode("SOME_BARCODE")

  private fun aBarcodeEvent(
    userId: String = "any_user",
    createdTime: Instant = Instant.now(),
    location: String = "",
    eventType: BarcodeEventType,
    ipAddress: String = IP_ADDRESS,
  ) =
    BarcodeEvent(
      barcode = aBarcode(),
      userId = userId,
      eventType = eventType,
      createdDateTime = createdTime,
      location = location,
      ipAddress = ipAddress
    )

  private fun aBarcodeRecipient() = BarcodeRecipient(barcode = aBarcode(), name = "some_name", prisonCode = "some_prison_code", prisonNumber = "some_prison_number", dob = LocalDate.of(1990, 1, 1))

  private fun mockFindBarcodeEventCreated(barcodeEvent: BarcodeEvent?) =
    whenever(barcodeEventRepository.findByBarcodeAndEventTypeCreated(aBarcode()))
      .thenReturn(barcodeEvent)

  private fun mockFindBarcodeEvents(eventType: BarcodeEventType, barcodeEvents: List<BarcodeEvent>) =
    whenever(barcodeEventRepository.findByBarcodeAndEventTypeOrderByCreatedDateTime(any(), eq(eventType)))
      .thenReturn(barcodeEvents)

  private fun mockSaveBarcodeEvent(eventType: BarcodeEventType) =
    whenever(barcodeEventRepository.save(any())).thenReturn(aBarcodeEvent(eventType = eventType))

  private fun mockUserOrganisation(userId: String, organisation: String?) {
    whenever(cjsmService.findOrganisation(userId)).thenReturn(organisation)
  }

  private fun mockGetBarcodeRecipient(barcodeRecipient: BarcodeRecipient?, barcode: Barcode = barcodeRecipient?.barcode ?: aBarcode()) =
    whenever(barcodeRecipientRepository.getByBarcode(barcode)).thenReturn(barcodeRecipient)
}
