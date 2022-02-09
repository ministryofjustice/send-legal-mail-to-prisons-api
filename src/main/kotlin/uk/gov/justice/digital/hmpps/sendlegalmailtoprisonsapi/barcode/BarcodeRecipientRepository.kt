package uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.barcode

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.contact.Contact
import java.time.LocalDate
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id
import javax.persistence.JoinColumn
import javax.persistence.ManyToOne
import javax.persistence.OneToOne
import javax.persistence.Table

@Repository
interface BarcodeRecipientRepository : JpaRepository<BarcodeRecipient, Long> {
  fun getByBarcode(barcode: Barcode): BarcodeRecipient?
}

@Entity
@Table(name = "barcode_recipients")
data class BarcodeRecipient(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  val id: Long = -1,

  @OneToOne
  @JoinColumn(name = "barcode")
  val barcode: Barcode,

  val name: String,

  @Column(name = "prison_code")
  val prisonCode: String,

  val dob: LocalDate? = null,

  @Column(name = "prison_number")
  val prisonNumber: String? = null,

  @ManyToOne
  @JoinColumn(name = "contact")
  val contact: Contact? = null,
)
