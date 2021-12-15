package uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.cjsm

import com.amazonaws.services.s3.AmazonS3
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.IntegrationTest

class CjsmOrgsResourceTest : IntegrationTest() {

  @Autowired
  private lateinit var s3Client: AmazonS3

  @Autowired
  private lateinit var s3Config: S3Config

  @Autowired
  private lateinit var cjsmService: CjsmService

  @BeforeEach
  fun `refresh csv`() {
    deleteCsv()
    uploadCsv()
  }

  @Test
  fun `Can load the CJSM orgs from an S3 bucket`() {
    webTestClient.get()
      .uri("/cjsm")
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation())
      .exchange()
      .expectStatus().isOk
      .expectBody()
      .jsonPath("$").value<String> { assertThat(it).contains("firstName,lastName") }
      .jsonPath("$").value<String> { assertThat(it).contains("Amanda,Brown") }
  }

  private fun uploadCsv() =
    s3Client.putObject(
      s3Config.bucketName, s3Config.cjsmOrgsCsv,
      """
      firstName,lastName
      Amanda,Brown
      """.trimIndent()
    )

  private fun deleteCsv() =
    try {
      s3Client.deleteObject(s3Config.bucketName, s3Config.cjsmOrgsCsv)
    } catch (ex: Exception) {
      // NOOP
    }
}
