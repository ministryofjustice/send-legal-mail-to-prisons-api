package uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.prisonersearch

import uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.barcode.BarcodeRecipient
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.stream.Collectors

data class PrisonerSearchRequest(
  val prisonNumber: String? = null,
  val firstName: String?,
  val lastName: String,
  val dob: LocalDate? = null,
) {
  constructor(barcodeRecipient: BarcodeRecipient) : this(
    barcodeRecipient.prisonNumber,
    barcodeRecipient.names.first,
    barcodeRecipient.names.second,
    barcodeRecipient.dob,
  )

  fun toMatchPrisonersRequestBody(): Map<String, String?> =
    mapOf(
      "nomsNumber" to prisonNumber,
      "firstName" to firstName,
      "lastName" to lastName,
      "dateOfBirth" to dob?.format(DateTimeFormatter.ISO_LOCAL_DATE),
    )

  fun toGlobalSearchRequestBody(): Map<String, String?> =
    mapOf(
      "prisonerIdentifier" to prisonNumber,
      "firstName" to firstName,
      "lastName" to lastName,
      "dateOfBirth" to dob?.format(DateTimeFormatter.ISO_LOCAL_DATE),
      "includeAliases" to "true",
    )
}

private val BarcodeRecipient.names: Pair<String?, String>
  get() = name.split(" ")
    .let {
      if (it.size > 1) {
        it[0] to it.stream().skip(1).collect(Collectors.joining(" "))
      } else {
        null to it.stream().collect(Collectors.joining(" "))
      }
    }
