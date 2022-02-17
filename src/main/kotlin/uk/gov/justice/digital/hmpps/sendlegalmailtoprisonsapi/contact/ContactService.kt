package uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.contact

import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.config.DuplicateContactException
import uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.config.ResourceNotFoundException
import java.time.Clock
import java.time.Instant

@Service
class ContactService(private val contactRepository: ContactRepository, private val clock: Clock) {

  fun createContact(userId: String, contactRequest: ContactRequest): Contact {
    val now = Instant.now(clock)
    val newContactEntity = Contact(
      owner = userId,
      name = contactRequest.prisonerName,
      prisonCode = contactRequest.prisonId,
      dob = contactRequest.dob,
      prisonNumber = contactRequest.prisonNumber,
      created = now,
      updated = now
    )
    try {
      return contactRepository.save(newContactEntity)
    } catch (dataIntegrityViolationException: DataIntegrityViolationException) {
      throw DuplicateContactException(userId, contactRequest.prisonNumber!!)
    }
  }

  fun updateContact(userId: String, contactId: Long, contactRequest: ContactRequest): Contact? =
    contactRepository.getContactByOwnerAndId(userId, contactId)
      ?.let { existingContact -> toContact(contactRequest, existingContact) }
      ?.let { newContact -> contactRepository.save(newContact) }

  fun searchContactsByName(userId: String, name: String): Collection<Contact> =
    contactRepository.findContactByOwnerAndNameContainingIgnoreCase(userId, name)

  fun getContactByPrisonNumber(userId: String, prisonNumber: String): Contact =
    contactRepository.getContactByOwnerAndPrisonNumber(userId, prisonNumber)
      ?: throw ResourceNotFoundException("Could not find a matching Contact [$userId, $prisonNumber]")

  fun getContactById(id: Long): Contact? =
    contactRepository.getById(id)

  private fun toContact(contactRequest: ContactRequest, contact: Contact) =
    with(contactRequest) {
      contact.copy(
        name = prisonerName,
        prisonCode = prisonId,
        dob = dob,
        prisonNumber = prisonNumber,
        updated = Instant.now(clock),
      )
    }
}
