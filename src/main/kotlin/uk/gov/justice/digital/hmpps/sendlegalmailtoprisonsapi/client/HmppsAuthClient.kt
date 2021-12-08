package uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.client

import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono

@Component
class HmppsAuthClient(private val hmppsAuthWebClient: WebClient) {

  fun getUserDetails() =
    hmppsAuthWebClient.get()
      .uri("/api/user/me")
      .accept(MediaType.APPLICATION_JSON)
      .retrieve()
      .bodyToMono<UserDetails>()
      .block()!!
}

data class UserDetails(val activeCaseLoadId: String)
