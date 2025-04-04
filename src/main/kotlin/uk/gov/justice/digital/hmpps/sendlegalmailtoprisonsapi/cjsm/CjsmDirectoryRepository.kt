package uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.cjsm

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.Hibernate
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface CjsmDirectoryRepository : JpaRepository<CjsmDirectoryEntry, Long> {
  fun findBySecureEmail(secureEmail: String): CjsmDirectoryEntry?
}

@Entity
@Table(name = "cjsm_directory")
data class CjsmDirectoryEntry(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  val id: Long = 0,

  @Column(unique = true)
  val secureEmail: String,

  val firstName: String,

  val lastName: String,

  val organisation: String,

  val townCity: String,

  val businessType: String,
) {
  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other == null || Hibernate.getClass(this) != Hibernate.getClass(other)) return false
    other as CjsmDirectoryEntry

    return secureEmail == other.secureEmail
  }

  override fun hashCode(): Int = secureEmail.hashCode()

  override fun toString(): String = "CjsmDirectoryEntry(secureEmail=$secureEmail, firstName='$firstName', lastName=$lastName, organisation=$organisation, townCity=$townCity, businessType=$businessType)"
}
