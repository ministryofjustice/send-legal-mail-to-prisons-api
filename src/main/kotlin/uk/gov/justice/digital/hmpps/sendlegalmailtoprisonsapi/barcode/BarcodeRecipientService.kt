package uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.barcode

import org.springframework.stereotype.Service

@Service
class BarcodeRecipientService(private val barcodeRecipientRepository: BarcodeRecipientRepository) {
  fun saveBarcodeRecipient(barcodeRecipient: BarcodeRecipient): BarcodeRecipient =
    barcodeRecipientRepository.save(barcodeRecipient)
}
