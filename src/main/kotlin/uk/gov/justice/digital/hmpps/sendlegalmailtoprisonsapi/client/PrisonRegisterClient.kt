package uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.client

import org.springframework.http.HttpStatus.NOT_FOUND
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import org.springframework.web.reactive.function.client.bodyToMono
import reactor.core.publisher.Mono
import uk.gov.justice.digital.hmpps.prisonregister.model.PrisonDto

@Component
class PrisonRegisterClient(private val prisonRegisterWebClient: WebClient) {

  fun getPrison(prisonCode: String): PrisonDto? = prisonRegisterWebClient.get()
    .uri { it.path("/prisons/id/{id}").build(prisonCode) }
    .accept(MediaType.APPLICATION_JSON)
    .retrieve()
    .bodyToMono<PrisonDto>()
    .onErrorResume(WebClientResponseException::class.java) { emptyWhenNotFound(it) }
    .block()

  fun <T : WebClientResponseException> emptyWhenNotFound(exception: WebClientResponseException): Mono<T> = if (exception.statusCode == NOT_FOUND) Mono.empty() else Mono.error(exception)
}
