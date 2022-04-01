package uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.prisonersearch

import com.microsoft.applicationinsights.TelemetryClient
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import uk.gov.justice.digital.hmpps.prisonersearch.model.PagePrisoner
import uk.gov.justice.digital.hmpps.prisonersearch.model.Prisoner
import uk.gov.justice.digital.hmpps.prisonersearch.model.PrisonerMatch
import uk.gov.justice.digital.hmpps.prisonersearch.model.PrisonerMatches
import uk.gov.justice.digital.hmpps.prisonersearch.model.PrisonerMatches.MatchedBy.aLLSUPPLIED

class PrisonerSearchResultsProcessorTest {

  private val telemetryClient = mock<TelemetryClient>()
  private val prisonerSearchResultsLogger = mock<PrisonerSearchResultsLogger>()
  private val prisonerSearchResultsProcessor = PrisonerSearchResultsProcessor(telemetryClient, prisonerSearchResultsLogger)

  val prisonerSearchRequest = PrisonerSearchRequest("A1234BC", "John", "Smith", null)
  val caseload = "BXI"

  @Test
  fun `should process search results from match prisoners (PrisonerMatches)`() {
    val prisonerMatches = PrisonerMatches(
      matchedBy = aLLSUPPLIED,
      matches = listOf(
        PrisonerMatch(Prisoner(restrictedPatient = false, prisonerNumber = "A1234BC", firstName = "John", lastName = "Smith"))
      )
    )
    val expectedCustomDimensions = mapOf(
      "dataSource" to "MATCH_PRISONERS",
      "searchType" to "PRISON_NUMBER",
      "numberOfResults" to "1",
      "exactMatchCount" to "1",
      "aliasExactMatchCount" to "0",
      "singleResultIdentified" to "true"
    )

    prisonerSearchResultsProcessor.processSearchResults(prisonerMatches, prisonerSearchRequest, caseload)

    verify(telemetryClient).trackEvent("prisoner-search", expectedCustomDimensions, null)
    verify(prisonerSearchResultsLogger).logResults(prisonerMatches, prisonerSearchRequest, caseload)
  }

  @Test
  fun `should process search results from global search (PagePrisoner)`() {
    val pagePrisoner = PagePrisoner(
      content = listOf(
        Prisoner(restrictedPatient = false, prisonerNumber = "A1234BC", firstName = "John", lastName = "Smith")
      )
    )
    val expectedCustomDimensions = mapOf(
      "dataSource" to "GLOBAL_SEARCH",
      "searchType" to "PRISON_NUMBER",
      "numberOfResults" to "1",
      "exactMatchCount" to "1",
      "aliasExactMatchCount" to "0",
      "singleResultIdentified" to "true"
    )

    prisonerSearchResultsProcessor.processSearchResults(pagePrisoner, prisonerSearchRequest, caseload)

    verify(telemetryClient).trackEvent("prisoner-search", expectedCustomDimensions, null)
    verify(prisonerSearchResultsLogger).logResults(pagePrisoner, prisonerSearchRequest, caseload)
  }
}
