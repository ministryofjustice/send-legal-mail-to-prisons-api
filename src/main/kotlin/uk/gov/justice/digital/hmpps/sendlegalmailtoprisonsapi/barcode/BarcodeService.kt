package uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.barcode

import org.springframework.stereotype.Service

@Service
class BarcodeService(
  private val barcodeRepository: BarcodeRepository,
  private val barcodeEventService: BarcodeEventService,
  private val barcodeGeneratorService: BarcodeGeneratorService,
  private val barcodeRecipientService: BarcodeRecipientService,
) {

  fun createBarcode(userId: String, createBarcodeRequest: CreateBarcodeRequest?): String =
    createBarcode()
      .also { barcodeEventService.createEvent(barcode = it, userId = userId, status = BarcodeStatus.CREATED) }
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
          createEvent(barcode, userId, BarcodeStatus.CHECKED, location)
          checkForCreated(barcode)
          checkForDuplicate(barcode, userId, location)
          checkForExpired(barcode, userId, location)
          checkForRandomSecurityCheck(barcode, userId, location)
        }
      }
      .let { barcode -> barcodeEventService.getCreatedBy(barcode) }
}
