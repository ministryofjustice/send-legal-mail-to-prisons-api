package uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.barcode

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class BarcodeService(
  private val barcodeRepository: BarcodeRepository,
  private val barcodeEventService: BarcodeEventService,
  private val barcodeGeneratorService: BarcodeGeneratorService,
  private val barcodeRecipientService: BarcodeRecipientService,
) {

  @Transactional
  fun createBarcode(userId: String, createBarcodeRequest: CreateBarcodeRequest): String =
    createBarcode()
      .also { barcodeEventService.createEvent(barcode = it, userId = userId, eventType = BarcodeEventType.CREATED) }
      .also { barcodeRecipientService.saveBarcodeRecipient(it, createBarcodeRequest) }
      .code

  private fun createBarcode(): Barcode {
    var barcode = barcodeGeneratorService.generateBarcode()
    while (barcodeRepository.existsById(barcode)) {
      barcode = barcodeGeneratorService.generateBarcode()
    }
    return barcodeRepository.save(Barcode(barcode))
  }

  fun checkBarcode(userId: String, code: String, location: String): String =
    barcodeRepository.findById(code).orElseGet { barcodeRepository.save(Barcode(code)) }
      .also { barcode ->
        with(barcodeEventService) {
          createEvent(barcode, userId, BarcodeEventType.CHECKED, location)
          checkForCreated(barcode)
          checkForDuplicate(barcode, userId, location)
          checkForExpired(barcode, userId, location)
          checkForRandomSecurityCheck(barcode, userId, location)
        }
      }
      .let { barcode -> barcodeEventService.getCreatedBy(barcode) }

  fun registerEvent(userId: String, location: String, code: String, barcodeEventType: BarcodeEventType) {
    barcodeRepository.findById(code).orElseGet { barcodeRepository.save(Barcode(code)) }
      .also { barcode ->
        with(barcodeEventService) {
          createEvent(barcode, userId, barcodeEventType, location)
          checkForCreated(barcode)
        }
      }
  }
}
