package uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.prisonersearch

import mu.KotlinLogging
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.prisonersearch.model.PagePrisoner
import uk.gov.justice.digital.hmpps.prisonersearch.model.Prisoner
import uk.gov.justice.digital.hmpps.prisonersearch.model.PrisonerMatches

private val log = KotlinLogging.logger {}

@Component
class PrisonerSearchResultsLogger {

  fun logResults(
    prisonerMatches: PrisonerMatches,
    prisonerSearchRequest: PrisonerSearchRequest,
    caseload: String
  ) {
    log.info { "/match-prisoners returned ${prisonerMatches.matches.size} potential prisoner matches by ${prisonerMatches.matchedBy.value}" }
    prisonerMatches.matches.forEach { it.prisoner.logData(prisonerSearchRequest, caseload, "/match-prisoners") }
    prisonerMatches.matches.map { prisonerMatch -> prisonerMatch.prisoner }
      .logMatchingSummary(prisonerSearchRequest, "/match-prisoners")
  }

  fun logResults(
    pagePrisoner: PagePrisoner,
    prisonerSearchRequest: PrisonerSearchRequest,
    caseload: String
  ) {
    log.info { "/global-search found ${pagePrisoner.totalElements} potential prisoner matches" }
    log.info { "/global-search page 1 of ${pagePrisoner.totalPages}" }
    pagePrisoner.content?.forEach { it.logData(prisonerSearchRequest, caseload, "/global-search") }
    pagePrisoner.content?.logMatchingSummary(prisonerSearchRequest, "/global-search")
  }

  private fun Prisoner.logData(prisonerSearchRequest: PrisonerSearchRequest, caseload: String, prefix: String) {
    log.info { "$prefix prisoner: prisonNumber matched: ${if (prisonerSearchRequest.prisonNumber != null) prisonerSearchRequest.prisonNumber == prisonerNumber else "N/A"}" }
    log.info { "$prefix prisoner: Status: $status" }
    log.info { "$prefix prisoner: InOutStatus: $inOutStatus" }
    log.info { "$prefix prisoner: LegalStatus: $legalStatus" }
    log.info { "$prefix prisoner: LastMovementTypeCode: $lastMovementTypeCode" }
    log.info { "$prefix prisoner: LastMovementReasonCode: $lastMovementReasonCode" }
    log.info { "$prefix prisoner: Forwarding required (prisonId not matched): ${prisonId != caseload}" }
    log.info { "$prefix prisoner: Has cell location: ${!cellLocation.isNullOrBlank()}" }
    log.info { "$prefix prisoner: Has alias(es): ${aliases?.isNotEmpty() == true}" }
    log.info { "$prefix prisoner: DOB matched: ${if (prisonerSearchRequest.dob != null) mainDobMatches(prisonerSearchRequest) else "N/A"}" }
    log.info { "$prefix prisoner: DOB matched on alias: ${if (prisonerSearchRequest.dob != null) aliasDobMatches(prisonerSearchRequest) else "N/A"}" }
    log.info { "$prefix prisoner: Name matched (exact): ${mainNameMatches(prisonerSearchRequest)}" }
    log.info { "$prefix prisoner: Name matched on alias (exact): ${aliasNameMatches(prisonerSearchRequest)}" }
  }

  private fun List<Prisoner>.logMatchingSummary(prisonerSearchRequest: PrisonerSearchRequest, prefix: String) {
    if (isEmpty() || size > 1) {
      log.info { "$prefix summary - Did not return a single exact match for ${searchStyle(prisonerSearchRequest)} search" }
    } else {
      log.info { "$prefix summary - Returned a single prisoner that was${if (this[0].isExactMatch(prisonerSearchRequest)) "" else " not"} an exact match on ${searchStyle(prisonerSearchRequest)} search" }
    }
  }

  private fun Prisoner.isExactMatch(prisonerSearchRequest: PrisonerSearchRequest): Boolean =
    if (prisonerSearchRequest.prisonNumber != null) {
      // prison number search needs to match prison number and name (either main name or one of the aliases)
      prisonerSearchRequest.prisonNumber == prisonerNumber && (mainNameMatches(prisonerSearchRequest) || aliasNameMatches(prisonerSearchRequest))
    } else {
      // name + dob search needs to match name (either main name or one of the aliases) and dob (either main dob or one of the aliases)
      (mainNameMatches(prisonerSearchRequest) || aliasNameMatches(prisonerSearchRequest)) &&
        prisonerSearchRequest.dob != null && (mainDobMatches(prisonerSearchRequest) || aliasDobMatches(prisonerSearchRequest))
    }

  private fun searchStyle(prisonerSearchRequest: PrisonerSearchRequest): String =
    if (prisonerSearchRequest.prisonNumber != null) "prison number and name" else "name and dob"

  private fun Prisoner.mainNameMatches(prisonerSearchRequest: PrisonerSearchRequest): Boolean =
    prisonerSearchRequest.firstName?.uppercase() == firstName?.uppercase() && prisonerSearchRequest.lastName.uppercase() == lastName?.uppercase()

  private fun Prisoner.aliasNameMatches(prisonerSearchRequest: PrisonerSearchRequest): Boolean =
    aliases?.any { prisonerSearchRequest.firstName?.uppercase() == it.firstName.uppercase() && prisonerSearchRequest.lastName.uppercase() == it.lastName.uppercase() } == true

  private fun Prisoner.mainDobMatches(prisonerSearchRequest: PrisonerSearchRequest): Boolean =
    prisonerSearchRequest.dob == dateOfBirth

  private fun Prisoner.aliasDobMatches(prisonerSearchRequest: PrisonerSearchRequest): Boolean =
    aliases?.any { prisonerSearchRequest.dob == it.dateOfBirth } == true
}
