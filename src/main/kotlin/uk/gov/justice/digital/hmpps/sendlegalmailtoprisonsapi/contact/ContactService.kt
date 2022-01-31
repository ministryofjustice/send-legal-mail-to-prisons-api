package uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.contact

import mu.KotlinLogging
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.config.DuplicateContactException
import java.time.Clock
import java.time.Instant

private val log = KotlinLogging.logger {}

@Service
class ContactService(private val contactRepository: ContactRepository, private val clock: Clock) {

  fun createContact(userId: String, createContactRequest: CreateContactRequest): Contact {
    val now = Instant.now(clock)
    val newContactEntity = Contact(
      owner = userId,
      name = createContactRequest.prisonerName,
      prisonCode = createContactRequest.prisonId,
      dob = createContactRequest.dob,
      prisonNumber = createContactRequest.prisonNumber,
      created = now,
      updated = now
    )
    try {
      return contactRepository.save(newContactEntity)
        .also {
          log.debug { "Created new Contact: $it" }
        }
    } catch (dataIntegrityViolationException: DataIntegrityViolationException) {
      throw DuplicateContactException(userId, createContactRequest.prisonNumber!!)
    }
  }
}
