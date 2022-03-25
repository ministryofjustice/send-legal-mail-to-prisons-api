package uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.prisonersearch

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.given
import org.mockito.kotlin.mock
import org.springframework.web.reactive.function.client.WebClientResponseException.create
import reactor.core.publisher.Mono
import uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.barcode.Barcode
import uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.barcode.BarcodeRecipient
import uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.prisonersearch.MatchedBy.ALL_SUPPLIED
import java.time.LocalDate

class PrisonerSearchServiceTest {
  val prisonerSearchClient = mock<PrisonerSearchClient>()
  val prisonerSearchService = PrisonerSearchService(prisonerSearchClient)

  @Nested
  inner class LookupPrisoner {
    @Test
    fun `should lookup prisoner`() {
      val barcodeRecipient = BarcodeRecipient(
        barcode = aBarcode(),
        prisonCode = "BXI",
        prisonNumber = "A1234BC",
        name = "John Smith",
        dob = null
      )
      given { prisonerSearchClient.matchPrisoners(any()) }.willReturn(
        Mono.just(aMatchPrisonersResponse())
      )

      prisonerSearchService.lookupPrisoner(barcodeRecipient)
      // nothing to assert at this stage
    }

    @Test
    fun `should lookup prisoner given user is forbidden from calling the API`() {
      val barcodeRecipient = BarcodeRecipient(
        barcode = aBarcode(),
        prisonCode = "BXI",
        prisonNumber = "A1234BC",
        name = "John Smith",
        dob = null
      )
      given { prisonerSearchClient.matchPrisoners(any()) }.willReturn(
        Mono.error(create(403, "Forbidden", null, null, null))
      )

      prisonerSearchService.lookupPrisoner(barcodeRecipient)
      // nothing to assert at this stage
    }
  }

  @Nested
  inner class Create_PrisonerSearchRequest {
    @Test
    fun `should create PrisonerSearchRequest given BarcodeRecipient with prisonNumber`() {
      val barcodeRecipient = BarcodeRecipient(
        barcode = aBarcode(),
        prisonCode = "BXI",
        prisonNumber = "A1234BC",
        name = "John Smith",
        dob = null
      )
      val expectedPrisonSearchRequest =
        PrisonerSearchRequest(prisonNumber = "A1234BC", firstName = "John", lastName = "Smith", dob = null)

      val prisonerSearchRequest = prisonerSearchRequest(barcodeRecipient)

      assertThat(prisonerSearchRequest).isEqualTo(expectedPrisonSearchRequest)
    }

    @Test
    fun `should create PrisonerSearchRequest given BarcodeRecipient with DOB`() {
      val barcodeRecipient = BarcodeRecipient(
        barcode = aBarcode(),
        prisonCode = "BXI",
        prisonNumber = null,
        name = "John Smith",
        dob = LocalDate.MIN
      )
      val expectedPrisonSearchRequest =
        PrisonerSearchRequest(prisonNumber = null, firstName = "John", lastName = "Smith", dob = LocalDate.MIN)

      val prisonerSearchRequest = prisonerSearchRequest(barcodeRecipient)

      assertThat(prisonerSearchRequest).isEqualTo(expectedPrisonSearchRequest)
    }

    @Test
    fun `should create PrisonerSearchRequest given BarcodeRecipient with name with many components`() {
      val barcodeRecipient = BarcodeRecipient(
        barcode = aBarcode(),
        prisonCode = "BXI",
        prisonNumber = "A1234BC",
        name = "John Bobby Smith",
        dob = null
      )
      val expectedPrisonSearchRequest =
        PrisonerSearchRequest(prisonNumber = "A1234BC", firstName = "John", lastName = "Bobby Smith", dob = null)

      val prisonerSearchRequest = prisonerSearchRequest(barcodeRecipient)

      assertThat(prisonerSearchRequest).isEqualTo(expectedPrisonSearchRequest)
    }

    @Test
    fun `should create PrisonerSearchRequest given BarcodeRecipient with name with just one component`() {
      val barcodeRecipient =
        BarcodeRecipient(barcode = aBarcode(), prisonCode = "BXI", prisonNumber = "A1234BC", name = "John", dob = null)
      val expectedPrisonSearchRequest =
        PrisonerSearchRequest(prisonNumber = "A1234BC", firstName = null, lastName = "John", dob = null)

      val prisonerSearchRequest = prisonerSearchRequest(barcodeRecipient)

      assertThat(prisonerSearchRequest).isEqualTo(expectedPrisonSearchRequest)
    }
  }

  private fun aBarcode() = Barcode("SOME_BARCODE")

  private fun aMatchPrisonersResponse() = MatchPrisonersResponse(
    matchedBy = ALL_SUPPLIED,
    matches = listOf(
      PrisonerMatch(
        Prisoner(
          prisonerNumber = "A1234BC",
          firstName = "John",
          lastName = "Smith",
          dateOfBirth = LocalDate.MIN,
          prisonId = "BXI",
          cellLocation = "2-2-015",
          status = "ACTIVE IN"
        )
      )
    )
  )
}
