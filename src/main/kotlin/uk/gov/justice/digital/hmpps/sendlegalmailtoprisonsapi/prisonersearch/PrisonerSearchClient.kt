package uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.prisonersearch

import mu.KotlinLogging
import org.springframework.http.HttpStatus.FORBIDDEN
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import org.springframework.web.reactive.function.client.bodyToMono
import reactor.core.publisher.Mono
import uk.gov.justice.digital.hmpps.prisonersearch.model.PagePrisoner
import uk.gov.justice.digital.hmpps.prisonersearch.model.PrisonerMatches

private val log = KotlinLogging.logger {}

@Component
class PrisonerSearchClient(private val prisonerSearchWebClient: WebClient) {
  fun matchPrisoners(prisonerSearchRequest: PrisonerSearchRequest): Mono<PrisonerMatches> {
    return prisonerSearchWebClient
      .post()
      .uri("/match-prisoners")
      .accept(MediaType.APPLICATION_JSON)
      .bodyValue(prisonerSearchRequest.toMatchPrisonersRequestBody())
      .retrieve()
      .bodyToMono<PrisonerMatches>()
      .onErrorResume { webClientErrorHandler(it) }
    // In theory this is a possible way to submit the request on a different thread ... but it can't get the auth token because
    // `userContext` is on the main request thread, so [WebClientConfiguration#addAuthHeaderFilterFunction] results in a NPE
    // Needs some thought if we want to do this .... (other concurrency approaches may suffer the same fate if they use a different thread)
    // .subscribeOn(Schedulers.parallel())
  }

  fun globalSearch(prisonerSearchRequest: PrisonerSearchRequest): Mono<PagePrisoner> = prisonerSearchWebClient
    .post()
    .uri("/global-search")
    .accept(MediaType.APPLICATION_JSON)
    .bodyValue(prisonerSearchRequest.toGlobalSearchRequestBody())
    .retrieve()
    .bodyToMono<PagePrisoner>()
    .onErrorResume { webClientErrorHandler(it) }

  private fun <API_RESPONSE_BODY_TYPE : Any> webClientErrorHandler(exception: Throwable): Mono<API_RESPONSE_BODY_TYPE> = with(exception) {
    if (this is WebClientResponseException) {
      val uriPath = request?.uri?.path
      if (statusCode == FORBIDDEN) {
        log.info { "Client token does not have correct role to call prisoner-offender-search $uriPath" }
      } else {
        log.error { "Failed to call prisoner-offender-search $uriPath [statusCode: $statusCode, body: ${this.responseBodyAsString}]" }
      }
    } else {
      log.error("Failed to call prisoner-offender-search", exception)
    }
  }
    .let { Mono.empty() }
}
