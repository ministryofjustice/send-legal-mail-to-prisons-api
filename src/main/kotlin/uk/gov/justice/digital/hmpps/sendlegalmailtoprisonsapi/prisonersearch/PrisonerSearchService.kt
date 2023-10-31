package uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.prisonersearch

import org.springframework.stereotype.Service
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
    prisonerSearchClient.matchPrisoners(prisonerSearchRequest).subscribe {
      prisonerSearchResultsProcessor.processSearchResults(it, prisonerSearchRequest, caseload)?.let { prisoner ->
        forwardingNeeded = forwardingNeeded || (prisoner.prisonId == caseload)
      }
    }
    prisonerSearchClient.globalSearch(prisonerSearchRequest).subscribe {
      prisonerSearchResultsProcessor.processSearchResults(it, prisonerSearchRequest, caseload)?.let { prisoner ->
        forwardingNeeded = forwardingNeeded || (prisoner.prisonId == caseload)
      }
    }

    return forwardingNeeded
  }
}
