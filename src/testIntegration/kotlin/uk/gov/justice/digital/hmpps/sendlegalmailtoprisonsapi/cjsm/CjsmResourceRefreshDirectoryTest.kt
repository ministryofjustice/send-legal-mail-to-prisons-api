package uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.cjsm

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.doThrow
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.io.Resource
import org.springframework.http.MediaType
import software.amazon.awssdk.services.s3.model.CopyObjectRequest
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest
import software.amazon.awssdk.services.s3.model.ListObjectsRequest
import software.amazon.awssdk.services.s3.model.PutObjectRequest
import software.amazon.awssdk.services.s3.model.S3Exception
import uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.IntegrationTest
import java.nio.file.Path

class CjsmResourceRefreshDirectoryTest : IntegrationTest() {

  @Value("classpath:cjsm/cjsm-directory.csv")
  private lateinit var cjsmCsvResource: Resource

  @BeforeEach
  fun `clear the s3 bucket`() {
    val listObjectRequest = ListObjectsRequest.builder().bucket(s3Config.bucketName).build()
    val bucketObjects = amazonS3.listObjects(listObjectRequest)
    bucketObjects.contents().forEach {
      val deleteObjectsRequest = DeleteObjectRequest.builder().bucket(s3Config.bucketName).key(it.key()).build()
      amazonS3.deleteObject(deleteObjectsRequest)
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
      val listObjectsRequest = ListObjectsRequest.builder().bucket(s3Config.bucketName).build()

      val bucketObjects = amazonS3.listObjects(listObjectsRequest)
      // The CJSM directory csv should have been removed the bucket
      assertThat(bucketObjects.contents().find { it.key() == s3Config.cjsmDirectoryCsvName }).isNull()
      // But the archived CJSM directory csv should exist in the bucket
      assertThat(bucketObjects.contents().find { it.key().contains(s3Config.cjsmDirectoryCsvName) }).isNotNull
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
      doThrow(S3Exception::class.java).`when`(
        amazonS3,
      ).copyObject(any<CopyObjectRequest>())

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
      val listObjectsRequest = ListObjectsRequest.builder().bucket(s3Config.bucketName).build()
      val bucketObjects = amazonS3.listObjects(listObjectsRequest)
      assertThat(bucketObjects.contents().find { it.key() == s3Config.cjsmDirectoryCsvName }).isNotNull
    }
  }

  fun uploadCjsmDirectoryCsvToS3() {
    val putObjectRequest = PutObjectRequest.builder().bucket(s3Config.bucketName).key(s3Config.cjsmDirectoryCsvName).build()
    val path = Path.of(cjsmCsvResource.uri)
    amazonS3.putObject(putObjectRequest, path)
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
