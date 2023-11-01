package uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.barcode

import com.microsoft.applicationinsights.TelemetryClient
import mu.KotlinLogging
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.config.ResourceNotFoundException
import uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.config.ValidationException
import uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.prisonersearch.PrisonerSearchService

private val log = KotlinLogging.logger {}

@Service
class BarcodeService(
  private val barcodeRepository: BarcodeRepository,
  private val barcodeEventService: BarcodeEventService,
  private val barcodeGeneratorService: BarcodeGeneratorService,
  private val barcodeRecipientService: BarcodeRecipientService,
  private val prisonerSearchService: PrisonerSearchService,
  private val telemetryClient: TelemetryClient,
) {
  enum class ScanEventType { READY_FOR_DELIVERY, NON_EXISTENT_BARCODE, DUPLICATE, EXPIRED, RANDOM_CHECK, MORE_CHECKS_REQUESTED }
  enum class TelemetryEventType(val eventName: String) { CREATE("barcode-created"), SCAN("barcode-scanned") }

  @Transactional
  fun createBarcode(userId: String, sourceIp: String, createBarcodeRequest: CreateBarcodeRequest): String =
    createBarcode()
      .also { barcode ->
        barcodeEventService.createEvent(barcode = barcode, userId = userId, eventType = BarcodeEventType.CREATED, sourceIp = sourceIp)
          .also {
            trackCreateEvent(barcode, userId, createBarcodeRequest)
          }
      }
      .also { barcodeRecipientService.saveBarcodeRecipient(it, createBarcodeRequest) }
      .code

  private fun createBarcode(): Barcode {
    var barcode = barcodeGeneratorService.generateBarcode()
    while (barcodeRepository.existsById(barcode)) {
      barcode = barcodeGeneratorService.generateBarcode()
    }
    return barcodeRepository.save(Barcode(barcode))
  }

  fun checkBarcode(userId: String, code: String, location: String, sourceIp: String): String =
    barcodeRepository.findById(code).orElseGet { barcodeRepository.save(Barcode(code)) }
      .also { barcode ->
        with(barcodeEventService) {
          var isForwardingRequired = false
          createEvent(barcode, userId, BarcodeEventType.CHECKED, location, sourceIp)
          try {
            checkForCreated(barcode)
          } catch (e: ResourceNotFoundException) {
            // if ResourceNotFoundException thrown track the event
            // else track the event post call to lookupRecipient
            trackScanEvent(ScanEventType.NON_EXISTENT_BARCODE, barcode, userId, location)
            throw e
          }

          barcodeRecipientService.getBarcodeRecipient(barcode)
            ?.lookupRecipient().also { it?.let { isForwardingRequired = it } }
            ?: log.info { "No BarcodeRecipient record for barcode ${barcode.code}" }

          try {
            checkForDuplicate(barcode, userId, location, sourceIp)
            checkForExpired(barcode, userId, location, sourceIp)
            checkForRandomSecurityCheck(barcode, userId, location, sourceIp)

            // if no validation exception thrown above it means that the barcode is ready for delivery
            trackScanEvent(ScanEventType.READY_FOR_DELIVERY, barcode, userId, location, isForwardingRequired)
          } catch (e: ValidationException) {
            when (e.errorCode.code) {
              "DUPLICATE" -> trackScanEvent(ScanEventType.DUPLICATE, barcode, userId, location, isForwardingRequired)
              "EXPIRED" -> trackScanEvent(ScanEventType.EXPIRED, barcode, userId, location, isForwardingRequired)
              "RANDOM_CHECK" -> trackScanEvent(ScanEventType.RANDOM_CHECK, barcode, userId, location, isForwardingRequired)
            }
            throw e
          }
        }
      }
      .let { barcode -> barcodeEventService.getCreatedBy(barcode) }

  fun registerEvent(userId: String, location: String, sourceIp: String, code: String, barcodeEventType: BarcodeEventType) {
    barcodeRepository.findById(code).orElseGet { barcodeRepository.save(Barcode(code)) }
      .also { barcode ->
        with(barcodeEventService) {
          createEvent(barcode, userId, barcodeEventType, location, sourceIp)
          trackScanEvent(ScanEventType.MORE_CHECKS_REQUESTED, barcode, userId, location, false)
          checkForCreated(barcode)
        }
      }
  }

  private fun BarcodeRecipient.lookupRecipient(): Boolean {
    return prisonerSearchService.lookupPrisoner(this)
  }

  fun trackCreateEvent(barcode: Barcode, userId: String, createBarcodeRequest: CreateBarcodeRequest) {
    trackEvent(
      TelemetryEventType.CREATE,
      mapOf(
        "establishment" to createBarcodeRequest.prisonId,
        "prisonNumber" to createBarcodeRequest.prisonNumber,
        "barcodeNumber" to barcode.code,
        "sender" to userId,
      ),
    )
  }

  fun trackScanEvent(scanEventType: ScanEventType, barcode: Barcode, userId: String, location: String, isForwardingRequired: Boolean? = false) {
    trackEvent(
      TelemetryEventType.SCAN,
      mapOf(
        "activeCaseLoadId" to location,
        "barcodeNumber" to barcode.code,
        "forwardingRequired" to isForwardingRequired.toString(),
        "username" to userId,
        "outcome" to scanEventType.name,
      ),
    )
  }

  fun trackEvent(event: TelemetryEventType, properties: Map<String, String?>) =
    telemetryClient.trackEvent(event.eventName, properties, null)
}
