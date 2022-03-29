package uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.prisonersearch

import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono
import reactor.core.publisher.Mono
import uk.gov.justice.digital.hmpps.prisonersearch.model.PrisonerMatches

@Component
class PrisonerSearchClient(private val prisonerSearchWebClient: WebClient) {
  fun matchPrisoners(prisonerSearchRequest: PrisonerSearchRequest): Mono<PrisonerMatches> {
    return prisonerSearchWebClient
      .post()
      .uri("/match-prisoners")
      .accept(MediaType.APPLICATION_JSON)
      .bodyValue(prisonerSearchRequest.toRequestBody())
      .retrieve()
      .bodyToMono()
  }
}
