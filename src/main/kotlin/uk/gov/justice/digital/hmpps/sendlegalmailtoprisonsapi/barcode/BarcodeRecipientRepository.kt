package uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.barcode

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToOne
import jakarta.persistence.Table
import org.hibernate.Hibernate
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.contact.Contact
import java.time.LocalDate

@Repository
interface BarcodeRecipientRepository : JpaRepository<BarcodeRecipient, Long> {
  fun getByBarcode(barcode: Barcode): BarcodeRecipient?
}

@Entity
@Table(name = "barcode_recipients")
data class BarcodeRecipient(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  val id: Long? = null,

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
) {
  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other == null || Hibernate.getClass(this) != Hibernate.getClass(other)) return false
    other as BarcodeRecipient

    return id != null && id == other.id
  }

  override fun hashCode(): Int = javaClass.hashCode()

  @Override
  override fun toString(): String {
    return this::class.simpleName + "(id = $id , barcode = $barcode , prisonCode = $prisonCode , contactId = ${contact?.id} )"
  }
}
