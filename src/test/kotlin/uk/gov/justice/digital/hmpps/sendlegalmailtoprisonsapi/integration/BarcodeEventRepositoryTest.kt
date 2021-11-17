package uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.integration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.barcode.Barcode
import uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.barcode.BarcodeEvent

class BarcodeEventRepositoryTest : IntegrationTest() {

  @Test
  fun `can create and retrieve a barcode event`() {
    val barcode = barcodeRepository.save(Barcode("SOME_BARCODE"))
    val barcodeEvent = barcodeEventRepository.save(BarcodeEvent(barcode = barcode, userId = "SOME_USER"))

    val savedBarcodeEvent = barcodeEventRepository.findById(barcodeEvent.id).orElseThrow()

    assertThat(savedBarcodeEvent.barcode).isEqualTo(barcodeEvent.barcode)
  }
}
