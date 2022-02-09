package uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.contact

import org.springframework.data.jpa.repository.JpaRepository
import java.time.Instant
import java.time.LocalDate
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id
import javax.persistence.Table

interface ContactRepository : JpaRepository<Contact, Int> {
  fun findContactByOwnerAndNameContainingIgnoreCase(owner: String, name: String): List<Contact>

  fun getContactByOwnerAndPrisonNumber(owner: String, prisonNumber: String): Contact?
}

@Entity
@Table(name = "contacts")
data class Contact(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  val id: Long? = null,

  val owner: String,

  val name: String,

  @Column(name = "prison_code")
  val prisonCode: String,

  val dob: LocalDate? = null,

  @Column(name = "prison_number")
  val prisonNumber: String? = null,

  val created: Instant,

  val updated: Instant
)
