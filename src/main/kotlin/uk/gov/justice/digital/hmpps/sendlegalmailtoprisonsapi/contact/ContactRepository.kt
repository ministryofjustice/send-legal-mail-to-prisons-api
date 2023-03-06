package uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.contact

import org.hibernate.Hibernate
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
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
  // We need the @Query due to a bug in hibernate-core 5.6.7.Final which breaks a test in ContactResourceSearchContactsTest
  // It's not an ideal solution - we have to pass in an upper case name because jpql doesn't handle upper in a like - but seems better than pinning hibernate-core to 5.6.5.Final for as long as it takes to be fixed
  // TODO when hibernate-core moves beyond 5.6.7.Final try removing the @Query and running ContactResourceSearchContactsTest to see if we no longer need this workaround.
  @Query("select c from Contact as c where c.owner = :owner and upper(c.name) like %:name%")
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

  val updated: Instant,
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
