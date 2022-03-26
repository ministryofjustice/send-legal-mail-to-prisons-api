package uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.prisonersearch

import mu.KotlinLogging
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClientResponseException
import reactor.core.publisher.Mono
import uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.barcode.BarcodeRecipient
import java.util.stream.Collectors

private val log = KotlinLogging.logger {}

@Service
class PrisonerSearchService(private val prisonerSearchClient: PrisonerSearchClient) {
  fun lookupPrisoner(barcodeRecipient: BarcodeRecipient) {
    val prisonerSearchRequest = prisonerSearchRequest(barcodeRecipient)
    prisonerSearchClient.matchPrisoners(prisonerSearchRequest)
      .onErrorResume {
        with(it) {
          if (this is WebClientResponseException) {
            if (statusCode == HttpStatus.FORBIDDEN) {
              log.info { "Client token does not have correct role to call prisoner-search" }
            } else {
              log.error { "Failed to call prisoner-search - statusCode $statusCode" }
            }
          } else {
            log.error("Failed to call prisoner-search", it)
          }
        }
          .let { Mono.empty() }
      }
      // In theory this is a possible way to submit the request on a different thread ... but it can't get the auth token because
      // `userContext` is on the main request thread, so [WebClientConfiguration#addAuthHeaderFilterFunction] results in a NPE
      // Needs some thought if we want to do this .... (other concurrency approaches may suffer the same fate if they use a different thread)
      // .subscribeOn(Schedulers.parallel())
      .subscribe {
        log.info { "Found ${it.matches.size} potential prisoner matches" }
      }
  }
}

fun prisonerSearchRequest(barcodeRecipient: BarcodeRecipient): PrisonerSearchRequest {
  val names = barcodeRecipient.name.split(" ")
  val firstName: String? = names.firstElementIfMoreThan1ElementElseNull()
  val lastName: String = names.secondAndBeyondElementsIfMoreThan1ElementElseAllElements()
  return PrisonerSearchRequest(barcodeRecipient.prisonNumber, firstName, lastName, barcodeRecipient.dob)
}

private fun List<String>.firstElementIfMoreThan1ElementElseNull(): String? =
  if (this.size > 1) this[0] else null

private fun List<String>.secondAndBeyondElementsIfMoreThan1ElementElseAllElements(): String =
  if (this.size > 1) this.stream().skip(1).collect(Collectors.joining(" ")) else this.stream().collect(Collectors.joining(" "))
