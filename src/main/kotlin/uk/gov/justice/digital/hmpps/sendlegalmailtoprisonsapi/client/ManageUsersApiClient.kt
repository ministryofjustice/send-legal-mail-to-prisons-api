package uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.client

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono

@Component
class ManageUsersApiClient(
  @Qualifier("manageUsersApiWebClient") private val webClient: WebClient,
) {
  fun getUserDetails() =
    webClient.get()
      .uri("/users/me")
      .accept(MediaType.APPLICATION_JSON)
      .retrieve()
      .bodyToMono<UserDetails>()
      .block()!!
}

data class UserDetails(val activeCaseLoadId: String? = null)
