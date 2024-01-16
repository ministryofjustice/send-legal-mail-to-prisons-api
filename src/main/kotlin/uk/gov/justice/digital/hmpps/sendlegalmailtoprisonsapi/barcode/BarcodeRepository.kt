package uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.barcode

import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface BarcodeRepository : JpaRepository<Barcode, String>

@Entity
@Table(name = "barcodes")
data class Barcode(
  @Id
  val code: String = "",
)
