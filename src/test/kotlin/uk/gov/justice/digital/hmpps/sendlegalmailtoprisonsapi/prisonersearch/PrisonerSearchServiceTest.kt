package uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.prisonersearch

import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.given
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.springframework.web.reactive.function.client.WebClientResponseException.create
import reactor.core.publisher.Mono
import uk.gov.justice.digital.hmpps.prisonersearch.model.PagePrisoner
import uk.gov.justice.digital.hmpps.prisonersearch.model.Prisoner
import uk.gov.justice.digital.hmpps.prisonersearch.model.PrisonerMatch
import uk.gov.justice.digital.hmpps.prisonersearch.model.PrisonerMatches
import uk.gov.justice.digital.hmpps.prisonersearch.model.PrisonerMatches.MatchedBy.aLLSUPPLIED
import uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.barcode.Barcode
import uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.barcode.BarcodeRecipient
import uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.security.UserContext
import java.time.LocalDate

class PrisonerSearchServiceTest {
  val prisonerSearchClient = mock<PrisonerSearchClient>()
  val prisonerSearchResultsProcessor = mock<PrisonerSearchResultsProcessor>()
  val userContext = mock<UserContext>()
  val prisonerSearchService = PrisonerSearchService(prisonerSearchClient, prisonerSearchResultsProcessor, userContext)

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
      given { prisonerSearchClient.globalSearch(any()) }.willReturn(
        Mono.just(aGlobalSearchResponse())
      )
      given { userContext.caseload }.willReturn("BXI")
      val expectedPrisonerSearchRequest = PrisonerSearchRequest("A1234BC", "John", "Smith", null)

      prisonerSearchService.lookupPrisoner(barcodeRecipient)

      verify(prisonerSearchResultsProcessor).processSearchResults(aMatchPrisonersResponse(), expectedPrisonerSearchRequest, "BXI")
      verify(prisonerSearchResultsProcessor).processSearchResults(aGlobalSearchResponse(), expectedPrisonerSearchRequest, "BXI")
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
      given { prisonerSearchClient.globalSearch(any()) }.willReturn(
        Mono.error(create(403, "Forbidden", null, null, null))
      )

      prisonerSearchService.lookupPrisoner(barcodeRecipient)

      verifyNoInteractions(prisonerSearchResultsProcessor)
    }
  }

  private fun aBarcode() = Barcode("SOME_BARCODE")

  private fun aMatchPrisonersResponse() = PrisonerMatches(
    matchedBy = aLLSUPPLIED,
    matches = listOf(
      PrisonerMatch(
        Prisoner(
          prisonerNumber = "A1234BC",
          firstName = "John",
          lastName = "Smith",
          dateOfBirth = LocalDate.MIN,
          prisonId = "BXI",
          cellLocation = "2-2-015",
          status = "ACTIVE IN",
          restrictedPatient = false
        )
      )
    )
  )

  private fun aGlobalSearchResponse() = PagePrisoner(
    totalElements = 1,
    totalPages = 1,
    content = listOf(
      Prisoner(
        prisonerNumber = "A1234BC",
        firstName = "John",
        lastName = "Smith",
        dateOfBirth = LocalDate.MIN,
        prisonId = "BXI",
        cellLocation = "2-2-015",
        status = "ACTIVE IN",
        restrictedPatient = false
      )
    )
  )
}
