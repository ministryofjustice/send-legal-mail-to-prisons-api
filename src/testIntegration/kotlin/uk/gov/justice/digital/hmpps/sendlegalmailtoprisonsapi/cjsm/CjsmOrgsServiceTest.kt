package uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.cjsm

import com.amazonaws.services.s3.AmazonS3
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.IntegrationTest

class CjsmOrgsServiceTest : IntegrationTest() {

  @Autowired
  private lateinit var s3Client: AmazonS3

  @Autowired
  private lateinit var s3Config: S3Config

  @Autowired
  private lateinit var cjsmOrgsService: CjsmOrgsService

  @Test
  fun `Can load the CJSM orgs from an S3 bucket`() {
    uploadCsv()

    val contents = cjsmOrgsService.readCjsmOrgs()

    assertThat(contents).contains("firstName", "lastName", "Amanda", "Brown")
  }

  private fun uploadCsv() =
    s3Client.putObject(
      s3Config.bucketName, "cjsm-orgs.csv",
      """
      firstName,lastName
      Amanda,Brown
      """.trimIndent()
    )
}
