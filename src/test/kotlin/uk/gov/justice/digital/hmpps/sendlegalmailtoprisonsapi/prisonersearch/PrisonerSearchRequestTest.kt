package uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.prisonersearch

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.barcode.Barcode
import uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.barcode.BarcodeRecipient
import java.time.LocalDate

class PrisonerSearchRequestTest {

  @Nested
  inner class BarcodeRecipientConstructor {
    @Test
    fun `should create PrisonerSearchRequest given BarcodeRecipient with prisonNumber`() {
      val barcodeRecipient = BarcodeRecipient(
        barcode = aBarcode(),
        prisonCode = "BXI",
        prisonNumber = "A1234BC",
        name = "John Smith",
        dob = null,
      )
      val expectedPrisonSearchRequest =
        PrisonerSearchRequest(prisonNumber = "A1234BC", firstName = "John", lastName = "Smith", dob = null)

      val prisonerSearchRequest = PrisonerSearchRequest(barcodeRecipient)

      assertThat(prisonerSearchRequest).isEqualTo(expectedPrisonSearchRequest)
    }

    @Test
    fun `should create PrisonerSearchRequest given BarcodeRecipient with DOB`() {
      val barcodeRecipient = BarcodeRecipient(
        barcode = aBarcode(),
        prisonCode = "BXI",
        prisonNumber = null,
        name = "John Smith",
        dob = LocalDate.MIN,
      )
      val expectedPrisonSearchRequest =
        PrisonerSearchRequest(prisonNumber = null, firstName = "John", lastName = "Smith", dob = LocalDate.MIN)

      val prisonerSearchRequest = PrisonerSearchRequest(barcodeRecipient)

      assertThat(prisonerSearchRequest).isEqualTo(expectedPrisonSearchRequest)
    }

    @Test
    fun `should create PrisonerSearchRequest given BarcodeRecipient with name with many components`() {
      val barcodeRecipient = BarcodeRecipient(
        barcode = aBarcode(),
        prisonCode = "BXI",
        prisonNumber = "A1234BC",
        name = "John Bobby Smith",
        dob = null,
      )
      val expectedPrisonSearchRequest =
        PrisonerSearchRequest(prisonNumber = "A1234BC", firstName = "John", lastName = "Bobby Smith", dob = null)

      val prisonerSearchRequest = PrisonerSearchRequest(barcodeRecipient)

      assertThat(prisonerSearchRequest).isEqualTo(expectedPrisonSearchRequest)
    }

    @Test
    fun `should create PrisonerSearchRequest given BarcodeRecipient with name with just one component`() {
      val barcodeRecipient =
        BarcodeRecipient(barcode = aBarcode(), prisonCode = "BXI", prisonNumber = "A1234BC", name = "John", dob = null)
      val expectedPrisonSearchRequest =
        PrisonerSearchRequest(prisonNumber = "A1234BC", firstName = null, lastName = "John", dob = null)

      val prisonerSearchRequest = PrisonerSearchRequest(barcodeRecipient)

      assertThat(prisonerSearchRequest).isEqualTo(expectedPrisonSearchRequest)
    }
  }

  @Nested
  inner class ToMatchPrisonersRequestBody {
    @Test
    fun `should convert to request body given request with all fields`() {
      val prisonerSearchRequest = PrisonerSearchRequest(prisonNumber = "A1234BC", firstName = "John", lastName = "Smith", dob = LocalDate.of(1965, 2, 16))
      val expectedRequestBody = mapOf(
        "prisonerIdentifier" to "A1234BC",
        "firstName" to "John",
        "lastName" to "Smith",
        "dateOfBirth" to "1965-02-16",
        "includeAliases" to "true",
      )

      assertThat(prisonerSearchRequest.toGlobalSearchRequestBody()).isEqualTo(expectedRequestBody)
    }

    @Test
    fun `should convert to request body given request with date of birth`() {
      val prisonerSearchRequest = PrisonerSearchRequest(prisonNumber = null, firstName = "John", lastName = "Smith", dob = LocalDate.of(1965, 2, 16))
      val expectedRequestBody = mapOf(
        "nomsNumber" to null,
        "firstName" to "John",
        "lastName" to "Smith",
        "dateOfBirth" to "1965-02-16",
      )

      assertThat(prisonerSearchRequest.toMatchPrisonersRequestBody()).isEqualTo(expectedRequestBody)
    }

    @Test
    fun `should convert to request body given request with prison number`() {
      val prisonerSearchRequest = PrisonerSearchRequest(prisonNumber = "A1234BC", firstName = "John", lastName = "Smith", dob = null)
      val expectedRequestBody = mapOf(
        "nomsNumber" to "A1234BC",
        "firstName" to "John",
        "lastName" to "Smith",
        "dateOfBirth" to null,
      )

      assertThat(prisonerSearchRequest.toMatchPrisonersRequestBody()).isEqualTo(expectedRequestBody)
    }

    @Test
    fun `should convert to request body given request without firstname`() {
      val prisonerSearchRequest = PrisonerSearchRequest(prisonNumber = "A1234BC", firstName = null, lastName = "Smith", dob = null)
      val expectedRequestBody = mapOf(
        "nomsNumber" to "A1234BC",
        "firstName" to null,
        "lastName" to "Smith",
        "dateOfBirth" to null,
      )

      assertThat(prisonerSearchRequest.toMatchPrisonersRequestBody()).isEqualTo(expectedRequestBody)
    }
  }

  @Nested
  inner class ToGlobalSearchRequestBody {
    @Test
    fun `should convert to request body given request with all fields`() {
      val prisonerSearchRequest = PrisonerSearchRequest(prisonNumber = "A1234BC", firstName = "John", lastName = "Smith", dob = LocalDate.of(1965, 2, 16))
      val expectedRequestBody = mapOf(
        "prisonerIdentifier" to "A1234BC",
        "firstName" to "John",
        "lastName" to "Smith",
        "dateOfBirth" to "1965-02-16",
        "includeAliases" to "true",
      )

      assertThat(prisonerSearchRequest.toGlobalSearchRequestBody()).isEqualTo(expectedRequestBody)
    }

    @Test
    fun `should convert to request body given request with date of birth`() {
      val prisonerSearchRequest = PrisonerSearchRequest(prisonNumber = null, firstName = "John", lastName = "Smith", dob = LocalDate.of(1965, 2, 16))
      val expectedRequestBody = mapOf(
        "prisonerIdentifier" to null,
        "firstName" to "John",
        "lastName" to "Smith",
        "dateOfBirth" to "1965-02-16",
        "includeAliases" to "true",
      )

      assertThat(prisonerSearchRequest.toGlobalSearchRequestBody()).isEqualTo(expectedRequestBody)
    }

    @Test
    fun `should convert to request body given request with prison number`() {
      val prisonerSearchRequest = PrisonerSearchRequest(prisonNumber = "A1234BC", firstName = "John", lastName = "Smith", dob = null)
      val expectedRequestBody = mapOf(
        "prisonerIdentifier" to "A1234BC",
        "firstName" to "John",
        "lastName" to "Smith",
        "dateOfBirth" to null,
        "includeAliases" to "true",
      )

      assertThat(prisonerSearchRequest.toGlobalSearchRequestBody()).isEqualTo(expectedRequestBody)
    }

    @Test
    fun `should convert to request body given request without firstname`() {
      val prisonerSearchRequest = PrisonerSearchRequest(prisonNumber = "A1234BC", firstName = null, lastName = "Smith", dob = null)
      val expectedRequestBody = mapOf(
        "prisonerIdentifier" to "A1234BC",
        "firstName" to null,
        "lastName" to "Smith",
        "dateOfBirth" to null,
        "includeAliases" to "true",
      )

      assertThat(prisonerSearchRequest.toGlobalSearchRequestBody()).isEqualTo(expectedRequestBody)
    }
  }

  private fun aBarcode() = Barcode("SOME_BARCODE")
}
