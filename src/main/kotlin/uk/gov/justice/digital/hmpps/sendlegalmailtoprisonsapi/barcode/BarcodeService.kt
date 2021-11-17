package uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.barcode

import org.springframework.stereotype.Service

@Service
class BarcodeService(
  private val barcodeRepository: BarcodeRepository,
  private val barcodeEventRepository: BarcodeEventRepository,
  private val barcodeGeneratorService: BarcodeGeneratorService,
) {

  fun createBarcode(userId: String): String =
    createBarcode()
      .also { barcodeEventRepository.save(BarcodeEvent(barcode = it, userId = userId)) }
      .code

  private fun createBarcode(): Barcode {
    var barcode = barcodeGeneratorService.generateBarcode()
    while (barcodeRepository.existsById(barcode)) {
      barcode = barcodeGeneratorService.generateBarcode()
    }
    return barcodeRepository.save(Barcode(barcode))
  }
}
