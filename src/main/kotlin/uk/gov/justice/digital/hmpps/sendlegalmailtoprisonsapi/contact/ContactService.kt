package uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.contact

import org.springframework.stereotype.Service
import java.time.Instant

@Service
class ContactService(private val contactRepository: ContactRepository) {

  fun createContact(userId: String, createContactRequest: CreateContactRequest): Contact {
    // TODO - check whether the contact exists in order to allow the API to return a 409
    // Need to consider what defines contact equality - a combination of owner, name & prisonId ?
    val now = Instant.now()
    val newContactEntity = Contact(
      owner = userId,
      name = createContactRequest.prisonerName,
      prisonCode = createContactRequest.prisonId,
      dob = createContactRequest.dob,
      prisonNumber = createContactRequest.prisonNumber,
      created = now,
      updated = now
    )
    return contactRepository.save(newContactEntity)
  }
}
