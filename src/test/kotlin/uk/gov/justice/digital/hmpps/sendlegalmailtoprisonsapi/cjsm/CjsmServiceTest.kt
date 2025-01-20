package uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.cjsm

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.check
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoMoreInteractions
import org.mockito.kotlin.whenever
import software.amazon.awssdk.services.s3.S3Client

class CjsmServiceTest {

  private val amazonS3 = mock<S3Client>()
  private val s3Config = mock<S3Config>()
  private val cjsmDirectoryRepository = mock<CjsmDirectoryRepository>()
  private val cjsmService = CjsmService(amazonS3, s3Config, cjsmDirectoryRepository)

  @Test
  fun `can load a simple csv`() {
    val csv = """
      Firstname,Lastname,UserType,Organisation,Secure Email,Address1,Address2,Town/City,Postcode,Telephone,Mobile,Description,Business Type,CJ Area,Account Status
      Abigail,Husbands,smtpmail,29 Somerset Street (London),abigailhusbands@23es.com.cjsm.net,29 Somerset Street,29 Somerset Street,London,WC3R3AB,020 4137 3533,,,Barristers,London,Active
    """.trimIndent()

    cjsmService.saveCjsmDirectoryStream(csv.byteInputStream())

    verify(cjsmDirectoryRepository).save(
      check {
        assertThat(it).extracting("secureEmail", "firstName", "lastName", "organisation", "townCity", "businessType")
          .isEqualTo(listOf("abigailhusbands@23es.com.cjsm.net", "Abigail", "Husbands", "29 Somerset Street (London)", "London", "Barristers"))
      },
    )
  }

  @Test
  fun `can load a csv with commas`() {
    val csv = """
      Firstname,Lastname,UserType,Organisation,Secure Email,Address1,Address2,Town/City,Postcode,Telephone,Mobile,Description,Business Type,CJ Area,Account Status
      Bridlington,AJU,smtpgroup,Humberside Police,ajubridlington@humberside.pnn.police.uk.cjsm.net,"Sessions House, New Walk","Sessions House, New Walk",Beverley,HU17 7AF, , , ,Police,Humberside,Active
    """.trimIndent()

    cjsmService.saveCjsmDirectoryStream(csv.byteInputStream())

    verify(cjsmDirectoryRepository).save(
      check {
        assertThat(it).extracting("secureEmail", "firstName", "lastName", "organisation", "townCity", "businessType")
          .isEqualTo(listOf("ajubridlington@humberside.pnn.police.uk.cjsm.net", "Bridlington", "AJU", "Humberside Police", "Beverley", "Police"))
      },
    )
  }

  @Test
  fun `ignores records without an email address`() {
    val csv = """
      Firstname,Lastname,UserType,Organisation,Secure Email,Address1,Address2,Town/City,Postcode,Telephone,Mobile,Description,Business Type,CJ Area,Account Status
      Bridlington,AJU,smtpgroup,Humberside Police,ajubridlington@humberside.pnn.police.uk.cjsm.net,"Sessions House, New Walk","Sessions House, New Walk",Beverley,HU17 7AF, , , ,Police,Humberside,Active
      No,Email,ignored?,,,"Sessions House, New Walk","Sessions House, New Walk",Beverley,HU17 7AF, , , ,Police,Humberside,Active
    """.trimIndent()

    cjsmService.saveCjsmDirectoryStream(csv.byteInputStream())

    verify(cjsmDirectoryRepository).deleteAll()
    verify(cjsmDirectoryRepository).flush()
    verify(cjsmDirectoryRepository).save(
      check {
        assertThat(it).extracting("secureEmail", "firstName", "lastName", "organisation", "townCity", "businessType")
          .isEqualTo(listOf("ajubridlington@humberside.pnn.police.uk.cjsm.net", "Bridlington", "AJU", "Humberside Police", "Beverley", "Police"))
      },
    )
    verifyNoMoreInteractions(cjsmDirectoryRepository)
  }

  @Test
  fun `carries on despite exceptions thrown by the database`() {
    val csv = """
      Firstname,Lastname,UserType,Organisation,Secure Email,Address1,Address2,Town/City,Postcode,Telephone,Mobile,Description,Business Type,CJ Area,Account Status
      Abigail,Husbands,smtpmail,29 Somerset Street (London),abigailhusbands@23es.com.cjsm.net,29 Somerset Street,29 Somerset Street,London,WC3R3AB,020 4137 3533,,,Barristers,London,Active
      Bridlington,AJU,smtpgroup,Humberside Police,ajubridlington@humberside.pnn.police.uk.cjsm.net,"Sessions House, New Walk","Sessions House, New Walk",Beverley,HU17 7AF, , , ,Police,Humberside,Active
    """.trimIndent()

    whenever(cjsmDirectoryRepository.save(any()))
      .thenThrow(RuntimeException::class.java)
      .thenReturn(CjsmDirectoryEntry(1L, "any", "any", "any", "any", "any", "any"))

    cjsmService.saveCjsmDirectoryStream(csv.byteInputStream())

    verify(cjsmDirectoryRepository).save(
      check {
        assertThat(it).extracting("secureEmail", "firstName", "lastName", "organisation", "townCity", "businessType")
          .isEqualTo(listOf("abigailhusbands@23es.com.cjsm.net", "Abigail", "Husbands", "29 Somerset Street (London)", "London", "Barristers"))
      },
    )
    verify(cjsmDirectoryRepository).save(
      check {
        assertThat(it).extracting("secureEmail", "firstName", "lastName", "organisation", "townCity", "businessType")
          .isEqualTo(listOf("ajubridlington@humberside.pnn.police.uk.cjsm.net", "Bridlington", "AJU", "Humberside Police", "Beverley", "Police"))
      },
    )
  }
}
