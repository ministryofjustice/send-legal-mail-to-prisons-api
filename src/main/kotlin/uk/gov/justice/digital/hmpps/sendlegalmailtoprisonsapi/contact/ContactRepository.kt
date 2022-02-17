package uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.contact

import org.hibernate.Hibernate
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.time.Instant
import java.time.LocalDate
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id
import javax.persistence.Table

@Repository
interface ContactRepository : JpaRepository<Contact, Long> {
  fun findContactByOwnerAndNameContainingIgnoreCase(owner: String, name: String): List<Contact>

  fun getContactByOwnerAndId(owner: String, id: Long): Contact?

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
) {
  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other == null || Hibernate.getClass(this) != Hibernate.getClass(other)) return false
    other as Contact

    return id != null && id == other.id
  }

  override fun hashCode(): Int = javaClass.hashCode()

  @Override
  override fun toString(): String {
    return this::class.simpleName + "(id = $id , owner = $owner , prisonCode = $prisonCode )"
  }
}
