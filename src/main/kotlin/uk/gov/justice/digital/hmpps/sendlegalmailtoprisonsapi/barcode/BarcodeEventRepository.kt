package uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.barcode

import org.hibernate.Hibernate
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.time.Instant
import javax.persistence.Entity
import javax.persistence.EnumType
import javax.persistence.Enumerated
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id
import javax.persistence.JoinColumn
import javax.persistence.ManyToOne
import javax.persistence.Table

@Repository
interface BarcodeEventRepository : JpaRepository<BarcodeEvent, Long> {

  fun findByBarcodeAndEventTypeOrderByCreatedDateTime(barcode: Barcode, eventType: BarcodeEventType): List<BarcodeEvent>

  @Query("select be from BarcodeEvent as be where be.barcode = :barcode and be.eventType = 'CREATED'")
  fun findByBarcodeAndEventTypeCreated(barcode: Barcode): BarcodeEvent?
}

@Entity
@Table(name = "barcode_events")
data class BarcodeEvent(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  val id: Long = -1,

  @ManyToOne
  @JoinColumn(name = "barcode")
  val barcode: Barcode,

  val userId: String,

  @Enumerated(EnumType.STRING)
  val eventType: BarcodeEventType,

  val createdDateTime: Instant = Instant.now(),

  val location: String = "",
) {
  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other == null || Hibernate.getClass(this) != Hibernate.getClass(other)) return false
    other as BarcodeEvent

    return barcode == other.barcode && createdDateTime == other.createdDateTime
  }

  override fun hashCode(): Int = barcode.hashCode() * createdDateTime.hashCode()

  override fun toString(): String {
    return "BarcodeEvent(barcode=$barcode, userId='$userId', eventType=$eventType, dateTime=$createdDateTime, location=$location)"
  }
}

enum class BarcodeEventType { CREATED, CHECKED, DUPLICATE, EXPIRED, RANDOM_CHECK }
