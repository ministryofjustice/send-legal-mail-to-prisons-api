package uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.cjsm

import com.amazonaws.services.s3.AmazonS3
import mu.KotlinLogging
import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVParser
import org.apache.commons.csv.CSVRecord
import org.springframework.stereotype.Service
import java.io.InputStream

private val log = KotlinLogging.logger {}

@Service
class CjsmService(
  private val amazonS3: AmazonS3,
  private val s3Config: S3Config,
  private val cjsmDirectoryRepository: CjsmDirectoryRepository
) {

  fun saveCjsmDirectoryCsv() =
    amazonS3.getObject(s3Config.bucketName, s3Config.cjsmDirectoryCsvName)
      .objectContent
      .let { saveCjsmDirectoryStream(it) }

  fun saveCjsmDirectoryStream(inputStream: InputStream) =
    CSVParser.parse(inputStream, Charsets.UTF_8, CSVFormat.DEFAULT)
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

  private fun CSVRecord.firstName() = this[0]
  private fun CSVRecord.lastName() = this[1]
  private fun CSVRecord.organisation() = this[3]
  private fun CSVRecord.secureEmail() = this[4]
  private fun CSVRecord.townCity() = this[7]
  private fun CSVRecord.businessType() = this[12]
}
