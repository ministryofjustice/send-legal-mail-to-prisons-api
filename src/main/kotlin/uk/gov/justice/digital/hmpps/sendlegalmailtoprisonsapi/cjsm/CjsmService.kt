package uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.cjsm

import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.s3.model.AmazonS3Exception
import com.amazonaws.services.s3.model.DeleteObjectRequest
import mu.KotlinLogging
import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVParser
import org.apache.commons.csv.CSVRecord
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.config.ResourceNotFoundException
import java.io.IOException
import java.io.InputStream
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.transaction.Transactional
import kotlin.jvm.Throws

private val log = KotlinLogging.logger {}

@Service
class CjsmService(
  private val amazonS3: AmazonS3,
  private val s3Config: S3Config,
  private val cjsmDirectoryRepository: CjsmDirectoryRepository
) {

  @Transactional
  fun saveCjsmDirectoryCsv() =
    try {
      amazonS3.getObject(s3Config.bucketName, s3Config.cjsmDirectoryCsvName)
        .also { log.info("Found CJSM directory file upgrade for S3Object ${it.key}") }
        .objectContent
        .let { saveCjsmDirectoryStream(it) }
    } catch (ex: AmazonS3Exception) {
      log.error("Failed CJSM directory upload due to AmazonS3Exception", ex)
      throw ResourceNotFoundException("Failed to load the CJSM directory file due to ${ex.message}")
    } catch (ex: IOException) {
      log.error("Failed CJSM directory upload due to IOException", ex)
      throw ResourceNotFoundException("Failed to load the CJSM directory file due to ${ex.message}")
    }
      .also { archiveCjsmDirectoryFile() }

  private fun archiveCjsmDirectoryFile() {
    val archiveFileName = """/done/${s3Config.cjsmDirectoryCsvName}-${DateTimeFormatter.ISO_DATE_TIME.format(LocalDateTime.now())}"""
    log.info("Attempting to archive CJSM directory file ${s3Config.cjsmDirectoryCsvName} to $archiveFileName")
    amazonS3.copyObject(
      s3Config.bucketName,
      s3Config.cjsmDirectoryCsvName,
      s3Config.bucketName,
      archiveFileName
    )
    log.info("Copied the existing CJSM directory file ${s3Config.cjsmDirectoryCsvName} to $archiveFileName")
    amazonS3.deleteObject(DeleteObjectRequest(s3Config.bucketName, s3Config.cjsmDirectoryCsvName))
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

  fun findUser(secureEmail: String): CjsmDirectoryEntry? = cjsmDirectoryRepository.findBySecureEmail(secureEmail)

  private fun CSVRecord.firstName() = this[0]
  private fun CSVRecord.lastName() = this[1]
  private fun CSVRecord.organisation() = this[3]
  private fun CSVRecord.secureEmail() = this[4]
  private fun CSVRecord.townCity() = this[7]
  private fun CSVRecord.businessType() = this[12]
}
