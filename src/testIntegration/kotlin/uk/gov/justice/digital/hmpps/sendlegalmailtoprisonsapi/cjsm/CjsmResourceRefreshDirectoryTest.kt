package uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.cjsm

import com.amazonaws.services.s3.model.AmazonS3Exception
import com.amazonaws.services.s3.model.DeleteObjectRequest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyString
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.io.Resource
import org.springframework.http.MediaType
import uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.IntegrationTest

class CjsmResourceRefreshDirectoryTest : IntegrationTest() {

  @Value("classpath:cjsm/cjsm-directory.csv")
  private lateinit var cjsmCsvResource: Resource

  @BeforeEach
  fun `clear the s3 bucket`() {
    val bucketObjects = amazonS3.listObjects(s3Config.bucketName)
    bucketObjects.objectSummaries.forEach {
      amazonS3.deleteObject(DeleteObjectRequest(s3Config.bucketName, it.key))
    }
  }

  @Nested
  inner class HappyPath {
    @Test
    fun `should replace existing data with CJSM directory csv and archive the csv`() {
      uploadCjsmDirectoryCsvToS3()
      saveCjsmDirectoryEntry("should.be.deleted@company.com.cjsm.net")

      webTestClient.post()
        .uri("/cjsm/directory/refresh")
        .accept(MediaType.APPLICATION_JSON)
        .contentType(MediaType.APPLICATION_JSON)
        .headers(setAuthorisation())
        .exchange()
        .expectStatus().isOk

      assertThat(cjsmDirectoryRepository.count()).isEqualTo(100)

      // The old database entries should have been deleted
      assertThat(cjsmDirectoryRepository.findBySecureEmail("should.be.deleted@company.com.cjsm.net")).isNull()

      val bucketObjects = amazonS3.listObjects(s3Config.bucketName)
      // The CJSM directory csv should have been removed the bucket
      assertThat(bucketObjects.objectSummaries.find { it.key == s3Config.cjsmDirectoryCsvName }).isNull()
      // But the archived CJSM directory csv should exist in the bucket
      assertThat(bucketObjects.objectSummaries.find { it.key.contains(s3Config.cjsmDirectoryCsvName) }).isNotNull
    }

    @Test
    fun `should allow two runs in a row`() {
      uploadCjsmDirectoryCsvToS3()

      webTestClient.post()
        .uri("/cjsm/directory/refresh")
        .accept(MediaType.APPLICATION_JSON)
        .contentType(MediaType.APPLICATION_JSON)
        .headers(setAuthorisation())
        .exchange()
        .expectStatus().isOk

      uploadCjsmDirectoryCsvToS3()

      webTestClient.post()
        .uri("/cjsm/directory/refresh")
        .accept(MediaType.APPLICATION_JSON)
        .contentType(MediaType.APPLICATION_JSON)
        .headers(setAuthorisation())
        .exchange()
        .expectStatus().isOk

      assertThat(cjsmDirectoryRepository.count()).isEqualTo(100)
    }
  }

  @Nested
  inner class SadPaths {
    @Test
    fun `if no CJSM directory csv file exists we should return not found and retain existing data `() {
      saveCjsmDirectoryEntry("should.not.be.deleted@company.com.cjsm.net")

      webTestClient.post()
        .uri("/cjsm/directory/refresh")
        .accept(MediaType.APPLICATION_JSON)
        .contentType(MediaType.APPLICATION_JSON)
        .headers(setAuthorisation())
        .exchange()
        .expectStatus().isNotFound

      assertThat(cjsmDirectoryRepository.findBySecureEmail("should.not.be.deleted@company.com.cjsm.net")).isNotNull
    }


    @Test
    fun `if the upload fails we should leave the old CJSM directory csv and retain existing data`() {
      uploadCjsmDirectoryCsvToS3()
      saveCjsmDirectoryEntry("should.not.be.deleted@company.com.cjsm.net")
      whenever(amazonS3.copyObject(anyString(), anyString(), anyString(), anyString()))
        .thenThrow(AmazonS3Exception::class.java)

      webTestClient.post()
        .uri("/cjsm/directory/refresh")
        .accept(MediaType.APPLICATION_JSON)
        .contentType(MediaType.APPLICATION_JSON)
        .headers(setAuthorisation())
        .exchange()
        .expectStatus().is5xxServerError

      // The old database entries should not be deleted
      assertThat(cjsmDirectoryRepository.findBySecureEmail("should.not.be.deleted@company.com.cjsm.net")).isNotNull

      // The CJSM directory csv should still be in the bucket
      val bucketObjects = amazonS3.listObjects(s3Config.bucketName)
      assertThat(bucketObjects.objectSummaries.find { it.key == s3Config.cjsmDirectoryCsvName }).isNotNull
    }
  }

  fun uploadCjsmDirectoryCsvToS3() {
    amazonS3.putObject(s3Config.bucketName, s3Config.cjsmDirectoryCsvName, cjsmCsvResource.file)
  }

  fun saveCjsmDirectoryEntry(secureEmail: String) =
    cjsmDirectoryRepository.save(
      CjsmDirectoryEntry(
        secureEmail = secureEmail,
        firstName = "any",
        lastName = "any",
        organisation = "any",
        townCity = "any",
        businessType = "any",
      ),
    )
}
