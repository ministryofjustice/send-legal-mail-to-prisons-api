package uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.barcode

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.check
import org.mockito.kotlin.given
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.contact.Contact
import uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.contact.ContactService
import java.time.Instant

class BarcodeRecipientServiceTest {

  val barcodeRecipientRepository = mock<BarcodeRecipientRepository>()
  val contactService = mock<ContactService>()
  val barcodeRecipientService = BarcodeRecipientService(barcodeRecipientRepository, contactService)

  @Nested
  inner class SaveBarcodeRecipient {
    @Test
    fun `should save barcode recipient with looked up contact`() {
      val barcode = Barcode("SOME_BARCODE")
      val createBarcodeRequest = CreateBarcodeRequest(prisonerName = "Fred Bloggs", prisonId = "BXI", prisonNumber = "A1234BC", contactId = 1234)

      val contact = Contact(id = 1, owner = "a-user@cjsm.net", name = "Fred Bloggs", prisonCode = "BXI", prisonNumber = "A1234BC", created = Instant.now(), updated = Instant.now())
      given { contactService.getContactById(any()) }.willReturn(contact)

      val expectedBarcodeRecipient = BarcodeRecipient(
        barcode = barcode,
        name = "Fred Bloggs",
        prisonCode = "BXI",
        prisonNumber = "A1234BC",
        contact = contact
      )
      given { barcodeRecipientRepository.save(any()) }.willReturn(expectedBarcodeRecipient)

      val barcodeRecipient = barcodeRecipientService.saveBarcodeRecipient(barcode, createBarcodeRequest)

      assertThat(barcodeRecipient).isEqualTo(expectedBarcodeRecipient)
      verify(barcodeRecipientRepository).save(
        check<BarcodeRecipient> {
          assertThat(it).usingRecursiveComparison().isEqualTo(expectedBarcodeRecipient)
        }
      )
      verify(contactService).getContactById(1234)
    }

    @Test
    fun `should save barcode recipient without looked up contact`() {
      val barcode = Barcode("SOME_BARCODE")
      val createBarcodeRequest = CreateBarcodeRequest(prisonerName = "Fred Bloggs", prisonId = "BXI", prisonNumber = "A1234BC")

      val expectedBarcodeRecipient = BarcodeRecipient(
        barcode = barcode,
        name = "Fred Bloggs",
        prisonCode = "BXI",
        prisonNumber = "A1234BC",
        contact = null
      )
      given { barcodeRecipientRepository.save(any()) }.willReturn(expectedBarcodeRecipient)

      val barcodeRecipient = barcodeRecipientService.saveBarcodeRecipient(barcode, createBarcodeRequest)

      assertThat(barcodeRecipient).isEqualTo(expectedBarcodeRecipient)
      verify(barcodeRecipientRepository).save(
        check<BarcodeRecipient> {
          assertThat(it).usingRecursiveComparison().isEqualTo(expectedBarcodeRecipient)
        }
      )
      verifyNoInteractions(contactService)
    }
  }
}
