package uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.barcode

import org.springframework.stereotype.Service
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

@Service
class BarcodeStatsService(private val barcodeEventRepository: BarcodeEventRepository) {

  fun countBarcodesCreated(): Long = barcodeEventRepository.countAllEventsByType(BarcodeEventType.CREATED)

  fun countBarcodesCreatedOnDay(createdDay: LocalDate): Long {
    val (startOfDay, endOfDay) = getDayBoundaries(createdDay)
    return barcodeEventRepository.countAllEventsBetweenTimes(BarcodeEventType.CREATED, startOfDay, endOfDay)
  }

  fun countBarcodesScanned(): Long = barcodeEventRepository.countDistinctBarcodesWithEvent(BarcodeEventType.CHECKED)

  fun countBarcodesScannedOnDay(createdDay: LocalDate): Long {
    val (startOfDay, endOfDay) = getDayBoundaries(createdDay)
    return barcodeEventRepository.countDistinctBarcodesWithEventBetweenTimes(BarcodeEventType.CHECKED, startOfDay, endOfDay)
  }

  fun countUniqueUsersCreatedBarcodes(): Long = barcodeEventRepository.countDistinctUsersWithEvent(BarcodeEventType.CREATED)

  private fun getDayBoundaries(day: LocalDate): Pair<Instant, Instant> =
    Pair(
      day.atStartOfDay(ZoneOffset.UTC).toInstant(),
      day.atTime(23, 59, 59, 999999000).toInstant(ZoneOffset.UTC),
    )
}
