package uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.prisonersearch

import com.microsoft.applicationinsights.TelemetryClient
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.prisonersearch.model.PagePrisoner
import uk.gov.justice.digital.hmpps.prisonersearch.model.Prisoner
import uk.gov.justice.digital.hmpps.prisonersearch.model.PrisonerMatches
import uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.prisonersearch.PrisonerSearchDataSource.GLOBAL_SEARCH
import uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.prisonersearch.PrisonerSearchDataSource.MATCH_PRISONERS
import uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.prisonersearch.PrisonerSearchType.NAME_DOB
import uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.prisonersearch.PrisonerSearchType.PRISON_NUMBER

@Component
class PrisonerSearchResultsProcessor(
  private val telemetryClient: TelemetryClient,
  private val prisonerSearchResultsLogger: PrisonerSearchResultsLogger,
) {

  fun processSearchResults(
    prisonerMatches: PrisonerMatches,
    prisonerSearchRequest: PrisonerSearchRequest,
    caseload: String
  ) {
    prisonerSearchResultsLogger.logResults(prisonerMatches, prisonerSearchRequest, caseload)

    val prisoners = prisonerMatches.matches.map { it.prisoner }
    val prisonerMatchCount = PrisonerMatchCount.of(prisoners, prisonerSearchRequest)
    val customDimensions = PrisonerSearchCustomDimensions(
      dataSource = MATCH_PRISONERS,
      searchType = prisonerSearchRequest.searchType,
      numberOfResults = prisoners.size,
      exactMatchCount = prisonerMatchCount.mainDetailsCount,
      aliasExactMatchCount = prisonerMatchCount.aliasDetailsCount
    ).asMap()

    telemetryClient.trackEvent("prisoner-search", customDimensions, null)
  }

  fun processSearchResults(
    pagePrisoner: PagePrisoner,
    prisonerSearchRequest: PrisonerSearchRequest,
    caseload: String
  ) {
    prisonerSearchResultsLogger.logResults(pagePrisoner, prisonerSearchRequest, caseload)

    val prisoners = pagePrisoner.content ?: emptyList()
    val prisonerMatchCount = PrisonerMatchCount.of(prisoners, prisonerSearchRequest)
    val customDimensions = PrisonerSearchCustomDimensions(
      dataSource = GLOBAL_SEARCH,
      searchType = prisonerSearchRequest.searchType,
      numberOfResults = prisoners.size,
      exactMatchCount = prisonerMatchCount.mainDetailsCount,
      aliasExactMatchCount = prisonerMatchCount.aliasDetailsCount
    ).asMap()

    telemetryClient.trackEvent("prisoner-search", customDimensions, null)
  }
}

private val PrisonerSearchRequest.searchType: PrisonerSearchType
  get() = if (prisonNumber != null) PRISON_NUMBER else NAME_DOB

data class PrisonerMatchCount(
  val mainDetailsCount: Int,
  val aliasDetailsCount: Int
) {
  companion object {
    fun of(prisoners: List<Prisoner>, prisonerSearchRequest: PrisonerSearchRequest): PrisonerMatchCount {
      val totalPrisoners = prisoners.size

      if (prisonerSearchRequest.searchType == PRISON_NUMBER) {
        // Count the prisoners with a match on prison number & main name, or prison number & alias name
        val prisonersNotMatchingMainDetails = prisoners.filterNot {
          it.prisonerNumber == prisonerSearchRequest.prisonNumber &&
            it.firstName?.uppercase() == prisonerSearchRequest.firstName?.uppercase() && it.lastName?.uppercase() == prisonerSearchRequest.lastName.uppercase()
        }
        return PrisonerMatchCount(
          mainDetailsCount = totalPrisoners - prisonersNotMatchingMainDetails.size,
          aliasDetailsCount = prisonersNotMatchingMainDetails.count {
            it.aliases?.any { alias ->
              it.prisonerNumber == prisonerSearchRequest.prisonNumber &&
                alias.firstName.uppercase() == prisonerSearchRequest.firstName?.uppercase() && alias.lastName.uppercase() == prisonerSearchRequest.lastName.uppercase()
            } == true
          }
        )
      } else {
        // Count the prisoners with a match on main name and DOB, or alias name and DOB
        val prisonersNotMatchingMainDetails = prisoners.filterNot {
          it.firstName?.uppercase() == prisonerSearchRequest.firstName?.uppercase() && it.lastName?.uppercase() == prisonerSearchRequest.lastName.uppercase() &&
            it.dateOfBirth == prisonerSearchRequest.dob
        }
        return PrisonerMatchCount(
          mainDetailsCount = totalPrisoners - prisonersNotMatchingMainDetails.size,
          aliasDetailsCount = prisonersNotMatchingMainDetails.count {
            it.aliases?.any { alias ->
              alias.firstName.uppercase() == prisonerSearchRequest.firstName?.uppercase() && alias.lastName.uppercase() == prisonerSearchRequest.lastName.uppercase() &&
                alias.dateOfBirth == prisonerSearchRequest.dob
            } == true
          }
        )
      }
    }
  }
}
