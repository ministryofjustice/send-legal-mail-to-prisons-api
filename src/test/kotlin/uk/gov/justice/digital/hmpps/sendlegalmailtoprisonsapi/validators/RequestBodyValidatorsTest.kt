package uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.validators

import org.assertj.core.api.Assertions.assertThatCode
import org.assertj.core.api.Assertions.assertThatExceptionOfType
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.barcode.CreateBarcodeRequest
import uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.config.ValidationException
import uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.contact.ContactRequest
import java.time.LocalDate

class RequestBodyValidatorsTest {

  @Nested
  inner class CreateContactRequestValidators {
    @Test
    fun `should validate given request with dob`() {
      val contactRequest = ContactRequest(prisonerName = "Fred Bloggs", prisonId = "BXI", dob = LocalDate.now())

      assertThatCode { validateRequestHasDobOrPrisonNumber(contactRequest) }
        .doesNotThrowAnyException()
    }

    @Test
    fun `should validate given request with prison number`() {
      val contactRequest = ContactRequest(prisonerName = "Fred Bloggs", prisonId = "BXI", prisonNumber = "A1234BC")

      assertThatCode { validateRequestHasDobOrPrisonNumber(contactRequest) }
        .doesNotThrowAnyException()
    }

    @Test
    fun `should validate given request with both dob and prison number`() {
      val contactRequest = ContactRequest(prisonerName = "Fred Bloggs", prisonId = "BXI", dob = LocalDate.now(), prisonNumber = "A1234BC")

      assertThatCode { validateRequestHasDobOrPrisonNumber(contactRequest) }
        .doesNotThrowAnyException()
    }

    @Test
    fun `should throw when validating given request without both dob and prison number`() {
      val contactRequest = ContactRequest(prisonerName = "Fred Bloggs", prisonId = "BXI")

      assertThatExceptionOfType(ValidationException::class.java)
        .isThrownBy { validateRequestHasDobOrPrisonNumber(contactRequest) }
    }
  }

  @Nested
  inner class CreateBarcodeRequestValidators {
    @Test
    fun `should validate given request with dob`() {
      val createBarcodeRequest = CreateBarcodeRequest(prisonerName = "Fred Bloggs", prisonId = "BXI", dob = LocalDate.now())

      assertThatCode { validateRequestHasDobOrPrisonNumber(createBarcodeRequest) }
        .doesNotThrowAnyException()
    }

    @Test
    fun `should validate given request with prison number`() {
      val createBarcodeRequest = CreateBarcodeRequest(prisonerName = "Fred Bloggs", prisonId = "BXI", prisonNumber = "A1234BC")

      assertThatCode { validateRequestHasDobOrPrisonNumber(createBarcodeRequest) }
        .doesNotThrowAnyException()
    }

    @Test
    fun `should validate given request with both dob and prison number`() {
      val createBarcodeRequest = CreateBarcodeRequest(prisonerName = "Fred Bloggs", prisonId = "BXI", dob = LocalDate.now(), prisonNumber = "A1234BC")

      assertThatCode { validateRequestHasDobOrPrisonNumber(createBarcodeRequest) }
        .doesNotThrowAnyException()
    }

    @Test
    fun `should throw when validating given request without both dob and prison number`() {
      val createBarcodeRequest = CreateBarcodeRequest(prisonerName = "Fred Bloggs", prisonId = "BXI")

      assertThatExceptionOfType(ValidationException::class.java)
        .isThrownBy { validateRequestHasDobOrPrisonNumber(createBarcodeRequest) }
    }
  }
}
