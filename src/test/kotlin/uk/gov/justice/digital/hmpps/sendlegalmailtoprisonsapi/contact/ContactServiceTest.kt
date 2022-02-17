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
import org.mockito.kotlin.verify
import org.springframework.dao.DataIntegrityViolationException
import uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.config.DuplicateContactException
import uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.config.ResourceNotFoundException
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
      val expectedContact = Contact(
        id = 1,
        owner = "a-user@cjsm.net",
        name = "John Smith",
        prisonCode = "BXI",
        prisonNumber = "A1234BC",
        created = Instant.now(clock),
        updated = Instant.now(clock)
      )
      given { contactRepository.save(any()) }.willReturn(expectedContact)

      val contact = contactService.createContact("a-user@cjsm.net", contactRequest)

      assertThat(contact).isEqualTo(expectedContact)
      verify(contactRepository).save(
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

    @Test
    fun `should create contact given request containing prisoner DOB`() {
      val contactRequest = ContactRequest(
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
        created = Instant.now(clock),
        updated = Instant.now(clock)
      )
      given { contactRepository.save(any()) }.willReturn(expectedContact)

      val contact = contactService.createContact("a-user@cjsm.net", contactRequest)

      assertThat(contact).isEqualTo(expectedContact)
      verify(contactRepository).save(
        check<Contact> {
          assertThat(it).usingRecursiveComparison().isEqualTo(
            Contact(
              id = null,
              owner = "a-user@cjsm.net",
              name = "John Smith",
              prisonCode = "BXI",
              dob = LocalDate.of(1990, 12, 20),
              created = Instant.now(clock),
              updated = Instant.now(clock)
            )
          )
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
      verify(contactRepository).save(
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

    private val aContact = Contact(
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
      owner = aContact.owner,
      name = aContactRequest.prisonerName,
      prisonCode = aContactRequest.prisonId,
      dob = aContactRequest.dob,
      prisonNumber = aContactRequest.prisonNumber,
      created = yesterday,
      updated = now,
    )

    @Test
    fun `should return saved contact if contact exists`() {
      given { contactRepository.getContactByOwnerAndId(anyString(), anyLong()) }.willReturn(aContact)
      given { contactRepository.save(any()) }.willReturn(updatedContact)

      assertThat(contactService.updateContact("some-user", 1L, aContactRequest)).isEqualTo(updatedContact)

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
      val expectedContacts = listOf(
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
      given { contactRepository.findContactByOwnerAndNameContainingIgnoreCase(any(), any()) }.willReturn(
        expectedContacts
      )

      val contacts = contactService.searchContactsByName("a-user@cjsm.net", "john")

      assertThat(contacts).isEqualTo(expectedContacts)
      verify(contactRepository).findContactByOwnerAndNameContainingIgnoreCase("a-user@cjsm.net", "john")
    }

    @Test
    fun `should return empty collection given no matching contacts`() {
      given { contactRepository.findContactByOwnerAndNameContainingIgnoreCase(any(), any()) }.willReturn(emptyList())

      val contacts = contactService.searchContactsByName("a-user@cjsm.net", "fred")

      assertThat(contacts).isEmpty()
      verify(contactRepository).findContactByOwnerAndNameContainingIgnoreCase("a-user@cjsm.net", "fred")
    }
  }

  @Nested
  inner class GetContactByPrisonNumber {
    @Test
    fun `should return contact given contact is found on the database`() {
      val expectedContact = Contact(
        id = 1,
        owner = "a-user@cjsm.net",
        name = "John Smith",
        prisonCode = "BXI",
        dob = LocalDate.of(1990, 12, 20),
        created = Instant.now(clock),
        updated = Instant.now(clock)
      )
      given { contactRepository.getContactByOwnerAndPrisonNumber(any(), any()) }.willReturn(expectedContact)

      val contact = contactService.getContactByPrisonNumber("a-user@cjsm.net", "A1234BC")

      assertThat(contact).isEqualTo(expectedContact)
      verify(contactRepository).getContactByOwnerAndPrisonNumber("a-user@cjsm.net", "A1234BC")
    }

    @Test
    fun `should throw ResourceNotFoundException given contact is not found on the database`() {
      given { contactRepository.getContactByOwnerAndPrisonNumber(any(), any()) }.willReturn(null)

      assertThrows<ResourceNotFoundException> {
        contactService.getContactByPrisonNumber("a-user@cjsm.net", "A1234BC")
      }
      verify(contactRepository).getContactByOwnerAndPrisonNumber("a-user@cjsm.net", "A1234BC")
    }
  }

  @Nested
  inner class GetContactById {
    @Test
    fun `should get contact by id`() {
      val expectedContact = Contact(
        id = 1,
        owner = "a-user@cjsm.net",
        name = "John Smith",
        prisonCode = "BXI",
        dob = LocalDate.of(1990, 12, 20),
        created = Instant.now(clock),
        updated = Instant.now(clock)
      )
      given { contactRepository.getById(any()) }.willReturn(expectedContact)

      val contact = contactService.getContactById(1)

      assertThat(contact).isEqualTo(expectedContact)
      verify(contactRepository).getById(1)
    }

    @Test
    fun `should return null given unknown contact id`() {

      given { contactRepository.getById(any()) }.willReturn(null)

      val contact = contactService.getContactById(2)

      assertThat(contact).isNull()
      verify(contactRepository).getById(2)
    }
  }
}
