package uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.prisonersearch

import com.microsoft.applicationinsights.TelemetryClient
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.prisonersearch.model.PagePrisoner
import uk.gov.justice.digital.hmpps.prisonersearch.model.Prisoner
import uk.gov.justice.digital.hmpps.prisonersearch.model.PrisonerAlias
import uk.gov.justice.digital.hmpps.prisonersearch.model.PrisonerMatches
import uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.prisonersearch.PrisonerSearchDataSource.GLOBAL_SEARCH
import uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.prisonersearch.PrisonerSearchDataSource.MATCH_PRISONERS
import uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.prisonersearch.PrisonerSearchType.NAME_DOB
import uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.prisonersearch.PrisonerSearchType.PRISON_NUMBER

@Component
class PrisonerSearchResultsProcessor(private val telemetryClient: TelemetryClient) {

  fun processSearchResults(
    prisonerMatches: PrisonerMatches,
    prisonerSearchRequest: PrisonerSearchRequest,
    caseload: String
  ) {
    val prisoners = prisonerMatches.matches.map { it.prisoner }
    val prisonerMatch = PrisonerMatch.of(prisoners, prisonerSearchRequest)

    with(
      prisonerSearchSummaryCustomDimensions(
        dataSource = MATCH_PRISONERS,
        searchType = prisonerSearchRequest.searchType,
        numberOfResults = prisoners.size,
        exactMatchCount = prisonerMatch.mainDetailsCount,
        aliasExactMatchCount = prisonerMatch.aliasDetailsCount
      )
    ) {
      telemetryClient.trackEvent("prisoner-search-summary", this, null)
    }

    prisonerMatch.bestMatch?.also {
      with(prisonerSearchBestMatchCustomDimensions(it, caseload)) {
        telemetryClient.trackEvent("prisoner-search-best-match", this, null)
      }
    }
  }

  fun processSearchResults(
    pagePrisoner: PagePrisoner,
    prisonerSearchRequest: PrisonerSearchRequest,
    caseload: String
  ) {
    val prisoners = pagePrisoner.content ?: emptyList()
    val prisonerMatch = PrisonerMatch.of(prisoners, prisonerSearchRequest)

    with(
      prisonerSearchSummaryCustomDimensions(
        dataSource = GLOBAL_SEARCH,
        searchType = prisonerSearchRequest.searchType,
        numberOfResults = prisoners.size,
        exactMatchCount = prisonerMatch.mainDetailsCount,
        aliasExactMatchCount = prisonerMatch.aliasDetailsCount
      )
    ) {
      telemetryClient.trackEvent("prisoner-search-summary", this, null)
    }

    prisonerMatch.bestMatch?.also {
      with(prisonerSearchBestMatchCustomDimensions(it, caseload)) {
        telemetryClient.trackEvent("prisoner-search-best-match", this, null)
      }
    }
  }
}

private val PrisonerSearchRequest.searchType: PrisonerSearchType
  get() = if (prisonNumber != null) PRISON_NUMBER else NAME_DOB

data class PrisonerMatch(
  val mainDetailsCount: Int,
  val aliasDetailsCount: Int,
  val bestMatch: Prisoner?
) {
  companion object {
    fun of(prisoners: List<Prisoner>, prisonerSearchRequest: PrisonerSearchRequest): PrisonerMatch {
      val totalPrisoners = prisoners.size

      if (prisonerSearchRequest.searchType == PRISON_NUMBER) {
        // Match prisoners on prison number & main name, or prison number & alias name
        val prisonersMatchingMainDetails = prisoners.filter {
          it.matchesName(prisonerSearchRequest) && it.matchesPrisonNumber(prisonerSearchRequest)
        }.toSet()
        val prisonersMatchingAliasDetails = (prisoners - prisonersMatchingMainDetails).filter {
          it.aliases?.any { alias ->
            it.matchesPrisonNumber(prisonerSearchRequest) && alias.matchesName(prisonerSearchRequest)
          } == true
        }

        return PrisonerMatch(
          mainDetailsCount = prisonersMatchingMainDetails.size,
          aliasDetailsCount = prisonersMatchingAliasDetails.size,
          bestMatch = bestPrisonerMatch(prisonersMatchingMainDetails, prisonersMatchingAliasDetails)
        )
      } else {
        // Match prisoners on main name and DOB, or alias name and DOB
        val prisonersMatchingMainDetails = prisoners.filter {
          it.matchesName(prisonerSearchRequest) && it.matchesDateOfBirth(prisonerSearchRequest)
        }.toSet()
        val prisonersMatchingAliasDetails = (prisoners - prisonersMatchingMainDetails).filter {
          it.aliases?.any { alias ->
            alias.matchesName(prisonerSearchRequest) && alias.matchesDateOfBirth(prisonerSearchRequest)
          } == true
        }

        return PrisonerMatch(
          mainDetailsCount = prisonersMatchingMainDetails.size,
          aliasDetailsCount = prisonersMatchingAliasDetails.size,
          bestMatch = bestPrisonerMatch(prisonersMatchingMainDetails, prisonersMatchingAliasDetails)
        )
      }
    }

    private fun Prisoner.matchesName(prisonerSearchRequest: PrisonerSearchRequest): Boolean =
      firstName?.uppercase() == prisonerSearchRequest.firstName?.uppercase() && lastName?.uppercase() == prisonerSearchRequest.lastName.uppercase()

    private fun Prisoner.matchesPrisonNumber(prisonerSearchRequest: PrisonerSearchRequest): Boolean =
      prisonerNumber == prisonerSearchRequest.prisonNumber

    private fun Prisoner.matchesDateOfBirth(prisonerSearchRequest: PrisonerSearchRequest): Boolean =
      dateOfBirth == prisonerSearchRequest.dob

    private fun PrisonerAlias.matchesName(prisonerSearchRequest: PrisonerSearchRequest): Boolean =
      firstName.uppercase() == prisonerSearchRequest.firstName?.uppercase() && lastName.uppercase() == prisonerSearchRequest.lastName.uppercase()

    private fun PrisonerAlias.matchesDateOfBirth(prisonerSearchRequest: PrisonerSearchRequest): Boolean =
      dateOfBirth == prisonerSearchRequest.dob

    private fun bestPrisonerMatch(prisonersMatchingMainDetails: Collection<Prisoner>, prisonersMatchingAliasDetails: Collection<Prisoner>): Prisoner? =
      if (prisonersMatchingMainDetails.size + prisonersMatchingAliasDetails.size != 1) {
        null
      } else {
        if (prisonersMatchingMainDetails.isNotEmpty()) prisonersMatchingMainDetails.first() else prisonersMatchingAliasDetails.first()
      }
  }
}
