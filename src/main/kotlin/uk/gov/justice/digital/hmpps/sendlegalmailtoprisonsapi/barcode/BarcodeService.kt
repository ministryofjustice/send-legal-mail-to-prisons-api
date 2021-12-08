package uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.barcode

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.config.Duplicate
import uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.config.ValidationException
import javax.persistence.EntityNotFoundException

@Service
class BarcodeService(
  private val barcodeRepository: BarcodeRepository,
  private val barcodeEventRepository: BarcodeEventRepository,
  private val barcodeGeneratorService: BarcodeGeneratorService,
) {

  fun createBarcode(userId: String): String =
    createBarcode()
      .also { barcodeEventRepository.save(BarcodeEvent(barcode = it, userId = userId, status = BarcodeStatus.CREATED)) }
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
      .also { barcode -> createCheckedEvent(barcode, userId, location) }
      .also { barcode -> checkForCreated(barcode) }
      .also { barcode -> checkForDuplicate(barcode, userId, location) }
  }

  private fun createCheckedEvent(barcode: Barcode, userId: String, location: String) {
    barcodeEventRepository.save(
      BarcodeEvent(
        barcode = barcode,
        userId = userId,
        status = BarcodeStatus.CHECKED,
        location = location,
      )
    )
  }

  private fun checkForCreated(barcode: Barcode) =
    barcodeEventRepository.findByBarcodeAndStatusOrderByCreatedDateTime(barcode, BarcodeStatus.CREATED).firstOrNull()
      ?: throw EntityNotFoundException("The barcode is not found")

  private fun checkForDuplicate(barcode: Barcode, userId: String, location: String) =
    barcodeEventRepository.findByBarcodeAndStatusOrderByCreatedDateTime(barcode, BarcodeStatus.CHECKED)
      .takeIf { checkedEvents -> checkedEvents.size > 1 }
      ?.first()
      ?.also { firstCheck ->
        barcodeEventRepository.save(
          BarcodeEvent(
            barcode = barcode,
            userId = userId,
            status = BarcodeStatus.DUPLICATE,
            location = location
          )
        )
        throw ValidationException(Duplicate(firstCheck.createdDateTime, firstCheck.location))
      }
}
