package uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.barcode

import org.hibernate.Hibernate
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.time.LocalDateTime
import javax.persistence.Entity
import javax.persistence.EnumType
import javax.persistence.Enumerated
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id
import javax.persistence.JoinColumn
import javax.persistence.ManyToOne
import javax.persistence.Table
import javax.validation.constraints.NotNull

@Repository
interface BarcodeEventRepository : JpaRepository<BarcodeEvent, Long> {
  fun findByBarcode(barcode: Barcode): List<BarcodeEvent>
}

@Entity
@Table(name = "barcode_events")
data class BarcodeEvent(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @NotNull
  val id: Long = -1,
  @NotNull
  @ManyToOne
  @JoinColumn(name = "barcode")
  val barcode: Barcode,
  @NotNull
  val userId: String,
  @NotNull
  @Enumerated(EnumType.STRING)
  val status: BarcodeStatus = BarcodeStatus.CREATED,
  @NotNull
  val dateTime: LocalDateTime = LocalDateTime.now(),
) {
  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other == null || Hibernate.getClass(this) != Hibernate.getClass(other)) return false
    other as BarcodeEvent

    return barcode == other.barcode && dateTime == other.dateTime
  }

  override fun hashCode(): Int = barcode.hashCode() * dateTime.hashCode()

  override fun toString(): String {
    return "BarcodeEvent(barcode=$barcode, userId='$userId', status=$status, dateTime=$dateTime)"
  }
}

enum class BarcodeStatus { CREATED, SCANNED }
