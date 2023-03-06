package uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.barcode

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.barcode.BarcodeEventType.DUPLICATE
import uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.barcode.BarcodeEventType.EXPIRED
import uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.barcode.BarcodeEventType.RANDOM_CHECK
import uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.cjsm.CjsmService
import uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.config.Duplicate
import uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.config.Expired
import uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.config.RandomCheck
import uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.config.ResourceNotFoundException
import uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.config.ValidationException
import java.time.Instant

@Service
class BarcodeEventService(
  private val barcodeEventRepository: BarcodeEventRepository,
  private val barcodeRecipientRepository: BarcodeRecipientRepository,
  private val barcodeConfig: BarcodeConfig,
  private val randomCheckService: RandomCheckService,
  private val cjsmService: CjsmService,
) {

  fun createEvent(barcode: Barcode, userId: String, eventType: BarcodeEventType, location: String = "", sourceIp: String): BarcodeEvent =
    barcodeEventRepository.save(
      BarcodeEvent(
        barcode = barcode,
        userId = userId,
        eventType = eventType,
        location = location,
        ipAddress = sourceIp,
      ),
    )

  fun checkForCreated(barcode: Barcode) =
    barcodeEventRepository.findByBarcodeAndEventTypeCreated(barcode)
      ?: throw ResourceNotFoundException("Barcode ${barcode.code} not found")

  fun checkForDuplicate(barcode: Barcode, userId: String, location: String, sourceIp: String) =
    barcodeEventRepository.findByBarcodeAndEventTypeOrderByCreatedDateTime(barcode, BarcodeEventType.CHECKED)
      .takeIf { checkedEvents -> checkedEvents.size > 1 }
      ?.first()
      ?.also { firstCheck ->
        createEvent(barcode, userId, DUPLICATE, location, sourceIp)
        val recipient = barcodeRecipientRepository.getByBarcode(barcode) ?: BarcodeRecipient(barcode = barcode, name = "unknown", prisonCode = "unknown", prisonNumber = "unknown")
        throw ValidationException(Duplicate(firstCheck.createdDateTime, firstCheck.location, getCreatedBy(barcode), recipient.name, recipient.prisonNumber, recipient.dob))
      }

  fun checkForExpired(barcode: Barcode, userId: String, location: String, sourceIp: String) =
    barcodeEventRepository.findByBarcodeAndEventTypeCreated(barcode)
      ?.takeIf { createdEvent -> createdEvent.createdDateTime < Instant.now().minus(barcodeConfig.expiry) }
      ?.also { createdEvent ->
        createEvent(barcode, userId, EXPIRED, location, sourceIp)
        throw ValidationException(Expired(createdEvent.createdDateTime, barcodeConfig.expiry.toDays(), getCreatedBy(barcode)))
      }

  fun checkForRandomSecurityCheck(barcode: Barcode, userId: String, location: String, sourceIp: String) =
    randomCheckService.requiresRandomCheck()
      .takeIf { requiresRandomCheck -> requiresRandomCheck }
      ?.also {
        createEvent(barcode, userId, RANDOM_CHECK, location, sourceIp)
        throw ValidationException(RandomCheck(getCreatedBy(barcode)))
      }

  fun getCreatedBy(barcode: Barcode): String =
    barcodeEventRepository.findByBarcodeAndEventTypeCreated(barcode)
      ?.userId
      ?.let { userId -> findOrganisation(userId) ?: userId }
      ?: "An error occurred and we cannot identify the barcode sender"

  private fun findOrganisation(userId: String): String? =
    cjsmService.findOrganisation(userId)
      ?.takeIf { it.isNotBlank() }
}
