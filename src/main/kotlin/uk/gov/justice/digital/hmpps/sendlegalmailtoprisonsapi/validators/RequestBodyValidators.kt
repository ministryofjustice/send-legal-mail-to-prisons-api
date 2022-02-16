package uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.validators

import uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.barcode.CreateBarcodeRequest
import uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.config.MalformedRequest
import uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.config.ValidationException
import uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.contact.ContactRequest

fun validateRequestHasDobOrPrisonNumber(requestBody: ContactRequest) {
  if (requestBody.dob == null && requestBody.prisonNumber.isNullOrEmpty()) {
    throw ValidationException(MalformedRequest)
  }
}

fun validateRequestHasDobOrPrisonNumber(requestBody: CreateBarcodeRequest) {
  if (requestBody.dob == null && requestBody.prisonNumber.isNullOrEmpty()) {
    throw ValidationException(MalformedRequest)
  }
}
