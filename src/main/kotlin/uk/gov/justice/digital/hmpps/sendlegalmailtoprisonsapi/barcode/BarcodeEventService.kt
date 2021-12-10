package uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.barcode

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.config.Duplicate
import uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.config.Expired
import uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.config.ValidationException
import java.time.Instant
import javax.persistence.EntityNotFoundException

@Service
class BarcodeEventService(
  private val barcodeEventRepository: BarcodeEventRepository,
  private val barcodeConfig: BarcodeConfig,
) {

  fun createEvent(barcode: Barcode, userId: String, status: BarcodeStatus, location: String = ""): BarcodeEvent =
    barcodeEventRepository.save(
      BarcodeEvent(
        barcode = barcode,
        userId = userId,
        status = status,
        location = location,
      )
    )

  fun checkForCreated(barcode: Barcode) =
    barcodeEventRepository.findByBarcodeAndStatusOrderByCreatedDateTime(barcode, BarcodeStatus.CREATED).firstOrNull()
      ?: throw EntityNotFoundException("The barcode is not found")

  fun checkForDuplicate(barcode: Barcode, userId: String, location: String) =
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

  fun checkForExpired(barcode: Barcode, userId: String, location: String) =
    barcodeEventRepository.findByBarcodeAndStatusOrderByCreatedDateTime(barcode, BarcodeStatus.CREATED)
      .firstOrNull()
      ?.takeIf { createdEvent -> createdEvent.createdDateTime < Instant.now().minus(barcodeConfig.expiry) }
      ?.also { createdEvent ->
        barcodeEventRepository.save(
          BarcodeEvent(
            barcode = barcode,
            userId = userId,
            status = BarcodeStatus.EXPIRED,
            location = location,
          )
        )
        throw ValidationException(Expired(createdEvent.createdDateTime, barcodeConfig.expiry))
      }
}
