package uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.barcode

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.config.ResourceNotFoundException
import uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.contact.ContactRepository

@Service
class BarcodeRecipientService(
  private val barcodeRecipientRepository: BarcodeRecipientRepository,
  private val contactRepository: ContactRepository,
) {
  fun saveBarcodeRecipient(barcode: Barcode, createBarcodeRequest: CreateBarcodeRequest): BarcodeRecipient =
    BarcodeRecipient(
      barcode = barcode,
      name = createBarcodeRequest.prisonerName,
      prisonCode = createBarcodeRequest.prisonId,
      prisonNumber = createBarcodeRequest.prisonNumber,
      dob = createBarcodeRequest.dob,
      contact = createBarcodeRequest.contactId?.let { contactId -> contactRepository.getById(contactId) }
    ).let {
      barcodeRecipientRepository.save(it)
    }

  fun getBarcodeRecipient(barcode: Barcode): BarcodeRecipient =
    barcodeRecipientRepository.getByBarcode(barcode)
      ?: throw ResourceNotFoundException("Barcode recipient not found for barcode ${barcode.code}")
}
