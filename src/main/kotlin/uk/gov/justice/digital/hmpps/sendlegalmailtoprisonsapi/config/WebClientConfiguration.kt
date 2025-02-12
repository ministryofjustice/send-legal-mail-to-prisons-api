package uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpHeaders
import org.springframework.web.reactive.function.client.ClientRequest
import org.springframework.web.reactive.function.client.ExchangeFilterFunction
import org.springframework.web.reactive.function.client.ExchangeFunction
import org.springframework.web.reactive.function.client.WebClient
import uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.security.UserContext

@Configuration
class WebClientConfiguration(
  @Value("\${manage.users.api.url}") private val manageUsersApiUrl: String,
  @Value("\${prisoner.search.url}") private val prisonerSearchApiUrl: String,
  @Value("\${prison.register.url}") private val prisonRegisterApiUrl: String,
  private val userContext: UserContext,
) {
  @Bean
  fun manageUsersApiWebClient(): WebClient = WebClient.builder()
    .baseUrl(manageUsersApiUrl)
    .filter(addAuthHeaderFilterFunction())
    .build()

  @Bean
  fun manageUsersApiHealthWebClient(): WebClient = WebClient.builder().baseUrl(manageUsersApiUrl).build()

  @Bean
  fun prisonerSearchWebClient(): WebClient = WebClient.builder()
    .baseUrl(prisonerSearchApiUrl)
    .filter(addAuthHeaderFilterFunction())
    .build()

  @Bean
  fun prisonRegisterWebClient(): WebClient = WebClient.builder()
    .baseUrl(prisonRegisterApiUrl)
    .build()

  private fun addAuthHeaderFilterFunction(): ExchangeFilterFunction = ExchangeFilterFunction { request: ClientRequest, next: ExchangeFunction ->
    val filtered = ClientRequest.from(request)
      .header(HttpHeaders.AUTHORIZATION, userContext.authToken)
      .build()
    next.exchange(filtered)
  }
}
