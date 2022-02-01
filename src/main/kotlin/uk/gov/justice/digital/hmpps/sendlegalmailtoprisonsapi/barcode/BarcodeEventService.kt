package uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.barcode

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.barcode.BarcodeStatus.DUPLICATE
import uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.barcode.BarcodeStatus.EXPIRED
import uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.barcode.BarcodeStatus.RANDOM_CHECK
import uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.cjsm.CjsmService
import uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.config.Duplicate
import uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.config.Expired
import uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.config.RandomCheck
import uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.config.ValidationException
import java.time.Instant
import javax.persistence.EntityNotFoundException

@Service
class BarcodeEventService(
  private val barcodeEventRepository: BarcodeEventRepository,
  private val barcodeConfig: BarcodeConfig,
  private val randomCheckService: RandomCheckService,
  private val cjsmService: CjsmService,
) {

  @Transactional
  fun createEvent(barcode: Barcode, userId: String, status: BarcodeStatus, location: String = ""): BarcodeEvent =
    barcodeEventRepository.save(
      BarcodeEvent(
        barcode = barcode,
        userId = userId,
        status = status,
        location = location,
      )
    )

  @Transactional(readOnly = true)
  fun checkForCreated(barcode: Barcode) =
    barcodeEventRepository.findByBarcodeAndStatusCreated(barcode)
      ?: throw EntityNotFoundException("The barcode is not found")

  @Transactional(readOnly = true)
  fun checkForDuplicate(barcode: Barcode, userId: String, location: String) =
    barcodeEventRepository.findByBarcodeAndStatusOrderByCreatedDateTime(barcode, BarcodeStatus.CHECKED)
      .takeIf { checkedEvents -> checkedEvents.size > 1 }
      ?.first()
      ?.also { firstCheck ->
        createEvent(barcode, userId, DUPLICATE, location)
        throw ValidationException(Duplicate(firstCheck.createdDateTime, firstCheck.location, getCreatedBy(barcode)))
      }

  @Transactional(readOnly = true)
  fun checkForExpired(barcode: Barcode, userId: String, location: String) =
    barcodeEventRepository.findByBarcodeAndStatusCreated(barcode)
      ?.takeIf { createdEvent -> createdEvent.createdDateTime < Instant.now().minus(barcodeConfig.expiry) }
      ?.also { createdEvent ->
        createEvent(barcode, userId, EXPIRED, location)
        throw ValidationException(Expired(createdEvent.createdDateTime, barcodeConfig.expiry.toDays(), getCreatedBy(barcode)))
      }

  @Transactional(readOnly = true)
  fun checkForRandomSecurityCheck(barcode: Barcode, userId: String, location: String) =
    randomCheckService.requiresRandomCheck()
      .takeIf { requiresRandomCheck -> requiresRandomCheck }
      ?.also {
        createEvent(barcode, userId, RANDOM_CHECK, location)
        throw ValidationException(RandomCheck(getCreatedBy(barcode)))
      }

  @Transactional(readOnly = true)
  fun getCreatedBy(barcode: Barcode): String =
    barcodeEventRepository.findByBarcodeAndStatusCreated(barcode)
      ?.userId
      ?.let { userId -> findOrganisation(userId) ?: userId }
      ?: "An error occurred and we cannot identify the barcode sender"

  private fun findOrganisation(userId: String): String? =
    cjsmService.findOrganisation(userId)
      ?.takeIf { it.isNotBlank() }
}
