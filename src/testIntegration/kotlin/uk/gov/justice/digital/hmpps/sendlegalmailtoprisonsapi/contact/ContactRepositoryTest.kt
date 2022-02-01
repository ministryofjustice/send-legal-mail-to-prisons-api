package uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.contact

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.jdbc.Sql

@DataJpaTest
@ActiveProfiles("test")
@Sql("/createContacts.sql")
class ContactRepositoryTest {

  @Autowired
  private lateinit var contactRepository: ContactRepository

  @Nested
  inner class FindContactByOwnerAndNameContainingIgnoreCase {
    @Test
    fun `should find Contacts by owner and name`() {
      val contacts =
        contactRepository.findContactByOwnerAndNameContainingIgnoreCase(
          "some.user@company.com.cjsm.net",
          "john"
        )

      val contactNames = contacts.map { it.name }
      assertThat(contactNames).containsExactly("John Smith", "Johnathon Evans", "Malcolm Johnston", "Bart Johnson")
    }

    @Test
    fun `should find zero Contacts given owner and name that match no records`() {
      val contacts = contactRepository.findContactByOwnerAndNameContainingIgnoreCase(
        "some.user.with.no.contacts.in.the.database@company.com.cjsm.net",
        "john"
      )

      assertThat(contacts).isEmpty()
    }
  }
}
