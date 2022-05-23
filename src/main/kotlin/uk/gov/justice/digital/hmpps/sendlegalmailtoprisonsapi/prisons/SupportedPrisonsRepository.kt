package uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.prisons

import org.hibernate.Hibernate
import org.springframework.data.annotation.LastModifiedBy
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.time.Instant
import java.time.format.DateTimeFormatter
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.EntityListeners
import javax.persistence.Id
import javax.persistence.Table

@Repository
interface SupportedPrisonsRepository : JpaRepository<SupportedPrison, String> {
  fun findByActive(active: Boolean): List<SupportedPrison>
}

@Entity
@Table(name = "supported_prisons")
@EntityListeners(AuditingEntityListener::class)
data class SupportedPrison(
  @Id
  val code: String = "",
  val active: Boolean,
  @LastModifiedBy
  @Column(name = "updated_by", nullable = false)
  var updatedBy: String? = null,
  @LastModifiedDate
  @Column(name = "updated", nullable = false)
  var updated: Instant? = null,
) {
  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other == null || Hibernate.getClass(this) != Hibernate.getClass(other)) return false
    other as SupportedPrison

    return code == other.code
  }

  override fun hashCode(): Int = javaClass.hashCode()

  @Override
  override fun toString(): String =
    this::class.simpleName + "(code = $code, active = $active, updatedBy = ${updatedBy ?: "null"}, " +
      "updated = ${updated?.let { DateTimeFormatter.ISO_DATE_TIME.format(updated) } ?: "null"} )"
}
