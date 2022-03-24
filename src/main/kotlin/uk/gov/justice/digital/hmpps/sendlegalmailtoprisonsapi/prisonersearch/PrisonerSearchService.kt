package uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.prisonersearch

import mu.KotlinLogging
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClientResponseException
import uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.barcode.BarcodeRecipient
import java.util.stream.Collectors

private val log = KotlinLogging.logger {}

@Service
class PrisonerSearchService(private val prisonerSearchClient: PrisonerSearchClient) {
  fun lookupPrisoner(barcodeRecipient: BarcodeRecipient) {
    val prisonerSearchRequest = prisonerSearchRequest(barcodeRecipient)
    try {
      val matchPrisonersResponse = prisonerSearchClient.matchPrisoners(prisonerSearchRequest)
      log.info { "Found ${matchPrisonersResponse.matches.size} potential prisoner matches" }
    } catch (webClientResponseException: WebClientResponseException) {
      if (webClientResponseException.statusCode == HttpStatus.FORBIDDEN) {
        log.info { "Client token does not have correct role to call prisoner-search" }
      } else {
        log.error { "Failed to call prisoner-search - statusCode ${webClientResponseException.statusCode}" }
      }
    }
  }

  private fun prisonerSearchRequest(barcodeRecipient: BarcodeRecipient): PrisonerSearchRequest {
    val names = barcodeRecipient.name.split(" ")
    val firstName: String = names[0]
    val lastName: String? = if (names.size > 1) names.stream().skip(1).collect(Collectors.joining(" ")) else null
    return PrisonerSearchRequest(barcodeRecipient.prisonNumber, firstName, lastName, barcodeRecipient.dob)
  }
}
