package uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.prisons

import org.hibernate.Hibernate
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.time.Instant
import java.time.format.DateTimeFormatter
import javax.persistence.Entity
import javax.persistence.Id
import javax.persistence.Table

@Repository
interface SupportedPrisonsRepository : JpaRepository<SupportedPrison, String> {
  fun findByActive(active: Boolean): List<SupportedPrison>
}

@Entity
@Table(name = "supported_prisons")
data class SupportedPrison(
  @Id
  val code: String = "",
  val active: Boolean,
  val updatedBy: String,
  val updated: Instant,
) {
  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other == null || Hibernate.getClass(this) != Hibernate.getClass(other)) return false
    other as SupportedPrison

    return code == other.code
  }

  override fun hashCode(): Int = javaClass.hashCode()

  @Override
  override fun toString(): String {
    return this::class.simpleName + "(code = $code, active = $active, updatedBy = $updatedBy, updated = ${DateTimeFormatter.ISO_DATE_TIME.format(updated)} )"
  }
}
