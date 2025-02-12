package uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.barcode

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import org.hibernate.Hibernate
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.time.Instant

@Repository
interface BarcodeEventRepository : JpaRepository<BarcodeEvent, Long> {

  fun findByBarcodeAndEventTypeOrderByCreatedDateTime(barcode: Barcode, eventType: BarcodeEventType): List<BarcodeEvent>

  @Query("select be from BarcodeEvent as be where be.barcode = :barcode and be.eventType = 'CREATED'")
  fun findByBarcodeAndEventTypeCreated(barcode: Barcode): BarcodeEvent?

  @Query("select count(*) from BarcodeEvent as be where be.eventType = :eventType")
  fun countAllEventsByType(eventType: BarcodeEventType): Long

  @Query("select count(distinct(be.barcode)) from BarcodeEvent as be where be.eventType = :eventType")
  fun countDistinctBarcodesWithEvent(eventType: BarcodeEventType): Long

  @Query("select count(*) from BarcodeEvent as be where be.eventType = :eventType and be.createdDateTime between :fromTime and :toTime")
  fun countAllEventsBetweenTimes(eventType: BarcodeEventType, fromTime: Instant, toTime: Instant): Long

  @Query("select count(distinct(be.barcode)) from BarcodeEvent as be where be.eventType = :eventType and be.createdDateTime between :fromTime and :toTime")
  fun countDistinctBarcodesWithEventBetweenTimes(eventType: BarcodeEventType, fromTime: Instant, toTime: Instant): Long

  @Query("select count(distinct(be.userId)) from BarcodeEvent as be where be.eventType = :eventType")
  fun countDistinctUsersWithEvent(eventType: BarcodeEventType): Long
}

@Entity
@Table(name = "barcode_events")
data class BarcodeEvent(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  val id: Long = 0,

  @ManyToOne
  @JoinColumn(name = "barcode")
  val barcode: Barcode,

  val userId: String,

  @Enumerated(EnumType.STRING)
  val eventType: BarcodeEventType,

  val createdDateTime: Instant = Instant.now(),

  val location: String = "",

  @Column(name = "ip_address")
  val ipAddress: String,
) {
  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other == null || Hibernate.getClass(this) != Hibernate.getClass(other)) return false
    other as BarcodeEvent

    return barcode == other.barcode && createdDateTime == other.createdDateTime
  }

  override fun hashCode(): Int = barcode.hashCode() * createdDateTime.hashCode()

  override fun toString(): String = "BarcodeEvent(barcode=$barcode, userId='$userId', eventType=$eventType, dateTime=$createdDateTime, location=$location, ipAddress=$ipAddress)"
}

enum class BarcodeEventType { CREATED, CHECKED, DUPLICATE, EXPIRED, RANDOM_CHECK, MORE_CHECKS_REQUESTED }
