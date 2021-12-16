package uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.cjsm

import com.amazonaws.services.s3.AmazonS3
import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVParser
import org.springframework.stereotype.Service
import java.io.BufferedInputStream
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader

@Service
class CjsmService(
  private val amazonS3: AmazonS3,
  private val s3Config: S3Config,
  private val cjsmDirectoryRepository: CjsmDirectoryRepository
) {

  fun readCjsmOrgs(): String =
    try {
      amazonS3.getObjectAsString(s3Config.bucketName, s3Config.cjsmOrgsCsv)
    } catch (ex: Exception) {
      "An error occurred attempting to read ${s3Config.cjsmOrgsCsv} from S3 bucket ${s3Config.bucketName}: ${ex.message}"
    }

  fun saveCjsmDirectoryCsv() =
    amazonS3.getObject(s3Config.bucketName, s3Config.cjsmOrgsCsv)
      .objectContent
      .let { streamCjsmDirectoryCsv(it) }

  fun streamCjsmDirectoryCsv(inputStream: InputStream) =
    inputStream.let { BufferedReader(InputStreamReader(BufferedInputStream(it))) }
      .useLines { lines ->
        lines.mapNotNull { line -> fromCjsmDirectoryCsvLine(line) }
          .forEach { cjsmDirectoryEntry -> cjsmDirectoryRepository.save(cjsmDirectoryEntry) }
      }

  private fun fromCjsmDirectoryCsvLine(csvLine: String): CjsmDirectoryEntry? =
    csvLine.takeIf { !it.contains("Secure Email") }
      ?.let { CSVParser.parse(csvLine, CSVFormat.DEFAULT).first() }
      ?.let { csvRecord ->
        CjsmDirectoryEntry(
          firstName = csvRecord[0],
          lastName = csvRecord[1],
          organisation = csvRecord[3],
          secureEmail = csvRecord[4],
          townCity = csvRecord[7],
          businessType = csvRecord[12],
        )
      }
}
