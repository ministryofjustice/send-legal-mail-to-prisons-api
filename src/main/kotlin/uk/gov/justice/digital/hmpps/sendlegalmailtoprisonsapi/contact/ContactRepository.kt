package uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.contact

import org.springframework.data.jpa.repository.JpaRepository
import java.time.Instant
import java.time.LocalDate
import javax.persistence.*

interface ContactRepository: JpaRepository<Contact, Int>

@Entity
@Table(name = "contacts")
data class Contact (
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  val id: Long,

  val owner: String,
  val name: String,
  val prisonCode: String,
  val dob: LocalDate?,
  val prisonNumber: String?,
  val created: Instant,
  val updated: Instant
)