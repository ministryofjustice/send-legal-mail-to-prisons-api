package uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.cjsm

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id
import javax.persistence.Table
import javax.validation.constraints.NotNull

@Repository
interface CjsmDirectoryRepository : JpaRepository<CjsmDirectoryEntry, Long>

@Entity
@Table(name = "cjsm_directory")
data class CjsmDirectoryEntry(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @NotNull
  val id: Long = -1,
  @NotNull
  val secureEmail: String,
  val firstName: String,
  val lastName: String,
  val organisation: String,
  val townCity: String,
  val businessType: String,
)
