package uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.contact

import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.config.DuplicateContactException
import java.time.Clock
import java.time.Instant

@Service
class ContactService(private val contactRepository: ContactRepository, private val clock: Clock) {

  fun getContact(userId: String, contactId: Long): ContactResponse? = contactRepository.getContactByOwnerAndId(owner = userId, id = contactId)
    ?.let { toContactResponse(it) }

  fun createContact(userId: String, contactRequest: ContactRequest): ContactResponse {
    val now = Instant.now(clock)
    val newContactEntity = Contact(
      owner = userId,
      name = contactRequest.prisonerName,
      prisonCode = contactRequest.prisonId,
      dob = contactRequest.dob,
      prisonNumber = contactRequest.prisonNumber,
      created = now,
      updated = now,
    )
    try {
      return toContactResponse(contactRepository.save(newContactEntity))
    } catch (dataIntegrityViolationException: DataIntegrityViolationException) {
      throw DuplicateContactException(userId, contactRequest.prisonNumber!!)
    }
  }

  fun updateContact(userId: String, contactId: Long, contactRequest: ContactRequest): ContactResponse? = try {
    contactRepository.getContactByOwnerAndId(userId, contactId)
      ?.let { existingContact -> toContact(contactRequest, existingContact) }
      ?.let { newContact -> contactRepository.save(newContact) }
      ?.let { savedContact -> toContactResponse(savedContact) }
  } catch (ex: DataIntegrityViolationException) {
    throw DuplicateContactException(userId, contactRequest.prisonNumber!!)
  }

  fun searchContactsByName(userId: String, name: String): Collection<ContactResponse> = // We have to uppercase the name due to a deficiency in jpql - see the ContactRepository for more details
    contactRepository.findContactByOwnerAndNameContainingIgnoreCase(userId, name.uppercase())
      .map { toContactResponse(it) }

  fun getContactByPrisonNumber(userId: String, prisonNumber: String): ContactResponse? = contactRepository.getContactByOwnerAndPrisonNumber(userId, prisonNumber)
    ?.let { toContactResponse(it) }

  private fun toContact(contactRequest: ContactRequest, contact: Contact) = with(contactRequest) {
    contact.copy(
      name = prisonerName,
      prisonCode = prisonId,
      dob = dob,
      prisonNumber = prisonNumber,
      updated = Instant.now(clock),
    )
  }

  private fun toContactResponse(contact: Contact): ContactResponse = ContactResponse(
    id = contact.id!!,
    prisonerName = contact.name,
    prisonId = contact.prisonCode,
    dob = contact.dob,
    prisonNumber = contact.prisonNumber,
  )
}
