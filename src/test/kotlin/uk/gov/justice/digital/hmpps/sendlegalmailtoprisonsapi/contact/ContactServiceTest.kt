package uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.contact

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.any
import org.mockito.kotlin.given
import org.mockito.kotlin.mock
import org.springframework.dao.DataIntegrityViolationException
import uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.config.DuplicateContactException
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class ContactServiceTest {

  private val contactRepository = mock<ContactRepository>()
  private val clock = Clock.fixed(Instant.now(), ZoneId.of("Europe/London"))
  private val contactService = ContactService(contactRepository, clock)

  @Nested
  inner class CreateContact {

    @Test
    fun `should create contact given request containing prison number`() {
      val createContactRequest = CreateContactRequest(
        prisonerName = "John Smith",
        prisonId = "BXI",
        prisonNumber = "A1234BC"
      )
      val expectedContact = Contact(
        id = 1,
        owner = "a-user@cjsm.net",
        name = "John Smith",
        prisonCode = "BXI",
        prisonNumber = "A1234BC",
        created = Instant.now(),
        updated = Instant.now()
      )
      given { contactRepository.save(any()) }.willReturn(expectedContact)

      val contact = contactService.createContact("a-user@cjsm.net", createContactRequest)

      assertThat(contact).isEqualTo(expectedContact)
    }

    @Test
    fun `should create contact given request containing prisoner DOB`() {
      val createContactRequest = CreateContactRequest(
        prisonerName = "John Smith",
        prisonId = "BXI",
        dob = LocalDate.of(1990, 12, 20)
      )
      val expectedContact = Contact(
        id = 1,
        owner = "a-user@cjsm.net",
        name = "John Smith",
        prisonCode = "BXI",
        dob = LocalDate.of(1990, 12, 20),
        created = Instant.now(),
        updated = Instant.now()
      )
      given { contactRepository.save(any()) }.willReturn(expectedContact)

      val contact = contactService.createContact("a-user@cjsm.net", createContactRequest)

      assertThat(contact).isEqualTo(expectedContact)
    }

    @Test
    fun `should throw DuplicateContactException given database throws constraint violation`() {
      val createContactRequest = CreateContactRequest(
        prisonerName = "John Smith",
        prisonId = "BXI",
        prisonNumber = "A1234BC"
      )
      given { contactRepository.save(any()) }.willThrow(DataIntegrityViolationException("Duplicate record on index contacts_uni_idx_owner_prison_number"))

      assertThrows<DuplicateContactException> {
        contactService.createContact("a-user@cjsm.net", createContactRequest)
      }
    }
  }
}
