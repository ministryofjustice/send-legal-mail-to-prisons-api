package uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.barcode

import org.assertj.core.api.Assertions.assertThatExceptionOfType
import org.junit.jupiter.api.Test
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.orm.jpa.JpaObjectRetrievalFailureException
import uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.IntegrationTest
import uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.barcode.BarcodeRecipientAssert.Companion.assertThat

class BarcodeRecipientRepositoryTest : IntegrationTest() {

  @Test
  fun `should save barcode recipient`() {
    val barcode = Barcode("123456789012")
    barcodeRepository.save(barcode)

    val barcodeRecipient = BarcodeRecipient(
      barcode = barcode,
      name = "Fred Bloggs",
      prisonCode = "BXI",
      prisonNumber = "A1234BC"
    )

    barcodeRecipientRepository.save(barcodeRecipient)

    assertThat(barcodeRecipientRepository.getByBarcode(barcode)).hasFieldsEqualToFieldsOf(barcodeRecipient)
  }

  @Test
  fun `should not save given barcode not previously saved in database`() {
    val barcodeNotPresentInDatabase = Barcode("123456789012")

    val barcodeRecipient = BarcodeRecipient(
      barcode = barcodeNotPresentInDatabase,
      name = "Fred Bloggs",
      prisonCode = "BXI",
      prisonNumber = "A1234BC"
    )

    assertThatExceptionOfType(JpaObjectRetrievalFailureException::class.java)
      .isThrownBy { barcodeRecipientRepository.save(barcodeRecipient) }
      .withMessageContaining("Barcode with id 123456789012")
  }

  @Test
  fun `should not save a second recipient for the same barcode`() {
    val barcode = Barcode("123456789012")
    barcodeRepository.save(barcode)
    barcodeRecipientRepository.save(
      BarcodeRecipient(
        barcode = barcode,
        name = "Fred Bloggs",
        prisonCode = "BXI",
        prisonNumber = "A1234BC"
      )
    )

    val secondBarcodeRecipient = BarcodeRecipient(
      barcode = barcode,
      name = "John Smith",
      prisonCode = "LEI",
      prisonNumber = "Q9816HG"
    )

    assertThatExceptionOfType(DataIntegrityViolationException::class.java)
      .isThrownBy { barcodeRecipientRepository.save(secondBarcodeRecipient) }
      .withMessageContaining("constraint [barcode_recipients_barcode_unique]")
  }

  @Test
  fun `should not delete barcode given a recipient exists`() {
    val barcode = Barcode("123456789012")
    barcodeRepository.save(barcode)
    barcodeRecipientRepository.save(
      BarcodeRecipient(
        barcode = barcode,
        name = "Fred Bloggs",
        prisonCode = "BXI",
        prisonNumber = "A1234BC"
      )
    )

    assertThatExceptionOfType(DataIntegrityViolationException::class.java)
      .isThrownBy { barcodeRepository.delete(barcode) }
      .withMessageContaining("constraint [barcode_recipients_barcode_fkey]")
  }
}
