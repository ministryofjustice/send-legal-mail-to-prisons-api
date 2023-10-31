package uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.prisonersearch

import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import uk.gov.justice.digital.hmpps.prisonersearch.model.PagePrisoner
import uk.gov.justice.digital.hmpps.prisonersearch.model.PrisonerMatch
import uk.gov.justice.digital.hmpps.prisonersearch.model.PrisonerMatches
import uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.barcode.BarcodeRecipient
import uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.security.UserContext

@Service
class PrisonerSearchService(
  private val prisonerSearchClient: PrisonerSearchClient,
  private val prisonerSearchResultsProcessor: PrisonerSearchResultsProcessor,
  private val userContext: UserContext,
) {

  fun lookupPrisoner(barcodeRecipient: BarcodeRecipient): Boolean {
    val prisonerSearchRequest = PrisonerSearchRequest(barcodeRecipient)
    val caseload = userContext.caseload
    var forwardingNeeded = false
    // TODO - revert back later. added to check on dev env for the timebeing
    prisonerSearchClient.matchPrisoners(prisonerSearchRequest).onErrorResume {
      return@onErrorResume Mono.just(PrisonerMatches(listOf<PrisonerMatch>(), PrisonerMatches.MatchedBy.nOTHING))
    }.block()?.let {
      if (it.matches.isNotEmpty()) {
        prisonerSearchResultsProcessor.processSearchResults(it, prisonerSearchRequest, caseload)?.let { prisoner ->
          forwardingNeeded = (prisoner.prisonId != caseload)
        }
      }
    }

    prisonerSearchClient.globalSearch(prisonerSearchRequest).onErrorResume {
      return@onErrorResume Mono.just(PagePrisoner(totalElements = 0))
    }.block()?.let {
      if (it.totalElements != null && it.totalElements > 0) {
        prisonerSearchResultsProcessor.processSearchResults(it, prisonerSearchRequest, caseload)?.let { prisoner ->
          forwardingNeeded = forwardingNeeded || (prisoner.prisonId != caseload)
        }
      }
    }

    return forwardingNeeded
  }
}
