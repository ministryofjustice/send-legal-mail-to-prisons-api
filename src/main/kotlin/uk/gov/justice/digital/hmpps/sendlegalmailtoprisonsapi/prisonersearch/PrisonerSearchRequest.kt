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
  constructor(barcodeRecipient: BarcodeRecipient) : this(
    barcodeRecipient.prisonNumber,
    barcodeRecipient.firstName,
    barcodeRecipient.lastName,
    barcodeRecipient.dob
  )

  fun toRequestBody(): Map<String, String?> =
    mapOf(
      "nomsNumber" to prisonNumber,
      "firstName" to firstName,
      "lastName" to lastName,
      "dateOfBirth" to dob?.format(DateTimeFormatter.ISO_LOCAL_DATE)
    )
}

private val BarcodeRecipient.firstName: String?
  get() = this.name.split(" ").firstElementIfMoreThan1ElementElseNull()

private val BarcodeRecipient.lastName: String
  get() = this.name.split(" ").secondAndBeyondElementsIfMoreThan1ElementElseAllElements()

private fun List<String>.firstElementIfMoreThan1ElementElseNull(): String? =
  if (this.size > 1) this[0] else null

private fun List<String>.secondAndBeyondElementsIfMoreThan1ElementElseAllElements(): String =
  if (this.size > 1) this.stream().skip(1).collect(Collectors.joining(" ")) else this.stream()
    .collect(Collectors.joining(" "))
