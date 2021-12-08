package uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.barcode

import org.springframework.stereotype.Service

@Service
class BarcodeService(
  private val barcodeRepository: BarcodeRepository,
  private val barcodeEventService: BarcodeEventService,
  private val barcodeGeneratorService: BarcodeGeneratorService,
) {

  fun createBarcode(userId: String): String =
    createBarcode()
      .also { barcodeEventService.createEvent(barcode = it, userId = userId, status = BarcodeStatus.CREATED) }
      .code

  private fun createBarcode(): Barcode {
    var barcode = barcodeGeneratorService.generateBarcode()
    while (barcodeRepository.existsById(barcode)) {
      barcode = barcodeGeneratorService.generateBarcode()
    }
    return barcodeRepository.save(Barcode(barcode))
  }

  fun checkBarcode(userId: String, code: String, location: String) {
    barcodeRepository.findById(code).orElseGet { barcodeRepository.save(Barcode(code)) }
      .also { barcode -> barcodeEventService.createEvent(barcode, userId, BarcodeStatus.CHECKED, location) }
      .also { barcode -> barcodeEventService.checkForCreated(barcode) }
      .also { barcode -> barcodeEventService.checkForDuplicate(barcode, userId, location) }
  }
}
