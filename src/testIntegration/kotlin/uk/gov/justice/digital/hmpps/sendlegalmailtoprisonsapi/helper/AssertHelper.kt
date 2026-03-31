package uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.helper

import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.within
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.contact.ContactRepository
import uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.prisons.SupportedPrison
import uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.prisons.SupportedPrisonsRepository
import java.time.Instant
import java.time.temporal.ChronoUnit

@Transactional(readOnly = true)
@Component
class AssertHelper {
  @Autowired
  private lateinit var contactRepository: ContactRepository

  @Autowired
  protected lateinit var supportedPrisonsRepository: SupportedPrisonsRepository

  fun assertContactPrisonerNumberNoMatch(existingContactId: Long, updateContactId: Long) {
    val existingContact = contactRepository.getReferenceById(existingContactId)
    val updatedContact = contactRepository.getReferenceById(updateContactId)
    Assertions.assertThat(existingContact.prisonNumber).isNotEqualTo(updatedContact.prisonNumber)
  }

  fun assertContact(existingContactId: Long, prisonerName: String, prisonNumber: String, prisonId: String) {
    val savedContact = contactRepository.getReferenceById(existingContactId)
    assertThat(savedContact.name).isEqualTo(prisonerName)
    assertThat(savedContact.prisonNumber).isEqualTo(prisonNumber)
    assertThat(savedContact.prisonCode).isEqualTo(prisonId)
    assertThat(savedContact.updated).isAfter(savedContact.created)
  }

  fun assertPrison(prisonCode: String, expectedCode: String, expectedActive: Boolean, expectedUpdatedBy: String) {
    val supportedPrison = supportedPrisonsRepository.getReferenceById(prisonCode)

    assertThat(supportedPrison)
      .extracting(SupportedPrison::code, SupportedPrison::active, SupportedPrison::updatedBy)
      .isEqualTo(listOf(expectedCode, expectedActive, expectedUpdatedBy))
    assertThat(supportedPrison.updated).isCloseTo(Instant.now(), within(5, ChronoUnit.SECONDS))
  }
}
