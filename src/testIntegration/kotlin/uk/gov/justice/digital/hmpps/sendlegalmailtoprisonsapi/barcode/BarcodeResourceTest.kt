package uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.barcode

import com.microsoft.applicationinsights.TelemetryClient
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.groups.Tuple
import org.springframework.boot.test.mock.mockito.SpyBean
import uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.IntegrationTest

abstract class BarcodeResourceTest : IntegrationTest() {
  @SpyBean
  lateinit var telemetryClient: TelemetryClient

  protected fun aCreateBarcodeRequest(): CreateBarcodeRequest =
    CreateBarcodeRequest(prisonerName = "Fred Bloggs", prisonId = "BXI", prisonNumber = "A1234BC")

  protected fun assertBarcodeEventCreated(
    expectedBarcode: Barcode,
    expectedBarcodeEventType: BarcodeEventType,
    expectedSourceIp: String = "127.0.0.1",
  ) {
    assertThat(
      barcodeEventRepository.findByBarcodeAndEventTypeOrderByCreatedDateTime(
        expectedBarcode,
        expectedBarcodeEventType,
      ),
    )
      .isNotEmpty
      .extracting(BarcodeEvent::barcode, BarcodeEvent::ipAddress)
      .containsExactly(Tuple.tuple(expectedBarcode, expectedSourceIp))
  }
}
