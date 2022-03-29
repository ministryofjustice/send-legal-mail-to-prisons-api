package uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.prisonersearch

import mu.KotlinLogging
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.prisonersearch.model.Prisoner
import uk.gov.justice.digital.hmpps.prisonersearch.model.PrisonerMatch
import uk.gov.justice.digital.hmpps.prisonersearch.model.PrisonerMatches
import uk.gov.justice.digital.hmpps.prisonersearch.model.PrisonerMatches.MatchedBy.aLLSUPPLIED
import uk.gov.justice.digital.hmpps.prisonersearch.model.PrisonerMatches.MatchedBy.aLLSUPPLIEDALIAS
import uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.barcode.BarcodeRecipient
import uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.security.UserContext

private val log = KotlinLogging.logger {}

@Service
class PrisonerSearchService(private val prisonerSearchClient: PrisonerSearchClient, private val userContext: UserContext) {
  fun lookupPrisoner(barcodeRecipient: BarcodeRecipient) {
    val prisonerSearchRequest = PrisonerSearchRequest(barcodeRecipient)
    prisonerSearchClient.matchPrisoners(prisonerSearchRequest)
      .subscribe { processPrisonerMatches(prisonerSearchRequest, it) }
  }

  private fun processPrisonerMatches(prisonerSearchRequest: PrisonerSearchRequest, prisonerMatches: PrisonerMatches) {
    with(prisonerMatches) {

      if (matches.isEmpty()) {
        log.info { "/match-prisoners returned 0 potential prisoner matches" }
        return
      }

      var bestMatch: Prisoner? = null
      log.info { "/match-prisoners returned ${matches.size} potential prisoner matches by ${matchedBy.value}" }
      if (matches.size == 1) {
        bestMatch = matches[0].prisoner
      } else if (matches.size > 1) {
        // Find what we think is the best match
        if (this.matchedBy == aLLSUPPLIED || this.matchedBy == aLLSUPPLIEDALIAS) {
          // eg: we searched for a name and DOB, and /match-prisoners found n records that matched all criteria
          bestMatch = matches.bestMatchBasedOnCurrentPrisonId()
        } else {
          // eg: we searched for a name and DOB, and /match-prisoners found n records that matched some of the criteria
          // Our logic to find the best match will likely have to do more than match on prisonId, but this should do for now
          bestMatch = matches.bestMatchBasedOnCurrentPrisonId()
        }
      }

      if (bestMatch != null) {
        log.info { "Best matched prisoner: prisonNumber matched: ${bestMatch.prisonerNumber == prisonerSearchRequest.prisonNumber}" }
        log.info { "Best matched prisoner: Forwarding not required (prisonId matched): ${bestMatch.prisonId == userContext.caseload}" }
        log.info { "Best matched prisoner: Has cell location: ${!bestMatch.cellLocation.isNullOrBlank()}" }
      } else {
        log.info { "/match-prisoners returned ${matches.size} potential matches but we could not filter it down to a best match" }
      }
    }
  }

  private fun List<PrisonerMatch>.bestMatchBasedOnCurrentPrisonId(): Prisoner? =
    this.filter { prisonerMatch -> prisonerMatch.prisoner.prisonId == userContext.caseload }
      .also {
        log.info { "Filtered ${this.size} potential matches down to ${it.size} based on prisonId" }
      }
      .takeIf { it.size == 1 }
      ?.first()?.prisoner
}
