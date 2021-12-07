package uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.barcode

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.toNullable
import javax.persistence.EntityNotFoundException

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

  fun checkBarcode(userId: String, code: String) {
    barcodeRepository.findById(code).toNullable()
      ?.also { barcode -> createCheckedEventFromCreatedEvent(userId, barcode) }
      ?: also {
        createCheckedEventForInvalidBarcode(userId, code)
        throw EntityNotFoundException("The barcode is not found")
      }
  }

  private fun createCheckedEventFromCreatedEvent(userId: String, barcode: Barcode) {
    barcodeEventRepository.findByBarcodeAndStatus(barcode, BarcodeStatus.CREATED).firstOrNull()
      ?.also {
        barcodeEventRepository.save(
          BarcodeEvent(
            barcode = barcode,
            userId = userId,
            status = BarcodeStatus.CHECKED,
          )
        )
      }
  }

  private fun createCheckedEventForInvalidBarcode(userId: String, code: String) {
    barcodeRepository.findById(code).orElseGet { barcodeRepository.save(Barcode(code = code)) }
      .also { barcode ->
        barcodeEventRepository.save(
          BarcodeEvent(
            barcode = barcode,
            userId = userId,
            status = BarcodeStatus.CHECKED,
          )
        )
      }
  }
}
