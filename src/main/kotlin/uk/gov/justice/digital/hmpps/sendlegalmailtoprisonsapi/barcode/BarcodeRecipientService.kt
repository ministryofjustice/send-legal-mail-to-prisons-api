package uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.barcode

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.contact.ContactService

@Service
class BarcodeRecipientService(
  private val barcodeRecipientRepository: BarcodeRecipientRepository,
  private val contactService: ContactService
) {
  fun saveBarcodeRecipient(barcode: Barcode, createBarcodeRequest: CreateBarcodeRequest?): BarcodeRecipient? =
    createBarcodeRequest?.let {
      val barcodeRecipient = BarcodeRecipient(
        barcode = barcode,
        name = it.prisonerName,
        prisonCode = it.prisonId,
        prisonNumber = it.prisonNumber,
        dob = it.dob,
        contact = it.contactId?.let { contactId -> contactService.getContactById(contactId) }
      )
      barcodeRecipientRepository.save(barcodeRecipient)
    }
}
