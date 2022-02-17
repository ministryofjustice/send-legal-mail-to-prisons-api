package uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.contact

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.ArgumentMatchers.anyLong
import org.mockito.ArgumentMatchers.anyString
import org.mockito.kotlin.any
import org.mockito.kotlin.check
import org.mockito.kotlin.given
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.then
import org.springframework.dao.DataIntegrityViolationException
import uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.config.DuplicateContactException
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

class ContactServiceTest {

  private val contactRepository = mock<ContactRepository>()
  private val clock = Clock.fixed(Instant.now(), ZoneId.of("Europe/London"))
  private val contactService = ContactService(contactRepository, clock)

  @Nested
  inner class CreateContact {

    @Test
    fun `should create contact given request containing prison number`() {
      val contactRequest = ContactRequest(
        prisonerName = "John Smith",
        prisonId = "BXI",
        prisonNumber = "A1234BC"
      )
      val savedContact = Contact(
        id = 1,
        owner = "a-user@cjsm.net",
        name = "John Smith",
        prisonCode = "BXI",
        prisonNumber = "A1234BC",
        created = Instant.now(clock),
        updated = Instant.now(clock)
      )
      val expectedContactResponse = ContactResponse(
        id = 1,
        prisonerName = "John Smith",
        prisonId = "BXI",
        prisonNumber = "A1234BC",
      )
      given { contactRepository.save(any()) }.willReturn(savedContact)

      val contactResponse = contactService.createContact("a-user@cjsm.net", contactRequest)

      assertThat(contactResponse).isEqualTo(expectedContactResponse)
      then(contactRepository).should().save(
        check<Contact> {
          assertThat(it).usingRecursiveComparison().isEqualTo(savedContact.copy(id = null))
        }
      )
    }

    @Test
    fun `should create contact given request containing prisoner DOB`() {
      val contactRequest = ContactRequest(
        prisonerName = "John Smith",
        prisonId = "BXI",
        dob = LocalDate.of(1990, 12, 20)
      )
      val savedContact = Contact(
        id = 1,
        owner = "a-user@cjsm.net",
        name = "John Smith",
        prisonCode = "BXI",
        dob = LocalDate.of(1990, 12, 20),
        created = Instant.now(clock),
        updated = Instant.now(clock)
      )
      val expectedContactResponse = ContactResponse(
        id = 1,
        prisonerName = "John Smith",
        prisonId = "BXI",
        dob = LocalDate.of(1990, 12, 20),
      )
      given { contactRepository.save(any()) }.willReturn(savedContact)

      val contactResponse = contactService.createContact("a-user@cjsm.net", contactRequest)

      assertThat(contactResponse).isEqualTo(expectedContactResponse)
      then(contactRepository).should().save(
        check<Contact> {
          assertThat(it).usingRecursiveComparison().isEqualTo(savedContact.copy(id = null))
        }
      )
    }

    @Test
    fun `should throw DuplicateContactException given database throws constraint violation`() {
      val contactRequest = ContactRequest(
        prisonerName = "John Smith",
        prisonId = "BXI",
        prisonNumber = "A1234BC"
      )
      given { contactRepository.save(any()) }.willThrow(DataIntegrityViolationException("Duplicate record on index contacts_uni_idx_owner_prison_number"))

      assertThrows<DuplicateContactException> {
        contactService.createContact("a-user@cjsm.net", contactRequest)
      }
      then(contactRepository).should().save(
        check<Contact> {
          assertThat(it).usingRecursiveComparison().isEqualTo(
            Contact(
              id = null,
              owner = "a-user@cjsm.net",
              name = "John Smith",
              prisonCode = "BXI",
              prisonNumber = "A1234BC",
              created = Instant.now(clock),
              updated = Instant.now(clock)
            )
          )
        }
      )
    }
  }

  @Nested
  inner class UpdateContact {
    private val now = Instant.now(clock)
    private val yesterday = now.minus(1L, ChronoUnit.DAYS)
    private val aContactRequest: ContactRequest =
      ContactRequest(
        prisonerName = "new-name",
        prisonId = "new-prison-code",
        dob = LocalDate.of(1999, 12, 12),
        prisonNumber = "new-prison-number"
      )
    private val existingContact = Contact(
      id = 1L,
      owner = "some-user",
      name = "some-name",
      prisonCode = "some-prison-code",
      dob = LocalDate.of(1990, 1, 1),
      prisonNumber = "some-prison-number",
      created = yesterday,
      updated = yesterday,
    )
    private val updatedContact = Contact(
      id = 1L,
      owner = "some-user",
      name = "new-name",
      prisonCode = "new-prison-code",
      dob = LocalDate.of(1999, 12, 12),
      prisonNumber = "new-prison-number",
      created = yesterday,
      updated = now,
    )
    private val expectedContactResponse = ContactResponse(
      id = 1L,
      prisonerName = "new-name",
      prisonId = "new-prison-code",
      dob = LocalDate.of(1999, 12, 12),
      prisonNumber = "new-prison-number",
    )

    @Test
    fun `should return saved contact if contact exists`() {
      given { contactRepository.getContactByOwnerAndId(anyString(), anyLong()) }.willReturn(existingContact)
      given { contactRepository.save(any()) }.willReturn(updatedContact)

      val contactResponse = contactService.updateContact("some-user", 1L, aContactRequest)

      assertThat(contactResponse).isEqualTo(expectedContactResponse)
      then(contactRepository).should().getContactByOwnerAndId("some-user", 1L)
      then(contactRepository).should().save(
        check<Contact> {
          assertThat(it).usingRecursiveComparison().isEqualTo(updatedContact)
        }
      )
    }

    @Test
    fun `should return null if contact does not exist`() {
      given { contactRepository.getContactByOwnerAndId(anyString(), anyLong()) }.willReturn(null)

      assertThat(contactService.updateContact("some-user", 1L, aContactRequest)).isNull()

      then(contactRepository).should(never()).save(any())
    }
  }

  @Nested
  inner class SearchContactsByName {
    @Test
    fun `should return matching contacts`() {
      val existingContacts = listOf(
        Contact(
          id = 1,
          owner = "a-user@cjsm.net",
          name = "John Smith",
          prisonCode = "BXI",
          dob = LocalDate.of(1990, 12, 20),
          created = Instant.now(clock),
          updated = Instant.now(clock)
        ),
        Contact(
          id = 1921,
          owner = "a-user@cjsm.net",
          name = "Barbara Johnston",
          prisonCode = "LEI",
          prisonNumber = "B4372PC",
          created = Instant.now(clock),
          updated = Instant.now(clock)
        )
      )
      val expectedContactResponses = listOf(
        ContactResponse(
          id = 1,
          prisonerName = "John Smith",
          prisonId = "BXI",
          dob = LocalDate.of(1990, 12, 20),
        ),
        ContactResponse(
          id = 1921,
          prisonerName = "Barbara Johnston",
          prisonId = "LEI",
          prisonNumber = "B4372PC",
        )
      )
      given { contactRepository.findContactByOwnerAndNameContainingIgnoreCase(any(), any()) }.willReturn(
        existingContacts
      )

      val contactResponses = contactService.searchContactsByName("a-user@cjsm.net", "john")

      assertThat(contactResponses).isEqualTo(expectedContactResponses)
      then(contactRepository).should().findContactByOwnerAndNameContainingIgnoreCase("a-user@cjsm.net", "john")
    }

    @Test
    fun `should return empty collection given no matching contacts`() {
      given { contactRepository.findContactByOwnerAndNameContainingIgnoreCase(any(), any()) }.willReturn(emptyList())

      val contactResponses = contactService.searchContactsByName("a-user@cjsm.net", "fred")

      assertThat(contactResponses).isEmpty()
      then(contactRepository).should().findContactByOwnerAndNameContainingIgnoreCase("a-user@cjsm.net", "fred")
    }
  }

  @Nested
  inner class GetContactByPrisonNumber {
    @Test
    fun `should return contact given contact is found on the database`() {
      val existingContact = Contact(
        id = 1,
        owner = "a-user@cjsm.net",
        name = "John Smith",
        prisonCode = "BXI",
        dob = LocalDate.of(1990, 12, 20),
        created = Instant.now(clock),
        updated = Instant.now(clock)
      )
      val expectedContactResponse = ContactResponse(
        id = 1,
        prisonerName = "John Smith",
        prisonId = "BXI",
        dob = LocalDate.of(1990, 12, 20),
      )
      given { contactRepository.getContactByOwnerAndPrisonNumber(any(), any()) }.willReturn(existingContact)

      val contactResponse = contactService.getContactByPrisonNumber("a-user@cjsm.net", "A1234BC")

      assertThat(contactResponse).isEqualTo(expectedContactResponse)
      then(contactRepository).should().getContactByOwnerAndPrisonNumber("a-user@cjsm.net", "A1234BC")
    }

    @Test
    fun `should return null given contact is not found on the database`() {
      given { contactRepository.getContactByOwnerAndPrisonNumber(any(), any()) }.willReturn(null)

      val contact = contactService.getContactByPrisonNumber("a-user@cjsm.net", "A1234BC")

      assertThat(contact).isNull()
      then(contactRepository).should().getContactByOwnerAndPrisonNumber("a-user@cjsm.net", "A1234BC")
    }
  }
}
