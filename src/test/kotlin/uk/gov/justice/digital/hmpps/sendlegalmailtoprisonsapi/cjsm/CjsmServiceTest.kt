package uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.cjsm

import com.amazonaws.services.s3.AmazonS3
import com.nhaarman.mockitokotlin2.check
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.verifyNoMoreInteractions
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class CjsmServiceTest {

  private val amazonS3 = mock<AmazonS3>()
  private val s3Config = mock<S3Config>()
  private val cjsmDirectoryRepository = mock<CjsmDirectoryRepository>()
  private val cjsmService = CjsmService(amazonS3, s3Config, cjsmDirectoryRepository)

  @Test
  fun `can load a simple csv`() {
    val csv = """
      Firstname,Lastname,UserType,Organisation,Secure Email,Address1,Address2,Town/City,Postcode,Telephone,Mobile,Description,Business Type,CJ Area,Account Status
      Abigail,Husbands,smtpmail,29 Somerset Street (London),abigailhusbands@23es.com.cjsm.net,29 Somerset Street,29 Somerset Street,London,WC3R3AB,020 4137 3533,,,Barristers,London,Active
    """.trimIndent()

    cjsmService.streamCjsmDirectoryCsv(csv.byteInputStream())

    verify(cjsmDirectoryRepository).save(
      check {
        assertThat(it).extracting("secureEmail", "firstName", "lastName", "organisation", "townCity", "businessType")
          .isEqualTo(listOf("abigailhusbands@23es.com.cjsm.net", "Abigail", "Husbands", "29 Somerset Street (London)", "London", "Barristers"))
      }
    )
    verifyNoMoreInteractions(cjsmDirectoryRepository)
  }
}
