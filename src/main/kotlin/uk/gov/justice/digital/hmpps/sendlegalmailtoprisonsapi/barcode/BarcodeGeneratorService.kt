package uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.barcode

import org.springframework.stereotype.Service
import kotlin.random.Random

@Service
class BarcodeGeneratorService {
  private val maxBarcode = 999_999_999_999

  fun generateBarcode(): String = Random.nextLong(maxBarcode).toString()
    .padStart(12, '0')
}
