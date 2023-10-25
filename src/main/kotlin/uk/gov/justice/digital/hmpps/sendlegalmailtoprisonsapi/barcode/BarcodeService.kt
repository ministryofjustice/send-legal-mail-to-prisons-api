package uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.barcode

import mu.KotlinLogging
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.prisonersearch.PrisonerSearchService

private val log = KotlinLogging.logger {}

@Service
class BarcodeService(
  private val barcodeRepository: BarcodeRepository,
  private val barcodeEventService: BarcodeEventService,
  private val barcodeGeneratorService: BarcodeGeneratorService,
  private val barcodeRecipientService: BarcodeRecipientService,
  private val prisonerSearchService: PrisonerSearchService,
) {

  @Transactional
  fun createBarcode(userId: String, sourceIp: String, createBarcodeRequest: CreateBarcodeRequest): String =
    createBarcode()
      .also { barcodeEventService.createEvent(barcode = it, userId = userId, eventType = BarcodeEventType.CREATED, sourceIp = sourceIp, createBarcodeRequest = createBarcodeRequest) }
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
          createEvent(barcode, userId, BarcodeEventType.CHECKED, location, sourceIp)
          checkForCreated(barcode)

          barcodeRecipientService.getBarcodeRecipient(barcode)
            ?.lookupRecipient()
            ?: log.info { "No BarcodeRecipient record for barcode ${barcode.code}" }

          checkForDuplicate(barcode, userId, location, sourceIp)
          checkForExpired(barcode, userId, location, sourceIp)
          checkForRandomSecurityCheck(barcode, userId, location, sourceIp)
        }
      }
      .let { barcode -> barcodeEventService.getCreatedBy(barcode) }

  fun registerEvent(userId: String, location: String, sourceIp: String, code: String, barcodeEventType: BarcodeEventType) {
    barcodeRepository.findById(code).orElseGet { barcodeRepository.save(Barcode(code)) }
      .also { barcode ->
        with(barcodeEventService) {
          createEvent(barcode, userId, barcodeEventType, location, sourceIp)
          checkForCreated(barcode)
        }
      }
  }

  private fun BarcodeRecipient.lookupRecipient() {
    prisonerSearchService.lookupPrisoner(this)
  }
}
