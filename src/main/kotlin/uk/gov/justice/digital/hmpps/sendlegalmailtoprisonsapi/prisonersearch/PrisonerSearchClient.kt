package uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.prisonersearch

import com.fasterxml.jackson.annotation.JsonFormat
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono
import reactor.core.publisher.Mono
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Component
class PrisonerSearchClient(private val prisonerSearchWebClient: WebClient) {
  fun matchPrisoners(prisonerSearchRequest: PrisonerSearchRequest): Mono<MatchPrisonersResponse> {
    return prisonerSearchWebClient
      .post()
      .uri("/match-prisoners")
      .accept(MediaType.APPLICATION_JSON)
      .bodyValue(prisonerSearchRequest.toRequestBody())
      .retrieve()
      .bodyToMono()
  }
}

data class MatchPrisonersResponse(
  val matches: List<PrisonerMatch>,
  val matchedBy: MatchedBy
)

data class PrisonerMatch(
  val prisoner: Prisoner
)

data class Prisoner(
  val prisonerNumber: String,
  val firstName: String,
  val lastName: String,
  @field:JsonFormat(pattern = "yyyy-MM-dd")
  val dateOfBirth: LocalDate,
  val prisonId: String,
  val cellLocation: String,
  val status: String
)

enum class MatchedBy {
  ALL_SUPPLIED, ALL_SUPPLIED_ALIAS, HMPPS_KEY, EXTERNAL_KEY, NAME, PARTIAL_NAME, PARTIAL_NAME_DOB_LENIENT, NOTHING;
}

data class PrisonerSearchRequest(
  val prisonNumber: String? = null,
  val firstName: String?,
  val lastName: String,
  val dob: LocalDate? = null
) {
  fun toRequestBody(): Map<String, String?> =
    mapOf(
      "nomsNumber" to prisonNumber,
      "firstName" to firstName,
      "lastName" to lastName,
      "dateOfBirth" to dob?.format(DateTimeFormatter.ISO_LOCAL_DATE)
    )
}

data class PrisonerSearchResult(
  var prisonNumber: String,
  var status: String,
  var cellLocation: String,
  var prisonId: String
)
