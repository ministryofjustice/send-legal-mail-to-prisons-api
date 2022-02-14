package uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.barcode

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.contact.ContactService

@Service
class BarcodeRecipientService(
  private val barcodeRecipientRepository: BarcodeRecipientRepository,
  private val contactService: ContactService
) {
  fun saveBarcodeRecipient(barcode: Barcode, createBarcodeRequest: CreateBarcodeRequest): BarcodeRecipient =
    BarcodeRecipient(
      barcode = barcode,
      name = createBarcodeRequest.prisonerName,
      prisonCode = createBarcodeRequest.prisonId,
      prisonNumber = createBarcodeRequest.prisonNumber,
      dob = createBarcodeRequest.dob,
      contact = createBarcodeRequest.contactId?.let { contactId -> contactService.getContactById(contactId) }
    ).let {
      barcodeRecipientRepository.save(it)
    }
}
