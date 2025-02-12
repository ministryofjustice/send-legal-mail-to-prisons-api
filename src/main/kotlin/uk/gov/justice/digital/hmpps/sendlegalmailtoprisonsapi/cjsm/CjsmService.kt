package uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.cjsm

import jakarta.transaction.Transactional
import mu.KotlinLogging
import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVParser
import org.apache.commons.csv.CSVRecord
import org.springframework.stereotype.Service
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.CopyObjectRequest
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest
import software.amazon.awssdk.services.s3.model.GetObjectRequest
import software.amazon.awssdk.services.s3.model.S3Exception
import uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.config.ResourceNotFoundException
import java.io.IOException
import java.io.InputStream
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.jvm.Throws

private val log = KotlinLogging.logger {}

@Service
class CjsmService(
  private val amazonS3: S3Client,
  private val s3Config: S3Config,
  private val cjsmDirectoryRepository: CjsmDirectoryRepository,
) {

  @Transactional
  fun saveCjsmDirectoryCsv() = try {
    val getObjectRequest = GetObjectRequest.builder().bucket(s3Config.bucketName).key(s3Config.cjsmDirectoryCsvName).build()
    val response = amazonS3.getObject(getObjectRequest)
    saveCjsmDirectoryStream(response)
  } catch (ex: S3Exception) {
    log.error("Failed CJSM directory upload due to AmazonS3Exception", ex)
    throw ResourceNotFoundException("Failed to load the CJSM directory file due to ${ex.message}")
  } catch (ex: IOException) {
    log.error("Failed CJSM directory upload due to IOException", ex)
    throw ResourceNotFoundException("Failed to load the CJSM directory file due to ${ex.message}")
  }
    .also { archiveCjsmDirectoryFile() }

  private fun archiveCjsmDirectoryFile() {
    val archiveFileName =
      """/done/${s3Config.cjsmDirectoryCsvName}-${DateTimeFormatter.ISO_DATE_TIME.format(LocalDateTime.now())}"""
    log.info("Attempting to archive CJSM directory file ${s3Config.cjsmDirectoryCsvName} to $archiveFileName")
    val copyObjectRequest = CopyObjectRequest.builder().sourceBucket(s3Config.bucketName)
      .sourceKey(s3Config.cjsmDirectoryCsvName)
      .destinationBucket(s3Config.bucketName)
      .destinationKey(archiveFileName)
      .build()
    amazonS3.copyObject(copyObjectRequest)
    log.info("Copied the existing CJSM directory file ${s3Config.cjsmDirectoryCsvName} to $archiveFileName")

    val deleteObjectRequest = DeleteObjectRequest.builder().bucket(s3Config.bucketName).key(s3Config.cjsmDirectoryCsvName).build()
    amazonS3.deleteObject(deleteObjectRequest)
    log.info("Deleted the existing ${s3Config.cjsmDirectoryCsvName}")
  }

  @Throws(IOException::class)
  fun saveCjsmDirectoryStream(inputStream: InputStream) {
    cjsmDirectoryRepository.deleteAll()
    cjsmDirectoryRepository.flush()
    CSVParser.parse(inputStream, Charsets.UTF_8, CSVFormat.DEFAULT)
      .asSequence()
      .forEach { csvRecord ->
        takeIf { !csvRecord.secureEmail().contains("Secure Email") }
          ?.takeIf { csvRecord.secureEmail().isNotBlank() }
          ?.let {
            with(csvRecord) {
              CjsmDirectoryEntry(
                firstName = firstName(),
                lastName = lastName(),
                organisation = organisation(),
                secureEmail = secureEmail(),
                townCity = townCity(),
                businessType = businessType(),
              )
            }
          }
          ?.also { cjsmDirectoryEntry ->
            try {
              cjsmDirectoryRepository.save(cjsmDirectoryEntry)
            } catch (ex: Exception) {
              log.error("Failed to load record $cjsmDirectoryEntry due to ${ex.message}")
            }
          }
      }
  }

  fun findOrganisation(secureEmail: String): String? = findUser(secureEmail)?.organisation

  fun findUser(secureEmail: String): UserDetails? = cjsmDirectoryRepository.findBySecureEmail(secureEmail)?.let { toUserDetails(it) }

  private fun CSVRecord.firstName() = this[0]
  private fun CSVRecord.lastName() = this[1]
  private fun CSVRecord.organisation() = this[3]
  private fun CSVRecord.secureEmail() = this[4]
  private fun CSVRecord.townCity() = this[7]
  private fun CSVRecord.businessType() = this[12]
}

private fun toUserDetails(cjsmDirectoryEntry: CjsmDirectoryEntry): UserDetails = UserDetails(
  cjsmDirectoryEntry.secureEmail,
  cjsmDirectoryEntry.organisation,
  cjsmDirectoryEntry.businessType,
  cjsmDirectoryEntry.townCity,
)
