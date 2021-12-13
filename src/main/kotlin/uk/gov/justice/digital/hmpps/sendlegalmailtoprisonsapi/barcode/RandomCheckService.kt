package uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.barcode

import org.springframework.stereotype.Service
import kotlin.random.Random

@Service
class RandomCheckService(private val barcodeConfig: BarcodeConfig) {

  fun requiresRandomCheck() = Random.nextInt(0, 100) < barcodeConfig.randomCheckPercentage
}
