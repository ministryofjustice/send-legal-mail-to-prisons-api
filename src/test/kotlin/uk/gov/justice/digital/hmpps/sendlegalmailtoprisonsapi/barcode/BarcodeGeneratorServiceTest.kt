package uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.barcode

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class BarcodeGeneratorServiceTest {

  private val barcodeGeneratorService = BarcodeGeneratorService()

  @Test
  fun `The barcode is always 12 digits`() {
    val barcodes = (1..20).map { barcodeGeneratorService.generateBarcode() }

    barcodes.forEach { assertThat(it.length).isEqualTo(12) }
  }

  @Test
  fun `The barcode contains only digits`() {
    val barcodes = (1..20).map { barcodeGeneratorService.generateBarcode() }

    barcodes.forEach { barcode ->
      barcode.toCharArray().forEach { char ->
        assertThat(char.isDigit()).isTrue
      }
    }
  }
}
