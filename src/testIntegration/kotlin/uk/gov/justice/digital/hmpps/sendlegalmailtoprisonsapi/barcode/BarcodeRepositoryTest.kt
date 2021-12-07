package uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.barcode

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.IntegrationTest

class BarcodeRepositoryTest : IntegrationTest() {

  @Test
  fun `can create and retrieve a barcode`() {
    val barcode = barcodeRepository.save(Barcode(code = "SOME_BARCODE"))

    val savedBarcode = barcodeRepository.findById("SOME_BARCODE").orElseThrow()

    assertThat(savedBarcode).isEqualTo(barcode)
  }
}