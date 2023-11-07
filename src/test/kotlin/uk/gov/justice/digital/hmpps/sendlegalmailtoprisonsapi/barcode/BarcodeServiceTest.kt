package uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.barcode

import com.microsoft.applicationinsights.TelemetryClient
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyString
import org.mockito.kotlin.any
import org.mockito.kotlin.check
import org.mockito.kotlin.eq
import org.mockito.kotlin.isNull
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.config.Duplicate
import uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.config.Expired
import uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.config.RandomCheck
import uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.config.ResourceNotFoundException
import uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.config.ValidationException
import uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.prisonersearch.PrisonerSearchService
import java.time.Instant
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.util.Optional

class BarcodeServiceTest {

  private val barcodeRepository = mock<BarcodeRepository>()
  private val barcodeEventService = mock<BarcodeEventService>()
  private val barcodeGeneratorService = mock<BarcodeGeneratorService>()
  private val barcodeRecipientService = mock<BarcodeRecipientService>()
  private val prisonerSearchService = mock<PrisonerSearchService>()
  private val telemetryClient = mock<TelemetryClient>()
  private val barcodeService = BarcodeService(
    barcodeRepository,
    barcodeEventService,
    barcodeGeneratorService,
    barcodeRecipientService,
    prisonerSearchService,
    telemetryClient,
  )

  val IP_ADDRESS = "127.0.0.1"

  @Nested
  inner class CreateBarcode {
    @Test
    fun `should create barcode and created event and barcode recipient if ok`() {
      whenever(barcodeGeneratorService.generateBarcode()).thenReturn("SOME_BARCODE")
      mockFindBarcode(null)
      mockSaveBarcode()
      whenever(barcodeEventService.createEvent(any(), anyString(), any(), anyString(), anyString())).thenReturn(
        BarcodeEvent(1L, aBarcode(), "some_user", BarcodeEventType.CREATED, ipAddress = IP_ADDRESS),
      )
      val createBarcodeRequest = CreateBarcodeRequest(prisonerName = "Fred Bloggs", prisonId = "BXI", prisonNumber = "A1234BC")

      val code = barcodeService.createBarcode("some_user", IP_ADDRESS, createBarcodeRequest)

      assertThat(code).isEqualTo("SOME_BARCODE")
      verify(barcodeRepository).save(aBarcode())
      verify(barcodeEventService).createEvent(aBarcode(), "some_user", BarcodeEventType.CREATED, "", IP_ADDRESS)
      verify(barcodeRecipientService).saveBarcodeRecipient(aBarcode(), createBarcodeRequest)
      verify(telemetryClient).trackEvent(
        eq("barcode-created"),
        check {
          assertThat(it["establishment"]).isEqualTo("BXI")
          assertThat(it["prisonNumber"]).isEqualTo("A1234BC")
          assertThat(it["barcodeNumber"]).isEqualTo(aBarcode().code)
          assertThat(it["sender"]).isEqualTo("some_user")
        },
        isNull(),
      )
      verify(telemetryClient, times(1)).trackEvent(eq("barcode-created"), any(), isNull())
    }
  }

  @Nested
  inner class CheckBarcode {
    @Test
    fun `should complete ok and create checked event if barcode exists`() {
      mockFindBarcode()
      val barcodeRecipient = mockFindBarcodeRecipient()
      whenever(barcodeEventService.getCreatedBy(aBarcode())).thenReturn("some_sender")

      val createdBy = barcodeService.checkBarcode("some_user", "SOME_BARCODE", "some_location", IP_ADDRESS)

      assertThat(createdBy).isEqualTo("some_sender")
      verify(barcodeEventService)
        .createEvent(aBarcode(), "some_user", BarcodeEventType.CHECKED, "some_location", IP_ADDRESS)
      verify(prisonerSearchService).lookupPrisoner(barcodeRecipient)
      verify(telemetryClient).trackEvent(
        eq("barcode-scanned"),
        check {
          assertThat(it["activeCaseLoadId"]).isEqualTo("some_location")
          assertThat(it["barcodeNumber"]).isEqualTo("SOME_BARCODE")
          assertThat(it["username"]).isEqualTo("some_user")
          assertThat(it["outcome"]).isEqualTo("READY_FOR_DELIVERY")
        },
        isNull(),
      )
      verify(telemetryClient, times(1)).trackEvent(eq("barcode-scanned"), any(), isNull())
    }

    @Test
    fun `should throw not found and create barcode checked event if barcode does not exist`() {
      val expectedException = ResourceNotFoundException("some_message")
      mockFindBarcode(null)
      mockSaveBarcode()
      whenever(barcodeEventService.checkForCreated(aBarcode())).thenThrow(expectedException)

      assertThatThrownBy { barcodeService.checkBarcode("some_user", "SOME_BARCODE", "some_location", IP_ADDRESS) }
        .isEqualTo(expectedException)

      verify(barcodeRepository).save(aBarcode())
      verify(barcodeEventService)
        .createEvent(aBarcode(), "some_user", BarcodeEventType.CHECKED, "some_location", IP_ADDRESS)
      verifyNoInteractions(barcodeRecipientService)
      verifyNoInteractions(prisonerSearchService)

      verify(telemetryClient).trackEvent(
        eq("barcode-scanned"),
        check {
          assertThat(it["activeCaseLoadId"]).isEqualTo("some_location")
          assertThat(it["barcodeNumber"]).isEqualTo("SOME_BARCODE")
          assertThat(it["username"]).isEqualTo("some_user")
          assertThat(it["outcome"]).isEqualTo("NON_EXISTENT_BARCODE")
        },
        isNull(),
      )
      verify(telemetryClient, times(1)).trackEvent(eq("barcode-scanned"), any(), isNull())
    }

    @Test
    fun `should throw validation exception and create checked event if duplicate`() {
      val yesterday = Instant.now().minus(1, ChronoUnit.DAYS)
      val expectedException = ValidationException(Duplicate(yesterday, "previous_location", "some_sender", "some_recipient", "some_prison_number", LocalDate.of(1990, 1, 1)))
      mockFindBarcode()
      val barcodeRecipient = mockFindBarcodeRecipient()
      whenever(barcodeEventService.checkForDuplicate(aBarcode(), "current_user", "current_location", IP_ADDRESS))
        .thenThrow(expectedException)

      assertThatThrownBy { barcodeService.checkBarcode("current_user", "SOME_BARCODE", "current_location", IP_ADDRESS) }
        .isEqualTo(expectedException)

      verify(barcodeEventService)
        .createEvent(aBarcode(), "current_user", BarcodeEventType.CHECKED, "current_location", IP_ADDRESS)
      verify(prisonerSearchService).lookupPrisoner(barcodeRecipient)
      verify(telemetryClient).trackEvent(
        eq("barcode-scanned"),
        check {
          assertThat(it["activeCaseLoadId"]).isEqualTo("current_location")
          assertThat(it["barcodeNumber"]).isEqualTo("SOME_BARCODE")
          assertThat(it["username"]).isEqualTo("current_user")
          assertThat(it["outcome"]).isEqualTo("DUPLICATE")
        },
        isNull(),
      )
      verify(telemetryClient, times(1)).trackEvent(eq("barcode-scanned"), any(), isNull())
    }

    @Test
    fun `should throw validation exception and create checked event if expired`() {
      val barcodeExpiryDays = 28L
      val expired = Instant.now().minus(barcodeExpiryDays + 1, ChronoUnit.DAYS)
      val expectedException = ValidationException(Expired(expired, barcodeExpiryDays, "some_sender"))
      mockFindBarcode()
      val barcodeRecipient = mockFindBarcodeRecipient()
      whenever(barcodeEventService.checkForExpired(aBarcode(), "current_user", "current_location", IP_ADDRESS))
        .thenThrow(expectedException)

      assertThatThrownBy { barcodeService.checkBarcode("current_user", "SOME_BARCODE", "current_location", IP_ADDRESS) }
        .isEqualTo(expectedException)

      verify(barcodeEventService)
        .createEvent(aBarcode(), "current_user", BarcodeEventType.CHECKED, "current_location", IP_ADDRESS)
      verify(prisonerSearchService).lookupPrisoner(barcodeRecipient)
      verify(telemetryClient).trackEvent(
        eq("barcode-scanned"),
        check {
          assertThat(it["activeCaseLoadId"]).isEqualTo("current_location")
          assertThat(it["barcodeNumber"]).isEqualTo("SOME_BARCODE")
          assertThat(it["username"]).isEqualTo("current_user")
          assertThat(it["outcome"]).isEqualTo("EXPIRED")
        },
        isNull(),
      )
      verify(telemetryClient, times(1)).trackEvent(eq("barcode-scanned"), any(), isNull())
    }

    @Test
    fun `should throw validation exception and create checked event if selected for random check`() {
      val expectedException = ValidationException(RandomCheck("some_sender"))
      mockFindBarcode()
      val barcodeRecipient = mockFindBarcodeRecipient()
      whenever(barcodeEventService.checkForRandomSecurityCheck(aBarcode(), "current_user", "current_location", IP_ADDRESS))
        .thenThrow(expectedException)

      assertThatThrownBy { barcodeService.checkBarcode("current_user", "SOME_BARCODE", "current_location", IP_ADDRESS) }
        .isEqualTo(expectedException)

      verify(barcodeEventService)
        .createEvent(aBarcode(), "current_user", BarcodeEventType.CHECKED, "current_location", IP_ADDRESS)
      verify(prisonerSearchService).lookupPrisoner(barcodeRecipient)
      verify(telemetryClient).trackEvent(
        eq("barcode-scanned"),
        check {
          assertThat(it["activeCaseLoadId"]).isEqualTo("current_location")
          assertThat(it["barcodeNumber"]).isEqualTo("SOME_BARCODE")
          assertThat(it["username"]).isEqualTo("current_user")
          assertThat(it["outcome"]).isEqualTo("RANDOM_CHECK")
        },
        isNull(),
      )
      verify(telemetryClient, times(1)).trackEvent(eq("barcode-scanned"), any(), isNull())
    }

    @Test
    fun `should not call prisonerSearchService if barcode recipient not found`() {
      mockFindBarcode()
      mockFindBarcodeRecipientNotFound()

      barcodeService.checkBarcode("current_user", "SOME_BARCODE", "current_location", IP_ADDRESS)

      verifyNoInteractions(prisonerSearchService)
      verify(telemetryClient).trackEvent(
        eq("barcode-scanned"),
        check {
          assertThat(it["activeCaseLoadId"]).isEqualTo("current_location")
          assertThat(it["barcodeNumber"]).isEqualTo("SOME_BARCODE")
          assertThat(it["username"]).isEqualTo("current_user")
          assertThat(it["outcome"]).isEqualTo("READY_FOR_DELIVERY")
        },
        isNull(),
      )
      verify(telemetryClient, times(1)).trackEvent(eq("barcode-scanned"), any(), isNull())
    }
  }

  @Nested
  inner class RegisterEvent {
    @Test
    fun `should create event if barcode exists`() {
      mockFindBarcode()

      barcodeService.registerEvent("some_user", "some_location", IP_ADDRESS, "SOME_BARCODE", BarcodeEventType.MORE_CHECKS_REQUESTED)

      verify(barcodeEventService)
        .createEvent(aBarcode(), "some_user", BarcodeEventType.MORE_CHECKS_REQUESTED, "some_location", IP_ADDRESS)
      verify(telemetryClient).trackEvent(
        eq("barcode-scanned"),
        check {
          assertThat(it["activeCaseLoadId"]).isEqualTo("some_location")
          assertThat(it["barcodeNumber"]).isEqualTo("SOME_BARCODE")
          assertThat(it["username"]).isEqualTo("some_user")
          assertThat(it["outcome"]).isEqualTo("MORE_CHECKS_REQUESTED")
        },
        isNull(),
      )
      verify(telemetryClient, times(1)).trackEvent(eq("barcode-scanned"), any(), isNull())
    }

    @Test
    fun `should create barcode and event if barcode does not exist`() {
      mockFindBarcode(null)
      mockSaveBarcode()

      barcodeService.registerEvent("some_user", "some_location", IP_ADDRESS, "SOME_BARCODE", BarcodeEventType.MORE_CHECKS_REQUESTED)

      verify(barcodeRepository).save(aBarcode())
      verify(barcodeEventService)
        .createEvent(aBarcode(), "some_user", BarcodeEventType.MORE_CHECKS_REQUESTED, "some_location", IP_ADDRESS)
      verify(telemetryClient).trackEvent(
        eq("barcode-scanned"),
        check {
          assertThat(it["activeCaseLoadId"]).isEqualTo("some_location")
          assertThat(it["barcodeNumber"]).isEqualTo("SOME_BARCODE")
          assertThat(it["username"]).isEqualTo("some_user")
          assertThat(it["outcome"]).isEqualTo("MORE_CHECKS_REQUESTED")
        },
        isNull(),
      )
      verify(telemetryClient, times(1)).trackEvent(eq("barcode-scanned"), any(), isNull())
    }
  }

  // Test helpers
  private fun aBarcode() = Barcode("SOME_BARCODE")

  private fun mockFindBarcode(barcode: Barcode? = aBarcode()) =
    whenever(barcodeRepository.findById("SOME_BARCODE")).thenReturn(Optional.ofNullable(barcode))

  private fun mockSaveBarcode() =
    whenever(barcodeRepository.save(any())).thenReturn(aBarcode())

  private fun mockFindBarcodeRecipient(barcode: Barcode = aBarcode()): BarcodeRecipient {
    val barcodeRecipient = BarcodeRecipient(id = 1, barcode = barcode, name = "John Smith", prisonCode = "SKI")
    whenever(barcodeRecipientService.getBarcodeRecipient(barcode)).thenReturn(barcodeRecipient)
    return barcodeRecipient
  }

  private fun mockFindBarcodeRecipientNotFound(barcode: Barcode = aBarcode()) =
    whenever(barcodeRecipientService.getBarcodeRecipient(barcode)).thenReturn(null)
}
