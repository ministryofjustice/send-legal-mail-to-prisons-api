package uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.barcode

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.time.LocalDate
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id
import javax.persistence.JoinColumn
import javax.persistence.OneToOne
import javax.persistence.Table
import javax.validation.constraints.NotNull

@Repository
interface BarcodeRecipientRepository : JpaRepository<BarcodeRecipient, Long> {
  fun getByBarcode(barcode: Barcode): BarcodeRecipient?
}

@Entity
@Table(name = "barcode_recipients")
data class BarcodeRecipient(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @NotNull
  val id: Long = -1,

  @NotNull
  @OneToOne
  @JoinColumn(name = "barcode")
  val barcode: Barcode,

  @NotNull
  val name: String,

  @NotNull
  @Column(name = "prison_code")
  val prisonCode: String,

  val dob: LocalDate? = null,

  @Column(name = "prison_number")
  val prisonNumber: String? = null,
)
