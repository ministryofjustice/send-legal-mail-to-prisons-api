package uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.prisonersearch

data class PrisonerSearchCustomDimensions(
  val dataSource: PrisonerSearchDataSource,
  val searchType: PrisonerSearchType,
  val numberOfResults: Int,
  val exactMatchCount: Int,
  val aliasExactMatchCount: Int
) {
  val singleResultIdentified: Boolean
    get() = exactMatchCount + aliasExactMatchCount == 1

  fun asMap(): Map<String, String> =
    mapOf(
      "dataSource" to dataSource.toString(),
      "searchType" to searchType.toString(),
      "numberOfResults" to numberOfResults.toString(),
      "exactMatchCount" to exactMatchCount.toString(),
      "aliasExactMatchCount" to aliasExactMatchCount.toString(),
      "singleResultIdentified" to singleResultIdentified.toString()
    )
}

enum class PrisonerSearchDataSource {
  MATCH_PRISONERS, GLOBAL_SEARCH
}

enum class PrisonerSearchType {
  PRISON_NUMBER, NAME_DOB
}
