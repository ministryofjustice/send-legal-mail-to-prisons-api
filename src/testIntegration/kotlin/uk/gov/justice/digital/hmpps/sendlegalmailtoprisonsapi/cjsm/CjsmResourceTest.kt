package uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.cjsm

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.io.Resource
import org.springframework.http.MediaType
import uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.IntegrationTest

class CjsmResourceTest : IntegrationTest() {

  @Value("classpath:cjsm/cjsm-directory.csv")
  private lateinit var cjsmCsvResource: Resource

  @BeforeEach
  fun `load the CJSM directory CSV file`() {
    amazonS3.putObject(s3Config.bucketName, s3Config.cjsmDirectoryCsvName, cjsmCsvResource.file)
  }

  @Test
  fun `can load CJSM directory into the database`() {
    webTestClient.post()
      .uri("/cjsm/directory/refresh")
      .accept(MediaType.APPLICATION_JSON)
      .contentType(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation())
      .exchange()
      .expectStatus().isOk

    val cjsmDirectorySize = cjsmDirectoryRepository.count()

    assertThat(cjsmDirectorySize).isEqualTo(100)
  }
}
