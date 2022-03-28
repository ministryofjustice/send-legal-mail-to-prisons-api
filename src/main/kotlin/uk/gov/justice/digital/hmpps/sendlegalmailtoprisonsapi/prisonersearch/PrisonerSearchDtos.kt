package uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.prisonersearch

import uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.barcode.BarcodeRecipient
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.stream.Collectors

data class PrisonerSearchRequest(
  val prisonNumber: String? = null,
  val firstName: String?,
  val lastName: String,
  val dob: LocalDate? = null
) {
  companion object {
    fun fromBarcodeRecipient(barcodeRecipient: BarcodeRecipient): PrisonerSearchRequest {
      val names = barcodeRecipient.name.split(" ")
      val firstName: String? = names.firstElementIfMoreThan1ElementElseNull()
      val lastName: String = names.secondAndBeyondElementsIfMoreThan1ElementElseAllElements()
      return PrisonerSearchRequest(barcodeRecipient.prisonNumber, firstName, lastName, barcodeRecipient.dob)
    }

    private fun List<String>.firstElementIfMoreThan1ElementElseNull(): String? =
      if (this.size > 1) this[0] else null

    private fun List<String>.secondAndBeyondElementsIfMoreThan1ElementElseAllElements(): String =
      if (this.size > 1) this.stream().skip(1).collect(Collectors.joining(" ")) else this.stream().collect(Collectors.joining(" "))
  }

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
