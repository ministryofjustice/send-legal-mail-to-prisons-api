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
  private val prisonerSearchResultsProcessor = PrisonerSearchResultsProcessor(telemetryClient)

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
      "singleResultIdentified" to "true",
      "matchedBy" to "ALL_SUPPLIED"
    )
    val expectedBestMatchCustomDimensions = mapOf(
      "status" to null,
      "inOutStatus" to null,
      "legalStatus" to null,
      "lastMovementTypeCode" to null,
      "lastMovementReasonCode" to null,
      "forwardingRequired" to "true",
      "hasCellLocation" to "false"
    )

    prisonerSearchResultsProcessor.processSearchResults(prisonerMatches, prisonerSearchRequest, caseload)

    verify(telemetryClient).trackEvent("prisoner-search-summary", expectedCustomDimensions, null)
    verify(telemetryClient).trackEvent("prisoner-search-best-match", expectedBestMatchCustomDimensions, null)
  }

  @Test
  fun `should process search results from global search (PagePrisoner)`() {
    val pagePrisoner = PagePrisoner(
      content = listOf(
        Prisoner(restrictedPatient = false, prisonerNumber = "A1234BC", firstName = "John", lastName = "Smith")
      )
    )
    val expectedSummaryCustomDimensions = mapOf(
      "dataSource" to "GLOBAL_SEARCH",
      "searchType" to "PRISON_NUMBER",
      "numberOfResults" to "1",
      "exactMatchCount" to "1",
      "aliasExactMatchCount" to "0",
      "singleResultIdentified" to "true",
      "matchedBy" to null
    )
    val expectedBestMatchCustomDimensions = mapOf(
      "status" to null,
      "inOutStatus" to null,
      "legalStatus" to null,
      "lastMovementTypeCode" to null,
      "lastMovementReasonCode" to null,
      "forwardingRequired" to "true",
      "hasCellLocation" to "false"
    )

    prisonerSearchResultsProcessor.processSearchResults(pagePrisoner, prisonerSearchRequest, caseload)

    verify(telemetryClient).trackEvent("prisoner-search-summary", expectedSummaryCustomDimensions, null)
    verify(telemetryClient).trackEvent("prisoner-search-best-match", expectedBestMatchCustomDimensions, null)
  }
}
