package uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.barcode

import org.springframework.stereotype.Service
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneOffset

@Service
class BarcodeStatsService(private val barcodeEventRepository: BarcodeEventRepository) {

  fun countBarcodesCreated(): Long = barcodeEventRepository.countAllEventsByType(BarcodeEventType.CREATED)

  fun countBarcodesCreatedOnDay(createdDay: Instant): Long {
    val (startOfDay, endOfDay) = getDayBoundaries(createdDay)
    return barcodeEventRepository.countAllEventsBetweenTimes(BarcodeEventType.CREATED, startOfDay, endOfDay)
  }

  fun countBarcodesScanned(): Long = barcodeEventRepository.countDistinctBarcodesWithEvent(BarcodeEventType.CHECKED)

  fun countBarcodesScannedOnDay(createdDay: Instant): Long {
    val (startOfDay, endOfDay) = getDayBoundaries(createdDay)
    return barcodeEventRepository.countDistinctBarcodesWithEventBetweenTimes(BarcodeEventType.CHECKED, startOfDay, endOfDay)
  }

  fun countUniqueUsersCreatedBarcodes(): Long = barcodeEventRepository.countDistinctUsersWithEvent(BarcodeEventType.CREATED)

  private fun getDayBoundaries(createdDay: Instant): Pair<Instant, Instant> =
    Pair(
      createdDay.atOffset(ZoneOffset.UTC).with(LocalTime.of(0, 0, 0, 0)).toInstant(),
      createdDay.atOffset(ZoneOffset.UTC).with(LocalTime.of(23, 59, 59, 999999000)).toInstant()
    )
}
