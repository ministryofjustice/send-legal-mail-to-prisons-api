package uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.barcode

import uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.IntegrationTest

abstract class BarcodeResourceTest : IntegrationTest() {
  protected fun aCreateBarcodeRequest(): CreateBarcodeRequest =
    CreateBarcodeRequest(prisonerName = "Fred Bloggs", prisonId = "BXI", prisonNumber = "A1234BC")
}
