package uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.integration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory
import uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.barcode.Barcode
import uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.barcode.BarcodeEvent

class BarcodeEventRepositoryTest : IntegrationTest() {

  private val log = LoggerFactory.getLogger(this::class.java)

  @Test
  fun `can create and retrieve a barcode event`() {
    val barcode = barcodeRepository.save(Barcode("SOME_BARCODE"))
    val event = BarcodeEvent(barcode = barcode, userId = "SOME_USER")
    log.info(">>>>>> event has datetime=${event.dateTime}")
    val barcodeEvent = barcodeEventRepository.save(event)
    log.info(">>>>>> barcode event has datetime=${barcodeEvent.dateTime}")

    val savedBarcodeEvent = barcodeEventRepository.findById(barcodeEvent.id).orElseThrow()
    log.info(">>>>>> saved barcode event has datetime=${savedBarcodeEvent.dateTime}")

    assertThat(savedBarcodeEvent).isEqualTo(barcodeEvent)
  }
}
