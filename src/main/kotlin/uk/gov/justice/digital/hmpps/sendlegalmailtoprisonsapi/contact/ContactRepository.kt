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
import javax.validation.constraints.NotNull

interface ContactRepository : JpaRepository<Contact, Int>

@Entity
@Table(name = "contacts")
data class Contact(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  val id: Long? = null,

  @NotNull
  val owner: String,

  @NotNull
  val name: String,

  @NotNull
  @Column(name = "prison_code")
  val prisonCode: String,

  val dob: LocalDate? = null,

  @Column(name = "prison_number")
  val prisonNumber: String? = null,

  @NotNull
  val created: Instant,

  @NotNull
  val updated: Instant
)
