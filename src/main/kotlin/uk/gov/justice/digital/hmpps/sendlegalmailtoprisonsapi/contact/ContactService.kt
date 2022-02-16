package uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.contact

import mu.KotlinLogging
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.config.DuplicateContactException
import uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.config.ResourceNotFoundException
import java.time.Clock
import java.time.Instant

private val log = KotlinLogging.logger {}

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
        .also {
          log.debug { "Created new Contact: $it" }
        }
    } catch (dataIntegrityViolationException: DataIntegrityViolationException) {
      throw DuplicateContactException(userId, contactRequest.prisonNumber!!)
    }
  }

  fun searchContactsByName(userId: String, name: String): Collection<Contact> =
    contactRepository.findContactByOwnerAndNameContainingIgnoreCase(userId, name)
      .also {
        log.debug { "Returning ${it.size} matching Contact records" }
      }

  fun getContactByPrisonNumber(userId: String, prisonNumber: String): Contact =
    contactRepository.getContactByOwnerAndPrisonNumber(userId, prisonNumber)
      ?.also {
        log.debug { "Returning Contact: $it" }
      }
      ?: throw ResourceNotFoundException("Could not find a matching Contact [$userId, $prisonNumber]")

  fun getContactById(id: Long): Contact? =
    contactRepository.getById(id)
}
