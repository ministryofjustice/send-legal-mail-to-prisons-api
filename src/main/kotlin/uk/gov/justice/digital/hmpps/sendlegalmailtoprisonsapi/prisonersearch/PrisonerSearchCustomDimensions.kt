package uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.prisonersearch

import uk.gov.justice.digital.hmpps.prisonersearch.model.Prisoner

fun prisonerSearchSummaryCustomDimensions(
  dataSource: PrisonerSearchDataSource,
  searchType: PrisonerSearchType,
  numberOfResults: Int,
  exactMatchCount: Int,
  aliasExactMatchCount: Int
): Map<String, String?> =
  mapOf(
    "dataSource" to dataSource.toString(),
    "searchType" to searchType.toString(),
    "numberOfResults" to numberOfResults.toString(),
    "exactMatchCount" to exactMatchCount.toString(),
    "aliasExactMatchCount" to aliasExactMatchCount.toString(),
    "singleResultIdentified" to (exactMatchCount + aliasExactMatchCount == 1).toString()
  )

fun prisonerSearchBestMatchCustomDimensions(prisoner: Prisoner, caseload: String): Map<String, String?> =
  mapOf(
    "status" to prisoner.status,
    "inOutStatus" to prisoner.inOutStatus?.value,
    "legalStatus" to prisoner.legalStatus?.value,
    "lastMovementTypeCode" to prisoner.lastMovementTypeCode,
    "lastMovementReasonCode" to prisoner.lastMovementReasonCode,
    "forwardingRequired" to (prisoner.prisonId != caseload).toString(),
    "hasCellLocation" to (!prisoner.cellLocation.isNullOrBlank()).toString()
  )

enum class PrisonerSearchDataSource {
  MATCH_PRISONERS, GLOBAL_SEARCH
}

enum class PrisonerSearchType {
  PRISON_NUMBER, NAME_DOB
}
